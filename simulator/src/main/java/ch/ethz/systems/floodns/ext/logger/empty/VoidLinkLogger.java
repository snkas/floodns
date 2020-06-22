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

package ch.ethz.systems.floodns.ext.logger.empty;

import ch.ethz.systems.floodns.core.Link;
import ch.ethz.systems.floodns.core.LinkLogger;
import ch.ethz.systems.floodns.core.Metadata;
import ch.ethz.systems.floodns.core.Simulator;

public class VoidLinkLogger extends LinkLogger {

    public VoidLinkLogger(Simulator simulator, Link link) {
        super(simulator, link);
    }

    @Override
    protected void saveLinkUtilization(long start, long end, double utilization) {
        // Intentionally does nothing
    }

    @Override
    protected void saveNumActiveFlows(long start, long end, int numActiveFlows) {
        // Intentionally does nothing
    }

    @Override
    protected void saveInfo(int linkId, int srcNodeId, int dstNodeId, long startTime, long endTime, long duration,
                           double avgUtilization, double avgNumActiveFlows, Metadata metadata) {
        // Intentionally does nothing
    }

}
