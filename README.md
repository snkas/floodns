# floodns: temporal routed flow simulation

[![Build Status](https://travis-ci.org/snkas/floodns.svg?branch=master)](https://travis-ci.org/snkas/floodns) [![codecov](https://codecov.io/gh/snkas/floodns/branch/master/graph/badge.svg)](https://codecov.io/gh/snkas/floodns)

This is a flow-level simulator to simulate routed flows over time. The abstraction is between a flow optimization (e.g., via a linear program) and a packet-level simulation (e.g., ns-3). It does not model packets, but it calculates the rate of flows over time.

As a consequence, simulating large flows is generally quicker because the runtime is determined by the calculation of rate assignment rather than individual packet interactions as is the case for packet-level simulation. On the other side, this means that because packets are not modeled, (a) latency, (b) queueing, and (c) end-point congestion control cannot be directly modeled. Additionally, because flows must be routed along acyclic paths in the current model abstraction, you cannot apply perfect-split traffic engineering.

**THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. (see also the MIT License in ./LICENSE).**

## Overview

### Requirements

* Java 7 or higher (check: `java -version`)
* Maven 3 or higher (check: `mvn --version`)
* Python 3.5 or higher (check: `python --version`)
* Gnuplot 4.4 or higher (check: `gnuplot --version`)
* OR-tools and ortoolslpparser to solve linear programs from file:

```bash
$ pip install ortools
$ pip install git+https://github.com/snkas/python-ortools-lp-parser
```

### Building

1. Go to the simulator folder `cd simulator`
   
2. Build basic simulation executable jar:
   ```
   mvn clean compile assembly:single
   mv target/floodns-*-jar-with-dependencies.jar floodns-basic-sim.jar
   ```
   
3. Install in your system's maven repository:
   ```
   mvn source:jar javadoc:jar install
   ```

### Getting started

There are two possible ways to get started:

(A) **Basic simulation** which limits you to a simple static topology and a simple flow arrival schedule. You only have to generate simple intuitive files to use the simulator. Please follow the tutorial `TUTORIAL_BASIC_SIM.md`

(B) **Programmatically** make use of floodns yourself and extend it to fit your particular use case. Basic simulation is still a good example of good experimental practice and separation of concerns. Please follow the tutorial `TUTORIAL_CODE_YOURSELF.md`

### Testing

Run all tests:

```bash
$ mvn test
```

Coverage report can be found in `target/site/jacoco`

# Framework

### 1. Key concepts

#### 1.1 Core components

The following concepts are part of the core package, meaning that regardless of your specific use-case you will have to abide their limitations:
- **Network(V, E, F):** network consisting of node set *V* and link set *E* connecting these nodes. Within the network is a set of flows *F* present;
- **Node:** point in the network to which links can be connected. It can function as a flow entry, relay or exit;
- **Link(u, v, c):** a non-unique directed edge from node *u* to node *v* with a fixed capacity *c*;
- **Flow(s, t, path):** a stream from start node *s* to target node *t* over a fixed *path* (continuous set of links) with a certain bandwidth;
- **Connection(Q, s, t):** abstraction for an amount *Q* that is desired to be transported from *s* to *t* over a set of zero or more flows {f<sub>1</sub>, f<sub>2</sub>, ...} which can dynamically change over time;
- **Event:** core component which is user-defined (see section 1.3.2);
- **Aftermath:** core component which enforces some state invariant (user-defined; see section 1.3.2), for example max-min fair (MMF) allocation;
- **Simulator:** event-driven single-run engine. It executes events until the maximum runtime or there are no events left.

#### 1.2 Application format

The general format of an application is as follows:

1. Instantiate the two core components: (a) the network N, and (b) the aftermath A;
2. Instantiate the simulator;
3. Insert initial events E into the simulator event priority queue;
4. Run the simulator;
5. Analyze the logs.

#### 1.3 Simulation

##### 1.3.1 Algorithm pseudo-code

The pseudo-code of the simulator is as follows:

```
function SIMULATE(N, E, A) {
    Q = priority_queue()
    Q.insert_all(E)
    
    t_prev = 0
    while (Q.hasNext() and Q.peek().time() <= runtime) {
        event = Q.pop()
        
        // Update connections' progression
        t_now = event.time
        elapsed = t_now - t_prev
        if (elapsed > 0) {
            update_connections_progression(elapsed)
        }
        t_prev = t_now
        
        // Execute all events in the same time tick
        event.execute()
        while (Q.hasNextInSameTick()) {
            event = Q.pop()
            event.execute()
        }
        
        // Perform aftermath
        A.perform()
        
        // Put an empty event when the next connection finishes
        if (now != runtime) {
            Q.put(empty_event(
                    min{runtime, calculate_next_connection_finish_time()}
            ))
        }
    
    }
}
```

##### 1.3.2 User-defined actions

As you can see, there are two user-defined actions in the simulation: `event.execute()`, and `aftermath.perform()`. Their supposed behavior is defined as follows:

* **Events** (`event.execute()`): in each time tick, all events for that time tick are consecutively executed. An event can do any modification to the network, including (a) adding/removing links, (b) changing flows' bandwidth, (c) adding/removing flows of connections, and (d) inserting/canceling events. 

* **Aftermath** (`aftermath.perform()`): the aftermath is called after all events in the time tick are executed. The responsibility of the aftermath is typically to ensure some network-wide consistency or invariant, which the events do not take care of. It can for example be seen as the central network controller, assigning flow rules based on global congestion knowledge. The aftermath can do any modification to the network (equal to the capabilities of an event): (a) adding/removing links, (b) changing flows' bandwidth, (c) adding/removing flows of connections, and (d) inserting/canceling events. 

### 2. Extensions

The extension package (*ch.ethz.systems.floodns.ext*) contains useful helper classes to get started quickly. The following helper extensions are present:
- **Routing Strategy (ext.routing):** the routing strategy is used to generate the set of flows given a connection. E.g. ECMP, VLB, KSP and KSP-MPTCP are currently implemented.
- **Basic simulation (ext.basicsim):** convenient ability to just run given a configuration, topology and flow schedule.
- **Allocator (ext.allocator):** Weighted and upper-limited max-min fair allocation (MMF) is implemented.
- **Log (ext.logger.file):** CSV file (RFC 4180 compliant) logging and null logging. CSV logging is described at the end of this README.

### 3. CSV logs

A CSV log consists of new line (\r\n) separated entries. Each entry is comma-separated. The following logs are generated by the ext.log.file logger:
* **flow_bandwidth.csv.log:** for each flow all the intervals of bandwidth. Entry: `flow_id,interval_start_time,interval_end_time,bandwidth`
* **flow_info.csv.log**: aggregate information for each flow. Entry: `flow_id,source_node_id,dest_node_id,PATH,start_time,end_time,duration,amount_sent,average_bandwidth`
* **link_info.csv.log**: aggregate information for each link. Entry: `link_id,from_node_id,to_node_id,start_time,end_time,duration,average_utilization,average_number_active_flows`
* **link_num_active_flows.csv.log**: for each link all the intervals of number of active flows. Entry: `link_id,from_node_id,to_node_id,interval_start_time,interval_end_time,number_active_flows`
* **link_utilization.csv.log**: for each link all the intervals of utilization. Entry: `link_id,from_node_id,to_node_id,interval_start_time,interval_end_time,utilization`
* **node_info.csv.log:** aggregate node information for each node. Entry: `node_id,average_number_active_flows`
* **node_num_active_flows.csv.log:** for each node all the intervals of number of active flows. Entry: `node_id,interval_start_time,interval_end_time,number_active_flows`
* **connection_bandwidth.csv.log:** for each connection all the intervals of bandwidth. Entry: `connection_id,interval_start_time,interval_end_time,bandwidth`
* **connection_info.csv.log:** aggregate information for each connection. Entry: `connection_id,source_node_id,dest_node_id,total_size,amount_sent,FLOW_LIST,start_time,end_time,duration,average_bandwidth,COMPLETED`

In which the special formats are:

* PATH: `node1_id-[link1_id]->node2_id-[link2_id]->...-[last_link_id]->last_node_id`
* FLOW_LIST: semi-colon separated list of flow identifiers, e.g. `1;5;6`
* COMPLETED: `T` (for true) iff connection finished, else `F` (for false)

# Frequently Asked Questions (FAQ)

**In what order are events in the same time tick executed?**

By the order at which the events were inserted into the simulation's event priority queue. For example, you instantiate event A for t=402, and then event B for t=402. You insert first event B and then event A into the event priority queue. Upon time tick t=402, first event B will be executed and then event A.

**If all the events in a time tick are cancelled, will the aftermath be called?**

No. The aftermath is only called when *one or more events in a time tick are active*.

**Can you disable events before inserting them into the simulation?**

No. You must simply not insert them into the simulation, which has the same effect.

**With what flow precision are connections finished?**

Although the general precision is 1e-10, the connection finish precision is based on the remaining size and the connection bandwidth. For example, you have a a single connection over a single link which has 1000 flow units remaining and a total bandwidth of 9 flow units per time unit (sum of all flows belonging to the connection). The flow will then be scheduled to finish in `ceil(1000/9)=112` time units. This means that it will transport `112*9=1008` flow units, 8 more than was desired. Assuming that the time unit is small (e.g. nanoseconds) correspondingly the possible bandwidth as well (e.g. bit/ns), this imprecision will be quite small.
