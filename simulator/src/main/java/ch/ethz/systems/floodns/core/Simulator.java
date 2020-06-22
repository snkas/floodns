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

import ch.ethz.systems.floodns.ext.logger.empty.VoidLoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static ch.ethz.systems.floodns.core.Simulator.SimulatorState.*;

/**
 * The <b>simulator</b> performs the run of flows throughout time,
 * given (a) the network, (b) the aftermath, (c) the logging mechanism,
 * and (d) a start set of events which trigger the remainder of events
 * in the simulation.<br>
 * <br>
 * Besides for setup, the simulator is the point of contact for the aftermath and events.
 * It offers the following functionality:
 * <ol>
 *      <li>The start, modification and termination of connections and their flows
 *      (via {@link #activateConnection(Connection)}, {@link #addFlowToConnection(Connection, AcyclicPath)},
 *      {@link #endFlow(Flow)}, and {@link #terminateConnection(Connection)})</li>
 *      <li>The addition of new links (via {@link #addNewLink(int, int, double)})</li>
 *      <li>The removal of existing links (via {@link #removeExistingLink(Link)})</li>
 *      <li>The insertion of new events (via {@link #insertEvents(Event...)}) or
 * canceling of events (via {@link #cancelEvent(Event)})</li>
 * </ol>
 *
 * A simulator can only be run once, along with the {@link Aftermath aftermath} used.
 */
public class Simulator {

    // Class logger
    private static final Logger logger = LogManager.getLogger(Simulator.class);

    // All possible states of the simulator
    enum SimulatorState {
        INSTANTIATED,
        SETUP,
        RUNNING,
        FINISHED
    }

    // Time interval at which to show the percentage of progress
    private long progressShowInterval;

    // Main ordered event queue (run variable)
    private final PriorityQueue<Event> eventQueue;
    private long eventIdCounter;

    // Current time in the simulation (run variable)
    private long now;

    // Total runtime to run
    private long totalRuntime;

    // What is the state of the simulator
    private SimulatorState state;

    // Logger factory
    private LoggerFactory loggerFactory;

    // Main components
    private Network network;
    private Aftermath aftermath;

    // Connection management
    private ConnectionFinishEvent nextConnectionFinishEvent;
    private HashMap<Integer, Connection> idToActiveConnection = new HashMap<>();
    private int connectionIdCounter;

    // Precision
    private static final double DEFAULT_FLOW_PRECISION = 1e-10;
    private final double flowPrecision;

    /**
     * Instantiation of the simulator with default flow precision (1e-10).
     *
     * After instantiation, it is possible to {@link #setup(Network, Aftermath, LoggerFactory) setup} the simulator.
     */
    public Simulator() {
        this(DEFAULT_FLOW_PRECISION);
    }

    /**
     * Instantiation of the simulator.
     *
     * After instantiation, it is possible to {@link #setup(Network, Aftermath, LoggerFactory) setup} the simulator.
     *
     * @param   flowPrecision       Flow precision (recommended: 1e-10)
     */
    public Simulator(double flowPrecision) {
        this.flowPrecision = flowPrecision;
        this.eventQueue = new PriorityQueue<>();
        this.eventIdCounter = 0;
        this.state = INSTANTIATED;
        this.progressShowInterval = 10000000L;
        this.loggerFactory = new VoidLoggerFactory(this);
        this.connectionIdCounter = 0;
        this.now = 0;
    }

    /**
     * Retrieve the flow precision.
     *
     * @return Flow precision
     */
    public double getFlowPrecision() {
        return flowPrecision;
    }

    /**
     * Set the progress show interval when running the simulation.
     *
     * @param interval      Interval (in time units)
     */
    public void setProgressShowInterval(long interval) {
        this.progressShowInterval = interval;
    }

    /**
     * Retrieve the identifier of the next connection (automatically increments afterwards).
     *
     * @return Next connection identifier
     */
    int getNextConnectionId() {
        return connectionIdCounter++;
    }

    /**
     * Retrieve the identifier of the next event (automatically increments afterwards).
     *
     * @return Next event identifier
     */
    long getNextEventId() {
        return eventIdCounter++;
    }

    /**
     * Retrieve the runtime of the current run (can only be called if the simulator is running)
     *
     * @return Total runtime
     */
    public long getTotalRuntime() {
        if (state != RUNNING) {
            throw new IllegalStateException("Impossible to retrieve the runtime if the simulator is not running.");
        }
        return totalRuntime;
    }

