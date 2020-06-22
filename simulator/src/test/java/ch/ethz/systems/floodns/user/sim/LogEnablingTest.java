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

import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.LoggerFactory;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.basicsim.schedule.TrafficSchedule;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static ch.ethz.systems.floodns.PathTestUtility.createAcyclicPath;
import static junit.framework.TestCase.assertTrue;

public class LogEnablingTest {

    @Test
    public void testAllLogEnabling() throws IOException {
        for (int i = -1; i < 9; i++) {
            testLog(i);
        }
    }

    private void testLog(int logNo) throws IOException {
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
        trafficSchedule.addConnectionStartEvent(0, 3, 10000, 30);
        trafficSchedule.addConnectionStartEvent(0, 3, 10000000, 50);

        // Initialize logger factory
        LoggerFactory loggerFactory = new FileLoggerFactory(simulator,
                Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString());
        loggerFactory.setConnectionInfoSavingEnabled(logNo != 0);
        loggerFactory.setConnectionBandwidthSavingEnabled(logNo != 1);
        loggerFactory.setFlowInfoSavingEnabled(logNo != 2);
        loggerFactory.setFlowBandwidthSavingEnabled(logNo != 3);
        loggerFactory.setNodeInfoSavingEnabled(logNo != 4);
        loggerFactory.setNodeNumActiveFlowsSavingEnabled(logNo != 5);
        loggerFactory.setLinkInfoSavingEnabled(logNo != 6);
        loggerFactory.setLinkNumActiveFlowsSavingEnabled(logNo != 7);
        loggerFactory.setLinkUtilizationSavingEnabled(logNo != 8);

        // Setup and run simulator
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), loggerFactory);
        simulator.insertEvents(trafficSchedule.getConnectionStartEvents());
        simulator.run(655);

        // Test the log content
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());
        assertTrue((logNo == 0) == testLogReader.getConnectionIdToInfoLog().isEmpty());
        assertTrue((logNo == 1) == testLogReader.getConnectionIdToBandwidthLog().isEmpty());
        assertTrue((logNo == 2) == testLogReader.getFlowIdToInfoLog().isEmpty());
        assertTrue((logNo == 3) == testLogReader.getFlowIdToBandwidthLog().isEmpty());
        assertTrue((logNo == 4) == testLogReader.getNodeIdToInfoLog().isEmpty());
        assertTrue((logNo == 5) == testLogReader.getNodeIdToNumActiveFlowsLog().isEmpty());
        assertTrue((logNo == 6) == testLogReader.getLinkIdToInfoLog().isEmpty());
        assertTrue((logNo == 7) == testLogReader.getLinkIdToNumActiveFlowsLog().isEmpty());
        assertTrue((logNo == 8) == testLogReader.getLinkIdToUtilizationLog().isEmpty());

    }

}
