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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class FileLoggerFactory extends LoggerFactory {

    // Class logger
    private static final Logger logger = LogManager.getLogger(FileLoggerFactory.class);

    // Log folder path (e.g. "C:/folder/temp")
    private final String logFolderPath;

    // Log file writers
    private final BufferedWriter writerLinkInfoFile;
    private final BufferedWriter writerLinkUtilizationFile;
    private final BufferedWriter writerLinkNumActiveFlowsFile;
    private final BufferedWriter writerNodeInfoFile;
    private final BufferedWriter writerNodeNumActiveFlowsFile;
    private final BufferedWriter writerFlowBandwidthFile;
    private final BufferedWriter writerConnectionBandwidthFile;
    private final BufferedWriter writerFlowInfoFile;
    private final BufferedWriter writerConnectionInfoFile;

    // Log file names
    public final static String FILE_NAME_LINK_INFO = "link_info.csv";
    public final static String FILE_NAME_LINK_UTILIZATION = "link_utilization.csv";
    public final static String FILE_NAME_LINK_NUM_ACTIVE_FLOWS = "link_num_active_flows.csv";
    public final static String FILE_NAME_NODE_INFO = "node_info.csv";
    public final static String FILE_NAME_NODE_NUM_ACTIVE_FLOWS = "node_num_active_flows.csv";
    public final static String FILE_NAME_FLOW_INFO = "flow_info.csv";
    public final static String FILE_NAME_FLOW_BANDWIDTH = "flow_bandwidth.csv";
    public final static String FILE_NAME_CONNECTION_INFO = "connection_info.csv";
    public final static String FILE_NAME_CONNECTION_BANDWIDTH = "connection_bandwidth.csv";
    public final static String FILE_NAME_RUN_FINISHED = "run_finished.txt";

    /**
     * Constructor for file logger factory.
     * Automatically opens all the writing streams.
     *
     * Closed once finished using {@link #close()} by the simulator.
     *
     * @param simulator         Simulator instance
     * @param logFolderPath     Log folder path (e.g. /mnt/user/my/log/folder)
     */
    public FileLoggerFactory(Simulator simulator, String logFolderPath) {
        super(simulator);
        this.logFolderPath = logFolderPath;

        // Ensure it exists
        File runFolder = new File(logFolderPath);
        if (!runFolder.exists() && !runFolder.mkdirs() && !runFolder.exists()) {
            throw new RuntimeException("Could not make run folder: " + logFolderPath);
        }

        // Open all file writers
        try {
            this.writerLinkInfoFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_LINK_INFO));
            this.writerLinkUtilizationFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_LINK_UTILIZATION));
            this.writerLinkNumActiveFlowsFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_LINK_NUM_ACTIVE_FLOWS));
            this.writerNodeInfoFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_NODE_INFO));
            this.writerNodeNumActiveFlowsFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_NODE_NUM_ACTIVE_FLOWS));
            this.writerFlowInfoFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_FLOW_INFO));
            this.writerFlowBandwidthFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_FLOW_BANDWIDTH));
            this.writerConnectionInfoFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_CONNECTION_INFO));
            this.writerConnectionBandwidthFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_CONNECTION_BANDWIDTH));
            BufferedWriter writerRunFinishedFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_RUN_FINISHED));
            writerRunFinishedFile.write("No");
            writerRunFinishedFile.close();
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }

    }

    /**
     * Retrieve the base log folder path.
     *
     * @return  Log folder path
     */
    public String getLogFolderPath() {
        return logFolderPath;
    }

    @Override
    public NodeLogger createNodeLogger(Node node) {
        return new FileNodeLogger(
                simulator,
                node,
                writerNodeNumActiveFlowsFile,
                writerNodeInfoFile
        );
    }

    @Override
    public LinkLogger createLinkLogger(Link link) {
        return new FileLinkLogger(
                simulator,
                link,
                writerLinkUtilizationFile,
                writerLinkNumActiveFlowsFile,
                writerLinkInfoFile
        );
    }

    @Override
    public FlowLogger createFlowLogger(Flow flow) {
        return new FileFlowLogger(
                simulator,
                flow,
                writerFlowBandwidthFile,
                writerFlowInfoFile
        );
    }

    @Override
    public ConnectionLogger createConnectionLogger(Connection connection) {
        return new FileConnectionLogger(
                simulator,
                connection,
                writerConnectionBandwidthFile,
                writerConnectionInfoFile
        );
    }

    @Override
    protected void close() {

        try {
            writerLinkInfoFile.close();
            writerLinkUtilizationFile.close();
            writerLinkNumActiveFlowsFile.close();
            writerNodeInfoFile.close();
            writerNodeNumActiveFlowsFile.close();
            writerFlowBandwidthFile.close();
            writerConnectionBandwidthFile.close();
            writerFlowInfoFile.close();
            writerConnectionInfoFile.close();
            BufferedWriter writerRunFinishedFile = new BufferedWriter(new FileWriter(logFolderPath + "/" + FILE_NAME_RUN_FINISHED));
            writerRunFinishedFile.write("Yes");
            writerRunFinishedFile.close();
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }

    }

    /**
     * Retrieve absolute path of log file.
     *
     * @param logFileName   Log file name (e.g. "temp.csv")
     *
     * @return Complete path (e.g. "C:/folder/temp.csv")
     */
    public String completePath(String logFileName) {
        return logFolderPath + "/" + logFileName;
    }

}
