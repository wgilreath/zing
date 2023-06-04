/*
 * @(#)Zing.kt
 *
 * Title: Zing - Zero packet pING network utility implemented in Kotlin.
 *
 * Description: Zero packet PING utility that checks host by name or ip-address 
 *     is active, and time in milliseconds to reach.
 *
 * @author William F. Gilreath (will@wfgilreath.xyz)
 * @version 1.3.0  06/04/23
 *
 * Copyright © 2023 All Rights Reserved.
 *
 * License: This software is subject to the terms of the GNU General Public License (GPL)  
 *     version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.
 *
 * You must accept the terms of the GNU General Public License (GPL) license agreement
 *     to use this software.
 *
 **/

package xyz.wfgilreath.net

import java.net.*
import kotlin.math.sqrt
import kotlin.system.exitProcess

object Zing {

    //Zing usage and example parameters from defaults defined in code
    private const val ZING_USAGE = "Usage: zing -h | [-4|-6] [-c count] [-op ops] [-p ports] [-t timeout] host"
    private const val ZING_EXAMPLE = "zing -4 -c 4 -op 4 -p 80,443 -t 4000 google.com"
    private const val MAX_NUM_PORTS = 8 //maximum number of ports that can be specified on CLI

    private var inet_addr: InetAddress? = null // network address name for hostname
    private var tcp4Flag = true // default tcp4 ip-address
    private var timeout = 4000 // default socket time 4000 ms = 4-seconds
    private var count = 4 // default count of times to perform ops

    private var ports = arrayOf(80, 443, 0, 0, 0, 0, 0, 0) // default ports http, https

    private var host = "localhost" // default host name is localhost or 127.0.0.1
    private var hostName = "" // result host name from DNS query
    private var hostAddr = "" // result host address from DNS query
    private var hostFlag = true // default is host is present, available
    private var limit = 4 // default limit on number of ops

    /**
     * Get TCP/IP 4 32-bit address from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return instance of TCP/IP-4 32-bit address for host name.
     */
    @Throws(UnknownHostException::class)
    fun getIPv4Addr(hostName: String?): Inet4Address? {
        val addresses = InetAddress.getAllByName(hostName)
        for (addr in addresses) {
            if (addr is Inet4Address) {
                return addr
            } // end if
        } // end for
        return null
    } // end getIPv4Addr

    /**
     * Get TCP/IP 6 128-bit address from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return instance of TCP/IP-6 128-bit address for host name.
     */
    @Throws(UnknownHostException::class)
    fun getIPv6Addr(hostName: String?): Inet6Address? {
        val addresses = InetAddress.getAllByName(hostName)
        for (addr in addresses) {
            if (addr is Inet6Address) {
                return addr
            } // end if
        } // end for
        return null
    } // end getIPv6Addr

    /**
     * Get InetAddress object containing TCP/IP data from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return - return InetAddress which is either TCP/IP-4 or TCP/IP-6
     * address.
     */
    private fun getHostAddrName(hostName: String?): InetAddress? {
        val iaddr: InetAddress?
        try {
            iaddr = if (tcp4Flag) {
                getIPv4Addr(hostName)
            } else {
                getIPv6Addr(hostName)
            } // end if
            if (hostFlag) {
                if (iaddr == null) throw RuntimeException(String.format("Unable to get Internet address for host: %s%n", hostName))
                Zing.hostName = iaddr.hostName
                hostAddr = iaddr.hostAddress
                hostFlag = false
            } // end if
        } catch (ex: Exception) {
            print(String.format(".. Error: Cannot resolve %s: Unknown host.%n", host))
            exitProcess(1)
        } // end try
        return iaddr
    } // end getHostAddrName

    /**
     * Zing a given host on the network at a specific port on the host.
     *
     * @param host - host name of computer system on a network.
     * @param port - port on the computer system on a network.
     * @return double - total socket time to zing computer system or -1.0d for
     * not available.
     */
    private fun doZingToHost(host: String?, port: Int): Double {

        if (inet_addr == null) {
            inet_addr = getHostAddrName(host)
        } // end if

        try {
            if (inet_addr!!.isReachable(timeout)) { // command-line option -timeout
                print(String.format(".. Error: Host %s/%s is unreachable.%n", host, inet_addr!!.hostAddress))
                exitProcess(1)
            } // end if
        } catch (ex: Exception) {
            print(String.format(".. Error: Host %s/%s contact timeout.%n", host, inet_addr!!.hostAddress))
            exitProcess(1)
        } // end try

        var presentFlag = true // host at socket is present, default is true
        var socketTimeStart: Long = 0
        var socketTimeClose: Long = 0
        var socketTimeTotal: Long = 0

        try {
            socketTimeStart = System.currentTimeMillis()
            Socket(host, port).use { socket -> socket.soTimeout = timeout }
            socketTimeClose = System.currentTimeMillis()
        } catch (ex: SocketTimeoutException) {
            print(String.format("Timed out after %d ms waiting for host.%n", timeout))
            presentFlag = false
        } catch (ex: Exception) {
            presentFlag = false
        } // end try

        if (presentFlag) {
            socketTimeTotal += (socketTimeClose - socketTimeStart)
        } else {
            print(".")
            return -1.0
        } // end if

        return socketTimeTotal.toDouble()
    } // end doZingToHost

    /**
     * Get total time to zing using equation: time = (double) timeTotal /
     * (double) ports.length / (double) limit;
     *
     * @param totalTime is the accumulated time overall.
     * @param portsLength is the total number of ports.
     * @param limit is the limit on the number of ops per zing request.
     * @return - total time to zing host averaged with equation.
     */
    private fun getTotalTime(totalTime: Double, portsLength: Int, limit: Int): Double {
        return totalTime / portsLength / limit
    } // end getTotalTime

