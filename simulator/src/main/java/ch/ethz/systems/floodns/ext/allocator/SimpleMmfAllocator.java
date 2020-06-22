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

import java.util.*;

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
 * Algorithm is based on that described in section II.B in (the authors have no affiliation with the implementation):
 * Nace, D., Doan, N.L., Gourdin, E. and Liau, B., 2006.
 * Computing optimal max-min fair resource allocation for elastic flows.
 * IEEE/ACM Transactions on Networking (TON), 14(6), pp.1272-1281.
 */
public class SimpleMmfAllocator extends Allocator {

    public SimpleMmfAllocator(Simulator simulator, Network network) {
        super(simulator, network);
    }

    /**
     * Allocate each flow and link its entitled bandwidth by playing fair
     * according to the Max-Min Fair Rate Allocation for a network with a
     * given routing.
     *
     * Runtime is O(|E|^2 + |E|*|F|).
     */
    @Override
    public void perform() {

        // Reset all flow bandwidth to zero
        for (Flow f : network.getActiveFlows()) {
            simulator.allocateFlowBandwidth(f, 0);
        }

        // Mapping of the tightness to the links experiencing that tightness
        TreeMap<Double, Set<Link>> tightnessToLink = new TreeMap<>();

        // Mapping of the link to the number of fixed flows on it
        Map<Link, Integer> linkToNumFixedFlows = new HashMap<>();

        // Save how much each link can allocate to each flow flowing through it
        // such that we can easily identify the current bottleneck, the TIGHTEST LINK.
        for (Link link : network.getFlowActiveLinks()) {

            // At the beginning, none are fixed
            linkToNumFixedFlows.put(link, 0);

            // Insert into ordered mapping of tightness of links
            double tightness = calculateTightness(link, linkToNumFixedFlows);
            Set<Link> existing = tightnessToLink.get(tightness);
            if (existing != null) {
                existing.add(link);
            } else {
                Set<Link> newSet = new HashSet<>();
                newSet.add(link);
                tightnessToLink.put(tightness, newSet);
            }

        }

        // If there are no active links, there is no need to do any allocation
        if (tightnessToLink.size() == 0) {
            return;
        }

        // Each time find the link which is the current bottleneck
        double lowestKey = tightnessToLink.firstKey();
        Set<Flow> flowsFixed = new HashSet<>();
        while (true) {

            // Retrieve the tightest link
            Set<Link> linkSet = tightnessToLink.get(lowestKey);
            Link lowestLink = linkSet.iterator().next();

            // Set of links that were affected by the flow being set
            Set<Link> affectedLinks = new HashSet<>();

            Map<Link, Double> linkToPreviousTightness = new HashMap<>();

            // Note for each link how much the flow is fixed to
            for (Flow f : lowestLink.getActiveFlows()) {

                // An active flow which is already fixed does not matter
                if (!flowsFixed.contains(f)) {
                    flowsFixed.add(f);

                    // Save the previous allocation of the link so that it can be removed later from the mapping
                    for (Link link : f.getPath()) {
                        if (!affectedLinks.contains(link)) {
                            linkToPreviousTightness.put(link, calculateTightness(link, linkToNumFixedFlows));
                            affectedLinks.add(link);
                        }
                        linkToNumFixedFlows.put(link, linkToNumFixedFlows.get(link) + 1);
                    }

                    // Finalize the flow allocation
                    simulator.allocateFlowBandwidth(f, lowestKey);

                }

            }

            // Remap each affected link
            for (Link link : affectedLinks) {

                // Remove the link from the existing tightness mapping
                double prevTightness = linkToPreviousTightness.get(link);
                Set<Link> temp = tightnessToLink.get(prevTightness);
                temp.remove(link);
                if (temp.size() == 0) {
                    tightnessToLink.remove(prevTightness);
                }

                // Determine how much the link can now allocate
                double allocatable = calculateTightness(link, linkToNumFixedFlows);
                Set<Link> linkSetToPlaceIn = tightnessToLink.get(allocatable);
                if (linkSetToPlaceIn != null) {
                    linkSetToPlaceIn.add(link);
                } else {
                    Set<Link> newSet = new HashSet<>();
                    newSet.add(link);
                    tightnessToLink.put(allocatable, newSet);
                }

            }

            // Next most bottlenecked link
            lowestKey = tightnessToLink.firstKey();

            // If all of the flows have already a fixed bandwidth
            // the algorithm has terminated (all flow-active links
            // must have been assigned everything as well)
            if (flowsFixed.size() == network.getActiveFlows().size()) {
                break;
            }

        }

    }

    /**
     * Calculate how much the fair allocation would give, as such the uniform division of the remainder capacity.
     *
     * @param link                  Tightest link
     * @param numFlowsFixedOnLink   Link-to-number-fixed-flows mapping
     *
     * @return  Fair share of the remaining link bandwidth
     */
    private double calculateTightness(Link link, Map<Link, Integer> numFlowsFixedOnLink) {
        if (numFlowsFixedOnLink.get(link) == link.getActiveFlowIds().size()) {
            return Double.MAX_VALUE;
        } else {
            return link.getRemainderCapacity() / (link.getActiveFlowIds().size() - numFlowsFixedOnLink.get(link));
        }
    }

}
