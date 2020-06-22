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
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.allocator.VoidAllocator;
import ch.ethz.systems.floodns.ext.basicsim.schedule.TrafficSchedule;
import ch.ethz.systems.floodns.ext.basicsim.topology.FileToTopologyConverter;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.logger.empty.VoidLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import ch.ethz.systems.floodns.ext.routing.EcmpRoutingStrategy;
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimulatorBasicTest {

    @Test
    public void testEnsureSimulatorGetComponents() {
        Simulator simulator = new Simulator();
        Topology topology = FileToTopologyConverter.convert("test_data/tiny_clos_5_to_3.properties", 10);
        final Network network = topology.getNetwork();
        Aftermath allocator = new VoidAllocator(simulator, network);
        LoggerFactory loggerFactory = new VoidLoggerFactory(simulator);
        simulator.setup(network, allocator, loggerFactory);
        assertTrue(simulator.getNetwork() == network);
        assertTrue(simulator.getAftermath() == allocator);
        assertTrue(simulator.getLoggerFactory() == loggerFactory);
    }

    @Test
    public void testSimulatorMixUp() {

        // Setup
        Simulator simulator = new Simulator();
        Simulator simulatorB = new Simulator();
        Topology topology = FileToTopologyConverter.convert("test_data/tiny_clos_5_to_3.properties", 10);
        final Network network = topology.getNetwork();
        Aftermath allocator = new VoidAllocator(simulator, network);
        Aftermath allocatorB = new VoidAllocator(simulatorB, network);
        LoggerFactory loggerFactory = new VoidLoggerFactory(simulator);
        LoggerFactory loggerFactoryB = new VoidLoggerFactory(simulatorB);

        // Allocator has wrong simulator
        boolean thrown = false;
        try {
            simulator.setup(network, allocatorB, loggerFactory);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Logger factory has wrong simulator
        thrown = false;
        try {
            simulator.setup(network, allocator, loggerFactoryB);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testDoubleRunOrSetupFail() {
        Simulator simulator = new Simulator();

        //      5  6  7
        // <fully connected>
        // 0   1   2   3   4
        //
        Topology topology = FileToTopologyConverter.convert("test_data/tiny_clos_5_to_3.properties", 10);
        Network network = topology.getNetwork();

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new EcmpRoutingStrategy(simulator, topology, new Random(229)));
        trafficSchedule.addConnectionStartEvent(0, 1, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 2, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 4, 10000000, 0);

        // Setup and run legally
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), new VoidLoggerFactory(simulator));
        simulator.run(1000);

        // Illegal to run again
        boolean thrown = false;
        try {
            simulator.run(10002);
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // ... or to setup again
        thrown = false;
        try {
            simulator.setup(network, new SimpleMmfAllocator(simulator, network), new VoidLoggerFactory(simulator));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testRunCommand() throws IOException {
        Simulator simulator = new Simulator();

        //      5  6  7
        // <fully connected>
        // 0   1   2   3   4
        //
        Topology topology = FileToTopologyConverter.convert("test_data/tiny_clos_5_to_3.properties", 10);
        Network network = topology.getNetwork();

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new EcmpRoutingStrategy(simulator, topology, new Random(229)));
        trafficSchedule.addConnectionStartEvent(0, 1, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 2, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 4, 10000000, 0);

        // Setup and run legally
        FileLoggerFactory loggerFactory = new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString());
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), loggerFactory);
        simulator.run(1000);

        // Now run some command on it
        loggerFactory.runCommandOnLogFolder("echo");

    }


    @Test
    public void testEcmp() throws IOException {
        Simulator simulator = new Simulator();

        //      5  6  7
        // <fully connected>
        // 0   1   2   3   4
        //
        Topology topology = FileToTopologyConverter.convert("test_data/tiny_clos_5_to_3.properties", 10);
        Network network = topology.getNetwork();

        // Very basic route decider
        RoutingStrategy routeDecider = new EcmpRoutingStrategy(simulator, topology, new Random(209049));

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, routeDecider);
        trafficSchedule.addConnectionStartEvent(0, 1, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 2, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000000, 0);
        trafficSchedule.addConnectionStartEvent(0, 4, 10000000, 0);

        // Setup and run simulator
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), new FileLoggerFactory(simulator, Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString()));

        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());
        simulator.setProgressShowInterval(100);
        simulator.run(1000);

        // Testing out in-memory observations
        FileLoggerFactory factory = (FileLoggerFactory) simulator.getLoggerFactory();

        // Number of active flows in each node
        TestLogReader testLogReader = new TestLogReader(factory);
        assertEquals(4, (int) testLogReader.getNodeIdToNumActiveFlowsLog().get(0).get(new ImmutablePair<>(0L, 1000L)));
        assertEquals(1, (int) testLogReader.getNodeIdToNumActiveFlowsLog().get(1).get(new ImmutablePair<>(0L, 1000L)));
        assertEquals(1, (int) testLogReader.getNodeIdToNumActiveFlowsLog().get(2).get(new ImmutablePair<>(0L, 1000L)));
        assertEquals(1, (int) testLogReader.getNodeIdToNumActiveFlowsLog().get(3).get(new ImmutablePair<>(0L, 1000L)));
        assertEquals(1, (int) testLogReader.getNodeIdToNumActiveFlowsLog().get(4).get(new ImmutablePair<>(0L, 1000L)));

        // Flow throughput equality, as there is only a single flow per connection
        for (int i = 0; i < 4; i++) {
            assertEquals(
                    testLogReader.getFlowIdToBandwidthLog().get(i).get(new ImmutablePair<>(0L, 1000L)),
                    testLogReader.getConnectionIdToBandwidthLog().get(i).get(new ImmutablePair<>(0L, 1000L))
            );
        }

    }

    @Test
    public void testFullyConnectedN4() throws IOException {
        Simulator simulator = new Simulator();

        // Full connected n=4 graph with bi-directional links
        //
        // 0 - - 1
        // | \ / |
        // | / \ |
        // 3 - - 2
        //
        final Network network = new Network(4);
        network.addLink(0, 1, 100);
        network.addLink(1, 0, 100);
        network.addLink(1, 2, 100);
        network.addLink(2, 1, 100);
        network.addLink(2, 3, 100);
        network.addLink(3, 2, 100);
        network.addLink(3, 0, 100);
        network.addLink(0, 3, 100);
        network.addLink(3, 1, 100);
        network.addLink(1, 3, 100);
        network.addLink(0, 2, 100);
        network.addLink(2, 0, 100);

        // Very basic route decider
        RoutingStrategy routeDecider = new RoutingStrategy(simulator) {
            @Override
            public void assignStartFlows(Connection connection) {
                if (connection.getSrcNodeId() == 0 && connection.getDstNodeId() == 3) {
                    simulator.addFlowToConnection(connection, createAcyclicPath(network, "0-1-2-3"));
                } else {
                    throw new RuntimeException("Unsupported flow.");
                }
            }
        };

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, routeDecider);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000, 0);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000, 50);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000000, 50);

        // Setup and run simulator
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), new FileLoggerFactory(simulator,
                Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString()));
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());
        simulator.run(1000);
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Connection bandwidth
        Map<Integer, Map<Pair<Long, Long>, Double>> connectionIdToConnectionBandwidth = testLogReader.getConnectionIdToBandwidthLog();
        assertEquals(2, connectionIdToConnectionBandwidth.get(0).size());
        assertEquals(100.0, connectionIdToConnectionBandwidth.get(0).get(new ImmutablePair<>((long) 0, (long) 50)), simulator.getFlowPrecision());
        assertEquals(100.0/3.0, connectionIdToConnectionBandwidth.get(0).get(new ImmutablePair<>((long) 50, (long) 200)), simulator.getFlowPrecision());

        assertEquals(2, connectionIdToConnectionBandwidth.get(1).size());
        assertEquals(100.0/3.0, connectionIdToConnectionBandwidth.get(1).get(new ImmutablePair<>((long) 50, (long) 200)), simulator.getFlowPrecision());
        assertEquals(50.0, connectionIdToConnectionBandwidth.get(1).get(new ImmutablePair<>((long) 200, (long) 300)), simulator.getFlowPrecision());

        assertEquals(3, connectionIdToConnectionBandwidth.get(2).size());
        assertEquals(100.0/3.0, connectionIdToConnectionBandwidth.get(2).get(new ImmutablePair<>((long) 50, (long) 200)), simulator.getFlowPrecision());
        assertEquals(50.0, connectionIdToConnectionBandwidth.get(2).get(new ImmutablePair<>((long) 200, (long) 300)), simulator.getFlowPrecision());
        assertEquals(100.0, connectionIdToConnectionBandwidth.get(2).get(new ImmutablePair<>((long) 300, (long) 1000)), simulator.getFlowPrecision());

        // Flow bandwidth
        Map<Integer, Map<Pair<Long, Long>, Double>> flowIdToFlowBandwidth = testLogReader.getFlowIdToBandwidthLog();
        assertEquals(2, flowIdToFlowBandwidth.get(0).size());
        assertEquals(100.0, flowIdToFlowBandwidth.get(0).get(new ImmutablePair<>((long) 0, (long) 50)), simulator.getFlowPrecision());
        assertEquals(100.0/3.0, flowIdToFlowBandwidth.get(0).get(new ImmutablePair<>((long) 50, (long) 200)), simulator.getFlowPrecision());

        assertEquals(2, flowIdToFlowBandwidth.get(1).size());
        assertEquals(100.0/3.0, flowIdToFlowBandwidth.get(1).get(new ImmutablePair<>((long) 50, (long) 200)), simulator.getFlowPrecision());
        assertEquals(50.0, flowIdToFlowBandwidth.get(1).get(new ImmutablePair<>((long) 200, (long) 300)), simulator.getFlowPrecision());

        assertEquals(3, flowIdToFlowBandwidth.get(2).size());
        assertEquals(100.0/3.0, flowIdToFlowBandwidth.get(2).get(new ImmutablePair<>((long) 50, (long) 200)), simulator.getFlowPrecision());
        assertEquals(50.0, flowIdToFlowBandwidth.get(2).get(new ImmutablePair<>((long) 200, (long) 300)), simulator.getFlowPrecision());
        assertEquals(100.0, flowIdToFlowBandwidth.get(2).get(new ImmutablePair<>((long) 300, (long) 1000)), simulator.getFlowPrecision());

    }

    @Test
    public void testConnectionFinish() throws IOException {
        Simulator simulator = new Simulator();

        // Full connected n=4 graph with bi-directional links
        //
        // 0 - - 1
        // | \ / |
        // | / \ |
        // 3 - - 2
        //
        final Network network = new Network(2);
        network.addLink(0, 1, 100);

        // Very basic route decider
        RoutingStrategy routeDecider = new RoutingStrategy(simulator) {
            @Override
            public void assignStartFlows(Connection connection) {
                simulator.addFlowToConnection(connection, createAcyclicPath(network, "0-1"));
            }
        };

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, routeDecider);
        trafficSchedule.addConnectionStartEvent(0, 1, 1000000000, 0);

        // Setup and run simulator
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), new FileLoggerFactory(simulator,
                Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString()));
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());
        simulator.run(1000);
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Connection values bandwidth
        Map<Integer, List<String>> connIdToInfo = testLogReader.getConnectionIdToInfoLog();
        List<String> info = connIdToInfo.get(0);

        // Format:
        // connection_id,source_node_id,dest_node_id,total_size,amount_sent,FLOW_LIST,start_time,end_time,duration,average_bandwidth,COMPLETED
        assertEquals(0, (int) Integer.valueOf(info.get(0)));
        assertEquals(1, (int) Integer.valueOf(info.get(1)));
        assertEquals(1000000000, Double.valueOf(info.get(2)), simulator.getFlowPrecision());
        assertEquals(100000, Double.valueOf(info.get(3)), simulator.getFlowPrecision());
        assertEquals("0", info.get(4));
        assertEquals(0, (int) Integer.valueOf(info.get(5)));
        assertEquals(1000, (int) Integer.valueOf(info.get(6)));
        assertEquals(1000, (int) Integer.valueOf(info.get(7)));
        assertEquals(100, Double.valueOf(info.get(8)), simulator.getFlowPrecision());
        assertEquals("F", info.get(9));

    }

    @Test
    public void testCalculateFinish() {
        Simulator simulator = new Simulator();

        //      5  6  7
        // <fully connected>
        // 0   1   2   3   4
        //
        Topology topology = FileToTopologyConverter.convert("test_data/tiny_clos_5_to_3.properties", 10);
        final Network network = topology.getNetwork();

        // Setup
        final List<Long> finishTimesA = new ArrayList<>();
        final List<Long> finishTimesB = new ArrayList<>();
        simulator.setup(network, new Aftermath(simulator, network) {
            @Override
            public void perform() {
                if (simulator.getActiveConnections().size() == 2) {
                    simulator.allocateFlowBandwidth(network.getActiveFlow(0), 3.5);
                    simulator.allocateFlowBandwidth(network.getActiveFlow(1), 2.9);
                    finishTimesA.add(simulator.getActiveConnection(0).timeTillUpdateNeeded());
                    finishTimesB.add(simulator.getActiveConnection(1).timeTillUpdateNeeded());
                } else if (simulator.getActiveConnections().size() == 1) {
                    simulator.allocateFlowBandwidth(network.getActiveFlow(1), 5.9);
                    finishTimesB.add(simulator.getActiveConnection(1).timeTillUpdateNeeded());
                }
            }
        }, new VoidLoggerFactory(simulator));

        // Traffic
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, new RoutingStrategy(simulator) {
            @Override
            public void assignStartFlows(Connection connection) {
                if (connection.getConnectionId() == 0) {
                    simulator.addFlowToConnection(connection, createAcyclicPath(network, "0-5-2"));
                } else if (connection.getConnectionId() == 1) {
                    simulator.addFlowToConnection(connection, createAcyclicPath(network, "1-6-2"));
                }
            }
        });
        trafficSchedule.addConnectionStartEvent(0, 2, 887.2, 0);
        trafficSchedule.addConnectionStartEvent(1, 2, 8938.43, 0);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());

        // Run
        simulator.run(5000);

        // Time till completion updates
        assertEquals(254, (long) finishTimesA.get(0));
        assertEquals(3083, (long) finishTimesB.get(0));
        assertEquals(1391, (long) finishTimesB.get(1));

    }

    @Test
    public void testFinishCorrectly() {
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
                    simulator.activateConnection(connection);

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
                }
        );
        simulator.run(100);

    }

    @Test
    public void testAllTriggeredEnding() {
        Simulator simulator = new Simulator();

        // Initialize network
        Network network = new Network(4);
        network.addLink(2, 3, 13.0);

        simulator.setup(network, new VoidAllocator(simulator, network), new VoidLoggerFactory(simulator));
        final List<Boolean> triggeredList = new ArrayList<>();
        simulator.insertEvents(
                new Event(simulator, -1, 100) {
                    @Override
                    protected void trigger() {
                        triggeredList.add(Boolean.TRUE);
                    }
                },
                new Event(simulator, 1, 100) {
                    @Override
                    protected void trigger() {
                        triggeredList.add(Boolean.TRUE);
                    }
                },
                new Event(simulator, 1, 101) {
                    @Override
                    protected void trigger() {
                        triggeredList.add(Boolean.TRUE);
                    }
                },
                new Event(simulator, 1, 99) {
                    @Override
                    protected void trigger() {
                        triggeredList.add(Boolean.TRUE);
                    }
                }
        );
        simulator.run(100);
        assertEquals(3, triggeredList.size());

    }

    @Test
    public void testTriggeredCase() {
        Simulator simulator = new Simulator();

        // Initialize network
        Network network = new Network(4);
        network.addLink(2, 3, 13.0);

        simulator.setup(network, new VoidAllocator(simulator, network), new VoidLoggerFactory(simulator));
        final List<Boolean> triggeredList = new ArrayList<>();
        simulator.insertEvents(
                new Event(simulator, 1, 55) {
                    @Override
                    protected void trigger() {
                        triggeredList.add(Boolean.TRUE);
                    }
                },
                new Event(simulator, 1, 155) {
                    @Override
                    protected void trigger() {
                        triggeredList.add(Boolean.FALSE);
                    }
                }
        );
        simulator.run(100);
        assertEquals(1, triggeredList.size());
        assertTrue(triggeredList.get(0));

    }

    @Test
    public void testInactiveSecondEvent() {
        Simulator simulator = new Simulator();

        // Initialize network
        Network network = new Network(4);
        network.addLink(2, 3, 13.0);

        simulator.setup(network, new VoidAllocator(simulator, network), new VoidLoggerFactory(simulator));
        final List<Boolean> triggeredList = new ArrayList<>();
        final Event eventB = new Event(simulator, 1, 78) {
            @Override
            protected void trigger() {
                triggeredList.add(Boolean.FALSE);
            }
        };
        simulator.insertEvents(
                new Event(simulator, 1, 55) {
                    @Override
                    protected void trigger() {
                        triggeredList.add(Boolean.TRUE);
                        simulator.cancelEvent(eventB);
                    }
                },
                eventB

        );
        simulator.run(100);
        assertEquals(1, triggeredList.size());
        assertTrue(triggeredList.get(0));

    }

}
