package assignments;

import java.util.Random;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.list.ListOfStatProbes;

/**
 *
 * @author mctenthij
 */
public class Hospital {
    Ambulance[] ambulances;
    Region[] regions;
    StopEvent stopEvent;
    
    int numAmbulances;    
    double[] arrivalRates;
    double serviceRate;
    double stopTime;
    int numRegions;

    Random rng = new Random();
    
    Tally serviceTimeTally;
    Tally waitTimeTally;
    Tally withinTargetTally;
    ListOfStatProbes<StatProbe> stats;
    
    public static void main(String[] args) {
        int C = 20;                  //#Ambulances
        int regions = 7;            //#Regions
        double[] lambdas = {1./15,1./15,1./15,1./15,1./15,1./15,1./15};          //Arrival rates per region
        double mu = 1.0;           //Service rate
        double maxTime = 1000000;    //Simulation endtime (minutes)

        new Hospital(C,lambdas,mu,maxTime,regions).start();
        System.out.println(new Hospital(C,lambdas,mu,maxTime,regions).serveAllRegions(C,lambdas,mu,maxTime,regions,true).report());
    }
    
    public void start() {
        ListOfStatProbes output = simulateEmergencies();
        System.out.println(output.report());
    }
    
    public int determineBaseRegion(int ambulanceNumber) {
        // This function can be altered to test different ambulance placements
            if(ambulanceNumber<2){
                return 0;
            }
            if(ambulanceNumber<5){
                return 1;
            }
            if (ambulanceNumber<8) {
                return 2;
            }
            if (ambulanceNumber<11){
                return 3;
            }
            if(ambulanceNumber<14){
                return 4;
            }
            if(ambulanceNumber<17){
                return 5;
            }
            return 6;
    }

    public double[] determineRegionLocation(int j) {
        // This function must be adjusted
        double[] location = new double[2];
        double hexagonRadius = 5;
        double apothem = Math.sqrt(Math.pow(hexagonRadius,2) - Math.pow(hexagonRadius/2,2));
        switch (j){
            case 0:
                location[0] = 0.0; // X-Coordinate of accident location
                location[1] = 0.0; // Y-Coordinate of accident location
                break;
            case 1:
                location[0] = 0.0;
                location[1] = apothem*2;
                break;
            case 2:
                location[0] = hexagonRadius*1.5;
                location[1] = apothem;
                break;
            case 3:
                location[0] = hexagonRadius*1.5;
                location[1] = -apothem;
                break;
            case 4:
                location[0] = 0.0;
                location[1] = -apothem*2;
                break;
            case 5:
                location[0] = -hexagonRadius*1.5;
                location[1] = -apothem;
                break;
            case 6:
                location[0] = -hexagonRadius*1.5;
                location[1] = apothem;
                break;
        }
        return location;
    }

    public Hospital(int numAmbulances, double[] arrivalRates, double serviceRate, double stopTime, int numRegions) {
        this.numAmbulances = numAmbulances;
        this.arrivalRates = arrivalRates;
        this.serviceRate = serviceRate;
        this.stopTime = stopTime;
        this.numRegions = numRegions;
        
        ambulances = new Ambulance[numAmbulances];
        regions = new Region[numRegions];
        stats = new ListOfStatProbes<>("Stats for Tallies");
        
        for (int j = 0; j < numRegions; j++) {
            double[] baseLocation = determineRegionLocation(j);
            RandomStream arrivalRandomStream = getStream();
            Region region = new Region(baseLocation[0], baseLocation[1], arrivalRandomStream, arrivalRates[j], getStream(), j);
            regions[j] = region;
        }
        
        for (int i = 0; i < numAmbulances; i++) {
            int region = determineBaseRegion(i);
            RandomStream serviceRandomStream = getStream();
            Ambulance ambulance = new Ambulance(regions[region], serviceRandomStream, serviceRate);
            ambulances[i] = ambulance;
            regions[region].idleAmbulances.add(ambulance);
        }
        
        //Create stopEvent
        stopEvent = new StopEvent();
        
        //Create Tallies
        waitTimeTally = new Tally("Waittime");
        serviceTimeTally = new Tally("Servicetime");
        withinTargetTally = new Tally("Arrival within target");
        //Add Tallies in ListOfStatProbes for later reporting
        stats.add(waitTimeTally);
        stats.add(serviceTimeTally);
        stats.add(withinTargetTally);
    }
    
    public Hospital(int numAmbulances, double[] arrivalRates, double serviceRate, double stopTime, int numRegions, boolean outside) {
        this.numAmbulances = numAmbulances;
        this.arrivalRates = arrivalRates;
        this.serviceRate = serviceRate;
        this.stopTime = stopTime;
        this.numRegions = numRegions;

        ambulances = new Ambulance[numAmbulances];
        regions = new Region[numRegions];
        stats = new ListOfStatProbes<>("Stats for Tallies");

        for (int j = 0; j < numRegions; j++) {
            double[] baseLocation = determineRegionLocation(j);
            RandomStream arrivalRandomStream = getStream();
            Region region = new Region(baseLocation[0], baseLocation[1], arrivalRandomStream, arrivalRates[j], getStream(), j);
            regions[j] = region;
        }

        for (int i = 0; i < numAmbulances; i++) {
            int region = determineBaseRegion(i);
            RandomStream serviceRandomStream = getStream();
            Ambulance ambulance = new Ambulance(region, serviceRandomStream, serviceRate, outside, regions);
            ambulances[i] = ambulance;
            regions[region].idleAmbulances.add(ambulance);
        }

        //Create stopEvent
        stopEvent = new StopEvent();

        //Create Tallies
        waitTimeTally = new Tally("Waittime");
        serviceTimeTally = new Tally("Servicetime");
        withinTargetTally = new Tally("Arrival within target");
        //Add Tallies in ListOfStatProbes for later reporting
        stats.add(waitTimeTally);
        stats.add(serviceTimeTally);
        stats.add(withinTargetTally);
    }
    
    /*
        DO NOT CHANGE FUNCTION FROM HERE!
    */
    
    public ListOfStatProbes simulateEmergencies() {
        Sim.init();
        waitTimeTally.init();
        serviceTimeTally.init();
        withinTargetTally.init();
        
        for (int j = 0; j < numRegions; j++) {
            regions[j].arrivalProcess.init();
        }
        stopEvent.schedule(stopTime);
        Sim.start();

        for (int k = 0; k < numAmbulances; k++) {
            for (double obs: ambulances[k].serviceTimeTally.getArray()) {
                serviceTimeTally.add(obs);
            }
            for (double obs: ambulances[k].waitTimeTally.getArray()) {
                waitTimeTally.add(obs);
            }
            for (double obs: ambulances[k].withinTargetTally.getArray()) {
                withinTargetTally.add(obs);
            }
        }
        
        return stats;
    }
    
    public ListOfStatProbes serveAllRegions(int numAmbulances, double[] arrivalRates, double serviceRate, double stopTime, int numRegions, boolean outside) {
        ListOfStatProbes output = new Hospital(numAmbulances, arrivalRates, serviceRate, stopTime, numRegions, outside).simulateEmergencies();
        return output;
        
    }
    
    public MRG32k3a getStream() {
        long[] seed = new long[6];
        for (int i =0;i<seed.length;i++) {
            seed[i] = (long) rng.nextInt();
        }
        MRG32k3a myrng = new MRG32k3a();
        myrng.setSeed(seed);
        return myrng;
    }
    
    //Stop simulation by using this event
    class StopEvent extends Event {

        @Override
        public void actions() {
            Sim.stop();
        }
    }
    
}
