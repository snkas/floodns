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

package ch.ethz.systems.floodns.user.log;

import ch.ethz.systems.floodns.core.Aftermath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.basicsim.schedule.TrafficSchedule;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import ch.ethz.systems.floodns.ext.metadata.SimpleStringMetadata;
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static org.junit.Assert.assertEquals;

public class FullLogEqualityTest {

    @Test
    public void testFullEqualityFlowsFairShare() throws IOException {
        Simulator simulator = new Simulator();

        // Full connected n=4 graph with bi-directional links
        //
        // 0 - - 1
        // | \ / |
        // | / \ |
        // 3 - - 2
        //
        final Network network = new Network(4);
        network.addLink(0, 1, 90);
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
            int i = 0;
            @Override
            public void assignStartFlows(Connection connection) {
                switch (i) {
                    case 0:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "0-1-2-3"));
                        break;
                    case 1:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "1-2"));
                        break;

                    case 2:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "3-1-2"));
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "3-0-2"));
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "3-0-1-2"));
                        break;

                    case 3:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "2-0-1-3"));
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "2-0-1-3"));
                        break;

                    case 4:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "2-0-1-3"));
                        break;

                    default:
                        throw new RuntimeException("Unsupported flow.");
                }
                i++;
            }
        };

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, routeDecider);
        trafficSchedule.addConnectionStartEvent(0, 3, 57373, 0);
        trafficSchedule.addConnectionStartEvent(1, 2, 4262, 50);
        trafficSchedule.addConnectionStartEvent(3, 2, 44226, 896);
        trafficSchedule.addConnectionStartEvent(2, 3, 333511, 1043);
        trafficSchedule.addConnectionStartEvent(2, 3, 333511999, 1200);

        // Logger factory
        FileLoggerFactory fileLoggerFactory = new FileLoggerFactory(simulator,
               Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString());

        // Setup and run simulator
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), fileLoggerFactory);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());
        simulator.run(72975);

        // Exact log file equality
        TestLogReader testLogReader = new TestLogReader(fileLoggerFactory);

        // Flow bandwidth
        Map<Integer, Map<Pair<Long, Long>, Double>> flowIdToFlowBandwidth = testLogReader.getFlowIdToBandwidthLog();

        // Exact amount of entries
        assertEquals(3, flowIdToFlowBandwidth.get(0).size());
        assertEquals(1, flowIdToFlowBandwidth.get(1).size());
        assertEquals(2, flowIdToFlowBandwidth.get(2).size());
        assertEquals(2, flowIdToFlowBandwidth.get(3).size());
        assertEquals(2, flowIdToFlowBandwidth.get(4).size());
        assertEquals(3, flowIdToFlowBandwidth.get(5).size());
        assertEquals(3, flowIdToFlowBandwidth.get(6).size());
        assertEquals(2, flowIdToFlowBandwidth.get(7).size());

        // Flow correctness
        //
        // 0 to 50:
        // 0 active with 90.0 bandwidth bottlenecked at 0-1
        //
        // 50 to 136:
        // 0 and 1 active, bottlenecked at 1-2, so 50.0 bandwidth each
        //
        // 136 to 676
        // 0 active with 90.0 bandwidth bottlenecked at 0-1
        //
        // 896 to 1043
        // 2, 3, 4 active with 50.0 bandwidth bottlenecked half at 3-0 and 1-2
        //
        // 1043 to 1174:
        // 2 active with 70.0 bandwidth bottlenecked at 1-2 (b.c. 4 has 30.0 bandwidth)
        // 3 active with 70.0 bandwidth bottlenecked at 3-0 (b.c. 4 has 30.0 bandwidth)
        // 4, 5, 6 active with 30.0 bandwidth bottlenecked at 0-1
        //
        // 1174 to 1200:
        // 5, 6 active with 45.0 bandwidth bottlenecked at 0-1
        //
        // 1200 to 6589:
        // 5, 6, 7 active with 30.0 bandwidth bottlenecked at 0-1
        //
        // 6589 to 72975:
        // 7 active with 90.0 bandwidth bottlenecked at 0-1
        //
        assertEquals(90, flowIdToFlowBandwidth.get(0).get(new ImmutablePair<>((long) 0, (long) 50)), simulator.getFlowPrecision());
        assertEquals(50, flowIdToFlowBandwidth.get(0).get(new ImmutablePair<>((long) 50, (long) 136)), simulator.getFlowPrecision());
        assertEquals(90, flowIdToFlowBandwidth.get(0).get(new ImmutablePair<>((long) 136, (long) 676)), simulator.getFlowPrecision());
        assertEquals(50, flowIdToFlowBandwidth.get(1).get(new ImmutablePair<>((long) 50, (long) 136)), simulator.getFlowPrecision());
        assertEquals(50, flowIdToFlowBandwidth.get(2).get(new ImmutablePair<>((long) 896, (long) 1043)), simulator.getFlowPrecision());
        assertEquals(70, flowIdToFlowBandwidth.get(2).get(new ImmutablePair<>((long) 1043, (long) 1174)), simulator.getFlowPrecision());
        assertEquals(50, flowIdToFlowBandwidth.get(3).get(new ImmutablePair<>((long) 896, (long) 1043)), simulator.getFlowPrecision());
        assertEquals(70, flowIdToFlowBandwidth.get(3).get(new ImmutablePair<>((long) 1043, (long) 1174)), simulator.getFlowPrecision());
        assertEquals(50, flowIdToFlowBandwidth.get(4).get(new ImmutablePair<>((long) 896, (long) 1043)), simulator.getFlowPrecision());
        assertEquals(30, flowIdToFlowBandwidth.get(4).get(new ImmutablePair<>((long) 1043, (long) 1174)), simulator.getFlowPrecision());
        assertEquals(30, flowIdToFlowBandwidth.get(5).get(new ImmutablePair<>((long) 1043, (long) 1174)), simulator.getFlowPrecision());
        assertEquals(45, flowIdToFlowBandwidth.get(5).get(new ImmutablePair<>((long) 1174, (long) 1200)), simulator.getFlowPrecision());
        assertEquals(30, flowIdToFlowBandwidth.get(5).get(new ImmutablePair<>((long) 1200, (long) 6589)), simulator.getFlowPrecision());
        assertEquals(30, flowIdToFlowBandwidth.get(6).get(new ImmutablePair<>((long) 1043, (long) 1174)), simulator.getFlowPrecision());
        assertEquals(45, flowIdToFlowBandwidth.get(6).get(new ImmutablePair<>((long) 1174, (long) 1200)), simulator.getFlowPrecision());
        assertEquals(30, flowIdToFlowBandwidth.get(6).get(new ImmutablePair<>((long) 1200, (long) 6589)), simulator.getFlowPrecision());
        assertEquals(30, flowIdToFlowBandwidth.get(7).get(new ImmutablePair<>((long) 1200, (long) 6589)), simulator.getFlowPrecision());
        assertEquals(90, flowIdToFlowBandwidth.get(7).get(new ImmutablePair<>((long) 6589, (long) 72975)), simulator.getFlowPrecision());

    }

    @Test
    public void testFullEquality() throws IOException {
        Simulator simulator = new Simulator();

        // Directed
        //   1
        //  /  \
        // 0 -  2 -> 3
        final Network network = new Network(4);
        network.addLink(0, 1, 80);
        network.addLink(1, 0, 100);
        network.addLink(0, 2, 100);
        network.addLink(2, 0, 78);
        network.addLink(1, 2, 55);
        network.addLink(2, 1, 30);
        network.addLink(2, 3, 1);


        // Very basic route decider
        RoutingStrategy routeDecider = new RoutingStrategy(simulator) {
            int i = 0;
            @Override
            public void assignStartFlows(Connection connection) {
                switch (i) {
                    case 0:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "0-1-2"));
                        break;
                    case 1:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "1-2"));
                        break;

                    case 2:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "1-0"));
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "1-2-0"));
                        break;

                    case 3:
                        simulator.addFlowToConnection(connection, createAcyclicPath(network, "1-0"));
                        break;

                    default:
                        throw new RuntimeException("Unsupported flow.");
                }
                i++;
            }
        };

        // Traffic schedule
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, routeDecider);
        trafficSchedule.addConnectionStartEvent(0, 2, 57373, 0);
        trafficSchedule.addConnectionStartEvent(1, 2, 4262, 50);
        trafficSchedule.addConnectionStartEvent(1, 0, 44226, 156);
        trafficSchedule.addConnectionStartEvent(1, 0, 22, 156);

        // Allocator
        Aftermath allocator = new Aftermath(simulator, network) {
            @Override
            public void perform() {
                if (network.isFlowActive(0)) {
                    simulator.allocateFlowBandwidth(network.getActiveFlow(0), 10.0 / 3.0);
                    simulator.getNetwork().getNode(0).setMetadata(new SimpleStringMetadata("nodeInfo"));
                    assertEquals("nodeInfo", ((SimpleStringMetadata) simulator.getNetwork().getNode(0).getMetadata()).getLabel());
                    simulator.getNetwork().getLink(0).setMetadata(new SimpleStringMetadata("linkInfo"));
                    simulator.getNetwork().getActiveFlow(0).setMetadata(new SimpleStringMetadata("flowInfo"));
                    simulator.getActiveConnection(0).setMetadata(new SimpleStringMetadata("connInfo"));
                }
                if (network.isFlowActive(1)) {
                    simulator.allocateFlowBandwidth(network.getActiveFlow(1), 23);
                }
                if (network.isFlowActive(2)) {
                    simulator.allocateFlowBandwidth(network.getActiveFlow(2), 22);
                }
                if (network.isFlowActive(3)) {
                    simulator.allocateFlowBandwidth(network.getActiveFlow(3), 11);
                }
            }
        };

        // Logger factory
        FileLoggerFactory fileLoggerFactory = new FileLoggerFactory(simulator,
                Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString());

        // Setup and run simulator
        simulator.setup(network, allocator, fileLoggerFactory);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());
        simulator.run(72975);

        // Exact log file equality
        TestLogReader testLogReader = new TestLogReader(fileLoggerFactory);

        // Connection info
        Map<Integer, List<String>> connectionIdToInfo = testLogReader.getConnectionIdToInfoLog();
        assertEquals(4, connectionIdToInfo.size());
        assertEquals(11, connectionIdToInfo.get(0).size());
        assertEquals(10, connectionIdToInfo.get(1).size());
        assertEquals(10, connectionIdToInfo.get(2).size());
        assertEquals(10, connectionIdToInfo.get(3).size());
        assertEquals("connInfo", connectionIdToInfo.get(0).get(10));
        testConnectionInfo(simulator, connectionIdToInfo.get(0), "0,2,57373.0", "0,0,17212,17212", "T", 10.0 / 3.0, (long) Math.ceil(57373.0 / (10.0 / 3.0)));
        testConnectionInfo(simulator, connectionIdToInfo.get(1), "1,2,4262.0", "1,50,236,186", "T", 23.0, (long) Math.ceil(4262.0 / (23.0)));
        testConnectionInfo(simulator, connectionIdToInfo.get(2), "1,0,44226.0", "2;3,156,1497,1341", "T", 33.0, (long) Math.ceil(44226.0 / (33.0)));
        testConnectionInfo(simulator, connectionIdToInfo.get(3), "1,0,22.0", "4,156,72975,72819", "F", 0.0, 72975 - 156);

        // Connection bandwidth
        Map<Integer, Map<Pair<Long, Long>, Double>> connectionIdToBandwidth = testLogReader.getConnectionIdToBandwidthLog();
        assertEquals(4, connectionIdToBandwidth.size());
        assertEquals(1, connectionIdToBandwidth.get(0).size());
        assertEquals(1, connectionIdToBandwidth.get(1).size());
        assertEquals(1, connectionIdToBandwidth.get(2).size());
        assertEquals(1, connectionIdToBandwidth.get(3).size());
        assertEquals(10.0 / 3.0, connectionIdToBandwidth.get(0).get(new ImmutablePair<>((long) 0, (long) 17212)), simulator.getFlowPrecision());
        assertEquals(23, connectionIdToBandwidth.get(1).get(new ImmutablePair<>((long) 50, (long) 236)), simulator.getFlowPrecision());
        assertEquals(22 + 11, connectionIdToBandwidth.get(2).get(new ImmutablePair<>((long) 156, (long) 1497)), simulator.getFlowPrecision());
        assertEquals(0, connectionIdToBandwidth.get(3).get(new ImmutablePair<>((long) 156, (long) 72975)), simulator.getFlowPrecision());

        // Flow info
        Map<Integer, List<String>> flowIdToInfo = testLogReader.getFlowIdToInfoLog();
        assertEquals(5, flowIdToInfo.size());
        assertEquals(9, flowIdToInfo.get(0).size());
        assertEquals(8, flowIdToInfo.get(1).size());
        assertEquals(8, flowIdToInfo.get(2).size());
        assertEquals(8, flowIdToInfo.get(3).size());
        assertEquals(8, flowIdToInfo.get(4).size());
        assertEquals("flowInfo", flowIdToInfo.get(0).get(8));
        testFlowInfo(simulator, flowIdToInfo.get(0), "0,2,0-[0]->1-[4]->2,0,17212,17212", 17212 * 10.0 / 3.0, 17212);
        testFlowInfo(simulator, flowIdToInfo.get(1), "1,2,1-[4]->2,50,236,186", (236 - 50) * 23.0, 236 - 50);
        testFlowInfo(simulator, flowIdToInfo.get(2), "1,0,1-[1]->0,156,1497,1341", (1497 - 156) * 22.0, 1497 - 156);
        testFlowInfo(simulator, flowIdToInfo.get(3), "1,0,1-[4]->2-[3]->0,156,1497,1341", (1497 - 156) * 11.0, 1497 - 156);
        testFlowInfo(simulator, flowIdToInfo.get(4), "1,0,1-[1]->0,156,72975,72819", 0.0, 72975 - 156);

        // Flow bandwidth
        Map<Integer, Map<Pair<Long, Long>, Double>> flowIdToFlowBandwidth = testLogReader.getFlowIdToBandwidthLog();
        assertEquals(5, flowIdToFlowBandwidth.size());
        assertEquals(1, flowIdToFlowBandwidth.get(0).size());
        assertEquals(1, flowIdToFlowBandwidth.get(1).size());
        assertEquals(1, flowIdToFlowBandwidth.get(2).size());
        assertEquals(1, flowIdToFlowBandwidth.get(3).size());
        assertEquals(1, flowIdToFlowBandwidth.get(4).size());
        assertEquals(10.0 / 3.0, flowIdToFlowBandwidth.get(0).get(new ImmutablePair<>((long) 0, (long) 17212)), simulator.getFlowPrecision());
        assertEquals(23, flowIdToFlowBandwidth.get(1).get(new ImmutablePair<>((long) 50, (long) 236)), simulator.getFlowPrecision());
        assertEquals(22, flowIdToFlowBandwidth.get(2).get(new ImmutablePair<>((long) 156, (long) 1497)), simulator.getFlowPrecision());
        assertEquals(11, flowIdToFlowBandwidth.get(3).get(new ImmutablePair<>((long) 156, (long) 1497)), simulator.getFlowPrecision());
        assertEquals(0, flowIdToFlowBandwidth.get(4).get(new ImmutablePair<>((long) 156, (long) 72975)), simulator.getFlowPrecision());

        // Link info
        Map<Integer, List<String>> linkIdToInfo = testLogReader.getLinkIdToInfoLog();
        assertEquals(7, linkIdToInfo.size());
        assertEquals(8, linkIdToInfo.get(0).size());
        assertEquals(7, linkIdToInfo.get(1).size());
        assertEquals(7, linkIdToInfo.get(2).size());
        assertEquals(7, linkIdToInfo.get(3).size());
        assertEquals(7, linkIdToInfo.get(4).size());
        assertEquals(7, linkIdToInfo.get(5).size());
        assertEquals(7, linkIdToInfo.get(6).size());
        assertEquals("linkInfo", linkIdToInfo.get(0).get(7));
        testLinkInfo(simulator, linkIdToInfo.get(0), "0,1,0,72975,72975", 17212 * (10.0 / 3.0) / (72975 * 80.0), 17212.0 / 72975);
        testLinkInfo(simulator, linkIdToInfo.get(1), "1,0,0,72975,72975", 22.0 * (1497 - 156) / (72975 * 100.0), (1497.0 - 156 + 72975 - 156) / 72975);
        testLinkInfo(simulator, linkIdToInfo.get(2), "0,2,0,72975,72975", 0.0, 0.0);
        testLinkInfo(simulator, linkIdToInfo.get(3), "2,0,0,72975,72975", 11.0 * (1497 - 156) / (72975 * 78.0), (1497.0 - 156) / 72975);
        testLinkInfo(simulator, linkIdToInfo.get(4), "1,2,0,72975,72975", (17212 * (10.0 / 3.0) + (236 - 50) * 23.0 + (1497 - 156) * 11.0) / (72975 * 55.0), (17212 + (236 - 50) + (1497 - 156)) / 72975.0);
        testLinkInfo(simulator, linkIdToInfo.get(5), "2,1,0,72975,72975", 0.0, 0.0);
        testLinkInfo(simulator, linkIdToInfo.get(6), "2,3,0,72975,72975", 0.0, 0.0);

        // Link number active flows
        Map<Integer, Map<Pair<Long, Long>, Integer>> linkIdToNumActiveFlows = testLogReader.getLinkIdToNumActiveFlowsLog();
        assertEquals(7, linkIdToNumActiveFlows.size());
        assertEquals(2, linkIdToNumActiveFlows.get(0).size());
        assertEquals(3, linkIdToNumActiveFlows.get(1).size());
        assertEquals(1, linkIdToNumActiveFlows.get(2).size());
        assertEquals(3, linkIdToNumActiveFlows.get(3).size());
        assertEquals(6, linkIdToNumActiveFlows.get(4).size());
        assertEquals(1, linkIdToNumActiveFlows.get(5).size());
        assertEquals(1, linkIdToNumActiveFlows.get(6).size());

        assertEquals(1, (int) linkIdToNumActiveFlows.get(0).get(new ImmutablePair<>((long) 0, (long) 17212)));
        assertEquals(0, (int) linkIdToNumActiveFlows.get(0).get(new ImmutablePair<>((long) 17212, (long) 72975)));

        assertEquals(0, (int) linkIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 0, (long) 156)));
        assertEquals(2, (int) linkIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 156, (long) 1497)));
        assertEquals(1, (int) linkIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 1497, (long) 72975)));

        assertEquals(0, (int) linkIdToNumActiveFlows.get(2).get(new ImmutablePair<>((long) 0, (long) 72975)));

        assertEquals(0, (int) linkIdToNumActiveFlows.get(3).get(new ImmutablePair<>((long) 0, (long) 156)));
        assertEquals(1, (int) linkIdToNumActiveFlows.get(3).get(new ImmutablePair<>((long) 156, (long) 1497)));
        assertEquals(0, (int) linkIdToNumActiveFlows.get(3).get(new ImmutablePair<>((long) 1497, (long) 72975)));

        assertEquals(1, (int) linkIdToNumActiveFlows.get(4).get(new ImmutablePair<>((long) 0, (long) 50)));
        assertEquals(2, (int) linkIdToNumActiveFlows.get(4).get(new ImmutablePair<>((long) 50, (long) 156)));
        assertEquals(3, (int) linkIdToNumActiveFlows.get(4).get(new ImmutablePair<>((long) 156, (long) 236)));
        assertEquals(2, (int) linkIdToNumActiveFlows.get(4).get(new ImmutablePair<>((long) 236, (long) 1497)));
        assertEquals(1, (int) linkIdToNumActiveFlows.get(4).get(new ImmutablePair<>((long) 1497, (long) 17212)));
        assertEquals(0, (int) linkIdToNumActiveFlows.get(4).get(new ImmutablePair<>((long) 17212, (long) 72975)));

        assertEquals(0, (int) linkIdToNumActiveFlows.get(5).get(new ImmutablePair<>((long) 0, (long) 72975)));

        assertEquals(0, (int) linkIdToNumActiveFlows.get(6).get(new ImmutablePair<>((long) 0, (long) 72975)));

        // Link utilization
        Map<Integer, Map<Pair<Long, Long>, Double>> linkIdToUtilization = testLogReader.getLinkIdToUtilizationLog();
        assertEquals(7, linkIdToUtilization.size());
        assertEquals(2, linkIdToUtilization.get(0).size());
        assertEquals(3, linkIdToUtilization.get(1).size());
        assertEquals(1, linkIdToUtilization.get(2).size());
        assertEquals(3, linkIdToUtilization.get(3).size());
        assertEquals(6, linkIdToUtilization.get(4).size());
        assertEquals(1, linkIdToUtilization.get(5).size());
        assertEquals(1, linkIdToUtilization.get(6).size());

        assertEquals((10.0 / 3.0) / 80.0, linkIdToUtilization.get(0).get(new ImmutablePair<>((long) 0, (long) 17212)), simulator.getFlowPrecision());
        assertEquals(0, linkIdToUtilization.get(0).get(new ImmutablePair<>((long) 17212, (long) 72975)), simulator.getFlowPrecision());

        assertEquals(0, linkIdToUtilization.get(1).get(new ImmutablePair<>((long) 0, (long) 156)), simulator.getFlowPrecision());
        assertEquals(22.0 / 100.0, linkIdToUtilization.get(1).get(new ImmutablePair<>((long) 156, (long) 1497)), simulator.getFlowPrecision());
        assertEquals(0, linkIdToUtilization.get(1).get(new ImmutablePair<>((long) 1497, (long) 72975)), simulator.getFlowPrecision());

        assertEquals(0, linkIdToUtilization.get(2).get(new ImmutablePair<>((long) 0, (long) 72975)), simulator.getFlowPrecision());

        assertEquals(0, linkIdToUtilization.get(3).get(new ImmutablePair<>((long) 0, (long) 156)), simulator.getFlowPrecision());
        assertEquals(11.0 / 78.0, linkIdToUtilization.get(3).get(new ImmutablePair<>((long) 156, (long) 1497)), simulator.getFlowPrecision());
        assertEquals(0, linkIdToUtilization.get(3).get(new ImmutablePair<>((long) 1497, (long) 72975)), simulator.getFlowPrecision());

        assertEquals((10.0 / 3.0) / 55.0, linkIdToUtilization.get(4).get(new ImmutablePair<>((long) 0, (long) 50)), simulator.getFlowPrecision());
        assertEquals((10.0 / 3.0 + 23.0) / 55.0, linkIdToUtilization.get(4).get(new ImmutablePair<>((long) 50, (long) 156)), simulator.getFlowPrecision());
        assertEquals((10.0 / 3.0 + 23.0 + 11.0) / 55.0, linkIdToUtilization.get(4).get(new ImmutablePair<>((long) 156, (long) 236)), simulator.getFlowPrecision());
        assertEquals((10.0 / 3.0 + 11.0) / 55.0, linkIdToUtilization.get(4).get(new ImmutablePair<>((long) 236, (long) 1497)), simulator.getFlowPrecision());
        assertEquals((10.0 / 3.0) / 55.0, linkIdToUtilization.get(4).get(new ImmutablePair<>((long) 1497, (long) 17212)), simulator.getFlowPrecision());
        assertEquals(0, linkIdToUtilization.get(4).get(new ImmutablePair<>((long) 17212, (long) 72975)), simulator.getFlowPrecision());

        assertEquals(0, linkIdToUtilization.get(5).get(new ImmutablePair<>((long) 0, (long) 72975)), simulator.getFlowPrecision());

        assertEquals(0, linkIdToUtilization.get(6).get(new ImmutablePair<>((long) 0, (long) 72975)), simulator.getFlowPrecision());

        // Node info
        Map<Integer, List<String>> nodeIdToInfo = testLogReader.getNodeIdToInfoLog();
        assertEquals(4, nodeIdToInfo.size());
        assertEquals(2, nodeIdToInfo.get(0).size());
        assertEquals(1, nodeIdToInfo.get(1).size());
        assertEquals(1, nodeIdToInfo.get(2).size());
        assertEquals(1, nodeIdToInfo.get(3).size());
        assertEquals("nodeInfo", nodeIdToInfo.get(0).get(1));
        assertEquals((17212 + 1497 - 156 + 1497 - 156 + 72975 - 156) / 72975.0, Double.valueOf(nodeIdToInfo.get(0).get(0)), simulator.getFlowPrecision());
        assertEquals((17212 + (236 - 50) + (1497 - 156) + (1497 - 156) + (72975 - 156)) / 72975.0, Double.valueOf(nodeIdToInfo.get(1).get(0)), simulator.getFlowPrecision());
        assertEquals((17212 + (236 - 50) + (1497 - 156)) / 72975.0, Double.valueOf(nodeIdToInfo.get(2).get(0)), simulator.getFlowPrecision());
        assertEquals(0.0, Double.valueOf(nodeIdToInfo.get(3).get(0)), simulator.getFlowPrecision());

        // Node number active flows
        Map<Integer, Map<Pair<Long, Long>, Integer>> nodeIdToNumActiveFlows = testLogReader.getNodeIdToNumActiveFlowsLog();
        assertEquals(4, nodeIdToNumActiveFlows.size());
        assertEquals(4, nodeIdToNumActiveFlows.get(0).size());
        assertEquals(6, nodeIdToNumActiveFlows.get(1).size());
        assertEquals(6, nodeIdToNumActiveFlows.get(2).size());
        assertEquals(1, nodeIdToNumActiveFlows.get(3).size());

        assertEquals(1, (int) nodeIdToNumActiveFlows.get(0).get(new ImmutablePair<>((long) 0, (long) 156)));
        assertEquals(4, (int) nodeIdToNumActiveFlows.get(0).get(new ImmutablePair<>((long) 156, (long) 1497)));
        assertEquals(2, (int) nodeIdToNumActiveFlows.get(0).get(new ImmutablePair<>((long) 1497, (long) 17212)));
        assertEquals(1, (int) nodeIdToNumActiveFlows.get(0).get(new ImmutablePair<>((long) 17212, (long) 72975)));

        assertEquals(1, (int) nodeIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 0, (long) 50)));
        assertEquals(2, (int) nodeIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 50, (long) 156)));
        assertEquals(5, (int) nodeIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 156, (long) 236)));
        assertEquals(4, (int) nodeIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 236, (long) 1497)));
        assertEquals(2, (int) nodeIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 1497, (long) 17212)));
        assertEquals(1, (int) nodeIdToNumActiveFlows.get(1).get(new ImmutablePair<>((long) 17212, (long) 72975)));

        assertEquals(1, (int) nodeIdToNumActiveFlows.get(2).get(new ImmutablePair<>((long) 0, (long) 50)));
        assertEquals(2, (int) nodeIdToNumActiveFlows.get(2).get(new ImmutablePair<>((long) 50, (long) 156)));
        assertEquals(3, (int) nodeIdToNumActiveFlows.get(2).get(new ImmutablePair<>((long) 156, (long) 236)));
        assertEquals(2, (int) nodeIdToNumActiveFlows.get(2).get(new ImmutablePair<>((long) 236, (long) 1497)));
        assertEquals(1, (int) nodeIdToNumActiveFlows.get(2).get(new ImmutablePair<>((long) 1497, (long) 17212)));
        assertEquals(0, (int) nodeIdToNumActiveFlows.get(2).get(new ImmutablePair<>((long) 17212, (long) 72975)));

        assertEquals(0, (int) nodeIdToNumActiveFlows.get(3).get(new ImmutablePair<>((long) 0, (long) 72975)));

    }

    private void testLinkInfo(Simulator simulator, List<String> info, String start, double avgUtil, double avgNum) {
        assertEquals(start, info.get(0) + "," + info.get(1) + "," + info.get(2) + "," + info.get(3) + "," + info.get(4));
        assertEquals(avgUtil, Double.valueOf(info.get(5)), simulator.getFlowPrecision());
        assertEquals(avgNum, Double.valueOf(info.get(6)), simulator.getFlowPrecision());
    }

    private void testFlowInfo(Simulator simulator, List<String> info, String start, double bandwidthSum, long duration) {
        assertEquals(start, info.get(0) + "," + info.get(1) + "," + info.get(2) + "," + info.get(3) + "," + info.get(4) + "," + info.get(5));
        assertEquals(bandwidthSum, Double.valueOf(info.get(6)), simulator.getFlowPrecision());
        assertEquals(bandwidthSum / duration, Double.valueOf(info.get(7)), simulator.getFlowPrecision());
    }

    private void testConnectionInfo(Simulator simulator, List<String> info, String start, String mid, String end, double bandwidth, long duration) {
        assertEquals(start, info.get(0) + "," + info.get(1) + "," + info.get(2));
        assertEquals(bandwidth * duration, Double.valueOf(info.get(3)), simulator.getFlowPrecision());
        assertEquals(mid, info.get(4) + "," + info.get(5) + "," + info.get(6) + "," + info.get(7));
        assertEquals(bandwidth, Double.valueOf(info.get(8)), simulator.getFlowPrecision());
        assertEquals(end, info.get(9));
    }

}
