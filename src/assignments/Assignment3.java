/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assignments;

import assignments.ThresholdQueue;

import java.util.HashSet;
import java.util.Random;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.TallyStore;

/**
 *
 * @author mctenthij
 */
public class Assignment3 {

    class State {
        int xval;
        int yval;
        TallyStore values;

        public State(int x,int y) {
            this.xval = x;
            this.yval = y;
            this.values = new TallyStore("("+x+","+y+")");
            this.values.init();

        }
    }

    State[] outputs;
    int numStates;
    int budget;
    int xmin;
    int ymin;
    int xmax;
    int ymax;

    int k;
    int K;
    double arrivalRate;
    double avgService;
    double avgHighService;
    double maxTime;

    Random rng = new Random();

    static final double BIGM = 9999999999999.99;

    public Assignment3(int xmin, int xmax, int ymin, int ymax, int budget, double arrivalRate, double avgServiceRate, double avgHighServiceRate, double stopTime, int k, int K) {
        int xrange = xmax-xmin+1;
        int yrange = ymax-ymin+1;
        numStates = xrange*yrange;
        outputs = new State[numStates];

        for (int i = 0; i < xrange; i++) {
            for (int j = 0; j < yrange; j++) {
                State state = new State(i+xmin,j+ymin);
                outputs[(yrange)*i+j] = state;
            }
        }

        this.budget = budget;
        this.ymin=ymin;
        this.xmin=xmin;
        this.ymax=ymax;
        this.xmax=xmax;

        this.k = k;
        this.K = K;
        this.arrivalRate = arrivalRate;
        this.avgService = avgServiceRate;
        this.avgHighService = avgHighServiceRate;
        this.maxTime = stopTime;
    }

    public int calcPos(int x, int y) {
        return (x-xmin)*(ymax-ymin+1)+y-ymin;
    }

    public State selectOptimalState() {
        double minimum = BIGM;
        State min = null;
        for (int i = 0; i < numStates; i++) {
            if (outputs[i].values.numberObs() > 0) {
                if (outputs[i].values.average() < minimum) {
                    minimum = outputs[i].values.average();
                    min = outputs[i];
                }
            }
        }
        return min;
    }

    public State getState(int[] val) {
        int pos = calcPos(val[0],val[1]);
        return outputs[pos];
    }


    public MRG32k3a getStream() {
        long[] seed = new long[6];
        long[] m1seeds;
        long[] m2seeds;
        //Fill the long[] with random seeds
        do {
            m1seeds = rng.longs(3,0,4294967087L).toArray();
        } while(m1seeds[0]==0 && m1seeds[1]==0 && m1seeds[2]==0);
        do {
            m2seeds = rng.longs(3,0,4294944443L).toArray();
        } while(m2seeds[0]==0 && m2seeds[1]==0 && m2seeds[2]==0);

        for(int i = 0;i<6;i++){
            if(i<3){
                seed[i] = m1seeds[i];
            } else {
                seed[i] = m2seeds[i-3];
            }
        }
        MRG32k3a myrng = new MRG32k3a();
        myrng.setSeed(seed);
        return myrng;
    }


    public void runSingleRun(int lower, int upper) {
        MRG32k3a arrival = getStream();
        MRG32k3a service = getStream();

        Sim.init();
        ThresholdQueue model = new ThresholdQueue(arrivalRate, avgService, avgHighService, maxTime, lower, upper, arrival, service);
        double result = model.getAverageCosts().average();
        int i = calcPos(lower,upper);
        outputs[i].values.add(result);
    }

    public State runRankingSelection(int initialRuns, double alpha) {
    // Perform initial runs
        for(int i = 0;i<numStates;i++) {
            for (int j = 0; j < (initialRuns/numStates); j++) {
                runSingleRun(outputs[i].xval, outputs[i].yval);
            }
        }
        HashSet<State> I = selectCandidateSolutions(alpha);

        double remaining_runs = budget - initialRuns;
        for(int i = 0;i<numStates;i++) {
            if(I.contains(outputs[i])) {
                for (int j = 0; j < remaining_runs/I.size(); j++) {
                    runSingleRun(outputs[i].xval, outputs[i].yval);
                }
            }
        }
        State opt = selectOptimalState();
        return opt;
    }

