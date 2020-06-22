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

import ch.ethz.systems.floodns.core.Network;

/**
 * Floyd-Warshall shortest paths algorithm.
 */
public class FloydWarshallAlgorithm {

    private static final int INFINITE_DISTANCE = 999999999;

    private final Network network;

    public FloydWarshallAlgorithm(Network network) {
        this.network = network;
    }

    /**
     * Calculate all the shortest path lengths and store them internally.
     * Uses the modified Floyd-Warshall algorithm.
     *
     * Based on the algorithm described in:
     *
     * Floyd, Robert W. "Algorithm 97: shortest path."
     * Communications of the ACM 5.6 (1962): 345.
     *
     * @return 2-d array with the shortest path distances
     */
    public int[][] calculateShortestPaths() {

        int numNodes = network.getNumNodes();
        int[][] shortestPathLen = new int[numNodes][numNodes];

        // Initial scan to find easy shortest paths
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                if (i == j) {
                    shortestPathLen[i][j] = 0;                       // To itself
                } else if (network.getNode(i).hasOutgoingLinksTo(j)) {
                    shortestPathLen[i][j] = 1;                       // To direct neighbor
                } else {
                    shortestPathLen[i][j] = INFINITE_DISTANCE;       // To someone not directly connected
                }
            }
        }

        // Floyd-Warshall algorithm
        for (int k = 0; k < numNodes; k++) {
            for (int i = 0; i < numNodes; i++) {
                for (int j = 0; j < numNodes; j++) {
                    if (shortestPathLen[i][j] > shortestPathLen[i][k] + shortestPathLen[k][j]) {
                        shortestPathLen[i][j] = shortestPathLen[i][k] + shortestPathLen[k][j];
                    }
                }
            }
        }

        return shortestPathLen;

    }

}
