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

import numpy as np
import os
import sys
import exputil


def analyze_flow_info(logs_floodns_dir, analysis_folder_dir):

    # Read in all the columns
    flows_info_csv_columns = exputil.read_csv_direct_in_columns(
        logs_floodns_dir + '/flow_info.csv.log',
        "pos_int,pos_int,pos_int,string,pos_int,pos_int,pos_int,pos_float,pos_float,string"
    )
    flow_id_list = flows_info_csv_columns[0]
    source_id_list = flows_info_csv_columns[1]
    target_id_list = flows_info_csv_columns[2]
    path_list = flows_info_csv_columns[3]
    path_length_list = list(map(lambda x: len(x.split(">")) - 1, path_list))
    # start_time_list = flows_info_csv_columns[4]
    # end_time_list = flows_info_csv_columns[5]
    # duration_list = flows_info_csv_columns[6]
    # total_sent_list = flows_info_csv_columns[7]
    avg_throughput_list = flows_info_csv_columns[8]
    # metadata_list = flows_info_csv_columns[9]

    # Calculate some statistics
    if len(flow_id_list) == 0:
        statistics = {
            'all_num_flows': len(flow_id_list)
        }
        
    else:
        statistics = {
            'all_num_flows': len(flow_id_list),
            'all_flow_num_unique_sources': len(set(source_id_list)),
            'all_flow_num_unique_targets': len(set(target_id_list)),
    
            'all_flow_avg_throughput_sum': sum(avg_throughput_list),
            'all_flow_avg_throughput_min': np.min(avg_throughput_list),
            'all_flow_avg_throughput_0.1th': np.percentile(avg_throughput_list, 0.1),
            'all_flow_avg_throughput_1th': np.percentile(avg_throughput_list, 1),
            'all_flow_avg_throughput_mean': np.mean(avg_throughput_list),
            'all_flow_avg_throughput_median': np.median(avg_throughput_list),
            'all_flow_avg_throughput_99th': np.percentile(avg_throughput_list, 99),
            'all_flow_avg_throughput_99.9th': np.percentile(avg_throughput_list, 99.9),
            'all_flow_avg_throughput_max': np.max(avg_throughput_list),
    
            'all_flow_path_length_min': np.min(path_length_list),
            'all_flow_path_length_0.1th': np.percentile(path_length_list, 0.1),
            'all_flow_path_length_1th': np.percentile(path_length_list, 1),
            'all_flow_path_length_mean': np.mean(path_length_list),
            'all_flow_path_length_median': np.median(path_length_list),
            'all_flow_path_length_99th': np.percentile(path_length_list, 99),
            'all_flow_path_length_99.9th': np.percentile(path_length_list, 99.9),
            'all_flow_path_length_max': np.max(path_length_list),
        }
    
    # Print results
    output_filename = analysis_folder_dir + '/flow_info.statistics'
    print('Writing flow statistics: ' + output_filename)
    with open(output_filename, 'w+') as outfile:
        for key, value in sorted(statistics.items()):
            outfile.write(str(key) + "=" + str(value) + "\n")