    public HashSet<State> selectCandidateSolutions(double alpha) {
        HashSet<State> I = new HashSet();
        double significanceCorrection  = new NormalDist().inverseF(Math.pow(1 - alpha, 1.0 / (numStates - 1)));
        boolean add = true;
        for(int i = 0; i < numStates; i++) {
            for(int j = 0; j < numStates; j++) {
                if(i != j && outputs[i].values.average() > outputs[j].values.average() +
                        significanceCorrection * (Math.sqrt(outputs[i].values.variance()
                                + outputs[j].values.variance())/Math.sqrt(numStates))) {
                    add = false;
                }
            }
            if (add) {I.add(outputs[i]);}
            add = true;
        }
        //Find all candidate solutions for the ranking and selection method
        return I;
    }


    public State runLocalSearch() {

        //Perform Local Search
        State currentState = selectRandomStart();
        State neighbour;

        int m = budget;
        while(m > 0){
            neighbour = selectRandomNeighbor(currentState);
            runSingleRun(currentState.xval, currentState.yval);
            runSingleRun(neighbour.xval, neighbour.yval);
            currentState = selectBestState(currentState,neighbour);
            m -= 2;
        }
        return selectOptimalState();
    }

    public State selectBestState(State current, State neighbor){

        if(current.values.average() < neighbor.values.average()) {
            return current;
        }
        return neighbor;
    }

    public State selectRandomStart() {
        //Select a random state
        State state = outputs[rng.nextInt(numStates)];
        return state;
    }

    public State selectRandomNeighbor(State state) {
        //Select a random neighbor
        int new_x = state.xval;;
        int new_y = state.yval;
        while (true) {
            int neighbour = rng.nextInt(4);
            switch (neighbour) {
                case 0:
                    new_x += 1;
                    break;
                case 1:
                    new_x -= 1;
                    break;
                case 2:
                    new_y += 1;
                    break;
                case 3:
                    new_y -= 1;
                    break;
            }
            if((new_x <= xmax) && (new_x >= xmin) && (new_y <= ymax) && (new_y >= ymin)) {
                break; }
            new_x = state.xval;
            new_y = state.yval;
        }
        int[] vals = {new_x, new_y};
        return getState(vals);
    }

    public double[] simulateCommonRandomNumbersRun(int k2, int K2){
        double[] results = new double[2];

        //Perform CRN on (k,K) and (k2,K2) as parameters, average costs is result per run
        MRG32k3a arrival = getStream();
        MRG32k3a service = getStream();
        MRG32k3a alt_arrival = arrival.clone();
        MRG32k3a alt_service = service.clone();

        Sim.init();
        ThresholdQueue model = new ThresholdQueue(arrivalRate, avgService, avgHighService, maxTime, k, K, arrival, service);
        results[0] = model.getAverageCosts().average();

        Sim.init();
        ThresholdQueue alt_model = new ThresholdQueue(arrivalRate, avgService, avgHighService, maxTime, k2, K2, alt_arrival, alt_service);
        results[1] = alt_model.getAverageCosts().average();
        return results;
    }

    public static void main(String[] args) {
        int lower = 5;              // k-threshold for queue
        int upper = 20;              // K-threshold for queue
        int lower2 = 10;             // k-threshold for alternative queue
        int upper2 = 20;             // K-threshold for alternative queue
        double lambdaInv = 1.5;      // Arrival rate
        double muHighInv = 4;      // Average service time
        double muLowInv = 2;       // Average service time
        double maxTime = 10000;      // Simulation endtime (seconds)

        int kMin = 5;
        int kMax = 10;
        int KMin = 10;
        int KMax = 20;
        int budget = 5000;

        Assignment3 crn = new Assignment3(kMin, kMax, KMin, KMax, budget, lambdaInv, muLowInv, muHighInv, maxTime, lower, upper);
        crn.simulateCommonRandomNumbersRun(lower2,upper2);

        Assignment3 optimization = new Assignment3(kMin, kMax, KMin, KMax, budget, lambdaInv, muLowInv, muHighInv, maxTime, lower, upper);
        optimization.runLocalSearch();

        Assignment3 optimization2 = new Assignment3(kMin, kMax, KMin, KMax, budget, lambdaInv, muLowInv, muHighInv, maxTime, lower, upper);
        optimization2.runRankingSelection(budget/2, 0.05);
    }

}