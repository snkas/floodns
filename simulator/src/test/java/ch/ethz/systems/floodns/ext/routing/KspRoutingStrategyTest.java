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

import ch.ethz.systems.floodns.PathTestUtility;
import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static ch.ethz.systems.floodns.ext.basicsim.topology.TopologyTestUtility.constructTopology;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

public class KspRoutingStrategyTest {

    @Test
    public void kspOnlyTors() throws IOException {

        // 0 - 1
        // |\ /|\
        // | 4 | 5
        // |/ \|/
        // 2 - 3
        Topology topology = constructTopology(
                6,
                10,
                "set()",
                "set(0, 1, 2, 3, 4, 5)",
                "set(0, 1, 3)",
                "set(0-1,1-3,2-3,0-2,0-4,1-4,2-4,3-4,1-5,5-3)",
                6
        );
        Simulator simulator = new Simulator();
        Network network = topology.getNetwork();
        KspRoutingStrategy strategy = new KspRoutingStrategy(new Simulator(), topology, new Random(12345), 100);

        // All shortest paths between 0 and 3
        int[] counter = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < 1200; i++) {
            AcyclicPath path = strategy.assignSinglePath(new Connection(simulator, network.getNode(0), network.getNode(3), 1000));
            if (PathTestUtility.createAcyclicPath(network, "0-2-3").equals(path)) {
                counter[0]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-2-4-3").equals(path)) {
                counter[1]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-2-4-1-3").equals(path)) {
                counter[2]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-2-4-1-5-3").equals(path)) {
                counter[3]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-4-2-3").equals(path)) {
                counter[4]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-4-3").equals(path)) {
                counter[5]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-4-1-3").equals(path)) {
                counter[6]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-4-1-5-3").equals(path)) {
                counter[7]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-1-4-2-3").equals(path)) {
                counter[8]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-1-4-3").equals(path)) {
                counter[9]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-1-3").equals(path)) {
                counter[10]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-1-5-3").equals(path)) {
                counter[11]++;
            } else {
                fail();
            }
        }
        for (int i = 0; i < 12; i++) {
            assertTrue(counter[i] >= 50);
        }
    }

    @Test
    public void kspWithServers() throws IOException {

        // 0 - 1        ... with 0, 1 and 3 having 2 servers each
        // |\ /|\
        // | 4 | 5
        // |/ \|/
        // 2 - 3
        Topology topology = constructTopology(
                12,
                16,
                "set(6,7,8,9,10,11)",
                "set(0, 1, 2, 3, 4, 5)",
                "set(0, 1, 3)",
                "set(0-1,1-3,2-3,0-2,0-4,1-4,2-4,3-4,1-5,5-3,0-6,0-7,1-8,1-9,10-3,3-11)",
                55
        );
        Simulator simulator = new Simulator();
        Network network = topology.getNetwork();
        KspRoutingStrategy strategy = new KspRoutingStrategy(new Simulator(), topology, new Random(12345), 100);

        // One of the three valiant nodes must be chosen
        int[] counter = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < 1200; i++) {
            AcyclicPath path = strategy.assignSinglePath(new Connection(simulator, network.getNode(6), network.getNode(11), 1000));
            if (PathTestUtility.createAcyclicPath(network, "6-0-2-3-11").equals(path)) {
                counter[0]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-2-4-3-11").equals(path)) {
                counter[1]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-2-4-1-3-11").equals(path)) {
                counter[2]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-2-4-1-5-3-11").equals(path)) {
                counter[3]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-4-2-3-11").equals(path)) {
                counter[4]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-4-3-11").equals(path)) {
                counter[5]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-4-1-3-11").equals(path)) {
                counter[6]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-4-1-5-3-11").equals(path)) {
                counter[7]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-1-4-2-3-11").equals(path)) {
                counter[8]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-1-4-3-11").equals(path)) {
                counter[9]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-1-3-11").equals(path)) {
                counter[10]++;
            } else if (PathTestUtility.createAcyclicPath(network, "6-0-1-5-3-11").equals(path)) {
                counter[11]++;
            } else {
                fail();
            }
        }
        for (int i = 0; i < 12; i++) {
            assertTrue(counter[i] >= 50);
        }

    }

    @Test
    public void kspOnlyTorsLimitedK() throws IOException {

        // 0 - 1
        // |\ /|\
        // | 4 | 5
        // |/ \|/
        // 2 - 3
        Topology topology = constructTopology(
                6,
                10,
                "set()",
                "set(0, 1, 2, 3, 4, 5)",
                "set(0, 1, 3)",
                "set(0-1,1-3,2-3,0-2,0-4,1-4,2-4,3-4,1-5,5-3)",
                6
        );
        Simulator simulator = new Simulator();
        Network network = topology.getNetwork();
        KspRoutingStrategy strategy = new KspRoutingStrategy(new Simulator(), topology, new Random(12345), 8);

        // All shortest paths between 0 and 3
        int[] counter = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < 800; i++) {
            AcyclicPath path = strategy.assignSinglePath(new Connection(simulator, network.getNode(0), network.getNode(3), 1000));
            if (PathTestUtility.createAcyclicPath(network, "0-2-3").equals(path)) {
                counter[0]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-2-4-3").equals(path)) {
                counter[1]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-4-2-3").equals(path)) {
                counter[2]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-4-3").equals(path)) {
                counter[3]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-4-1-3").equals(path)) {
                counter[4]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-1-4-3").equals(path)) {
                counter[5]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-1-3").equals(path)) {
                counter[6]++;
            } else if (PathTestUtility.createAcyclicPath(network, "0-1-5-3").equals(path)) {
                counter[7]++;
            } else {
                fail();
            }
        }
        for (int i = 0; i < 8; i++) {
            assertTrue(counter[i] >= 50);
        }
    }

}
