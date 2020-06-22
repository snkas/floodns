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

package ch.ethz.systems.floodns.ext.routing;

import ch.ethz.systems.floodns.PathTestUtility;
import ch.ethz.systems.floodns.TestNetworkCreator;
import ch.ethz.systems.floodns.core.Network;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RoutingUtilityTest {

    @Test
    public void testConvertToAcyclic() {
        Network network = TestNetworkCreator.fullyConnected(100, 7.0);

        // Many behind each other
        assertEquals(
                PathTestUtility.createAcyclicPath(network, "0-1-2-5-6"),
                RoutingUtility.convertToAcyclic(
                        PathTestUtility.createPotentiallyCyclicPath(network, "0-1-2-5-6-7-6-7-6")
                )
        );

        // Two cycles
        assertEquals(
                PathTestUtility.createAcyclicPath(network, "0-1-2-3-11"),
                RoutingUtility.convertToAcyclic(
                        PathTestUtility.createPotentiallyCyclicPath(network, "0-1-2-6-7-2-3-8-9-8-9-3-11")
                )
        );

    }

}
