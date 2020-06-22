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
import ch.ethz.systems.floodns.ext.allocator.MinMaxLinkCapLpAllocator;
import ch.ethz.systems.floodns.ext.lputils.GlopLpSolver;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class MinMaxLinkCapLpAllocatorTest {


    @Test
    public void testAsymmetricLinksSingle() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 - 1 - 2
        //     |
        //     3
        //
        network.addLink(0, 1, 1.0);
        network.addLink(1, 0, 3.0);
        network.addLink(1, 2, 7.0);
        network.addLink(2, 1, 5.0);
        network.addLink(1, 3, 10.0);
        network.addLink(3, 1, 86.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1-2");
                PathTestUtility.startSimpleFlow(simulator, network, "0-1-3");
                PathTestUtility.startSimpleFlow(simulator, network, "2-1-3");
                PathTestUtility.startSimpleFlow(simulator, network, "1-3");
                PathTestUtility.startSimpleFlow(simulator, network, "2-1-0");

                // Demands
                Map<Integer, Double> connectionToDemand = new HashMap<>();
                connectionToDemand.put(0, 0.4);
                connectionToDemand.put(1, 0.6);
                connectionToDemand.put(2, 0.3);
                connectionToDemand.put(3, 9.9);
                connectionToDemand.put(4, 44.0);
                connectionToDemand.put(5, 5.0);

                MinMaxLinkCapLpAllocator allocator = new MinMaxLinkCapLpAllocator(simulator, network, connectionToDemand, new GlopLpSolver("external/glop_solver.py", true));
                allocator.perform();

                // Check bandwidth
                assertEquals(0.4 / 5.42, network.getActiveFlow(0).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(0.6 / 5.42, network.getActiveFlow(1).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(0.3 / 5.42, network.getActiveFlow(2).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(9.9 / 5.42, network.getActiveFlow(3).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(44.0 / 5.42, network.getActiveFlow(4).getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(5.0 / 5.42, network.getActiveFlow(5).getCurrentBandwidth(), simulator.getFlowPrecision());

                assertEquals(5.42, allocator.getObjectiveZ(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testAsymmetricLinksMulti() {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 - 1 - 2
        //     | /
        //     3
        //
        network.addLink(0, 1, 1.0);
        network.addLink(1, 0, 3.0);
        network.addLink(1, 2, 7.0);
        network.addLink(2, 1, 5.0);
        network.addLink(1, 3, 10.0);
        network.addLink(3, 1, 86.0);
        network.addLink(2, 3, 40.0);
        network.addLink(3, 2, 30.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flows
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                AcyclicPath path = createAcyclicPath(network, "1-2-3");
                AcyclicPath path2 = createAcyclicPath(network, "1-3");
                Connection conn = new Connection(simulator, path.getSrcNode(), path.getDstNode(), 1000);
                simulator.activateConnection(conn);
                simulator.addFlowToConnection(conn, path);
                simulator.addFlowToConnection(conn, path2);
                PathTestUtility.startSimpleFlow(simulator, network, "2-3");

                // Demands
                Map<Integer, Double> connectionToDemand = new HashMap<>();
                connectionToDemand.put(0, 0.8);
                connectionToDemand.put(1, 16.0);
                connectionToDemand.put(2, 34.0);

                MinMaxLinkCapLpAllocator allocator = new MinMaxLinkCapLpAllocator(simulator, network, connectionToDemand, new GlopLpSolver("external/glop_solver.py", true));
                allocator.perform();

                // Check bandwidth
                assertEquals(0.8, simulator.getActiveConnection(0).getTotalBandwidth(), simulator.getFlowPrecision());
                assertEquals(16.0, simulator.getActiveConnection(1).getTotalBandwidth(), simulator.getFlowPrecision());
                assertEquals(34.0, simulator.getActiveConnection(2).getTotalBandwidth(), simulator.getFlowPrecision());

                assertEquals(1.0, allocator.getObjectiveZ(), simulator.getFlowPrecision());

                assertTrue(allocator.getSolveTimeMs() >= 0);

            }

        });

    }

}
