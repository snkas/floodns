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

import java.util.*;

/**
 * The <b>node</b> is the communicating entity in the {@link Network network}.
 * Nodes are connected to each other via {@link Link links}. A node can serve
 * as both source, intermediary and sink for a {@link Flow flow}.
 *
 * @see Network
 * @see Link
 * @see Flow
 */
public class Node {

    // Node properties
    private final int nodeId;
    private final Set<Link> incomingLinks;
    private final Set<Link> outgoingLinks;
    private final Map<Integer, List<Link>> incomingLinksFromNode;
    private final Map<Integer, List<Link>> outgoingLinksToNode;
    private final Set<Integer> incomingConnectedTo;
    private final Set<Integer> outgoingConnectedTo;

    // Flow management
    private final HashMap<Integer, Flow> idToFlow;

    // Logging
    private NodeLogger logger;

    // Metadata
    private Metadata metadata;

    /**
     * Constructor for node.
     *
     * After construction, addition of links is done using {@link #addLink(Link) addLink},
     * addition of flows using {@link #addActiveFlow(Flow) addActiveFlow}, and removal
     * of flows using {@link #removeFlow(int) removeFlow}.
     *
     * @param simulator     Simulator instance
     * @param nodeId        Node identifier (0 <= nodeId < n)
     */
    Node(Simulator simulator, int nodeId) {
        assert(nodeId >= 0);
        this.nodeId = nodeId;
        this.incomingLinks = new HashSet<>();
        this.outgoingLinks = new HashSet<>();
        this.incomingLinksFromNode = new HashMap<>();
        this.outgoingLinksToNode = new HashMap<>();
        this.incomingConnectedTo = incomingLinksFromNode.keySet();
        this.outgoingConnectedTo = outgoingLinksToNode.keySet();
        this.idToFlow = new HashMap<>();
        this.metadata = null;
        this.setLoggerViaSimulator(simulator);
    }

    /**
     * Add a (uni-directional) link to or from this node.
     *
     * @param link      Link instance
     */
    void addLink(Link link) {
        assert(link.getFrom() == this.nodeId || link.getTo() == this.nodeId);

        // Outgoing link
        if (link.getFrom() == this.nodeId) {
            List<Link> linkSet = outgoingLinksToNode.get(link.getTo());
            if (linkSet == null) {
                linkSet = new ArrayList<>();
            }
            linkSet.add(link);
            outgoingLinksToNode.put(link.getTo(), linkSet);
            outgoingLinks.add(link);

        // Incoming link
        } else {
            List<Link> linkSet = incomingLinksFromNode.get(link.getFrom());
            if (linkSet == null) {
                linkSet = new ArrayList<>();
            }
            linkSet.add(link);
            incomingLinksFromNode.put(link.getFrom(), linkSet);
            incomingLinks.add(link);

        }

    }

    /**
     * Remove a (uni-directional) link to or from this node.
     *
     * @param link      Link instance
     */
    void removeLink(Link link) {
        assert(link.getFrom() == this.nodeId || link.getTo() == this.nodeId);

        // Outgoing link
        if (link.getFrom() == this.nodeId) {
            List<Link> linkSet = outgoingLinksToNode.get(link.getTo());
            linkSet.remove(link);
            if (linkSet.size() == 0) {
                outgoingLinksToNode.remove(link.getTo());
            }
            outgoingLinks.remove(link);

        // Incoming link
        } else {
            List<Link> linkSet = incomingLinksFromNode.get(link.getFrom());
            linkSet.remove(link);
            if (linkSet.size() == 0) {
                incomingLinksFromNode.remove(link.getFrom());
            }
            incomingLinks.remove(link);

        }

    }

    /**
     * Add a flow to the node.
     *
     * @param flow    Flow instance
     */
    void addActiveFlow(Flow flow) {
        assert(!idToFlow.containsKey(flow.getFlowId()));
        idToFlow.put(flow.getFlowId(), flow);
        logger.logNodeStateChange(idToFlow.size());
    }

    /**
     * Remove a flow from the node.
     *
     * @param flowId    Flow identifier
     */
    void removeFlow(int flowId) {
        assert(idToFlow.containsKey(flowId));
        idToFlow.remove(flowId);
        logger.logNodeStateChange(idToFlow.size());
    }

    /**
     * Retrieve the unique node identifier.
     *
     * @return  Node identifier
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Get all incoming links.
     *
     * @return  Unmodifiable set of incoming links
     */
    public Set<Link> getIncomingLinks() {
        return Collections.unmodifiableSet(incomingLinks);
    }

