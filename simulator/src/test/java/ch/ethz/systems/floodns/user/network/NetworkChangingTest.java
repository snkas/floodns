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
import ch.ethz.systems.floodns.core.Flow;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static ch.ethz.systems.floodns.PathTestUtility.startFlow;
import static org.junit.Assert.*;

public class NetworkChangingTest {

    @Test
    public void testStarSingleLinkRemoval() {

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

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Start some flows
                List<Flow> flows = new ArrayList<>();
                flows.add(startFlow(simulator, createAcyclicPath(network, "1-0-3")));
                flows.add(startFlow(simulator, createAcyclicPath(network, "1-0-7")));
                flows.add(startFlow(simulator, createAcyclicPath(network, "1-0-7")));
                flows.add(startFlow(simulator, createAcyclicPath(network, "2-0-3")));
                flows.add(startFlow(simulator, createAcyclicPath(network, "8-0-9")));

                // Do flow allocation
                Aftermath allocator = new SimpleMmfAllocator(simulator, network);
                allocator.perform();

                // Check allocation
                assertEquals(flows.get(0).getCurrentBandwidth(), 10.0 / 3.0, simulator.getFlowPrecision());
                assertEquals(flows.get(1).getCurrentBandwidth(), 10.0 / 3.0, simulator.getFlowPrecision());
                assertEquals(flows.get(2).getCurrentBandwidth(), 10.0 / 3.0, simulator.getFlowPrecision());
                assertEquals(flows.get(3).getCurrentBandwidth(), 20.0 / 3.0, simulator.getFlowPrecision());
                assertEquals(flows.get(4).getCurrentBandwidth(), 10.0, simulator.getFlowPrecision());
                assertEquals(1, network.getPresentLinksBetween(0, 3).size());

                // Remove a link
                simulator.removeExistingLink(network.getPresentLinksBetween(0, 3).get(0));

                // Check dimensionality
                assertEquals(3, network.getActiveFlows().size());
                assertEquals(17, network.getPresentLinks().size());

                // Do flow allocation
                allocator.perform();

                // Check allocation after removal
                assertEquals(0, flows.get(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(10.0 / 2.0, flows.get(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(10.0 / 2.0, flows.get(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(0, flows.get(3).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(10.0, flows.get(4).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertTrue(network.getPresentLinksBetween(0, 3).isEmpty());

            }

        });

    }

    @Test
    public void testOnlySingleLinkRemoval() {

        Simulator simulator = new Simulator();

        // 0 - 1
        Network network = new Network(2);
        network.addLink(0, 1, 10.0);


        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Check dimensions
                assertEquals(0, network.getActiveFlows().size());
                assertEquals(1, network.getPresentLinks().size());
                assertEquals(2, network.getNumNodes());

                // Full check of all existence
                assertEquals(1, network.getPresentLinks().size());
                assertEquals(1, network.getPresentLinksBetween(0, 1).size());
                assertEquals(network.getNode(0), network.getPresentLinksBetween(0, 1).get(0).getFromNode());
                assertEquals(network.getNode(1), network.getPresentLinksBetween(0, 1).get(0).getToNode());
                assertTrue(network.getNode(0).hasOutgoingLinksTo(1));
                assertFalse(network.getNode(0).hasIncomingLinksFrom(1));
                assertTrue(network.getNode(1).hasIncomingLinksFrom(0));
                assertFalse(network.getNode(1).hasOutgoingLinksTo(0));
                assertEquals(network.getNode(0).getOutgoingLinksTo(1).get(0), network.getLink(0));
                assertEquals(network.getNode(1).getIncomingLinksFrom(0).get(0), network.getLink(0));
                assertEquals(1, network.getNode(0).getOutgoingConnectedToNodes().size());
                assertEquals(0, network.getNode(1).getOutgoingConnectedToNodes().size());
                assertEquals(0, network.getNode(0).getIncomingConnectedToNodes().size());
                assertEquals(1, network.getNode(1).getIncomingConnectedToNodes().size());

                // Get link identifier and remove it
                int linkId = network.getPresentLinksBetween(0, 1).get(0).getLinkId();
                simulator.removeExistingLink(network.getLink(linkId));

                // Check dimensions
                assertEquals(0, network.getActiveFlows().size());
                assertEquals(0, network.getPresentLinks().size());
                assertEquals(2, network.getNumNodes());

                // Full check of all non-existence
                assertEquals(0, network.getPresentLinks().size());
                assertTrue(network.getPresentLinksBetween(0, 1).isEmpty());
                assertFalse(network.getNode(0).hasOutgoingLinksTo(1));
                assertFalse(network.getNode(0).hasIncomingLinksFrom(1));
                assertFalse(network.getNode(1).hasIncomingLinksFrom(0));
                assertFalse(network.getNode(1).hasOutgoingLinksTo(0));
                assertEquals(0, network.getNode(0).getOutgoingConnectedToNodes().size());
                assertEquals(0, network.getNode(1).getOutgoingConnectedToNodes().size());
                assertEquals(0, network.getNode(0).getIncomingConnectedToNodes().size());
                assertEquals(0, network.getNode(1).getIncomingConnectedToNodes().size());

            }

        });

    }

}
