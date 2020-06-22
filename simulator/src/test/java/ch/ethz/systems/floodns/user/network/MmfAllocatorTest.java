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

import ch.ethz.systems.floodns.PathTestUtility;
import ch.ethz.systems.floodns.core.*;
import ch.ethz.systems.floodns.ext.allocator.Allocator;
import ch.ethz.systems.floodns.ext.allocator.GenericMmfAllocator;
import ch.ethz.systems.floodns.ext.allocator.UpperLimitMmfAllocator;
import org.junit.Test;

import java.util.*;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static ch.ethz.systems.floodns.PathTestUtility.startFlow;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class MmfAllocatorTest {

    private abstract class FuncAllocator {
        abstract Allocator generate(Simulator simulator, Network network, Map<Flow, Double> flowToUpperLimit);
    }

    private class GenericFuncAllocator extends FuncAllocator {

        @Override
        Allocator generate(Simulator simulator, Network network, Map<Flow, Double> flowToUpperLimit) {
            return new GenericMmfAllocator(simulator, network, flowToUpperLimit, null);
        }

    }

    private class ULFuncAllocator extends FuncAllocator {

        @Override
        Allocator generate(Simulator simulator, Network network, Map<Flow, Double> flowToUpperLimit) {
            return new UpperLimitMmfAllocator(simulator, network, flowToUpperLimit);
        }

    }


    @Test
    public void testSingleNoLimitAndExactlySufficient() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 -> 1
        //DEFAULT_RUNTIME
        network.addLink(0, 1, 10.0);

        // Calling runtime outside of run is not permitted as it is not defined yet
        try {
            simulator.getTotalRuntime();
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Test that the runtime is conveyed properly
                assertEquals(NetworkTestHelper.DEFAULT_RUNTIME, simulator.getTotalRuntime());

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");

                Map<Flow, Double> flowToWeight = new HashMap<>();
                flowToWeight.put(network.getActiveFlow(0), 2.0);
                flowToWeight.put(network.getActiveFlow(1), 2.6);
                flowToWeight.put(network.getActiveFlow(2), 2.7);
                flowToWeight.put(network.getActiveFlow(3), 2.7);

                new GenericMmfAllocator(simulator, network, null, flowToWeight).perform();

                // Check bandwidth
                assertEquals(2.0, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(2.6, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(2.7, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(2.7, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testSingleUnweighted() {

        for (final FuncAllocator funcAllocator : new FuncAllocator[]{new GenericFuncAllocator(), new ULFuncAllocator()}) {

            Simulator simulator = new Simulator();
            Network network = new Network(6);

            //
            // 0 -> 1
            //
            network.addLink(0, 1, 10.0);

            NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

                @Override
                public void test() {

                    // Flows
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1");

                    Map<Flow, Double> flowToUpperLimit = new HashMap<>();
                    flowToUpperLimit.put(network.getActiveFlow(0), 2.0);
                    flowToUpperLimit.put(network.getActiveFlow(1), 2.6);
                    flowToUpperLimit.put(network.getActiveFlow(2), 4.0);
                    flowToUpperLimit.put(network.getActiveFlow(3), 5.0);

                    funcAllocator.generate(simulator, network, flowToUpperLimit).perform();

                    // Check bandwidth
                    assertEquals(2.0, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                    assertEquals(2.6, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                    assertEquals(2.7, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                    assertEquals(2.7, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());

                }

            });

        }

    }

    @Test
    public void testSingleWeighted() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 -> 1
        //
        network.addLink(0, 1, 16.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");

                Map<Flow, Double> flowToUpperLimit = new HashMap<>();
                flowToUpperLimit.put(network.getActiveFlow(0), 4.0);
                flowToUpperLimit.put(network.getActiveFlow(1), 2.0);
                flowToUpperLimit.put(network.getActiveFlow(2), 10.0);
                flowToUpperLimit.put(network.getActiveFlow(3), 4.0);

                Map<Flow, Double> flowToWeight = new HashMap<>();
                flowToWeight.put(network.getActiveFlow(0), 2.5);
                flowToWeight.put(network.getActiveFlow(1), 4.0);
                flowToWeight.put(network.getActiveFlow(2), 0.5);
                flowToWeight.put(network.getActiveFlow(3), 1.0);

                new GenericMmfAllocator(simulator, network, flowToUpperLimit, flowToWeight).perform();

                // Check bandwidth
                assertEquals(4.0, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(2.0, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(6.0, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(4.0, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testDoubleWeighted() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 -> 1 -> 2
        //
        network.addLink(0, 1, 16.0);
        network.addLink(1, 2, 16.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "1-2");
                PathTestUtility.startSimpleFlow(simulator, network, "1-2");
                PathTestUtility.startSimpleFlow(simulator, network, "1-2");
                PathTestUtility.startSimpleFlow(simulator, network, "1-2");

                Map<Flow, Double> flowToUpperLimit = new HashMap<>();
                flowToUpperLimit.put(network.getActiveFlow(0), 4.0);
                flowToUpperLimit.put(network.getActiveFlow(1), 2.0);
                flowToUpperLimit.put(network.getActiveFlow(2), 10.0);
                flowToUpperLimit.put(network.getActiveFlow(3), 4.0);
                flowToUpperLimit.put(network.getActiveFlow(4), 4.0);
                flowToUpperLimit.put(network.getActiveFlow(5), 2.0);
                flowToUpperLimit.put(network.getActiveFlow(6), 10.0);
                flowToUpperLimit.put(network.getActiveFlow(7), 4.0);

                Map<Flow, Double> flowToWeight = new HashMap<>();
                flowToWeight.put(network.getActiveFlow(0), 2.5);
                flowToWeight.put(network.getActiveFlow(1), 4.0);
                flowToWeight.put(network.getActiveFlow(2), 0.5);
                flowToWeight.put(network.getActiveFlow(3), 1.0);
                flowToWeight.put(network.getActiveFlow(4), 2.5);
                flowToWeight.put(network.getActiveFlow(5), 4.0);
                flowToWeight.put(network.getActiveFlow(6), 0.5);
                flowToWeight.put(network.getActiveFlow(7), 1.0);

                new GenericMmfAllocator(simulator, network, flowToUpperLimit, flowToWeight).perform();

                // Check bandwidth
                assertEquals(4.0, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(2.0, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(6.0, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(4.0, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(4.0, network.getActiveFlow(4).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(2.0, network.getActiveFlow(5).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(6.0, network.getActiveFlow(6).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(4.0, network.getActiveFlow(7).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testDoubleWeighted2() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 -> 1 -> 2
        //
        network.addLink(0, 1, 5.0);
        network.addLink(1, 2, 10.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1-2");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "1-2");

                Map<Flow, Double> flowToUpperLimit = new HashMap<>();
                flowToUpperLimit.put(network.getActiveFlow(0), 100.0);
                flowToUpperLimit.put(network.getActiveFlow(1), 100.0);
                flowToUpperLimit.put(network.getActiveFlow(2), 100.0);

                Map<Flow, Double> flowToWeight = new HashMap<>();
                flowToWeight.put(network.getActiveFlow(0), 10.0);
                flowToWeight.put(network.getActiveFlow(1), 10.0);
                flowToWeight.put(network.getActiveFlow(2), 1.0);

                new GenericMmfAllocator(simulator, network, flowToUpperLimit, flowToWeight).perform();

                // Check bandwidth
                assertEquals(2.5, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(2.5, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(7.5, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testDoubleWeighted3() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 -> 1 -> 2
        //
        network.addLink(0, 1, 10.0);
        network.addLink(1, 2, 5.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1-2");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");

                Map<Flow, Double> flowToWeight = new HashMap<>();
                flowToWeight.put(network.getActiveFlow(0), 9.0);
                flowToWeight.put(network.getActiveFlow(1), 1.0);

                new GenericMmfAllocator(simulator, network, null, flowToWeight).perform();

                // Check bandwidth
                assertEquals(5, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(5, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testDouble() {

        for (final FuncAllocator funcAllocator : new FuncAllocator[]{new GenericFuncAllocator(), new ULFuncAllocator()}) {

            Simulator simulator = new Simulator();
            Network network = new Network(6);

            //
            // 0 -> 1 -> 2
            //
            network.addLink(0, 1, 10.0);
            network.addLink(1, 2, 10.0);

            NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

                @Override
                public void test() {

                    // Flows
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1-2");
                    PathTestUtility.startSimpleFlow(simulator, network, "1-2");

                    Map<Flow, Double> flowToUpperLimit = new HashMap<>();
                    flowToUpperLimit.put(network.getActiveFlow(0), 1.0);
                    flowToUpperLimit.put(network.getActiveFlow(1), 1.0);
                    flowToUpperLimit.put(network.getActiveFlow(2), 10000.0);
                    flowToUpperLimit.put(network.getActiveFlow(3), 10000.0);

                    funcAllocator.generate(simulator, network, flowToUpperLimit).perform();

                    // Check bandwidth
                    assertEquals(1.0, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                    assertEquals(1.0, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                    assertEquals(5.0, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                    assertEquals(5.0, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());

                }

            });

        }

    }

    @Test
    public void testFive() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        //           5
        //           |
        // 0 -- 1 -- 3 -- 4
        //      |
        //      2
        //
        network.addLink(0, 1, 4);
        network.addLink(1, 0, 4);
        network.addLink(1, 2, 10);
        network.addLink(2, 1, 10);
        network.addLink(1, 3, 20);
        network.addLink(3, 1, 20);
        network.addLink(3, 5, 10);
        network.addLink(5, 3, 10);
        network.addLink(3, 4, 6);
        network.addLink(4, 3, 6);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1-3-4");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1-2");
                PathTestUtility.startSimpleFlow(simulator, network, "5-3-4");
                PathTestUtility.startSimpleFlow(simulator, network, "5-3-4");


                Map<Flow, Double> flowToUpperLimit = new HashMap<>();
                flowToUpperLimit.put(network.getActiveFlow(0), 10000.0);
                flowToUpperLimit.put(network.getActiveFlow(1), 2.2);
                flowToUpperLimit.put(network.getActiveFlow(2), 10000.0);
                flowToUpperLimit.put(network.getActiveFlow(3), 10000.0);

                Map<Flow, Double> flowToWeight = new HashMap<>();
                flowToWeight.put(network.getActiveFlow(0), 1.0);
                flowToWeight.put(network.getActiveFlow(1), 2.2);
                flowToWeight.put(network.getActiveFlow(2), 12.0);
                flowToWeight.put(network.getActiveFlow(3), 3.0);

                new GenericMmfAllocator(simulator, network, flowToUpperLimit, flowToWeight).perform();

                // Check bandwidth
                assertEquals(0.375, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(2.2, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(4.5, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(1.125, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testThreeSplitHeterogeneous() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //         2
        //       /   \
        // 0 - 1 - 3 - 5
        //       \   /
        //         4
        network.addLink(0, 1, 100);
        network.addLink(1, 2, 35);
        network.addLink(1, 3, 30);
        network.addLink(1, 4, 30);
        network.addLink(2, 5, 60);
        network.addLink(3, 5, 60);
        network.addLink(4, 5, 20);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                startFlow(simulator, createAcyclicPath(network, "0-1-2-5"));
                startFlow(simulator, createAcyclicPath(network, "0-1-3-5"));
                startFlow(simulator, createAcyclicPath(network, "0-1-4-5"));
                new GenericMmfAllocator(simulator, network, null, null).perform();
                assertEquals(35, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(30, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(20, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testThreeSplitWithFour() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //         2
        //       /   \
        // 0 - 1 - 3 - 5
        //       \   /
        //         4
        network.addLink(0, 1, 100);
        network.addLink(1, 2, 35);
        network.addLink(1, 3, 35);
        network.addLink(1, 4, 35);
        network.addLink(2, 5, 60);
        network.addLink(3, 5, 60);
        network.addLink(4, 5, 60);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                startFlow(simulator, createAcyclicPath(network, "0-1-2-5"));
                startFlow(simulator, createAcyclicPath(network, "0-1-2-5"));
                startFlow(simulator, createAcyclicPath(network, "0-1-3-5"));
                startFlow(simulator, createAcyclicPath(network, "0-1-4-5"));
                new GenericMmfAllocator(simulator, network, null, null).perform();
                assertEquals(17.5, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(17.5, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(32.5, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(32.5, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testThreeSplit() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //         2
        //       /   \
        // 0 - 1 - 3 - 5
        //       \   /
        //         4
        network.addLink(0, 1, 100);
        network.addLink(1, 2, 35);
        network.addLink(1, 3, 35);
        network.addLink(1, 4, 35);
        network.addLink(2, 5, 60);
        network.addLink(3, 5, 60);
        network.addLink(4, 5, 60);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                startFlow(simulator, createAcyclicPath(network, "0-1-2-5"));
                startFlow(simulator, createAcyclicPath(network, "0-1-3-5"));
                startFlow(simulator, createAcyclicPath(network, "0-1-4-5"));
                new GenericMmfAllocator(simulator, network, null, null).perform();
                assertEquals(100.0/3.0, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(100.0/3.0, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(100.0/3.0, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testNonSumOptimality() {
        Simulator simulator = new Simulator();
        Network network = new Network(4);

        //     1
        //   / | \
        // 0   |  3
        //   \ | /
        //     2
        network.addLink(0, 1, 10);
        network.addLink(1, 2, 1);
        network.addLink(1, 3, 10);
        network.addLink(0, 2, 10);
        network.addLink(2, 3, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                startFlow(simulator, createAcyclicPath(network, "0-1-3"));
                startFlow(simulator, createAcyclicPath(network, "0-1-2-3"));
                startFlow(simulator, createAcyclicPath(network, "0-2-3"));

                new GenericMmfAllocator(simulator, network, null, null).perform();

                // Check bandwidth
                assertEquals(9, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(1, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(9, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());

                // Check utilization
                assertEquals(1.0, network.getPresentLinksBetween(0, 1).get(0).getUtilization(), simulator.getFlowPrecision());
                assertEquals(1.0, network.getPresentLinksBetween(1, 2).get(0).getUtilization(), simulator.getFlowPrecision());
                assertEquals(0.9, network.getPresentLinksBetween(0, 2).get(0).getUtilization(), simulator.getFlowPrecision());
                assertEquals(0.9, network.getPresentLinksBetween(1, 3).get(0).getUtilization(), simulator.getFlowPrecision());
                assertEquals(1.0, network.getPresentLinksBetween(2, 3).get(0).getUtilization(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testWithIdleLink() {
        Simulator simulator = new Simulator();
        Network network = new Network(4);

        // 0
        //   \
        //     2 - 3
        //   /
        // 1
        network.addLink(0, 2, 100);
        network.addLink(1, 2, 100);
        network.addLink(2, 3, 100);


        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                startFlow(simulator, createAcyclicPath(network, "0-2-3"));
                new GenericMmfAllocator(simulator, network, null, null).perform();

                // Check bandwidth
                assertEquals(100, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testAllCircles() {
        for (int i = 1; i < 11; i++) {
            testCircleFlowConvergence(i);
        }

    }

    public void testCircleFlowConvergence(final int hops) {
        Simulator simulator = new Simulator();
        Network network = new Network(11);

        //      0 - 1 - 2 - 3 - 4
        //     /                |
        //    10 - 9 - 8 - ... /
        //
        for (int i = 0; i < 11; i++) {
            network.addLink(i, (i + 1) % 11, 10);
        }

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Create flows
                List<AcyclicPath> flowPaths = new ArrayList<>();
                for (int i = 0; i < 11; i++) {
                    StringBuilder s = new StringBuilder();
                    for (int j = 0; j < hops + 1; j++) {
                        s.append((i + j) % 11);
                        s.append("-");
                    }
                    flowPaths.add(createAcyclicPath(network, s.substring(0, s.length() - 1)));
                }

                // Add flows
                for (int i = 0; i < 11; i++) {
                    Connection connection = new Connection(simulator, flowPaths.get(i).getSrcNode(), flowPaths.get(i).getDstNode(), 1000);
                    simulator.activateConnection(connection);
                    simulator.addFlowToConnection(connection, flowPaths.get(i));
                }

                new GenericMmfAllocator(simulator, network, null, null).perform();

                // Check bandwidth
                for (Flow f : network.getActiveFlows()) {
                    assertEquals(10.0 / hops, f.getCurrentBandwidth(), simulator.getFlowPrecision());
                }

            }

        });

    }

    @Test
    public void testAllDuals() {
        testDualFlowConvergence(100, 100, 100, 50, 50);
        testDualFlowConvergence(25, 50, 75, 25, 50);
        testDualFlowConvergence(10, 10, 100, 10, 10);
        testDualFlowConvergence(30, 40, 100, 30, 40);
        testDualFlowConvergence(60, 50, 100, 50, 50);
        testDualFlowConvergence(100, 100, 10, 5, 5);
        testDualFlowConvergence(0.1, 0.1, 0.05, 0.025, 0.025);
        testDualFlowConvergence(0.1, 0.8, 0.6, 0.1, 0.5);
    }

    public void testDualFlowConvergence(double cap02, double cap12, double cap23, final double expFlow0, final double expFlow1) {
        Simulator simulator = new Simulator();
        Network network = new Network(4);

        // 0
        //   \
        //     2 - 3
        //   /
        // 1
        network.addLink(0, 2, cap02);
        network.addLink(1, 2, cap12);
        network.addLink(2, 3, cap23);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add two connections and their flows
                AcyclicPath path0 = createAcyclicPath(network, "0-2-3");
                AcyclicPath path1 = createAcyclicPath(network, "1-2-3");
                Connection conn0 = new Connection(simulator, path0.getSrcNode(), path0.getDstNode(), 1000);
                Connection conn1 = new Connection(simulator, path1.getSrcNode(), path1.getDstNode(), 1000);
                simulator.activateConnection(conn0);
                simulator.activateConnection(conn1);
                simulator.addFlowToConnection(conn0, path0);
                simulator.addFlowToConnection(conn1, path1);

                new GenericMmfAllocator(simulator, network, null, null).perform();

                // Check bandwidth
                assertEquals(expFlow0, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(expFlow1, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(expFlow0, conn0.getTotalBandwidth(), simulator.getFlowPrecision());
                assertEquals(expFlow1, conn1.getTotalBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testAddRemoveFlowsStatic() {
        Simulator simulator = new Simulator();
        Network network = new Network(4);

        // 0
        //   \
        //     2 - 3
        //   /
        // 1
        network.addLink(0, 2, 10);
        network.addLink(1, 2, 10);
        network.addLink(2, 3, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add two connections and their flows
                AcyclicPath path0 = createAcyclicPath(network, "0-2-3");
                AcyclicPath path1 = createAcyclicPath(network, "1-2-3");
                Connection conn0 = new Connection(simulator, path0.getSrcNode(), path0.getDstNode(), 1000);
                Connection conn1 = new Connection(simulator, path1.getSrcNode(), path1.getDstNode(), 1000);
                simulator.activateConnection(conn0);
                simulator.activateConnection(conn1);
                simulator.addFlowToConnection(conn0, path0);
                simulator.addFlowToConnection(conn1, path1);

                for (Flow f : conn0.getActiveFlows()) {
                    simulator.endFlow(f);
                }

                new GenericMmfAllocator(simulator, network, null, null).perform();

                // Check bandwidth
                assertEquals(0.0, conn0.getTotalBandwidth(), simulator.getFlowPrecision());
                assertEquals(10.0, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(10.0, conn1.getTotalBandwidth(), simulator.getFlowPrecision());

                // Check utilization
                assertEquals(network.getPresentLinksBetween(0, 2).get(0).getUtilization(), 0, simulator.getFlowPrecision());
                assertEquals(network.getPresentLinksBetween(1, 2).get(0).getUtilization(), 1, simulator.getFlowPrecision());
                assertEquals(network.getPresentLinksBetween(2, 3).get(0).getUtilization(), 1, simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void longTests() {
        final Random random = new Random(22421); // 3278957927589L
        testLong(2, 2, random.nextLong());
        testLong(2, 100, random.nextLong());
        testLong(100, 2, random.nextLong());
        for (int i = 0; i < 100; i++) {
            testLong(5, 17, random.nextLong());
        }
        testLong(7, 3, random.nextLong());
        testLong(7, 7, random.nextLong());
        testLong(17, 17, random.nextLong());
        testLong(11, 21, random.nextLong());
        testLong(30, 79, random.nextLong());
        testLong(57, 77, random.nextLong());
        testLong(89, 30, random.nextLong());
    }

    public void testLong(final int numNodes, final int numFlows, long seed) {
        Simulator simulator = new Simulator();
        Network network = new Network(numNodes);

        //
        // 0 -> 1 -> ... -> numNodes
        //
        final Random random = new Random(seed);
        for (int i = 0; i < numNodes - 1; i++) {
            network.addLink(i, i + 1, Math.abs(random.nextDouble()) * 20.0 + 0.02);
            network.addLink(i + 1, i, Math.abs(random.nextDouble()) * 20.0 + 0.02);
        }

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                for (int i = 0; i < numFlows; i++) {
                    StringBuilder pathStr = new StringBuilder();
                    int start = Math.abs(random.nextInt()) % numNodes;
                    int end = Math.abs(random.nextInt()) % numNodes;
                    while (start == end) {
                        end = Math.abs(random.nextInt()) % numNodes;
                    }

                    if (start > end) {
                        for (int j = start; j >= end; j--) {
                            pathStr.append(j);
                            if (j != end) {
                                pathStr.append("-");
                            }
                        }
                    } else {
                        for (int j = start; j <= end; j++) {
                            pathStr.append(j);
                            if (j != end) {
                                pathStr.append("-");
                            }
                        }
                    }
                    PathTestUtility.startSimpleFlow(simulator, network, pathStr.toString());
                }

                Map<Flow, Double> flowToUpperLimit = new HashMap<>();
                for (int i = 0; i < numFlows; i++) {
                    flowToUpperLimit.put(network.getActiveFlow(i), Math.abs(random.nextDouble()) * 20.0 + 0.02);
                }

                Map<Flow, Double> flowToWeight = new HashMap<>();
                for (int i = 0; i < numFlows; i++) {
                    flowToWeight.put(network.getActiveFlow(i), Math.abs(random.nextDouble()) * 20.0 + 0.02);
                }

                new GenericMmfAllocator(simulator, network, flowToUpperLimit, flowToWeight).perform();

                // At least one of the links must be fully satisfied
                boolean found = false;
                for (int i = 0; i < 2 * (numNodes - 1); i++) {
                    if (Math.abs(network.getLink(i).getRemainderCapacity()) <= simulator.getFlowPrecision()) {
                        found = true;
                    }
                }
                assertTrue(found);

            }

        });

    }

}
