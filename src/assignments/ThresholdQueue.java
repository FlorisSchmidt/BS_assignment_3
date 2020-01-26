/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assignments;

import java.util.LinkedList;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.list.ListOfStatProbes;

/**
 *
 * @author mctenthij
 */
public class ThresholdQueue {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int lower = 10;              // k-threshold for queue
        int upper = 20;             // K-threshold for queue
        double lambdaInv = 3.0;     // Arrival rate
        double muHighInv = 4.0;     // Average service time
        double muLowInv = 2.0;      // Average service time
        double maxTime = 10000;      // Simulation endtime (seconds)

        new ThresholdQueue(lambdaInv, muLowInv, muHighInv, maxTime, lower, upper).simulateOneRun();
        StatProbe test = new ThresholdQueue(lambdaInv, muLowInv, muHighInv, maxTime, lower, upper).getAverageCosts();
        System.out.println(test.average());
    }

    LinkedList<Customer> queue;
    Server server;

    ArrivalProcess arrivalProcess;
    StopEvent stopEvent;
    MRG32k3a serviceTimeRNG;

    int numServers = 1;
    int k;
    int K;
    double arrivalRate;
    double lowServiceRate;
    double highServiceRate;
    double stopTime;

    Tally serviceTimeTally;
    TallyStore waitTimeTally;
    ListOfStatProbes<StatProbe> stats;
    ListOfStatProbes<StatProbe> stats2;

    public ThresholdQueue(double arrivalRate, double avgServiceRate, double avgHighServiceRate, double stopTime, int k, int K) {
        this.arrivalRate = arrivalRate;
        this.lowServiceRate = avgServiceRate;
        this.highServiceRate = avgHighServiceRate;
        this.k = k;
        this.K = K;
        this.stopTime = stopTime;

        queue = new LinkedList<>();
        stats = new ListOfStatProbes<>("Stats for Accumulate");
        stats2 = new ListOfStatProbes<>("Stats for Tallies");

        for (int i = 0; i < numServers; i++) {
            String id = "server " + i;
            Accumulate utilization = new Accumulate(id);
            Accumulate runningCost = new Accumulate("cost server "+i);
            stats.add(utilization);
            stats.add(runningCost);
            server = new Server(utilization,runningCost);
        }

        //Create inter arrival time, and service time generators
        serviceTimeRNG = new MRG32k3a();
        arrivalProcess = new ArrivalProcess(new MRG32k3a(), arrivalRate);
        stopEvent = new StopEvent();

        //Create Tallies
        waitTimeTally = new TallyStore("Waittime");
        serviceTimeTally = new Tally("Servicetime");

        //Add Tallies in ListOfStatProbes for later reporting
        stats2.add(waitTimeTally);
        stats2.add(serviceTimeTally);
    }

    public ThresholdQueue(double arrivalRate, double avgServiceRate, double avgHighServiceRate, double stopTime, int k, int K, MRG32k3a arrival, MRG32k3a service) {

        // Build this initializer using the initializer above
        this.arrivalRate = arrivalRate;
        this.lowServiceRate = avgServiceRate;
        this.highServiceRate = avgHighServiceRate;
        this.k = k;
        this.K = K;
        this.stopTime = stopTime;

        queue = new LinkedList<>();
        stats = new ListOfStatProbes<>("Stats for Accumulate");
        stats2 = new ListOfStatProbes<>("Stats for Tallies");

        for (int i = 0; i < numServers; i++) {
            String id = "server " + i;
            Accumulate utilization = new Accumulate(id);
            Accumulate runningCost = new Accumulate("cost server "+i);
            stats.add(utilization);
            stats.add(runningCost);
            server = new Server(utilization,runningCost);
        }

        //Create inter arrival time, and service time generators
        serviceTimeRNG = service;
        arrivalProcess = new ArrivalProcess(arrival, arrivalRate);
        stopEvent = new StopEvent();

        //Create Tallies
        waitTimeTally = new TallyStore("Waittime");
        serviceTimeTally = new Tally("Servicetime");

        //Add Tallies in ListOfStatProbes for later reporting
        stats2.add(waitTimeTally);
        stats2.add(serviceTimeTally);
    }

    public void simulateOneRun() {
        Sim.init();
        waitTimeTally.init();
        serviceTimeTally.init();

        arrivalProcess.init();
        stopEvent.schedule(stopTime);
        Sim.start();
        System.out.println(stats.report());
        System.out.println(stats2.report());
    }

    public StatProbe getAverageCosts(){
        Sim.init();
        waitTimeTally.init();
        serviceTimeTally.init();

        arrivalProcess.init();
        stopEvent.schedule(stopTime);
        Sim.start();
        return stats.get(1);
    }

    void handleArrival() {
        Customer cust = new Customer();
        if (server.utilization.getLastValue() == 1.0) {
            queue.addLast(cust);
            updateRegime(server);
        } else {
            server.startService(cust);
        }
    }

    void updateRegime(Server server) {
        if (queue.size() + 1 <= k && server.inHighRegime == true) {
            server.setRegime(false);
        }
        if (queue.size() + 1 >= K && server.inHighRegime == false) {
            server.setRegime(true);
        }
    }

    void serviceCompleted(Server server, Customer cust) {
        cust.completed();
        waitTimeTally.add(cust.waitTime);
        serviceTimeTally.add(cust.serviceTime);
        if (!queue.isEmpty()) {
            Customer newCust = queue.removeFirst();
            server.startService(newCust);
        }
    }

    double drawExponentialValue(double x, double mu) {
        return -1*1/mu*Math.log(x);
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

    class Customer {

        private double arrivalTime;
        private double startTime;
        private double completionTime;
        private double waitTime;
        private double serviceTime;
        private double serviceRand;

        public Customer() {
            //Record arrival time when creating a new customer
            arrivalTime = Sim.time();
            startTime = Double.NaN;
            completionTime = Double.NaN;
            waitTime = Double.NaN;
            serviceTime = Double.NaN;
            serviceRand = serviceTimeRNG.nextDouble();
        }

        //Call this method when the service for this
        //customer started
        public void serviceStarted() {
            startTime = Sim.time();
            waitTime = startTime - arrivalTime;
        }

        //Call this method when the service for this
        //customer completed
        public void completed() {
            completionTime = Sim.time();
            serviceTime = completionTime - startTime;
        }
    }

    //This Event object represents a server
    class Server extends Event {
        static final double BUSY = 1.0;
        static final double IDLE = 0.0;
        static final double LOWCOST = 5.0;
        static final double HIGHCOST = 10.0;
        boolean inHighRegime = false;
        Accumulate utilization; //Record utilization
        Accumulate runningCost; //Record running costs
        Customer currentCust;   //Current customer in service

        public Server(Accumulate utilization, Accumulate runningCost) {
            this.utilization = utilization;
            this.runningCost = runningCost;
            utilization.init(IDLE);
            runningCost.init(IDLE);
            currentCust = null;
        }

        @Override
        public void actions() {
            utilization.update(IDLE);
            runningCost.update(IDLE);
            serviceCompleted(this, currentCust);
        }

        public void startService(Customer cust) {
            utilization.update(BUSY);
            currentCust = cust;
            cust.serviceStarted();

            double serviceTime;
            if (inHighRegime) {
                serviceTime = drawExponentialValue(cust.serviceRand,highServiceRate);
                runningCost.update(HIGHCOST);
            } else {
                serviceTime = drawExponentialValue(cust.serviceRand,lowServiceRate);
                runningCost.update(LOWCOST);
            }

            schedule(serviceTime);//Schedule this event
            //after serviceTime time units
        }

        public void setRegime(boolean toHigh) {
            if (toHigh) {
                inHighRegime = true;
                runningCost.update(HIGHCOST);
            } else {
                inHighRegime = false;
                runningCost.update(LOWCOST);
            }
        }
    }

    //Stop simulation by using this event
    class StopEvent extends Event {

        @Override
        public void actions() {
            Sim.stop();
        }
    }
}