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

/**
 * Helper that keeps track of updates to an observed phenomenon
 * in the logs, such that it saves only the changes instead
 * of every update the observed phenomenon gives.
 *
 * The helper works by keeping track of two intervals: alpha and beta.
 * [  alpha  ][  beta  ][ future ]
 *
 * At first log, alpha_start and alpha_value. At the second log,
 * we know alpha_end, beta_start and beta_value. At the third log, we know
 * beta_end, future_start and future_value. If the alpha_value and beta_value
 * are the same alpha and beta are merged into alpha, and the future interval
 * becomes the beta interval. If the value is different, the alpha interval is
 * persistently logged because we know we can't merge alpha and beta due to
 * different values. The beta interval becomes the alpha interval,
 * and the future interval becomes the beta interval.
 *
 * @param <T>   Type of the observed phenomenon
 */
public class LogUpdateHelper<T> {

    // Simulator handle
    private final Simulator simulator;

    // Method to save confirmed intervals
    private LogSaveFunction<T> saveFunction;
    private LogCompareFunction<T> compareFunction;

    // Last recorded value of the phenomenon
    private T lastUpdate;

    // Value of the current interval
    private long lastIntervalCheckTime;
    private long intervalAlphaLeftTime;
    private long intervalBetaLeftTime;
    private T intervalAlphaValue;

    // Finish check
    private boolean isFinished;

    /**
     * Log update helper constructor.
     *
     * @param simulator         Simulator instance
     * @param saveFunction      Saving method of intervals and their values
     * @param compareFunction   Comparison function for interval values
     */
    LogUpdateHelper(Simulator simulator, LogSaveFunction<T> saveFunction, LogCompareFunction<T> compareFunction) {
        this.simulator = simulator;
        this.saveFunction = saveFunction;
        this.compareFunction = compareFunction;
        this.lastIntervalCheckTime = -1;
        this.intervalAlphaLeftTime = -1;
        this.intervalBetaLeftTime = -1;
        this.lastUpdate = null;
        this.intervalAlphaValue = null;
        this.isFinished = false;
    }
    
    /**
     * Update the value in the log iff the value is a new value
     * and that some time has passed. Many updates can happen
     * during the same time unit, but only the last one will
     * be persisted.
     *
     * @param val   Value of observed phenomenon
     */
    public void update(T val) {

        if (isFinished) {
            return;
        }

        // Check if there is a new interval once per time moment
        if (simulator.getCurrentTime() > lastIntervalCheckTime) {

            // If no first interval is yet started, start interval alpha
            if (intervalAlphaLeftTime == -1) {
                intervalAlphaLeftTime = simulator.getCurrentTime();
            } else {

                // If no second interval is yet started, start interval beta
                if (intervalBetaLeftTime == -1) {
                    intervalBetaLeftTime = simulator.getCurrentTime();
                    intervalAlphaValue = lastUpdate;

                // If both interval alpha and beta has been started, the value of interval beta
                // is determined, as such interval alpha can be pushed to the log if it is different
                } else {

                    // The value of interval beta has now been determined
                    T intervalBetaValue = lastUpdate;

                    // If interval alpha and beta have the same value, log push, but merge
                    if (compareFunction.areEqual(intervalAlphaValue, intervalBetaValue)) {
                        intervalBetaLeftTime = simulator.getCurrentTime();

                    } else {

                        // Save the alpha interval
                        saveFunction.save(intervalAlphaLeftTime, intervalBetaLeftTime, intervalAlphaValue);

                        // And make beta interval become alpha
                        intervalAlphaLeftTime = intervalBetaLeftTime;
                        intervalAlphaValue = intervalBetaValue;
                        intervalBetaLeftTime = simulator.getCurrentTime();

                    }

                }

            }

            // Check once per time moment for new intervals
            lastIntervalCheckTime = simulator.getCurrentTime();

        }

        // Continuously update it
        lastUpdate = val;

    }

    /**
     * Finalize the log and save the last interval to the log medium.
     */
    public void finish() {

        // Only possible to finish logging once
        assert(!isFinished);
        isFinished = true;

        // If no value has been registered at all, even though the update logger helper
        // was created, it is an illegal state
        assert(intervalAlphaLeftTime != -1 || lastUpdate != null || lastIntervalCheckTime != -1);

        // If there is no beta interval, only save the alpha interval
        if (intervalBetaLeftTime == -1) {
            if (intervalAlphaLeftTime != simulator.getCurrentTime()) {
                saveFunction.save(intervalAlphaLeftTime, simulator.getCurrentTime(), lastUpdate);
            }

        // If there is a beta interval, save the (guaranteed non-empty) alpha interval and the beta interval
        } else {

            // If the last two intervals can be merged
            if (compareFunction.areEqual(intervalAlphaValue, lastUpdate)) {
                saveFunction.save(intervalAlphaLeftTime, simulator.getCurrentTime(), intervalAlphaValue);

            // If the last two intervals have different values, save separately
            } else {
                saveFunction.save(intervalAlphaLeftTime, intervalBetaLeftTime, intervalAlphaValue);
                if (intervalBetaLeftTime != simulator.getCurrentTime()) {
                    saveFunction.save(intervalBetaLeftTime, simulator.getCurrentTime(), lastUpdate);
                }
            }

        }

    }

    /**
     * Function that guarantees that the interval [start, end] with value val
     * is stored in some medium (e.g. file, in-memory, ...).
     *
     * @param <V>   Interval value type
     */
    public interface LogSaveFunction<V> {

        /**
         * Guaranteed save of interval with value.
         *
         * @param start     Left part of interval (inclusive)
         * @param end       Right part of interval (exclusive)
         * @param val       Value of the observed phenomenon during the interval
         */
        void save(long start, long end, V val);

    }

    /**
     * Function for comparison of interval values.
     *
     * @param <V>   Interval value type
     */
    public interface LogCompareFunction<V> {

        /**
         * Compare two values is they are significantly equal.
         *
         * @param val1      First value
         * @param val2      Second value
         *
         * @return True iff val1 and val2 are significantly equal
         */
        boolean areEqual(V val1, V val2);

    }

    /**
     * Function for comparison of interval integer values.
     */
    public static class LogNumCompareFunction implements LogUpdateHelper.LogCompareFunction<Integer> {

        @Override
        public boolean areEqual(Integer val1, Integer val2) {
            return val1.equals(val2);
        }

    }

    /**
     * Function for comparison of interval flow values (expressed in doubles) within flow precision.
     */
    public static class LogFlowCompareFunction implements LogUpdateHelper.LogCompareFunction<Double> {

        private final double precision;

        public LogFlowCompareFunction(double precision) {
            this.precision = precision;
        }

        @Override
        public boolean areEqual(Double val1, Double val2) {
            return Math.abs(val1 - val2) < precision;
        }

    }

}
