##
# The MIT License (MIT)
#
# Copyright (c) 2019 snkas
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
##

import csv
import numpy as np
import os
import sys


# Usage print
def print_usage():
    print("floodns python analysis tool v0.01.6")
    print("Usage: python analyze.py /path/to/run/folder")


# Check length of arguments
if len(sys.argv) != 2:
    print("Number of arguments must be exactly two: analyze.py and /path/to/run/folder.")
    print_usage()
    exit()

# Check run folder path given as first argument
run_folder_path = sys.argv[1]
if not os.path.isdir(run_folder_path):
    print("The run folder path does not exist: " + run_folder_path)
    print_usage()
    exit()

# Create analysis folder
analysis_folder_path = run_folder_path + '/analysis'
if not os.path.exists(analysis_folder_path):
    os.makedirs(analysis_folder_path)


##################################
# Analyze flow information
#
def analyze_flow_info():
    with open(run_folder_path + '/flow_info.csv.log') as f:
        reader = csv.reader(f)

        # Column lists
        flow_ids = []
        source_ids = []
        target_ids = []
        paths = []
        paths_lengths = []
        start_time = []
        end_time = []
        duration = []
        total_sent = []
        avg_throughput = []

        print("Reading in flow info log file...")

        # Read in column lists
        for row in reader:
            flow_ids.append(float(row[0]))
            source_ids.append(float(row[1]))
            target_ids.append(float(row[2]))
            paths.append(row[3])
            paths_lengths.append(len(row[3].split(">")) - 1)
            start_time.append(float(row[4]))
            end_time.append(float(row[5]))
            duration.append(float(row[6]))
            total_sent.append(float(row[7]))
            avg_throughput.append(float(row[8]))
            if len(row) != 10:
                print("Invalid row: ", row)
                exit()

        print("Calculating statistics...")

        statistics = {
            'all_flow_num': len(flow_ids),
            'all_flow_num_unique_sources': len(set(source_ids)),
            'all_flow_num_unique_targets': len(set(target_ids)),
            'all_flow_avg_throughput_total': sum(avg_throughput),
            'all_flow_avg_throughput_mean': np.mean(avg_throughput),
            'all_flow_avg_throughput_median': np.median(avg_throughput),
            'all_flow_avg_throughput_99th': np.percentile(avg_throughput, 99),
            'all_flow_avg_throughput_99.9th': np.percentile(avg_throughput, 99.9),
            'all_flow_avg_throughput_1th': np.percentile(avg_throughput, 1),
            'all_flow_avg_throughput_0.1th': np.percentile(avg_throughput, 0.1),
            'all_flow_path_length_mean': np.mean(paths_lengths),
            'all_flow_path_length_median': np.median(paths_lengths),
            'all_flow_path_length_99th': np.percentile(paths_lengths, 99),
            'all_flow_path_length_99.9th': np.percentile(paths_lengths, 99.9),
            'all_flow_path_length_1th': np.percentile(paths_lengths, 1),
            'all_flow_path_length_0.1th': np.percentile(paths_lengths, 0.1),
        }

        # Print raw results
        print('Writing to result file flow_info.statistics...')
        with open(analysis_folder_path + '/flow_info.statistics', 'w+') as outfile:
            for key, value in sorted(statistics.items()):
                outfile.write(str(key) + "=" + str(value) + "\n")


