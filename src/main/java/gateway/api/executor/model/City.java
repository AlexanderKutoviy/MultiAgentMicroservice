package gateway.api.executor.model;

public class City {
    private int x;
    private int y;

    public City(int x, int y) {
        this.x = x;
        this.y = y;
    }


    public int getXSize() {
        return x;
    }

    public int getYSize() {
        return y;
    }
}
