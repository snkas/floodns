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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import static ch.ethz.systems.floodns.ext.basicsim.topology.TopologyTestUtility.constructTopology;
import static ch.ethz.systems.floodns.ext.basicsim.topology.TopologyTestUtility.createSet;
import static org.junit.Assert.*;

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
                "set(0-1,1-2,2-3,3-4,1-5,1-6)",
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

    @Test
    public void testInvalidServerToSwitch() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                4,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(0-5, 1-3, 2-4, 3-4)",
                ""
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testNormalLinkDataRateMap() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                4,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(0-2, 1-3, 2-4, 3-4)",
                "map(0-2: 10, 1-3: 5, 2-4: 10, 3-4: 8)"
        );

        // Check topology is created properly
        Topology topology = FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        assertEquals(topology.getNetwork().getPresentLinksBetween(0, 2).get(0).getCapacity(), 10.0, 0.000001);
        assertEquals(topology.getNetwork().getPresentLinksBetween(1, 3).get(0).getCapacity(), 5, 0.000001);
        assertEquals(topology.getNetwork().getPresentLinksBetween(2, 4).get(0).getCapacity(), 10.0, 0.000001);
        assertEquals(topology.getNetwork().getPresentLinksBetween(3, 4).get(0).getCapacity(), 8.0, 0.000001);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testEmptyLinkDataRateMap() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                0,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set()",
                "map()"
        );

        // Check topology is created properly
        FileToTopologyConverter.convert(tempTopology.getAbsolutePath());

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testColonMissingMap() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map(2-3 10.0)"
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testDashMissingMap() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map(2 3:10.0)"
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testInvalidEdgeMap() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map(2-3:9,2-4:10.0)"
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testDuplicateEdgeMap() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map(2-3:6.0,2-3:6.0)"
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testMissingEdgeMap() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                2,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3,3-4)",
                "map(2-3:6.0)"
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testBadCapacity() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map(2-3:xyz)"
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testNegativeCapacity() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map(2-3:-5)"
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

    @Test
    public void testNonExistentFile() throws IOException {

        // Create a file which is deleted afterwards, such that it does not exist
        File tempFile = File.createTempFile("topology", ".tmp");
        assertTrue(tempFile.delete());

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempFile.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);


    }

    @Test
    public void testDistortedMapCapacity() throws IOException {

        File tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map(2-3:10"
        );

        boolean thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

        tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map2-3:10)"
        );

        thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

        tempTopology = TopologyTestUtility.constructTopologyFile(
                5,
                1,
                "set(0, 1)",
                "set(2, 3, 4)",
                "set(2, 3)",
                "set(2-3)",
                "map(2-3:10))"
        );

        thrown = false;
        try {
            FileToTopologyConverter.convert(tempTopology.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Clean-up of file
        assertTrue(tempTopology.delete());

    }

}
