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

public abstract class NodeLogger {

    // Simulator handle
    protected final Simulator simulator;

    // Properties
    protected final int nodeId;
    private double numActiveFlowsSum;
    private boolean isInfoSavingEnabled;
    private boolean isNumActiveFlowsSavingEnabled;

    // Log helper
    private LogUpdateHelper<Integer> numActiveFlowsHelper;

    protected NodeLogger(Simulator simulator, Node node) {
        this.simulator = simulator;

        // Property
        this.nodeId = node.getNodeId();
        this.numActiveFlowsSum = 0.0;
        this.isInfoSavingEnabled = true;
        this.isNumActiveFlowsSavingEnabled = true;

        // Log helper
        this.numActiveFlowsHelper = new LogUpdateHelper<>(simulator, new LogUpdateHelper.LogSaveFunction<Integer>() {
            @Override
            public void save(long start, long end, Integer val) {
                updateAndSaveNumActiveFlows(start, end, val);
            }
        }, new LogUpdateHelper.LogNumCompareFunction());

    }

    /**
     * Enable/disable logging the info of nodes.
     *
     * @param enabled   True iff logging enabled
     */
    public void setInfoSavingEnabled(boolean enabled) {
        this.isInfoSavingEnabled = enabled;
    }

    /**
     * Enable/disable logging the change of number of active flows.
     *
     * @param enabled   True iff logging enabled
     */
    public void setNumActiveFlowsSavingEnabled(boolean enabled) {
        isNumActiveFlowsSavingEnabled = enabled;
    }

    /**
     * Log a change of the connection state.
     *
     * @param numActiveFlows       Number of new active flows (>= 0)
     */
    final void logNodeStateChange(int numActiveFlows) {
        numActiveFlowsHelper.update(numActiveFlows);
    }

    /**
     * Final flush of logged state.
     *
     * @param metadata     Metadata
     */
    final void finalFlush(Metadata metadata) {
        numActiveFlowsHelper.finish();
        if (isInfoSavingEnabled) {
            this.saveInfo(nodeId, numActiveFlowsSum / simulator.getCurrentTime(), metadata);
        }
    }

    /**
     * Update the number of active flows and save its change.
     *
     * @param start            Time moment at which this value start to be valid
     * @param end              Time moment at which this value is last valid
     * @param numActiveFlows   Number of active flows (&gt;= 0)
     */
    private void updateAndSaveNumActiveFlows(long start, long end, int numActiveFlows) {
        numActiveFlowsSum += (end - start) * numActiveFlows;
        if (isNumActiveFlowsSavingEnabled) {
            saveNumActiveFlows(start, end, numActiveFlows);
        }
    }

    /**
     * Save the number of active flows in some medium (guaranteed).
     *
     * @param start            Time moment at which this value start to be valid
     * @param end              Time moment at which this value is last valid
     * @param numActiveFlows   Number of active flows (&gt;= 0)
     */
    protected abstract void saveNumActiveFlows(long start, long end, int numActiveFlows);

    /**
     * Save the final node information.
     *
     * @param nodeId                Node identifier
     * @param avgNumActiveFlows     Average number of active flows
     * @param metadata             Metadata (can be null)
     */
    protected abstract void saveInfo(int nodeId, double avgNumActiveFlows, Metadata metadata);

}
