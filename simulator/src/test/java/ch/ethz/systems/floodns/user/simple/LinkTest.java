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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.util.HashSet;

import static ch.ethz.systems.floodns.PathTestUtility.startFlow;
import static org.junit.Assert.*;

public class LinkTest {

    @Test
    public void testBasic() {

        // Create nodes
        Simulator simulator = new Simulator();
        Network network = new Network(9);
        Node nodeA = network.getNode(0);
        Node nodeB = network.getNode(8);

        // Create link
        Link link = network.addLink(0, 8, 10.0);

        // Getters
        assertEquals(0, link.getLinkId());
        assertEquals(0, link.getFrom());
        assertEquals(8, link.getTo());
        assertEquals(nodeA, link.getFromNode());
        assertEquals(nodeB, link.getToNode());
        assertEquals(10.0, link.getCapacity(), simulator.getFlowPrecision());

    }

    @Test
    public void testFlowManagementSingle() {
        Simulator simulator = new Simulator();

        // Create network
        Network network = new Network(4);
       final  Link link23 = network.addLink(2, 3, 10.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flow A
                AcyclicPath path = new AcyclicPath();
                path.add(link23);
                Flow flowA = startFlow(simulator, path);

                // Single flow
                simulator.allocateFlowBandwidth(flowA, 7.0);
                assertEquals(3.0, link23.getRemainderCapacity(), simulator.getFlowPrecision());

                // Now reset
                simulator.allocateFlowBandwidth(flowA, 0);
                assertEquals(10.0, link23.getRemainderCapacity(), simulator.getFlowPrecision());

                // Perform again allocation
                simulator.allocateFlowBandwidth(flowA, 4.0);
                assertEquals(6.0, link23.getRemainderCapacity(), simulator.getFlowPrecision());

                // Irrational allocation
                simulator.allocateFlowBandwidth(flowA, 10.0 / 3.0);
                assertEquals(10.0 - 10.0 / 3.0, link23.getRemainderCapacity(), simulator.getFlowPrecision());

                // Irrational (II) allocation
                simulator.allocateFlowBandwidth(flowA, Math.sqrt(2));
                assertEquals(10.0 - Math.sqrt(2), link23.getRemainderCapacity(), simulator.getFlowPrecision());

                // Now reset
                simulator.allocateFlowBandwidth(flowA, 0);
                assertEquals(10.0, link23.getRemainderCapacity(), simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testFlowManagementTriple() {
        Simulator simulator = new Simulator();

        // Create network
        Network network = new Network(4);
        final Link link23 = network.addLink(2, 3, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Flow A
                AcyclicPath path = new AcyclicPath();
                path.add(link23);
                Flow flowA = startFlow(simulator, path);

                // Flow B
                Flow flowB = startFlow(simulator, path);

                // Flow C
                Flow flowC = startFlow(simulator, path);

                // Allocate bandwidth
                simulator.allocateFlowBandwidth(flowA, 3.8);
                simulator.allocateFlowBandwidth(flowB, 2.5);
                simulator.allocateFlowBandwidth(flowC, 3.7);
                assertEquals(0, link23.getRemainderCapacity(), simulator.getFlowPrecision());
                assertEquals(1.0, link23.getUtilization(), simulator.getFlowPrecision());

                // Check active flows
                assertTrue(link23.getActiveFlows().contains(flowA));
                assertTrue(link23.getActiveFlows().contains(flowB));
                assertTrue(link23.getActiveFlows().contains(flowC));
                assertTrue(link23.getActiveFlows().size() == 3);
                assertTrue(link23.getActiveFlowIds().contains(0));
                assertTrue(link23.getActiveFlowIds().contains(1));
                assertTrue(link23.getActiveFlowIds().contains(2));
                assertTrue(link23.getActiveFlowIds().size() == 3);

                // Detach two flows (will force reset of allocation on path)
                simulator.endFlow(flowA);
                simulator.endFlow(flowB);

                // Check active flows
                assertFalse(link23.getActiveFlows().contains(flowA));
                assertFalse(link23.getActiveFlows().contains(flowB));
                assertTrue(link23.getActiveFlows().contains(flowC));
                assertTrue(link23.getActiveFlows().size() == 1);
                assertFalse(link23.getActiveFlowIds().contains(77));
                assertFalse(link23.getActiveFlowIds().contains(1));
                assertTrue(link23.getActiveFlowIds().contains(2));
                assertTrue(link23.getActiveFlowIds().size() == 1);
                assertEquals(6.3, link23.getRemainderCapacity(), simulator.getFlowPrecision());
                assertEquals(0.37, link23.getUtilization(), simulator.getFlowPrecision());

                // Full reset
                simulator.allocateFlowBandwidth(flowC, 0);

                // Allocate for flow C
                simulator.allocateFlowBandwidth(flowC, 4.5);
                assertEquals(0.45, link23.getUtilization(), simulator.getFlowPrecision());

            }

        });


    }
    @Test
    public void testEquality() {
        Network network = new Network(100);
        Link linkA = network.addLink(4, 8, 6.9);
        Link linkB = network.addLink(7, 99, 6.9);
        Link linkC = network.addLink(7, 99, 6.9);
        Link linkD = network.getLink(1);
        assertFalse(linkA.equals(linkB));
        assertFalse(linkB.equals(linkC));
        assertFalse(linkC.equals(linkD));
        assertTrue(linkB.equals(linkD));
        assertTrue(linkB == linkD);
    }

    @Test
    public void testOtherGetters() {
        Network network = new Network(100);
        Link link = network.addLink(4, 8, 10);
        assertEquals("Link#0[ 4 -> 8 (10.0/10.0 left) ]", link.toString());
        ImmutablePair<Integer, Integer> fromToPair = link.getFromToPair();
        assertEquals(4, (int) fromToPair.getLeft());
        assertEquals(8, (int) fromToPair.getRight());
    }

    @Test
    public void testHashMap() {
        Network network = new Network(100);
        Link linkA = network.addLink(4, 8, 10.0);
        Link linkB = network.addLink(78, 99, 10.0);
        HashSet<Link> set = new HashSet<>();
        set.add(linkA);
        set.add(linkB);
        assertEquals(2, set.size());
    }

    @Test
    public void testExtraInfo() {
        Network network = new Network(200);
        Link linkA = network.addLink(4, 8, 10.0);
        assertNull(linkA.getMetadata());
        linkA.setMetadata(new SimpleStringMetadata("someLinkLabelText"));
        assertNotNull(linkA.getMetadata());
        assertEquals("someLinkLabelText", ((SimpleStringMetadata) linkA.getMetadata()).toCsvValidLabel());
    }

}
