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
 * program under link capacity constraints, and positive
 * flows. An example:
 *
 * e = 90%
 * sum of all connections &gt;= e * N * Z
 * every connection &lt;= Z
 *
 * Linear program:
 *
 * Maximize Z ...
 *
 * ... subject to:
 *
 *  (0) Sum of flow belonging to connection equal to threshold:
 *
 *      for each connection c:
 *          SUM_{p in paths(c)} f_p &lt;= Z
 *
 *  (1) Only positive flows:
 *
 *      for all p in all_paths: f_p &gt;= 0
 *
 *  (2) Link capacity not exceeded:
 *
 *      for all e in E:
 *         SUM_{p in all_paths : e part of p} (f_p) / capacity(e)) &lt;= 1
 *
 *  (3) Super sum must be greater than threshold:
 *
 *      for each connection c:
 *          SUM_{p in paths(c)} f_p
 *      &gt;= N * Z * epsilon
 *
 */
public class EpsilonSumMaxLpAllocator extends Allocator {

    private double objectiveZ = 1.0;
    private long solveTimeMs = -1;
    private final double epsilon;
    private final LpSolver lpSolver;

    public EpsilonSumMaxLpAllocator(Simulator simulator, Network network, double epsilon, LpSolver lpSolver) {
        super(simulator, network);
        this.epsilon = epsilon;
        this.lpSolver = lpSolver;
    }

    /**
     * Solve the linear program  to find the flow allocation.
     */
    @Override
    public void perform() {

        // Open file
        File program;
        File solution;
        try {
            program = File.createTempFile("program", ".lp");
            solution = File.createTempFile("solution", ".sol");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create temporary file for linear program and solution.");
        }

        // Write linear program to file
        try {

            // Open writer
            PrintWriter out = new PrintWriter(program);

            // Objective
            out.println("max: Z;");
            out.println();

            // Sum of flow belonging to connection greater than threshold
            out.println("// Type 0: Sum of flow belonging to connection greater than or equal to threshold");
            for (Connection connection : simulator.getActiveConnections()) {
                out.print("c0_" + connection.getConnectionId() + ": ");
                boolean first = true;
                for (Flow f : connection.getActiveFlows()) {
                    if (!first) {
                        out.print(" + ");
                    }
                    first = false;
                    out.print("f_" + f.getFlowId());
                }
                out.format(" - Z <= 0;");
                out.println();
            }
            out.println();

            // Only positive flows
            out.println("// Type 1: only positive flows");
            for (Flow f : network.getActiveFlows()) {
                out.println("c1_" + f.getFlowId() + ": f_" + f.getFlowId() + " >= 0;");
            }
            out.println();

            // Utilization definition
            out.println("// Type 2: Link capacity not exceeded");
            for (Link link : network.getPresentLinks()) {
                if (link.getActiveFlows().size() > 0) {
                    out.format("c2_%d: ", link.getLinkId());
                    boolean first = true;
                    for (Flow f : link.getActiveFlows()) {
                        if (!first) {
                            out.print(" + ");
                        }
                        first = false;
                        out.format("f_%d", f.getFlowId());
                    }
                    out.format(" <= %f;", link.getCapacity());
                    out.println();
                }
            }
            out.println();

            // Super sum must be greater than threshold
            out.println("// Type 3: Super sum must be greater than threshold");
            boolean first = true;
            for (Connection connection : simulator.getActiveConnections()) {
                for (Flow f : connection.getActiveFlows()) {
                    if (!first) {
                        out.print(" + ");
                    }
                    first = false;
                    out.print("f_" + f.getFlowId());
                }
            }
            out.format(" - %f Z >= 0;", simulator.getActiveConnections().size() * epsilon);
            out.println();
            out.println();

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

            if (entry.getKey().startsWith("f_")) {

                // Parse weight solution line
                int flowId = Integer.parseInt(entry.getKey().split("_")[1]);
                Double flowBandwidth = entry.getValue();

                // Put into final mapping
                Flow flow = network.getActiveFlow(flowId);
                simulator.allocateFlowBandwidth(flow, flowBandwidth);

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
