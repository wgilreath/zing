#!/usr/bin/env ruby
#
# Name:  zing.py
#
# Title: Zing - Zero packet pING network utility implemented in Ruby v. 3.2.2
#
# Description: Zero packet PING utility that checks host by name or ip-address is
# active, and time to reach.
#
# Author William F. Gilreath (will@wfgilreath.xyz)
# Version 1.22 11/26/23
#
# Copyright © 2023 All Rights Reserved.
#
# License: This software is subject to the terms of the GNU General Public License (GPL)
#     version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.
#
# You must accept the terms of the GNU General Public License (GPL) license agreement
#     to use this software.
#
#

require 'socket'
require 'timeout'

#
# global variables for zing utility
#

$var_debug = false          # script debugging with trace information
$var_tcp = 4                # default TCP/IP version 4
$var_count = 6              # default number of zing op cycles
$var_limit = 4              # default operations limit per cycle
$var_host = "localhost"     # default host name
$var_time = 1500            # default network timeout in msec
$var_portlist = "80,443"    # default ports on host to zing
$var_ports = []             # array variable of integer ports
$var_addr = "127.0.0.1"     # default host address

# function for COnnect-DIsconnect Socket Time
def codist(in_sock, in_port, in_addr)

  begin
    time_start = Process.clock_gettime(Process::CLOCK_REALTIME, :nanosecond)

    in_sock.connect(Socket.pack_sockaddr_in(in_port, in_addr))

    in_sock.close

    time_close = Process.clock_gettime(Process::CLOCK_REALTIME, :nanosecond)

  rescue => e
    puts ""
    puts "Unknown Error: #{e}"
    if $var_debug
      puts " #{e.backtrace}"
    end
    puts ""
    exit 1 # exit or return ??
  end # rescue

  return (time_close - time_start) / 1000_000.to_f # nanosec, div 1000_000 time msec

end # codist

def zing_op(in_host, in_port, in_time, in_tcpv, in_limit)

  Timeout.timeout(in_time.to_i / 1000) do
    begin

      # for-loop with accumulated time in msec
      # from 0 to limit-1, accumulate, compute average time for op

      time_z = 0

      for x in 1..in_limit do

        sock = Socket.new(in_tcpv, Socket::SOCK_STREAM)

        ip_addr = IPSocket::getaddress(in_host)

        time_z = time_z + codist(sock, in_port, ip_addr) # do_codis_op

      end

      return time_z

    rescue SocketError => ex
      puts "Error: Domain name is not known, no ip-address found!"
      puts ""
      exit 1
    rescue Errno::ECONNREFUSED
      puts "Error: Connection refused!"
      puts ""
      exit 1
    rescue Errno::ENETUNREACH
      puts "Error: Host on network unreachable!" # no route to host
      puts ""
      return -1
    rescue Errno::EAFNOSUPPORT
      tcp_ver = "TCP/IP4"
      if $var_tcp == 6
        tcp_ver = "TCP/IP6"
      end

      puts "IP address #{tcp_ver} is not supported!"
      puts ""
      exit 1
      # return -1
    rescue Timeout::Error
      puts "Error: Connection Timed Out!"
      puts ""
      return 0
    rescue => e
      puts "Unknown Error: #{e}"
      if $var_debug
        puts " #{e.backtrace}"
      end
      puts ""
      exit 1 # exit or return ??
    end # rescue
  end # Timeout
end # zing_op

def zing_fun(in_host, in_port, in_time, in_tpcv, in_limit, in_count)

  if $var_debug
    puts "zing_fun() #{in_host} #{in_port} #{in_time} #{in_limit} #{in_count} "
    puts ""
  end

  begin

    time_tbl = Array.new(in_count)

    for idx in 0..in_count - 1 do

      time_z = zing_op(in_host, in_port, in_time, in_tpcv, in_limit)

      printf "ZING: Port: %3d    %s [%s] Time: % 8s ms\n\r", in_port, in_host, $var_addr, time_z.round(3).to_s

      time_tbl[idx] = time_z

    end # for

  rescue => e
    puts ""
    puts "zing_fun() Unknown Error: #{e}!"
    if $var_debug
      puts " #{e.backtrace}"
    end
    puts ""
    exit 1 # exit or return ??
  end # rescue

  return time_tbl

end #zing_fun

def zing_stat(tbl, total)

  if tbl == nil
    return
  end

  avg = 0
  min = tbl[0]
  max = tbl[0]

  # get min, max, avg
  for i in 1..tbl.length - 1 do

    avg = avg + tbl[i]
    if min > tbl[i]
      min = tbl[i]
    else
      if max < tbl[i]
        max = tbl[i]

      end # if
    end # if
  end # for

  avg = avg / tbl.length

  sdev = std_dev(tbl)

  if $var_debug
    puts "avg = #{avg.round(3)} "
    puts "min = #{min.round(3)} "
    puts "max = #{max.round(3)} "
    puts "sdev = #{sdev.round(3)} "
  end

  total_ops = $var_count * $var_ports.length
  puts "--- zing summary for #{$var_host}/#{$var_addr} ---"
  puts "#{total_ops} total ops used; total time: #{total} ms" # total-time from time-tbl ??
  printf "total-time min/avg/max/stddev = %.3f/%.3f/%.3f/%.3f ms\n\r", min, avg, max, sdev

