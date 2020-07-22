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

import ch.ethz.systems.floodns.core.Aftermath;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.basicsim.schedule.Schedule;
import ch.ethz.systems.floodns.ext.basicsim.topology.FileToTopologyConverter;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.routing.EcmpRoutingStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class BasicSimulation {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java -jar floodns-basic-sim.jar [run directory]");
        }
        BasicSimulation.simulate(args[0]);
    }

    private static void simulate(String runDirectory) {

        try {

            // Check run directory exists
            File f = new File(runDirectory);
            if (!f.exists() || !f.isDirectory()) {
                throw new IllegalArgumentException("Run directory does not exist: " + runDirectory);
            }

            // Config
            FileInputStream fileInputStream = new FileInputStream(runDirectory + "/config_floodns.properties");
            NoDuplicatesProperties config = new NoDuplicatesProperties();
            config.load(fileInputStream);
            fileInputStream.close();
            config.validate(new String[]{
                    "filename_topology", "filename_schedule",
                    "simulation_end_time_ns", "simulation_seed"
            });

            // Base simulation properties
            long simulationEndTimeNs = config.getPositiveLongOrFail("simulation_end_time_ns");
            long simulationSeed = config.getPositiveLongOrFail("simulation_seed");
            Random simulationRandom = new Random(simulationSeed);

            // Topology
            Topology topology = FileToTopologyConverter.convert(
                    runDirectory + "/" + config.getStringOrFail("filename_topology")
            );
            Network network = topology.getNetwork();

            // Initialize simulator
            Simulator simulator = new Simulator();
            FileLoggerFactory loggerFactory = new FileLoggerFactory(simulator, runDirectory + "/logs_floodns");
            Aftermath aftermath = new SimpleMmfAllocator(simulator, network);
            simulator.setup(network, aftermath, loggerFactory);

            // Schedule
            EcmpRoutingStrategy routingStrategy = new EcmpRoutingStrategy(simulator, topology, new Random(simulationRandom.nextLong()));
            Schedule schedule = new Schedule(runDirectory + "/" + config.getStringOrFail("filename_schedule"), topology, simulationEndTimeNs);
            simulator.insertEvents(schedule.getConnectionStartEvents(simulator, routingStrategy));

            // Run
            simulator.run(simulationEndTimeNs);

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

    }

}
