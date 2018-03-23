package gateway.api.executor.model;

import gateway.api.executor.agents.AeroDriverAgent;
import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class TaskContext {
    public static AtomicLong iterationNumber = new AtomicLong(1);
    private static ArrayList<Long> driverWaitingTimeArray = new ArrayList<>();


    private static AgentContainer mainContainer;

    private static ArrayList<Agent> passengerAgents;

    private static ArrayList<Location> alreadyUsedLocations = new ArrayList<>();

    private static final City zhitomyrCity = new City(9000, 7200);

    private static int driversCount = 15;
//    private static int driversCount = 2;
    public static final String DRIVER_SERVICE_TYPE = "aeroDriving";
    public static final String DRIVER_SERVICE_NAME = "Aerodriver";


    public static final int PASSANGERS_COUNT = 280;
//    public static final int PASSANGERS_COUNT = 3;
    public static final String PASSENGER_SERVICE_TYPE = "passengerVisiting";
    public static final String PASSENGER_SERVICE_NAME = "Passenger";

    public static final long DIVISION_COEF = 60;

    //divided by 1000
    private static final long VISIT_PERIOD_MIN = 60*60/DIVISION_COEF;
    private static final long VISIT_PERIOD_MAX = 6*60*60/DIVISION_COEF;

    //divided by 1000 (from 30 min to 30h)
    private static final long VISIT_DURATION_MIN = 30*60/DIVISION_COEF;
    private static final long VISIT_DURATION_MAX = 3*60*60/DIVISION_COEF;


    public static City getCity(){
        return zhitomyrCity;
    }

    public static Location generateRandomFreeLocation(){
        Random rand = new Random();
        Location randomFreeLocation;
        do {
            int xRand = rand.nextInt(getCity().getXSize());
            int yRand = rand.nextInt(getCity().getYSize());
            randomFreeLocation = new Location(xRand, yRand);
        } while (alreadyUsedLocations.contains(randomFreeLocation));
        addLocationToAlreadyUsed(randomFreeLocation);
        return randomFreeLocation;
    }

    private synchronized static void addLocationToAlreadyUsed(Location usedLocation){
        alreadyUsedLocations.add(usedLocation);
    }


    public static long getVisitEveryPeriodInMillis(){
        return VISIT_PERIOD_MIN + (long) (Math.random()*
                (VISIT_PERIOD_MAX - VISIT_PERIOD_MIN));
    }

    public static long getVisitDurationTime(){
        return VISIT_DURATION_MIN + (long) (Math.random()*
                (VISIT_DURATION_MAX - VISIT_DURATION_MIN));
    }

    public static synchronized void incrementDriversCount(){
        Agent aeroDriverAgent = new AeroDriverAgent();
        AgentController aeroDriverAgentController = null;
        try {
            aeroDriverAgentController = mainContainer.acceptNewAgent("aeroDriverAgent" + driversCount, aeroDriverAgent);
            driversCount ++;
            aeroDriverAgentController.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getDriversCount() {
        return driversCount;
    }

    public static void setMainContainer(AgentContainer mainContainer) {
        TaskContext.mainContainer = mainContainer;
    }

    public static void setPassengerAgents(ArrayList<Agent> passengerAgents) {
        TaskContext.passengerAgents = passengerAgents;
    }


    public static double getAverageTaxiWaitingTime(){
        iterationNumber.incrementAndGet();
        long waitingTime = 0;
        if(!driverWaitingTimeArray.isEmpty()){
//            System.out.println("recordsCount: " + driverWaitingTimeArray.size());
            for (Long time : driverWaitingTimeArray) {
                waitingTime += time;
            }
        }
        return driverWaitingTimeArray.size() != 0 ? waitingTime/driverWaitingTimeArray.size() : 0;
    }

    public static void addDriverWaitingTime(Long driverWaitingTime){
//        System.out.println(driverWaitingTime);
        driverWaitingTimeArray.add(driverWaitingTime);
    }

    public static ArrayList<Agent> getPassengerAgents() {
        return passengerAgents;
    }
}
