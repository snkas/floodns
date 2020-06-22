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
import ch.ethz.systems.floodns.ext.utils.ConstantMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Upper limited weighted max-min fair allocation.
 *
 * Each flow has a fixed path p over the network.
 * Each flow has a desired maximum bandwidth, its cap
 * Each flow has a certain weight assigned to it, indicating how important it is.
 *
 * Runtime is not yet determined.
 * This allocator is EXPERIMENTAL.
 *
 * ------------
 *
 * CITATIONS
 *
 * Weight and upper limit mechanism based on notes at:
 *
 * (1) http://www.ece.rutgers.edu/~marsic/Teaching/CCN/minmax-fairsh.html (retrieved 25 April 2018).
 *
 * (2) Computer Networks: Performance and Quality of Service by Ivan Marsic
 *     http://www.ece.rutgers.edu/~marsic/books/CN/book-CN_marsic.pdf : page 315 (retrieved 25 April 2018)
 *
 */
public class GenericMmfAllocator extends Allocator {

    private final Map<Flow, Double> flowToUpperLimit;
    private final Map<Flow, Double> flowToWeight;

    /**
     * Constructor.
     *
     * @param simulator             Simulator instance
     * @param network               Network instance
     * @param flowToUpperLimit      Flow-to-upper-limit mapping (set null, if none)
     * @param flowToWeight          Flow-to-weight mapping (set null, if none)
     */
    public GenericMmfAllocator(Simulator simulator, Network network, Map<Flow, Double> flowToUpperLimit, Map<Flow, Double> flowToWeight) {
        super(simulator, network);

        // Upper limit
        if (flowToUpperLimit == null) {
            this.flowToUpperLimit = new ConstantMap<>(Double.MAX_VALUE);
        } else {
            this.flowToUpperLimit = flowToUpperLimit;
        }

        // Weighting
        if (flowToWeight == null) {
            this.flowToWeight = new ConstantMap<>(1.0);
        } else {
            this.flowToWeight = flowToWeight;
        }

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

        Set<Flow> temporarilyFixedFlows = new HashSet<>();
        while (linksWithUnfixedFlows.size() != 0) {

            Flow tightestFlow = null;
            double tightestAllocation = Double.MAX_VALUE;
            Link tightLink = null;
            boolean linkIsTight = false;

            // Go over all the links, and calculate the fair appropriation rate
            for (Link link : linksWithUnfixedFlows) {

                // Calculate total weight on the link
                double sumWeight = 0;
                for (Flow f : linkToUnfixedFlows.get(link)) {
                    if (!temporarilyFixedFlows.contains(f)) {
                        sumWeight += flowToWeight.get(f);
                    }
                }

                // For each flow, calculate its fair share and see if it is the tightest
                for (Flow f : linkToUnfixedFlows.get(link)) {
                    if (!temporarilyFixedFlows.contains(f)) {
                        double fairAllocation = Math.min(flowToUpperLimit.get(f), link.getRemainderCapacity() * flowToWeight.get(f) / sumWeight);

                        // If it is the tightest, set is as such
                        if (fairAllocation < tightestAllocation) {
                            tightestAllocation = fairAllocation;
                            tightLink = link;
                            tightestFlow = f;
                            linkIsTight = false;
                        }

                    }
                }

                // If it was this link on which the tightest flow is present, becomes tight because of the allocation
                // record it
                if (link == tightLink) {
                    if (Math.abs(tightestAllocation - link.getRemainderCapacity()) <= simulator.getFlowPrecision()) {
                        linkIsTight = true;
                    }
                }

            }

            // There was at least one flow present
            assert(tightestFlow != null);

            // If the tightest flow found is perpetually tight at that level, either (a) because the
            // link it was on becomes tight due to its allocation, or (b) because the flow was capped
            // by the upper limit
            if (linkIsTight || Math.abs(tightestAllocation - flowToUpperLimit.get(tightestFlow)) <= simulator.getFlowPrecision()) {

                // Remove all temporary flow fixes
                for (Flow f : temporarilyFixedFlows) {
                    simulator.allocateFlowBandwidth(f, 0);
                }
                temporarilyFixedFlows.clear();

                // Permanently fix the tightest flow
                simulator.allocateFlowBandwidth(tightestFlow, tightestAllocation);
                for (Link l : tightestFlow.getPath()) {
                    linkToUnfixedFlows.get(l).remove(tightestFlow);
                    if (linkToUnfixedFlows.get(l).size() == 0) {
                        linksWithUnfixedFlows.remove(l);
                    }
                }

            // If the tightest flow might not be permanently tight, only fix it temporarily
            } else {
                simulator.allocateFlowBandwidth(tightestFlow, tightestAllocation);
                temporarilyFixedFlows.add(tightestFlow);
            }

        }

    }

}
