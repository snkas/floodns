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

import java.util.List;

public abstract class ConnectionLogger {

    // Simulator handle
    protected final Simulator simulator;

    // Properties
    protected final int connectionId;
    private final Connection connection;
    private long inceptionTime;
    private double bandwidthSum;
    private boolean isInfoSavingEnabled;
    private boolean isBandwidthSavingEnabled;

    // Log helpers
    private LogUpdateHelper<Double> connectionBandwidthHelper;

    protected ConnectionLogger(Simulator simulator, Connection connection) {
        this.simulator = simulator;

        // Properties
        this.connectionId = connection.getConnectionId();
        this.connection = connection;
        this.inceptionTime = Long.MAX_VALUE;
        this.bandwidthSum = 0.0;
        this.isInfoSavingEnabled = true;
        this.isBandwidthSavingEnabled = true;

        // Log helpers
        this.connectionBandwidthHelper = new LogUpdateHelper<>(simulator, new LogUpdateHelper.LogSaveFunction<Double>() {
            @Override
            public void save(long start, long end, Double val) {
                updateAndSaveFlowBandwidth(start, end, val);
            }
        }, new LogUpdateHelper.LogFlowCompareFunction(simulator.getFlowPrecision()));

    }

    /**
     * Enable/disable logging the info of connections.
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
     * Log a change of the connection state.
     *
     * @param bandwidth       New current bandwidth of flow (>= 0)
     */
    final void logConnectionStateChange(double bandwidth) {
        connectionBandwidthHelper.update(bandwidth);
    }

    /**
     * Final flush of logged state.
     *
     * @param metadata     Metadata
     */
    final void finalFlush(Metadata metadata) {
        connectionBandwidthHelper.finish();
        if (isInfoSavingEnabled) {
            long timeDelta = simulator.getCurrentTime() - inceptionTime;
            this.saveInfo(
                    connectionId,
                    connection.getSrcNodeId(),
                    connection.getDstNodeId(),
                    connection.getTotalSize(),
                    bandwidthSum,
                    connection.getPastAndPresentFlowIds(),
                    inceptionTime,
                    simulator.getCurrentTime(),
                    timeDelta,
                    bandwidthSum / timeDelta,
                    connection.getTotalSize() - bandwidthSum <= simulator.getFlowPrecision(),
                    metadata
            );
        }
    }

    /**
     * Save the connection bandwidth in some medium (guaranteed).
     *
     * @param start       Time moment at which this value start to be valid
     * @param end         Time moment at which this value is last valid
     * @param bandwidth   Bandwidth of flow (>= 0)
     */
    private void updateAndSaveFlowBandwidth(long start, long end, double bandwidth) {
        this.bandwidthSum += (end - start) * bandwidth;
        if (start < inceptionTime) {
            inceptionTime = start;
        }
        if (isBandwidthSavingEnabled) {
            saveFlowBandwidth(start, end, bandwidth);
        }
    }

    /**
     * Save the connection bandwidth in some medium (guaranteed).
     *
     * @param start       Time moment at which this value start to be valid
     * @param end         Time moment at which this value is last valid
     * @param bandwidth   Bandwidth of flow (&gt;= 0)
     */
    protected abstract void saveFlowBandwidth(long start, long end, double bandwidth);

    /**
     * Save the final connection info.
     *
     * @param connectionId              Connection identifier
     * @param srcNodeId                 Source node identifier
     * @param dstNodeId                 Destination node identifier
     * @param totalSize                 Total size
     * @param transmittedSize           Transmitted size
     * @param pastAndPresentFlowIds     Past and present flow identifiers
     * @param startTime                 Starting time
     * @param endTime                   Ending time
     * @param duration                  Duration (= start - end)
     * @param avgBandwidth              Average bandwidth
     * @param finished                  True iff finished (transmitted size &gt;= total size)
     * @param metadata                 Metadata (can be null)
     */
    protected abstract void saveInfo(int connectionId, int srcNodeId, int dstNodeId, double totalSize,
                                     double transmittedSize, List<Integer> pastAndPresentFlowIds, long startTime,
                                     long endTime, long duration, double avgBandwidth, boolean finished,
                                     Metadata metadata);

}
