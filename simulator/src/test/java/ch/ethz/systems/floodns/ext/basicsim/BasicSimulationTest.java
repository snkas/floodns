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

package ch.ethz.systems.floodns.ext.basicsim;

import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class BasicSimulationTest {

    @Test
    public void testSimpleRingEcmp() throws IOException {
        String run_dir = Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString();

        // config_floodns.properties
        PrintWriter writerConfig = new PrintWriter(new FileWriter(run_dir + "/config_floodns.properties"));
        writerConfig.write("filename_topology=\"topology.properties\"\n");
        writerConfig.write("filename_schedule=\"schedule.csv\"\n");
        writerConfig.write("simulation_end_time_ns=5000000000\n");
        writerConfig.write("simulation_seed=123456789\n");
        writerConfig.write("link_data_rate_bit_per_ns=0.1\n");
        writerConfig.close();

        // topology.properties
        PrintWriter writerTopology = new PrintWriter(new FileWriter(run_dir + "/topology.properties"));
        writerTopology.write("num_nodes=4\n");
        writerTopology.write("num_undirected_edges=4\n");
        writerTopology.write("switches=set(0,1,2,3)\n");
        writerTopology.write("switches_which_are_tors=set(0,1,2,3)\n");
        writerTopology.write("servers=set()\n");
        writerTopology.write("undirected_edges=set(0-1,0-2,1-3,2-3)\n");
        writerTopology.close();

        // schedule.csv
        PrintWriter writerSchedule = new PrintWriter(new FileWriter(run_dir + "/schedule.csv"));
        int num_flows = 70;
        for (int i = 0; i < num_flows; i++) {
            writerSchedule.write(i + ",0,3,1000000,1000000000,,a" + i + "\n");
        }
        writerSchedule.close();

        // Now just perform the run
        BasicSimulation.main(new String[]{run_dir});

        // Get the logs (yes, we create a file logger factory just to get the path completion here)
        Map<Integer, List<String>> flowIdToInfoLog = TestLogReader.getIdToInfoMapping(run_dir + "/logs_floodns/" + FileLoggerFactory.FILE_NAME_FLOW_INFO);

        assertEquals(num_flows, flowIdToInfoLog.size());

        // Count how many when over each route
        int num_top = 0;
        int num_bottom = 0;
        for (int i = 0; i < num_flows; i++) {
            List<String> outcome = flowIdToInfoLog.get(i);
            if (outcome.get(2).equals("0-[2]->2-[6]->3")) {
                num_top++;
            } else if (outcome.get(2).equals("0-[0]->1-[4]->3")) {
                num_bottom++;
            } else {
                fail();
            }
        }

        // Check values
        double totalRate = 0.0;
        for (int i = 0; i < num_flows; i++) {
            List<String> outcome = flowIdToInfoLog.get(i);
            assertEquals(8, outcome.size());
            assertEquals("0", outcome.get(0)); // From
            assertEquals("3", outcome.get(1)); // To
            boolean is_top = outcome.get(2).equals("0-[2]->2-[6]->3"); // Route
            assertEquals(String.valueOf(1000000000), outcome.get(3)); // Start time
            double expectedRate = is_top ? 0.1 / num_top : 0.1 / num_bottom;
            assertEquals(1000000000 + Math.ceil(8.0 * 1000000.0 / expectedRate), Double.parseDouble(outcome.get(4)), 1.0); // End time
            assertEquals(Math.ceil(8.0 * 1000000.0 / expectedRate), Double.parseDouble(outcome.get(5)), 1.0); // Duration
            assertEquals(8.0 * 1000000.0, Double.parseDouble(outcome.get(6)), expectedRate); // Amount sent
            assertEquals(expectedRate, Double.parseDouble(outcome.get(7)), 0.000001); // Rate
            totalRate += expectedRate;
        }

        // With 70 flows, at least one has to go over the other path
        assertEquals(0.2, totalRate, 0.00001);

        // Similarly, let's say at least 20 up and 20 bottom
        assertTrue(num_top >= 20);
        assertTrue(num_bottom >= 20);
        assertEquals(num_flows, num_top + num_bottom);

    }

    @Test
    public void testRunDirDoesNotExist() {
        boolean thrown = false;
        try {
            BasicSimulation.main(new String[]{"/path/does/not/exist"});
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testRunDirInsufficientArguments() {
        boolean thrown = false;
        try {
            BasicSimulation.main(new String[]{});
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testRunDirTooManyArguments() {
        boolean thrown = false;
        try {
            BasicSimulation.main(new String[]{"a", "b"});
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testConfigDoesNotExist() throws IOException {
        boolean thrown = false;
        try {
            String run_dir = Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString();
            BasicSimulation.main(new String[]{run_dir});
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

}
