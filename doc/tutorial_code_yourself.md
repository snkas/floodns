# Tutorial: programmatically do everything yourself

1. In the README tutorial you installed floodns globally on your machine.

2. Create your own maven project (look up online how to do this)

3. Include it into your own maven project by adding the following dependency to your own `pom.xml`:

   ```xml
   <dependency>
       <groupId>ch.ethz.systems</groupId>
       <artifactId>floodns</artifactId>
       <version>3.6.1</version>
   </dependency>
   ```

4.  From `simulator` copy the `test_data` and `external` folder to the root of your own project folder.

5. Create your own DemoMain class (named `DemoMain.java`) in your own project, and copy the following code into it:

    ```java
    import ch.ethz.systems.floodns.core.Aftermath;
    import ch.ethz.systems.floodns.core.Network;
    import ch.ethz.systems.floodns.core.Simulator;
    import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
    import ch.ethz.systems.floodns.ext.basicsim.schedule.TrafficSchedule;
    import ch.ethz.systems.floodns.ext.basicsim.topology.FileToTopologyConverter;
    import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;
    import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
    import ch.ethz.systems.floodns.ext.routing.EcmpRoutingStrategy;
    
    import java.util.Random;
    
    public class DemoMain {
    
        public static void main(String[] args) {
    
            // Random number generators
            Random routingRandom = new Random(1234567);
    
            // Fat-tree topology
            Topology topology = FileToTopologyConverter.convert(
                    "test_data/1_to_1_capacity_10.properties" // 10 flow units / time unit ("bit/ns")
            );
            Network network = topology.getNetwork();
    
            // Create simulator
            Simulator simulator = new Simulator();
            FileLoggerFactory loggerFactory = new FileLoggerFactory(simulator, "demo_logs");
            Aftermath aftermath = new SimpleMmfAllocator(simulator, network);
            simulator.setup(network, aftermath, loggerFactory);
    
            // Routing
            EcmpRoutingStrategy routingStrategy = new EcmpRoutingStrategy(simulator, topology, routingRandom);
    
            // Traffic
            TrafficSchedule trafficSchedule = new TrafficSchedule(simulator, network, routingStrategy);
            trafficSchedule.addConnectionStartEvent(0, 1, 100000, 0); // "0 -> 1 send 100000 bit starting at t=0"
            simulator.insertEvents(trafficSchedule.getConnectionStartEvents());
    
            // Run the simulator
            simulator.run((long) 10e9); // 10e9 time units ("ns")
            loggerFactory.runCommandOnLogFolder("python external/analyze.py");
    
            // Simulation log files are now viewable in: demo_logs
            // Simulation statistical results are now viewable in: demo_logs/analysis
    
        }
    
    }
    ```

6. Run the DemoMain class in your own project. It will output some progress messages and write simulation log files to the `demo_logs` directory in your own maven project folder. It additionally executes an analysis command on the run folder, which outputs the results of a simple statistical analysis in the `demo_logs/analysis` directory.
