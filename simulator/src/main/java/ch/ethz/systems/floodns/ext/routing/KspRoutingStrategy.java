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

import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Node;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Routing decider for K-shortest paths. It schedules a single path
 * for each flow by uniformly choosing among the at-most K paths
 * it has found.
 */
public class KspRoutingStrategy extends SinglePathRoutingStrategy {

    private final Random random;
    private final Map<ImmutablePair<Integer, Integer>, List<AcyclicPath>> kShortestPaths;

    public KspRoutingStrategy(Simulator simulator, Topology topology, Random random, int k) {
        super(simulator, topology);
        this.random = random;
        this.kShortestPaths = RoutingUtility.determineKspRoutingStateBetweenToRs(k, topologyDetails, network);
    }

    @Override
    protected AcyclicPath assignSinglePath(Connection connection) {
        Node srcNode = connection.getSrcNode();
        Node dstNode = connection.getDstNode();

        // If it is auto-extended, every server is connected to a ToR, if it is not, every ToR is a server
        if (!topologyDetails.areTorsEndpoints()) {

            // Map to ToR nodes
            int srcTorId = topologyDetails.getTorIdOfServer(srcNode.getNodeId());
            int dstTorId = topologyDetails.getTorIdOfServer(dstNode.getNodeId());

            // Now retrieve paths
            List<AcyclicPath> kPaths = kShortestPaths.get(new ImmutablePair<>(srcTorId, dstTorId));
            AcyclicPath chosen = kPaths.get(Math.abs(random.nextInt()) % kPaths.size());

            // Modify the path to include the src -> srcToR, and dstToR -> dst edges
            AcyclicPath newPath = new AcyclicPath();
            newPath.add(network.getPresentLinksBetween(srcNode.getNodeId(), srcTorId).get(0));
            newPath.addAll(chosen);
            newPath.add(network.getPresentLinksBetween(dstTorId, dstNode.getNodeId()).get(0));
            return newPath;

        } else {

            // If it is not auto-extended, every ToR is a server, so the ToR paths can be used directly
            List<AcyclicPath> kPaths = kShortestPaths.get(new ImmutablePair<>(srcNode.getNodeId(), dstNode.getNodeId()));
            return kPaths.get(Math.abs(random.nextInt()) % kPaths.size());

        }

    }

}
