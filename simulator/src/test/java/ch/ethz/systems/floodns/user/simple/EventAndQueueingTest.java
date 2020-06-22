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

import ch.ethz.systems.floodns.core.Event;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.allocator.SimpleMmfAllocator;
import ch.ethz.systems.floodns.ext.allocator.VoidAllocator;
import ch.ethz.systems.floodns.ext.logger.empty.VoidLoggerFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EventAndQueueingTest {

    private class CustomEvent extends Event {

        final List<CustomEvent> callList;
        
        CustomEvent(Simulator simulator, int priority, long timeFromNow, List<CustomEvent> callList) {
            super(simulator, priority, timeFromNow);
            this.callList = callList;
        }

        @Override
        protected void trigger() {
            callList.add(this);
        }

    }

    @Test
    public void testConstruction() {

        // Setup
        Simulator simulator = new Simulator();
        Network network = new Network(0);
        simulator.setup(network, new SimpleMmfAllocator(simulator, network), new VoidLoggerFactory(simulator));

        // Insert event
        CustomEvent event = new CustomEvent(simulator, 0, 1000, null);
        assertEquals(event.getTime(), 1000L);
        assertTrue(event.isActive());
        simulator.insertEvents(event);

        // Disabled event
        simulator.cancelEvent(event);
        assertFalse(event.isActive());

    }

    @Test
    public void testPastInvalid() {

        Simulator simulator = new Simulator();
        boolean thrown = false;
        try {
            new CustomEvent(simulator, 0, -353, null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testQueueOrderWithPriority() {

        // Setup dummy simulator
        Simulator simulator = new Simulator();
        Network network = new Network(0);
        simulator.setup(network, new VoidAllocator(simulator, network), new VoidLoggerFactory(simulator));

        // Events
        List<CustomEvent> callList = new ArrayList<>();
        Event e1 = new CustomEvent(simulator, 0, 1000, callList);
        Event e2 = new CustomEvent(simulator, 0, 0, callList);
        Event e3 = new CustomEvent(simulator, -1, 848, callList);
        Event e4 = new CustomEvent(simulator, 1, 1000, callList);
        Event e5 = new CustomEvent(simulator, 0, 1000, callList);
        Event e6 = new CustomEvent(simulator, 2, 1000, callList);
        Event e7 = new CustomEvent(simulator, 3, 1000, callList);
        Event e8 = new CustomEvent(simulator, 2, 1000, callList);
        Event e9 = new CustomEvent(simulator, 453, 999999, callList);
        
        // Insert events
        simulator.insertEvents(e1, e2, e3, e4, e5, e6, e7, e8, e9);
        
        // Run
        simulator.run(10000000000L);
        
        // Empty queue and make sure that it is in correct order
        assertEquals(callList.size(), 9);
        assertTrue(callList.get(0) == e2);
        assertTrue(callList.get(1) == e3);
        assertTrue(callList.get(2) == e7);
        assertTrue(callList.get(3) == e6);
        assertTrue(callList.get(4) == e8);
        assertTrue(callList.get(5) == e4);
        assertTrue(callList.get(6) == e1);
        assertTrue(callList.get(7) == e5);
        assertTrue(callList.get(8) == e9);

    }

    @Test
    public void testComparison() {

        // Dummy simulator
        Simulator simulator = new Simulator();
        Network network = new Network(0);
        simulator.setup(network, new VoidAllocator(simulator, network), new VoidLoggerFactory(simulator));

        // Add events in ascending order
        List<Event> events = new ArrayList<>();
        events.add(new CustomEvent(simulator, 1, 0, new ArrayList<CustomEvent>()));
        events.add(new CustomEvent(simulator, 1, 0, new ArrayList<CustomEvent>()));
        events.add(new CustomEvent(simulator, 0, 0, new ArrayList<CustomEvent>()));
        events.add(new CustomEvent(simulator, 0, 0, new ArrayList<CustomEvent>()));
        events.add(new CustomEvent(simulator, 1, 1, new ArrayList<CustomEvent>()));
        events.add(new CustomEvent(simulator, 1, 1, new ArrayList<CustomEvent>()));
        events.add(new CustomEvent(simulator, 0, 1, new ArrayList<CustomEvent>()));
        events.add(new CustomEvent(simulator, 0, 1, new ArrayList<CustomEvent>()));
        simulator.insertEvents(events);

        // Full cartesian product of event comparisons
        for (int i = 0 ; i < 8; i++) {
            for (int j = 0 ; j < 8; j++) {
                if (i == j) {
                    assertEquals(0, events.get(i).compareTo(events.get(j)));
                } else if (i < j) {
                    assertTrue(events.get(i).compareTo(events.get(j)) < 0);
                } else if (i > j) {
                    assertTrue(events.get(i).compareTo(events.get(j)) > 0);
                }
            }
        }

    }

    @Test
    public void testToString() {
        Simulator simulator = new Simulator();
        Network network = new Network(0);
        simulator.setup(network, new VoidAllocator(simulator, network), new VoidLoggerFactory(simulator));
        Event e1 = new CustomEvent(simulator, 33, 1000, new ArrayList<CustomEvent>());
        Event e2 = new CustomEvent(simulator, 11, 22, new ArrayList<CustomEvent>());
        assertEquals(e1.toString(), "Event#-1[t=1000, p=33]");
        simulator.insertEvents(e1, e2);
        assertEquals(e1.toString(), "Event#0[t=1000, p=33]");
        assertEquals(e2.toString(), "Event#1[t=22, p=11]");
    }

}
