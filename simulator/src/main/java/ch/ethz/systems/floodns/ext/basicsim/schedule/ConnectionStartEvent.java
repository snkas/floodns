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
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;

public class ConnectionStartEvent extends Event {

    private final Connection connection;
    private final RoutingStrategy routingStrategy;

    /**
     * Create event which will happen the given amount of time later.
     *
     * @param simulator         Simulator instance
     * @param timeFromNow       Time it will take before happening from now
     * @param connection        User connection instance
     * @param routingStrategy   Routing strategy
     */
    public ConnectionStartEvent(Simulator simulator, long timeFromNow, Connection connection, RoutingStrategy routingStrategy) {
        super(simulator, 0, timeFromNow);
        this.connection = connection;
        this.routingStrategy = routingStrategy;
    }

    @Override
    protected void trigger() {
        simulator.activateConnection(connection);
        routingStrategy.assignStartFlows(connection);
    }

    @Override
    public String toString() {
        return connection + " starting at t=" + this.getTime();
    }

    public Connection getConnection() {
        return connection;
    }

}
