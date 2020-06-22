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

package ch.ethz.systems.floodns.user.log;

import ch.ethz.systems.floodns.core.*;

import java.util.List;

public class CustomLoggerFactory extends LoggerFactory {

    int a = 0;
    int b = 0;
    int c = 0;
    int d = 0;
    int e = 0;
    int f = 0;
    int g = 0;
    int h = 0;
    int i = 0;

    CustomLoggerFactory(Simulator simulator) {
        super(simulator);
    }

    @Override
    public NodeLogger createNodeLogger(final Node node) {
        return new NodeLogger(simulator, node) {
            @Override
            protected void saveNumActiveFlows(long start, long end, int numActiveFlows) {
                a++;
            }

            @Override
            protected void saveInfo(int nodeId, double avgNumActiveFlows, Metadata extraInfo) {
                b++;
            }
        };
    }

    @Override
    public LinkLogger createLinkLogger(final Link link) {
        return new LinkLogger(simulator, link) {
            @Override
            protected void saveLinkUtilization(long start, long end, double utilization) {
                c++;
            }

            @Override
            protected void saveNumActiveFlows(long start, long end, int numActiveFlows) {
                d++;
            }

            @Override
            protected void saveInfo(int linkId, int srcNodeId, int dstNodeId, long startTime, long endTime, long duration, double avgUtilization, double avgNumActiveFlows, Metadata extraInfo) {
                e++;
            }
        };
    }

    @Override
    public FlowLogger createFlowLogger(Flow flow) {
        return new FlowLogger(simulator, flow) {
            @Override
            protected void saveFlowBandwidth(long start, long end, double bandwidth) {
                f++;
            }

            @Override
            protected void saveInfo(int flowId, int srcNodeId, int dstNodeId, AcyclicPath path, long startTime, long endTime, long duration, double bandwidthSum, double avgBandwidth, Metadata extraInfo) {
                g++;
            }
        };
    }

    @Override
    public ConnectionLogger createConnectionLogger(Connection connection) {
        return new ConnectionLogger(simulator, connection) {
            @Override
            protected void saveFlowBandwidth(long start, long end, double bandwidth) {
                h++;
            }

            @Override
            protected void saveInfo(int connectionId, int srcNodeId, int dstNodeId, double totalSize, double transmittedSize, List<Integer> pastAndPresentFlowIds, long startTime, long endTime, long duration, double avgBandwidth, boolean finished, Metadata extraInfo) {
                i++;
            }
        };
    }

    @Override
    public void close() {

    }

}
