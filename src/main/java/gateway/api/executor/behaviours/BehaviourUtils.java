package gateway.api.executor.behaviours;

public class BehaviourUtils {

    public static void sleep(long timeInMillis){
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
