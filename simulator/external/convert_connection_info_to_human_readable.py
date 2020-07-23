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

import sys


def convert_connection_info_to_human_readable(logs_floodns_dir):
    with open(logs_floodns_dir + "/connection_info.csv", "r") as f_in:
        with open(logs_floodns_dir + "/connection_info.txt", "w+") as f_out:

            # Info
            print("Assuming flow unit is in bits, and time unit is in nanoseconds.")

            # Header
            f_out.write("Conn. ID   Source   Target   Size           Sent           Flows' IDs     Start time (ns)    "
                        "End time (ns)      Duration         Progress     Avg. rate        Finished?     Metadata\n")

            # Each line
            for line in f_in:
                spl = line.split(",")
                f_out.write(
                    "%-10d %-8d %-8d %-14s %-14s %-14s %-18d %-18d %-16s %-12s %-16s %s %s\n" % (
                        int(spl[0]),  # Connection ID
                        int(spl[1]),  # Source
                        int(spl[2]),  # Target
                        "%.2f Mbit" % (float(spl[3]) / 1000000.0),  # Size
                        "%.2f Mbit" % (float(spl[4]) / 1000000.0),  # Sent
                        spl[5],       # Flow identifiers
                        int(spl[6]),  # Start time (ns)
                        int(spl[7]),  # End time (ns)
                        "%.2f ms" % (float(spl[8]) / 1000000.0),  # Duration
                        "%.2f%%" % (float(spl[4]) / float(spl[3]) * 100.0),  # Progress
                        "%.2f Mbit/s" % (float(spl[9]) * 1000),  # Average rate
                        "YES" if spl[10] == "T" else "NO",       # Finished?
                        spl[11].strip() if len(spl) > 10 else ""         # Metadata
                    )
                )

            # Finished
            print("Human readable file: " + logs_floodns_dir + "/connection_info.txt")


def main():
    args = sys.argv[1:]
    if len(args) != 1:
        print("Usage: python convert_connection_info_to_human_readable.py </path/to/floodns_logs>")
    else:
        convert_connection_info_to_human_readable(args[0])


if __name__ == "__main__":
    main()
