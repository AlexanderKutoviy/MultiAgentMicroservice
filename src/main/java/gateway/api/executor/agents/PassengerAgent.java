package gateway.api.executor.agents;

import gateway.api.executor.behaviours.BehaviourUtils;
import gateway.api.executor.behaviours.PassengerVisitRequestBehaviour;
import gateway.api.executor.helpers.DFServiceHelper;
import gateway.api.executor.model.Location;
import gateway.api.executor.model.PassengerStatus;
import gateway.api.executor.model.TaskContext;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.ArrayList;

public class PassengerAgent extends Agent {
    private Location home;
    private static long modulationPeriod = 0;

    private PassengerStatus status;

    @Override
    protected void setup(){
        home = TaskContext.generateRandomFreeLocation();
        status = PassengerStatus.AT_HOME;
//        System.out.println("Passenger agent with ID: " + getAID().getName()  + " set up!");

        DFServiceHelper.registerAgentInYellowPages(this, TaskContext.PASSENGER_SERVICE_NAME, TaskContext.PASSENGER_SERVICE_TYPE);

        handleInvitationsServer();

        visitAnotherPassengerBehaviour(TaskContext.getVisitEveryPeriodInMillis());

    }

    public void setStatus(PassengerStatus status) {
        this.status = status;
    }


    /**
     * Visit another Passenger (with status = AT_HOME) every period of time
     */
    private void visitAnotherPassengerBehaviour(long period){
        addBehaviour(new TickerBehaviour(this, period) {
            @Override
            protected void onTick() {
                //if waiting time > 1h
                modulationPeriod += period;
                if(modulationPeriod > 24*60*60.0/TaskContext.DIVISION_COEF){
                    modulationPeriod = 0;
                    if(TaskContext.getAverageTaxiWaitingTime() > 60*60.0/TaskContext.DIVISION_COEF){
                        System.out.println("Drivers count increased!");
                        TaskContext.incrementDriversCount();
                    }
                }
//                if(PassengerStatus.AT_HOME.equals(status)){
//                    System.out.println("Visit another pessanger behaviour for agent " + getAID().getName());
                    //find passengers
                    ArrayList<AID> passengersAIDList = new ArrayList<>();
                    DFAgentDescription[] passengers = DFServiceHelper.findAgents(myAgent, null, TaskContext.PASSENGER_SERVICE_TYPE);
                    if(passengers != null){
                        for(int i = 0; i < passengers.length; i++){
                            //filter current agent from list
                            if(!myAgent.getAID().equals(passengers[i].getName())){
                                passengersAIDList.add(passengers[i].getName());
                            }
                        }
                        myAgent.addBehaviour(new PassengerVisitRequestBehaviour((PassengerAgent) myAgent, passengersAIDList));
                    }
                }
//            }
        });
    }

    private void handleInvitationsServer(){
        addBehaviour(new HandleInvitationsServer());
    }


    private class HandleInvitationsServer extends CyclicBehaviour {
        private Integer step = 0;
        private MessageTemplate mt;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    handleVisitProposal(mt);
                    break;
                case 1:
                    mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                            MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
                    handleVisitConfirmation(mt);
                    break;
                case 2:
                    //HOW MANY TIME TO BE AT THE VISIT ?
                    mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    handleVisitDurationInformation(mt);
                    break;
            }
        }

        private void handleVisitProposal(MessageTemplate mt) {
            ACLMessage invitationProposalMessage = myAgent.receive(mt);
            if(invitationProposalMessage != null){
                ACLMessage reply = invitationProposalMessage.createReply();
                if (PassengerStatus.AT_HOME.equals(status)) {
                    reply.setPerformative(ACLMessage.AGREE);
                    status = PassengerStatus.VISIT_PLANNED;
                    step = 1;
                    try {
                        reply.setContentObject(home);
                        myAgent.send(reply);
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                } else {
                    reply.setPerformative(ACLMessage.CANCEL);
                    step = 0;
                    myAgent.send(reply);
                }
            } else {
                //we would like to execute action only when a new message is received
                //When a new message is inserted in the agent’s message queue all blocked behaviours
                //becomes available for execution again so that they have a chance to process the received message.
                block();
            }
        }

        private void handleVisitConfirmation(MessageTemplate mt) {
            ACLMessage confirmationMessage = myAgent.receive(mt);
            if(confirmationMessage != null){
                if(confirmationMessage.getPerformative() == ACLMessage.CONFIRM){
                    step = 2;
                } else {
                    status = PassengerStatus.AT_HOME;
                    step = 0;
                }
            } else {
                block();
            }
        }

        private void handleVisitDurationInformation(MessageTemplate mt) {
            ACLMessage visitTimeInformationMessage = myAgent.receive(mt);
            if(visitTimeInformationMessage != null){
                try {
                    Long visitDurationTime = new Long(1000);
//                    Long visitDurationTime = (Long) visitTimeInformationMessage.getContentObject();
                    System.out.println("Average taxi waiting time in system: " + TaskContext.getAverageTaxiWaitingTime());
                    BehaviourUtils.sleep(visitDurationTime);
                } catch (Exception e) {
//                    e.printStackTrace();
                }
                status = PassengerStatus.AT_HOME;
                step = 0;
            } else {
                block();
            }
        }
    }

    public Location getLocation() {
        return home;
    }

    /**
     * Deregister aeroDriving service from yellow pages before terminating agent
     */
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e){
            e.printStackTrace();
        }
//        System.out.println("Passenger service was deregistered for agent " + getAID().getName());
    }
}
