package assignments;

import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.TallyStore;
/**
 *
 * @author mctenthij
 */
public class Ambulance extends Event {
    Region baseRegion;
    Accident currentCust; //Current customer in service
    double responseTime = 15.0;
    boolean serveOutsideRegion;
    ExponentialGen serviceTimeGen;
    TallyStore waitTimeTally = new TallyStore("Waittime");
    TallyStore serviceTimeTally = new TallyStore("Servicetime");
    TallyStore withinTargetTally = new TallyStore("Arrival within target");
    
    public Ambulance(Region baseRegion, RandomStream rng, double serviceTimeRate) {
        currentCust = null;
        this.baseRegion = baseRegion;
        serviceTimeGen = new ExponentialGen(rng, serviceTimeRate);
        serveOutsideRegion = false;
    }
    
    public Ambulance(Region baseRegion, RandomStream rng, double serviceTimeRate, boolean outside) {
        currentCust = null;
        this.baseRegion = baseRegion;
        serviceTimeGen = new ExponentialGen(rng, serviceTimeRate);
        serveOutsideRegion = outside;
    }
    
    public void serviceCompleted(Ambulance amb, Accident currentCust) {
        currentCust.completed(Sim.time());
        serviceTimeTally.add(currentCust.getServiceTime());
        waitTimeTally.add(currentCust.getWaitTime());
        if (currentCust.getWaitTime() < responseTime) {
            withinTargetTally.add(1);
        }
        else {
            withinTargetTally.add(0);
        }
        if(!amb.baseRegion.queue.isEmpty()) {
            Accident cust = amb.baseRegion.queue.removeFirst();
            cust.serviceStarted(Sim.time());
        }
        else {
            amb.baseRegion.idleAmbulances.add(amb);
        }
    }

    public double drivingTimeToAccident(Accident cust) {
        double [] location = cust.getLocation();
        return Math.sqrt(Math.pow(location[0] - baseRegion.baseLocation[0],2)+Math.pow(location[1] - baseRegion.baseLocation[1],2));
    }

    public double drivingTimeToHostital(Accident cust) {
        // calculate the driving time from accident location to the hospital
        double[] location = cust.getLocation();
        return Math.sqrt(Math.pow(location[0],2)+Math.pow(location[1],2));
    }

    @Override
    public void actions() {
        serviceCompleted(this, currentCust);
    }

    public void startService(Accident cust, double current) {
        currentCust = cust;
        double time_to_accident = drivingTimeToAccident(cust);
        cust.serviceStarted(current + time_to_accident);
        double serviceTime = serviceTimeGen.nextDouble();
        double busyServing; // Calculate the time needed to process the accident
        double returnToBaseTime = Math.sqrt(Math.pow(baseRegion.baseLocation[0],2)+Math.pow(baseRegion.baseLocation[1],2));
        busyServing = serviceTime + drivingTimeToHostital(cust) +  + returnToBaseTime;
        schedule(busyServing); //Schedule this event after serviceTime time units
    }
}