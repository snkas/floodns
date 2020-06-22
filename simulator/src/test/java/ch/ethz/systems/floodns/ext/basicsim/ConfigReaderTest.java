package ch.ethz.systems.floodns.ext.basicsim;

import ch.ethz.systems.floodns.core.Node;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class ConfigReaderTest {

    @Test
    public void testConfig() throws IOException {
        String run_dir = Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString();

        PrintWriter writerConfig;
        boolean thrown = false;

        writerConfig = new PrintWriter(new FileWriter(run_dir + "/config_floodns.properties"));
        writerConfig.write("filename_topology=\"topology.properties\"\n");
        writerConfig.write("filename_schedule=schedule.csv\n");
        writerConfig.write("simulation_end_time_ns=500000000000\n");
        writerConfig.write("simulation_seed=123456789\n");
        writerConfig.write("link_data_rate_bit_per_ns=12.12523\n");
        writerConfig.close();

        NoDuplicatesProperties config = new NoDuplicatesProperties();
        FileInputStream fileInputStream = new FileInputStream(run_dir + "/config_floodns.properties");
        config.load(fileInputStream);
        fileInputStream.close();

        assertEquals("topology.properties", config.getStringOrFail("filename_topology"));
        assertEquals("schedule.csv", config.getStringOrFail("filename_schedule"));
        assertEquals(500000000000L, config.getPositiveLongOrFail("simulation_end_time_ns"));
        assertEquals(123456789L, config.getPositiveLongOrFail("simulation_seed"));
        assertEquals(12.12523, config.getPositiveDoubleOrFail("link_data_rate_bit_per_ns"), 0.00000001);

    }

    @Test
    public void testConfigDuplicates() throws IOException {
        String run_dir = Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString();

        PrintWriter writerConfig;
        boolean thrown = false;

        writerConfig = new PrintWriter(new FileWriter(run_dir + "/config_floodns.properties"));
        writerConfig.write("filename_topology=\"topology.properties\"\n");
        writerConfig.write("filename_schedule=schedule.csv\n");
        writerConfig.write("filename_schedule=schedule2.csv\n");
        writerConfig.write("simulation_end_time_ns=500000000000\n");
        writerConfig.write("simulation_seed=123456789\n");
        writerConfig.write("link_data_rate_bit_per_ns=12.12523\n");
        writerConfig.close();

        NoDuplicatesProperties config = new NoDuplicatesProperties();
        FileInputStream fileInputStream = new FileInputStream(run_dir + "/config_floodns.properties");
        thrown = false;
        try {
            config.load(fileInputStream);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    @Test
    public void testValidate() throws IOException {
        String run_dir = Files.createTempDirectory("temp_run_dir").toAbsolutePath().toString();

        PrintWriter writerConfig;
        boolean thrown;

        writerConfig = new PrintWriter(new FileWriter(run_dir + "/config_floodns.properties"));
        writerConfig.write("a=\"topology.properties\"\n");
        writerConfig.write("b=schedule.csv\n");
        writerConfig.write("c=500000000000\n");
        writerConfig.write("d=-123456789\n");
        writerConfig.write("e=-12.12523\n");
        writerConfig.close();

        NoDuplicatesProperties config = new NoDuplicatesProperties();
        FileInputStream fileInputStream = new FileInputStream(run_dir + "/config_floodns.properties");
        config.load(fileInputStream);
        config.validate(new String[]{"a", "b", "c", "d", "e"});

        thrown = false;
        try {
            config.validate(new String[]{"a", "b", "c", "d"});
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue(thrown);

        thrown = false;
        try {
            config.validate(new String[]{"a", "b", "f", "c", "d", "e"});
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue(thrown);

        thrown = false;
        try {
            config.getStringOrFail("f");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        thrown = false;
        try {
            config.getPositiveDoubleOrFail("e");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        thrown = false;
        try {
            config.getPositiveLongOrFail("d");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        thrown = false;
        try {
            config.getPositiveLongOrFail("e");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

}
