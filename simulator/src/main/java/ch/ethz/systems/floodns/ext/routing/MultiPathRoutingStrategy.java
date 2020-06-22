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
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;

/**
 * Routing decider for a multi-path approach. For every connection, it starts as many
 * flows as there are paths mapping the source to the destination.
 */
public abstract class MultiPathRoutingStrategy extends TopologyRoutingStrategy {

    private final Map<ImmutablePair<Integer, Integer>, List<AcyclicPath>> pathsMapping;

    public MultiPathRoutingStrategy(Simulator simulator, Topology topology, Map<ImmutablePair<Integer, Integer>, List<AcyclicPath>> pathsMapping) {
        super(simulator, topology);
        this.pathsMapping = pathsMapping;
    }

    @Override
    public void assignStartFlowsInTopology(Connection connection) {

        // If all tors are all servers, else, every server is connected to a ToR
        if (!topologyDetails.areTorsEndpoints()) {

            // Map to ToR nodes
            int srcTorId = topologyDetails.getTorIdOfServer(connection.getSrcNodeId());
            int dstTorId = topologyDetails.getTorIdOfServer(connection.getDstNodeId());

            // Now retrieve paths
            List<AcyclicPath> kPaths = pathsMapping.get(new ImmutablePair<>(srcTorId, dstTorId));

            // Modify each path to include the src -> srcToR, and dstToR -> dst edges
            for (AcyclicPath path : kPaths) {
                AcyclicPath newPath = new AcyclicPath();
                newPath.add(network.getPresentLinksBetween(connection.getSrcNodeId(), srcTorId).get(0));
                newPath.addAll(path);
                newPath.add(network.getPresentLinksBetween(dstTorId, connection.getDstNodeId()).get(0));
                simulator.addFlowToConnection(connection, newPath);
            }

        } else {

            // If it is not auto-extended, every ToR is a server, so the ToR paths can be used directly
            List<AcyclicPath> kPaths = pathsMapping.get(new ImmutablePair<>(connection.getSrcNodeId(), connection.getDstNodeId()));
            for (AcyclicPath path : kPaths) {
                simulator.addFlowToConnection(connection, path);
            }

        }

    }

}
