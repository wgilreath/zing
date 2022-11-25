#!/usr/bin/env python3
#
#

"""

 Name:  zing.py
 
 Title: Zing - Zero packet pING network utility implemented in Python.

 Description: Zero packet PING utility that checks host by name or ip-address is active, and time to reach.

 Author William F. Gilreath (will@wfgilreath.xyz)
 Version 1.2.2  10/12/22

 Copyright © 2022 All Rights Reserved.

 License: This software is subject to the terms of the GNU General Public License (GPL)
     version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.

 You must accept the terms of the GNU General Public License (GPL) license agreement
     to use this software.

 Note Python version of zing, the first, initial time value can skew overall results, so the first value
     in zing timetable is skipped for computing average, standard deviation, extremum.

"""

import math
import socket
import sys
import time

class Zing:

    ports = [80, 443]   # default ports http, https
    inet_addr = None    # network address name for hostname
    tcp4Flag = True     # default tcp4 ip-address
    timeout = 4000      # default socket time 4000 ms = 4-seconds
    host = "localhost"  # default host name is localhost or 127.0.0.1
    count = 4           # default count of times to perform ops
    host_name = None    # result host name from DNS query
    host_addr = None    # result host address from DNS query
    limit = 4           # default limit on number of ops

    #
    # Get total time to zing using equation: time = (double) timeTotal / (double) ports.length / (double) limit;
    #
    # parameter: in_total_time is the accumulated time overall.
    # parameter: in_ports_len is the total number of ports.
    # parameter: in_limit is the limit on the number of ops per zing request.
    # 
    # return - total time to zing host averaged with equation.
    #
    @staticmethod
    def get_total_time(in_total_time, in_ports_len, in_limit):
        result = in_total_time / in_ports_len / in_limit
        return result

    #
    # Get the wallclock real-time in milliseconds
    #
    # return - current wallclock time in milliseconds
    #
    @staticmethod
    def current_time_ms():
        return time.clock_gettime_ns(time.CLOCK_REALTIME) / 1000

    #
    # Calculate the standard deviation using the average time and the table of
    # zing times. Standard deviation is the statistical measure of variability.
    #
    # parameter: avg - average time to zing a computer system on the network.
    # parameter: values - the zing table of times for a count of each time to zing a computer system.
    #
    # return: double - computed standard deviation or variance in values
    #
    @staticmethod
    def stddev(in_avg, in_values):
        dv = 0.0
        for dbl in enumerate(in_values):
            if dbl[0] == 0:  # skip first value as skews standard deviation calculation
                continue
            dm = dbl[1] - in_avg
            dv = dv + (dm * dm)
        result = math.sqrt(dv / len(in_values) - 1)
        result = round(result, 3)
        return result

    #
    # Report or print the use of zing with parameters and example with default parameters
    #
    def usage(self):
        print("Usage: zing.py -h | [-4|-6] [-c count] [-op ops] [-p ports] [-t timeout] host")
        print("zing.py -4 -c 4 -op 4 -p 80,443 -t 4000 google.com")

    #
    # Process command-line arguments and set internal zing parameters from values.
    #
    # parameter: argv - command-line interface arguments passed to zing network utility
    #
    def process_args(self, argv):
        n = len(sys.argv)
        idx = 1
        while idx < n:
            arg = sys.argv[idx]

            if arg == "-4":
                self.tcp4Flag = True
            elif arg == "-6":
                self.tcp4Flag = False
            elif arg == "-c":
                self.count = sys.argv[idx + 1]
                idx = idx + 1
            elif arg == "-h":
                self.usage()
                exit(0)
            elif arg == "-op":
                self.limit = sys.argv[idx + 1]
                idx = idx + 1
            elif arg == "-p":
                self.ports = sys.argv[idx + 1].split(",")
                idx = idx + 1
            elif arg == "-t":
                self.timeout = sys.argv[idx + 1]
                idx = idx + 1
            else:
                if arg[0] == '-':
                    print("Error! Invalid command-line argument:", arg, "not recognized!")
                    exit(1)
                else:
                    self.host = arg
            idx = idx + 1

    #
    # Get host IP-address and host name
    #
    # parameter: in_host - specified host to zing
    #
    # return IP address as IPv4 or IPv6
    #
    def get_host_addr_name(self, in_host):
        try:
            if self.tcp4Flag:
                return self.get_host_addr_name_ip4(in_host)
            else:
                return self.get_host_addr_name_ip6(in_host)
        except Exception:
            print("Error! Unable to get host'", in_host, end="' ")
            print("host address or ip-address unknown!")
            exit(1)

    #
    # Get host IP-address and host name as IPv4
    #
    # parameter: in_host - specified host to zing
    #
    # return IP address as IPv4
    #
    def get_host_addr_name_ip4(self, in_host):
        ip_addr = socket.gethostbyname(in_host)
        self.host_addr = ip_addr
        self.host_name = in_host
        return ip_addr

    #
    # Get host IP-address and host name as IPv6
    #
    # parameter: in_host - specified host to zing
    #
    # return IP address as IPv6
    #
    def get_host_addr_name_ip6(self, in_host):
        ip_addr = self.get_ip_6(in_host)
        self.host_addr = ip_addr
        self.host_name = in_host
        return ip_addr

    #
    # Get host IP-address v6
    #
    # parameter: in_host - specified host to zing
    # parameter: in_port - specified port to use with default port as 0
    #
    # return IP address as IPv6
    #
    @staticmethod
    def get_ip_6(in_host, in_port=0):
        result = socket.getaddrinfo(in_host, in_port, socket.AF_INET6)
        ip_addr = result[0][4][0]
        return ip_addr

    #
    # Get host IP-address v4
    #
    # parameter: in_host - specified host to zing
    # parameter: in_port - specified port to use with default port as 0
    #
    # return IP address as IPv4
    #
    @staticmethod
    def get_ip_4(in_host, port=0):
        result = socket.getaddrinfo(in_host, port, socket.AF_INET)
        ip_addr = result[0][4][0]
        return ip_addr

    #
    # Report the time and if the host computer system is active or alive on the network.
    #
    # parameter: in_time - overall time to zing the host computer system on the network.
    #
    def report_time(self, in_time):
        print("", end=" ")
        print(int(self.limit) * len(self.ports), end=" ")
        print("ops to", end=" ")
        print(self.host_name, end=" ")
        print("(", end="")
        print(self.host_addr, end="")
        print(")", end=" ")

        if in_time >= 0.0:
            print("Active", end=" ")
            print("time", end=" = ")
            print(round(in_time, 3), end=" ")
            print("ms ")
        else:
            print("Absent", end="!")
            exit(0)

    #
    # Zing a given host on the network at a specific port on the host.
    #
    # parameter: in_host - host name of computer system on a network.
    # parameter: in_port - port on the computer system on a network.
    #
    # return: double - total socket time to zing computer system or -1.0d for  not available.
    #
    def do_zing_to_host(self, in_host, in_port):
        if self.inet_addr is None:
            self.inet_addr = self.get_host_addr_name(in_host)

        self.host_addr = self.inet_addr
        self.host_name = in_host

        present_flag = True

        sock = socket.socket(socket.AF_INET6, socket.SOCK_DGRAM, 0)
        sock.settimeout(int(self.timeout))

        # get start time in milliseconds
        sock_time_start = self.current_time_ms()

        # sock.connect((self.inet_addr, int(self.ports[0]), 0, 0))
        sock.connect((self.inet_addr, int(in_port), 0, 0))

        sock.shutdown(socket.SHUT_RDWR)
        sock.close()

        # get close time in milliseconds
        sock_time_close = self.current_time_ms()

        if present_flag:
            sock_time_total = sock_time_close - sock_time_start
        else:
            print("", end=".")
            return -1.0

        return sock_time_total

    #
    # The main method is the central start method of the zing network utility
    # that invokes other methods to zing a host computer system on a network.
    #
    @staticmethod
    def main():
        zing = Zing()
        n = len(sys.argv)

        if n == 1:
            zing.usage()
            exit(0)

        zing.host = sys.argv[1]
        zing.process_args(sys.argv)

        zing.get_host_addr_name(zing.host)

        print("ZING:", zing.host_name, end=" (")
        print(zing.host_addr, end="):")
        print(" ", len(zing.ports), end=" ")
        print("ports used,", end=" ")
        print((int(zing.limit) * len(zing.ports)), end=" ")
        print("ops per cycle")

        zing_time_table = []
        time_zing_start = zing.current_time_ms()

        for x in range(int(zing.count) + 1):  # add one to count as we skip first entry hence need 1...count+1
            total_time = 0

            # skip reporting/printing timing for first zero-th 0 cycle of the for-loop
            if x > 0:
               print("#", x, end=".")

            for y in range(int(zing.limit)):
                for port in enumerate(zing.ports):
                    zing_time = zing.do_zing_to_host(zing.host, int(port[1]))
                    zing_time_table.append(zing_time)
                    total_time = total_time + zing_time

            if x > 0:
                print(".", end="")

            total_time = zing.get_total_time(int(total_time), len(zing.ports), int(zing.limit))

            if x > 0:
                print(".", end="")

            if x > 0:
                zing.report_time(total_time)

        time_zing_close = zing.current_time_ms()

        min_time = int(zing.timeout)
        max_time = 0
        avg_time = 0

        for val in enumerate(zing_time_table):
            # skip first value as initial time is large and skews results
            if val[0] == 0:  
                continue
            if val[1] > max_time:
                max_time = val[1]

            if val[1] < min_time:
                min_time = val[1]

            avg_time = avg_time + val[1]

        avg_time = avg_time / (len(zing_time_table) - 1)
        std_dev = zing.stddev(avg_time, zing_time_table)

        print()
        print("--- zing summary for", end=" ")
        print(zing.host_name, end="/")
        print(zing.host_addr, end=" ---")
        print()
        print(len(zing.ports) * int(zing.limit), end=" ")
        print("total ops used; total time", end=": ")
        print(time_zing_close - time_zing_start, end=" ms")
        print()
        print("total-time min/avg_time/max/stddev ", end="= ")
        print(round(min_time, 3), end="/")  
        print(round(avg_time, 3), end="/")
        print(round(max_time, 3), end="/")
        print(std_dev, end=" ms")
        print()
        print()


if __name__ == '__main__':
    try:
        sys.exit(Zing.main())
    except Exception as ex:
        import traceback

        exc_type, exc_val, exc_trace = sys.exc_info()
        print("Error! ", exc_val)
        print(exc_type, exc_val)
        print("Trace:")
        traceback.print_tb(exc_trace, file=sys.stdout)
        sys.exit(1)
