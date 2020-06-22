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
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import org.junit.Test;

import static ch.ethz.systems.floodns.PathTestUtility.semiSpecificAcyclicPath;
import static ch.ethz.systems.floodns.PathTestUtility.startFlow;
import static org.junit.Assert.assertEquals;

public class NetworkDoubleEdgesTest {

    @Test
    public void testThreeSplitHeterogeneous() {
        Simulator simulator = new Simulator();
        Network network = new Network(3);

        //    0
        //   / \
        //  1 - 2
        //
        // 0-1 has three links each way, 1-2 two, 2-0 one
        network.addLink(0, 1, 100);
        network.addLink(0, 1, 100);
        network.addLink(0, 1, 100);
        network.addLink(1, 0, 100);
        network.addLink(1, 0, 100);
        network.addLink(1, 0, 100);
        network.addLink(1, 2, 100);
        network.addLink(1, 2, 100);
        network.addLink(2, 1, 100);
        network.addLink(2, 1, 100);
        network.addLink(0, 2, 100);
        network.addLink(2, 0, 100);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // f0_x: 0 to 1 over each of the three possible edges
                Flow f0_0 = startFlow(simulator, semiSpecificAcyclicPath(network, "0(0)-1(0)-2"));
                Flow f0_1 = startFlow(simulator, semiSpecificAcyclicPath(network, "0(1)-1(0)-2"));
                Flow f0_2 = startFlow(simulator, semiSpecificAcyclicPath(network, "0(2)-1(1)-2"));

                // Perform allocation
                new SimpleMmfAllocator(simulator, network).perform();

                // Check bandwidth
                assertEquals(50, f0_0.getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(50, f0_1.getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(100, f0_2.getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

}
