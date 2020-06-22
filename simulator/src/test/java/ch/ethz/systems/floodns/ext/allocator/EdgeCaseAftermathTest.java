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

package ch.ethz.systems.floodns.ext.allocator;

import ch.ethz.systems.floodns.PathTestUtility;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.logger.file.FileLoggerFactory;
import ch.ethz.systems.floodns.ext.logger.file.TestLogReader;
import ch.ethz.systems.floodns.user.network.NetworkTestHelper;
import ch.ethz.systems.floodns.user.network.TestBody;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class EdgeCaseAftermathTest {

    @Test
    public void testFlowAddingRemovingSameTick() throws IOException {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 -> 1
        //
        network.addLink(0, 1, 10.0);

        String path = Files.createTempDirectory("temp").toAbsolutePath().toString();
        FileLoggerFactory loggerFactory = new FileLoggerFactory(simulator, path);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {
                PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                simulator.allocateFlowBandwidth(network.getActiveFlow(0), 7.3);
                simulator.endFlow(network.getActiveFlow(0));
            }

        }, loggerFactory);

        TestLogReader reader = new TestLogReader(loggerFactory);
        assertEquals(reader.getFlowIdToInfoLog().get(0).get(0), "0");
        assertEquals(reader.getFlowIdToInfoLog().get(0).get(1), "1");
        assertEquals(reader.getFlowIdToInfoLog().get(0).get(2), "0-[0]->1");
        assertEquals(reader.getFlowIdToInfoLog().get(0).get(3), "0");
        assertEquals(reader.getFlowIdToInfoLog().get(0).get(4), "0");
        assertEquals(reader.getFlowIdToInfoLog().get(0).get(5), "0");
        assertEquals(reader.getFlowIdToInfoLog().get(0).get(6), "0.0");
        assertEquals(reader.getFlowIdToInfoLog().get(0).get(7), "0.0");

        assertEquals(path, loggerFactory.getLogFolderPath());

    }

    @Test
    public void testLinkStartedEndedSameTick() throws IOException {
        Simulator simulator = new Simulator();
        Network network = new Network(6);

        //
        // 0 -> 1
        //
        network.addLink(0, 1, 10.0);

        String path = Files.createTempDirectory("temp").toAbsolutePath().toString();
        FileLoggerFactory loggerFactory = new FileLoggerFactory(simulator, path);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                simulator.addNewLink(0, 1, 10.0);
                simulator.removeExistingLink(network.getLink(1));

            }

        }, loggerFactory);

        TestLogReader reader = new TestLogReader(loggerFactory);
        assertEquals(reader.getLinkIdToInfoLog().get(0).get(0), "0");
        assertEquals(reader.getLinkIdToInfoLog().get(0).get(1), "1");
        assertEquals(reader.getLinkIdToInfoLog().get(0).get(2), "0");
        assertEquals(reader.getLinkIdToInfoLog().get(0).get(3), "10");
        assertEquals(reader.getLinkIdToInfoLog().get(0).get(4), "10");
        assertEquals(reader.getLinkIdToInfoLog().get(0).get(5), "0.0");
        assertEquals(reader.getLinkIdToInfoLog().get(0).get(6), "0.0");

    }

}
