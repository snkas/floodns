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

package ch.ethz.systems.floodns.ext.basicsim.topology;

import java.util.*;

public class TopologyDetails {

    private int numNodes;
    private int numUndirectedEdges;
    private Set<Integer> serverNodeIds;
    private Set<Integer> switchNodeIds;
    private Set<Integer> switchesWhichAreTorsNodeIds;
    private Map<Integer, Set<Integer>> torToServerIds;
    private Map<Integer, Integer> serverToTorId;

    TopologyDetails() {
        this.numNodes = -1;
        this.numUndirectedEdges = -1;
        this.serverNodeIds = null;
        this.switchNodeIds = null;
        this.switchesWhichAreTorsNodeIds = null;
    }

    /**
     * Set the nodes having a transport layer using a set of identifiers.
     *
     * @param torNodeIds   Set of identifiers
     */
    void setServerNodeIds(Set<Integer> torNodeIds) {
        this.serverNodeIds = torNodeIds;
    }

    /**
     * Set the nodes which only function as switches.
     *
     * @param switchNodeIds   Set of identifiers
     */
    void setSwitchNodeIds(Set<Integer> switchNodeIds) {
        this.switchNodeIds = switchNodeIds;
    }

    /**
     * Set the nodes marked as ToRs using a set of identifiers.
     *
     * @param switchesWhichAreTorsNodeIds   Set of identifiers
     */
    void setSwitchesWhichAreTorsNodeIds(Set<Integer> switchesWhichAreTorsNodeIds) {
        this.switchesWhichAreTorsNodeIds = switchesWhichAreTorsNodeIds;
        this.torToServerIds = new HashMap<>();
        for (Integer x : switchesWhichAreTorsNodeIds) {
            torToServerIds.put(x, new HashSet<Integer>());
        }
        this.serverToTorId = new HashMap<>();
    }

    /**
     * Save that a ToR has a server linked to it.
     *
     * @param torId     ToR identifier
     * @param serverId  Server identifier
     */
    void saveTorHasServer(int torId, int serverId) {
        torToServerIds.get(torId).add(serverId);
        serverToTorId.put(serverId, torId);
    }

    /**
     * Set the number of nodes.
     *
     * @param numNodes  Number of nodes
     */
    void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    /**
     * Set the number of undirected edges.
     *
     * @param numUndirectedEdges  Number of undirected edges
     */
    void setNumUndirectedEdges(int numUndirectedEdges) {
        this.numUndirectedEdges = numUndirectedEdges;
    }

    /**
     * Retrieve number of nodes.
     *
     * @return  Number of nodes
     */
    public int getNumNodes() {
        return numNodes;
    }

    /**
     * Retrieve number of undirected edges.
     *
     * @return  Number of undirected edges
     */
    public int getNumUndirectedEdges() {
        return numUndirectedEdges;
    }

    /**
     * Retrieve all node identifiers marked which are denoted as servers (can be used as endpoints of a connection).
     *
     * @return  List of server node identifiers
     */
    public Set<Integer> getServerNodeIds() {
        return Collections.unmodifiableSet(serverNodeIds);
    }

    /**
     * Retrieve all node identifiers only doing switching.
     *
     * @return  List of switch node identifiers
     */
    public Set<Integer> getSwitchNodeIds() {
        return Collections.unmodifiableSet(switchNodeIds);
    }

    /**
     * Retrieve all node identifiers marked as ToR.
     *
     * @return  List of ToR node identifiers
     */
    public Set<Integer> getSwitchesWhichAreTorsNodeIds() {
        return Collections.unmodifiableSet(switchesWhichAreTorsNodeIds);
    }

    /**
     * Get all the servers associated with a certain ToR.
     *
     * @param torNodeId     ToR node identifier
     *
     * @return Set of associated servers
     */
    public Set<Integer> getServersOfTor(int torNodeId) {
        return torToServerIds.get(torNodeId);
    }

    /**
     * Retrieve the ToR identifier of ToR to which the server belongs and is connected to.
     *
     * @param serverId  Server identifier
     *
     * @return  ToR identifier
     */
    public int getTorIdOfServer(int serverId) {
        return serverToTorId.get(serverId);
    }

    /**
     * Get the number of Top of Racks (ToRs) (either all
     * ToRs are servers, or every server is connected to a ToR).
     *
     * @return  Number of ToRs
     */
    public int getNumSwitchesWhichAreTors() {
        return switchesWhichAreTorsNodeIds.size();
    }

    /**
     * Get the number of switches (nodes that cannot have
     * a transport layer, nor be directly connected to a server).
     *
     * @return  Number of switches
     */
    public int getNumSwitches() {
        return switchNodeIds.size();
    }

    /**
     * Get the number of servers (nodes with transport layer).
     *
     * @return  Number of servers
     */
    public int getNumServers() {
        return serverNodeIds.size();
    }

    /**
     * If the topology has zero servers, the ToRs are endpoints.
     * If there are servers, the servers are endpoints.
     *
     * @return True iff the topology has zero servers (i.e., ToRs are the only endpoints)
     */
    public boolean areTorsEndpoints() {
        return serverNodeIds.size() == 0;
    }

    /**
     * Check whether a node is a valid endpoint.
     *
     * @param nodeId    Node identifier
     *
     * @return True iff (a) there are servers and it is a server, or (b) if there are no servers and it is a ToR.
     */
    public boolean isValidEndpoint(int nodeId) {
        if (areTorsEndpoints()) {
            return switchesWhichAreTorsNodeIds.contains(nodeId);
        } else {
            return serverNodeIds.contains(nodeId);
        }
    }

    /**
     * Check whether a node is an invalid endpoint.
     *
     * @param nodeId    Node identifier
     *
     * @return False iff (a) there are servers and it is a server, or (b) if there are no servers and it is a ToR.
     */
    public boolean isInvalidEndpoint(int nodeId) {
        return !isValidEndpoint(nodeId);
    }

    /**
     * Retrieve all the valid endpoints.
     *
     * @return Set of endpoints (all ToRs if there are no servers, if there are servers, only the servers)
     */
    public Set<Integer> getEndpoints() {
        if (areTorsEndpoints()) {
            return switchesWhichAreTorsNodeIds;
        } else {
            return serverNodeIds;
        }
    }

}
