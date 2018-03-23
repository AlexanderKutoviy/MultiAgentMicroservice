package gateway.api.executor.behaviours;

import gateway.api.executor.agents.PassengerAgent;
import gateway.api.executor.helpers.DFServiceHelper;
import gateway.api.executor.model.Location;
import gateway.api.executor.model.PassengerStatus;
import gateway.api.executor.model.TaskContext;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.*;

public class PassengerVisitRequestBehaviour extends Behaviour {
    private int step = 0;
    private ArrayList<AID> passengers;
    private PassengerAgent sender;
    private int repliesCount;
    private MessageTemplate mt; // The template to receive replies
    private List<AID> passengersAcceptedInvitation = new ArrayList<>();
    private Map<AID, Location> locationsMap = new HashMap<>();
    private final String CONVERSATION_ID = "visitProposal";



    public PassengerVisitRequestBehaviour(PassengerAgent sender, ArrayList<AID> passengers){
        this.passengers = passengers;
        this.sender = sender;
    }

    public void action() {
        switch (step){
            case 0:
                proposeVisit();
                break;
            case 1:
                handleAgreementsFromAnotherPassenger();
                break;
            case 2:
                sendConfirmationMessageToFirstPassenger();
                break;
            case 3:
                long timeToWait1 = getAeroDriver(locationsMap.get(passengersAcceptedInvitation.get(0)));
                TaskContext.addDriverWaitingTime(timeToWait1);
                //wait for a driver
                BehaviourUtils.sleep(timeToWait1);
                step = 4;
                break;
            case 4:
                sendVisitDurationInformationMessage();
                step = 5;
                break;
            case 5:
                //get taxi to home
                long timeToWait2 = getAeroDriver(sender.getLocation());
                TaskContext.addDriverWaitingTime(timeToWait2);
                BehaviourUtils.sleep(timeToWait2);
                sender.setStatus(PassengerStatus.AT_HOME);
                step = 0;
                break;
        }
    }

