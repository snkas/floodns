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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The <b>connection</b> is a user-level concept. It expresses the desire to
 * move a certain flow size from source {@link Node node} A to target node B.
 * It can encompass zero or more {@link Flow network flows} over time to achieve
 * this goal. For example, ECMP would have connections with only a single network flow,
 * whereas MPTCP would have multiple network flows per connection. The flows assigned
 * to a connection can change over time, as flows are ended and started. It is however
 * impossible for a flow to be assigned to more than one connection in its entire lifetime.
 *
 * @see Node
 * @see Flow
 */
public class Connection {

    // Simulator
    private Simulator simulator;

    // Properties
    private final int connectionId;
    private final Node srcNode;
    private final Node dstNode;
    private final int srcNodeId;
    private final int dstNodeId;
    private final double totalSize;

    // State
    private double remainder;
    private double remainderUpdateThreshold;
    private Set<Flow> flows;
    private List<Integer> pastAndPresentFlowIds;
    private double totalBandwidth;
    public enum Status {
        AWAITING_ACTIVATION,
        ACTIVE,
        TERMINATED
    }
    private Status status;

    // Logging
    private ConnectionLogger logger;

    // Metadata
    private Metadata metadata;

    /**
     * Constructor for connection.
     *
     * @param simulator     Simulator instance
     * @param srcNode       Source node
     * @param dstNode       Destination node
     * @param totalSize     Total size to be transmitted from source to destination (&gt; 0)
     */
    public Connection(Simulator simulator, Node srcNode, Node dstNode, double totalSize) {

        // Endpoints must be distinct
        if (srcNode.getNodeId() == dstNode.getNodeId()) {
            throw new IllegalArgumentException("Cannot create a connection to the same node.");
        }

        // Illegal connection flow size
        if (totalSize <= simulator.getFlowPrecision()) {
            throw new IllegalArgumentException("Total flow size to transfer must be positive and nonzero.");
        }

        // Properties
        this.connectionId = simulator.getNextConnectionId();
        this.srcNode = srcNode;
        this.dstNode = dstNode;
        this.srcNodeId = srcNode.getNodeId();
        this.dstNodeId = dstNode.getNodeId();
        this.totalSize = totalSize;

        // State
        this.remainder = totalSize;
        this.remainderUpdateThreshold = totalSize;
        this.flows = new HashSet<>();
        this.pastAndPresentFlowIds = new ArrayList<>();
        this.totalBandwidth = 0.0;
        this.status = Status.AWAITING_ACTIVATION;

        // Logging
        this.metadata = null;
        this.setLoggerViaSimulator(simulator);

    }

    /**
     * Retrieve the status of the connection.
     *
     * @return  Connection status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Set the status of the connection to ACTIVE.
     */
    void setActive() {
        assert(status == Status.AWAITING_ACTIVATION);
        this.status = Status.ACTIVE;
    }

    /**
     * Set the status of the connection to TERMINATED.
     */
    void setTerminated() {
        assert(status == Status.ACTIVE);
        this.status = Status.TERMINATED;
    }

    /**
     * Retrieve the simulator this flow belongs to.
     *
     * @return  Simulator
     */
    Simulator getSimulator() {
        return simulator;
    }

    /**
     * Retrieve unique connection identifier.
     *
     * @return  User flow identifier
     */
    public int getConnectionId() {
        return connectionId;
    }

    /**
     * Add a new flow path to the connection.
     *
     * @param flow  New flow instance to add to this connection
     */
    void addFlow(Flow flow) {
        assert(flow.getPath().getSrcNode().equals(srcNode) && flow.getPath().getDstNode().equals(dstNode));
        flows.add(flow);
        pastAndPresentFlowIds.add(flow.getFlowId());
    }

    /**
     * Remove an existing flow from the connection.
     *
     * @param flow  Existing flow instance
     */
    void removeFlow(Flow flow) {
        assert(flows.contains(flow));
        flows.remove(flow);
        adaptBandwidth(-flow.getCurrentBandwidth());
    }

    /**
     * Remove unnecessary references such that garbage
     * collection can remove them.
     */
    void cleanup() {
        assert(flows.isEmpty() && !pastAndPresentFlowIds.isEmpty());
        flows = null;
        pastAndPresentFlowIds.clear();
        pastAndPresentFlowIds = null;
    }

    /**
     * Retrieve the set of active flows which are <b>added to the network</b> .
     *
     * @return  Set of active flows (null, if {@link #cleanup() cleaned up})
     */
    public Set<Flow> getActiveFlows() {
        return flows;
    }

    /**
     * Get all flow identifiers of flows that have been
     * used by the connection.
     *
     * @return  List of flow identifiers
     */
    public List<Integer> getPastAndPresentFlowIds() {
        return pastAndPresentFlowIds;
    }

