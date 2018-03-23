package gateway.api.executor.agents;

import gateway.api.executor.behaviours.BehaviourUtils;
import gateway.api.executor.helpers.DFServiceHelper;
import gateway.api.executor.model.AeroDriverStatus;
import gateway.api.executor.model.Location;
import gateway.api.executor.model.TaskContext;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;

import static jade.lang.acl.MessageTemplate.MatchPerformative;

public class AeroDriverAgent extends Agent {
    private AeroDriverStatus status;
    private Location driverLocation;
    private MessageTemplate mt;
    private Integer driverStepNumber = 0;


    @Override
    protected void setup(){
        DFServiceHelper.registerAgentInYellowPages(this, TaskContext.DRIVER_SERVICE_NAME, TaskContext.DRIVER_SERVICE_TYPE);
        status = AeroDriverStatus.FREE;
        driverLocation = TaskContext.generateRandomFreeLocation();
        //AID is agent identifier, consist of <nickname>@<platform-name>
//        System.out.println("AeroDriver agent with ID: " + getAID().getName()  + " set up!");
        handlePassengerCalls();
    }


    private void handlePassengerCalls(){
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                switch (driverStepNumber){
                    case 0:
                        if(AeroDriverStatus.FREE.equals(status)){
                            handleProposal();
                        } else {
                            rejectProposal();
                        }
                        break;
                    case 1:
                        handleConfirmationFromPassenger();
                        break;


                }
            }

            private void handleProposal(){
                mt = MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage proposalMessage = myAgent.receive(mt);
                if(proposalMessage != null){
                    status = AeroDriverStatus.BUSY;
                    ACLMessage reply = proposalMessage.createReply();
                    try {
                        reply.setContentObject(driverLocation);
                        reply.setPerformative(ACLMessage.AGREE);
                        driverStepNumber = 1;
                        myAgent.send(reply);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }

            private void rejectProposal(){
                mt = MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage proposalMessage = myAgent.receive(mt);
                if(proposalMessage != null){
                    ACLMessage reply = proposalMessage.createReply();
                    reply.setPerformative(ACLMessage.CANCEL);
                    myAgent.send(reply);
                    driverStepNumber = 0;
                } else {
                    block();
                }
            }

            private void handleConfirmationFromPassenger(){
                mt = MessageTemplate.or(MatchPerformative(ACLMessage.CONFIRM), MatchPerformative(ACLMessage.CANCEL));
                ACLMessage confirmationMessage = myAgent.receive(mt);
                if(confirmationMessage != null){
                    int performative = confirmationMessage.getPerformative();
                    if(performative == ACLMessage.CANCEL){
                        status = AeroDriverStatus.FREE;
                    } else {
                        //taxi busy time for getting the target
                        try {
                            Long timeToRide = (Long) confirmationMessage.getContentObject();
                            System.out.println("time to ride: " + timeToRide);
                            BehaviourUtils.sleep(timeToRide*1000);
                            status = AeroDriverStatus.FREE;
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    }
                    driverStepNumber = 0;
                } else {
                    block();
                }
            }
        });
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
//        System.out.println("Aero driving service was deregistered for agent " + getAID().getName());
    }
}
