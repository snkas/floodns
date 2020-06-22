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

package ch.ethz.systems.floodns.ext.routing;

import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.basicsim.topology.Topology;

/**
 * The single path routing strategy ensures that there is a function
 * in the strategy that assigns a single path for each connection.
 */
public abstract class SinglePathRoutingStrategy extends TopologyRoutingStrategy {

    public SinglePathRoutingStrategy(Simulator simulator, Topology topology) {
        super(simulator, topology);
    }

    @Override
    public final void assignStartFlowsInTopology(Connection connection) {

        // Generate and add single flow (e.g., as ECMP is uni-flow decision)
        simulator.addFlowToConnection(connection, assignSinglePath(connection));

    }

    /**
     * Assignment of a single path for a connection
     *
     * @param connection   Connection
     *
     * @return Acyclic single path
     */
    protected abstract AcyclicPath assignSinglePath(Connection connection);

}
