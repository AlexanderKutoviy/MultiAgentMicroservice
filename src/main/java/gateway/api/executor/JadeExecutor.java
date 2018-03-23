package gateway.api.executor;

import gateway.api.executor.agents.AeroDriverAgent;
import gateway.api.executor.agents.PassengerAgent;
import gateway.api.executor.model.TaskContext;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;

public class JadeExecutor {

    public static void execute() throws StaleProxyException {
        ArrayList<Agent> passengerAgents = new ArrayList<>();
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = runtime.createMainContainer(profile);
        TaskContext.setMainContainer(mainContainer);

        //run gui
//        AgentController rma = mainContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
//        rma.start();

        //run all passenger agents
        for(int i = 0; i < TaskContext.PASSANGERS_COUNT; i++){
            Agent passengerAgent = new PassengerAgent();
            passengerAgents.add(passengerAgent);
            AgentController passengerAgentController = mainContainer.acceptNewAgent("passengerAgent" + i, passengerAgent);
            passengerAgentController.start();
        }

        TaskContext.setPassengerAgents(passengerAgents);

        //run all aeroDriver agents
        for(int i = 0; i < TaskContext.getDriversCount(); i++){
            Agent aeroDriverAgent = new AeroDriverAgent();
            AgentController aeroDriverAgentController = mainContainer.acceptNewAgent("aeroDriverAgent" + i, aeroDriverAgent);
            aeroDriverAgentController.start();
        }
    }

    public static void main(String [] args) throws StaleProxyException {
        execute();
    }
}