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

        //Fill the long[] with random seeds
        seed = rng.longs(6,0,49494444).toArray();
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

        //Perform initial runs

        HashSet<State> I = selectCandidateSolutions(alpha);

        //Perform rest of the runs

        State opt = selectOptimalState();

        return opt;
    }

    public HashSet<State> selectCandidateSolutions(double alpha) {
        HashSet<State> I = new HashSet();

        //Find all candidate solutions for the ranking and selection method

        return I;
    }

    public State runLocalSearch() {

        //Perform Local Search
        State currentState = selectRandomStart();
        int m = 0;
        while(m<=budget){
            selectBestState(currentState,selectRandomNeighbor(currentState);

        }
        State opt = selectOptimalState();
        return opt;
    }

    public State selectBestState(State current, State neighbor){
        MRG32k3a arrival = getStream();
        MRG32k3a service = getStream();

        ThresholdQueue model = new ThresholdQueue(arrivalRate,avgService,avgHighService,maxTime,current.xval,current.yval,arrival,service);
        ThresholdQueue model2 = new ThresholdQueue(arrivalRate,avgService,avgHighService,maxTime,neighbor.xval,neighbor.yval,arrival,service);
        if(model.getAverageCosts().average()>model2.getAverageCosts().average()){
            return neighbor;
        }
        //Return best state
        return current;
    }

    public State selectRandomStart() {
        //Select a random state
        State state = outputs[rng.nextInt(numStates)];
        return state;
    }

    public State selectRandomNeighbor(State state) {
        //Select a random neighbor
        boolean illegal = true;
        int x_offset = 0;
        int y_offset = 0;
        while (illegal) {
            x_offset = rng.nextInt(2) - 1;
            y_offset = rng.nextInt(2) - 1;
            if (state.xval + x_offset <= xmax && state.xval + x_offset >= xmin && state.yval + y_offset <= ymax
                    && state.yval >= ymin && !(x_offset == 0 && y_offset == 0)) {
                illegal = false;
            }
        }
        int[] vals = {state.xval + x_offset, (state.yval + y_offset)};
        return getState(vals);
    }

    public double[] simulateCommonRandomNumbersRun(int k2, int K2){
        double[] results = new double[2];

        //Perform CRN on (k,K) and (k2,K2) as parameters, average costs is result per run
        MRG32k3a arrival = getStream();
        MRG32k3a service = getStream();
        ThresholdQueue model = new ThresholdQueue(arrivalRate, avgService, avgHighService, maxTime, k, K, arrival, service);
        ThresholdQueue model2 = new ThresholdQueue(arrivalRate, avgService, avgHighService, maxTime, k2, K2, arrival, service);
        results[0] = model.getAverageCosts().average();
        results[1] = model2.getAverageCosts().average();
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
        optimization2.runRankingSelection(100, 0.05);
    }

}