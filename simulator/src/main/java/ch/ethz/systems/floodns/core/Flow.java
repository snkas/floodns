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

/**
 * The <b>flow</b> is a presence in the {@link Network network}, which is defined
 * by a {@link Node node} source s and a node sink t (with s != t), between which
 * there is an {@link AcyclicPath acyclic path} P = [s, ..., t], which traverses
 * over existing {@link Link links}. The path P occupies a certain amount of
 * bandwidth on each link it covers, which can be assigned by the {@link Event events}.
 * or the {@link Aftermath aftermath}.
 *
 * @see Network
 * @see Node
 * @see AcyclicPath
 * @see Link
 * @see Aftermath
 * @see Event
 */
public class Flow {

    // Simulator
    private final Simulator simulator;

    // Flow properties
    private int flowId;
    private final Node srcNode;
    private final Node dstNode;
    private final int srcNodeId;
    private final int dstNodeId;

    // Flow management properties
    private double currentBandwidth;
    private final AcyclicPath path;
    private final Connection parentConnection;

    // Logging
    private final FlowLogger logger;

    // Metadata
    private Metadata metadata;

    /**
     * Constructor for flow.
     *
     * @param simulator     Simulator instance
     * @param connection    Connection instance
     * @param flowId        Unique flow identifier
     * @param path          Acyclic path from source to destination
     */
    Flow(Simulator simulator, Connection connection, int flowId, AcyclicPath path) {
        assert(path.size() > 0);

        this.flowId = flowId;
        this.srcNode = path.getSrcNode();
        this.dstNode = path.getDstNode();
        this.srcNodeId = srcNode.getNodeId();
        this.dstNodeId = dstNode.getNodeId();
        this.currentBandwidth = 0;
        this.path = path;
        this.parentConnection = connection;
        this.simulator = simulator;
        this.metadata = null;
        this.logger = simulator.getLoggerFactory().internalCreateFlowLogger(this);
        this.logger.logFlowStateChange(0);

        // Add activity on links and nodes
        srcNode.addActiveFlow(this);
        for (Link link : path) {
            link.getToNode().addActiveFlow(this);
            link.addActiveFlow(this);
        }

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
     * Retrieve the unique flow identifier.
     *
     * @return  Flow identifier
     */
    public int getFlowId() {
        return flowId;
    }

    /**
     * Get source node of the flow.
     *
     * @return  Flow source node
     */
    public Node getSrcNode() {
        return srcNode;
    }

    /**
     * Get destination node of the flow.
     *
     * @return  Flow destination node
     */
    public Node getDstNode() {
        return dstNode;
    }

    /**
     * Get identifier of source node of the flow.
     *
     * @return  Flow source node identifier
     */
    public int getSrcNodeId() {
        return srcNodeId;
    }

    /**
     * Get identifier of destination node of the flow.
     *
     * @return  Flow destination node identifier
     */
    public int getDstNodeId() {
        return dstNodeId;
    }

    /**
     * Allocate the new bandwidth of the flow.
     *
     * @param newBandwidth  New bandwidth (>= 0)
     *
     * @throws IllegalArgumentException     If there is insufficient available bandwidth (can be prevented by checking
     *                                      first by calling {@link #canAllocateBandwidth(double)}) or if the bandwidth
     *                                      is negative.
     */
    void allocateBandwidth(double newBandwidth) {
        assert(currentBandwidth >= 0);

        // Allocations just under zero are set to zero
        if (newBandwidth < 0 && newBandwidth > -simulator.getFlowPrecision()) {
            newBandwidth = 0;
        }

        if (newBandwidth < 0) {
            throw new IllegalArgumentException("Cannot allocate negative flow bandwidth (" + newBandwidth + ").");
        }

        // Save the old bandwidth to adjust the link accordingly
        double oldBandwidth = this.currentBandwidth;

        // Set the bandwidth of the flow
        this.currentBandwidth = newBandwidth;

        // Update logger
        logger.logFlowStateChange(currentBandwidth);

        // Fix the allocation for the flow on the path's links
        for (Link link : path) {
            link.fixAllocationForFlow(this, oldBandwidth, newBandwidth);
        }

        // Update connection
        parentConnection.adaptBandwidth(newBandwidth - oldBandwidth);

    }

    /**
     * Check whether it is possible to allocate a certain amount of bandwidth.
     *
     * @param potentialNewBandwidth     The potential new bandwidth to test if there is enough bandwidth available
     *
     * @return True iff enough capacity is available on this flow's path
     *         (as such, {{@link #allocateBandwidth(double)} will guaranteed to succeed}).
     *         Negative bandwidth will always return false.
     */
    public boolean canAllocateBandwidth(double potentialNewBandwidth) {
        for (Link link : path) {
            if (potentialNewBandwidth - (link.getRemainderCapacity() + currentBandwidth) >= simulator.getFlowPrecision()) {
                return false;
            }
        }
        return potentialNewBandwidth >= 0;
    }

    /**
     * Detach the flow from the nodes and links it is registered to as active.
     */
    void detach() {
        srcNode.removeFlow(flowId);
        for (Link link : path) {
            link.getToNode().removeFlow(flowId);
            link.removeFlow(this);
        }
        this.currentBandwidth = 0;
    }

    /**
     * Retrieve the current bandwidth of the flow.
     *
     * @return  Current flow bandwidth
     */
    public double getCurrentBandwidth() {
        return currentBandwidth;
    }

    /**
     * Retrieve the parent connection.
     *
     * @return  Connection it belongs to (null, if does not exist)
     */
    public Connection getParentConnection() {
        return parentConnection;
    }

    /**
     * Retrieve path of the flow.
     *
     * @return  Acyclic flow path
     */
    public AcyclicPath getPath() {
        return path;
    }

    /**
     * Get the logger of this flow.
     *
     * @return  Logger instance
     */
    FlowLogger getLogger() {
        return logger;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && flowId == ((Flow) o).flowId;

    }

    @Override
    public int hashCode() {
        return flowId;
    }

    @Override
    public String toString() {
        return "Flow#" + flowId + "[ " + srcNodeId + " -> " + dstNodeId + ", bw=" + currentBandwidth + ", path="
                + path + " ]";
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
