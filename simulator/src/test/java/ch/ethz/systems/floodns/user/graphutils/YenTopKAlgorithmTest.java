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

package ch.ethz.systems.floodns.user.graphutils;

import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.ext.basicsim.topology.FileToTopologyConverter;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.graphutils.YenTopKspAlgorithmWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class YenTopKAlgorithmTest {

    // Class logger
    private static final Logger logger = LogManager.getLogger(YenTopKAlgorithmTest.class);

    @Test
    public void testSimpleUnweighted() {

        // 3 -------
        // |       |
        // 0 - 1 - 4
        // |
        // 2
        // |
        // 5

        Network network = new Network(6);
        network.addLink(0, 1, 1);
        network.addLink(1, 4, 1);
        network.addLink(0, 2, 1);
        network.addLink(0, 3, 1);
        network.addLink(3, 4, 1);
        network.addLink(1, 0, 7);
        network.addLink(4, 1, 1);
        network.addLink(2, 0, 1);
        network.addLink(3, 0, 1);
        network.addLink(4, 3, 7);
        network.addLink(2, 5, 3);
        network.addLink(5, 2, 3);

        YenTopKspAlgorithmWrapper alg = new YenTopKspAlgorithmWrapper(network);
        List<AcyclicPath> p = alg.getShortestPaths(3, 5, 2);

        assertEquals(2, p.size());

        AcyclicPath p1 = p.get(0);
        assertEquals(0, p1.get(0).getTo());
        assertEquals(2, p1.get(1).getTo());
        assertEquals(5, p1.get(2).getTo());
        assertEquals(3, p1.size());

        AcyclicPath p2 = p.get(1);
        assertEquals(4, p2.get(0).getTo());
        assertEquals(1, p2.get(1).getTo());
        assertEquals(0, p2.get(2).getTo());
        assertEquals(2, p2.get(3).getTo());
        assertEquals(5, p2.get(4).getTo());
        assertEquals(5, p2.size());

    }

    @Test
    public void testNoPath() {

        // 3 <-------
        // <|       |
        // 0 - 1 - 4
        // |
        // 2
        // |
        // 5

        Network network = new Network(6);
        network.addLink(0, 1, 1);
        network.addLink(1, 4, 1);
        network.addLink(0, 2, 1);
        network.addLink(0, 3, 1);
        network.addLink(1, 0, 7);
        network.addLink(4, 1, 1);
        network.addLink(2, 0, 1);
        network.addLink(4, 3, 7);
        network.addLink(2, 5, 3);
        network.addLink(5, 2, 3);

        YenTopKspAlgorithmWrapper alg = new YenTopKspAlgorithmWrapper(network);
        List<AcyclicPath> p = alg.getShortestPaths(3, 5, 1000);
        assertEquals(0, p.size());

    }

    @Test
    public void testSingleMulti() {

        Network network = new Network(2);
        network.addLink(0, 1, 1);
        network.addLink(0, 1, 1);
        network.addLink(0, 1, 1);
        network.addLink(0, 1, 1);


        boolean thrown = false;
        try {
            new YenTopKspAlgorithmWrapper(network);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testFatTrees() {
        for (int k : new int[]{4, 6, 8, 10, 12}) {
            for (String apx : new String[]{ "_sym", "_asym"}) {
                testFatTree(k, apx);
            }
        }

    }

    private void testFatTree(int kFatTree, String apx) {

        // Generate network
        Topology topology = FileToTopologyConverter.convert(
                "test_data/fat_tree_k" + kFatTree + "" + apx + ".properties"
        );
        Network network = topology.getNetwork();

        // Calculate shortest paths
        YenTopKspAlgorithmWrapper alg = new YenTopKspAlgorithmWrapper(network);
        int targetK = 2 * ((kFatTree / 2) * (kFatTree / 2));
        List<AcyclicPath> p = alg.getShortestPaths(0, kFatTree * kFatTree / 2 - 1, targetK);

        // No duplicates
        long len = 0;
        long count = 0;
        for (int i = 0; i < targetK; i++) {
            assertTrue(len <= p.get(i).size());
            len = p.get(i).size();
            if (len == 4) {
                count++;
            }
            for (int j = 0; j < targetK; j++) {
                if (i != j) {
                    if (p.get(i).equals(p.get(j))) {
                        logger.debug(i + ": " + p.get(i));
                        logger.debug(j + ": " + p.get(j));
                        logger.debug("Invalid: path " + i + " " + j + " are equal.");
                        fail();
                    }
                }
            }
        }

        // Only (k/2) * (k/2) of path length 4
        assertEquals((kFatTree / 2) * (kFatTree / 2), count);

    }
    
}
