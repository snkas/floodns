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

import os
import sys
from multiprocessing.dummy import Pool as ThreadPool
from subprocess import call


# Usage print
def print_usage():
    print("floodns python multi-analysis tool v0.01.6")
    print("Usage: python multi_analyze.py <analyze.py> <threads i.e.> /path/to/folder/of/run/folders")


# Analyze a list of folders
def analyze(folders):
    i = 1
    for run_folder_path in folders:
        call(["python3", analyze_py, run_folder_path])
        print("Progress " + str(i) + "/" + str(len(folders)))
        i = i + 1


# Main command-line flow
def main():

    # Check length of arguments
    if len(sys.argv) != 4:
        print("Number of arguments must be exactly three: multi_analyze.py <analyze.py> <threads i.e. 4> /path/to/folder/of/run/folders")
        print_usage()
        exit()

    global analyze_py
    analyze_py = sys.argv[1]

    # Number of threads is first argument
    num_threads = int(sys.argv[2])

    # Check collection run folder path given as second argument
    coll_run_folder_path = sys.argv[3]
    if not os.path.isdir(coll_run_folder_path):
        print("The folder path does not exist: " + coll_run_folder_path)
        print_usage()
        exit()

    # List of all sub-folders
    sub_folders = [coll_run_folder_path + "/" + name for name in os.listdir(coll_run_folder_path)
                   if os.path.isdir(os.path.join(coll_run_folder_path, name))]

    # Split into THREADS lists
    list_of_list_of_sub_folders = []
    for z in range(num_threads):
        list_of_list_of_sub_folders.append([])
    a = 0
    for f1 in sub_folders:
        list_of_list_of_sub_folders[a].append(f1)
        a = (a + 1) % num_threads

    # Run on THREADS threads
    pool = ThreadPool(num_threads)
    pool.map(analyze, list_of_list_of_sub_folders)


# Call main flow
main()