    /**
     * Get source node identifier.
     *
     * @return  Source node identifier
     */
    public int getSrcNodeId() {
        return srcNodeId;
    }

    /**
     * Get destination node identifier.
     *
     * @return  Destination node identifier
     */
    public int getDstNodeId() {
        return dstNodeId;
    }

    /**
     * Get source node.
     *
     * @return  Source node
     */
    public Node getSrcNode() {
        return srcNode;
    }

    /**
     * Get destination node identifier.
     *
     * @return  Destination node identifier
     */
    public Node getDstNode() {
        return dstNode;
    }

    /**
     * Get the total absolute size of the connection.
     *
     * @return  Total size of the connection
     */
    public double getTotalSize() {
        return totalSize;
    }

    /**
     * Reduce the remainder.
     *
     * @param elapsed    Amount of time elapsed
     *
     * @return  True iff the complete flow has been completed
     */
    boolean reduceRemainder(long elapsed) {
        assert(elapsed > 0);

        // Reduce remainder, and check if complete
        remainder -= elapsed * totalBandwidth;
        remainderUpdateThreshold -= elapsed * totalBandwidth;
        return remainder <= simulator.getFlowPrecision();

    }

    /**
     * Calculate the amount of time needed for this connection to surpass the update threshold.
     *
     * @return  Time till update (upper bound)
     */
    public long timeTillUpdateNeeded() {
        if (totalBandwidth == 0.0) {
            return Long.MAX_VALUE;
        }
        if (remainderUpdateThreshold > simulator.getFlowPrecision()) {
            return (long) Math.ceil((remainderUpdateThreshold - simulator.getFlowPrecision()) / totalBandwidth);
        } else {
            return (long) Math.ceil((remainder - simulator.getFlowPrecision()) / totalBandwidth);
        }
    }

    /**
     * Set the remainder update threshold.
     *
     * @param threshold     New remainder update threshold (cannot be larger than the remainder)
     */
    public void setRemainderUpdateThreshold(double threshold) {
        if (threshold > remainder) {
            throw new IllegalArgumentException(String.format(
                    "Illegal remainder update threshold %f (larger than remainder = %f)."
            , threshold, remainder));
        }
        remainderUpdateThreshold = threshold;
    }

    /**
     * Get the remainder update threshold.
     *
     * @return  Remainder update threshold
     */
    public double getRemainderUpdateThreshold() {
        return remainderUpdateThreshold;
    }

    /**
     * Check whether the remainder update threshold has passed.
     *
     * @return  True iff the remainder update threshold is passed (within precision)
     */
    public boolean isRemainderUpdateThresholdPassed() {
        return remainderUpdateThreshold <= simulator.getFlowPrecision();
    }

    /**
     * Calculate the bandwidth of this connection by adding
     * up the bandwidth of all encapsulated flows that belong to it.
     */
    void adaptBandwidth(double deltaBandwidth) {
        totalBandwidth += deltaBandwidth;
        if (totalBandwidth < 0.0) {
            totalBandwidth = 0.0;
        }
        logger.logConnectionStateChange(totalBandwidth);
    }

    /**
     * Retrieve total bandwidth of the connection up until the current time tick.
     * It is only recalculated after the time tick is over and all the flows
     * belonging to the connection are final.
     *
     * @return  Total bandwidth up until this time tick
     */
    public double getTotalBandwidth() {
        return totalBandwidth;
    }

    /**
     * Retrieve remainder flow size of the connection.
     *
     * NOTE: can be negative (within at most the sum of bandwidth). This happens when the
     * previous remainder divided by the total bandwidth was not a whole number.
     * For example, if you have 20 flow units / time unit, and a flow of size 1001 units,
     * it will take 51 time units to transfer all the flow units. The remainder will thus be
     * -19 flow units.
     *
     * @return  Connection flow size remainder
     */
    public double getRemainder() {
        return remainder;
    }

    /**
     * Set the logger for this connection.
     *
     * @param simulator    Simulator
     */
    void setLoggerViaSimulator(Simulator simulator) {
        this.simulator = simulator;
        this.logger = simulator.getLoggerFactory().internalCreateConnectionLogger(this);
        this.logger.logConnectionStateChange(0.0);
    }

    /**
     * Get the logger of this connection.
     *
     * @return  Logger instance
     */
    ConnectionLogger getLogger() {
        return logger;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && connectionId == ((Connection) o).connectionId;
    }

    @Override
    public int hashCode() {
        return connectionId;
    }

    @Override
    public String toString() {
        return "Connection#" + connectionId + "[ " + srcNodeId + " -> " + dstNodeId + "; size=(" + remainder + "/"
               + totalSize + " remaining); flows=" + getPastAndPresentFlowIds() + " ]";
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
