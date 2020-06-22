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

import java.util.*;

/**
 * The <b>network</b> consists of {@link Node nodes} which are connected to each
 * other via {@link Link links}. Over these links, {@link Flow flows} are being
 * routed on {@link AcyclicPath acyclic paths}. Construction is performed by
 * specifying the constant number of nodes via {@link #Network(int)}, and
 * the addition of links is done via {@link #addLink(int, int, double)}).
 * During the simulation, all network manipulation tasks are performed
 * using the interface provided by the {@link Simulator simulator}.
 *
 * @see Node
 * @see Link
 * @see Flow
 * @see AcyclicPath
 * @see Simulator
 */
public class Network {

    // Simulator handle
    private Simulator simulator;

    // Graph properties
    private final int numNodes;
    private final List<Node> nodes;

    // Link management
    private int linkIdCounter;
    private final Set<Link> presentLinks;
    private final Map<Integer, Link> idToPresentLink;
    private final Map<ImmutablePair<Integer, Integer>, List<Link>> nodeIdsToPresentLinks;

    // Flow management
    private int flowIdCounter;
    private final Map<Integer, Flow> idToActiveFlow;
    private final Set<Link> flowActiveLinks;

    /**
     * Constructor for network.
     *
     * The number of nodes are fixed beforehand, given by the parameter.
     *
     * After instantiation, adding links to the network is done using {@link #addLink(int, int, double) addLink},
     * adding/updating flows is done using {@link #startFlow(Connection, AcyclicPath) startFlow}, and removing
     * flows is done using {@link #endFlow(Flow) removeExistingFlow}.
     *
     * @param numNodes      Number of nodes n
     *
     * @see #startFlow(Connection, AcyclicPath)
     * @see #endFlow(Flow)
     * @see #addLink(int, int, double)
     */
    public Network(int numNodes) {
        assert(numNodes >= 0);

        // Initially the simulator handle is a dummy
        this.simulator = new Simulator();

        // Graph variables
        this.numNodes = numNodes;
        this.nodes = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            this.nodes.add(new Node(simulator, i));
        }
        this.idToPresentLink = new HashMap<>();
        this.presentLinks = new HashSet<>();
        this.nodeIdsToPresentLinks = new HashMap<>();
        this.linkIdCounter = 0;

