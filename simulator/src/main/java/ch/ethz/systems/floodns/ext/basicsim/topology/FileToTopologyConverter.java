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

package ch.ethz.systems.floodns.ext.basicsim.topology;

import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.ext.basicsim.NoDuplicatesProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Converter for a file to the network.
 */
public class FileToTopologyConverter {

    /**
     * Convert a file to an instantiated topology.
     *
     * @param fileName              File name
     * @param uniformLinkCapacity   Uniform link capacity
     *
     * @return  Topology
     */
    public static Topology convert(String fileName, double uniformLinkCapacity) {
        return convert(fileName, new UniformLinkCapacityAssigner(uniformLinkCapacity));
    }

    /**
     * Read in a graph and its details.
     *
     * Properties with the following properties for example:
     *
     * num_nodes=3
     * num_undirected_edges=3
     * switches=set(0,1,2)
     * switches_which_are_tors=set(0,1,2)
     * servers=set()
     * undirected_edges=set(0-1,1-2,0-2)
     *
     * @param fileName              File name (e.g. /path/to/file.topology)
     * @param linkCapacityAssigner  Link capacity assigner
     *
     * @return Graph and its details
     */
    public static Topology convert(String fileName, LinkCapacityAssigner linkCapacityAssigner) {

        try {

            // Load the properties file
            FileInputStream fileInputStream = new FileInputStream(fileName);
            NoDuplicatesProperties properties = new NoDuplicatesProperties();
            properties.load(fileInputStream);

            // Check required properties
            properties.validate(new String[]{
                    "num_nodes", "num_undirected_edges", "switches", "servers", "switches_which_are_tors", "undirected_edges"
            });

            // Set it within the details
            TopologyDetails details = new TopologyDetails();
            details.setNumNodes(parsePositiveInteger(properties.getProperty("num_nodes")));
            details.setNumUndirectedEdges(parsePositiveInteger(properties.getProperty("num_undirected_edges")));
            details.setSwitchNodeIds(convertToSetOfIntegers(properties.getProperty("switches")));
            details.setServerNodeIds(convertToSetOfIntegers(properties.getProperty("servers")));
            details.setSwitchesWhichAreTorsNodeIds(convertToSetOfIntegers(properties.getProperty("switches_which_are_tors")));

            // ToRs must overlap with switches
            if (!details.getSwitchNodeIds().containsAll(details.getSwitchesWhichAreTorsNodeIds())) {
                throw new IllegalArgumentException(
                        String.format(
                            "Not all ToRs (%s) are set as switches (%s).",
                                StringUtils.join(details.getSwitchesWhichAreTorsNodeIds(), ","),
                                StringUtils.join(details.getSwitchNodeIds(), ",")
                        )
                );
            }

            // Switches cannot overlap with servers
            Set<Integer> switchServerIntersection = new HashSet<>(details.getSwitchNodeIds());
            switchServerIntersection.retainAll(details.getServerNodeIds());
            if (switchServerIntersection.size() > 0) {
                throw new IllegalArgumentException("Switches overlap with servers: not allowed.");
            }

            // The join of switches and servers must cover all nodes
            Set<Integer> allJoin = new HashSet<>(details.getSwitchNodeIds());
            allJoin.addAll(details.getServerNodeIds());
            if (allJoin.size() != details.getNumNodes()) {
                throw new IllegalArgumentException("The switches and servers together must cover the entire topology.");
            }

            // Instantiate network
            Network network = new Network(details.getNumNodes());
            List<Pair<Integer, Integer>> edgeList = edgeSetToSortedAscendingEdgeList(convertToSetOfUndirectedEdgePairs(properties.getProperty("undirected_edges")));
            for (Pair<Integer, Integer> pair : edgeList) {
                int from = pair.getLeft();
                int to = pair.getRight();

                // Links between servers are not allowed
                if (details.getServerNodeIds().contains(from) && details.getServerNodeIds().contains(to)) {
                    throw new IllegalArgumentException("Direct links between servers are not permitted.");
                }

                // Save ToR connections
                if (details.getServerNodeIds().contains(from)) {
                    if (!details.getSwitchesWhichAreTorsNodeIds().contains(to)) {
                        throw new IllegalArgumentException("Server must be connected to ToR.");
                    }
                    details.saveTorHasServer(to, from);
                }
                if (details.getServerNodeIds().contains(to)) {
                    if (!details.getSwitchesWhichAreTorsNodeIds().contains(from)) {
                        throw new IllegalArgumentException("Server must be connected to ToR.");
                    }
                    details.saveTorHasServer(from, to);
                }

                // Add to network
                network.addLink(from, to, linkCapacityAssigner.assignCapacity(details, from, to));
                network.addLink(to, from, linkCapacityAssigner.assignCapacity(details, to, from));

            }

            // Check that the exact amount of edges is added
            if (network.getPresentLinks().size() != details.getNumUndirectedEdges() * 2) {
                throw new IllegalArgumentException(String.format(
                        "Number of edges indicated (%d) does not match with the amount of edges defined (%d).",
                        details.getNumUndirectedEdges() * 2,
                        network.getPresentLinks().size()
                ));
            }

            // Servers can only have one ToR connected to it
            for (int i : details.getServerNodeIds()) {
                if (network.getOutgoingLinksOf(i).size() != 1) {
                    throw new IllegalArgumentException("Server with id " + i + " is connect to zero or more than one ToR.");
                }
            }

            // Return final instantiated network
            return new Topology(network, details);

        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file: " + fileName + "; I/O error: " + e.getCause());
        }

    }

