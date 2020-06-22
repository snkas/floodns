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

import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.ConnectionLogger;
import ch.ethz.systems.floodns.core.Metadata;
import ch.ethz.systems.floodns.core.Simulator;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class FileConnectionLogger extends ConnectionLogger {

    private final Writer writerConnectionBandwidthFile;
    private final Writer writerConnectionInfoFile;

    public FileConnectionLogger(Simulator simulator, Connection connection, Writer writerConnectionBandwidthFile, Writer writerConnectionInfoFile) {
        super(simulator, connection);
        this.writerConnectionBandwidthFile = writerConnectionBandwidthFile;
        this.writerConnectionInfoFile = writerConnectionInfoFile;
    }

    @Override
    protected void saveFlowBandwidth(long start, long end, double bandwidth) {
        try {
            writerConnectionBandwidthFile.write(connectionId + "," + start + "," + end + "," + bandwidth + "\r\n");
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

    /**
     * Save the general (statistical) information of the connection.
     * Values are comma-separated, lines are new-line-separated.
     *
     * File format:
     * [flowId],[srcNodeId],[dstNodeId],[totalSize],[transmittedSize],[childFlows ; sep.], \
     * [startTime],[endTime],[duration],[avg. bandwidth],[T/F iff finished]
     *
     * For example:
     * Connection #7 (35 &rarr; 33) with child flows [8, 9, 13], starting at t=11, ending at t=130, avg. bandwidth of 8.512,
     * total size of 10000 of which 3888.4 have been sent
     *
     * ... gives in the log the following line:
     * 7,35,33,10000,3888.4,8;9;13,11,130,119,8.512,F,[extra info]
     */
    @Override
    protected void saveInfo(int connectionId, int srcNodeId, int dstNodeId, double totalSize, double transmittedSize,
                           List<Integer> pastAndPresentFlowIds, long startTime, long endTime, long duration,
                           double avgBandwidth, boolean finished, Metadata metadata) {
        try {
            writerConnectionInfoFile.write(
                    connectionId + "," +
                    srcNodeId + "," +
                    dstNodeId + "," +
                    totalSize + "," +
                    transmittedSize + "," +
                    flowIdsToString(pastAndPresentFlowIds) + "," +
                    startTime + "," +
                    endTime + "," +
                    duration + "," +
                    avgBandwidth + "," +
                    (finished ? "T" : "F") + "," +
                    (metadata == null ? "" : ((CsvPrintableMetadata) metadata).toCsvValidLabel()) +
                    "\r\n"
            );

        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

    /**
     * Convert a list of identifiers into a ;-separated string.
     *
     * @param flowIds   Flow identifiers (e.g. [3, 4, 5])
     *
     * @return ;-separated list string (e.g. "3;4;5")
     */
    private String flowIdsToString(List<Integer> flowIds) {
        StringBuilder s = new StringBuilder();
        for (Integer i : flowIds) {
            s.append(i);
            s.append(";");
        }
        return s.substring(0, s.length() > 1 ? s.length() - 1 : s.length());
    }

}
