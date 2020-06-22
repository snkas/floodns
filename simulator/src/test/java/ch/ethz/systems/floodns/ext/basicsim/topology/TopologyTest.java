package ch.ethz.systems.floodns.ext.basicsim.topology;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class TopologyTest {

    @Test
    public void testInvalidTopologies() throws IOException {

        // 0 -- 1 -- 2 -- 3 -- 4 (0 and 4 ToRs, 1, 2 and 3 are switches only)
        TopologyDetails a = TopologyTestUtility.constructTopology(
                5,
                4,
                "set()",
                "set(0, 1, 2, 3, 4)",
                "set(0, 4)",
                "set(0-1,1-2,2-3,3-4)",
                10
        ).getDetails();
        assertEquals(a.getEndpoints(), a.getSwitchesWhichAreTorsNodeIds());

        // 0 -- 1 -- 2 -- 3 -- 4 (0 and 4 are servers, 1/3 ToRs, 2 only switch)
        a = TopologyTestUtility.constructTopology(
                5,
                4,
                "set(0, 4)",
                "set(1, 2, 3)",
                "set(1, 3)",
                "set(0-1,1-2,2-3,3-4)",
                10
        ).getDetails();
        assertEquals(a.getEndpoints(), a.getServerNodeIds());

        boolean thrown;

        // Not all are covered
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set(0)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Switches don't cover ToRs
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set(0, 4)",
                    "set(1, 2)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Switches and servers overlap
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set(0, 4, 2)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Number of edges don't match
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4,1-3)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Link between servers
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    5,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4,0-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Server to two ToRs
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    5,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4,0-3)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Server to switch
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    5,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4,0-2)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Server to switch
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    5,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4,4-2)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testInvalidEntryFormats() throws IOException {

        // 0 -- 1 -- 2 -- 3 -- 4
        TopologyTestUtility.constructTopology(
                5,
                4,
                "set(0, 4)",
                "set(1, 2, 3)",
                "set(1, 3)",
                "set(0-1,1-2,2-3,3-4)",
                10
        );

        boolean thrown;

        // Negative value
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    -4,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Duplicate string
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set(0, 4)",
                    "set(1, 1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Duplicate integer
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set(0, 4)",
                    "set(1, 01, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Invalid edge split
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3--4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Duplicate edge
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    5,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4,4-3)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Duplicate string
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    5,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3- 4,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // set(...) not typed correctly
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set (0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3)",
                    "set(0-1,1-2,2-3,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // set(...) not typed correctly
        thrown = false;
        try {
            TopologyTestUtility.constructTopology(
                    5,
                    4,
                    "set(0, 4)",
                    "set(1, 2, 3)",
                    "set(1, 3",
                    "set(0-1,1-2,2-3,3-4)",
                    10
            );
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

}
