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

import ch.ethz.systems.floodns.core.*;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static ch.ethz.systems.floodns.PathTestUtility.startSimpleFlow;
import static org.junit.Assert.*;

public class NetworkBasicTest {

    @Test
    public void testCross() {
        Simulator simulator = new Simulator();
        Network network = new Network(5);

        // 0   1
        //  \ /
        //   4
        //  / \
        // 2   3
        network.addLink(0, 4, 100);
        network.addLink(4, 0, 100);
        network.addLink(1, 4, 100);
        network.addLink(4, 1, 100);
        network.addLink(2, 4, 100);
        network.addLink(4, 2, 100);
        network.addLink(3, 4, 100);
        network.addLink(4, 3, 100);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add two connections and their flows
                AcyclicPath path0 = createAcyclicPath(network, "0-4-3");
                AcyclicPath path1 = createAcyclicPath(network, "2-4-1");
                Connection conn0 = new Connection(simulator, path0.getSrcNode(), path0.getDstNode(), 1000);
                Connection conn1 = new Connection(simulator, path1.getSrcNode(), path1.getDstNode(), 1000);
                simulator.activateConnection(conn0);
                simulator.activateConnection(conn1);
                simulator.addFlowToConnection(conn0, path0);
                simulator.addFlowToConnection(conn1, path1);

                new SimpleMmfAllocator(simulator, network).perform();

                // Check bandwidth
                assertEquals(100, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(100, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());

                // Check flow occupancy
                assertEquals(1, network.getNode(0).getActiveFlowsIds().size());
                assertTrue(network.getNode(0).getActiveFlowsIds().contains(0));
                assertEquals(1, network.getNode(1).getActiveFlowsIds().size());
                assertTrue(network.getNode(1).getActiveFlowsIds().contains(1));
                assertEquals(1, network.getNode(2).getActiveFlowsIds().size());
                assertTrue(network.getNode(2).getActiveFlowsIds().contains(1));
                assertEquals(1, network.getNode(3).getActiveFlowsIds().size());
                assertTrue(network.getNode(3).getActiveFlowsIds().contains(0));
                assertEquals(2, network.getNode(4).getActiveFlowsIds().size());
                assertTrue(network.getNode(4).getActiveFlowsIds().contains(0));
                assertTrue(network.getNode(4).getActiveFlowsIds().contains(1));

                // Clear the network of all flows
                assertTrue(network.isFlowActive(0));
                simulator.endFlow(network.getActiveFlow(0));
                assertFalse(network.isFlowActive(0));
                simulator.endFlow(network.getActiveFlow(1));
                for (Node n : network.getNodes()) {
                    assertEquals(0, n.getActiveFlows().size());
                }
                assertEquals(0, network.getActiveFlows().size());
                for (Link link : network.getPresentLinks()) {
                    assertEquals(0, link.getActiveFlowIds().size());
                    assertEquals(0, link.getActiveFlows().size());
                }

            }

        });

    }

    @Test
    public void testOneLinkManyFlows() {
        Simulator simulator = new Simulator();
        Network network = new Network(2);

        // 0 - 1
        network.addLink(0, 1, 333.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Create flows with paths
                List<Flow> flows = new ArrayList<>();
                for (int i = 0; i < 1007; i++) {
                    flows.add(startSimpleFlow(simulator, network, "0-1"));
                }

                // Perform allocation
                new SimpleMmfAllocator(simulator, network).perform();

                // Check bandwidth
                for (int i = 0; i < 1007; i++) {
                    assertEquals(333.0 / 1007.0, flows.get(i).getCurrentBandwidth(), simulator.getFlowPrecision());
                }

            }

        });

    }

    @Test
    public void testSingleBidirectionalLinkUsed() {
        Simulator simulator = new Simulator();
        Network network = new Network(2);

        // 0 - 1
        network.addLink(0, 1, 100);
        network.addLink(1, 0, 100);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Create flows with paths
                List<Flow> flows = new ArrayList<>();
                flows.add(startSimpleFlow(simulator, network, "0-1"));
                flows.add(startSimpleFlow(simulator, network, "1-0"));

                // Perform allocation
                new SimpleMmfAllocator(simulator, network).perform();

                // Check bandwidth
                for (int i = 0; i < 2; i++) {
                    assertEquals(100.0, flows.get(i).getCurrentBandwidth(), simulator.getFlowPrecision());
                }

            }

        });

    }

    @Test
    public void testSingleLinkUsed() {
        Simulator simulator = new Simulator();
        Network network = new Network(2);

        // 0 - 1
        network.addLink(0, 1, 0.4);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add flows
                Flow f0 = startSimpleFlow(simulator, network, "0-1");

                // Perform allocation
                new SimpleMmfAllocator(simulator, network).perform();

                // Check bandwidth
                assertEquals(0.4, f0.getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testSingleLinkUnused() {
        Simulator simulator = new Simulator();
        Network network = new Network(2);

        // 0 - 1
        network.addLink(0, 1, 100);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Perform allocation
                new SimpleMmfAllocator(simulator, network).perform();
                assertTrue(network.getPresentLinksBetween(0, 1).get(0).getActiveFlows().size() == 0);
                assertEquals(100, network.getPresentLinksBetween(0, 1).get(0).getRemainderCapacity(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testSingleNode() {
        Simulator simulator = new Simulator();
        Network network = new Network(1);

        // 0

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Perform allocation
                new SimpleMmfAllocator(simulator, network).perform();

            }

        });

    }

    @Test
    public void testEmpty() {
        Simulator simulator = new Simulator();
        Network network = new Network(0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                new SimpleMmfAllocator(simulator, network).perform();

            }

        });

    }

}
