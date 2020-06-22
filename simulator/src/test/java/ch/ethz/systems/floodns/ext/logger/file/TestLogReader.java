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

package ch.ethz.systems.floodns.ext.logger.file;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestLogReader {

    private final FileLoggerFactory fileLoggerFactory;

    public TestLogReader(FileLoggerFactory fileLoggerFactory) {
        this.fileLoggerFactory = fileLoggerFactory;
    }

    public Map<Integer, Map<Pair<Long, Long>, Double>> getFlowIdToBandwidthLog() throws IOException {
        return getDoubleMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_FLOW_BANDWIDTH));
    }

    public Map<Integer, Map<Pair<Long, Long>, Double>> getLinkIdToUtilizationLog() throws IOException {
        return getDoubleMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_LINK_UTILIZATION));
    }

    public Map<Integer, Map<Pair<Long, Long>, Integer>> getLinkIdToNumActiveFlowsLog() throws IOException {
        return getIntegerMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_LINK_NUM_ACTIVE_FLOWS));
    }

    public Map<Integer, Map<Pair<Long, Long>, Integer>> getNodeIdToNumActiveFlowsLog() throws IOException {
        return getIntegerMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_NODE_NUM_ACTIVE_FLOWS));
    }

    public Map<Integer, Map<Pair<Long, Long>, Double>> getConnectionIdToBandwidthLog() throws IOException {
        return getDoubleMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_CONNECTION_BANDWIDTH));
    }

    public Map<Integer, List<String>> getNodeIdToInfoLog() throws IOException {
        return getIdToInfoMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_NODE_INFO));
    }

    public Map<Integer, List<String>> getLinkIdToInfoLog() throws IOException {
        return getIdToInfoMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_LINK_INFO));
    }

    public Map<Integer, List<String>> getFlowIdToInfoLog() throws IOException {
        return getIdToInfoMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_FLOW_INFO));
    }

    public Map<Integer, List<String>> getConnectionIdToInfoLog() throws IOException {
        return getIdToInfoMapping(fileLoggerFactory.completePath(FileLoggerFactory.FILE_NAME_CONNECTION_INFO));
    }

    public static Map<Integer, List<String>> getIdToInfoMapping(String fileName) throws IOException {

        Map<Integer, List<String>> mapping = new HashMap<>();

        // Open file stream
        FileReader input = new FileReader(fileName);
        BufferedReader br = new BufferedReader(input);

        // Go over parameter lines one-by-one, stop when encountering non-parameter lines
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            String spl[] = line.split(",");
            Integer identifier = Integer.valueOf(spl[0]);
            List<String> values = new ArrayList<>();
            Collections.addAll(values, spl);
            values.remove(0);
            mapping.put(identifier, values);
        }

        return mapping;

    }

    private static Map<Integer, Map<Pair<Long, Long>, Double>> getDoubleMapping(String fileName) throws IOException {

        Map<Integer, Map<Pair<Long, Long>, Double>> mapping = new HashMap<>();

        // Open file stream
        FileReader input = new FileReader(fileName);
        BufferedReader br = new BufferedReader(input);

        // Go over parameter lines one-by-one, stop when encountering non-parameter lines
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            String spl[] = line.split(",");

            int flowId = Integer.valueOf(spl[0]);
            long startTime = Long.valueOf(spl[1]);
            long endTime = Long.valueOf(spl[2]);

            // Create mapping if not exists
            Map<Pair<Long, Long>, Double> flowMapping = mapping.get(flowId);
            if (flowMapping == null) {
                flowMapping = new HashMap<>();
                mapping.put(flowId, flowMapping);
            }

            flowMapping.put(new ImmutablePair<>(startTime, endTime), Double.valueOf(spl[3]));

        }

        return mapping;

    }

    private static Map<Integer, Map<Pair<Long, Long>, Integer>> getIntegerMapping(String fileName) throws IOException {

        Map<Integer, Map<Pair<Long, Long>, Integer>> mapping = new HashMap<>();

        // Open file stream
        FileReader input = new FileReader(fileName);
        BufferedReader br = new BufferedReader(input);

        // Go over parameter lines one-by-one, stop when encountering non-parameter lines
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            String spl[] = line.split(",");

            int flowId = Integer.valueOf(spl[0]);
            long startTime = Long.valueOf(spl[1]);
            long endTime = Long.valueOf(spl[2]);

            // Create mapping if not exists
            Map<Pair<Long, Long>, Integer> flowMapping = mapping.get(flowId);
            if (flowMapping == null) {
                flowMapping = new HashMap<>();
                mapping.put(flowId, flowMapping);
            }

            flowMapping.put(new ImmutablePair<>(startTime, endTime), Integer.valueOf(spl[3]));

        }

        return mapping;

    }

}