##################################
# Analyze connection information
#
def analyze_connection_info(lower_threshold, upper_threshold):
    with open(run_folder_path + '/connection_info.csv.log') as f:
        reader = csv.reader(f)

        # Column lists
        connection_ids = []
        source_ids = []
        target_ids = []
        total_size = []
        total_sent = []
        own_flows = []
        own_flows_count = []
        start_time = []
        end_time = []
        duration = []
        fct = []
        throughput = []
        completed_throughput = []
        completed = []

        print("Reading in connection info log file...")

        # Read in column lists
        count_completed = 0
        count_incomplete = 0
        for row in reader:
            if lower_threshold <= float(row[3]) <= upper_threshold:
                connection_ids.append(float(row[0]))
                source_ids.append(float(row[1]))
                target_ids.append(float(row[2]))
                total_size.append(float(row[3]))
                total_sent.append(float(row[4]))
                flow_ids_list = row[5].split(";")
                own_flows_count.append(len(flow_ids_list))
                own_flows.append(flow_ids_list)
                start_time.append(float(row[6]))
                end_time.append(float(row[7]))
                duration.append(float(row[8]))
                if row[10] == 'T':
                    fct.append(float(row[8]))
                    completed_throughput.append(float(row[9]))
                throughput.append(float(row[9]))
                completed.append(row[10] == 'T')
                if row[10] == 'T':
                    count_completed = count_completed + 1
                else:
                    count_incomplete = count_incomplete + 1
                if len(row) != 12:
                    print("Invalid row: ", row)
                    exit()

        print("Calculating statistics...")

        if len(connection_ids) > 0:

            statistics = {
                'all_num_connections': len(connection_ids),
                'all_num_connections_completed': count_completed,
                'all_num_connections_incomplete': count_incomplete,
                'all_num_connections_fraction_completed': float(count_completed) / float(len(connection_ids)),
                'all_connection_num_unique_sources': len(set(source_ids)),
                'all_connection_num_unique_targets': len(set(target_ids)),
                'all_connection_throughput_total': sum(throughput),
                'all_connection_throughput_mean': np.mean(throughput),
                'all_connection_throughput_median': np.median(throughput),
                'all_connection_throughput_99th': np.percentile(throughput, 99),
                'all_connection_throughput_99.9th': np.percentile(throughput, 99.9),
                'all_connection_throughput_1th': np.percentile(throughput, 1),
                'all_connection_throughput_0.1th': np.percentile(throughput, 0.1),
                'all_connection_num_flows_total': sum(own_flows_count),
                'all_connection_num_flows_mean': np.mean(own_flows_count),
                'all_connection_num_flows_median': np.median(own_flows_count),
                'all_connection_num_flows_99th': np.percentile(own_flows_count, 99),
                'all_connection_num_flows_99.9th': np.percentile(own_flows_count, 99.9),
                'all_connection_num_flows_1th': np.percentile(own_flows_count, 1),
                'all_connection_num_flows_0.1th': np.percentile(own_flows_count, 0.1),
                'completed_connection_fct_mean': np.mean(fct),
                'completed_connection_fct_median': np.median(fct),
                'completed_connection_fct_99th': np.percentile(fct, 99),
                'completed_connection_fct_99.9th': np.percentile(fct, 99.9),
                'completed_connection_fct_1th': np.percentile(fct, 1),
                'completed_connection_fct_0.1th': np.percentile(fct, 0.1),
                'completed_connection_throughput_mean': np.mean(completed_throughput),
                'completed_connection_throughput_median': np.median(completed_throughput),
                'completed_connection_throughput_99th': np.percentile(completed_throughput, 99),
                'completed_connection_throughput_99.9th': np.percentile(completed_throughput, 99.9),
                'completed_connection_throughput_1th': np.percentile(completed_throughput, 1),
                'completed_connection_throughput_0.1th': np.percentile(completed_throughput, 0.1),
            }
        else:
            statistics = {
                'all_num_connections': 0
            }

        # Print raw results
        statistics_filename = format(
            "%s/connection_info_lb_%d_ub_%d.statistics" %
            (analysis_folder_path,
             lower_threshold,
             upper_threshold)
        )

        print('Writing to result file %s...' % statistics_filename)
        with open(statistics_filename, 'w+') as outfile:
            for key, value in sorted(statistics.items()):
                outfile.write(str(key) + "=" + str(value) + "\n")


