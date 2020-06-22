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

import ch.ethz.systems.floodns.core.Metadata;
import ch.ethz.systems.floodns.core.Node;
import ch.ethz.systems.floodns.core.NodeLogger;
import ch.ethz.systems.floodns.core.Simulator;

import java.io.IOException;
import java.io.Writer;

public class FileNodeLogger extends NodeLogger {

    private final Writer writerNodeNumActiveFlowsFile;
    private final Writer writerNodeInfoFile;

    public FileNodeLogger(Simulator simulator, Node node, Writer writerNodeNumActiveFlowsFile, Writer writerNodeInfoFile) {
        super(simulator, node);
        this.writerNodeNumActiveFlowsFile = writerNodeNumActiveFlowsFile;
        this.writerNodeInfoFile = writerNodeInfoFile;
    }

    @Override
    protected void saveNumActiveFlows(long start, long end, int numActiveFlows) {
        try {
            writerNodeNumActiveFlowsFile.write(nodeId + "," + start + "," + end + "," + numActiveFlows + "\r\n");
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

    /**
     * Save the general (statistical) information of the node.
     * Values are comma-separated, lines are new-line-separated.
     *
     * File format:
     * [nodeId],[avg.act.fl. 0.0+]
     *
     * For example:
     * Node #223 with average active flows of 3.1415926535989
     *
     * ... gives in the log the following line:
     * 223,3.1415926535989,[extra info]
     */
    @Override
    protected void saveInfo(int nodeId, double avgNumActiveFlows, Metadata metadata) {
        try {
            writerNodeInfoFile.write(
                    nodeId + "," +
                    avgNumActiveFlows + "," +
                    (metadata == null ? "" : ((CsvPrintableMetadata) metadata).toCsvValidLabel()) +
                    "\r\n"
            );
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

}
