package ch.ethz.systems.floodns.user.simple;

import ch.ethz.systems.floodns.core.Aftermath;
import ch.ethz.systems.floodns.core.Network;
import ch.ethz.systems.floodns.core.Simulator;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class AftermathLegalityTest {

    @Test
    public void testLegality() {
        Simulator simulator = new Simulator();
        Network network = new Network(3);

        // Normal
        new Aftermath(simulator, network) {
            @Override
            public void perform() {

            }
        };

        // Null network
        boolean thrown = false;
        try {
            new Aftermath(simulator, null) {
                @Override
                public void perform() {

                }
            };
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Null simulator
        thrown = false;
        try {
        new Aftermath(null, network) {
            @Override
            public void perform() {

            }
        };
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // Both network and simulator null
        thrown = false;
        try {
            new Aftermath(null, null) {
                @Override
                public void perform() {

                }
            };
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

}
