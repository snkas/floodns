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

package ch.ethz.systems.floodns.core;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * The <b>link</b> is a directed edge with a constant capacity <b>c</b>
 * between two {@link Node nodes} <b>a</b> and <b>b</b> (with <b>a</b> != <b>b</b> )
 * in the {@link Network network}. A link can be shared by zero or more {@link Flow flows}.
 *
 * @see Node
 * @see Network
 * @see Flow
 */
public class Link {

    // Simulator
    private Simulator simulator;

    // Link properties
    private Network network;
    private int linkId;
    private final ImmutablePair<Integer, Integer> fromToPair;
    private final int from;
    private final int to;
    private final Node fromNode;
    private final Node toNode;
    private final double capacity;

    // Flow management
    private final HashMap<Integer, Flow> idToFlow;
    private double remainderCapacity;

    // Logging
    private LinkLogger logger;

    // Metadata
    private Metadata metadata;

    /**
     * Constructor for link.
     *
     * @param simulator     Simulator instance
     * @param network       Network it is part of
     * @param linkId        Unique link identifier
     * @param fromNode      Source node instance
     * @param toNode        Destination node instance
     * @param capacity      Link capacity (> 0)
     */
    Link(Simulator simulator, Network network, int linkId, Node fromNode, Node toNode, double capacity) {
        assert(fromNode.getNodeId() != toNode.getNodeId() && capacity > 0);
        this.network = network;
        this.linkId = linkId;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.from = fromNode.getNodeId();
        this.to = toNode.getNodeId();
        this.fromToPair = new ImmutablePair<>(from, to);
        this.capacity = capacity;
        this.remainderCapacity = capacity;
        this.idToFlow = new HashMap<>();
        this.metadata = null;
        this.setLoggerViaSimulator(simulator);
    }

    /**
     * Retrieve the simulator this link belongs to.
     *
     * @return  Simulator
     */
    Simulator getSimulator() {
        return simulator;
    }

    /**
     * Retrieve the network this link belongs to.
     *
     * @return  Network
     */
    Network getNetwork() {
        return network;
    }

    /**
     * Retrieve the unique link identifier.
     *
     * @return  Link identifier
     */
    public int getLinkId() {
        return linkId;
    }

    /**
     * Fix the allocation for a specific flow.
     *
     * @param flow            Flow instance
     * @param oldBandwidth    Old flow bandwidth
     * @param newBandwidth    New flow bandwidth
     */
    void fixAllocationForFlow(Flow flow, double oldBandwidth, double newBandwidth) {
        assert(idToFlow.containsKey(flow.getFlowId())); // Is the flow present on this link
        assert(flow.getCurrentBandwidth() == newBandwidth);
        remainderCapacity += oldBandwidth;
        if (newBandwidth - remainderCapacity >= simulator.getFlowPrecision()) {
            throw new IllegalArgumentException(
                    "Impossible to allocate for flow " + flow + " the bandwidth (" + newBandwidth +
                    ") because it exceeds remainder link capacity (" + remainderCapacity + ") " +
                    "on link " + this + "."
            );
        }
        remainderCapacity -= newBandwidth;
        logger.logLinkUtilizationChange(getUtilization());
    }

    /**
     * Add a flow to the link.
     *
     * @param flow    Flow instance
     */
    void addActiveFlow(Flow flow) {
        assert(!idToFlow.containsKey(flow.getFlowId()));
        idToFlow.put(flow.getFlowId(), flow);
        logger.logLinkNumActiveFlowsChange(idToFlow.size());
    }

    /**
     * Remove an existing active flow from the link.
     *
     * @param flow    Flow instance
     */
    void removeFlow(Flow flow) {
        assert(idToFlow.containsKey(flow.getFlowId()));

        // Remove flow identifier from active flows on this link
        idToFlow.remove(flow.getFlowId());
        logger.logLinkNumActiveFlowsChange(idToFlow.size());

        // Remove any bandwidth allocation
        remainderCapacity += flow.getCurrentBandwidth();
        logger.logLinkUtilizationChange(getUtilization());

    }

    /**
     * Retrieve the amount of capacity unused currently of the link.
     *
     * @return  Amount of unused bandwidth of the link
     */
    public double getRemainderCapacity() {
        return remainderCapacity;
    }

    /**
     * Get identifier of source node of the link.
     *
     * @return  Source node identifier
     */
    public int getFrom() {
        return from;
    }

    /**
     * Get identifier of destination node of the link.
     *
     * @return  Destination node identifier
     */
    public int getTo() {
        return to;
    }

    /**
     * Get immutable ({@link #getFrom() from}, {@link #getTo() to})-pair.
     *
     * @return  From-to pair
     */
    public ImmutablePair<Integer, Integer> getFromToPair() {
        return fromToPair;
    }

    /**
     * Get source node of the link.
     *
     * @return  Source node
     */
    public Node getFromNode() {
        return fromNode;
    }

    /**
     * Get destination node of the link.
     *
     * @return  Destination node
     */
    public Node getToNode() {
        return toNode;
    }

    /**
     * Retrieve link capacity.
     *
     * @return  Link capacity
     */
    public double getCapacity() {
        return capacity;
    }

    /**
     * Calculate current utilization of the link.
     *
     * @return  Link utilization (range [0.0 ,1.0] within flow precision)
     */
    public double getUtilization() {
        return (capacity - remainderCapacity) / capacity;
    }

    /**
     * Get unmodifiable set of flows identifiers of flows that are currently active on this link.
     *
     * @return Unmodifiable collection of flow identifiers of active flows
     */
    public Set<Integer> getActiveFlowIds() {
        return Collections.unmodifiableSet(idToFlow.keySet());
    }

    /**
     * Get unmodifiable collection of flows that are currently active on this link.
     *
     * @return Unmodifiable collection of active flows
     */
    public Collection<Flow> getActiveFlows() {
        return Collections.unmodifiableCollection(idToFlow.values());
    }

    /**
     * Set the logger for this link.
     *
     * @param simulator    Simulator
     */
    void setLoggerViaSimulator(Simulator simulator) {
        this.simulator = simulator;
        this.logger = simulator.getLoggerFactory().internalCreateLinkLogger(this);
        this.logger.logLinkUtilizationChange(0.0);
        this.logger.logLinkNumActiveFlowsChange(0);
    }

    /**
     * Get the logger of this link.
     *
     * @return  Logger instance
     */
    LinkLogger getLogger() {
        return logger;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && linkId == ((Link) o).linkId;
    }

    @Override
    public int hashCode() {
        return linkId;
    }

    @Override
    public String toString() {
        return "Link#" + linkId + "[ " + from + " -> " + to + " (" + remainderCapacity + "/" + capacity + " left) ]";
    }

    /**
     * Set the metadata.
     *
     * @param metadata  Metadata
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Get the Metadata (e.g., for reading it, editing it, or logging).
     *
     * @return Metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

}
