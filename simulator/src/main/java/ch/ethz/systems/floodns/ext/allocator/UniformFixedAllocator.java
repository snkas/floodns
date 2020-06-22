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

import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Flow;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;

import java.util.Collection;

/**
 * The Uniform Fixed (UF) allocator simply allocates a fixed
 * uniform bandwidth to every flow as long as there is sufficient capacity.
 */
public class UniformFixedAllocator extends Allocator {

    private final double uniformFlowBandwidth;

    public UniformFixedAllocator(Simulator simulator, Network network, double uniformFlowBandwidth) {
        super(simulator, network);
        this.uniformFlowBandwidth = uniformFlowBandwidth;
    }

    @Override
    public void perform() {

        // Reset all flow bandwidth to zero such that the full link capacity is free
        // to be set for the flows
        for (Flow f : network.getActiveFlows()) {
            simulator.allocateFlowBandwidth(f, 0);
        }

        // Uniformly set the flow bandwidth
        Collection<Connection> activeConnections = simulator.getActiveConnections();
        for (Connection conn : activeConnections) {
            for (Flow f : conn.getActiveFlows()) {

                // Note: this can fail if there is not enough capacity available on the path of the flow
                simulator.allocateFlowBandwidth(f, uniformFlowBandwidth);

            }
        }

    }

}
