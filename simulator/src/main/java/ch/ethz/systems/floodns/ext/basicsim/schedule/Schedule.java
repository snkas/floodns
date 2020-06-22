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

import ch.ethz.systems.floodns.core.Event;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Schedule {

    private final List<ScheduleEntry> entries;
    private final Topology topology;

    public Schedule(String fileName, Topology topology, long simulationDurationNs) {
        this.entries = new ArrayList<>();
        this.topology = topology;

        try {

            // Open file stream
            FileReader input = new FileReader(fileName);
            BufferedReader br = new BufferedReader(input);

            // Go over lines one-by-one
            String line;
            int lineCounter = 0;
            long prevStartTimeNs = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                String[] spl = line.split(",",-1);

                // All 7 must be there
                if (spl.length != 7) {
                    throw new IllegalArgumentException("File contains line which is not 7 columns: " + line);
                }

                // Add start event (this will throw all IllegalArgumentException if any of the inputs is invalid)
                ScheduleEntry entry = new ScheduleEntry(
                        Long.parseLong(spl[0]),
                        Integer.parseInt(spl[1]),
                        Integer.parseInt(spl[2]),
                        Long.parseLong(spl[3]),
                        Long.parseLong(spl[4]),
                        spl[5],
                        spl[6]
                );

                if (entry.getConnectionId() != lineCounter) {
                    throw new IllegalArgumentException("Connection ID is not ascending by one each line (violation: " + entry.getConnectionId() + ")");
                }

                // Must be weakly ascending start time
                if (prevStartTimeNs > entry.getStartTimeNs()) {
                    throw new IllegalArgumentException(
                            "Start time is not weakly ascending (on line with connection ID: %" + entry.getConnectionId() + ", violation: " + entry.getStartTimeNs() + ")"
                    );
                }
                prevStartTimeNs = entry.getStartTimeNs();

                // Check node IDs
                if (!topology.getDetails().isValidEndpoint(entry.getFromNodeId())) {
                    throw new IllegalArgumentException("Invalid from node ID: " + entry.getFromNodeId());
                }
                if (!topology.getDetails().isValidEndpoint(entry.getToNodeId())) {
                    throw new IllegalArgumentException("Invalid to node ID: " + entry.getToNodeId());
                }
                if (entry.getFromNodeId() == entry.getToNodeId()) {
                    throw new IllegalArgumentException("Connection to itself at node ID: " + entry.getFromNodeId());
                }

                // Check start time
                if (entry.getStartTimeNs() >= simulationDurationNs) {
                    throw new IllegalArgumentException("Connection " + entry.getConnectionId() + " has invalid start time " + entry.getStartTimeNs() + " >= " + simulationDurationNs);
                }

                // Put into schedule
                entries.add(entry);

                lineCounter++;

            }

            // Close file stream
            br.close();

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read schedule", e);
        }

    }

    public List<ScheduleEntry> getEntries() {
        return entries;
    }

    public List<Event> getConnectionStartEvents(Simulator simulator, RoutingStrategy routingStrategy) {
        TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, topology.getNetwork(), routingStrategy);
        for (ScheduleEntry entry : entries) {
            trafficSchedule.addConnectionStartEvent(
                    entry.getFromNodeId(),
                    entry.getToNodeId(),
                    entry.getSizeByte() * 8,
                    entry.getStartTimeNs(),
                    entry.getMetadata()
            );
        }
        return trafficSchedule.getConnectionStartEvents();
    }

}
