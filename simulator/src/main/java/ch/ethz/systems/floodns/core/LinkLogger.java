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

public abstract class LinkLogger {

    // Simulator handle
    protected final Simulator simulator;

    // Properties
    protected final int linkId;
    private final int srcNodeId;
    private final int dstNodeId;
    private long inceptionTime;
    private double utilizationSum;
    private double numActiveFlowsSum;
    private boolean isInfoSavingEnabled;
    private boolean isUtilizationSavingEnabled;
    private boolean isNumActiveFlowsSavingEnabled;

    // Phenomenon log update helpers
    private LogUpdateHelper<Integer> numActiveFlowsHelper;
    private LogUpdateHelper<Double> utilizationHelper;

    protected LinkLogger(Simulator simulator, Link link) {
        this.simulator = simulator;

        // Properties
        this.linkId = link.getLinkId();
        this.srcNodeId = link.getFrom();
        this.dstNodeId = link.getTo();
        this.inceptionTime = simulator.getCurrentTime();
        this.utilizationSum = 0.0;
        this.numActiveFlowsSum = 0.0;
        this.isInfoSavingEnabled = true;
        this.isUtilizationSavingEnabled = true;
        this.isNumActiveFlowsSavingEnabled = true;

        // Log helpers
        this.utilizationHelper = new LogUpdateHelper<>(simulator, new LogUpdateHelper.LogSaveFunction<Double>() {
            @Override
            public void save(long start, long end, Double val) {
                updateAndSaveLinkUtilization(start, end, val);
            }
        }, new LogUpdateHelper.LogFlowCompareFunction(simulator.getFlowPrecision()));
        this.numActiveFlowsHelper = new LogUpdateHelper<>(simulator, new LogUpdateHelper.LogSaveFunction<Integer>() {
            @Override
            public void save(long start, long end, Integer val) {
                updateAndSaveNumActiveFlows(start, end, val);
            }
        }, new LogUpdateHelper.LogNumCompareFunction());

    }

    /**
     * Enable/disable logging the info of links.
     *
     * @param enabled   True iff logging enabled
     */
    public void setInfoSavingEnabled(boolean enabled) {
        this.isInfoSavingEnabled = enabled;
    }

    /**
     * Enable/disable logging the change of utilization.
     *
     * @param enabled   True iff logging enabled
     */
    public void setUtilizationSavingEnabled(boolean enabled) {
        this.isUtilizationSavingEnabled = enabled;
    }

    /**
     * Enable/disable logging the change of number of active flows.
     *
     * @param enabled   True iff logging enabled
     */
    public void setNumActiveFlowsSavingEnabled(boolean enabled) {
        this.isNumActiveFlowsSavingEnabled = enabled;
    }

    /**
     * Log a change of the link state of active number of flows.
     *
     * @param numActiveFlows    New number of active flows (>= 0)
     */
    final void logLinkNumActiveFlowsChange(int numActiveFlows) {
        numActiveFlowsHelper.update(numActiveFlows);
    }

    /**
     * Log a change of the link state of utilization.
     *
     * @param utilization       New utilization of link (0 <= utilization <= 1)
     */
    final void logLinkUtilizationChange(double utilization) {
        utilizationHelper.update(utilization);
    }

    /**
     * Final flush of logged state.
     *
     * @param metadata     Metadata
     */
    final void finalFlush(Metadata metadata) {
        numActiveFlowsHelper.finish();
        utilizationHelper.finish();
        if (isInfoSavingEnabled) {
            long timeDelta = simulator.getCurrentTime() - inceptionTime;
            this.saveInfo(
                    linkId,
                    srcNodeId,
                    dstNodeId,
                    inceptionTime,
                    simulator.getCurrentTime(),
                    timeDelta,
                    timeDelta == 0 ? 0 : utilizationSum / (double) timeDelta,
                    timeDelta == 0 ? 0 : numActiveFlowsSum / (double) timeDelta,
                    metadata
            );
        }
    }

    /**
     * Update the link utilization sum and save its change.
     *
     * @param start         Time moment at which this value start to be valid
     * @param end           Time moment at which this value is last valid
     * @param utilization   Utilization of link (0 <= utilization <= 1)
     */
    private void updateAndSaveLinkUtilization(long start, long end, double utilization) {
        this.utilizationSum += (end - start) * utilization;
        if (isUtilizationSavingEnabled) {
            saveLinkUtilization(start, end, utilization);
        }
    }

    /**
     * Update the number of active flows change sum and save its change.
     *
     * @param start            Time moment at which this value start to be valid
     * @param end              Time moment at which this value is last valid
     * @param numActiveFlows   Number of active flows (>= 0)
     */
    private void updateAndSaveNumActiveFlows(long start, long end, int numActiveFlows) {
        this.numActiveFlowsSum += (end - start) * numActiveFlows;
        if (isNumActiveFlowsSavingEnabled) {
            saveNumActiveFlows(start, end, numActiveFlows);
        }
    }

    /**
     * Save the link utilization in some medium (guaranteed).
     *
     * @param start         Time moment at which this value start to be valid
     * @param end           Time moment at which this value is last valid
     * @param utilization   Utilization of link (0 &lt;= utilization &lt;= 1)
     */
    protected abstract void saveLinkUtilization(long start, long end, double utilization);

    /**
     * Save the number of active flows in some medium (guaranteed).
     *
     * @param start            Time moment at which this value start to be valid
     * @param end              Time moment at which this value is last valid
     * @param numActiveFlows   Number of active flows (&gt;= 0)
     */
    protected abstract void saveNumActiveFlows(long start, long end, int numActiveFlows);

    /**
     * Save the final link information.
     *
     * @param linkId                Link identifier
     * @param srcNodeId             Source node identifier
     * @param dstNodeId             Destination node identifier
     * @param startTime             Start time
     * @param endTime               End time
     * @param duration              Duration (= end - start)
     * @param avgUtilization        Average utilization
     * @param avgNumActiveFlows     Average number of active flows
     * @param metadata             Metadata (can be null)
     */
    protected abstract void saveInfo(int linkId, int srcNodeId, int dstNodeId, long startTime, long endTime,
                                     long duration, double avgUtilization, double avgNumActiveFlows,
                                     Metadata metadata);

}
