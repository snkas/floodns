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

public abstract class LoggerFactory {

    protected final Simulator simulator;
    private boolean isConnectionInfoSavingEnabled = true;
    private boolean isConnectionBandwidthSavingEnabled = true;
    private boolean isFlowInfoSavingEnabled = true;
    private boolean isFlowBandwidthSavingEnabled = true;
    private boolean isLinkInfoSavingEnabled = true;
    private boolean isLinkUtilizationSavingEnabled = true;
    private boolean isLinkNumActiveFlowsSavingEnabled = true;
    private boolean isNodeInfoSavingEnabled = true;
    private boolean isNodeNumActiveFlowsSavingEnabled = true;

    public LoggerFactory(Simulator simulator) {
        this.simulator = simulator;
    }

    /**
     * Retrieve the simulator instance of this network.
     *
     * @return  Simulator instance
     */
    public Simulator getSimulator() {
        return simulator;
    }

    final NodeLogger internalCreateNodeLogger(Node node) {
        NodeLogger logger = createNodeLogger(node);
        logger.setInfoSavingEnabled(isNodeInfoSavingEnabled);
        logger.setNumActiveFlowsSavingEnabled(isNodeNumActiveFlowsSavingEnabled);
        return logger;
    }

    final LinkLogger internalCreateLinkLogger(Link link) {
        LinkLogger logger = createLinkLogger(link);
        logger.setInfoSavingEnabled(isLinkInfoSavingEnabled);
        logger.setUtilizationSavingEnabled(isLinkUtilizationSavingEnabled);
        logger.setNumActiveFlowsSavingEnabled(isLinkNumActiveFlowsSavingEnabled);
        return logger;
    }

    final FlowLogger internalCreateFlowLogger(Flow flow) {
        FlowLogger logger = createFlowLogger(flow);
        logger.setInfoSavingEnabled(isFlowInfoSavingEnabled);
        logger.setBandwidthSavingEnabled(isFlowBandwidthSavingEnabled);
        return logger;
    }

    final ConnectionLogger internalCreateConnectionLogger(Connection connection) {
        ConnectionLogger logger = createConnectionLogger(connection);
        logger.setInfoSavingEnabled(isConnectionInfoSavingEnabled);
        logger.setBandwidthSavingEnabled(isConnectionBandwidthSavingEnabled);
        return logger;
    }

    public abstract NodeLogger createNodeLogger(Node node);
    public abstract LinkLogger createLinkLogger(Link link);
    public abstract FlowLogger createFlowLogger(Flow flow);
    public abstract ConnectionLogger createConnectionLogger(Connection connection);
    public abstract void close();

    public void setConnectionInfoSavingEnabled(boolean enabled) {
        isConnectionInfoSavingEnabled = enabled;
    }

    public void setConnectionBandwidthSavingEnabled(boolean enabled) {
        isConnectionBandwidthSavingEnabled = enabled;
    }

    public void setFlowInfoSavingEnabled(boolean enabled) {
        isFlowInfoSavingEnabled = enabled;
    }

    public void setFlowBandwidthSavingEnabled(boolean enabled) {
        isFlowBandwidthSavingEnabled = enabled;
    }

    public void setLinkInfoSavingEnabled(boolean enabled) {
        isLinkInfoSavingEnabled = enabled;
    }

    public void setLinkUtilizationSavingEnabled(boolean enabled) {
        isLinkUtilizationSavingEnabled = enabled;
    }

    public void setLinkNumActiveFlowsSavingEnabled(boolean enabled) {
        isLinkNumActiveFlowsSavingEnabled = enabled;
    }

    public void setNodeInfoSavingEnabled(boolean enabled) {
        isNodeInfoSavingEnabled = enabled;
    }

    public void setNodeNumActiveFlowsSavingEnabled(boolean enabled) {
        isNodeNumActiveFlowsSavingEnabled = enabled;
    }

}
