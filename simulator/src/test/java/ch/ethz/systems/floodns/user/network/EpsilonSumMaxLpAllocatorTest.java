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
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.EpsilonSumMaxLpAllocator;
import ch.ethz.systems.floodns.ext.lputils.GlopLpSolver;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class EpsilonSumMaxLpAllocatorTest {

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

                EpsilonSumMaxLpAllocator allocator = new EpsilonSumMaxLpAllocator(simulator, network, 1.0, new GlopLpSolver("external/glop_solver.py", true));
                allocator.perform();

                // Check bandwidth
                double alpha = 1.0 / 3.0;
                assertTrue(network.getActiveFlow(0).getCurrentBandwidth() >= alpha * 1.0 - simulator.getFlowPrecision());
                assertTrue(network.getActiveFlow(1).getCurrentBandwidth() >= alpha * 1.0 - simulator.getFlowPrecision());
                assertTrue(network.getActiveFlow(2).getCurrentBandwidth() >= alpha * 1.0 - simulator.getFlowPrecision());
                assertTrue(network.getActiveFlow(3).getCurrentBandwidth() >= alpha * 1.0 - simulator.getFlowPrecision());
                assertTrue(network.getActiveFlow(4).getCurrentBandwidth() >= alpha * 1.0 - simulator.getFlowPrecision());
                assertTrue(network.getActiveFlow(5).getCurrentBandwidth() >= alpha * 1.0 - simulator.getFlowPrecision());

                assertEquals(1.0 / 3.0, allocator.getObjectiveZ(), simulator.getFlowPrecision());

                assertTrue(allocator.getSolveTimeMs() >= 0);

            }

        });

    }

}
