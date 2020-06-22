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

import java.util.Collection;

public abstract class Event implements Comparable<Event> {

    // Simulator handle
    protected final Simulator simulator;

    // Added for absolute determinism in the event priority queue
    private long eid;

    // Priority if events are in the same time tick
    private final int priority;

    // Time to trigger
    private final long time;

    // Whether the event is active
    private boolean active;

    /**
     * Create event which will happen the given amount of time later.
     *
     * @param simulator       Simulator instance to which this event belongs
     * @param priority        Time tick priority
     * @param timeFromNow     Time it will take before happening from now
     */
    public Event(Simulator simulator, int priority, long timeFromNow) {
        if (timeFromNow < 0) {
            throw new IllegalArgumentException("Not allowed to create an event in the past (" + timeFromNow + " < 0).");
        }
        this.simulator = simulator;
        this.time = simulator.getTimeFromNow(timeFromNow);
        this.active = true;
        this.priority = priority;
        this.eid = -1;
    }

    /**
     *
     * Retrieve the simulator instance of this event.
     *
     * @return  Simulator instance
     */
    Simulator getSimulator() {
        return simulator;
    }

    /**
     * Check whether the event should still happen.
     * An event is deactivated instead of removed from the event
     * queue because it is expensive to find and remove it.
     *
     * @return  True iff event is still planned to be triggered
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Cancel the event, meaning that it will never be executed.
     */
    void cancel() {
        active = false;
    }

    /**
     * <b>Goal:</b> trigger whatever has to happen in the simulation when this event is triggered.<br>
     * <br>
     * <b>Available functionality:</b> (a) addition and removal of links, (b) activation and
     * adaptation of connections, and (c) insertion and canceling of events.
     *
     * @see Simulator#addNewLink(int, int, double)
     * @see Simulator#removeExistingLink(Link)
     *
     * @see Simulator#activateConnection(Connection)
     * @see Simulator#addFlowToConnection(Connection, AcyclicPath)
     * @see Simulator#endFlow(Flow)
     * @see Simulator#allocateFlowBandwidth(Flow, double)
     *
     * @see Simulator#insertEvents(Event...)
     * @see Simulator#insertEvents(Collection)
     * @see Simulator#cancelEvent(Event)
     */
    protected abstract void trigger();

    /**
     * Retrieve absolute simulation time at which the
     * event must occur.
     *
     * @return  Absolute simulation event time
     */
    public long getTime() {
        return time;
    }

    /**
     * Set the event priority queue order for within the same time tick.
     */
    void setEid() {
        this.eid = simulator.getNextEventId();
    }

    /**
     * Check whether the event has been inserted into the event queue already.
     *
     * @return  True iff inserted into the event queue
     */
    boolean wasInserted() {
        return this.eid != -1;
    }

    @Override
    public int compareTo(Event o) {
        return (this.time < o.time ? -1 :
                    (this.time == o.time ?
                        (this.priority < o.priority ? 1 :
                                (this.priority == o.priority ? Long.compare(this.eid, o.eid) : -1)
                        )
                    : 1)
        );
    }

    @Override
    public String toString() {
        return "Event#" + eid + "[t=" + time + ", p=" + priority + "]";
    }

}
