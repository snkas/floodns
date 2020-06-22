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

import static org.junit.Assert.assertEquals;

public class ChangingAllocatorTest {

    @Test
    public void testChangingEveryTime() throws IOException {
        Simulator simulator = new Simulator();

        // Create new network, simulator and allocator
        Network network = new Network(2);
        network.addLink(0, 1, 99.0);

        // Change allocator
        //
        // This allocator removes a flow, and then adds a new flow again, for every connection,
        // for every time an allocation is performed. This is to demonstrate the uf.removeFlow()
        // usage and how it should be used in combination with the simulator.
        //
        Aftermath allocator = new Allocator(simulator, network) {

            @Override
            public void perform() {

                // Retrieve all the connections
                Collection<Connection> activeConnections = simulator.getActiveConnections();

                // Go over every connection, and re-create a link
                for (Connection conn : activeConnections) {

                    // Remove the flow
                    for (Flow f : conn.getActiveFlows()) {
                        simulator.endFlow(f);
                    }

                    // Create link
                    AcyclicPath path = new AcyclicPath();
                    path.add(network.getLink(0));

                    // Add new path to connection
                    simulator.addFlowToConnection(conn, path);

                    // Allocate the bandwidth for the flow
                    simulator.allocateFlowBandwidth(conn.getActiveFlows().iterator().next(), 30);

                }

            }

        };

        simulator.setup(network, allocator, new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString()));

        // Start some connections
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new VoidRoutingStrategy(simulator));
        trafficSchedule.addConnectionStartEvent(0, 1, 809090, 0);
        trafficSchedule.addConnectionStartEvent(0, 1, 47373, 44);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());

        // Run the simulator
        simulator.run(20000);

        // Read out the logs
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Bandwidth consistent
        assertEquals(30.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(0L, 20000L)), simulator.getFlowPrecision());
        assertEquals(30.0, testLogReader.getConnectionIdToBandwidthLog().get(1).get(new ImmutablePair<>(44L, 1624L)), simulator.getFlowPrecision());

    }

}