def analyze_connection_info(logs_floodns_dir, analysis_folder_dir):

    # Read in all the columns
    flows_info_csv_columns = exputil.read_csv_direct_in_columns(
        logs_floodns_dir + '/connection_info.csv.log',
        "pos_int,pos_int,pos_int,pos_float,pos_float,string,pos_int,pos_int,pos_int,pos_float,string,string"
    )
    connection_id_list = flows_info_csv_columns[0]
    source_id_list = flows_info_csv_columns[1]
    target_id_list = flows_info_csv_columns[2]
    # total_size_list = flows_info_csv_columns[3]
    # total_sent_list = flows_info_csv_columns[4]
    # flows_string_list = flows_info_csv_columns[5]
    # num_flows_list = list(map(lambda x: len(x.split(";")), flows_string_list))
    # start_time_list = flows_info_csv_columns[6]
    # end_time_list = flows_info_csv_columns[7]
    duration_list = flows_info_csv_columns[8]
    avg_throughput_list = flows_info_csv_columns[9]
    completed_string_list = flows_info_csv_columns[10]
    completed_list = []
    count_completed = 0
    count_incomplete = 0
    for c in completed_string_list:
        if c == "T":
            completed_list.append(True)
            count_completed += 1
        elif c == "F":
            completed_list.append(False)
            count_incomplete += 1
        else:
            raise ValueError("Invalid completed value: " + c)
    # metadata_list = flows_info_csv_columns[11]

    # Calculate some statistics
    if len(connection_id_list) == 0:
        statistics = {
            'all_num_connections': len(connection_id_list),
        }
        
    else:

        statistics = {
            'all_num_connections': len(connection_id_list),
            'all_num_connections_completed': count_completed,
            'all_num_connections_incomplete': count_incomplete,
            'all_num_connections_fraction_completed': float(count_completed) / float(len(connection_id_list)),
            'all_connection_num_unique_sources': len(set(source_id_list)),
            'all_connection_num_unique_targets': len(set(target_id_list)),

            'all_connection_avg_throughput_min': np.min(avg_throughput_list),
            'all_connection_avg_throughput_0.1th': np.percentile(avg_throughput_list, 0.1),
            'all_connection_avg_throughput_1th': np.percentile(avg_throughput_list, 1),
            'all_connection_avg_throughput_mean': np.mean(avg_throughput_list),
            'all_connection_avg_throughput_median': np.median(avg_throughput_list),
            'all_connection_avg_throughput_99th': np.percentile(avg_throughput_list, 99),
            'all_connection_avg_throughput_99.9th': np.percentile(avg_throughput_list, 99.9),
            'all_connection_avg_throughput_max': np.max(avg_throughput_list),
            'all_connection_avg_throughput_sum': sum(avg_throughput_list),
        }

        completion_time = []
        completion_throughput = []
        for i in range(len(connection_id_list)):
            if completed_list[i]:
                completion_time.append(duration_list[i])
                completion_throughput.append(avg_throughput_list[i])

        if count_completed > 0:
            statistics.update({
                'completed_connection_completion_time_min': np.min(completion_time),
                'completed_connection_completion_time_0.1th': np.percentile(completion_time, 0.1),
                'completed_connection_completion_time_1th': np.percentile(completion_time, 1),
                'completed_connection_completion_time_mean': np.mean(completion_time),
                'completed_connection_completion_time_median': np.median(completion_time),
                'completed_connection_completion_time_99th': np.percentile(completion_time, 99),
                'completed_connection_completion_time_99.9th': np.percentile(completion_time, 99.9),
                'completed_connection_completion_time_max': np.max(completion_time),

                'completed_connection_throughput_min': np.min(completion_throughput),
                'completed_connection_throughput_0.1th': np.percentile(completion_throughput, 0.1),
                'completed_connection_throughput_1th': np.percentile(completion_throughput, 1),
                'completed_connection_throughput_mean': np.mean(completion_throughput),
                'completed_connection_throughput_median': np.median(completion_throughput),
                'completed_connection_throughput_99th': np.percentile(completion_throughput, 99),
                'completed_connection_throughput_99.9th': np.percentile(completion_throughput, 99.9),
                'completed_connection_throughput_max': np.max(completion_throughput),
            })

    # Print raw results
    output_filename = analysis_folder_dir + '/connection_info.statistics'
    print('Writing connection statistics: %s' % output_filename)
    with open(output_filename, 'w+') as outfile:
        for key, value in sorted(statistics.items()):
            outfile.write(str(key) + "=" + str(value) + "\n")


def analyze_link_info(logs_floodns_dir, analysis_folder_dir):

    # Read in all the columns
    link_info_csv_columns = exputil.read_csv_direct_in_columns(
        logs_floodns_dir + '/link_info.csv.log',
        "pos_int,pos_int,pos_int,pos_int,pos_int,pos_int,pos_float,pos_float,string"
    )
    link_id_list = link_info_csv_columns[0]
    source_id_list = link_info_csv_columns[1]
    target_id_list = link_info_csv_columns[2]
    # start_time_list = link_info_csv_columns[3]
    # end_time_list = link_info_csv_columns[4]
    # duration_list = link_info_csv_columns[5]
    avg_utilization_list = link_info_csv_columns[6]
    # avg_active_flows_list = link_info_csv_columns[7]
    # metadata_list = link_info_csv_columns[8]

    # Count how many links had utilization of zero
    num_link_inactive = 0
    num_link_active = 0
    for u in avg_utilization_list:
        if u == 0:
            num_link_inactive += 1
        else:
            num_link_active += 1

    # Calculate some statistics
    if len(link_id_list) == 0:
        statistics = {
            'all_num_links': len(link_id_list),
        }
    else:

        # General statistics
        statistics = {
            'all_num_links': len(link_id_list),
            'all_num_links_active': num_link_active,
            'all_num_links_inactive': num_link_inactive,
            'all_link_unique_sources': len(set(source_id_list)),
            'all_link_unique_targets': len(set(target_id_list)),

            'all_link_avg_utilization_min': np.min(avg_utilization_list),
            'all_link_avg_utilization_0.1th': np.percentile(avg_utilization_list, 0.1),
            'all_link_avg_utilization_1th': np.percentile(avg_utilization_list, 1),
            'all_link_avg_utilization_mean': np.mean(avg_utilization_list),
            'all_link_avg_utilization_median': np.median(avg_utilization_list),
            'all_link_avg_utilization_std': np.std(avg_utilization_list),
            'all_link_avg_utilization_99th': np.percentile(avg_utilization_list, 99),
            'all_link_avg_utilization_99.9th': np.percentile(avg_utilization_list, 99.9),
            'all_link_avg_utilization_max': np.max(avg_utilization_list),
        }

    # Print raw results
    output_filename = analysis_folder_dir + '/link_info.statistics'
    print('Writing link statistics: %s' % output_filename)
    with open(output_filename, 'w+') as outfile:
        for key, value in sorted(statistics.items()):
            outfile.write(str(key) + "=" + str(value) + "\n")