    private void sendVisitDurationInformationMessage() {
        ACLMessage visitDurationInfoMessage = new ACLMessage(ACLMessage.INFORM);
        visitDurationInfoMessage.addReceiver(passengersAcceptedInvitation.get(0));
        visitDurationInfoMessage.setConversationId(CONVERSATION_ID);
        try {
            visitDurationInfoMessage.setContentObject(new Long(TaskContext.getVisitDurationTime()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        visitDurationInfoMessage.setReplyWith("info" + System.currentTimeMillis());
        sender.send(visitDurationInfoMessage);
    }

    private void proposeVisit(){
        //proposal message (try to invite any other passenger)
        ACLMessage proposalMessage = new ACLMessage(ACLMessage.PROPOSE);
        //send invitation proposal for every passenger
        for(int i = 0; i < passengers.size(); i++){
            proposalMessage.addReceiver(passengers.get(i));
        }
        proposalMessage.setConversationId(CONVERSATION_ID);
        proposalMessage.setReplyWith("propose" + System.currentTimeMillis());
        sender.send(proposalMessage);

        // Prepare the template to get proposals
        mt = MessageTemplate.and(MessageTemplate.MatchConversationId(CONVERSATION_ID),
                MessageTemplate.MatchInReplyTo(proposalMessage.getReplyWith()));
        step = 1;
        repliesCount = 0;
    }

    private void handleAgreementsFromAnotherPassenger() {
        // Receive all agreements from passengers
        ACLMessage reply = sender.receive(mt);
        if(reply != null){
            repliesCount ++;
            if(reply.getPerformative() == ACLMessage.AGREE){
                passengersAcceptedInvitation.add(reply.getSender());
                try {
                    locationsMap.put(reply.getSender(), (Location) reply.getContentObject());
                } catch (UnreadableException e) {
//                    e.printStackTrace();
                }
            }
            if(passengers.size() == repliesCount){
                //we received all replies
                step = 2;
            }
        } else {
            block();
        }
    }

    private void sendConfirmationMessageToFirstPassenger() {
        if(!passengersAcceptedInvitation.isEmpty()){
            sender.setStatus(PassengerStatus.VISIT_PLANNED);
            //Send confirmation message to any of the accepted passengers
            ACLMessage acceptanceMessage = new ACLMessage(ACLMessage.CONFIRM);
            acceptanceMessage.addReceiver(passengersAcceptedInvitation.get(0));
            acceptanceMessage.setConversationId(CONVERSATION_ID);
            acceptanceMessage.setReplyWith("confirm" + System.currentTimeMillis());
            sender.send(acceptanceMessage);

            //send cancel message for another passengers
            ACLMessage cancellationMessage =  new ACLMessage(ACLMessage.CANCEL);
            for(int i = 1; i < passengersAcceptedInvitation.size(); i ++){
                cancellationMessage.addReceiver(passengersAcceptedInvitation.get(i));
            }
            cancellationMessage.setConversationId(CONVERSATION_ID);
            cancellationMessage.setReplyWith("cancel" + System.currentTimeMillis());
            sender.send(cancellationMessage);

            step = 3;
        } else {
            step = 0;
            sender.setStatus(PassengerStatus.AT_HOME);
        }
    }

    private long getAeroDriver(Location locationTo){
        long startWaitingTime = System.currentTimeMillis();
        int findDriverStepNumber = 0;
        final String GET_DRIVER_CONVERSATION_ID = "callDriverId";
        DFAgentDescription[] aeroDrivers = null;
        Map.Entry<Double, AID> closestDriver = null;
        Map<AID, Location> acceptedTaxiDrivers2Locations = new HashMap<>();
        boolean isDriverFound = false;
        while(!isDriverFound) {
            switch (findDriverStepNumber) {
                case 0:
                    aeroDrivers = DFServiceHelper.findAgents(myAgent, null, TaskContext.DRIVER_SERVICE_TYPE);
                    if(aeroDrivers != null){
                        //get free drivers with theirs location
                        ACLMessage proposalMessage = new ACLMessage(ACLMessage.PROPOSE);
                        proposalMessage.setConversationId(GET_DRIVER_CONVERSATION_ID);
                        proposalMessage.setReplyWith("aeroDriverPropose" + System.currentTimeMillis());
                        for (DFAgentDescription aeroDriver : aeroDrivers) {
                            proposalMessage.addReceiver(aeroDriver.getName());
                        }
                        myAgent.send(proposalMessage);
                        // Prepare the template to get proposals
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId(GET_DRIVER_CONVERSATION_ID),
                                MessageTemplate.MatchInReplyTo(proposalMessage.getReplyWith()));
                        findDriverStepNumber = 1;
                        break;
                    }
                case 1:
                    // Receive all answers from drivers
                    ACLMessage reply = sender.blockingReceive(mt, 3000);
                    if(reply != null) {
                        if (ACLMessage.AGREE == reply.getPerformative()) {
                            try {
                                Location taxiLocation = (Location) reply.getContentObject();
                                acceptedTaxiDrivers2Locations.put(reply.getSender(), taxiLocation);
                            } catch (UnreadableException e) {
//                                e.printStackTrace();
                            }
                        } else if (ACLMessage.CANCEL == reply.getPerformative()) { /*currently do nothing*/}
                    }
                    //find closest driver
                    closestDriver = chooseClosestAeroDriver(acceptedTaxiDrivers2Locations, locationTo);
                    if(closestDriver != null){
                        //send cancellation message to another drivers
                        for (int i = 0; i < aeroDrivers.length; i++) {
                            if (!aeroDrivers[i].getName().equals(closestDriver.getValue())) {
                                ACLMessage cancellationMessage = new ACLMessage(ACLMessage.CANCEL);
                                cancellationMessage.setReplyWith("driverCancel" + System.currentTimeMillis());
                                cancellationMessage.setConversationId(GET_DRIVER_CONVERSATION_ID);
                                cancellationMessage.addReceiver(aeroDrivers[i].getName());
                                myAgent.send(cancellationMessage);
                            }
                        }

                        //send confirmation message to the closest driver
                        ACLMessage confirmationMessage = new ACLMessage(ACLMessage.CONFIRM);
                        confirmationMessage.setReplyWith("driverConfirm" + System.currentTimeMillis());
                        confirmationMessage.setConversationId(GET_DRIVER_CONVERSATION_ID);
                        confirmationMessage.addReceiver(closestDriver.getValue());

                        isDriverFound = true;
                        long endWaitingTime = System.currentTimeMillis();

                        //taxi busy time for getting the target
                        try {
                            confirmationMessage.setContentObject(new Long((long) (closestDriver.getKey().doubleValue() / (17*60))));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        myAgent.send(confirmationMessage);
                        //60km/h = 16.7m/s
                        return (long) (closestDriver.getKey().doubleValue() / (17*60)) + (endWaitingTime - startWaitingTime)/1000;
                    }
            }
        }
        return 0;
    }

    private Map.Entry<Double, AID> chooseClosestAeroDriver(Map<AID, Location> acceptedTaxiDrivers2Locations, Location locationTo){
        if(!acceptedTaxiDrivers2Locations.isEmpty()){
            Map<Double, AID> distance2Driver = new TreeMap<>(Comparator.reverseOrder());
            for (AID driver : acceptedTaxiDrivers2Locations.keySet()) {
                Location driverLocation = acceptedTaxiDrivers2Locations.get(driver);
                double distance = Math.sqrt(Math.pow(locationTo.getX() - driverLocation.getX(), 2) +
                        Math.pow(locationTo.getY() - driverLocation.getY(), 2));
                distance2Driver.put(distance, driver);
            }
            return distance2Driver.entrySet().iterator().next();
        }
        return null;
    }

    public boolean done() {
        return step == 5;
    }
}
