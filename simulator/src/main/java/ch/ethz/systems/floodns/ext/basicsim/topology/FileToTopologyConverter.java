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
import ch.ethz.systems.floodns.ext.utils.ConstantMap;
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
     * link_data_rate_bit_per_ns=map(0->1:10, 1->2:10, 0->2:10, 1->0:10, 2->1:10, 2->0:10)
     *
     * @param fileName              File name (e.g. /path/to/file.topology)
     *
     * @return Graph and its details
     */
    public static Topology convert(String fileName) {

        try {

            // Load the properties file
            FileInputStream fileInputStream = new FileInputStream(fileName);
            NoDuplicatesProperties properties = new NoDuplicatesProperties();
            properties.load(fileInputStream);

            // Check required properties
            properties.validate(new String[]{
                    "num_nodes", "num_undirected_edges", "switches", "servers", "switches_which_are_tors", "undirected_edges", "link_data_rate_bit_per_ns"
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
            Map<Pair<Integer, Integer>, Double> linkToCapacity = convertToLinkCapacityMap(edgeList, properties.getProperty("link_data_rate_bit_per_ns"));
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
                network.addLink(from, to, linkToCapacity.get(pair));
                network.addLink(to, from, linkToCapacity.get(new ImmutablePair<>(to, from)));

            }

            // Check that the exact amount of edges is added
            if (network.getPresentLinks().size() != details.getNumUndirectedEdges() * 2) {
                throw new IllegalArgumentException(String.format(
                        "Number of edges indicated (%d) does not match with the amount of edges defined (%d).",
                        details.getNumUndirectedEdges() * 2,
                        network.getPresentLinks().size()
                ));
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
            if (b < a) {
                throw new IllegalArgumentException("The first node identifier should be the lower one: " + s);
            }
            setPairsSet.add(new ImmutablePair<>(a, b));
        }
        if (stringSet.size() != setPairsSet.size()) {
            throw new IllegalArgumentException("Duplicate undirected edge pair in set");
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

    private static double parsePositiveDouble(String s) {
        double value = Double.parseDouble(s);
        if (value < 0.0) {
            throw new IllegalArgumentException("Value must be a positive double: " + s);
        }
        return value;
    }

    private static Map<Pair<Integer, Integer>, Double> convertToLinkCapacityMap(List<Pair<Integer, Integer>> edgeList, String val) {

        // If it does not start with map, it must be just a single value
        if (!val.trim().startsWith("map")) {
            return new ConstantMap<>(parsePositiveDouble(val));
        }

        // Parse the mapping
        Map<Pair<Integer, Integer>, Double> result = new HashMap<>();
        if (val.startsWith("map(") && val.endsWith(")")) {
            String inner = val.substring(4, val.length() - 1);
            if (inner.trim().length() > 0) {

                // Split inner on the comma
                String[] spl = inner.split(",");
                for (String s : spl) {

                    // a-b:c into (a-b, c)
                    String[] splColon = s.split(":");
                    if (splColon.length != 2) {
                        throw new IllegalArgumentException("Mapping must be a->b:c, incorrect: " + s);
                    }

                    // a->b into (a, b)
                    String[] dashSplit = splColon[0].split("->");
                    if (dashSplit.length != 2) {
                        throw new IllegalArgumentException("Mapping key must be a->b, incorrect: " + splColon[0]);
                    }
                    Pair<Integer, Integer> edge = new ImmutablePair<>(parsePositiveInteger(dashSplit[0].trim()), parsePositiveInteger(dashSplit[1].trim()));

                    // (a, b) must be in the topology
                    if (!edgeList.contains(edge) && !edgeList.contains(new ImmutablePair<>(edge.getRight(), edge.getLeft()))) {
                        throw new IllegalArgumentException("Edge does not exist: " + edge);
                    }

                    // Parse capacity value
                    double capacity = parsePositiveDouble(splColon[1].trim());
                    if (result.containsKey(edge)) {
                        throw new IllegalArgumentException("Duplicate in link to capacity mapping: " + edge);
                    }
                    result.put(edge, capacity);

                }
            }

            // Check all edges are in the mapping
            if (result.size() != edgeList.size() * 2) {
                throw new IllegalArgumentException("Not all edges were covered");
            }

        } else {
            throw new IllegalArgumentException("Value must be a single value or map(...): " + val);
        }

        return result;
    }

}
