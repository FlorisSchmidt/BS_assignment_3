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
        // Process the completed `customer'
    }

    public double drivingTimeToAccident(Accident cust) {
        // calculate the driving time from the baselocation of the ambulance to the accident location
        return 0.0;
    }
    
    public double drivingTimeToHostital(Accident cust) {
        // calculate the driving time from accident location to the hospital
        return 0.0;
    }

    @Override
    public void actions() {
        serviceCompleted(this, currentCust);
    }

    public void startService(Accident cust, double current) {
        currentCust = cust;
        cust.serviceStarted(current);
        
        double serviceTime = serviceTimeGen.nextDouble();
        double busyServing = 0.0; // Calculate the time needed to process the accident
        
        schedule(busyServing); //Schedule this event after serviceTime time units
    }
}