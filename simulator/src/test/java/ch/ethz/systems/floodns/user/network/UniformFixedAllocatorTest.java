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

package ch.ethz.systems.floodns.user.network;

import ch.ethz.systems.floodns.core.Aftermath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.UniformFixedAllocator;
import ch.ethz.systems.floodns.ext.basicsim.schedule.ConnectionStartEvent;
import ch.ethz.systems.floodns.ext.basicsim.schedule.TrafficSchedule;
import ch.ethz.systems.floodns.ext.basicsim.topology.FileToTopologyConverter;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import ch.ethz.systems.floodns.ext.routing.EcmpRoutingStrategy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class UniformFixedAllocatorTest {

    @Test
    public void testUniform() throws IOException {
        Simulator simulator = new Simulator();

        // Create new network, simulator and allocator
        Topology topology = FileToTopologyConverter.convert("test_data/1_to_1_capacity_9.properties");
        Network network = topology.getNetwork();

        Aftermath allocator = new UniformFixedAllocator(simulator, network, 0.91);
        simulator.setup(network, allocator, new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString()));

        // Start some connections
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new EcmpRoutingStrategy(simulator, topology, new Random(893289)));
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
        ), new EcmpRoutingStrategy(simulator, topology, new Random(38592859)));
        simulator.insertEvents(event);

        // Run the simulator
        simulator.run(100000);

        // Only one links should remain (allocation does not happen in the last moment, as it does not affect the logs)
        assertEquals(2, network.getPresentLinks().size());

        // Read out the logs
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Bandwidth consistent
        assertEquals(0.91, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(0L, 109L)), simulator.getFlowPrecision());
        assertEquals(0.91, testLogReader.getConnectionIdToBandwidthLog().get(1).get(new ImmutablePair<>(10L, 100000L)), simulator.getFlowPrecision());
        assertEquals(0.91, testLogReader.getConnectionIdToBandwidthLog().get(2).get(new ImmutablePair<>(20L, 129L)), simulator.getFlowPrecision());
        assertEquals(0.91, testLogReader.getConnectionIdToBandwidthLog().get(3).get(new ImmutablePair<>(450L, 559L)), simulator.getFlowPrecision());
        assertEquals(0.91, testLogReader.getConnectionIdToBandwidthLog().get(4).get(new ImmutablePair<>(6747L, 6856L)), simulator.getFlowPrecision());

    }

}
