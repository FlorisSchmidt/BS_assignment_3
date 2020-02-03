package assignments;

/**
 *
 * @author mctenthij
 */
public class Accident {
    private double arrivalTime;
    private double startTime;
    private double waitTime;
    private double serviceTime;
    private double xLocation;
    private double yLocation;

    public Accident(double arrivalTime, double xLocation, double yLocation) {
        //Record arrival time when creating a new customer
        this.arrivalTime = arrivalTime;
        this.startTime = Double.NaN;
        this.waitTime = Double.NaN;
        this.serviceTime = Double.NaN;
        this.xLocation = xLocation;
        this.yLocation = yLocation;
    }
    
    public double[] getLocation() {
        double[] result = new double[2];
        result[0] = xLocation;
        result[1] = yLocation;
        return result;
    }
    
    public double getServiceTime() {
        return serviceTime;
    }
    
    public double getWaitTime() {
        return waitTime;
    }

    //Call this method when the service for this
    //customer started
    public void serviceStarted(double current) {
        this.startTime = current;
        this.waitTime = current - this.arrivalTime;
    }

    //Call this method when the service for this
    //customer completed
    public void completed(double current) {
        this.serviceTime = current - this.startTime;
    }
    
}
