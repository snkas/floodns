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

import ch.ethz.systems.floodns.core.Flow;
import ch.ethz.systems.floodns.core.Link;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static ch.ethz.systems.floodns.PathTestUtility.startSimpleFlow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NetworkPropertiesTest {

    @Test
    public void testBasicPropertiesStar() {
        Simulator simulator = new Simulator();

        //
        //       9   8
        //        \ /
        //    1--- 0  ...    with bi-directional links
        //        / \
        //       2   3
        //
        //
        Network network = new Network(10);
        for (int i = 1; i < 10; i++) {
            network.addLink(0, i, 10.0);
            network.addLink(i, 0, 10.0);
        }

        // Dimensionality
        assertEquals(10, network.getNodes().size());
        assertEquals(network.getNumNodes(), network.getNodes().size());
        assertEquals(18, network.getPresentLinks().size());
        assertEquals("Network[ |V|=10, |E|=18, |F|=0 ]", network.toString());

        // Links
        for (int i = 1; i < 10; i++) {
            assertEquals(1, network.getPresentLinksBetween(0, i).size());
            assertEquals(1, network.getPresentLinksBetween(i, 0).size());
            assertEquals(1, network.getPresentLinksBetween(network.getNode(0), network.getNode(i)).size());
            assertEquals(1, network.getPresentLinksBetween(network.getNode(i), network.getNode(0)).size());
        }

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Start some flows
                List<Flow> flows = new ArrayList<>();
                flows.add(startSimpleFlow(simulator, network, "1-0-3"));
                flows.add(startSimpleFlow(simulator, network, "1-0-7"));
                flows.add(startSimpleFlow(simulator, network, "1-0-7"));
                flows.add(startSimpleFlow(simulator, network, "2-0-3"));
                flows.add(startSimpleFlow(simulator, network, "8-0-9"));

                // Check all flows are now active for the network
                assertEquals(5, network.getActiveFlows().size());
                assertTrue(network.getActiveFlows().containsAll(flows));
                ArrayList<Integer> list0to4 = new ArrayList<>();
                list0to4.add(0);
                list0to4.add(1);
                list0to4.add(2);
                list0to4.add(3);
                list0to4.add(4);
                assertTrue(network.getActiveFlowsIds().containsAll(list0to4));

                // Check allocation
                assertEquals(flows.get(0).getCurrentBandwidth(), 0, simulator.getFlowPrecision());
                assertEquals(flows.get(1).getCurrentBandwidth(), 0, simulator.getFlowPrecision());
                assertEquals(flows.get(2).getCurrentBandwidth(), 0, simulator.getFlowPrecision());
                assertEquals(flows.get(3).getCurrentBandwidth(), 0, simulator.getFlowPrecision());
                assertEquals(flows.get(4).getCurrentBandwidth(), 0, simulator.getFlowPrecision());

                // Getting a link outside, but knowing its identifier
                Link linkGotten = network.getLink(createAcyclicPath(network, "1-0-3").get(0).getLinkId());
                assertEquals(linkGotten.getFrom(), 1);
                assertEquals(linkGotten.getTo(), 0);

                // Do flow allocation
                new SimpleMmfAllocator(simulator, network).perform();

                // Check allocation
                assertEquals(flows.get(0).getCurrentBandwidth(), 10.0/3.0, simulator.getFlowPrecision());
                assertEquals(flows.get(1).getCurrentBandwidth(), 10.0/3.0, simulator.getFlowPrecision());
                assertEquals(flows.get(2).getCurrentBandwidth(), 10.0/3.0, simulator.getFlowPrecision());
                assertEquals(flows.get(3).getCurrentBandwidth(), 20.0/3.0, simulator.getFlowPrecision());
                assertEquals(flows.get(4).getCurrentBandwidth(), 10.0, simulator.getFlowPrecision());

                // Remove a flow
                simulator.endFlow(flows.get(2));

                // Redo flow allocation
                new SimpleMmfAllocator(simulator, network).perform();

                // Recheck allocation
                assertEquals(5.0, flows.get(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(5.0, flows.get(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(5.0, flows.get(3).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(10.0, flows.get(4).getCurrentBandwidth(), simulator.getFlowPrecision());

                // Remove a flow such that a link becomes inactive
                simulator.endFlow(flows.get(3));

                // Re-redo flow allocation
                new SimpleMmfAllocator(simulator, network).perform();

                // Re-recheck allocation
                assertEquals(flows.get(0).getCurrentBandwidth(), 5.0, simulator.getFlowPrecision());
                assertEquals(flows.get(1).getCurrentBandwidth(), 5.0, simulator.getFlowPrecision());
                assertEquals(flows.get(4).getCurrentBandwidth(), 10.0, simulator.getFlowPrecision());

            }

        });

    }

}
