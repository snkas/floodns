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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The <b>acyclic path</b> is used as path for a {@link Flow flow}
 * in the {@link Network network} and consists of a list of {@link Link links}.
 *
 * @see Flow
 * @see Network
 * @see Link
 */
public class AcyclicPath extends ArrayList<Link> {
    // Inherits all other methods and properties from parent

    // Set of existing nodes somewhere on the path
    private Set<Integer> existing = new HashSet<>();
    private int previous = -1;
    private Network network = null;

    @Override
    public boolean add(Link link) {

        // Continuity check
        if (previous != link.getFrom() && previous != -1) {
            throw new IllegalArgumentException("Not allowed to create discontinuous path.");
        }
        previous = link.getTo();

        // Cycle check
        if (existing.contains(link.getTo())) {
            throw new IllegalArgumentException("Not allowed to create a cyclic path.");
        }
        existing.add(link.getFrom());
        existing.add(link.getTo());

        // Simulator consistency
        if (network == null) {
            network = link.getNetwork();
        } else if (network != link.getNetwork()) {
            throw new IllegalArgumentException("Cannot add link of a different network.");
        }

        // Add to internal state
        return super.add(link);

    }

    /**
     * Retrieve the simulator used throughout the path.
     * This can be different from when it was added, as the paths could've been prepared before the simulation run.
     * Nevertheless, because they all belong to the same network, the first link will be the leading simulator,
     * because all links are part of the same network. And simulator change happens on a network-level.
     *
     * @return  Simulator instance
     */
    Simulator getSimulator() {
        assert(size() > 0);
        return this.get(0).getSimulator();
    }

    @Override
    public boolean addAll(Collection<? extends Link> c) {
        Object[] a = c.toArray();
        for (Object b : a) {
            add((Link) b);
        }
        return true;
    }

    /**
     * Retrieve source (first) node of the path.
     *
     * @return  Source node
     */
    public Node getSrcNode() {
        return this.get(0).getFromNode();
    }

    /**
     * Retrieve destination (last) node of the path.
     *
     * @return  Destination node
     */
    public Node getDstNode() {
        return this.get(this.size() - 1).getToNode();
    }

}