    /**
     * Report or print the use of zing with parameters and example with default
     * parameters
     */
    private fun usage() {
        print(String.format("%n%s%n%s%n%n", ZING_USAGE, ZING_EXAMPLE))
    } // end usage

    /**
     * Process command-line arguments and set internal zing parameters from
     * values.
     *
     * @param args - command-line interface arguments passed to zing network
     * utility
     */
    private fun processArgs(args: Array<String>) {
        try {
            var idx = 0
            while (idx < args.size) {
                when (val arg = args[idx]) {
                    "-4" -> tcp4Flag = true
                    "-6" -> tcp4Flag = false
                    "-c" -> {
                        count = args[idx + 1].toInt()
                        idx++
                    }

                    "-op" -> {
                        limit = args[idx + 1].toInt()
                        idx++
                    }

                    "-p" -> {
                        val tmpPorts = args[idx + 1].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() // split comma delimited list

                        if (tmpPorts.size > MAX_NUM_PORTS) {
                            print(String.format("%nError: Number of ports specified exceeds limit of %d-ports!%n", MAX_NUM_PORTS))
                            exitProcess(1)
                        }//end if

                        ports = Array(tmpPorts.size) { 0 }

                        var x = 0
                        while (x < ports.size) {
                            ports[x] = Integer.parseInt(tmpPorts[x]) // trim any spaces for a , b , c ??
                            x++
                        }
                    }

                    "-t" -> {
                        timeout = args[idx + 1].toInt()
                        idx++
                    }

                    "-h" -> {
                        usage()
                        exitProcess(0)
                    }

                    else -> if (arg[0] == '-') {
                        print(String.format("Error '%s' is invalid command-line parameter!%n", arg))
                        exitProcess(1)
                    } else {
                        host = arg
                    } // end if
                }
                idx++
            }
        } catch (ex: Exception) {
            print("Error with command-line arguments!")
            ex.printStackTrace()
            exitProcess(1)
        } // end try

    } // end processArgs

    /**
     * Report the time and if the host computer system is active or alive on the
     * network.
     *
     * @param time - overall time to zing the host computer system on the
     * network.
     */
    private fun report(time: Double) {
        print(String.format(" %d ops to %s (%s): ", limit * ports.size, hostName, hostAddr))

        //if time == -1.0 no timing statistics, unable to zing host computer system
        if (time >= 0.0) {
            print("Active ")
            print(String.format("time = %,.3f ms%n", time))
        } else {
            println("Absent!")
            exitProcess(0)
        } // end if
    } // end report

    /**
     * Calculate the standard deviation using the average time and the table of
     * zing times.Standard deviation is the statistical measure of variability.
     *
     * @param avg - average time to zing a computer system on the network.
     * @param values - the zing table of times for a count of each time to zing
     * a computer system.
     * @return double - computed standard deviation or variance in values
     */
    @Strictfp
    fun stddev(avg: Double, values: DoubleArray): Double {
        var dv = 0.0
        for (dbl in values) {
            val dm = dbl - avg
            dv += dm * dm
        } //end for
        return sqrt(dv / values.size)
    } //end stddev

    /**
     * The main method is the central start method of the zing network utility
     * that invokes other methods to zing a host computer system on a network.
     *
     * @param args - command-line arguments to the zing network utility.
     */
    @JvmStatic
    fun main(args: Array<String>) {

        if (args.isEmpty()) {
            usage()
            exitProcess(0)
        } // end if

        host = args[0]
        processArgs(args)

        inet_addr = getHostAddrName(host)
        print(String.format("ZING: %s (%s): %d ports used, %d ops per cycle%n",
                hostName, hostAddr, ports.size,
                limit * ports.size))
        val timeZingStart = System.currentTimeMillis()
        val zingTimeTable = DoubleArray(count)
        for (x in 0 until count) {
            var zingTime: Double
            var totalTime = 0.0
            print(String.format("#%d ", x + 1))
            print(".")
            for (y in 0 until limit) {
                for (port in ports) {
                    zingTime = doZingToHost(host, port)
                    zingTimeTable[x] = zingTime
                    totalTime += zingTime
                } // end for(port)
            } // end for(limit)
            print(".")
            val time = getTotalTime(totalTime, ports.size, limit)
            print(".")
            report(time) // time = -1.0d, absent, else active
        } // end for(count)
        val timeZingClose = System.currentTimeMillis()

        //get min, max, avg
        var min = Double.MAX_VALUE
        var max = Double.MIN_VALUE
        var avg = 0.0
        for (x in 0 until count) {
            if (min > zingTimeTable[x]) {
                min = zingTimeTable[x]
            } //end if
            if (max < zingTimeTable[x]) {
                max = zingTimeTable[x]
            } //end if
            avg += zingTimeTable[x]
        } //end for
        avg /= count
        val stdDev = stddev(avg, zingTimeTable)
        print(String.format("%n--- zing summary for %s/%s ---%n", hostName, hostAddr))
        print(String.format("%d total ops used; total time: %d ms%n", ports.size * limit * count, timeZingClose - timeZingStart))
        print(String.format("total-time min/avg/max/stddev = %.3f/%.3f/%.3f/%.3f ms", min, avg, max, stdDev))
        print(String.format("%n%n"))

        exitProcess(0)
    } // end main

} // end class Zing
