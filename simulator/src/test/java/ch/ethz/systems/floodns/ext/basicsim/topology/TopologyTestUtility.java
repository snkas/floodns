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

package ch.ethz.systems.floodns.ext.basicsim.topology;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class TopologyTestUtility {

    /**
     * Create set instance.
     *
     * @param integers  Integers into set
     *
     * @return  Set of integers
     */
    static Set<Integer> createSet(Integer... integers) {
        Set<Integer> res = new HashSet<>();
        Collections.addAll(res, integers);
        return res;
    }

    /**
     * Construct a complete topology.
     *
     * @param numNodes              Number of nodes
     * @param numUndirectedEdges    Number of undirected edges
     * @param servers               Servers parameter value
     * @param switches              Switches parameter value
     * @param switchesWhichAreTors  ToRs parameter value
     * @param undirectedEdges       Undirected edges
     * @param linkCapacityUniform   Uniform link capacity
     *
     * @return  Topology
     *
     * @throws IOException  Thrown iff temporary file I/O failed
     */
    public static Topology constructTopology(
            int numNodes,
            int numUndirectedEdges,
            String servers,
            String switches,
            String switchesWhichAreTors,
            String undirectedEdges,
            double linkCapacityUniform
    ) throws IOException {

        // Create topology file
        File tempTopology = constructTopologyFile(numNodes, numUndirectedEdges, servers, switches, switchesWhichAreTors, undirectedEdges, String.valueOf(linkCapacityUniform));

        // Create topology
        Topology res = FileToTopologyConverter.convert(tempTopology.getAbsolutePath());

        // Clean-up of file
        assertTrue(tempTopology.delete());

        return res;

    }

    /**
     * Construct a complete topology.
     *
     * @param numNodes              Number of nodes
     * @param numUndirectedEdges    Number of edges
     * @param servers               Servers parameter value
     * @param switches              Switches parameter value
     * @param switchesWhichAreTors  ToRs parameter value
     * @param undirectedEdges       Undirected edges
     * @param linkDataRateBitPerNs  Link data rate
     *
     * @return  Topology
     *
     * @throws IOException  Thrown iff temporary file I/O failed
     */
    public static File constructTopologyFile(
            int numNodes,
            int numUndirectedEdges,
            String servers,
            String switches,
            String switchesWhichAreTors,
            String undirectedEdges,
            String linkDataRateBitPerNs
    ) throws IOException {


        // Create temporary files
        File tempTopology = File.createTempFile("topology", ".tmp");

        // Write temporary topology file
        BufferedWriter topologyWriter = new BufferedWriter(new FileWriter(tempTopology));
        topologyWriter.write("# A comment line followed by a white line\n\n");
        topologyWriter.write("num_nodes=" + numNodes + "\n");
        topologyWriter.write("num_undirected_edges=" + numUndirectedEdges + "\n");
        topologyWriter.write("servers=" + servers + "\n");
        topologyWriter.write("switches=" + switches + "\n");
        topologyWriter.write("switches_which_are_tors=" + switchesWhichAreTors + "\n");
        topologyWriter.write("undirected_edges=" + undirectedEdges + "\n");
        topologyWriter.write("link_data_rate_bit_per_ns=" + linkDataRateBitPerNs + "\n");
        topologyWriter.close();

        // Return temporary topology file
        return tempTopology;

    }

}