    private static Set<String> convertToSetOfStrings(String val) {
        Set<String> stringSet = new HashSet<>();
        if (val.startsWith("set(") && val.endsWith(")")) {
            String inner = val.substring(4, val.length() - 1);
            if (inner.trim().length() > 0) {
                String[] spl = inner.split(",");
                for (String s : spl) {
                    stringSet.add(s.trim());
                }
                if (spl.length != stringSet.size()) {
                    throw new IllegalArgumentException("Duplicate string in set.");
                }
            }
        } else {
            throw new IllegalArgumentException("Value must be like set(...): " + val);
        }
        return stringSet;
    }

    private static Set<Integer> convertToSetOfIntegers(String val) {
        Set<String> stringSet = convertToSetOfStrings(val);
        Set<Integer> intSet = new HashSet<>();
        for (String s : stringSet) {
            intSet.add(parsePositiveInteger(s));
        }
        if (stringSet.size() != intSet.size()) {
            throw new IllegalArgumentException("Duplicate integer in set.");
        }
        return intSet;
    }

    private static Set<Pair<Integer, Integer>> convertToSetOfUndirectedEdgePairs(String val) {
        Set<String> stringSet = convertToSetOfStrings(val);
        Set<Pair<Integer, Integer>> setPairsSet = new HashSet<>();
        for (String s : stringSet) {
            String[] spl = s.split("-");
            if (spl.length != 2) {
                throw new IllegalArgumentException("Invalid set pair: " + s);
            }
            int a = parsePositiveInteger(spl[0]);
            int b = parsePositiveInteger(spl[1]);
            setPairsSet.add(new ImmutablePair<>(Math.min(a, b), Math.max(b, a)));
        }
        if (stringSet.size() != setPairsSet.size()) {
            throw new IllegalArgumentException("Duplicate undirected edge pair in set.");
        }
        return setPairsSet;
    }

    private static List<Pair<Integer, Integer>> edgeSetToSortedAscendingEdgeList(Set<Pair<Integer, Integer>> edgeSet) {
        List<Pair<Integer, Integer>> list = new ArrayList<>(edgeSet);
        Collections.sort(list);
        return list;
    }

    private static int parsePositiveInteger(String s) {
        int value = Integer.parseInt(s);
        if (value < 0) {
            throw new IllegalArgumentException("Value must be a positive integer: " + s);
        }
        return value;
    }

}
