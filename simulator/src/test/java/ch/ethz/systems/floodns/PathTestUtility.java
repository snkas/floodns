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

import ch.ethz.systems.floodns.core.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.List;

public class PathTestUtility {

    public static Flow startFlow(Simulator simulator, AcyclicPath path) {
        return startFlow(simulator, path, 1000);
    }

    public static Flow startFlow(Simulator simulator, AcyclicPath path, double totalSize) {
        Connection conn = new Connection(simulator, path.getSrcNode(), path.getDstNode(), totalSize);
        simulator.activateConnection(conn);
        simulator.addFlowToConnection(conn, path);
        return conn.getActiveFlows().iterator().next();
    }

    public static Flow startSimpleFlow(Simulator simulator, Network network, String pathStr) {
        return startFlow(simulator, createAcyclicPath(network, pathStr));
    }

    public static Flow startSimpleFlow(Simulator simulator, Network network, String pathStr, double totalSize) {
        return startFlow(simulator, createAcyclicPath(network, pathStr), totalSize);
    }

    public static Flow startSemiSpecificFlow(Simulator simulator, Network network, String pathStr) {
        return startFlow(simulator, semiSpecificAcyclicPath(network, pathStr));
    }

    /**
     * Create an acyclic path using the string.
     *
     * @param network   Network
     * @param p         Acyclic path string (e.g. "1-2-3-9-11")
     *
     * @return  Acyclic path
     */
    public static AcyclicPath createAcyclicPath(Network network, String p) {
        AcyclicPath path = new AcyclicPath();
        String[] spl = p.split("-");
        int prev = Integer.valueOf(spl[0]);
        for (int i = 1; i < spl.length; i++) {
            path.add(network.getPresentLinksBetween(prev, Integer.valueOf(spl[i])).get(0));
            prev = Integer.valueOf(spl[i]);
        }
        return path;
    }

    /**
     * Create a path which is potentially cyclic.
     *
     * @param network   Network
     * @param p         Path string (e.g. "44-3-2", "3-29-3-0-29-1")
     *
     * @return  Path (could be acyclic)
     */
    public static List<Link> createPotentiallyCyclicPath(Network network, String p) {
        List<Link> path = new ArrayList<>();
        String[] spl = p.split("-");
        int prev = Integer.valueOf(spl[0]);
        for (int i = 1; i < spl.length; i++) {
            path.add(network.getPresentLinksBetween(prev, Integer.valueOf(spl[i])).get(0));
            prev = Integer.valueOf(spl[i]);
        }
        return path;
    }

    /**
     * Create acyclic path with being able to select which of the multiple links to take.
     *
     * @param network   Network
     * @param p         Acyclic path "0(0)-1(0)-2"
     *
     * @return  Acyclic path with specific double links taken
     */
    public static AcyclicPath semiSpecificAcyclicPath(Network network, String p) {
        AcyclicPath path = new AcyclicPath();
        String[] spl = p.split("-");
        ImmutablePair<Integer, Integer> prev = getNextAndSpec(spl[0]);
        for (int i = 1; i < spl.length; i++) {
            ImmutablePair<Integer, Integer> next = getNextAndSpec(spl[i]);
            path.add(network.getPresentLinksBetween(prev.getLeft(), next.getLeft()).get(prev.getRight()));
            prev = next;
        }
        return path;
    }

    private static ImmutablePair<Integer, Integer> getNextAndSpec(String s) {
        String[] spl = s.split("\\(");
        if (spl.length == 1) {
            return new ImmutablePair<>(Integer.valueOf(s), 0);
        } else {
            return new ImmutablePair<>(Integer.valueOf(spl[0]), Integer.valueOf(spl[1].substring(0, spl[1].length() - 1)));
        }
    }

}
