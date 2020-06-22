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

package ch.ethz.systems.floodns.ext.basicsim.topology;

import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;

import static ch.ethz.systems.floodns.ext.basicsim.topology.TopologyTestUtility.constructTopology;
import static ch.ethz.systems.floodns.ext.basicsim.topology.TopologyTestUtility.createSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FileToTopologyConverterTest {

    @Test
    public void testSimpleTopology() throws IOException {

        // 0 - 1 - 2 - 3 - 4
        //     |\
        //     5 6
           Topology result = constructTopology(
                7,
                6,
                "set(4, 5, 6)",
                "set(0, 1, 2, 3)",
                "set(1, 3)",
                "set(0-1,1-2,2-3,3-4,5-1,6-1)",
                10.0
        );

        // Test field values
        TopologyDetails details = result.getDetails();
        assertEquals(details.getNumNodes(), 7);
        assertEquals(details.getNumUndirectedEdges(), 6);
        assertEquals(createSet(4, 5, 6), details.getServerNodeIds());
        assertEquals(createSet(0, 1, 2, 3), details.getSwitchNodeIds());
        assertEquals(createSet(1, 3), details.getSwitchesWhichAreTorsNodeIds());
        assertEquals(3, details.getNumServers());
        assertEquals(4, details.getNumSwitches());
        assertEquals(2, details.getNumSwitchesWhichAreTors());
        assertFalse(details.areTorsEndpoints());
        HashSet<Integer> exp = new HashSet<>();
        exp.add(5);
        exp.add(6);
        assertEquals(details.getServersOfTor(1), exp);
        assertEquals(details.getTorIdOfServer(5), 1);
        assertEquals(details.getTorIdOfServer(6), 1);
        HashSet<Integer> exp2 = new HashSet<>();
        exp2.add(4);
        assertEquals(details.getServersOfTor(3), exp2);
        assertEquals(details.getTorIdOfServer(4), 3);

    }

}
