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

import ch.ethz.systems.floodns.core.*;

import java.util.Collection;

/**
 * The <b>allocator</b> is executed after all the events in a time tick have been processed.
 * To be executed, at least one of the events must have been active in that tick.<br>
 * <br>
 * The reason this component is called the 'allocator', is because it typically is used
 * to determine the {@link Flow flows} and their bandwidth of the present
 * {@link Connection connections} in the {@link Network network}. It can however perform
 * any complex function that an event can, e.g. adapt the topology or start/end flows.
 * This might be quite computationally expensive depending on the type of optimization it performs.
 *
 * @see Flow
 * @see Connection
 * @see Network
 */
public abstract class Allocator extends Aftermath {

    public Allocator(Simulator simulator, Network network) {
        super(simulator, network);
    }

    /**
     * <b>Goal:</b> with global knowledge allocate flows (and their bandwidth) to connections in the network.<br>
     * <br>
     * <b>Available functionality:</b> (a) addition and removal of links, (b) activation and
     * adaptation of connections and flows thereof, (c) and insertion and canceling of events.
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
    @Override
    public abstract void perform();

}
