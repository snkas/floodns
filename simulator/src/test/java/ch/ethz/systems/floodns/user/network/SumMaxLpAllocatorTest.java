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
import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SumMaxLpAllocator;
import ch.ethz.systems.floodns.ext.lputils.GlopLpSolver;
import org.junit.Test;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class SumMaxLpAllocatorTest {

    @Test
    public void testAsymmetricLinksSingle() {
        Simulator simulator = new Simulator(1e-4);
        Network network = new Network(6);

        //
        // 0 - 1 - 2
        //     |
        //     3
        //
        network.addLink(0, 1, 1.0);
        network.addLink(1, 0, 3.0);
        network.addLink(1, 2, 7.0);
        network.addLink(2, 1, 3.0);
        network.addLink(1, 3, 10.0);
        network.addLink(3, 1, 86.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1-3");
                PathTestUtility.startSimpleFlow(simulator, network, "2-1-3");
                PathTestUtility.startSimpleFlow(simulator, network, "1-3");
                PathTestUtility.startSimpleFlow(simulator, network, "2-1-0");

                SumMaxLpAllocator allocator = new SumMaxLpAllocator(simulator, network, new GlopLpSolver("external/glop_solver.py", true));
                allocator.perform();

                // Check bandwidth
                assertEquals(1.0, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(0.0, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(0.0, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(10.0, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(3.0, network.getActiveFlow(4).getCurrentBandwidth(), simulator.getFlowPrecision());

                // 1  (0 -> 1 bottleneck)
                // 3  (2 -> 1 bottleneck)
                // 10 (1 -> 3 bottleneck)
                //
                assertEquals(14.0, allocator.getObjectiveZ(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testAsymmetricLinksMulti() {
        Simulator simulator = new Simulator(1e-4);
        Network network = new Network(6);

        //
        // 0 - 1 - 2
        //     | / |
        //     3 - 4
        //
        network.addLink(0, 1, 100.0);
        network.addLink(1, 0, 3.0);
        network.addLink(1, 2, 7.0);
        network.addLink(2, 1, 5.0);
        network.addLink(1, 3, 10.0);
        network.addLink(3, 1, 86.0);
        network.addLink(2, 3, 40.0);
        network.addLink(3, 2, 30.0);
        network.addLink(2, 4, 40.0);
        network.addLink(4, 2, 30.0);
        network.addLink(3, 4, 40.0);
        network.addLink(4, 3, 30.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                AcyclicPath path = createAcyclicPath(network, "1-2-4-3");
                AcyclicPath path1 = createAcyclicPath(network, "1-2-3");
                AcyclicPath path2 = createAcyclicPath(network, "1-3");
                Connection conn = new Connection(simulator, path.getSrcNode(), path.getDstNode(), 1000);
                simulator.activateConnection(conn);
                simulator.addFlowToConnection(conn, path);
                simulator.addFlowToConnection(conn, path1);
                simulator.addFlowToConnection(conn, path2);
                PathTestUtility.startSimpleFlow(simulator, network, "2-3");

                SumMaxLpAllocator allocator = new SumMaxLpAllocator(simulator, network, new GlopLpSolver("external/glop_solver.py", true));
                allocator.perform();

                // Check bandwidth
                assertEquals(100, simulator.getActiveConnection(0).getTotalBandwidth(), simulator.getFlowPrecision());
                assertEquals(17.0, simulator.getActiveConnection(1).getTotalBandwidth(), simulator.getFlowPrecision());
                assertEquals(40.0, simulator.getActiveConnection(2).getTotalBandwidth(), simulator.getFlowPrecision());

                assertEquals(100 + 17 + 40, allocator.getObjectiveZ(), simulator.getFlowPrecision());

                assertTrue(allocator.getSolveTimeMs() >= 0);

            }

        });

    }

}
