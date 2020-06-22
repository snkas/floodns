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

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static ch.ethz.systems.floodns.PathTestUtility.startFlow;
import static org.junit.Assert.*;

public class NetworkIllegalTest {

    @Test
    public void testInvalidEndFlowAgain() {
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

                // Add flows
                Flow f0 = startFlow(simulator, createAcyclicPath(network, "0-4-3"));

                // Perform allocation
                new SimpleMmfAllocator(simulator, network).perform();

                // End flow
                assertEquals(f0, network.getActiveFlow(0));
                simulator.endFlow(f0);

                // End again
                boolean thrown = false;
                try {
                    simulator.endFlow(f0);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

            }

        });

    }

    @Test
    public void testEmptyPathStartFlow() {
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

                // End again
                boolean thrown = false;
                AcyclicPath path = new AcyclicPath();
                Connection conn = new Connection(simulator, network.getNode(0), network.getNode(1), 1000);
                simulator.activateConnection(conn);
                try {
                    simulator.addFlowToConnection(conn, path);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

            }

        });

    }

    @Test
    public void testInvalidAddLinkIndexFrom() {
        Network network = new Network(5);
        boolean thrown = false;
        try {
            network.addLink(-4, 4, 100);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testInvalidAddLinkIndexTo() {
        Network network = new Network(5);
        boolean thrown = false;
        try {
            network.addLink(0, 5, 100);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testInvalidAddLinkCapacity() {
        Network network = new Network(5);
        boolean thrown = false;
        try {
            network.addLink(0, 3, 0);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testInvalidDoubleRemoveLink() {
        Simulator simulator = new Simulator();
        Network network = new Network(5);
        network.addLink(0, 3, 55);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                Link link = network.getLink(0);
                simulator.removeExistingLink(link);
                boolean thrown = false;
                try {
                    simulator.removeExistingLink(link);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

            }

        });
    }

    @Test
    public void testExcessiveAllocationAttempt() {
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

                // Add flows
                Flow f0 = startFlow(simulator, createAcyclicPath(network, "0-4-3"));

                // Illegal excessive allocation
                boolean thrown = false;
                try {
                    simulator.allocateFlowBandwidth(f0, 101);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

            }

        });

    }

    @Test
    public void testZeroAllocationValid() {
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

                // Add flows
                Flow f0 = startFlow(simulator, createAcyclicPath(network, "0-4-3"));

                // Zero bandwidth allocation
                simulator.allocateFlowBandwidth(f0, 0);

            }

        });

    }

    @Test
    public void testNegativeAllocationAttempt() {
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

                // Add flows
                Flow f0 = startFlow(simulator, createAcyclicPath(network, "0-4-3"));

                // Illegal negative allocation
                boolean thrown = false;
                try {
                    simulator.allocateFlowBandwidth(f0, -1);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

            }

        });

    }

    @Test
    public void testCanAllocate() {
        Simulator simulator = new Simulator();
        Network network = new Network(5);

        // 0   1
        //  \ /
        //   4
        //  / \
        // 2   3
        network.addLink(0, 4, 30);
        network.addLink(4, 0, 13);
        network.addLink(1, 4, 1);
        network.addLink(4, 1, 10000);
        network.addLink(2, 4, 70);
        network.addLink(4, 2, 100);
        network.addLink(3, 4, 100);
        network.addLink(4, 3, 100);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add flows
                Flow f0 = startFlow(simulator, createAcyclicPath(network, "0-4-3"));
                Flow f1 = startFlow(simulator, createAcyclicPath(network, "2-4"));

                // Zero bandwidth allocation
                assertTrue(f0.canAllocateBandwidth(30));
                assertTrue(f0.canAllocateBandwidth(10));
                assertTrue(f0.canAllocateBandwidth(0));
                assertFalse(f0.canAllocateBandwidth(31));
                assertTrue(f1.canAllocateBandwidth(70));
                assertFalse(f1.canAllocateBandwidth(71));
                assertTrue(f1.canAllocateBandwidth(0));
                assertFalse(f1.canAllocateBandwidth(-1));

            }


        });

    }

}
