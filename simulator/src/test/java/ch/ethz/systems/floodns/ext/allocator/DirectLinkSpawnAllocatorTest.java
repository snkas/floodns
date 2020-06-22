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

import ch.ethz.systems.floodns.core.Aftermath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.basicsim.schedule.ConnectionStartEvent;
import ch.ethz.systems.floodns.ext.basicsim.schedule.TrafficSchedule;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import ch.ethz.systems.floodns.ext.routing.VoidRoutingStrategy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class DirectLinkSpawnAllocatorTest {

    @Test
    public void testNewLinkEveryFlow() throws IOException {
        Simulator simulator = new Simulator();

        // Create new network, simulator and allocator
        Network network = new Network(2);
        Aftermath allocator = new DirectLinkSpawnAllocator(simulator, network, 9);
        simulator.setup(network, allocator, new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString()));

        // Start some connections
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new VoidRoutingStrategy(simulator));
        trafficSchedule.addConnectionStartEvent(0, 1, 99, 0);
        trafficSchedule.addConnectionStartEvent(0, 1, 9999999, 10);
        trafficSchedule.addConnectionStartEvent(0, 1, 99, 20);
        trafficSchedule.addConnectionStartEvent(0, 1, 99, 450);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());

        // And one manual to cover simulator function
        ConnectionStartEvent event = new ConnectionStartEvent(simulator, 6747, new Connection(
                simulator,
                network.getNode(0),
                network.getNode(1),
                99
        ), new VoidRoutingStrategy(simulator));
        simulator.insertEvents(event);

        // Run the simulator
        simulator.run(100000);

        // Only one links should remain (allocation does not happen in the last moment, as it does not affect the logs)
        assertEquals(1, network.getPresentLinks().size());

        // Read out the logs
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Bandwidth consistent
        assertEquals(9.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(0L, 11L)), simulator.getFlowPrecision());
        assertEquals(9.0, testLogReader.getConnectionIdToBandwidthLog().get(1).get(new ImmutablePair<>(10L, 100000L)), simulator.getFlowPrecision());
        assertEquals(9.0, testLogReader.getConnectionIdToBandwidthLog().get(2).get(new ImmutablePair<>(20L, 31L)), simulator.getFlowPrecision());
        assertEquals(9.0, testLogReader.getConnectionIdToBandwidthLog().get(3).get(new ImmutablePair<>(450L, 461L)), simulator.getFlowPrecision());
        assertEquals(9.0, testLogReader.getConnectionIdToBandwidthLog().get(4).get(new ImmutablePair<>(6747L, 6758L)), simulator.getFlowPrecision());

    }

}
