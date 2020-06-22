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

package ch.ethz.systems.floodns.user.sim;

import ch.ethz.systems.floodns.core.*;
import ch.ethz.systems.floodns.ext.allocator.VoidAllocator;
import ch.ethz.systems.floodns.ext.basicsim.topology.FileToTopologyConverter;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.logger.empty.VoidLoggerFactory;
import ch.ethz.systems.floodns.user.network.NetworkTestHelper;
import ch.ethz.systems.floodns.user.network.TestBody;
import org.junit.Test;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static junit.framework.TestCase.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;

public class SimulatorIllegalTest {

    private class CustomEvent extends Event {

        CustomEvent(Simulator simulator, int priority, long timeFromNow) {
            super(simulator, priority, timeFromNow);
        }

        @Override
        protected void trigger() {
            // Intentionally nothing
        }

    }

    @Test
    public void testIllegalStateNotRunningCalls() {
        Simulator simulator = new Simulator();

        //      5  6  7
        // <fully connected>
        // 0   1   2   3   4
        //
        Topology topology = FileToTopologyConverter.convert("test_data/tiny_clos_5_to_3.properties", 10);
        final Network network = topology.getNetwork();

        // Setup
        Aftermath allocator = new VoidAllocator(simulator, network);
        simulator.setup(network, allocator, new VoidLoggerFactory(simulator));
        assertTrue(simulator.getNetwork() == network);
        assertTrue(simulator.getAftermath() == allocator);

        // Cannot call removing link on simulator if not running
        boolean thrown = false;
        try {
            simulator.removeExistingLink(network.getLink(3));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Cannot call adding link on simulator if not running
        thrown = false;
        try {
            simulator.addNewLink(6, 7, 10.0);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Cannot activate connection on simulator if not running
        thrown = false;
        try {
            Connection connection = new Connection(simulator, network.getNode(0), network.getNode(1), 100);
            simulator.activateConnection(connection);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Cannot terminate connection on simulator if not running
        thrown = false;
        try {
            Connection connection = new Connection(simulator, network.getNode(0), network.getNode(1), 100);
            simulator.terminateConnection(connection);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testIllegalStateCases() {

        // Simulator & network
        Simulator simulator = new Simulator();
        Network network = new Network(2);
        network.addLink(0, 1, 10.0);

        // Insert event before setup
        boolean thrown = false;
        try {
            Event e1 = new CustomEvent(simulator, 0, 0);
            simulator.insertEvents(e1);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Remove link before setup
        thrown = false;
        try {
            simulator.removeExistingLink(network.getLink(0));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Cancel event before setup
        thrown = false;
        try {
            Event e1 = new CustomEvent(simulator, 0, 0);
            simulator.cancelEvent(e1);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Add link before setup
        thrown = false;
        try {
            simulator.addNewLink(0, 1, 10.0);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Start connection before setup
        thrown = false;
        try {
            Connection connection = new Connection(simulator, network.getNode(0), network.getNode(1), 1);
            simulator.activateConnection(connection);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Add flow to connection before setup
        thrown = false;
        try {
            Connection connection = new Connection(simulator, network.getNode(0), network.getNode(1), 1);
            simulator.addFlowToConnection(connection, createAcyclicPath(network, "0-1"));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Allocate flow before setup
        thrown = false;
        try {
            simulator.allocateFlowBandwidth(mock(Flow.class), 10.0);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // End flow before setup
        thrown = false;
        try {
            simulator.endFlow(mock(Flow.class));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testWrongSimulatorInsert() {

        // Setup simulator
        Simulator simulator = new Simulator();
        Simulator simulatorB = new Simulator();
        Network network = new Network(2);
        network.addLink(0, 1, 10.0);
        simulator.setup(network, new VoidAllocator(simulator, network), new VoidLoggerFactory(simulator));

        // Remove link before setup
        boolean thrown = false;
        try {
            simulator.removeExistingLink(network.getLink(0));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Insert event of other simulator
        thrown = false;
        try {
            Event e1 = new CustomEvent(simulatorB, 0, 0);
            simulator.insertEvents(e1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Cancel event of other simulator
        thrown = false;
        try {
            Event e1 = new CustomEvent(simulatorB, 0, 0);
            simulator.cancelEvent(e1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testSimulatorIllegalEventActions() {

        // Setup simulator
        Simulator simulator = new Simulator();
        Network network = new Network(2);
        network.addLink(0, 1, 10.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                Event e1 = new CustomEvent(simulator, 0, 78);
                Event e2 = new CustomEvent(simulator, 0, 78);
                simulator.insertEvents(e1);

                // Re-insert event
                boolean thrown = false;
                try {
                    simulator.insertEvents(e1);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

                // Cancel un-inserted event
                thrown = false;
                try {
                    simulator.cancelEvent(e2);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

                // Double cancel event
                thrown = false;
                try {
                    simulator.cancelEvent(e1);
                    simulator.cancelEvent(e1);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

            }

        });

    }

    @Test
    public void testSimulatorIllegalOtherActions() {

        // Setup simulator
        Simulator simulator = new Simulator();
        final Simulator simulatorB = new Simulator();
        Network network = new Network(2);
        final Network networkB = new Network(2);
        network.addLink(0, 1, 10.0);
        networkB.addLink(0, 1, 10.0);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Remove link of network not of this simulation
                boolean thrown = false;
                try {
                    simulator.removeExistingLink(networkB.getLink(0));
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

                // Start connection not of this simulation
                thrown = false;
                try {
                    Connection connection = new Connection(simulatorB, network.getNode(0), network.getNode(1), 1);
                    simulator.activateConnection(connection);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

                // Double start connection
                thrown = false;
                try {
                    Connection connection = new Connection(simulator, network.getNode(0), network.getNode(1), 1);
                    simulator.activateConnection(connection);
                    simulator.activateConnection(connection);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

                // Terminate connection not of this simulation
                thrown = false;
                try {
                    Connection connection = new Connection(simulatorB, network.getNode(0), network.getNode(1), 1);
                    simulator.terminateConnection(connection);
                } catch (IllegalArgumentException e) {
                    thrown = true;
                }
                assertTrue(thrown);

            }

        });

    }

    @Test
    public void testIllegalPossibleOnlyDuringSimulation() {

        // Setup dummy simulator
        Simulator simulator = new Simulator();
        Network network = new Network(2);
        network.addLink(0, 1, 10.0);
        final Network networkB = new Network(2);
        networkB.addLink(0, 1, 10.0);

        final Event e1 = new CustomEvent(simulator, 0, 10);
        final Event e2 = new CustomEvent(simulator, 0, 6);

        simulator.setup(network, new Aftermath(simulator, network) {

            Flow flowA;

            @Override
            public void perform() {
                if (simulator.getCurrentTime() == 10) {

                    // Insert event in past
                    boolean thrown = false;
                    try {
                        simulator.insertEvents(new CustomEvent(simulator, 0, 0));
                    } catch (IllegalArgumentException e) {
                        thrown = true;
                    }
                    assertTrue(thrown);

                    // Cancel event now
                    thrown = false;
                    try {
                        simulator.cancelEvent(e1);
                    } catch (IllegalArgumentException e) {
                        thrown = true;
                    }
                    assertTrue(thrown);

                    // Cancel event in past
                    thrown = false;
                    try {
                        simulator.cancelEvent(e2);
                    } catch (IllegalArgumentException e) {
                        thrown = true;
                    }
                    assertTrue(thrown);

                    // Inactive connection add flow
                    thrown = false;
                    try {
                        Connection connection = new Connection(simulator, network.getNode(0), network.getNode(1), 10);
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "0-1"));
                    } catch (IllegalArgumentException e) {
                        thrown = true;
                    }
                    assertTrue(thrown);

                    // Flow is not of the same network
                    thrown = false;
                    try {
                        Connection connection = new Connection(simulator, network.getNode(0), network.getNode(1), 10);
                        simulator.activateConnection(connection);
                        simulator.addFlowToConnection(connection, createAcyclicPath(networkB, "0-1"));
                    } catch (IllegalArgumentException e) {
                        thrown = true;
                    }
                    assertTrue(thrown);

                    // Now for the next time tick, start a connection
                    Connection connectionA = new Connection(simulator, network.getNode(0), network.getNode(1), 3);
                    simulator.activateConnection(connectionA);
                    flowA = simulator.addFlowToConnection(connectionA, createAcyclicPath(network, "0-1"));
                    simulator.allocateFlowBandwidth(flowA, 10.0);

                } else if (simulator.getCurrentTime() == 11) {

                    // Allocate bandwidth of inactive flow
                    boolean thrown = false;
                    try {
                        simulator.allocateFlowBandwidth(flowA, 10.0);
                    } catch (IllegalArgumentException e) {
                        thrown = true;
                    }
                    assertTrue(thrown);

                    // End inactive flow
                    thrown = false;
                    try {
                        simulator.endFlow(flowA);
                    } catch (IllegalArgumentException e) {
                        thrown = true;
                    }
                    assertTrue(thrown);

                }
            }

        }, new VoidLoggerFactory(simulator));

        // Insert start event
        simulator.insertEvents(e1, e2);
        simulator.run(10000);

    }

    @Test
    public void testSimulatorInceptionIllegal() {

        // Setup dummy simulator
        final Simulator simulatorA = new Simulator();
        final Network networkA = new Network(2);
        networkA.addLink(0, 1, 10.0);
        final Simulator simulatorB = new Simulator();
        final Network networkB = new Network(2);
        networkB.addLink(0, 1, 10.0);

        NetworkTestHelper.runTest(simulatorA, networkA, new TestBody(simulatorA, networkA) {

            @Override
            public void test() {

                NetworkTestHelper.runTest(simulatorB, networkB, new TestBody(simulatorB, networkB) {

                    @Override
                    public void test() {

                        // Start connection not of this simulation
                        boolean thrown = false;
                        try {
                            Connection connectionA = new Connection(simulatorA, networkA.getNode(0), networkA.getNode(1), 1);
                            simulatorA.activateConnection(connectionA);
                            Connection connectionB = new Connection(simulatorB, networkB.getNode(0), networkB.getNode(1), 1);
                            simulatorB.activateConnection(connectionB);
                            simulatorA.addFlowToConnection(connectionB, createAcyclicPath(networkB, "0-1"));
                        } catch (IllegalArgumentException e) {
                            thrown = true;
                        }
                        assertTrue(thrown);

                        // Allocate flow not of this simulation
                        thrown = false;
                        try {
                            Connection connectionA = new Connection(simulatorA, networkA.getNode(0), networkA.getNode(1), 1);
                            simulatorA.activateConnection(connectionA);
                            Flow flowA = simulatorA.addFlowToConnection(connectionA, createAcyclicPath(networkA, "0-1"));
                            simulatorB.allocateFlowBandwidth(flowA, 10.0);
                        } catch (IllegalArgumentException e) {
                            thrown = true;
                        }
                        assertTrue(thrown);

                        // End flow not of this simulation
                        thrown = false;
                        try {
                            Connection connectionA = new Connection(simulatorA, networkA.getNode(0), networkA.getNode(1), 1);
                            simulatorA.activateConnection(connectionA);
                            Flow flowA = simulatorA.addFlowToConnection(connectionA, createAcyclicPath(networkA, "0-1"));
                            simulatorB.endFlow(flowA);
                        } catch (IllegalArgumentException e) {
                            thrown = true;
                        }
                        assertTrue(thrown);

                        // Terminate connection not activated
                        thrown = false;
                        try {
                            Connection connectionA = new Connection(simulatorA, networkA.getNode(0), networkA.getNode(1), 1);
                            simulatorA.terminateConnection(connectionA);
                        } catch (IllegalArgumentException e) {
                            thrown = true;
                        }
                        assertTrue(thrown);

                    }

                });

            }

        });

    }

}
