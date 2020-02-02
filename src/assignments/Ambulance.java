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
    Region[] regions;
    Accident currentCust; //Current customer in service
    double responseTime = 15;
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
    
    public Ambulance(int region, RandomStream rng, double serviceTimeRate, boolean outside, Region[] regions) {
        currentCust = null;
        this.baseRegion = regions[region];
        this.regions = regions;
        serviceTimeGen = new ExponentialGen(rng, serviceTimeRate);
        serveOutsideRegion = outside;
    }

    public void serviceCompleted(Ambulance amb, Accident currentCust) {
        double returnToBaseTime = Math.sqrt(Math.pow(baseRegion.baseLocation[0],2)+Math.pow(baseRegion.baseLocation[1],2));
        currentCust.completed(Sim.time() - returnToBaseTime);
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
            amb.startService(cust, Sim.time());
        } else if (serveOutsideRegion) {
            //Allowed to serve outside of regions
            Accident cust = getOutsideAccident(amb);
            if(cust!=null){
                amb.startService(cust,Sim.time());
            } else {
                amb.baseRegion.idleAmbulances.add(amb);
            }
        } else {
            amb.baseRegion.idleAmbulances.add(amb);
        }
    }

    private Accident getOutsideAccident(Ambulance amb){
        int regionID = amb.baseRegion.regionID;
        // Check center region
        if(!regions[0].queue.isEmpty()){
            return regions[0].queue.removeFirst();
        }
        //Check outside regions from closes to longest distance away
        int MAX_TIME = 12; // Possible extra constraint
        for(int i = 1;i<4;i++){
            //Prevent going to region above 6 (doesn't exist
            if(regionID+i>6){
                if(!regions[regionID+i-6].queue.isEmpty()){
                    Accident cust = regions[regionID+i-6].queue.getFirst();
                    if(drivingTimeToAccident(cust)<MAX_TIME){
                        return regions[regionID+i-6].queue.removeFirst();
                    }
                }
                // Try going to region +1
            } else if(!regions[regionID+i].queue.isEmpty()){
                Accident cust = regions[regionID+i].queue.getFirst();
                if(drivingTimeToAccident(cust)<MAX_TIME){
                    return regions[regionID+i].queue.removeFirst();
                }
            }
            //Prevent going to region below 0 (doesn't exist
            if(regionID-i<1){
                if(!regions[regionID-i+6].queue.isEmpty()){
                    Accident cust = regions[regionID-i+6].queue.getFirst();
                    if(drivingTimeToAccident(cust)<MAX_TIME){
                        return regions[regionID-i+6].queue.removeFirst();
                    }
                }
                // Try going to region -1
            } else if(!regions[regionID-i].queue.isEmpty()){
                Accident cust = regions[regionID-i].queue.getFirst();
                if(drivingTimeToAccident(cust)<MAX_TIME){
                    return regions[regionID-i].queue.removeFirst();
                }
            }
        }
        return null;
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
        busyServing = serviceTime + drivingTimeToHostital(cust) + returnToBaseTime + time_to_accident;
        schedule(busyServing); //Schedule this event after serviceTime time units
    }
}