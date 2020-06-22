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

import java.util.*;

/**
 * Routing decider for the acyclic valiant routing approach.
 *
 * It only allocates a single flow for each connection, which is routed
 * along a shortest path to a valiant node (chosen uniformly among ToRs),
 * and then a shortest path to the destination node.
 */
public class ValiantRoutingStrategy extends SinglePathRoutingStrategy {

    private final Random random;
    private final List<Integer> valiantNodeIdsList;
    private final Set<Integer> valiantNodeIdsSet;
    private final boolean torsAreEndpoints;
    private final Map<ImmutablePair<Integer, Integer>, List<Link>> nextHopPossibilities;
    private final boolean permitTorsInValiantNodesAndRetryIfSrcDstChosen;

    public ValiantRoutingStrategy(Simulator simulator, Topology topology, Set<Integer> valiantNodeIds, Random random, boolean permitTorsInValiantNodesAndRetryIfSrcDstChosen) {
        super(simulator, topology);
        this.random = random;
        assert(!topologyDetails.getSwitchesWhichAreTorsNodeIds().isEmpty());
        this.valiantNodeIdsList = new ArrayList<>(valiantNodeIds);
        this.valiantNodeIdsSet = new HashSet<>(valiantNodeIds);
        this.torsAreEndpoints = topologyDetails.areTorsEndpoints();
        this.nextHopPossibilities = RoutingUtility.determineEcmpRoutingStateSwitches(topology, false);
        this.permitTorsInValiantNodesAndRetryIfSrcDstChosen = permitTorsInValiantNodesAndRetryIfSrcDstChosen;
        if (!permitTorsInValiantNodesAndRetryIfSrcDstChosen) {
            Set<Integer> intersection = new HashSet<>(topologyDetails.getSwitchesWhichAreTorsNodeIds());
            intersection.retainAll(valiantNodeIdsSet);
            if (intersection.size() > 0) {
                throw new IllegalArgumentException("ToRs overlap with valiant nodes: not allowed.");
            }
        }
    }

    @Override
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

        // Valiant node decision
        int valiantNodeId = valiantNodeIdsList.get(random.nextInt(valiantNodeIdsList.size()));
        if (permitTorsInValiantNodesAndRetryIfSrcDstChosen) {
            while (srcTorId == valiantNodeId || dstTorId == valiantNodeId) {
                valiantNodeId = valiantNodeIdsList.get(random.nextInt(valiantNodeIdsList.size()));
            }
        }

        // If the source ToR is the same as the destination ToR, then
        // there is no need to be going to a valiant node
        boolean passedValiant = (srcTorId == dstTorId);

        // Create path
        List<Link> potentialCyclicPath = new ArrayList<>();

        // First hop from server to ToR if necessary
        if (!torsAreEndpoints) {
            potentialCyclicPath.add(network.getPresentLinksBetween(srcNode.getNodeId(), srcTorId).get(0));
        }

        // Rest of the actual valiant path between ToRs
        Node current = network.getNode(srcTorId);
        while (true) {

            // Valiant check, s -> v, then v -> t
            int towardsId = passedValiant ? dstTorId : valiantNodeId;

            // Decide next hop
            List<Link> possibilities = nextHopPossibilities.get(new ImmutablePair<>(current.getNodeId(), towardsId));
            Link hop = possibilities.get(Math.abs(random.nextInt()) % possibilities.size());

            // Add link to path
            potentialCyclicPath.add(hop);

            // Move on
            current = hop.getToNode();

            // If arrived at valiant node
            if (current.getNodeId() == valiantNodeId) {
                passedValiant = true;
            }

            // Finish if end is reached
            if (current.equals(network.getNode(dstTorId))) {
                break;
            }

        }

        // And finally hop from ToR to server
        if (!torsAreEndpoints) {
            potentialCyclicPath.add(network.getPresentLinksBetween(dstTorId, dstNode.getNodeId()).get(0));
        }

        // Convert acyclic
        return RoutingUtility.convertToAcyclic(potentialCyclicPath);

    }

}
