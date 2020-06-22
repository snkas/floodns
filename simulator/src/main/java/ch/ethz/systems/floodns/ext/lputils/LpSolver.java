package ch.ethz.systems.floodns.ext.lputils;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Map;

public abstract class LpSolver {

    /**
     * Solve a CPLEX-input formatted program with the default options.
     *
     * @param cplexFilename     CPLEX input filename
     * @param solutionFilename  Output filename for solution
     *
     * @return Solution (objective, map of variable values)
     */
    public abstract ImmutablePair<Double, Map<String, Double>> solve(String cplexFilename, String solutionFilename);

}
