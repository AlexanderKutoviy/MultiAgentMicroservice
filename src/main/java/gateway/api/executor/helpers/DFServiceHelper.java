package gateway.api.executor.helpers;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class DFServiceHelper {

    /**
     * Register agent in the yellow pages
     */
    public static void registerAgentInYellowPages(Agent agent, String serviceName, String serviceType){
        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.setName(agent.getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(serviceType);
        serviceDescription.setName(serviceName);
        agentDescription.addServices(serviceDescription);
        try {
            DFService.register(agent, agentDescription);
        } catch (FIPAException e){
            e.printStackTrace();
        }
    }

    public static DFAgentDescription[] findAgents(Agent searchPerformer, String serviceName, String serviceType){
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        if(serviceName != null){
            serviceDescription.setName(serviceName);
        }
        if(serviceType != null) {
            serviceDescription.setType(serviceType);
        }
        template.addServices(serviceDescription);
        try {
            return DFService.searchUntilFound(searchPerformer, searchPerformer.getDefaultDF(),template, null, 60000);
        } catch (FIPAException e){
            e.printStackTrace();
        }
        return null;
    }
}