    /**
     * Setup the simulator.
     *
     * After the setup, it is possible to {@link #run(long) run} the simulator.
     *
     * WARNING: when re-using a network, be sure that it is not adapted in the previous simulator run.
     *
     * @param network             Network instance
     * @param aftermath           Aftermath instance
     * @param loggerFactory       Logger factory
     */
    public void setup(Network network, Aftermath aftermath, LoggerFactory loggerFactory) {
        assert(network != null && aftermath != null && loggerFactory != null);

        if (state != INSTANTIATED) {
            throw new IllegalStateException("Impossible to setup a simulator which is running or is finished.");
        } else if (aftermath.getSimulator() != this) {
            throw new IllegalArgumentException("The aftermath is not setup with this simulator.");
        } else if (loggerFactory.getSimulator() != this) {
            throw new IllegalArgumentException("The logger factory is not setup with this simulator.");
        }

        // Set main components
        this.network = network;
        this.aftermath = aftermath;

        // Logging decision
        this.loggerFactory = loggerFactory;

        // Setup simulator for the network (create loggers for nodes & links in it)
        // Flows and connections cannot yet be instantiated, as they are only a runtime concept
        this.network.setupLoggersWithSimulator(this);

        // Setup finished
        this.state = SETUP;

    }

    /**
     * Run the simulation for the specified amount of time.
     *
     * NOTE: You must first call {@link #setup(Network, Aftermath, LoggerFactory) setup}
     * to be able to run, else the run will not be able to start.
     *
     * @param runtime     Running time
     */
    public void run(long runtime) {

        if (state != SETUP) {
            throw new IllegalStateException("Cannot run a simulator which is not setup.");
        }
        state = RUNNING;

        // Runtime of the current run
        this.totalRuntime = runtime;

        // Log start
        logger.info("SIMULATION");
        logger.info(String.format("Running the simulation for %d time units...", runtime));

        // Time loop
        long startTime = System.currentTimeMillis();
        // long realTime = System.currentTimeMillis();
        long nextProgressLog = progressShowInterval;
        long elapsed;
        Event event, nextEvent;
        while (!eventQueue.isEmpty() && now <= runtime) {

            // Grab next active event
            event = eventQueue.poll();
            while (event != null && !event.isActive()) {
                event = eventQueue.poll();
            }

            // There is at least one active event in the queue at the start for this
            // loop to even be called because it is not possible to cancel an event if the
            // simulator is not running. After the first call of this loop, an active
            // next-connection-finish-event exists guaranteed in the queue. As such, it is
            // impossible for there to not exist an active event somewhere in the queue.
            assert(event != null && event.isActive());

            // Determine amount of time elapsed
            elapsed = now;
            now = event.getTime();
            elapsed = now - elapsed;

            // Update progress of flows if some time has passed
            if (elapsed > 0) {
                updateConnectionProgression(elapsed);
            }

            // Execute all events of this time tick
            nextEvent = eventQueue.peek();
            event.trigger(); // The first event we found is guaranteed active
            while (nextEvent != null && nextEvent.getTime() == now) {
                event = eventQueue.poll();
                if (event.isActive()) {
                    event.trigger();
                }
                nextEvent = eventQueue.peek();
            }

            // Call aftermath
            aftermath.perform();

            // Insert new flow finish event
            if (now != runtime) {

                // Cancel the previous flow finish event
                if (nextConnectionFinishEvent != null) {
                    nextConnectionFinishEvent.cancel();
                }

                // Insert the new next one
                nextConnectionFinishEvent = new ConnectionFinishEvent(
                        this,
                        Math.min(runtime - now, refreshAndGetNextConnectionUpdateTime())
                );
                insertEvents(nextConnectionFinishEvent);

            }

            // Log elapsed time
            if (now > nextProgressLog) {
                nextProgressLog += progressShowInterval;
                long realTimeNow = System.currentTimeMillis();
                logger.info(String.format(
                        "%5.2f%% - Simulation Time = ~%1.0e time units ::: Wallclock Time = %.2f s.",
                        now * 100.0 / runtime,
                        (double) now,
                        (realTimeNow - startTime) / 1000.0
                ));
            }

            // Finish after executing the events in the last time tick
            if (now == runtime) {
                break;
            }

        }

        // Make sure run ends at the final time
        now = runtime;

        // Log end
        logger.info(String.format("Finished simulation."));
        logger.info(String.format(
                "Simulation of %d time units took in wallclock time %.1f seconds.%n",
                runtime,
                ((System.currentTimeMillis() - startTime) / 1000.0)
        ));

        // Flush all logs

        // Log flush start
        long logFlushStartTime = System.currentTimeMillis();
        logger.info("WRITING LOGS");

        for (Node node : network.getNodes()) {
            node.getLogger().finalFlush(node.getMetadata());
        }
        logger.info("  > Node logs");

        for (Link link : network.getPresentLinks()) {
            link.getLogger().finalFlush(link.getMetadata());
        }
        logger.info("  > Link logs");

        for (Connection connection : idToActiveConnection.values()) {
            connection.getLogger().finalFlush(connection.getMetadata());
        }
        logger.info("  > Connection logs");

        // Finally reset the network such that it can be re-used (flushes flow results)
        network.finalizeFlows();
        logger.info("  > Flow logs");

        // Close all file streams
        loggerFactory.close();

        // Log flush end
        logger.info(String.format(
                "  > Log flush took %.1fs seconds%n",
                (System.currentTimeMillis() - logFlushStartTime) / 1000.0
        ));

        // After a run, it is finished
        state = FINISHED;

    }