    /**
     * Get all outgoing links.
     *
     * @return Unmodifiable set of outgoing links
     */
    public Set<Link> getOutgoingLinks() {
        return Collections.unmodifiableSet(outgoingLinks);
    }

    /**
     * Get unmodifiable set of node identifiers of which the
     * respective nodes have a link to this node.
     *
     * @return Unmodifiable set of node identifiers with a link to this node
     */
    public Set<Integer> getIncomingConnectedToNodes() {
        return Collections.unmodifiableSet(incomingConnectedTo);
    }

    /**
     * Get unmodifiable set of node identifiers of nodes towards which this node has a link.
     *
     * @return Unmodifiable set of node identifiers to which this node has a link
     */
    public Set<Integer> getOutgoingConnectedToNodes() {
        return Collections.unmodifiableSet(outgoingConnectedTo);
    }

    /**
     * Retrieve set of incoming links from the other node to this node.
     *
     * @param otherNodeId     Other node identifier
     *
     * @return Link set instance (null, if does not exist)
     */
    public List<Link> getIncomingLinksFrom(int otherNodeId) {
        return incomingLinksFromNode.get(otherNodeId) == null ? null
                : Collections.unmodifiableList(incomingLinksFromNode.get(otherNodeId));
    }

    /**
     * Retrieve set of outgoing links from this node to the other node.
     *
     * @param otherNodeId     Other node identifier
     *
     * @return Link set instance (null, if does not exist)
     */
    public List<Link> getOutgoingLinksTo(int otherNodeId) {
        return outgoingLinksToNode.get(otherNodeId) == null ? null
                : Collections.unmodifiableList(outgoingLinksToNode.get(otherNodeId));
    }

    /**
     * Retrieve incoming link from the other node to this node.
     *
     * @param otherNode     Other node instance
     *
     * @return Link instance (null, if does not exist)
     */
    public List<Link> getIncomingLinksFrom(Node otherNode) {
        return getIncomingLinksFrom(otherNode.getNodeId());
    }

    /**
     * Retrieve outgoing link from this node to the other node.
     *
     * @param otherNode     Other node instance
     *
     * @return Link instance (null, if does not exist)
     */
    public List<Link> getOutgoingLinksTo(Node otherNode) {
        return getOutgoingLinksTo(otherNode.getNodeId());
    }

    /**
     * Check whether this node has any incoming links from the node with the given identifier.
     *
     * @param otherNodeId   Other node identifier
     *
     * @return True iff a link exists from the other node to this node
     */
    public boolean hasIncomingLinksFrom(int otherNodeId) {
        return incomingConnectedTo.contains(otherNodeId);
    }

    /**
     * Check whether this node has any outgoing links to the node with the given identifier.
     *
     * @param otherNodeId   Other node identifier
     *
     * @return True iff a link exists from this node to the other
     */
    public boolean hasOutgoingLinksTo(int otherNodeId) {
        return outgoingConnectedTo.contains(otherNodeId);
    }

    /**
     * Check whether this node has any incoming links from the other node.
     *
     * @param otherNode     Other node instance
     *
     * @return True iff a link exists from the other node to this node
     */
    public boolean hasIncomingLinksFrom(Node otherNode) {
        return hasIncomingLinksFrom(otherNode.getNodeId());
    }

    /**
     * Check whether this node has any outgoing links to the other node.
     *
     * @param otherNode     Other node instance
     *
     * @return True iff a link exists from this node to the other
     */
    public boolean hasOutgoingLinksTo(Node otherNode) {
        return hasOutgoingLinksTo(otherNode.getNodeId());
    }

    /**
     * Get unmodifiable set of flows identifiers of flows that are currently active on this node.
     *
     * @return Unmodifiable collection of flow identifiers of active flows
     */
    public Set<Integer> getActiveFlowsIds() {
        return Collections.unmodifiableSet(idToFlow.keySet());
    }

    /**
     * Get unmodifiable collection of flows that are currently active on this node.
     *
     * @return Unmodifiable collection of active flows
     */
    public Collection<Flow> getActiveFlows() {
        return Collections.unmodifiableCollection(idToFlow.values());
    }

    /**
     * Set the logger for this node.
     *
     * @param simulator    Simulator
     */
    void setLoggerViaSimulator(Simulator simulator) {
        this.logger = simulator.getLoggerFactory().internalCreateNodeLogger(this);
        this.logger.logNodeStateChange(0);
    }

    /**
     * Get the logger of this node.
     *
     * @return  Logger instance
     */
    NodeLogger getLogger() {
        return logger;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && nodeId == ((Node) o).nodeId;
    }

    @Override
    public int hashCode() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "Node#" + String.valueOf(nodeId);
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
