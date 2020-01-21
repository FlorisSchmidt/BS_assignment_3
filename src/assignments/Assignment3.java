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

        State opt = selectOptimalState();
        return opt;
    }

    public State selectBestState(State current, State neighbor){

        //Return best state

        return current;
    }

    public State selectRandomStart() {
        State state;

        //Select a random state

        return state;
    }

    public State selectRandomNeighbor(State state) {
        State neighbor;

        //Select a random neighbor

        return neighbor;
    }

    public double[] simulateCommonRandomNumbersRun(int k2, int K2){
        double[] results = new double[2];

        //Perform CRN on (k,K) and (k2,K2) as parameters, average costs is result per run

        return results;
    }

    public static void main(String[] args) {
        int lower = ;              // k-threshold for queue
        int upper = ;              // K-threshold for queue
        int lower2 = ;             // k-threshold for alternative queue
        int upper2 = ;             // K-threshold for alternative queue
        double lambdaInv = ;      // Arrival rate
        double muHighInv = ;      // Average service time
        double muLowInv = ;       // Average service time
        double maxTime = 10000;      // Simulation endtime (seconds)

        int kMin = ;
        int kMax = ;
        int KMin = ;
        int KMax = ;
        int budget = ;

        Assignment3 crn = new Assignment3(kMin, kMax, KMin, KMax, budget, lambdaInv, muLowInv, muHighInv, maxTime, lower, upper);
        crn.simulateCommonRandomNumbersRun(lower2,upper2);

        Assignment3 optimization = new Assignment3(kMin, kMax, KMin, KMax, budget, lambdaInv, muLowInv, muHighInv, maxTime, lower, upper);
        optimization.runLocalSearch();

        Assignment3 optimization2 = new Assignment3(kMin, kMax, KMin, KMax, budget, lambdaInv, muLowInv, muHighInv, maxTime, lower, upper);
        optimization2.runRankingSelection(100, 0.05);
    }

}