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
import ch.ethz.systems.floodns.ext.allocator.GenericMmfAllocator;
import ch.ethz.systems.floodns.ext.logger.empty.VoidLoggerFactory;
import ch.ethz.systems.floodns.ext.metadata.SimpleStringMetadata;
import ch.ethz.systems.floodns.user.network.NetworkTestHelper;
import ch.ethz.systems.floodns.user.network.TestBody;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static ch.ethz.systems.floodns.PathTestUtility.startSimpleFlow;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ConnectionTest {
    
    private Simulator simulator;

    @Before
    public void setup() {
        this.simulator = new Simulator();
    }
    
    @Test
    public void testBase() {

        // Initialization
        Network network = new Network(4);
        Node node3 = network.getNode(3);
        Node node2 = network.getNode(2);
        Connection connection = new Connection(simulator, node3, node2, 10000);
        Connection connection2 = new Connection(simulator, node3, node2, 10000);
        Connection connection3 = new Connection(simulator, node3, node2, 10000);

        // Node getters
        assertEquals(3, connection.getSrcNodeId());
        assertEquals(2, connection.getDstNodeId());
        assertEquals(node3, connection.getSrcNode());
        assertEquals(node2, connection.getDstNode());
        assertEquals(10000, connection.getTotalSize(), simulator.getFlowPrecision());

        // Equality
        assertFalse(connection.equals(connection3));
        assertFalse(connection.equals(connection2));
        assertFalse(connection2.equals(connection3));

        // toString
        assertEquals("Connection#1[ 3 -> 2; size=(10000.0/10000.0 remaining); flows=[] ]", connection2.toString());

        // Hashing
        Set<Connection> connections = new HashSet<>();
        connections.add(connection);
        connections.add(connection2);
        connections.add(connection3);
        assertEquals(3, connections.size());

        // Zero size failure
        boolean thrown = false;
        try {
            new Connection(simulator, node3, node2, 0.0);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testProgressTracking() {
        Simulator simulator = new Simulator();

        // Initialize network
        Network network = new Network(4);
        network.addLink(2, 3, 13.0);

        Aftermath allocator = new Aftermath(simulator, network) {

            Flow f0, f1;
            Connection connection;

            @Override
            public void perform() {

                if (simulator.getCurrentTime() == 0) {

                    Node node2 = network.getNode(2);
                    Node node3 = network.getNode(3);

                    // Setup connection
                    connection = new Connection(simulator, node2, node3, 10000);
                    assertEquals(Connection.Status.AWAITING_ACTIVATION, connection.getStatus());
                    simulator.activateConnection(connection);
                    assertEquals(Connection.Status.ACTIVE, connection.getStatus());

                    // Simulate resolving of flows by simulator
                    f0 = simulator.addFlowToConnection(connection, createAcyclicPath(network, "2-3"));
                    f1 = simulator.addFlowToConnection(connection, createAcyclicPath(network, "2-3"));
                    assertEquals(connection, f0.getParentConnection());
                    assertEquals(connection, f1.getParentConnection());

                    // Simulate that flows have received bandwidth
                    simulator.allocateFlowBandwidth(f0, 4);
                    simulator.allocateFlowBandwidth(f1, 9);
                    assertEquals(Math.ceil(10000 / 13.0), connection.timeTillUpdateNeeded(), simulator.getFlowPrecision());

                } else if (simulator.getCurrentTime() == 433) {

                    // Pass 433 time units
                    assertEquals(13, connection.getTotalBandwidth(), simulator.getFlowPrecision());
                    assertEquals(4371, connection.getRemainder(), simulator.getFlowPrecision());

                    // Remove one flow
                    simulator.endFlow(f0);
                    assertEquals((long) Math.ceil(4371.0 / 9.0), connection.timeTillUpdateNeeded());

                } else if (simulator.getCurrentTime() == 433 + 485) { // Just one before completion

                    // Pass 486 time units
                    assertEquals(9, connection.getTotalBandwidth(), simulator.getFlowPrecision());
                    assertEquals(6, connection.getRemainder(), simulator.getFlowPrecision());

                    // Flow identifiers
                    assertTrue(connection.getPastAndPresentFlowIds().contains(0));
                    assertTrue(connection.getPastAndPresentFlowIds().contains(1));
                    assertEquals(2, connection.getPastAndPresentFlowIds().size());
                    assertEquals(Connection.Status.ACTIVE, connection.getStatus());

                } else if (simulator.getCurrentTime() == 433 + 486) { // Exact time tick of completion

                    // Pass 486 time units
                    assertEquals(0, connection.getTotalBandwidth(), simulator.getFlowPrecision());
                    assertEquals(-3, connection.getRemainder(), simulator.getFlowPrecision());
                    assertEquals(Connection.Status.TERMINATED, connection.getStatus());

                }

            }
        };
        simulator.setup(network, allocator, new VoidLoggerFactory(simulator));
        simulator.insertEvents(
                new Event(simulator, 0, 0) {
                    @Override
                    protected void trigger() {

                    }
                },
                new Event(simulator, 0, 433) {
                    @Override
                    protected void trigger() {

                    }
                },
                new Event(simulator, 0, 433 + 485) {
                    @Override
                    protected void trigger() {

                    }
                }
        );
        simulator.run(10000);

    }

    @Test
    public void testInvalidStartOrEndOfFlow() {
        Simulator simulator = new Simulator();

        // Initialize network
        Network network = new Network(10);
        network.addLink(0, 5, 13.0);
        network.addLink(5, 8, 13.0);
        network.addLink(8, 5, 13.0);
        network.addLink(5, 0, 13.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Illegal destination
                boolean thrown = false;
                try {
                    Connection conn = new Connection(simulator, network.getNode(0), network.getNode(5), 1000);
                    simulator.activateConnection(conn);
                    simulator.addFlowToConnection(conn, createAcyclicPath(network, "0-5-8"));
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

                // Illegal source
                thrown = false;
                try {
                    Connection conn = new Connection(simulator, network.getNode(0), network.getNode(5), 1000);
                    simulator.activateConnection(conn);
                    simulator.addFlowToConnection(conn, createAcyclicPath(network, "8-5"));
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

                // Illegal source & destination
                thrown = false;
                try {
                    Connection conn = new Connection(simulator, network.getNode(0), network.getNode(5), 1000);
                    simulator.activateConnection(conn);
                    simulator.addFlowToConnection(conn, createAcyclicPath(network, "5-0"));
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

            }

        });

    }

    @Test
    public void testConnectionUpdates() {
        Simulator simulator = new Simulator();

        // Initialize network
        // 0 -- 5 -- 8
        Network network = new Network(10);
        network.addLink(0, 5, 13.0);
        network.addLink(5, 0, 13.0);
        network.addLink(5, 8, 13.0);
        network.addLink(8, 5, 13.0);

        Aftermath aftermath = new Aftermath(simulator, network) {

            int i = 0;

            @Override
            public void perform() {

                if (i == 0) {
                    assertEquals(0, simulator.getCurrentTime());
                    startSimpleFlow(simulator, network, "0-5-8", 1000);
                    startSimpleFlow(simulator, network, "0-5-8", 799);
                    startSimpleFlow(simulator, network, "0-5-8", 100);
                    simulator.getActiveConnection(1).setRemainderUpdateThreshold(300);
                } else if (i == 1) {
                    assertEquals(24, simulator.getCurrentTime());
                    assertEquals(2, simulator.getActiveConnections().size());
                    assertEquals(196.0, simulator.getActiveConnection(1).getRemainderUpdateThreshold(), simulator.getFlowPrecision());
                    assertFalse(simulator.getActiveConnection(1).isRemainderUpdateThresholdPassed());
                } else if (i == 2) {
                    assertEquals(55, simulator.getCurrentTime());
                    assertEquals(2, simulator.getActiveConnections().size());
                    assertEquals(-5.5, simulator.getActiveConnection(1).getRemainderUpdateThreshold(), simulator.getFlowPrecision());
                    assertTrue(simulator.getActiveConnection(1).isRemainderUpdateThresholdPassed());
                } else if (i == 3) {
                    assertEquals(131, simulator.getCurrentTime());
                    assertEquals(1, simulator.getActiveConnections().size());
                } else if (i == 4) {
                    assertEquals(147, simulator.getCurrentTime());
                    assertEquals(0, simulator.getActiveConnections().size());
                } else if (i == 5) {
                    assertEquals(1000, simulator.getCurrentTime());
                    assertEquals(0, simulator.getActiveConnections().size());
                }

                // Perform max-min fair allocation
                new GenericMmfAllocator(simulator, network, null , null).perform();

                i++;

            }

        };

        simulator.setup(network, aftermath, new VoidLoggerFactory(simulator));
        simulator.insertEvents(new Event(simulator, 0, 0) {
            @Override
            protected void trigger() {

            }
        });

        simulator.run(1000);

    }

    @Test
    public void testConnectionUpdateInvalidThreshold() {
        Simulator simulator = new Simulator();

        // Initialize network
        // 0 -- 5 -- 8
        Network network = new Network(10);
        network.addLink(0, 5, 13.0);
        network.addLink(5, 0, 13.0);
        network.addLink(5, 8, 13.0);
        network.addLink(8, 5, 13.0);

        Aftermath aftermath = new Aftermath(simulator, network) {

            int i = 0;

            @Override
            public void perform() {

                if (i == 0) {
                    assertEquals(0, simulator.getCurrentTime());
                    startSimpleFlow(simulator, network, "0-5-8", 1000);
                    boolean thrown = false;
                    try {
                        simulator.getActiveConnection(0).setRemainderUpdateThreshold(1001);
                    } catch (IllegalArgumentException e) {
                        thrown = true;
                    }
                    assertTrue(thrown);
                }
                i++;

            }

        };

        simulator.setup(network, aftermath, new VoidLoggerFactory(simulator));
        simulator.insertEvents(new Event(simulator, 0, 0) {
            @Override
            protected void trigger() {

            }
        });

        simulator.run(1000);

    }

    @Test
    public void testExtraInfo() {
        Network network = new Network(4);
        Node node3 = network.getNode(3);
        Node node2 = network.getNode(2);
        Connection connection = new Connection(simulator, node3, node2, 10000);
        assertNull(connection.getMetadata());
        connection.setMetadata(new SimpleStringMetadata("someConnectionLabelText"));
        assertNotNull(connection.getMetadata());
        assertEquals("someConnectionLabelText", ((SimpleStringMetadata) connection.getMetadata()).toCsvValidLabel());
    }

    @Test
    public void testInvalidEndpoints() {
        Network network = new Network(4);
        Node node3 = network.getNode(3);
        try {
            new Connection(simulator, node3, node3, 10000);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

}
