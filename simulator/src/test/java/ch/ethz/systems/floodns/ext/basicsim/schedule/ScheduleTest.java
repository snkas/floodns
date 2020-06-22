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

package ch.ethz.systems.floodns.ext.basicsim.schedule;

import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Event;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.metadata.SimpleStringMetadata;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

import static ch.ethz.systems.floodns.ext.basicsim.topology.TopologyTestUtility.constructTopology;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class ScheduleTest {

    @Test
    public void testScheduleCorrect() throws IOException {

        String run_dir = Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString();

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

        // schedule.csv
        PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
        writerConfig.write("0,6,11,43626,255345,,a\n");
        writerConfig.write("1,8,7,333,2546262,,b\n");
        writerConfig.write("2,9,10,1000000,2546262,,c\n");
        writerConfig.write("3,10,9,1000000,2546262,x,a\n");
        writerConfig.write("4,7,6,1000000,7878475836,,\n");
        writerConfig.close();

        Simulator simulator = new Simulator();
        Schedule schedule = new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);
        List<Event> events = schedule.getConnectionStartEvents(simulator, null);
        assertEquals(5, events.size());
        int i = 0;
        for (Event e : events) {
            ConnectionStartEvent cse = (ConnectionStartEvent) e;
            Connection conn = cse.getConnection();
            assertEquals(i, conn.getConnectionId());
            if (i == 0) {
                assertEquals(6, conn.getSrcNodeId());
                assertEquals(11, conn.getDstNodeId());
                assertEquals(43626 * 8, conn.getRemainder(), simulator.getFlowPrecision());
                assertEquals(43626 * 8, conn.getTotalSize(), simulator.getFlowPrecision());
                assertEquals(255345, cse.getTime());
                assertEquals("a", ((SimpleStringMetadata) conn.getMetadata()).toCsvValidLabel());
            } else if (i == 1) {
                assertEquals(8, conn.getSrcNodeId());
                assertEquals(7, conn.getDstNodeId());
                assertEquals(333 * 8, conn.getRemainder(), simulator.getFlowPrecision());
                assertEquals(333 * 8, conn.getTotalSize(), simulator.getFlowPrecision());
                assertEquals(2546262, cse.getTime());
                assertEquals("b", ((SimpleStringMetadata) conn.getMetadata()).toCsvValidLabel());
            } else if (i == 2) {
                assertEquals(9, conn.getSrcNodeId());
                assertEquals(10, conn.getDstNodeId());
                assertEquals(1000000 * 8, conn.getRemainder(), simulator.getFlowPrecision());
                assertEquals(1000000 * 8, conn.getTotalSize(), simulator.getFlowPrecision());
                assertEquals(2546262, cse.getTime());
                assertEquals("c", ((SimpleStringMetadata) conn.getMetadata()).toCsvValidLabel());
            } else if (i == 3) {
                assertEquals(10, conn.getSrcNodeId());
                assertEquals(9, conn.getDstNodeId());
                assertEquals(1000000 * 8, conn.getRemainder(), simulator.getFlowPrecision());
                assertEquals(1000000 * 8, conn.getTotalSize(), simulator.getFlowPrecision());
                assertEquals(2546262, cse.getTime());
                assertEquals("a", ((SimpleStringMetadata) conn.getMetadata()).toCsvValidLabel());
            } else if (i == 4) {
                assertEquals(7, conn.getSrcNodeId());
                assertEquals(6, conn.getDstNodeId());
                assertEquals(1000000 * 8, conn.getRemainder(), simulator.getFlowPrecision());
                assertEquals(1000000 * 8, conn.getTotalSize(), simulator.getFlowPrecision());
                assertEquals(7878475836L, cse.getTime());
                assertEquals("", ((SimpleStringMetadata) conn.getMetadata()).toCsvValidLabel());
            } else {
                fail();
            }
            i++;
        }

        int j = 0;
        for (ScheduleEntry entry : schedule.getEntries()) {
            if (j == 3) {
                assertEquals("x", entry.getAdditionalParameters());
            } else {
                assertEquals("", entry.getAdditionalParameters());
            }
            j++;
        }
        assertEquals(5, j);

    }

    @Test
    public void testScheduleInvalidChecks() throws IOException {

        String run_dir = Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString();

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
        boolean thrown;

        // This is valid
        PrintWriter writerConfigValid = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
        writerConfigValid.write("0,6,11,43626,255345,,a\n");
        writerConfigValid.write("1,6,11,43626,255345,,a\n");
        writerConfigValid.close();
        new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);

        // Invalid source
        thrown = false;
        try {
            PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
            writerConfig.write("0,3,11,43626,255345,,a\n");
            writerConfig.close();
            new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Invalid destination
        thrown = false;
        try {
            PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
            writerConfig.write("0,11,66,43626,255345,,a\n");
            writerConfig.close();
            new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Not ascending flow ID
        thrown = false;
        try {
            PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
            writerConfig.write("0,6,11,43626,255345,,a\n");
            writerConfig.write("0,6,11,43626,255345,,a\n");
            writerConfig.close();
            new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // To itself
        thrown = false;
        try {
            PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
            writerConfig.write("0,11,11,43626,255345,,a\n");
            writerConfig.close();
            new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Not ascending time
        thrown = false;
        try {
            PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
            writerConfig.write("0,6,11,43626,255345,,a\n");
            writerConfig.write("1,6,11,43626,255344,,a\n");
            writerConfig.close();
            new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Beyond duration
        thrown = false;
        try {
            PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
            writerConfig.write("0,6,11,43626,255345,,a\n");
            writerConfig.write("1,6,11,43626,7878475837,,a\n");
            writerConfig.close();
            new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Split failed
        thrown = false;
        try {
            PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
            writerConfig.write("0,6,11,43626,255345,a\n");
            writerConfig.close();
            new Schedule(run_dir + "/schedule.csv", topology, 7878475837L);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

}
