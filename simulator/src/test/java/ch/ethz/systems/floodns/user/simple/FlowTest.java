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

package ch.ethz.systems.floodns.user.simple;

import ch.ethz.systems.floodns.core.*;
import ch.ethz.systems.floodns.ext.metadata.SimpleStringMetadata;
import ch.ethz.systems.floodns.user.network.NetworkTestHelper;
import ch.ethz.systems.floodns.user.network.TestBody;
import org.junit.Test;

import java.util.HashSet;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static ch.ethz.systems.floodns.PathTestUtility.startFlow;
import static org.junit.Assert.*;

public class FlowTest {

    @Test
    public void testBasic() {

        Simulator simulator = new Simulator();
        Network network = new Network(9);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                network.addLink(0, 5, 10);
                network.addLink(5, 8, 10);

                // Create nodes
                Node nodeA = network.getNode(0);
                Node nodeB = network.getNode(8);

                // Create flow
                Flow flow = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                Connection connection = simulator.getActiveConnection(0);

                // Getters
                assertEquals(0, flow.getSrcNodeId());
                assertEquals(8, flow.getDstNodeId());
                assertEquals(nodeA, flow.getSrcNode());
                assertEquals(nodeB, flow.getDstNode());
                assertEquals(0, flow.getCurrentBandwidth(), simulator.getFlowPrecision());
                assertEquals(flow.getPath(), createAcyclicPath(network, "0-5-8"));

                // Parent connection
                assertTrue(connection == flow.getParentConnection());


            }

        });

    }

    @Test
    public void testEquality() {
        Simulator simulator = new Simulator();
        Network network = new Network(9);
        network.addLink(0, 5, 10);
        network.addLink(5, 8, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                Flow flowA = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                Flow flowB = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                Flow flowC = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                assertFalse(flowA.equals(flowB));
                assertFalse(flowB.equals(flowC));
                assertFalse(flowC.equals(flowA));

            }

        });

    }

    @Test
    public void testBandwidth() {

        Simulator simulator = new Simulator();
        Network network = new Network(9);
        network.addLink(0, 5, 900);
        network.addLink(5, 8, 1000);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test () {

                Flow flow = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                assertEquals(0, flow.getCurrentBandwidth(), simulator.getFlowPrecision());
                simulator.allocateFlowBandwidth(flow, 834.0);
                assertEquals(834.0, flow.getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testTinyNegativeBandwidth() {

        Simulator simulator = new Simulator(1e-7);
        Network network = new Network(9);
        network.addLink(0, 5, 900);
        network.addLink(5, 8, 1000);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test () {

                Flow flow = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                assertEquals(0, flow.getCurrentBandwidth(), simulator.getFlowPrecision());
                simulator.allocateFlowBandwidth(flow, -1e-8);
                assertEquals(0, flow.getCurrentBandwidth(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testOtherGetters() {

        Simulator simulator = new Simulator();
        Network network = new Network(9);
        network.addLink(0, 5, 10);
        network.addLink(5, 8, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                Flow flow = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                simulator.allocateFlowBandwidth(flow, 3.0);
                assertEquals("Flow#0[ 0 -> 8, bw=3.0, path=[Link#0[ 0 -> 5 (7.0/10.0 left) ], Link#1[ 5 -> 8 (7.0/10.0 left) ]] ]", flow.toString());

            }

        });

    }

    @Test
    public void testHashMap() {

        Simulator simulator = new Simulator();
        Network network = new Network(9);
        network.addLink(0, 5, 10);
        network.addLink(5, 8, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                Flow flowA = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                Flow flowB = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                Flow flowC = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                HashSet<Flow> set = new HashSet<>();
                set.add(flowA);
                set.add(flowB);
                set.add(flowC);
                assertEquals(3, set.size());

            }

        });

    }

    @Test
    public void testExtraInfo() {
        Simulator simulator = new Simulator();
        Network network = new Network(9);
        network.addLink(0, 5, 10);
        network.addLink(5, 8, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                Flow flowA = startFlow(simulator, createAcyclicPath(network, "0-5-8"));
                assertNull(flowA.getMetadata());
                flowA.setMetadata(new SimpleStringMetadata("someFlowLabelText"));
                assertNotNull(flowA.getMetadata());
                assertEquals("someFlowLabelText", ((SimpleStringMetadata) flowA.getMetadata()).toCsvValidLabel());

            }

        });

    }

}
