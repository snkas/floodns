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

package ch.ethz.systems.floodns.ext.logger.file;

import ch.ethz.systems.floodns.core.*;

import java.io.IOException;
import java.io.Writer;

public class FileFlowLogger extends FlowLogger {

    private final Writer writerFlowBandwidthFile;
    private final Writer writerFlowInfoFile;

    public FileFlowLogger(Simulator simulator, Flow flow, Writer writerFlowBandwidthFile, Writer writerFlowInfoFile) {
        super(simulator, flow);
        this.writerFlowBandwidthFile = writerFlowBandwidthFile;
        this.writerFlowInfoFile = writerFlowInfoFile;
    }

    @Override
    protected void saveFlowBandwidth(long start, long end, double bandwidth) {
        try {
            writerFlowBandwidthFile.write(flowId + "," + start + "," + end + "," + bandwidth + "\r\n");
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

    /**
     * Save the general (statistical) information of the flow.
     * Values are comma-separated, lines are new-line-separated.
     *
     * File format (9 columns):
     * [flowId],[srcNodeId],[dstNodeId],[path (-[#linkId]-&gt;)-separated],[startTime],[endTime],[duration],bandwidthSum,avgBandwidth
     *
     * For example:
     * Flow #22 (35 &rarr; 33) over path 35-4-99-33, starting at 55, ending at 900
     * with link identifiers of the path 11, 22 and 77 having run for 5 time
     * units with an avg. bandwidth of exactly 30/time unit, and having transmitted
     * a total sum of 150 flow units over time.
     *
     * ... gives in the log the following line:
     * 22,35,33,35-[11]-&gt;4-[22]-&gt;99-[77]-&gt;33,55,900,845,150,30,[extra info]
     */
    @Override
    protected void saveInfo(int flowId, int srcNodeId, int dstNodeId, AcyclicPath path, long startTime, long endTime,
                  long duration, double bandwidthSum, double avgBandwidth, Metadata metadata) {
        try {
            writerFlowInfoFile.write(
                    flowId + "," +
                    srcNodeId + "," +
                    dstNodeId + "," +
                    pathToString(path) + "," +
                    startTime + "," +
                    endTime + "," +
                    duration + "," +
                    bandwidthSum + "," +
                    avgBandwidth + "," +
                    (metadata == null ? "" : ((CsvPrintableMetadata) metadata).toCsvValidLabel()) +
                    "\r\n"
            );
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

    /**
     * Convert a flow path to a human-readable string.
     *
     * @param path  Path of the flow
     *
     * @return Human-readable path
     */
    private String pathToString(AcyclicPath path) {
        StringBuilder s = new StringBuilder();
        s.append(path.get(0).getFrom());
        for (Link link : path) {
            s.append("-[");
            s.append(link.getLinkId());
            s.append("]->");
            s.append(link.getTo());
        }
        return s.toString();
    }

}