        // Flow variables
        this.flowIdCounter = 0;
        this.idToActiveFlow = new HashMap<>();
        this.flowActiveLinks = new HashSet<>();

    }

    /**
     * Couple the network to the simulator after its setup, such that all logging will be
     * done through that simulator.
     *
     * @param   simulator   Simulator instance
     */
    void setupLoggersWithSimulator(Simulator simulator) {

        // Cannot have remaining flows when binding to simulator, which is impossible
        // as flows are only existent during a simulation
        assert(flowIdCounter == 0 && this.getActiveFlows().size() == 0);

        // Set the simulator
        this.simulator = simulator;

        // Set the links' loggers
        for (Link link : getPresentLinks()) {
            link.setLoggerViaSimulator(simulator);
        }

        // Set the nodes' loggers
        for (Node node : nodes) {
            node.setLoggerViaSimulator(simulator);
        }

    }

    /**
     * Add a directional link to the network.
     *
     * @param from          Origin node identifier
     * @param to            Target node identifier
     * @param capacity      Capacity of link (must be &gt; 0)
     *
     * @return  Link instance that was created and added to the network
     *
     * @throws IllegalArgumentException     Iff the node identifiers are invalid or the capacity is lower or equal to 0.
     */
    public Link addLink(int from, int to, double capacity) {

        if (from < 0 || to < 0 || from >= numNodes || to >= numNodes) {
            throw new IllegalArgumentException("From (" + from + ") or to (" + to + ") outside of index range [0, "
                                               + numNodes + ").");
        } else if (capacity <= 0) {
            throw new IllegalArgumentException("Link capacity must be greater than 0 (given: " + capacity + ").");
        }

        // Initialize nodes and the link
        Node fromNode = nodes.get(from);
        Node toNode = nodes.get(to);
        Link link = new Link(simulator, this, linkIdCounter, fromNode, toNode, capacity);
        idToPresentLink.put(linkIdCounter, link);
        linkIdCounter++;
        fromNode.addLink(link);
        toNode.addLink(link);

        // Add to central collection
        presentLinks.add(link);

        // Add to mapping of pair of nodes to set of available links
        List<Link> linkSetBetweenNodes = nodeIdsToPresentLinks.get(link.getFromToPair());
        if (linkSetBetweenNodes == null) {
            linkSetBetweenNodes = new ArrayList<>();
        }
        linkSetBetweenNodes.add(link);
        nodeIdsToPresentLinks.put(link.getFromToPair(), linkSetBetweenNodes);

        // Return link instance
        return link;

    }

    /**
     * Remove an already present link from the network.
     *
     * @param link  Link instance
     *
     * @throws IllegalArgumentException     Iff the link is not currently present.
     */
    void removeLink(Link link) {

        if (!presentLinks.contains(link)) {
            throw new IllegalArgumentException("Link " + link + " is not present and as such cannot be removed.");
        }

        // Remove link from nodes
        link.getFromNode().removeLink(link);
        link.getToNode().removeLink(link);

        // Remove link from network mapping
        idToPresentLink.remove(link.getLinkId());
        presentLinks.remove(link);
        List<Link> linkSetBetweenNodes = nodeIdsToPresentLinks.get(link.getFromToPair());
        linkSetBetweenNodes.remove(link);
        if (linkSetBetweenNodes.size() == 0) {
            nodeIdsToPresentLinks.remove(link.getFromToPair());
        }

        // End all flows that were on the link
        while (link.getActiveFlows().size() > 0) {
            endFlow(link.getActiveFlows().iterator().next());
        }

        // Final log flush of the link
        link.getLogger().finalFlush(link.getMetadata());

    }

    /**
     * Get the full list of nodes in the network.
     * The index of a node in the list is equal to its node identifier.
     *
     * @return  Unmodifiable list of all nodes in the network.
     *          Note: changes to the network are propagated to the list after call.
     */
    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Retrieve number of nodes present in the network.
     *
     * @return Present number of nodes
     */
    public int getNumNodes() {
        return numNodes;
    }

    /**
     * Get the full set of present links in the network.
     *
     * @return  Unmodifiable set of all present links in the network.
     *          Note: changes (e.g. link removal) to the network are propagated to the set after call.
     */
    public Set<Link> getPresentLinks() {
        return Collections.unmodifiableSet(presentLinks);
    }

    /**
     * Get the full collection of active flows in the network.
     * A flow is active if it is currently occupying at least one link in the network.
     *
     * @return  Unmodifiable collection of flows currently active in the network.
     *          (note: changes to the network are propagated to the collection after call)
     */
    public Collection<Flow> getActiveFlows() {
        return Collections.unmodifiableCollection(idToActiveFlow.values());
    }

    /**
     * Get the full set of active flows' identifiers in the network.
     * A flow is active if it is currently occupying at least one link in the network.
     *
     * @return  Unmodifiable collection of flows' identifiers currently active in the network.
     *          (note: changes to the network are propagated to the collection after call)
     */
    public Set<Integer> getActiveFlowsIds() {
        return Collections.unmodifiableSet(idToActiveFlow.keySet());
    }

    /**
     * Get the link with the specified link identifier.
     *
     * @param linkId    Link identifier
     *
     * @return  Link instance (null, if not exists)
     */
    public Link getLink(int linkId) {
        return idToPresentLink.get(linkId);
    }

    /**
     * Get the active flow with the specified flow identifier.
     *
     * @param flowId    Flow identifier
     *
     * @return  Flow instance (null, if not exists)
     */
    public Flow getActiveFlow(int flowId) {
        return idToActiveFlow.get(flowId);
    }

    /**
     * Check whether the flow is active.
     *
     * @param flowId    Flow identifier
     *
     * @return  True iff the flow with that identifier exists and is active
     */
    public boolean isFlowActive(int flowId) {
        return idToActiveFlow.containsKey(flowId);
    }

    /**
     * Retrieve all link instances from &rarr; to.
     *
     * @param from  Source node identifier
     * @param to    Destination node identifier
     *
     * @return Unmodifiable list of link instances
     */
    public List<Link> getPresentLinksBetween(int from, int to) {
        List<Link> result = nodeIdsToPresentLinks.get(new ImmutablePair<>(from, to));
        if (result == null) {
            return Collections.unmodifiableList(new ArrayList<Link>(0));
        } else {
            return Collections.unmodifiableList(result);
        }
    }

    /**
     * Retrieve all link instances from &rarr; to.
     *
     * @param fromNode  Source node
     * @param toNode    Destination node
     *
     * @return Unmodifiable list of link instances
     */
    public List<Link> getPresentLinksBetween(Node fromNode, Node toNode) {
        return getPresentLinksBetween(fromNode.getNodeId(), toNode.getNodeId());
    }

    /**
     * Retrieve the set of links on which at least one flow is active.
     *
     * @return  Set of links with at least one flow active
     */
    public Set<Link> getFlowActiveLinks() {
        return Collections.unmodifiableSet(flowActiveLinks);
    }

    /**
     * Retrieve a specific node instance.
     *
     * @param nodeId    Node identifier (0 &lt;= nodeId &lt; n)
     *
     * @return Node instance (null, if node does not exist)
     */
    public Node getNode(int nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Retrieve the set of outgoing links of a node.
     *
     * @param nodeId    Node identifier (0 &lt;= nodeId &lt; n)
     *
     * @return Unmodifiable set of outgoing links
     */
    public Set<Link> getOutgoingLinksOf(int nodeId) {
        return getNode(nodeId).getOutgoingLinks();
    }

    /**
     * Set the path of the flow through the network.
     *
     * Runtime is O(|old_path| + |new_path|).
     *
     * @param connection    Connection instance
     * @param path          Flow path instance
     */
    Flow startFlow(Connection connection, AcyclicPath path) {
        assert(path.size() >= 1);

        // Create the flow
        int flowId = flowIdCounter;

        Flow flow = new Flow(simulator, connection, flowId, path); // Automatically adds itself to the nodes and links
        flowIdCounter++;

        // Put into mapping
        idToActiveFlow.put(flowId, flow);

        // Add path to collection of active links
        flowActiveLinks.addAll(path);

        // Return flow instance
        return flow;

    }

    /**
     * End and remove the flow from the network.
     *
     * Runtime is O(|path|).
     *
     * @param flow      Flow instance
     *
     * @throws IllegalArgumentException     Iff the flow is not active.
     */
    void endFlow(Flow flow) {

        // Flow must be active
        assert(idToActiveFlow.containsKey(flow.getFlowId()));

        // Retrieve flow identifier and delete from network mapping
        idToActiveFlow.remove(flow.getFlowId());

        // Detach the flow from the other components
        detachFlow(flow);

    }

    /**
     * Detach the flow from the other components.
     *
     * @param flow      Flow instance
     */
    private void detachFlow(Flow flow) {

        // Remove flow from parent connection
        flow.getParentConnection().removeFlow(flow);

        flow.detach(); // Removes itself from nodes and links
        for (Link link : flow.getPath()) {
            if (link.getActiveFlowIds().size() == 0) {
                flowActiveLinks.remove(link); // Remove from mapping of active links
            }
        }

        // The logger flushes the final state of the flow
        flow.getLogger().finalFlush(flow.getMetadata());

    }

    /**
     * Clear all the flows of the network. If the run did not add/remove links,
     * it guarantees a clean run again with reproducible results.
     */
    void finalizeFlows() {

        // End all flows
        Iterator<Map.Entry<Integer, Flow>> activeFlowEntries = idToActiveFlow.entrySet().iterator();
        for (; activeFlowEntries.hasNext();) {
            Map.Entry<Integer, Flow> entry = activeFlowEntries.next();
            Flow flow = entry.getValue();

            // Remove the flow entry from the network mapping
            activeFlowEntries.remove();

            // Detach the flow from the other components
            detachFlow(flow);

        }

        // Reset the flow counter
        flowIdCounter = 0;

        // Check that everything was indeed cleared
        assert(idToActiveFlow.isEmpty());
        assert(flowActiveLinks.isEmpty());
        for (Link l : presentLinks) {
            assert(l.getActiveFlowIds().isEmpty());
            assert(l.getActiveFlows().isEmpty());
        }

    }

    @Override
    public String toString() {
        return "Network[ |V|=" + numNodes + ", |E|=" + presentLinks.size() + ", |F|=" + idToActiveFlow.size() + " ]";
    }

}
