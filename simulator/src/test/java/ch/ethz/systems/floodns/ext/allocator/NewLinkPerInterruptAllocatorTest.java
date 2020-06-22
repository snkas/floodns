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
import ch.ethz.systems.floodns.ext.basicsim.schedule.TrafficSchedule;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import ch.ethz.systems.floodns.ext.routing.VoidRoutingStrategy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NewLinkPerInterruptAllocatorTest {

    @Test
    public void testNewLinkForEveryConnectionAtInterrupt() throws IOException {
        Simulator simulator = new Simulator();

        // Create new network, simulator and allocator
        Network network = new Network(2);

        // Change allocator
        //
        // This allocator removes all links, and thus the flows that run on top of them.
        // It then creates a new link for every connection, along with two flows that run on top.
        //
        Aftermath allocator = new Aftermath(simulator, network) {

            @Override
            public void perform() {

                // Retrieve all the connections
                Collection<Connection> activeConnections = simulator.getActiveConnections();

                // Remove all existing links
                Set<Link> links = new HashSet<>(network.getPresentLinks());
                for (Link l : links) {
                    simulator.removeExistingLink(l.getLinkId());
                }

                // Go over every connection, and re-create a link
                for (Connection conn : activeConnections) {
                    assertTrue(conn.getActiveFlows().size() == 0);

                    // Create link
                    Link link = simulator.addNewLink(conn.getSrcNodeId(), conn.getDstNodeId(), 99);
                    AcyclicPath path = new AcyclicPath();
                    path.add(link);

                    // Add two paths to connection
                    simulator.addFlowToConnection(conn, path);
                    simulator.addFlowToConnection(conn, path);

                    // Allocate the bandwidth for the flow
                    Iterator<Flow> it = conn.getActiveFlows().iterator();
                    simulator.allocateFlowBandwidth(it.next(), 49);
                    simulator.allocateFlowBandwidth(it.next(), 50);

                }

            }

        };

        // Setup simulator
        simulator.setup(network, allocator, new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString()));

        // Start some connections
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new VoidRoutingStrategy(simulator));
        trafficSchedule.addConnectionStartEvent(0, 1, 809090, 0);
        trafficSchedule.addConnectionStartEvent(0, 1, 47373, 44);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());

        // Run the simulator
        simulator.run(10000);

        // No links should remain
        assertEquals(0, network.getPresentLinks().size());

        // Read out the logs
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Bandwidth consistent
        assertEquals(99.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(0L, 8173L)), simulator.getFlowPrecision());
        assertEquals(99.0, testLogReader.getConnectionIdToBandwidthLog().get(1).get(new ImmutablePair<>(44L, 523L)), simulator.getFlowPrecision());

    }

    @Test
    public void testNoFlowsAtAll() throws IOException {
        Simulator simulator = new Simulator();

        // Create new network, simulator and allocator
        Network network = new Network(2);

        // Setup simulator
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString()));

        // Start some connections
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new VoidRoutingStrategy(simulator));
        trafficSchedule.addConnectionStartEvent(0, 1, 809090, 0);
        trafficSchedule.addConnectionStartEvent(0, 1, 47373, 44);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());

        // Run the simulator
        simulator.run(10000);

        // No links are there
        assertEquals(0, network.getPresentLinks().size());

        // Read out the logs
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Bandwidth consistent
        assertEquals(0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(0L, 10000L)), simulator.getFlowPrecision());
        assertEquals(0, testLogReader.getConnectionIdToBandwidthLog().get(1).get(new ImmutablePair<>(44L, 10000L)), simulator.getFlowPrecision());

    }

}
