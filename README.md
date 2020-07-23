# floodns: temporal routed flow simulation

[![Build Status](https://travis-ci.org/snkas/floodns.svg?branch=master)](https://travis-ci.org/snkas/floodns) [![codecov](https://codecov.io/gh/snkas/floodns/branch/master/graph/badge.svg)](https://codecov.io/gh/snkas/floodns)

This is a flow-level simulator to simulate routed flows over time. The abstraction is between a flow optimization (e.g., via a linear program) and a packet-level simulation (e.g., ns-3). It does not model packets, but it calculates the rate of flows over time.

As a consequence, simulating large flows is generally quicker because the runtime is determined by the calculation of rate assignment rather than individual packet interactions as is the case for packet-level simulation. On the other side, this means that because packets are not modeled, (a) latency, (b) queueing, and (c) end-point congestion control cannot be directly modeled. Additionally, because flows must be routed along acyclic paths in the current model abstraction, you cannot apply perfect-split traffic engineering.

**THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. (see also the MIT License in ./LICENSE).**

## Overview

### Requirements

* Java 7 or higher (check: `java -version`)
* Maven 3 or higher (check: `mvn --version`)
* Python 3.7 or higher (check: `python --version`)
* Gnuplot 4.4 or higher (check: `gnuplot --version`)
* OR-tools and ortoolslpparser to solve linear programs from file:

  ```bash
  pip install ortools
  pip install git+https://github.com/snkas/python-ortools-lp-parser
  ```

* To use the analysis functionality you need the numpy and exputil Python package:

  ```bash
  pip install numpy
  pip install git+https://github.com/snkas/exputilpy
  ```

### Building

1. Go to the simulator folder: `cd simulator`
   
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

(A) **Basic simulation** which limits you to a simple static topology and a simple flow arrival schedule. You only have to generate simple intuitive files to use the simulator. Please follow the tutorial `doc/tutorial_basic_sim.md`

(B) **Programmatically** make use of floodns yourself and extend it to fit your particular use case. Basic simulation is still a good example of good experimental practice and separation of concerns. Please follow the tutorial `doc/tutorial_code_yourself.md`

Beyond that, there is further documentation found in `doc/`, including:

* An overview of the entire framework: `doc/framework.md`

### Testing

Run all tests:

```bash
cd simulator
mvn test
```

Coverage report can be found in: `target/site/jacoco`
