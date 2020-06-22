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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The direct link spawn allocator (DLSA) creates a dedicated link for every
 * connection in the network with a certain limited link capacity.
 *
 * If a {@link ch.ethz.systems.floodns.ext.routing.RoutingStrategy routing strategy} is
 * used, it must be the {@link ch.ethz.systems.floodns.ext.routing.VoidRoutingStrategy}, as
 * this allocator manipulates the topology dynamically with links and pre-selected routing
 * paths need to be present for correct behavior.
 */
public class DirectLinkSpawnAllocator extends Allocator {

    private final double directLinkCapacity;

    DirectLinkSpawnAllocator(Simulator simulator, Network network, double directLinkCapacity) {
        super(simulator, network);
        this.directLinkCapacity = directLinkCapacity;
    }

    @Override
    public void perform() {

        // Retrieve all the connections
        Collection<Connection> activeConnections = simulator.getActiveConnections();

        // Remove all existing links without any active flows
        Set<Link> links = new HashSet<>(network.getPresentLinks());
        for (Link l : links) {
            if (l.getActiveFlowIds().size() == 0) {
                simulator.removeExistingLink(l);
            }
        }

        // Go over every connection
        for (Connection conn : activeConnections) {

            // Create a link for every connection without flow
            if (conn.getActiveFlows().size() == 0) {

                // Create link
                Link link = simulator.addNewLink(conn.getSrcNodeId(), conn.getDstNodeId(), directLinkCapacity);
                AcyclicPath path = new AcyclicPath();
                path.add(link);

                // Add path to connection
                simulator.addFlowToConnection(conn, path);

            }

            // Allocate the bandwidth for the flow
            simulator.allocateFlowBandwidth(conn.getActiveFlows().iterator().next(), directLinkCapacity);

        }

    }

}
