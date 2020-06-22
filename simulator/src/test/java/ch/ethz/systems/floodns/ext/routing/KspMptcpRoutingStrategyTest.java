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

package ch.ethz.systems.floodns.ext.routing;

import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.basicsim.schedule.ConnectionStartEvent;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static ch.ethz.systems.floodns.ext.basicsim.topology.TopologyTestUtility.constructTopology;
import static org.junit.Assert.assertEquals;

public class KspMptcpRoutingStrategyTest {

    @Test
    public void testManyK() throws IOException {

        //    0    1 -- 5 -- 7
        //    |  /      |    |
        //    2 -- 3 -- 4 -- 6
        //
        Topology topology = constructTopology(
                8,
                9,
                "set()",
                "set(0, 1, 2, 3, 4, 5, 6, 7)",
                "set(3, 5)",
                "set(0-2,1-2,2-3,3-4,1-5,5-4,4-6,5-7,6-7)",
                10.0
        );

        Simulator simulator = new Simulator();

        // Create strategy
        KspMultiPathRoutingStrategy strategy = new KspMultiPathRoutingStrategy(simulator, topology, 5);

        // Assign k-shortest paths of 3 -> 5
        Network network = topology.getNetwork();
        FileLoggerFactory loggerFactory = new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString());
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), loggerFactory);
        Connection connection = new Connection(simulator, network.getNode(3), network.getNode(5), 1000);
        simulator.insertEvents(new ConnectionStartEvent(simulator, 0, connection, strategy));
        simulator.run(1);

        String[] path0 = new TestLogReader(loggerFactory).getFlowIdToInfoLog().get(0).get(2).split("-\\[[0-9]*]->");
        String[] path1 = new TestLogReader(loggerFactory).getFlowIdToInfoLog().get(1).get(2).split("-\\[[0-9]*]->");
        String[] path2 = new TestLogReader(loggerFactory).getFlowIdToInfoLog().get(2).get(2).split("-\\[[0-9]*]->");
        int count = new TestLogReader(loggerFactory).getConnectionIdToInfoLog().get(0).get(4).split(";").length;

        assertEquals(3, count);

        // First one, 3 -> 4 -> 5
        assertEquals(3, path0.length);
        assertEquals(3, (int) Integer.valueOf(path0[0]));
        assertEquals(4, (int) Integer.valueOf(path0[1]));
        assertEquals(5, (int) Integer.valueOf(path0[2]));

        // Second one, 3 -> 2 -> 1 -> 5
        assertEquals(4, path1.length);
        assertEquals(3, (int) Integer.valueOf(path1[0]));
        assertEquals(2, (int) Integer.valueOf(path1[1]));
        assertEquals(1, (int) Integer.valueOf(path1[2]));
        assertEquals(5, (int) Integer.valueOf(path1[3]));

        // Third one, 3 -> 4 -> 6 -> 7 -> 5
        assertEquals(5, path2.length);
        assertEquals(3, (int) Integer.valueOf(path2[0]));
        assertEquals(4, (int) Integer.valueOf(path2[1]));
        assertEquals(6, (int) Integer.valueOf(path2[2]));
        assertEquals(7, (int) Integer.valueOf(path2[3]));
        assertEquals(5, (int) Integer.valueOf(path2[4]));

    }

    @Test
    public void testSquareExtended() throws IOException {

        //       0 -- 3 -- 5
        //       |  / |
        //  4 -- 1 -- 2
        //
        Topology topology = constructTopology(
                6,
                7,
                "set(4, 5)",
                "set(0, 1, 2, 3)",
                "set(1, 3)",
                "set(4-1,1-2,0-1,0-3,1-3,2-3,3-5)",
                10.0
        );

        Simulator simulator = new Simulator();

        // Create strategy
        KspMultiPathRoutingStrategy strategy = new KspMultiPathRoutingStrategy(simulator, topology, 5);

        // Assign k-shortest paths of 4 -> 5
        Network network = topology.getNetwork();
        FileLoggerFactory loggerFactory = new FileLoggerFactory(simulator, Files.createTempDirectory("temp").toAbsolutePath().toString());
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), loggerFactory);
        Connection connection = new Connection(simulator, network.getNode(4), network.getNode(5), 1000);
        simulator.insertEvents(new ConnectionStartEvent(simulator, 0, connection, strategy));
        simulator.run(1);

        String[] path0 = new TestLogReader(loggerFactory).getFlowIdToInfoLog().get(0).get(2).split("-\\[[0-9]*]->");
        String[] path1 = new TestLogReader(loggerFactory).getFlowIdToInfoLog().get(1).get(2).split("-\\[[0-9]*]->");
        String[] path2 = new TestLogReader(loggerFactory).getFlowIdToInfoLog().get(2).get(2).split("-\\[[0-9]*]->");
        int count = new TestLogReader(loggerFactory).getConnectionIdToInfoLog().get(0).get(4).split(";").length;

        assertEquals(3, count);

        // First, 3 -> 2 -> 1 -> 5
        assertEquals(4, path0.length);
        assertEquals(4, (int) Integer.valueOf(path0[0]));
        assertEquals(1, (int) Integer.valueOf(path0[1]));
        assertEquals(3, (int) Integer.valueOf(path0[2]));
        assertEquals(5, (int) Integer.valueOf(path0[3]));

        // Other two
        assertEquals(5, path1.length);
        assertEquals(5, path2.length);

    }

}