##################################
# Analyze link information
#
def analyze_link_info():
    with open(run_folder_path + '/link_info.csv.log') as f:
        reader = csv.reader(f)

        # Column lists
        link_id = []
        source_id = []
        target_id = []
        start_time = []
        end_time = []
        duration = []
        avg_utilization = []
        avg_active_flows = []
        num_link_utilization_zero = 0
        num_link_utilization_non_zero = 0

        print("Reading in link info log file...")

        # Read in column lists
        for row in reader:
            link_id.append(float(row[0]))
            source_id.append(float(row[1]))
            target_id.append(float(row[2]))
            start_time.append(float(row[3]))
            end_time.append(float(row[4]))
            duration.append(float(row[5]))
            avg_utilization.append(float(row[6]))
            avg_active_flows.append(float(row[7]))
            if float(row[6]) == 0:
                num_link_utilization_zero = num_link_utilization_zero + 1
            else:
                num_link_utilization_non_zero = num_link_utilization_non_zero + 1

            if len(row) != 9:
                print("Invalid row: ", row)
                exit()

        print("Calculating statistics...")

        # General statistics
        statistics = {
            'all_link_num': len(link_id),
            'all_link_num_active': num_link_utilization_non_zero,
            'all_link_num_inactive': num_link_utilization_zero,
            'all_link_unique_sources': len(set(source_id)),
            'all_link_unique_targets': len(set(target_id)),
            'all_link_avg_utilization_mean': np.mean(avg_utilization),
            'all_link_avg_utilization_median': np.median(avg_utilization),
            'all_link_avg_utilization_std': np.std(avg_utilization),
            'all_link_avg_utilization_99th': np.percentile(avg_utilization, 99),
            'all_link_avg_utilization_99.9th': np.percentile(avg_utilization, 99.9),
            'all_link_avg_utilization_1th': np.percentile(avg_utilization, 1),
            'all_link_avg_utilization_0.1th': np.percentile(avg_utilization, 0.1),
        }

        # Print raw results
        print('Writing to result file link_info.statistics...')
        with open(analysis_folder_path + '/link_info.statistics', 'w+') as outfile:
            for key, value in sorted(statistics.items()):
                outfile.write(str(key) + "=" + str(value) + "\n")


##################################
# Analyze node information
#
def analyze_node_info():
    with open(run_folder_path + '/node_info.csv.log') as f:
        reader = csv.reader(f)

        # Column lists
        node_id = []
        avg_active_flows = []
        num_node_active = 0
        num_node_inactive = 0

        print("Reading in node info log file...")

        # Read in column lists
        for row in reader:
            node_id.append(float(row[0]))
            avg_active_flows.append(float(row[1]))
            if float(row[1]) == 0:
                num_node_inactive = num_node_inactive + 1
            else:
                num_node_active = num_node_active + 1

            if len(row) != 3:
                print("Invalid row: ", row)
                exit()

        print("Calculating statistics...")

        # General statistics
        statistics = {
            'all_node_num': len(node_id),
            'all_node_num_active': num_node_active,
            'all_node_num_inactive': num_node_inactive,
            'all_node_avg_num_active_flows_mean': np.mean(avg_active_flows),
            'all_node_avg_num_active_flows_median': np.median(avg_active_flows),
            'all_node_avg_num_active_flows_std': np.std(avg_active_flows),
            'all_node_avg_num_active_flows_99th': np.percentile(avg_active_flows, 99),
            'all_node_avg_num_active_flows_99.9th': np.percentile(avg_active_flows, 99.9),
            'all_node_avg_num_active_flows_1th': np.percentile(avg_active_flows, 1),
            'all_node_avg_num_active_flows_0.1th': np.percentile(avg_active_flows, 0.1),
        }

        # Print raw results
        print('Writing to result file node_info.statistics...')
        with open(analysis_folder_path + '/node_info.statistics', 'w+') as outfile:
            for key, value in sorted(statistics.items()):
                outfile.write(str(key) + "=" + str(value) + "\n")


##################################
# Call analysis functions
#
analyze_flow_info()
analyze_connection_info(0, 10000000000)
analyze_link_info()
analyze_node_info()