end #zing_stat

# zing_stat calculate standard deviation
def std_dev(tbl)
  n = tbl.size # => 9
  tbl.map!(&:to_f) # => [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
  mean = tbl.reduce(&:+) / n # => 5.0
  sum_sqr = tbl.map { |x| x * x }.reduce(&:+) # => 285.0
  std_dev = Math.sqrt((sum_sqr - n * mean * mean) / (n - 1)) # => 2.7386127875258306
  return std_dev
end

def usage()
  puts "Usage: #{$0} ( (-4|-6) [-c <count>][-op <limit>][-p <port>][-t timeout] <hostname>| (-h|-help) )"
end

def process_args(args)

  len = args.length

  for i in 0..len - 1

    arg = args.at(i)
    if $var_debug
      puts "arg #{i} is #{arg} "
    end

    case arg
    when "-h"
    when "-help"
      puts ""
      usage
      puts ""
      exit 0
    when "-4" # tcp 4
      $var_tcp = 4
    when "-6" # tcp 6
      $var_tcp = 6
    when "-c" # count
      $var_count = args.at(i + 1).to_i
      i = i + 1
    when "-op" # limit
      $var_limit = args.at(i + 1).to_i
      i = i + 1
    when "-p" # ports
      $var_portlist = args.at(i + 1)
      i = i + 1
    when "-t" # timeout
      $var_time = args.at(i + 1).to_i
      i = i + 1
    else
      # hostname if arg has "-" then error, exit

      # check for '-' in arg
      if arg.include? "-"
        puts "Error CLI argument '#{arg}'' is invalid!"
        puts ""
        exit(1)
      else
        $var_host = arg
      end # if
    end # case
  end # for

  # dump zing args
  if $var_debug
    puts ""
    puts "Zing argv count = #{$var_count} "
    puts "Zing argv host = #{$var_host}"
    puts "Zing argv limit = #{$var_limit} "
    puts "Zing argv portlist = #{$var_portlist} "
    puts "Zing argv tcp = #{$var_tcp} "
    puts "Zing argv timeout = #{$var_time} "
    puts ""
  end

  # process default or CLI parameter ports list
  $var_ports = $var_portlist.split(",")

end # process_args

def validIPvHost(host)

  # get host name information
  addr_info = Socket.getaddrinfo(host, "http")

  if $var_debug
    puts ""
    puts "addr_info: #{addr_info} "
    puts ""
  end

  ipv = addr_info[0][4] # 2 AF_INET (ipV4) 30 AF_INET6 (ipV6)

  if $var_tcp == 4
    if ipv == Socket::AF_INET
      $var_addr = addr_info[0][3]
      return
    end
  else
    if $var_tcp == 6
      if ipv == Socket::AF_INET6
        $var_addr = addr_info[0][3]
        return
      end
    end
  end

  tcp_ver = "TCP/IP4"
  if $var_tcp == 6
    tcp_ver = "TCP/IP6"
  end

  puts "Error: IP address #{tcp_ver} is not supported!"
  puts ""
  exit 1

end #validIPvHost

system('clear')

len = ARGV.length

if len == 0
  puts ""
  puts "Error: #{$0} needs at least one command-line argument!"
  puts ""

  usage

  puts ""
  exit(1)
end

begin

  process_args(ARGV)

  # check if ip-version, host valid
  validIPvHost($var_host)

  tcpv = nil  #tcpv as Socket constant

  if $var_tcp == 4
    tcpv = Socket::AF_INET
  else
    if $var_tcp == 6
      tcpv = Socket::AF_INET6
    end
  end

  puts ""
  puts "ZING: #{$var_host} (#{$var_addr}): #{$var_ports.length} ports used, #{$var_limit} ops per cycle."
  puts ""

  zing_start_time = Process.clock_gettime(Process::CLOCK_REALTIME, :nanosecond)

  for i in 0..$var_ports.length - 1 do

    port = $var_ports.at(i)

    tbl = zing_fun($var_host, port.to_i, $var_time, tcpv, $var_limit, $var_count)

  end

  zing_close_time = Process.clock_gettime(Process::CLOCK_REALTIME, :nanosecond)

  zing_total_time = (zing_close_time - zing_start_time) / 1000_000.to_f # nanosec to millisec

  puts ""
  zing_stat(tbl, zing_total_time)
  puts ""

rescue => e
  puts ""
  puts "Main Unknown Error: #{e}"
  if $var_debug
    puts " #{e.backtrace}"
  end
  puts ""
  exit 1 # exit or return ??
end # rescue

exit 0
