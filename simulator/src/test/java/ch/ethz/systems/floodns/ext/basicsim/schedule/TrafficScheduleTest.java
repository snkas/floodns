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

package ch.ethz.systems.floodns.ext.basicsim.schedule;

import ch.ethz.systems.floodns.TestNetworkCreator;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Event;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.logger.empty.VoidLoggerFactory;
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

public class TrafficScheduleTest {

    private Network network;
    private Simulator simulator;

    @Mock
    private RoutingStrategy strategy;

    @Before
    public void setup() {
        network = TestNetworkCreator.star(10, 7, 10.0);
        simulator = new Simulator();
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), new VoidLoggerFactory(simulator));
    }

    @Test
    public void testScheduleCreation() {

        // Create schedule
        TrafficSchedule schedule = new TrafficSchedule(simulator, network, strategy);
        int times[] = new int[]{111, 300, 222, 0};
        int sources[] = new int[]{3, 3, 3, 9};
        int destinations[] = new int[]{4, 5, 5, 5};
        int fs[] = new int[]{10000, 5737, 10000, 10000};
        schedule.addConnectionStartEvent(sources[0], destinations[0], fs[0], times[0]);
        schedule.addConnectionStartEvent(sources[1], destinations[1], fs[1], times[1]);
        schedule.addConnectionStartEvent(sources[2], destinations[2], fs[2], times[2]);
        schedule.addConnectionStartEvent(sources[3], destinations[3], fs[3], times[3]);

        // Check dimensions
        assertEquals(4, schedule.getConnectionStartEvents().size());
        assertEquals(4, schedule.getConnections().size());

        // Check connection instances
        int i = 0;
        for (Connection connection : schedule.getConnections()) {
            assertEquals(i, connection.getConnectionId());
            assertEquals(sources[i], connection.getSrcNodeId());
            assertEquals(destinations[i], connection.getDstNodeId());
            assertEquals(fs[i], connection.getRemainder(), 0.0);
            i++;
        }

        // Check the flow start events
        int j = 0;
        for (Event event : schedule.getConnectionStartEvents()) {
            assertEquals(times[j], event.getTime());
            j++;
        }

    }

}
