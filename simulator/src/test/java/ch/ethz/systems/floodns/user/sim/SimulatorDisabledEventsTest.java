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

package ch.ethz.systems.floodns.user.sim;

import ch.ethz.systems.floodns.core.*;
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

public class SimulatorDisabledEventsTest {

    private class FixedTestAllocator extends Aftermath {

        private double bandwidthToAll;

        FixedTestAllocator(Simulator simulator, Network network, double startBandwidth) {
            super(simulator, network);
            bandwidthToAll = startBandwidth;
        }

        void changeBandwidth(double newBandwidth) {
            this.bandwidthToAll = newBandwidth;
        }

        @Override
        public void perform() {
            for (Flow f : network.getActiveFlows()) {
                simulator.allocateFlowBandwidth(f, bandwidthToAll);
            }
        }

    }

    private class ChangeAllBandwidthTestEvent extends Event {

        private FixedTestAllocator allocator;
        private double newBandwidth;

        ChangeAllBandwidthTestEvent(Simulator simulator, long timeFromNow, FixedTestAllocator allocator, double newBandwidth) {
            super(simulator, 0, timeFromNow);
            this.allocator = allocator;
            this.newBandwidth = newBandwidth;
        }

        @Override
        public void trigger() {
            this.allocator.changeBandwidth(newBandwidth);
        }

    }

    @Test
    public void testDisabledEventAsLast() throws IOException {

        Simulator simulator = new Simulator();

        // 0 -- 1
        Topology topology = FileToTopologyConverter.convert("test_data/1_to_1.properties", 10);
        Network network = topology.getNetwork();

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new EcmpRoutingStrategy(simulator, topology, new Random(229)));
        trafficSchedule.addConnectionStartEvent(0, 1, 10000000, 0);

        // Allocator
        FixedTestAllocator allocator = new FixedTestAllocator(simulator, network, 10);

        // Setup simulator
        simulator.setup(
                network,
                allocator,
                new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString())
        );
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());

        // Insert bandwidth change events
        ChangeAllBandwidthTestEvent eventA = new ChangeAllBandwidthTestEvent(simulator, 1000, allocator, 7);
        ChangeAllBandwidthTestEvent eventB = new ChangeAllBandwidthTestEvent(simulator, 3006, allocator, 6);
        ChangeAllBandwidthTestEvent eventC = new ChangeAllBandwidthTestEvent(simulator, 3006, allocator, 4);
        simulator.insertEvents(eventA, eventB, eventC);
        simulator.cancelEvent(eventC);

        // Run simulator
        simulator.run(1000000L);

        // Read out the logs
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Bandwidth consistent
        assertEquals(10.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(0L, 1000L)), simulator.getFlowPrecision());
        assertEquals(7.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(1000L, 3006L)), simulator.getFlowPrecision());
        assertEquals(6.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(3006L, 1000000L)), simulator.getFlowPrecision());

    }

    @Test
    public void testDisabledEventBetweenLast() throws IOException {

        Simulator simulator = new Simulator();

        // 0 -- 1
        Topology topology = FileToTopologyConverter.convert("test_data/1_to_1.properties", 10);
        Network network = topology.getNetwork();

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new EcmpRoutingStrategy(simulator, topology, new Random(229)));
        trafficSchedule.addConnectionStartEvent(0, 1, 113000, 0);

        // Allocator
        FixedTestAllocator allocator = new FixedTestAllocator(simulator, network, 10);

        // Setup simulator
        simulator.setup(
                network,
                allocator,
                new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString())
        );
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());

        // Insert bandwidth change events
        ChangeAllBandwidthTestEvent eventA = new ChangeAllBandwidthTestEvent(simulator, 1000, allocator, 7);
        ChangeAllBandwidthTestEvent eventB = new ChangeAllBandwidthTestEvent(simulator, 3006, allocator, 4);
        simulator.insertEvents(eventA);
        simulator.insertEvents(eventB);
        simulator.cancelEvent(eventB);

        // Run simulator
        simulator.run(100000L);

        // Read out the logs
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Bandwidth consistent
        assertEquals(10.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(0L, 1000L)), simulator.getFlowPrecision());
        assertEquals(7.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(1000L, 15715L)), simulator.getFlowPrecision());

    }

}
