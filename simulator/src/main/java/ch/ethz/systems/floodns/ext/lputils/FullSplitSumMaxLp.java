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
 * Total throughput linear program solver to solve the splittable multi-commodity flow problem.
 *
 * The linear program takes in a traffic matrix (TM) and maximizes the total sum of demand satisfied.
 *
 * The constraint formulation is based on the following paper (different objective; added destination node constraint):
 *
 * Jyothi, Sangeetha Abdu, et al. "Measuring and understanding throughput of network topologies."
 * High Performance Computing, Networking, Storage and Analysis, SC16: International Conference for. IEEE, 2016.
 */
public class FullSplitSumMaxLp {

    private final Simulator simulator;
    private final Network network;
    private final Map<Integer, Double> mapConnectionToThroughput;
    private final LpSolver lpSolver;

    public FullSplitSumMaxLp(Simulator simulator, LpSolver lpSolver) {
        this.simulator = simulator;
        this.network = simulator.getNetwork();
        this.mapConnectionToThroughput = new HashMap<>();
        this.lpSolver = lpSolver;
    }

    /**
     * Calculate the maximum total throughput achievable.
     *
     * @param connectionToDemand    Connection identifier to its demand
     *
     * @return  Maximum total throughput achievable across all demands
     */
    public double calculateMaxTotalThroughput(Map<Integer, Double> connectionToDemand) {

        // Open file
        File program;
        File solution;
        try {
            program = File.createTempFile("program", ".lp");
            solution = File.createTempFile("solution", ".sol");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create temporary file for linear program and/or solution.");
        }

        // Write linear program to file
        try {

            // Open writer
            PrintWriter out = new PrintWriter(program);

            // Objective
            out.print("max: ");
            boolean first1 = true;
            for (Connection connection : simulator.getActiveConnections()) {
                for (Link link : connection.getDstNode().getIncomingLinks()) {
                    if (!first1) {
                        out.format(" + ");
                    }
                    first1 = false;
                    out.format("f_%d_%d", connection.getConnectionId(), link.getLinkId());
                }
            }
            out.println(";");
            out.println();

            // Sum of amount of flow belonging to a connection going out must be larger than 0 but not more than
            // its proportional demand
            out.println("// Type 0: flow fairness constraints");
            for (Connection connection : simulator.getActiveConnections()) {
                out.format("c0_%d_1: ", connection.getConnectionId());
                boolean first = true;
                for (Link link : connection.getSrcNode().getOutgoingLinks()) {
                    if (!first) {
                        out.format(" + ");
                    }
                    first = false;
                    out.format("f_%d_%d", connection.getConnectionId(), link.getLinkId());
                }
                out.format(" >= 0;");
                out.println();
            }
            for (Connection connection : simulator.getActiveConnections()) {
                out.format("c0_%d_2: ", connection.getConnectionId());
                boolean first = true;
                for (Link link : connection.getSrcNode().getOutgoingLinks()) {
                    if (!first) {
                        out.format(" + ");
                    }
                    first = false;
                    out.format("f_%d_%d", connection.getConnectionId(), link.getLinkId());
                }
                out.format(" <= %f;", connectionToDemand.get(connection.getConnectionId()));
                out.println();
            }

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

            // Link capacity constraints
            out.println("// Type 1.5: link capacity lower bound");
            for (Link link : network.getPresentLinks()) {
                for (Connection connection : simulator.getActiveConnections()) {
                    out.format("c1.5_%d%d: f_%d_%d >= 0;\n", connection.getConnectionId(), link.getLinkId(), connection.getConnectionId(), link.getLinkId());
                }
            }

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

        // Return final total throughput
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
