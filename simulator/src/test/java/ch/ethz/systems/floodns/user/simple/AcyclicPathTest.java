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

package ch.ethz.systems.floodns.user.simple;

import ch.ethz.systems.floodns.core.AcyclicPath;
import ch.ethz.systems.floodns.core.Link;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Node;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AcyclicPathTest {

    @Test
    public void testFullAndInvalid() {

        Network network = new Network(9);
        Network networkB = new Network(9);
        Node node0 = network.getNode(0);
        Node node8 = network.getNode(8);

        Link link03 = network.addLink(0, 3, 10.0);
        Link link38 = network.addLink(3, 8, 10.0);
        Link link80 = network.addLink(8, 0, 10.0);
        Link link08 = network.addLink(0, 8, 10.0);
        Link link04B = networkB.addLink(0, 4, 10.0);

        // Create path 0 -> 3 -> 8
        AcyclicPath p = new AcyclicPath();
        p.add(link03);
        p.add(link38);

        // Check path
        assertEquals(link03, p.get(0));
        assertEquals(link38, p.get(1));
        assertEquals(node0, p.getSrcNode());
        assertEquals(node8, p.getDstNode());

        // Try to add discontinuous link, should throw
        // illegal argument exception
        boolean thrown = false;
        try {
            p.add(link08);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Try to add cycle, should throw
        // illegal argument exception
        thrown = false;
        try {
            p.add(link80);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Try to add different network
        thrown = false;
        try {
            p.add(link04B);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testAddAll() {

        Network network = new Network(10);
        Link link89 = network.addLink(8, 9, 10.0);
        Link link03 = network.addLink(0, 3, 10.0);
        Link link38 = network.addLink(3, 8, 10.0);
        Link link80 = network.addLink(8, 0, 10.0);
        Link link08 = network.addLink(0, 8, 10.0);

        // Create path 0 -> 3 -> 8
        AcyclicPath p = new AcyclicPath();
        p.add(link03);
        p.add(link38);

        // Create duplicate
        AcyclicPath p2 = new AcyclicPath();
        p2.addAll(p);

        // Modify original
        p.add(link89);

        // No coupling
        assertEquals(3, p.size());
        assertEquals(2, p2.size());

        // Try to add discontinuous link, should throw
        // illegal argument exception
        boolean thrown = false;
        try {
            ArrayList<Link> temp = new ArrayList<>();
            temp.add(link08);
            p2.addAll(temp);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Try to add cycle, should throw
        // illegal argument exception
        thrown = false;
        try {
            ArrayList<Link> temp = new ArrayList<>();
            temp.add(link80);
            p2.addAll(temp);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

}
