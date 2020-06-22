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

package ch.ethz.systems.floodns.user.log;

import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.basicsim.schedule.TrafficSchedule;
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;
import org.junit.Test;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static org.junit.Assert.assertEquals;

public class CustomLoggerTest {

    @Test
    public void testCustomLogger() {
        Simulator simulator = new Simulator();

        // Full connected n=4 graph with bi-directional links
        //
        // 0 - - 1
        // | \ / |
        // | / \ |
        // 3 - - 2
        //
        final Network network = new Network(4);
        network.addLink(0, 1, 100);
        network.addLink(1, 2, 100);
        network.addLink(2, 3, 100);
        network.addLink(1, 0, 100);
        network.addLink(2, 1, 100);
        network.addLink(3, 2, 100);
        network.addLink(3, 0, 100);
        network.addLink(0, 3, 100);
        network.addLink(3, 1, 100);
        network.addLink(1, 3, 100);
        network.addLink(0, 2, 100);
        network.addLink(2, 0, 100);

        // Very basic route decider
        RoutingStrategy routeDecider = new RoutingStrategy(simulator) {
            @Override
            public void assignStartFlows(Connection connection) {
                if (connection.getSrcNodeId() == 0 && connection.getDstNodeId() == 3) {
                    simulator.addFlowToConnection(connection, createAcyclicPath(network, "0-1-2-3"));
                } else {
                    throw new RuntimeException("Unsupported flow.");
                }
            }
        };

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, routeDecider);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000, 0);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000, 30);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000000, 50);

        // Initialize logger factory
        CustomLoggerFactory loggerFactory = new CustomLoggerFactory(simulator);

        // Setup and run simulator
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), loggerFactory);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());
        simulator.run(655);

        // Now test all loggers' counters
        assertEquals(20, loggerFactory.a); // For all 4 nodes: 1 period, 2 period, 3 period, 2 period, 1 period
        assertEquals(4, loggerFactory.b);  // For all 4 nodes, save info
        assertEquals(12, loggerFactory.c);  // For all 3 active links: utilization (always 1.0)
        assertEquals(24, loggerFactory.d); // 9 inactive and for all 3 active links active flows: 1 period, 2 period, 3 period, 2 period, 1 period
        assertEquals(12, loggerFactory.e); // For all 12 links: save info
        assertEquals(9, loggerFactory.f);  // Flow 0 1.0; flow 0/1 0.5; flow 0/1/2 0.333, flow 1/2 0.5, flow 2 1.0
        assertEquals(3, loggerFactory.g);  // For all 3 flows: save info
        assertEquals(9, loggerFactory.h);  // Flow 0 1.0; flow 0/1 0.5; flow 0/1/2 0.333, flow 1/2 0.5, flow 2 1.0
        assertEquals(3, loggerFactory.i);  // For all 3 connections: save info

    }

}