    /**
     * Determine the time at which the next first connection needs an update.
     *
     * @return  Next flow completion time of one of the active connections
     */
    private long refreshAndGetNextConnectionUpdateTime() {

        // Get minimum flow completion time
        long nextConnectionUpdateTime = Long.MAX_VALUE;
        for (Connection connection : idToActiveConnection.values()) {
            nextConnectionUpdateTime = Math.min(connection.timeTillUpdateNeeded(), nextConnectionUpdateTime);
        }

        // Return found time
        return nextConnectionUpdateTime;

    }

    /**
     * Update the progression of all active flows relative
     * to their connections in the simulator.
     *
     * @param elapsed   Time elapsed
     */
    private void updateConnectionProgression(long elapsed) {

        // Determine the progress of all flows
        List<Integer> finishedConnections = new ArrayList<>();
        for (Connection connection : idToActiveConnection.values()) {
            if (connection.reduceRemainder(elapsed)) {

                // If the connection is finished
                finishedConnections.add(connection.getConnectionId());
                for (Flow flow : new ArrayList<>(connection.getActiveFlows())) {
                    network.endFlow(flow);
                }

                // Remove all references to flows, such that they
                // can be garbage collected
                connection.getLogger().finalFlush(connection.getMetadata());
                connection.cleanup();
                connection.setTerminated();

            }
        }

        // Remove all finished connections from state
        for (Integer id : finishedConnections) {
            idToActiveConnection.remove(id);
        }

    }

    /**
     * Retrieve the network instance used by the simulator.
     *
     * @return  Network instance
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Retrieve the aftermath instance used by the simulator.
     *
     * @return  Aftermath instance
     */
    public Aftermath getAftermath() {
        return aftermath;
    }

    /**
     * Retrieve the logger factory of the simulator.
     *
     * @return  Logger factory instance
     */
    public LoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    /**
     * Retrieve the current time plus the amount of time specified.
     * This is used to plan events in the future.
     *
     * @param timeAddition   Amount of time relative from now
     *
     * @return  Absolute time
     */
    long getTimeFromNow(long timeAddition) {
        return now + timeAddition;
    }

    /**
     * Retrieve the current time since simulation start.
     *
     * @return  Current time
     */
    public long getCurrentTime() {
        return now;
    }

    /**
     * Retrieve all the connections currently active in the simulator.
     *
     * @return  Set of active connections
     */
    public Collection<Connection> getActiveConnections() {
        return Collections.unmodifiableCollection(idToActiveConnection.values());
    }

    /**
     * Get the active flow with the specified connection identifier.
     *
     * @param connectionId    Connection identifier
     *
     * @return  Connection instance (null, if not exists or not active)
     */
    public Connection getActiveConnection(int connectionId) {
        return idToActiveConnection.get(connectionId);
    }

    /**
     * Activate a connection in the simulator.
     * Once a connection is activated, flows can be freely added and removed.
     *
     * @param connection  Connection instance
     *
     * @throws IllegalArgumentException    Iff the connection is already active.
     * @throws IllegalStateException       Iff the state is not running.
     */
    public void activateConnection(Connection connection) {
        if (state != RUNNING) {
            throw new IllegalStateException("Cannot activate connection if the simulation is not running.");
        } else if (connection.getSimulator() != this) {
            throw new IllegalArgumentException("Connection " + connection + " is not bound to this simulator.");
        } else if (connection.getStatus() != Connection.Status.AWAITING_ACTIVATION) {
            throw new IllegalArgumentException("Connection " + connection + " is not awaiting activation.");
        }
        connection.setLoggerViaSimulator(this);
        idToActiveConnection.put(connection.getConnectionId(), connection);
        connection.setActive();
    }

