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

General properties of the simulation. The following must be defined:

* `simulation_end_time_ns` : How long to run the simulation in simulation time (ns)
* `simulation_seed` : If there is randomness present in the simulation, this guarantees reproducibility (exactly the same outcome) if the seed is the same
* `filename_topology` : Topology filename (relative to run folder)
* `filename_schedule` : Schedule filename (relative to run folder)

**schedule.csv**

Simple connection arrival schedule. Each line defines a connection (= typically, a routing strategy supplies 1 flow / connection, but it can be any number of its lifetime) as follows:

```
[connection_id],[from_node_id],[to_node_id],[size_byte],[start_time_ns],[additional_parameters],[metadata]
```

Notes: connection_id must increment each line. All values except additional_parameters and metadata are mandatory. `additional_parameters` should be set if you want to configure something special for each connection. `metadata` you can use for identification later on in the connection logs (e.g., to indicate the workload it was part of).

**topology.properties**

The topological layout of the network. The following properties must be defined:

* `num_nodes` : Number of nodes
* `num_undirected_edges` : Number of undirected edges (= links)
* `switches` : All node identifiers which are switches expressed as `set(a, b, c)`, e.g.: `set(5, 6)` means node 5 and 7 are switches
* `switches_which_are_tors` : All node identifiers which are also ToRs expressed as `set(a, b, c)`
* `servers` : All node identifiers which are servers expressed as `set(a, b, c)`
* `undirected_edges` : All undirected edges, expressed as `set(a-b, b-c)`, e.g.: `set(0-2, 2-3)` means two links, between 0 and 2, and between 2 and 3
* `link_data_rate_bit_per_ns` : Data rate set for all links (bit/ns = Gbit/s) (expressed as a number `double`, e.g.: `4.5` means 4.5 Gbit/s if you want the same rate for all links, else you express it as `map(a-b: c, d-e: f)`, e.g.: `map(0-1: 10.0, 6-7: 8.4)` link 0-1 has 10.0 Gbit/s and link 6-7 has 8.4 Gbit/s capacity. If you choose to express as a map, you have to give it individually for every edge.)

Please see the examples to understand each property. Besides it just defining a graph, the following rules apply:

* If there are servers defined, they can only have edges to a ToR.
* There is only a semantic difference between switches, switches which are ToRs and servers. If there are servers, only servers should be valid endpoints for applications. If there are no servers, ToRs should be valid endpoints instead.
