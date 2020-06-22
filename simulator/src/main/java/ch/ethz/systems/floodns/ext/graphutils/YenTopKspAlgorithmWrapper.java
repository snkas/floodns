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

package ch.ethz.systems.floodns.ext.graphutils;

import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Link;
import ch.ethz.systems.floodns.core.Network;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Yen's top k shortest paths algorithm.
 */
public class YenTopKspAlgorithmWrapper {

    private final Network network;
    private final YenKShortestPath<Integer, DefaultWeightedEdge> yenKShortestPath;

    /**
     * Yen's K-shortest paths algorithm wrapper.
     *
     * @param network       Network
     */
    public YenTopKspAlgorithmWrapper(Network network) {
        this.network = network;
        SimpleDirectedGraph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedGraph<>(DefaultWeightedEdge.class);
        for (int i = 0; i < network.getNumNodes(); i++) {
            graph.addVertex(i);
        }
        for (Link link : network.getPresentLinks()) {
            if (graph.addEdge(link.getFrom(), link.getTo()) == null) {
                throw new RuntimeException("Cannot edge, likely duplicate");
            }
        }
        this.yenKShortestPath = new YenKShortestPath<>(graph);

    }

    /**
     * Get the K-shortest paths between source and destination.
     *
     * @param src        Source node identifier
     * @param dst        Destination node identifier
     * @param targetK    Target k (result can be less if there are no more paths)
     *
     * @return Ordered list of k-shortest paths (at most target-k of size)
     */
    public List<AcyclicPath> getShortestPaths(int src, int dst, int targetK) {
        List<GraphPath<Integer, DefaultWeightedEdge>> paths = yenKShortestPath.getPaths(src, dst, targetK);
        List<AcyclicPath> result = new ArrayList<>();
        for (GraphPath<Integer, DefaultWeightedEdge> path : paths) {
            AcyclicPath acyclicPath = new AcyclicPath();
            for (int i = 0; i < path.getVertexList().size() - 1; i++) {
                acyclicPath.add(network.getPresentLinksBetween(path.getVertexList().get(i), path.getVertexList().get(i + 1)).get(0));
            }
            result.add(acyclicPath);
        }
        return result;
    }

}
