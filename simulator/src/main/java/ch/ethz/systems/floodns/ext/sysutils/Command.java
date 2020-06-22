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

package ch.ethz.systems.floodns.ext.sysutils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class Command {

    // Class logger
    private static final Logger logger = LogManager.getLogger(Command.class);

    /**
     * Run a command in the prompt (e.g. to call a python script) and write the resulting normal output to file.
     * Error write output is always shown.
     *
     * @param cmd               Command
     * @param outputFilename    Output filename
     * @param enableLogInfo     True iff to enable log info
     */
    public static void runCommandWriteOutput(String cmd, String outputFilename, boolean enableLogInfo) {

        Process p;
        try {

            if (enableLogInfo) {
                logger.info("Running command \"" + cmd + "\"...");
            }

            // Start process
            p = Runtime.getRuntime().exec(cmd);

            // Fetch input streams
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // Output writer
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename));

            // Write the output from the attempted command to file
            int s;
            while ((s = stdInput.read()) != -1) {
                writer.write(s);
            }

            // Show any errors from the attempted command
            String line;
            while ((line = stdError.readLine()) != null) {
                logger.error(line);
            }

            // Wait for the command thread to be ended
            p.waitFor();
            p.destroy();

            if (enableLogInfo) {
                logger.info("... command has been executed (any error output is shown above).");
                logger.info("... output is written to file '" + outputFilename + "'.");
            }

            writer.close();

        } catch (Exception e) {
            throw new RuntimeException("Command failed: " + cmd, e);
        }

    }

}
