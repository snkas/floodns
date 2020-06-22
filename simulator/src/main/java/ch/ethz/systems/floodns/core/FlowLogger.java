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

public abstract class FlowLogger {

    // Simulator handle
    protected final Simulator simulator;

    // Properties
    protected final int flowId;
    private final Flow flow;
    private final long inceptionTime;
    private double bandwidthSum;
    private boolean isInfoSavingEnabled;
    private boolean isBandwidthSavingEnabled;

    // Change track variables
    private LogUpdateHelper<Double> flowBandwidthHelper;

    protected FlowLogger(Simulator simulator, Flow flow) {
        this.simulator = simulator;

        // Properties
        this.flowId = flow.getFlowId();
        this.flow = flow;
        this.inceptionTime = simulator.getCurrentTime();
        this.bandwidthSum = 0.0;
        this.isInfoSavingEnabled = true;
        this.isBandwidthSavingEnabled = true;

        // Log helper
        this.flowBandwidthHelper = new LogUpdateHelper<>(simulator, new LogUpdateHelper.LogSaveFunction<Double>() {
            @Override
            public void save(long start, long end, Double val) {
                updateAndSaveFlowBandwidth(start, end, val);
            }
        }, new LogUpdateHelper.LogFlowCompareFunction(simulator.getFlowPrecision()));

    }

    /**
     * Enable/disable logging the info of flows.
     *
     * @param enabled   True iff logging enabled
     */
    public void setInfoSavingEnabled(boolean enabled) {
        this.isInfoSavingEnabled = enabled;
    }

    /**
     * Enable/disable logging the change of bandwidth.
     *
     * @param enabled   True iff logging enabled
     */
    public void setBandwidthSavingEnabled(boolean enabled) {
        isBandwidthSavingEnabled = enabled;
    }

    /**
     * Log a change of the flow state.
     *
     * @param bandwidth       New current bandwidth of flow (>= 0)
     */
    final void logFlowStateChange(double bandwidth) {
        flowBandwidthHelper.update(bandwidth);
    }

    /**
     * Final flush of logged state.
     *
     * @param metadata     Metadata
     */
    final void finalFlush(Metadata metadata) {
        flowBandwidthHelper.finish();
        if (isInfoSavingEnabled) {
            long deltaTime = simulator.getCurrentTime() - inceptionTime;
            this.saveInfo(
                    flowId,
                    flow.getSrcNodeId(),
                    flow.getDstNodeId(),
                    flow.getPath(),
                    inceptionTime,
                    simulator.getCurrentTime(),
                    deltaTime,
                    bandwidthSum,
                    deltaTime == 0 ? 0 : (bandwidthSum / deltaTime),
                    metadata
            );
        }
    }

    /**
     * Update the flow bandwidth and save its change.
     *
     * @param start            Time moment at which this value start to be valid
     * @param end              Time moment at which this value is last valid
     * @param bandwidth   Bandwidth of flow (>= 0)
     */
    private void updateAndSaveFlowBandwidth(long start, long end, double bandwidth) {
        this.bandwidthSum += (end - start) * bandwidth;
        if (isBandwidthSavingEnabled) {
            saveFlowBandwidth(start, end, bandwidth);
        }
    }

    /**
     * Save the flow bandwidth in some medium (guaranteed).
     *
     * @param start            Time moment at which this value start to be valid
     * @param end              Time moment at which this value is last valid
     * @param bandwidth         Bandwidth of flow (&gt;= 0)
     */
    protected abstract void saveFlowBandwidth(long start, long end, double bandwidth);

    /**
     * Save the final flow info.
     *
     * @param flowId                Flow identifier
     * @param srcNodeId             Source node identifier
     * @param dstNodeId             Destination node identifier
     * @param path                  Path
     * @param startTime             Start time
     * @param endTime               End time
     * @param duration              Duration (= end - start)
     * @param bandwidthSum          Total bandwidth transferred
     * @param avgBandwidth          Average bandwidth
     * @param metadata             Metadata (can be null)
     */
    protected abstract void saveInfo(int flowId, int srcNodeId, int dstNodeId, AcyclicPath path, long startTime,
                                     long endTime, long duration, double bandwidthSum, double avgBandwidth,
                                     Metadata metadata);

}
