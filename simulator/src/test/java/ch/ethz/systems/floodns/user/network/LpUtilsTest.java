package ch.ethz.systems.floodns.user.network;

import ch.ethz.systems.floodns.PathTestUtility;
import ch.ethz.systems.floodns.core.Connection;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.ext.lputils.FullSplitEpsilonSumMaxLp;
import ch.ethz.systems.floodns.ext.lputils.FullSplitMaxMinConnBwLp;
import ch.ethz.systems.floodns.ext.lputils.FullSplitSumMaxLp;
import ch.ethz.systems.floodns.ext.lputils.GlopLpSolver;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class LpUtilsTest {

    @Test
    public void simpleStarMcffc() {
        Simulator simulator = new Simulator(1e-4);
        Network network = new Network(5);

        // 0   1
        //  \ /
        //   4
        //  / \
        // 2   3
        network.addLink(0, 4, 100);
        network.addLink(4, 0, 100);
        network.addLink(1, 4, 100);
        network.addLink(4, 1, 100);
        network.addLink(2, 4, 100);
        network.addLink(4, 2, 100);
        network.addLink(3, 4, 100);
        network.addLink(4, 3, 30);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add two connections and their flows
                Connection conn0 = new Connection(simulator, network.getNode(0), network.getNode(1), 100);
                Connection conn1 = new Connection(simulator, network.getNode(0), network.getNode(1), 100);
                Connection conn2 = new Connection(simulator, network.getNode(0), network.getNode(3), 100);
                Connection conn3 = new Connection(simulator, network.getNode(2), network.getNode(3), 100);
                Connection conn4 = new Connection(simulator, network.getNode(4), network.getNode(3), 100);
                Connection conn5 = new Connection(simulator, network.getNode(0), network.getNode(3), 100);
                simulator.activateConnection(conn0);
                simulator.activateConnection(conn1);
                simulator.activateConnection(conn2);
                simulator.activateConnection(conn3);
                simulator.activateConnection(conn4);
                simulator.activateConnection(conn5);

                FullSplitMaxMinConnBwLp lp = new FullSplitMaxMinConnBwLp(simulator, new GlopLpSolver("external/glop_solver.py", true));
                Map<Integer, Double> connToDemand = new HashMap<>();
                connToDemand.put(0, 40.0);
                connToDemand.put(1, 70.0);
                connToDemand.put(2, 20.0);
                connToDemand.put(3, 50.0);
                connToDemand.put(4, 10.0);
                connToDemand.put(5, 40.0);

                assertEquals(0.25, lp.calculateAlpha(connToDemand), 1e-6);

            }

        });

    }

    @Test
    public void simpleStarNaiveMax() {
        Simulator simulator = new Simulator(1e-4);
        Network network = new Network(5);

        // 0   1
        //  \ /
        //   4
        //  / \
        // 2   3
        network.addLink(0, 4, 100);
        network.addLink(4, 0, 100);
        network.addLink(1, 4, 100);
        network.addLink(4, 1, 100);
        network.addLink(2, 4, 100);
        network.addLink(4, 2, 100);
        network.addLink(3, 4, 100);
        network.addLink(4, 3, 30);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add two connections and their flows
                Connection conn0 = new Connection(simulator, network.getNode(0), network.getNode(1), 100);
                Connection conn1 = new Connection(simulator, network.getNode(0), network.getNode(1), 100);
                Connection conn2 = new Connection(simulator, network.getNode(0), network.getNode(3), 100);
                Connection conn3 = new Connection(simulator, network.getNode(2), network.getNode(3), 100);
                Connection conn4 = new Connection(simulator, network.getNode(4), network.getNode(3), 100);
                Connection conn5 = new Connection(simulator, network.getNode(0), network.getNode(3), 100);
                simulator.activateConnection(conn0);
                simulator.activateConnection(conn1);
                simulator.activateConnection(conn2);
                simulator.activateConnection(conn3);
                simulator.activateConnection(conn4);
                simulator.activateConnection(conn5);

                FullSplitSumMaxLp lp = new FullSplitSumMaxLp(simulator, new GlopLpSolver("external/glop_solver.py", true));
                Map<Integer, Double> connToDemand = new HashMap<>();
                connToDemand.put(0, 40.0);
                connToDemand.put(1, 70.0);
                connToDemand.put(2, 20.0);
                connToDemand.put(3, 50.0);
                connToDemand.put(4, 10.0);
                connToDemand.put(5, 40.0);

                assertEquals(130, lp.calculateMaxTotalThroughput(connToDemand), 1e-6);

                Map<Integer, Double> connIdToThroughput = lp.getMapConnectionToThroughput();
                double sum = 0.0;
                for (int i = 0; i < 6; i++) {
                    sum += connIdToThroughput.get(i);
                }
                assertEquals(130.0, sum, 1e-6);

            }

        });

    }

    @Test
    public void simpleStarNaiveMax2() {
        Simulator simulator = new Simulator(1e-4);
        Network network = new Network(5);

        // 0   1
        //  \ /
        //   4
        //  / \
        // 2   3
        network.addLink(0, 4, 100);
        network.addLink(4, 0, 100);
        network.addLink(1, 4, 100);
        network.addLink(4, 1, 100);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add two connections and their flows
                Connection conn0 = new Connection(simulator, network.getNode(0), network.getNode(4), 100);
                simulator.activateConnection(conn0);

                FullSplitSumMaxLp lp = new FullSplitSumMaxLp(simulator, new GlopLpSolver("external/glop_solver.py", true));
                Map<Integer, Double> connToDemand = new HashMap<>();
                connToDemand.put(0, 40.0);

                assertEquals(40, lp.calculateMaxTotalThroughput(connToDemand), 1e-6);

                Map<Integer, Double> connIdToThroughput = lp.getMapConnectionToThroughput();
                assertEquals(40.0, connIdToThroughput.get(0), 1e-6);

            }

        });

    }

    @Test
    public void simpleEpsilonMinFlow() {
        Simulator simulator = new Simulator(1e-4);
        Network network = new Network(2);
        network.addLink(0, 1, 950);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add two connections and their flows
                for (int i = 0; i < 200; i++) {
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1");
                }

                FullSplitEpsilonSumMaxLp lp = new FullSplitEpsilonSumMaxLp(simulator, new GlopLpSolver("external/glop_solver.py", true));
                double objective = lp.calculate((1.0 - 0.95) * (1.0 - 0.95));
                assertTrue(objective >= 4.75);

            }

        });

    }

    @Test
    public void simpleEpsilonMinFlowTwo() {
        Simulator simulator = new Simulator(1e-4);
        Network network = new Network(6);
        network.addLink(0, 1, 100);
        network.addLink(0, 5, 100); // Filler
        network.addLink(1, 2, 100); // Filler
        network.addLink(2, 5, 100); // Filler
        network.addLink(2, 3, 0.01);

        NetworkTestHelper.runTest(simulator, network, new TestBody(simulator, network) {

            @Override
            public void test() {

                // Add two connections and their flows
                for (int i = 0; i < 100; i++) {
                    PathTestUtility.startSimpleFlow(simulator, network, "0-1-2");
                }
                PathTestUtility.startSimpleFlow(simulator, network, "2-3");

                FullSplitEpsilonSumMaxLp lp = new FullSplitEpsilonSumMaxLp(simulator, new GlopLpSolver("external/glop_solver.py", true));
                double objective = lp.calculate((1.0 - 0.95) * (1.0 - 0.95));

                // Z * 100 + 0.01 = (1 - 0.05 * 0.05) * 101 * Z
                // 0.01 = 0.7475 * Z
                assertEquals(objective, 0.01/0.7475, 1e-6);

                Map<Integer, Double> mapConnectionToThroughput = lp.getMapConnectionToThroughput();
                double sum = 0.0;
                for (int i = 0; i < 101; i++) {
                    sum += mapConnectionToThroughput.get(i);
                }
                assertEquals(sum, (1.0 - (1.0 - 0.95) * (1.0 - 0.95)) * 101.0 * objective, 1e-4);

            }

        });

    }

}
