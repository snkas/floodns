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

public class NodeTest {

    @Test
    public void testEquality() {

        Network network = new Network(6);
        Node nodeA = network.getNode(4);
        Node nodeB = network.getNode(4);
        Node nodeC = network.getNode(5);

        // Base equality
        assertTrue(nodeA.equals(nodeB));
        assertFalse(nodeB.equals(nodeC));

        // Base getters
        assertEquals(4, nodeA.getNodeId());

    }

    @Test
    public void testLinks() {
        Simulator simulator = new Simulator();

        //
        //  A  >  B
        //   >   <>
        //     C
        //

        Network network = new Network(57);
        network.addLink(4, 56, 10);
        network.addLink(4, 0, 10);
        network.addLink(56, 0, 10);
        network.addLink(0, 56, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Create nodes
                Node nodeA = network.getNode(4);
                Node nodeB = network.getNode(56);
                Node nodeC = network.getNode(0);

                // Create links
                Link linkAB = network.getLink(0);
                Link linkAC = network.getLink(1);
                Link linkBC = network.getLink(2);
                Link linkCB = network.getLink(3);

                // Outgoing connected to
                assertEquals(nodeA.getOutgoingConnectedToNodes().size(), 2);
                assertEquals(nodeB.getOutgoingConnectedToNodes().size(), 1);
                assertEquals(nodeC.getOutgoingConnectedToNodes().size(), 1);

                assertTrue(nodeA.getOutgoingConnectedToNodes().contains(0));
                assertTrue(nodeA.getOutgoingConnectedToNodes().contains(56));
                assertTrue(nodeB.getOutgoingConnectedToNodes().contains(0));
                assertTrue(nodeC.getOutgoingConnectedToNodes().contains(56));

                // Incoming connected to
                assertEquals(nodeA.getIncomingConnectedToNodes().size(), 0);
                assertEquals(nodeB.getIncomingConnectedToNodes().size(), 2);
                assertEquals(nodeC.getIncomingConnectedToNodes().size(), 2);

                assertTrue(nodeB.getIncomingConnectedToNodes().contains(4));
                assertTrue(nodeB.getIncomingConnectedToNodes().contains(0));
                assertTrue(nodeC.getIncomingConnectedToNodes().contains(4));
                assertTrue(nodeC.getIncomingConnectedToNodes().contains(56));

                // Boolean operators
                assertTrue(nodeA.hasOutgoingLinksTo(nodeB));
                assertTrue(nodeA.hasOutgoingLinksTo(nodeC));
                assertTrue(nodeB.hasOutgoingLinksTo(nodeC));
                assertTrue(nodeC.hasOutgoingLinksTo(nodeB));

                assertTrue(nodeB.hasIncomingLinksFrom(nodeA));
                assertTrue(nodeB.hasIncomingLinksFrom(nodeC));
                assertTrue(nodeC.hasIncomingLinksFrom(nodeA));
                assertTrue(nodeC.hasIncomingLinksFrom(nodeB));

                // Link retrieval operators
                assertEquals(nodeA.getOutgoingLinksTo(nodeB).get(0), linkAB);
                assertEquals(nodeA.getOutgoingLinksTo(nodeC).get(0), linkAC);
                assertEquals(nodeB.getOutgoingLinksTo(nodeC).get(0), linkBC);
                assertEquals(nodeC.getOutgoingLinksTo(nodeB).get(0), linkCB);

                assertEquals(nodeB.getIncomingLinksFrom(nodeA).get(0), linkAB);
                assertEquals(nodeB.getIncomingLinksFrom(nodeC).get(0), linkCB);
                assertEquals(nodeC.getIncomingLinksFrom(nodeA).get(0), linkAC);
                assertEquals(nodeC.getIncomingLinksFrom(nodeB).get(0), linkBC);

                // Check all outgoing links
                assertEquals(2, nodeA.getOutgoingLinks().size());
                assertTrue(nodeA.getOutgoingLinks().contains(linkAB));
                assertTrue(nodeA.getOutgoingLinks().contains(linkAC));
                assertEquals(1, nodeB.getOutgoingLinks().size());
                assertTrue(nodeB.getOutgoingLinks().contains(linkBC));
                assertEquals(1, nodeC.getOutgoingLinks().size());
                assertTrue(nodeC.getOutgoingLinks().contains(linkCB));

                // Check all incoming links
                assertEquals(0, nodeA.getIncomingLinks().size());
                assertEquals(2, nodeB.getIncomingLinks().size());
                assertTrue(nodeB.getIncomingLinks().contains(linkAB));
                assertTrue(nodeB.getIncomingLinks().contains(linkCB));
                assertEquals(2, nodeC.getIncomingLinks().size());
                assertTrue(nodeC.getIncomingLinks().contains(linkAC));
                assertTrue(nodeC.getIncomingLinks().contains(linkBC));

                // Removal of link A -> B
                simulator.removeExistingLink(linkAB);

                // Outgoing connected to
                assertEquals(nodeA.getOutgoingConnectedToNodes().size(), 1);
                assertEquals(nodeB.getOutgoingConnectedToNodes().size(), 1);
                assertEquals(nodeC.getOutgoingConnectedToNodes().size(), 1);

                assertTrue(nodeA.getOutgoingConnectedToNodes().contains(0));
                assertFalse(nodeA.getOutgoingConnectedToNodes().contains(56));
                assertTrue(nodeB.getOutgoingConnectedToNodes().contains(0));
                assertTrue(nodeC.getOutgoingConnectedToNodes().contains(56));

                // Incoming connected to
                assertEquals(nodeA.getIncomingConnectedToNodes().size(), 0);
                assertEquals(nodeB.getIncomingConnectedToNodes().size(), 1);
                assertEquals(nodeC.getIncomingConnectedToNodes().size(), 2);

                assertFalse(nodeB.getIncomingConnectedToNodes().contains(4));
                assertTrue(nodeB.getIncomingConnectedToNodes().contains(0));
                assertTrue(nodeC.getIncomingConnectedToNodes().contains(4));
                assertTrue(nodeC.getIncomingConnectedToNodes().contains(56));

                // Boolean operators
                assertFalse(nodeA.hasOutgoingLinksTo(nodeB));
                assertTrue(nodeA.hasOutgoingLinksTo(nodeC));
                assertTrue(nodeB.hasOutgoingLinksTo(nodeC));
                assertTrue(nodeC.hasOutgoingLinksTo(nodeB));

                assertFalse(nodeB.hasIncomingLinksFrom(nodeA));
                assertTrue(nodeB.hasIncomingLinksFrom(nodeC));
                assertTrue(nodeC.hasIncomingLinksFrom(nodeA));
                assertTrue(nodeC.hasIncomingLinksFrom(nodeB));

                // Link retrieval operators
                assertNull(nodeA.getOutgoingLinksTo(nodeB));
                assertEquals(nodeA.getOutgoingLinksTo(nodeC).get(0), linkAC);
                assertEquals(nodeB.getOutgoingLinksTo(nodeC).get(0), linkBC);
                assertEquals(nodeC.getOutgoingLinksTo(nodeB).get(0), linkCB);

                assertNull(nodeB.getIncomingLinksFrom(nodeA));
                assertEquals(nodeB.getIncomingLinksFrom(nodeC).get(0), linkCB);
                assertEquals(nodeC.getIncomingLinksFrom(nodeA).get(0), linkAC);
                assertEquals(nodeC.getIncomingLinksFrom(nodeB).get(0), linkBC);

                // Check all outgoing links
                assertEquals(1, nodeA.getOutgoingLinks().size());
                assertFalse(nodeA.getOutgoingLinks().contains(linkAB));
                assertTrue(nodeA.getOutgoingLinks().contains(linkAC));
                assertEquals(1, nodeB.getOutgoingLinks().size());
                assertTrue(nodeB.getOutgoingLinks().contains(linkBC));
                assertEquals(1, nodeC.getOutgoingLinks().size());
                assertTrue(nodeC.getOutgoingLinks().contains(linkCB));

                // Check all incoming links
                assertEquals(0, nodeA.getIncomingLinks().size());
                assertEquals(1, nodeB.getIncomingLinks().size());
                assertFalse(nodeB.getIncomingLinks().contains(linkAB));
                assertTrue(nodeB.getIncomingLinks().contains(linkCB));
                assertEquals(2, nodeC.getIncomingLinks().size());
                assertTrue(nodeC.getIncomingLinks().contains(linkAC));
                assertTrue(nodeC.getIncomingLinks().contains(linkBC));

            }

        });

    }

    @Test
    public void testFlows() {
        Simulator simulator = new Simulator();

        Network network = new Network(56);
        network.addLink(4, 55, 10);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                Node nodeA = network.getNode(4);
                Node nodeB = network.getNode(55);

                // Base initialization
                assertEquals(nodeA.getActiveFlows().size(), 0);
                assertEquals(nodeA.getActiveFlowsIds().size(), 0);

                // Add one flow
                Flow f0 = startFlow(simulator, createAcyclicPath(network, "4-55"));

                // Active flows there
                assertEquals(nodeA.getActiveFlows().size(), 1);
                assertEquals(nodeA.getActiveFlowsIds().size(), 1);
                assertTrue(nodeA.getActiveFlows().contains(f0));
                assertTrue(nodeA.getActiveFlowsIds().contains(0));
                assertEquals(nodeB.getActiveFlows().size(), 1);
                assertEquals(nodeB.getActiveFlowsIds().size(), 1);
                assertTrue(nodeB.getActiveFlows().contains(f0));
                assertTrue(nodeB.getActiveFlowsIds().contains(0));

                // Remove flow
                simulator.endFlow(f0);

                // Check there are no active flows
                assertEquals(nodeA.getActiveFlows().size(), 0);
                assertEquals(nodeA.getActiveFlowsIds().size(), 0);

            }

        });


    }

    @Test
    public void testOtherGetters() {
        Network network = new Network(56);
        Node node = network.getNode(4);
        assertEquals("Node#4", node.toString());
    }

    @Test
    public void testHashMap() {
        Network network = new Network(200);
        Node nodeA = network.getNode(4);
        Node nodeB = network.getNode(99);
        HashSet<Node> set = new HashSet<>();
        set.add(nodeA);
        set.add(nodeB);
        assertEquals(2, set.size());
    }

    @Test
    public void testExtraInfo() {
        Network network = new Network(200);
        Node nodeA = network.getNode(4);
        assertNull(nodeA.getMetadata());
        nodeA.setMetadata(new SimpleStringMetadata("someNodeLabelText"));
        assertNotNull(nodeA.getMetadata());
        assertEquals("someNodeLabelText", ((SimpleStringMetadata) nodeA.getMetadata()).toCsvValidLabel());
    }

}
