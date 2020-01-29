package assignments;

import java.util.LinkedList;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;

/**
 *
 * @author mctenthij
 */
public class Region {
    LinkedList<Accident> queue;     //Queue of the server
    LinkedList<Ambulance> idleAmbulances;  // Available ambulance
    double [] baseLocation;
    ArrivalProcess arrivalProcess;
    RandomStream locationStream;
    int regionID;
    
    public Region(double baseXCoordinate, double baseYCoordinate, RandomStream rng, double arrivalRate, RandomStream location, int rid) {
        queue = new LinkedList<>();
        idleAmbulances = new LinkedList<>();
        baseLocation = new double[2];
        baseLocation[0] = baseXCoordinate;
        baseLocation[1] = baseYCoordinate;
        arrivalProcess = new ArrivalProcess(rng,arrivalRate);
        locationStream = location;
        regionID = rid;
    }
    
    public void handleArrival() {
        // process a new arrival
    }
    
    public double[] drawLocation() {
        // determine the location of the accident
        double[] location = new double[2];
        location[0] = 0.0; // X-Coordinate of accident location
        location[1] = 0.0; // Y-Coordinate of accident location
        return location;
    }
    
    class ArrivalProcess extends Event {
        ExponentialGen arrivalTimeGen;
        double arrivalRate;

        public ArrivalProcess(RandomStream rng, double arrivalRate) {
            this.arrivalRate = arrivalRate;
            arrivalTimeGen = new ExponentialGen(rng, arrivalRate);
        }
        
        @Override
        public void actions() {
            double nextArrival = arrivalTimeGen.nextDouble();
            schedule(nextArrival);//Schedule this event after
            //nextArrival time units
            handleArrival();
        }
        
        public void init() {
            double nextArrival = arrivalTimeGen.nextDouble();
            schedule(nextArrival);//Schedule this event after
            //nextArrival time units
        }
    }
    
}
