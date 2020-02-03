package assignments;

import java.util.LinkedList;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;

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
        double[] accidentLocation = drawLocation();
        Accident accident = new Accident(Sim.time(),accidentLocation[0],accidentLocation[1]);
        if(queue.isEmpty() && !idleAmbulances.isEmpty()){
            Ambulance amb = idleAmbulances.removeFirst();
            amb.startService(accident,Sim.time());
        }
        else {queue.add(accident);}
    }
    
    public double[] drawLocation() {
        // determine the location of the accident
        double hexagonRadius = 5;
        double apothem = Math.sqrt(Math.pow(hexagonRadius,2) - Math.pow(hexagonRadius/2,2));
        double slope = apothem/(0.5 * 2.5);
        double x;
        double y;
        while(true) {
            // generate random x between -radius and +radius
            x = (locationStream.nextDouble() * 2 * hexagonRadius) - hexagonRadius;
            // generate random y between -apothem and +apothem
            y = (locationStream.nextDouble() * 2 * apothem) - apothem;
            // check whether random point falls within hexagon
            if (-0.5 * hexagonRadius < x && x < 0.5 * hexagonRadius) {
                break;
            }
            if (x <= -0.5 * hexagonRadius && y < Math.sqrt(3) * x + 5 *slope && y > -Math.sqrt(3) * x - 5 *slope) {
                break;
            }
            if (x >= 0.5 * hexagonRadius && y < -Math.sqrt(3) * x + 5 * slope && y > Math.sqrt(3) * x - 5 *slope) {
                break;
            }
        }
        // transpose to right location
        x += baseLocation[0];
        y += baseLocation[1];
        double[] location = new double[2];
        location[0] = x; // X-Coordinate of accident location
        location[1] = y; // Y-Coordinate of accident location
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
