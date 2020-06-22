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
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.logger.empty.VoidLoggerFactory;
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

public class ConnectionStartEventTest {

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
    public void testToString() {
        Connection connection = new Connection(simulator, network.getNode(2), network.getNode(5), 3099);
        ConnectionStartEvent event = new ConnectionStartEvent(simulator, 1000, connection, strategy);
        assertEquals("Connection#0[ 2 -> 5; size=(3099.0/3099.0 remaining); flows=[] ] starting at t=1000", event.toString());
    }

}
