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

package ch.ethz.systems.floodns.ext.lputils;

import ch.ethz.systems.floodns.core.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Percentile linear program solver to solve the splittable multi-commodity flow problem.
 *
 * The constraint formulation adapted from the following paper (different objective; added destination node constraint).
 *
 * Jyothi, Sangeetha Abdu, et al. "Measuring and understanding throughput of network topologies."
 * High Performance Computing, Networking, Storage and Analysis, SC16: International Conference for. IEEE, 2016.
 */
public class FullSplitEpsilonSumMaxLp {

    private final Simulator simulator;
    private final Network network;
    private final Map<Integer, Double> mapConnectionToThroughput;
    private final LpSolver lpSolver;

    public FullSplitEpsilonSumMaxLp(Simulator simulator, LpSolver lpSolver) {
        this.simulator = simulator;
        this.network = simulator.getNetwork();
        this.mapConnectionToThroughput = new HashMap<>();
        this.lpSolver = lpSolver;
    }

    /**
     * Calculate k for which the sum of all flows must be larger incorporating epsilon.
     *
     * @param epsilon   Sum of flow must be larger than (1 - epsilon) * N * k
     *
     * @return Objective boundary (maximum throughput for any demand)
     */
    public double calculate(double epsilon) {
        assert(epsilon >= 0 && epsilon <= 1);

        // To add demand constraints:
        //
        // , Map<Connection, Double> connectionToDemand
        // @param connectionToDemand    Connection to its demand

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

            // Flow constraints
            out.println("// Type 0: commodity constraints");
            for (Connection connection : simulator.getActiveConnections()) { // Definition of commodity flow
                out.format("c0_%d: ", connection.getConnectionId());
                boolean first = true;
                for (Link link : connection.getSrcNode().getOutgoingLinks()) {
                    if (!first) {
                        out.format(" + ");
                    }
                    first = false;
                    out.format("f_%d_%d", connection.getConnectionId(), link.getLinkId());
                }
                out.format(" - y_%d = 0;", connection.getConnectionId());
                out.println();
            }
            for (Connection connection : simulator.getActiveConnections()) { // Not negative
                out.format("c0_%d_0: y_%d >= 0;", connection.getConnectionId(), connection.getConnectionId());
                out.println();
            }
            // Demand constraint
            for (Connection connection : simulator.getActiveConnections()) { // Less than or equal to the barrier
                out.format("c0_%d_2: y_%d - Z <= 0;", connection.getConnectionId(), connection.getConnectionId());
                out.println();
            }
            out.format("cysum: "); // Sum of all commodity flow must be at least a percentile of it
            boolean first1 = true;
            for (Connection connection : simulator.getActiveConnections()) { // Less than or equal to the barrier
                if (!first1) {
                    out.format(" + ");
                }
                first1 = false;
                out.format("y_%d", connection.getConnectionId());
            }

            // Sum of flow must be larger than (1 - epsilon) * N * k
            out.format(" - %f Z >= 0;", (1.0 - epsilon) * simulator.getActiveConnections().size());
            out.println();

            // Link capacity constraints
            out.println("// Type 1: link capacity constraints");
            for (Link link : network.getPresentLinks()) {
                out.format("c1_%d: ", link.getLinkId());
                boolean first = true;
                for (Connection connection : simulator.getActiveConnections()) {
                    if (!first) {
                        out.format(" + ");
                    }
                    first = false;
                    out.format("f_%d_%d", connection.getConnectionId(), link.getLinkId());
                }
                out.format(" <= %f;", link.getCapacity());
                out.println();
            }

            out.println();

            // Flow conservation constraints
            out.println("// Type 2: flow conservation constraints");

            for (Connection connection : simulator.getActiveConnections()) {
                for (Node node : network.getNodes()) {
                    if (!node.equals(connection.getSrcNode()) && !node.equals(connection.getDstNode())) {

                        if (node.getIncomingLinks().size() > 0 || node.getOutgoingLinks().size() > 0) {

                            out.format("c2_%d_%d: ", connection.getConnectionId(), node.getNodeId());

                            // Flow incoming
                            boolean first = true;
                            for (Link link : node.getIncomingLinks()) {
                                if (!first) {
                                    out.format(" + ");
                                }
                                first = false;
                                out.format("f_%d_%d", connection.getConnectionId(), link.getLinkId());
                            }

                            // Flow outgoing
                            for (Link link : node.getOutgoingLinks()) {
                                out.format(" - f_%d_%d", connection.getConnectionId(), link.getLinkId());
                            }

                            out.println(" = 0;");

                        }

                    } else if (node.equals(connection.getSrcNode())) {

                        if (node.getIncomingLinks().size() > 0) {

                            out.format("c2_%d_%d: ", connection.getConnectionId(), node.getNodeId());

                            // Flow incoming
                            boolean first = true;
                            for (Link link : node.getIncomingLinks()) {
                                if (!first) {
                                    out.format(" + ");
                                }
                                first = false;
                                out.format("f_%d_%d", connection.getConnectionId(), link.getLinkId());
                            }

                            out.println(" = 0;");

                        }

                    } else if (node.equals(connection.getDstNode())) {

                        if (node.getOutgoingLinks().size() > 0) {

                            out.format("c2_%d_%d: ", connection.getConnectionId(), node.getNodeId());

                            // Flow incoming
                            boolean first = true;
                            for (Link link : node.getOutgoingLinks()) {
                                if (!first) {
                                    out.format(" + ");
                                }
                                first = false;
                                out.format("f_%d_%d", connection.getConnectionId(), link.getLinkId());
                            }

                            out.println(" = 0;");

                        }

                    }

                }
            }

            // Close file
            out.close();

        } catch (IOException e) {
            throw new IllegalStateException("Unable to write to temporary file.");
        }

        // Call solver
        ImmutablePair<Double, Map<String, Double>> result = lpSolver.solve(program.getAbsolutePath(), solution.getAbsolutePath());

        // Save results
        mapConnectionToThroughput.clear();
        for (Connection conn : simulator.getActiveConnections()) {
            mapConnectionToThroughput.put(conn.getConnectionId(), 0.0);
        }
        for (Map.Entry<String, Double> entry : result.getRight().entrySet()) {

            if (entry.getKey().startsWith("f_")) {

                int connId = Integer.parseInt(entry.getKey().split("_")[1]);
                int linkId = Integer.parseInt(entry.getKey().split("_")[2]);
                Double value = entry.getValue();

                if (simulator.getActiveConnection(connId).getSrcNode().getOutgoingLinks().contains(network.getLink(linkId))) {
                    mapConnectionToThroughput.put(connId, value + mapConnectionToThroughput.get(connId));
                }

            }

        }

        // Return final alpha value
        return result.getLeft();

    }

    /**
     * Retrieve the map from connection to throughput.
     *
     * @return  Unmodifiable map from connection to throughput
     */
    public Map<Integer, Double> getMapConnectionToThroughput() {
        return Collections.unmodifiableMap(mapConnectionToThroughput);
    }

}