def analyze_node_info(logs_floodns_dir, analysis_folder_dir):

    # Read in all the columns
    link_info_csv_columns = exputil.read_csv_direct_in_columns(
        logs_floodns_dir + '/node_info.csv.log',
        "pos_int,pos_float,string"
    )
    node_id_list = link_info_csv_columns[0]
    avg_active_flows_list = link_info_csv_columns[1]
    # metadata_list = link_info_csv_columns[2]

    # Count how many nodes did not see any flows
    num_node_inactive = 0
    num_node_active = 0
    for a in avg_active_flows_list:
        if a == 0:
            num_node_inactive += 1
        else:
            num_node_active += 1

    # Calculate some statistics
    if len(node_id_list) == 0:
        statistics = {
            'all_num_nodes': len(node_id_list),
        }
    else:

        # General statistics
        statistics = {
            'all_num_nodes': len(node_id_list),
            'all_num_nodes_active': num_node_active,
            'all_num_nodes_inactive': num_node_inactive,

            'all_node_avg_num_active_flows_min': np.min(avg_active_flows_list),
            'all_node_avg_num_active_flows_1th': np.percentile(avg_active_flows_list, 1),
            'all_node_avg_num_active_flows_0.1th': np.percentile(avg_active_flows_list, 0.1),
            'all_node_avg_num_active_flows_mean': np.mean(avg_active_flows_list),
            'all_node_avg_num_active_flows_median': np.median(avg_active_flows_list),
            'all_node_avg_num_active_flows_std': np.std(avg_active_flows_list),
            'all_node_avg_num_active_flows_99th': np.percentile(avg_active_flows_list, 99),
            'all_node_avg_num_active_flows_99.9th': np.percentile(avg_active_flows_list, 99.9),
            'all_node_avg_num_active_flows_max': np.max(avg_active_flows_list),
        }

    # Print raw results
    output_filename = analysis_folder_dir + '/node_info.statistics'
    print('Writing node statistics: %s' % output_filename)
    with open(output_filename, 'w+') as outfile:
        for key, value in sorted(statistics.items()):
            outfile.write(str(key) + "=" + str(value) + "\n")


def main():
    args = sys.argv[1:]
    if len(args) != 1:
        print("Must supply exactly one argument")
        print("Usage: python analyze.py [/path/to/run_folder/logs_floodns]")
        exit(1)
    else:

        # Check run folder path given as first argument
        logs_floodns_dir = sys.argv[1]
        if not os.path.isdir(logs_floodns_dir):
            print("The logs_floodns directory does not exist: " + logs_floodns_dir)
            exit()

        # Create analysis folder
        analysis_folder_dir = logs_floodns_dir + '/analysis'
        if not os.path.exists(analysis_folder_dir):
            os.makedirs(analysis_folder_dir)
        print("Output directory for analysis: " + analysis_folder_dir)

        # Perform all four analyses
        analyze_flow_info(logs_floodns_dir, analysis_folder_dir)
        analyze_connection_info(logs_floodns_dir, analysis_folder_dir)
        analyze_link_info(logs_floodns_dir, analysis_folder_dir)
        analyze_node_info(logs_floodns_dir, analysis_folder_dir)


if __name__ == "__main__":
    main()