    /**
     * Terminate a connection before it has been completed.
     *
     * @param connection  Connection instance
     */
    public void terminateConnection(Connection connection) {
        if (state != RUNNING) {
            throw new IllegalStateException("Cannot terminate connection if the simulation is not running.");
        } else if (connection.getSimulator() != this) {
            throw new IllegalArgumentException("Connection " + connection + " is not bound to this simulator.");
        } else if (connection.getStatus() != Connection.Status.ACTIVE) {
            throw new IllegalArgumentException("Connection " + connection + " is not active.");
        }
        for (Flow flow : new ArrayList<>(connection.getActiveFlows())) {
            network.endFlow(flow);
        }
        connection.getLogger().finalFlush(connection.getMetadata());
        connection.cleanup();
        connection.setTerminated();
        idToActiveConnection.remove(connection.getConnectionId());
    }

    /**
     * Add a new flow to an activated connection.
     *
     * @param connection  Connection instance
     * @param flowPath    Flow path
     *
     * @return  Flow instance
     *
     * @throws IllegalArgumentException    Iff the connection is not active.
     * @throws IllegalStateException       Iff the state is not running.
     */
    public Flow addFlowToConnection(Connection connection, AcyclicPath flowPath) {
        if (state != RUNNING) {
            throw new IllegalStateException("Cannot add flow to connection if the simulation is not running.");
        } else if (connection.getStatus() != Connection.Status.ACTIVE) {
            throw new IllegalArgumentException("Connection " + connection + " is not active.");
        } else if (connection.getSimulator() != this) {
            throw new IllegalArgumentException("Connection " + connection + " is not bound to this simulator.");
        } else if (flowPath.size() < 1) {
            throw new IllegalArgumentException("Flow path " + flowPath + " is empty.");
        } else if (flowPath.getSimulator() != this) {
            throw new IllegalArgumentException(
                    "Link" + (flowPath.size() == 1 ? "" : "s") + " of flow path " + flowPath +
                            " " + (flowPath.size() == 1 ? "is" : "are") + " not bound to this simulator."
            );
        } else if (!flowPath.getSrcNode().equals(connection.getSrcNode())
                   || !flowPath.getDstNode().equals(connection.getDstNode())) {
            throw new IllegalArgumentException(
                    "Cannot add flow which does not start and end at the connections' endpoints."
            );
        }
        Flow flow = network.startFlow(connection, flowPath);
        connection.addFlow(flow);
        return flow;
    }

    /**
     * Allocate a certain amount of bandwidth to a flow.
     * The allocation both is saved on the flow and on the links;
     * it forces that the link capacity is not exceeded.
     *
     * @param flow          Flow instance
     * @param bandwidth     Bandwidth [0, inf)
     *
     * @throws IllegalStateException        Iff the state is not running.
     * @throws IllegalArgumentException     Iff there is insufficient available bandwidth (can be prevented by checking
     *                                      first by calling {@link Flow#canAllocateBandwidth(double)}) or if the
     *                                      bandwidth is negative.
     */
    public void allocateFlowBandwidth(Flow flow, double bandwidth) {
        if (state != RUNNING) {
            throw new IllegalStateException("Cannot allocate flow bandwidth if the simulation is not running.");
        } else if (flow.getSimulator() != this) {
            throw new IllegalArgumentException("Flow " + flow + " is not bound to this simulator.");
        } else if (network.getActiveFlow(flow.getFlowId()) == null) {
            throw new IllegalArgumentException("Cannot allocate flow bandwidth if the flow is not active.");
        }
        flow.allocateBandwidth(bandwidth);
    }

    /**
     * End an assigned flow.
     *
     * @param flow      Flow instance (must be assigned to the connection)
     *
     * @throws IllegalArgumentException   Iff the connection the flow belongs to or the flow itself is not active.
     * @throws IllegalStateException      Iff the state is not running.
     */
    public void endFlow(Flow flow) {
        Connection connection = flow.getParentConnection();
        if (state != RUNNING) {
            throw new IllegalStateException("Cannot end flow of connection if the simulation is not running.");
        } else if (flow.getSimulator() != this) {
            throw new IllegalArgumentException("Flow " + flow + " is not bound to this simulator.");
        } else if (connection.getStatus() != Connection.Status.ACTIVE) {
            throw new IllegalArgumentException("Connection " + connection + " is not active.");
        } else if (!connection.getActiveFlows().contains(flow)) {
            throw new IllegalArgumentException("Connection " + connection + " does not have flow " + flow
                                               + " as active.");
        }
        network.endFlow(flow);
    }

