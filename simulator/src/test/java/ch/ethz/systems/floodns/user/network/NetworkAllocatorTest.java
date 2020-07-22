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

import ch.ethz.systems.floodns.core.*;
import ch.ethz.systems.floodns.ext.basicsim.schedule.ConnectionStartEvent;
import ch.ethz.systems.floodns.ext.basicsim.topology.FileToTopologyConverter;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import ch.ethz.systems.floodns.ext.routing.EcmpRoutingStrategy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ch.ethz.systems.floodns.PathTestUtility.startSemiSpecificFlow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NetworkAllocatorTest {

    @Test
    public void testThreeBandwidthOne() {
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

                // Start some flows
                List<Flow> flows = new ArrayList<>();
                flows.add(startSemiSpecificFlow(simulator, network, "0(0)-1(0)-2"));
                flows.add(startSemiSpecificFlow(simulator, network, "0(1)-1(0)-2"));
                flows.add(startSemiSpecificFlow(simulator, network, "0(2)-1(1)-2"));

                // Check all flows are now active for the network
                assertEquals(3, network.getActiveFlows().size());
                assertTrue(network.getActiveFlows().containsAll(flows));

                for (Flow f : network.getActiveFlows()) {
                    simulator.allocateFlowBandwidth(f, 1);
                }

                // Check allocation
                assertEquals(flows.get(0).getCurrentBandwidth(), 1, simulator.getFlowPrecision());
                assertEquals(flows.get(1).getCurrentBandwidth(), 1, simulator.getFlowPrecision());
                assertEquals(flows.get(2).getCurrentBandwidth(), 1, simulator.getFlowPrecision());

            }

        });

    }

    @Test
    public void testUselessAllocator() throws IOException {
        Simulator simulator = new Simulator();
        Topology topology = FileToTopologyConverter.convert("test_data/tiny_clos_2_to_7.properties");
        Network network = topology.getNetwork();

        Aftermath allocator = new Aftermath(simulator, network) {

            @Override
            public void perform() {

            }

        };
        simulator.setup(network, allocator, new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString()));
        simulator.insertEvents(new ConnectionStartEvent(
                simulator,
                100,
                new Connection(simulator, network.getNode(0), network.getNode(1), 10000),
                new EcmpRoutingStrategy(simulator, topology, new Random(100))
        ));
        simulator.run(1000);

        // Read out the logs
        TestLogReader testLogReader = new TestLogReader((FileLoggerFactory) simulator.getLoggerFactory());

        // Bandwidth consistent
        assertEquals(0.0, testLogReader.getConnectionIdToBandwidthLog().get(0).get(new ImmutablePair<>(100L, 1000L)), simulator.getFlowPrecision());

    }

}
