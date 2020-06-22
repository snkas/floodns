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

import ch.ethz.systems.floodns.core.*;
import ch.ethz.systems.floodns.ext.metadata.SimpleStringMetadata;
import ch.ethz.systems.floodns.ext.routing.RoutingStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The <b>traffic schedule</b> is responsible for collecting
 * and managing the user-planned connection start events.
 */
public class TrafficSchedule {

    protected final Simulator simulator;
    private final Network network;
    private final List<Event> connectionStartEventsList;
    private final List<Connection> connections;
    private final RoutingStrategy routingStrategy;

    public TrafficSchedule(Simulator simulator, Network network, RoutingStrategy routingStrategy) {
        this.simulator = simulator;
        this.network = network;
        this.connectionStartEventsList = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.routingStrategy = routingStrategy;
    }

    public void addConnectionStartEvent(int srcNodeId, int dstNodeId, double connSize, long time) {
        addConnectionStartEvent(srcNodeId, dstNodeId, connSize, time, (Metadata) null);
    }

    public void addConnectionStartEvent(int srcNodeId, int dstNodeId, double connSize, long time, String metadataString) {
        addConnectionStartEvent(srcNodeId, dstNodeId, connSize, time, new SimpleStringMetadata(metadataString));
    }

    /**
     * Create a new connection start event.
     *
     * @param srcNodeId     Source node identifier
     * @param dstNodeId     Destination node identifier
     * @param connSize      Connection size
     * @param time          Time at which to start the connection
     * @param metadata      Metadata to the connection (null if not to be set)
     */
    public void addConnectionStartEvent(int srcNodeId, int dstNodeId, double connSize, long time, Metadata metadata) {
        assert(srcNodeId != dstNodeId);

        // Create new connection
        Connection connection = new Connection(simulator, network.getNode(srcNodeId), network.getNode(dstNodeId), connSize);
        connections.add(connection);
        if (metadata != null) {
            connection.setMetadata(metadata);
        }

        // Create and add event to existing event collection
        ConnectionStartEvent event = new ConnectionStartEvent(simulator, time, connection, routingStrategy);
        connectionStartEventsList.add(event);

    }

    /**
     * Retrieve the list of all stored connection start events.
     *
     * @return  List of all connection start events
     */
    public List<Event> getConnectionStartEvents() {
        return Collections.unmodifiableList(connectionStartEventsList);
    }

    /**
     * Retrieve the list of connections.
     *
     * @return  List of all connections
     */
    public List<Connection> getConnections() {
        return Collections.unmodifiableList(connections);
    }

}