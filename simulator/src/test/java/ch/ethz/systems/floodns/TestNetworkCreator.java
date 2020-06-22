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

package ch.ethz.systems.floodns;

import ch.ethz.systems.floodns.core.Network;

public class TestNetworkCreator {

    /**
     * Fully connected n-graph with bi-directional links
     *
     * E.g. n=4:
     *
     *  0 - - 1
     *  | \ / |
     *  | / \ |
     *  3 - - 2
     *
     *  @param n    Number of nodes
     *  @param cap  Link capacity
     *
     * @return  Stand-alone network instance
     */
    public static Network fullyConnected(int n, double cap) {
        Network network = new Network(n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    network.addLink(i, j, cap);
                }
            }
        }
        return network;
    }

    /**
     * Star n-graph with bi-directional links
     *
     * E.g. n=5, center=3:
     *
     *  0   1
     *   \ /
     *    3
     *   / \
     *  2   4
     *
     *  @param n        Number of nodes
     *  @param center   Center node index
     *  @param cap      Link capacity
     *
     * @return  Stand-alone network instance
     */
    public static Network star(int n, int center, double cap) {
        Network network = new Network(n);
        for (int i = 0; i < n; i++) {
            if (i != center) {
                network.addLink(center, i, cap);
                network.addLink(i, center, cap);
            }
        }
        return network;
    }

}
