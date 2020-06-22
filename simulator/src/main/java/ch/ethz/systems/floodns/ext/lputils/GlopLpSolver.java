package ch.ethz.systems.floodns.ext.lputils;

import ch.ethz.systems.floodns.ext.sysutils.Command;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GlopLpSolver extends LpSolver {

    private final String glopCommand;
    private final boolean enableLogInfo;

    public GlopLpSolver(String glopCommand, boolean enableLogInfo) {
        this.glopCommand = glopCommand;
        this.enableLogInfo = enableLogInfo;
    }

    @Override
    public ImmutablePair<Double, Map<String, Double>> solve(String lpFilename, String solutionFilename) {

        // Call lp_solve
        Command.runCommandWriteOutput(
                String.format(
                        "python %s %s",
                        glopCommand,
                        lpFilename
                ),
                solutionFilename,
                enableLogInfo
        );

        // Retrieve objective and variables
        Double objective = 0.0;
        boolean foundObjective = false;
        Map<String, Double> variables = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(solutionFilename));
            String line;
            boolean areAtVariableSection = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Value of objective function:")) {
                    String[] spl = line.split("\\s+");
                    objective = Double.valueOf(spl[spl.length - 1]);
                    foundObjective = true;
                }

                // Variables
                if (areAtVariableSection) {
                    String[] spl = line.split("\\s+");
                    variables.put(spl[0], Double.valueOf(spl[1]));
                }

                if (line.startsWith("Actual values of the variables:")) {
                    areAtVariableSection = true;
                }

            }

        } catch (IOException e) {
            throw new IllegalStateException("Unable to read objective and/or variables.");
        }

        // Check if we found the objective (e.g., it was feasible)
        if (!foundObjective) {
            throw new IllegalStateException(
                    "Linear program was not solved; it is presumably unfeasible.\n" +
                            "Linear program file: " + lpFilename + "\n" +
                            "Solution file: " + solutionFilename);
        }

        // Return final result
        return new ImmutablePair<>(objective, variables);

    }

}
