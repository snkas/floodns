# Tutorial: basic simulation

## A first run

1. In the README tutorial you generated the executable jar `floodns-basic-sim.jar` in the `simulator` folder. This is now used as our primary executable.

2. The executable jar takes one argument, the run directory:

   ```
   java -jar floodns-basic-sim.jar [run directory]
   ```
   
   As an example, execute:
   
   ```
   cd simulator
   java -jar floodns-basic-sim.jar ../runs/example_single
   ```
   
3. Now you can find log files as output in `../runs/example_single/logs_floodns`, e.g. you can see:

   ```
   xdg-open ../runs/example_single/logs_floodns/connection_info.csv.log
   ```

## Run folder

The run folder must contain the input of a simulation.

**config.properties**

General properties of the simulation. The following are already defined:

* `filename_topology` : Topology filename (relative to run folder)
* `filename_schedule` : Schedule filename (relative to run folder)
* `simulation_end_time_ns` : How long to run the simulation in simulation time (ns)
* `simulation_seed` : If there is randomness present in the simulation, this guarantees reproducibility (exactly the same outcome) if the seed is the same
* `link_data_rate_bit_per_ns` : Data rate set for all links (bit/ns = Gbit/s)

**schedule.csv**

Simple connection arrival schedule. Each line defines a connection (= typically, a routing strategy supplies 1 flow / connection, but it can be any number of its lifetime) as follows:

```
connection_id,from_node_id,to_node_id,size_byte,start_time_ns,additional_parameters,metadata
```

Notes: connection_id must increment each line. All values except additional_parameters and metadata are mandatory. `additional_parameters` should be set if you want to configure something special for each connection. `metadata` you can use for identification later on in the connection logs (e.g., to indicate the workload it was part of).

**topology.properties**

The topological layout of the network. Please see the examples to understand each property. Besides it just defining a graph, the following rules apply:

* If there are servers defined, they can only have edges to a ToR.
* There is only a semantic difference between switches, switches which are ToRs and servers. If there are servers, only servers should be valid endpoints for applications. If there are no servers, ToRs should be valid endpoints instead.
