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

import ch.ethz.systems.floodns.core.Link;
import ch.ethz.systems.floodns.core.LinkLogger;
import ch.ethz.systems.floodns.core.Metadata;
import ch.ethz.systems.floodns.core.Simulator;

import java.io.IOException;
import java.io.Writer;

public class FileLinkLogger extends LinkLogger {

    // Writers to logs
    private final Writer writerLinkUtilizationFile;
    private final Writer writerLinkNumActiveFlowsFile;
    private final Writer writerLinkInfoFile;

    public FileLinkLogger(Simulator simulator, Link link, Writer writerLinkUtilizationFile, Writer writerLinkNumActiveFlowsFile, Writer writerLinkInfoFile) {
        super(simulator, link);

        // Writers
        this.writerLinkUtilizationFile = writerLinkUtilizationFile;
        this.writerLinkNumActiveFlowsFile = writerLinkNumActiveFlowsFile;
        this.writerLinkInfoFile = writerLinkInfoFile;

    }

    @Override
    protected void saveLinkUtilization(long start, long end, double utilization) {
        try {
            writerLinkUtilizationFile.write(linkId + "," + start + "," + end + "," + utilization + "\r\n");
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

    @Override
    protected void saveNumActiveFlows(long start, long end, int numActiveFlows) {
        try {
            writerLinkNumActiveFlowsFile.write(linkId + "," + start + "," + end + "," + numActiveFlows + "\r\n");
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

    /**
     * Save the general (statistical) information of the link.
     * Values are comma-separated, lines are new-line-separated.
     *
     * File format:
     * [linkId],[srcNodeId],[dstNodeId],[startTime],[endTime],[durationExistent],[avg.util 0.0-1.0],[avg.act.fl. 0.0+]
     *
     * For example:
     * Link #242 (34 &rarr; 55), created at 300, removed at 10000, avg. util. of 30%, avg. act. fl. 1.3
     *
     * ... gives in the log the following line:
     * 242,34,55,300,10000,99700,0.3,1.3,[extra info]
     */
    @Override
    protected void saveInfo(int linkId, int srcNodeId, int dstNodeId, long startTime, long endTime, long duration,
                           double avgUtilization, double avgNumActiveFlows, Metadata metadata) {

        try {
            writerLinkInfoFile.write(
                    linkId + "," +
                    srcNodeId + "," +
                    dstNodeId + "," +
                    startTime + "," +
                    endTime + "," +
                    duration + "," +
                    avgUtilization + "," +
                    avgNumActiveFlows + "," +
                    (metadata == null ? "" : ((CsvPrintableMetadata) metadata).toCsvValidLabel()) +
                    "\r\n"
            );
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }

    }

}
