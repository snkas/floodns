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

package ch.ethz.systems.floodns.ext.routing;

import ch.ethz.systems.floodns.core.*;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Routing decider for the Equal-Cost Multi-Path (ECMP) routing approach.
 * It only allocates a single flow for each connection, which is routed
 * along a shortest path.
 */
public class EcmpRoutingStrategy extends SinglePathRoutingStrategy {

    private final Map<ImmutablePair<Integer, Integer>, List<Link>> nextHopPossibilities;
    private final Random random;
    private final boolean torsAreEndpoints;

    public EcmpRoutingStrategy(Simulator simulator, Topology topology, Random random) {
        super(simulator, topology);
        this.random = random;
        this.nextHopPossibilities = RoutingUtility.determineEcmpRoutingStateSwitches(topology, true);
        this.torsAreEndpoints = this.topologyDetails.areTorsEndpoints();
    }

    protected AcyclicPath assignSinglePath(Connection connection) {
        Node srcNode = connection.getSrcNode();
        Node dstNode = connection.getDstNode();

        // Retrieve ToR identifiers
        int srcTorId = srcNode.getNodeId();
        int dstTorId = dstNode.getNodeId();
        if (!torsAreEndpoints) {
            srcTorId = topologyDetails.getTorIdOfServer(srcNode.getNodeId());
            dstTorId = topologyDetails.getTorIdOfServer(dstNode.getNodeId());
        }

        // Create path
        AcyclicPath path = new AcyclicPath();

        // First hop from server to ToR if necessary
        if (!torsAreEndpoints) {
            path.add(network.getPresentLinksBetween(srcNode.getNodeId(), srcTorId).get(0));
        }

        // Now add ECMP path between ToRs
        Node current = network.getNode(srcTorId);
        while (true) {

            // Finish if there
            if (current.equals(network.getNode(dstTorId))) {
                break;
            }

            // Decide next hop
            List<Link> possibilities = nextHopPossibilities.get(new ImmutablePair<>(current.getNodeId(), dstTorId));
            Link hop = possibilities.get(random.nextInt(possibilities.size()));

            // Add link to path
            path.add(hop);

            // Move on
            current = hop.getToNode();

        }

        // And finally from ToR to server
        if (!torsAreEndpoints) {
            path.add(network.getPresentLinksBetween(dstTorId, dstNode.getNodeId()).get(0));
        }

        return path;

    }

}