    /**
     * Create a new additional link during runtime.
     *
     * @param from      From node identifier
     * @param to        To node identifier
     * @param capacity  Link capacity (0, inf)
     *
     * @return Link created and added to the network
     *
     * @throws IllegalStateException      Iff the state is not running.
     * @throws IllegalArgumentException   Iff the node identifiers are invalid or the capacity is lower or equal to 0.
     */
    public Link addNewLink(int from, int to, double capacity) {
        if (state != RUNNING) {
            throw new IllegalStateException("Cannot add new link if the simulation is not running.");
        }
        return network.addLink(from, to, capacity);
    }

    /**
     * Remove an existing link during runtime.
     *
     * @param linkId  Link identifier
     *
     * @throws IllegalStateException      Iff the state is not running.
     */
    public void removeExistingLink(int linkId) {
        removeExistingLink(network.getLink(linkId));
    }

    /**
     * Remove an existing link during runtime.
     *
     * @param link  Link instance
     *
     * @throws IllegalStateException      Iff the state is not running.
     */
    public void removeExistingLink(Link link) {
        if (state != RUNNING) {
            throw new IllegalStateException("Cannot remove existing link if the simulation is not running.");
        } else if (link.getSimulator() != this) {
            throw new IllegalArgumentException("Link " + link + " is not bound to this simulator.");
        }
        network.removeLink(link);
    }

    /**
     * Insertion of any additional event(s) into the event priority queue.
     *
     * @param events     Event instances
     *
     * @throws IllegalStateException      Iff the state is not setup or running.
     * @throws IllegalArgumentException   Iff event is not in the future, or already inserted, or is not active
     */
    public void insertEvents(Event... events) {
        for (Event e : events) {
            insertEvent(e);
        }
    }

    /**
     * Insertion of any additional events within a collection into the event priority queue.
     *
     * @param events     Events collection
     *
     * @throws IllegalStateException      Iff the state is not setup or running.
     * @throws IllegalArgumentException   Iff event is not in the future, or already inserted, or is not active
     */
    public void insertEvents(Collection<Event> events) {
        for (Event e : events) {
            insertEvent(e);
        }
    }

    /**
     * Insert an event.
     *
     * @param e     Event e
     *
     * @throws IllegalStateException      Iff the state is not setup or running.
     * @throws IllegalArgumentException   Iff event is not in the future, or already inserted, or is not active.
     */
    private void insertEvent(Event e) {
        if (state != SETUP && state != RUNNING) {
            throw new IllegalStateException("Cannot insert event if the simulation is not being setup or is running.");
        } else if (e.getSimulator() != this) {
            throw new IllegalArgumentException("Event " + e + " is not bound to this simulator.");
        } else if (e.getTime() < now || (e.getTime() == now && state == RUNNING)) {
            throw new IllegalArgumentException("Cannot create event " + e + " which is not in the future.");
        } else if (e.wasInserted()) {
            throw new IllegalArgumentException("Cannot insert event " + e + " which is already inserted.");
        }
        e.setEid();
        eventQueue.add(e);
    }

    /**
     * Cancel a specific event (such that it will not be executed).
     *
     * @param event Event instance
     *
     * @throws IllegalStateException      Iff the state is not setup or running.
     * @throws IllegalArgumentException   Iff event is not active, is in the past or right now, is not active
     *                                    or is not inserted.
     */
    public void cancelEvent(Event event) {
        if (state != SETUP && state != RUNNING) {
            throw new IllegalStateException("Cannot cancel event if the simulation is not being setup or is running.");
        } else if (event.getSimulator() != this) {
            throw new IllegalArgumentException("Event " + event + " is not bound to this simulator.");
        } else if (event.getTime() < now) {
            throw new IllegalArgumentException("Cannot cancel event if it is in the past.");
        } else if (event.getTime() == now && state == RUNNING) {
            throw new IllegalArgumentException("Cannot cancel event if it is right now.");
        } else if (!event.isActive()) {
            throw new IllegalArgumentException("Cannot cancel event if it is not active.");
        } else if (!event.wasInserted()) {
            throw new IllegalArgumentException("Cannot cancel event if it is not inserted in the event queue.");
        }
        event.cancel();
    }

}
