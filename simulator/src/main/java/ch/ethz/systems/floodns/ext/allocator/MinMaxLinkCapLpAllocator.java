/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 snkas
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ch.ethz.systems.floodns.ext.allocator;

import ch.ethz.systems.floodns.core.*;
import ch.ethz.systems.floodns.ext.lputils.LpSolver;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Allocator which uses the linear solver to solve a
 * max-min link capacity linear program [1] to determine the
 * amount of rate for each flow by minimizing the maximum
 * link utilization.
 *
 * Linear program (taken from Table 2 of [1]):
 *
 * Minimize Z ...
 *
 * ... subject to:
 *
 *  (1) Path weights sum up to 1:
 *
 *      for all (s, t) in V: SUM_{p in paths(s&rarr;t)} w_p = 1
 *
 *  (2) Only positive weights:
 *
 *      for all p in all_paths: w_p &gt;= 0
 *
 *  (3) Link utilization definition:
 *
 *      for all e in E:
 *         U_e = SUM_{p in all_paths : e part of p} (w_p * demand_{endpoints of p}) / capacity(e))
 *
 *  (4) Link utilization under Z:
 *
 *      for all e in E: U_e &lt;= Z
 *
 * ------------
 *
 * CITATIONS
 *
 * Linear program based on the formulation in (Table 2 of the paper):
 * [1] Kumar, Praveen, et al. "Semi-Oblivious Traffic Engineering: The Road Not Taken." USENIX NSDI. 2018.
 */
public class MinMaxLinkCapLpAllocator extends Allocator {

    private final Map<Integer, Double> connectionToDemand;
    private double objectiveZ = 1.0;
    private long solveTimeMs = -1;
    private final LpSolver lpSolver;

    public MinMaxLinkCapLpAllocator(Simulator simulator, Network network, Map<Integer, Double> connectionToDemand, LpSolver lpSolver) {
        super(simulator, network);
        this.connectionToDemand = connectionToDemand;
        this.lpSolver = lpSolver;
    }

    /**
     * Solve the linear program to find the flow allocation
     * which minimizes the maximum link congestion respective to the
     * connection demands.
     */
    @Override
    public void perform() {

        // Open file
        File program;
        File solution;
        try {
            program = File.createTempFile("mmlc", ".lp");
            solution = File.createTempFile("mmlc", ".sol");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create temporary file for linear program and solution.");
        }

        // Write linear program to file
        try {

            // Open writer
            PrintWriter out = new PrintWriter(program);

            // Objective
            out.println("min: Z;");
            out.println();

            // Sum of all path weights of the flow paths belonging to a connection must be equal to 1
            out.println("// Type 0: sum of path weights equals 1");
            for (Connection connection : simulator.getActiveConnections()) {
                out.print("c0_" + connection.getConnectionId() + ": ");
                boolean first = true;
                for (Flow f : connection.getActiveFlows()) {
                    if (!first) {
                        out.print(" + ");
                    }
                    first = false;
                    out.print("w_" + f.getFlowId());
                }
                out.println(" = 1;");
            }

            // All path weights of the flow paths are non-zero
            out.println("// Type 1: non-zero path weights");
            for (Flow f : network.getActiveFlows()) {
                out.println("c1_" + f.getFlowId() + ": w_" + f.getFlowId() + " >= 0;");
            }

            // Utilization definition
            out.println("// Type 2: utilization definition");
            for (Link link : network.getPresentLinks()) {
                out.print("c2_" + link.getLinkId() + ": U_" + link.getLinkId());
                for (Flow f : link.getActiveFlows()) {
                    out.print(" - " + (connectionToDemand.get(f.getParentConnection().getConnectionId()) / link.getCapacity()) + " w_" + f.getFlowId());
                }
                out.println(" = 0;");
            }

            // Utilization of each link must be lower than the minimum
            out.println("// Type 3: utilization cannot exceed minimum");
            for (Link link : network.getPresentLinks()) {
                out.println("c3_" + link.getLinkId() + ": U_" + link.getLinkId() + " - Z <= 0;");
            }

            // Close file
            out.close();

        } catch (IOException e) {
            throw new IllegalStateException("Unable to write to temporary file.");
        }

        // Call solver
        long start = System.currentTimeMillis();
        ImmutablePair<Double, Map<String, Double>> result = lpSolver.solve(program.getAbsolutePath(), solution.getAbsolutePath());
        solveTimeMs = System.currentTimeMillis() - start;

        // Reset all flow bandwidth to zero such that the full link capacity is free
        // to be set for the flows
        for (Flow f : network.getActiveFlows()) {
            simulator.allocateFlowBandwidth(f, 0);
        }

        // Save results
        objectiveZ = result.getLeft();
        for (Map.Entry<String, Double> entry : result.getRight().entrySet()) {

            if (entry.getKey().startsWith("w_")) {

                // Parse weight solution line
                int flowId = Integer.parseInt(entry.getKey().split("_")[1]);
                Double weight = entry.getValue();

                // Put into final mapping
                Flow flow = network.getActiveFlow(flowId);
                simulator.allocateFlowBandwidth(flow, weight * connectionToDemand.get(flow.getParentConnection().getConnectionId()) / objectiveZ);

            }

        }

    }

    /**
     * Get the objective (Z) value of the previous {@link #perform()} call.
     *
     * @return Objective (Z) value
     */
    public double getObjectiveZ() {
        return objectiveZ;
    }

    /**
     * Get the time it took to solve the linear program in the previous ({@link #perform()} call.
     *
     * @return Solve time (ms)
     */
    public long getSolveTimeMs() {
        return solveTimeMs;
    }

}
