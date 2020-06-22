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

package ch.ethz.systems.floodns.ext.allocator;

import ch.ethz.systems.floodns.core.Flow;
import ch.ethz.systems.floodns.core.Link;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The simple max-min fair share (MMFS) allocator takes the tightest link in each step and
 * separates the allocation fairly between the (unfixed) flows on the link.
 * This iterative greedy process will converge with a fair allocation per flow,
 * though not necessarily optimal in terms of total sum of throughput.
 * The simple algorithm does not include flow weights or upper limits.
 *
 * Runtime is O(|E|^2 + |E|*|F|).
 *
 * ------------
 *
 * CITATIONS
 *
 * Upper limit mechanism based on notes at:
 *
 * (1) http://www.ece.rutgers.edu/~marsic/Teaching/CCN/minmax-fairsh.html (retrieved 25 April 2018).
 *
 * (2) Computer Networks: Performance and Quality of Service by Ivan Marsic
 *     http://www.ece.rutgers.edu/~marsic/books/CN/book-CN_marsic.pdf : page 315 (retrieved 25 April 2018)
 */
public class UpperLimitMmfAllocator extends Allocator {

    private final Map<Flow, Double> flowToUpperLimit;

    /**
     * Constructor.
     *
     * @param simulator             Simulator instance
     * @param network               Network instance
     * @param flowToUpperLimit      Flow-to-upper-limit mapping (set null, if none)
     */
    public UpperLimitMmfAllocator(Simulator simulator, Network network, Map<Flow, Double> flowToUpperLimit) {
        super(simulator, network);
        this.flowToUpperLimit = flowToUpperLimit;
    }

    /**
     * Perform the upper limited weighted max-min fair allocation.
     */
    @Override
    public void perform() {

        // Reset all flow bandwidth to zero
        for (Flow f : network.getActiveFlows()) {
            simulator.allocateFlowBandwidth(f, 0);
        }

        // Decide which flows are fixed
        Set<Link> linksWithUnfixedFlows = new HashSet<>(network.getFlowActiveLinks());
        Map<Link, Set<Flow>> linkToUnfixedFlows = new HashMap<>();
        for (Link l : linksWithUnfixedFlows) {
            linkToUnfixedFlows.put(l, new HashSet<>(l.getActiveFlows()));
        }

        while (linksWithUnfixedFlows.size() != 0) {

            Flow tightestFlow = null;
            double tightestAllocation = Double.MAX_VALUE;

            // Go over all the links, and calculate the fair appropriation rate
            for (Link link : linksWithUnfixedFlows) {

                // For each flow, calculate its fair share and see if it is the tightest
                for (Flow f : linkToUnfixedFlows.get(link)) {

                    // Min of the fair share and its upper limit
                    double fairAllocation = Math.min(
                            flowToUpperLimit.get(f),
                            link.getRemainderCapacity() / linkToUnfixedFlows.get(link).size()
                    );

                    // If it is the tightest, set is as such
                    if (fairAllocation < tightestAllocation) {
                        tightestAllocation = fairAllocation;
                        tightestFlow = f;
                    }
                }

            }

            // There was at least one flow present
            assert(tightestFlow != null);

            // Fix the tightest flow
            simulator.allocateFlowBandwidth(tightestFlow, tightestAllocation);
            for (Link l : tightestFlow.getPath()) {
                linkToUnfixedFlows.get(l).remove(tightestFlow);
                if (linkToUnfixedFlows.get(l).size() == 0) {
                    linksWithUnfixedFlows.remove(l);
                }
            }

        }

    }

}
