/*
 * @(#)Zing.java
 *
 * Title: Zing - Zero packet pING network utility.
 *
 * Description: A Java compiler using the Java Compiler API with options not in javac.
 *
 * @author William F. Gilreath (will@wfgilreath.xyz)
 * @version 1.2.2  09/01/22
 *
 * Copyright © 2022 All Rights Reserved.
 *
 * License: This software is subject to the terms of the GNU General Public License (GPL)  
 *     version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.
 *
 * You must accept the terms of the GNU General Public License (GPL) license agreement
 *     to use this software.
 *
 **/
package xyz.wfgilreath.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public final class Zing {

    // zing usage and example parameters from defaults defined in code
    public static final String   ZING_USAGE = "Usage: zing -h | [-4|-6] [-c count] [-op ops] [-p ports] [-t timeout] host";
    public static final String ZING_EXAMPLE = "zing -4 -c 4 -op 4 -p 80,443 -t 4000 google.com";

    public static InetAddress inet_addr = null; // network address name for hostname
    public static boolean     tcp4Flag  = true; // default tcp4 ip-address
    public static int timeout = 4000; // default socket time 4000 ms = 4-seconds
    public static int count   = 4;    // default count of times to perform ops
    public static Integer[] ports = new Integer[]{80, 443}; // default ports http, https
    public static String    host  = "localhost";// default host name is localhost or 127.0.0.1
    public static String hostName = "";         // result host name from DNS query
    public static String hostAddr = "";         // result host address from DNS query
    public static boolean hostFlag = true;       // default is host is present, available
    public static int     limit    = 4;          // default limit on number of ops

    /**
     * Private constructor to prevent instantiating this class except
     * internally.
     */
    private Zing() {
    }

    /**
     * Get TCP/IP 4 32-bit address from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return instance of TCP/IP-4 32-bit address for host name.
     * @throws java.net.UnknownHostException
     */
    public static Inet4Address getIPv4Addr(final String hostName) throws UnknownHostException {

        InetAddress[] addresses = InetAddress.getAllByName(hostName);

        for (InetAddress addr : addresses) {

            if (addr instanceof Inet4Address) {
                return (Inet4Address) addr;
            } // end if
        } // end for

        return null;
    }// end getIPv4Addr

    /**
     * Get TCP/IP 6 128-bit address from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return instance of TCP/IP-6 128-bit address for host name.
     * @throws java.net.UnknownHostException
     */
    public static Inet6Address getIPv6Addr(final String hostName) throws UnknownHostException {

        InetAddress[] addresses = InetAddress.getAllByName(hostName);

        for (InetAddress addr : addresses) {

            if (addr instanceof Inet6Address) {
                return (Inet6Address) addr;
            } // end if
        } // end for

        return null;
    }// end getIPv6Addr

    /**
     * Get InetAddress object containing TCP/IP data from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return - return InetAddress which is either TCP/IP-4 or TCP/IP-6
     * address.
     */
    public static InetAddress getHostAddrName(final String hostName) {

        InetAddress iaddr = null;

        try {

            if (Zing.tcp4Flag) {
                iaddr = Zing.getIPv4Addr(hostName);
            } else {
                iaddr = Zing.getIPv6Addr(hostName);
            } // end if

            if (hostFlag) {
                Zing.hostName = iaddr.getHostName();
                Zing.hostAddr = iaddr.getHostAddress();
                Zing.hostFlag = false;
            } // end if
        } catch (Exception _ignore) {
            System.out.printf(".. Error: Cannot resolve %s: Unknown host.%n", host);
            System.exit(1);
        } // end try

        return iaddr;
    }// end getHostAddrName

    /**
     * Zing a given host on the network at a specific port on the host.
     *
     * @param host - host name of computer system on a network.
     * @param port - port on the computer system on a network.
     * @return double - total socket time to zing computer system or -1.0d for
     * not available.
     */
    public static double doZingToHost(final String host, final int port) {

        if (inet_addr == null) {
            inet_addr = getHostAddrName(host);
        } // end if

        try {
            if (inet_addr.isReachable(timeout)) { // command-line option -timeout
                System.out.printf(".. Error: Host is unreachable.%n", host, inet_addr.getHostAddress());
                System.exit(1);
            } // end if

        } catch (Exception _ignore) {
            System.out.printf(".. Error: Host contact timeout.%n", host, inet_addr.getHostAddress());
            System.exit(1);
        } // end try

        boolean presentFlag = true; // host at socket is present, default is true

        long socketTimeStart = 0, socketTimeClose = 0, socketTimeTotal = 0;

        try {

            socketTimeStart = System.currentTimeMillis();

            Socket socket = new Socket(host, port);

            socket.setSoTimeout(timeout);

            socket.close();
            socketTimeClose = System.currentTimeMillis();
        } catch (SocketTimeoutException _ignore) {
            System.out.printf("Timed out after %d ms waiting for host.%n", timeout);
            presentFlag = false;
        } catch (Exception _ignore) {
            presentFlag = false;
        } // end try

        if (presentFlag) {
            socketTimeTotal = socketTimeTotal + (socketTimeClose - socketTimeStart);
        } else {
            System.out.print(".");
            return -1.0d;
        } // end if

        return (double) socketTimeTotal;

    }// end doZingToHost

    //  //
    /**
     * Get total time to zing using equation: time = (double) timeTotal /
     * (double) ports.length / (double) limit;
     *
     * @param  - totalTime is the accumulated time overall.
     * @param  - portsLength is the total number of ports.
     * @param  - limit is the limit on the number of ops per zing request.
     * @return - total time to zing host averaged with equation.
     */
    public static double getTotalTime(final double totalTime, final int portsLength, final int limit) {

        double time = totalTime / (double) portsLength / (double) limit;
        return time;

    }// end getTotalTime

    /**
     * Report or print the use of zing with parameters and example with default
     * parameters
     */
    public static void usage() {
        System.out.printf("%n%s%n%s%n%n", ZING_USAGE, ZING_EXAMPLE);
    }// end usage

    /**
     * Process command-line arguments and set internal zing parameters from
     * values.
     *
     * @param args - command-line interface arguments passed to zing network
     * utility
     */
    public static void processArgs(final String[] args) {

        try {
            for (int idx = 0; idx < args.length; idx++) {
                String arg = args[idx];
                switch (arg) {

                    case "-4":
                        tcp4Flag = true;
                        break;

                    case "-6":
                        tcp4Flag = false;
                        break;

                    case "-c":
                        count = Integer.valueOf(args[idx + 1]);
                        idx++;
                        break;
                    case "-op":
                        limit = Integer.valueOf(args[idx + 1]);
                        idx++;
                        break;

                    case "-p":
                        String[] tmpPorts = args[idx + 1].split(","); // split comma delimited list

                        ports = new Integer[tmpPorts.length];
                        for (int x = 0; x < tmpPorts.length; x++) {
                            ports[x] = Integer.valueOf(tmpPorts[x]); // trim any spaces for a , b , c ??
                        } // end for
                        break;

                    case "-t":
                        timeout = Integer.valueOf(args[idx + 1]);
                        idx++;
                        break;

                    case "-h":
                        usage();
                        System.exit(0);
                        break;

                    // check if arg has "-" at char[0], if so error invalid command-line parameter
                    default:
                        if (arg.charAt(0) == '-') {
                            System.out.printf("Error '%s' is invalid command-line parameter!%n", arg);
                            System.exit(1);
                        } else {
                            host = arg;
                        } // end if

                        break;

                }// end switch
            } // end for
        } catch (Exception _ignore) {
            System.out.printf("Error with command-line arguments!%n");
            System.exit(1);
        } // end try

    }// end processArgs

    /**
     * Report the time and if the host computer system is active or alive on the
     * network.
     *
     * @param time - overall time to zing the host computer system on the
     * network.
     */
    public static void report(final double time) {

        System.out.printf(" %d ops to %s (%s): ", limit * ports.length, hostName, hostAddr);

        //if time == -1.0 no timing statistics, unable to zing host computer system
        if (time >= 0.0d) {
            System.out.print("Active ");
            System.out.printf("time = %,.3f ms%n", time);
        } else {
            System.out.println("Absent!");
            System.exit(0);
        } // end if

    }// end report

    /**
     * Calculate the standard deviation using the average time and the table of
     * zing times.Standard deviation is the statistical measure of variability.
     *
     * @param avg - average time to zing a computer system on the network.
     * @param values - the zing table of times for a count of each time to zing
     * a computer system.
     * @return 
     */
    public static strictfp double stddev(final double avg, double[] values) {

        double dv = 0;

        for (double dbl : values) {
            double dm = dbl - avg;
            dv += dm * dm;
        }//end for

        return Math.sqrt(dv / values.length);

    }//end stddev

    /**
     * The main method is the central start method of the zing network utility
     * that invokes other methods to zing a host computer system on a network.
     *
     * @param args - command-line arguments to the zing network utility.
     */
    public static void main(final String[] args) {

        if (args.length == 0) {
            usage();
            System.exit(0);
        } // end if

        host = args[0];

        processArgs(args);

        inet_addr = getHostAddrName(host);

        System.out.printf("ZING: %s (%s): %d ports used, %d ops per cycle%n", 
                          hostName, hostAddr, ports.length,
                          (limit * ports.length));

        long timeZingStart = System.currentTimeMillis();

        double[] zingTimeTable = new double[count];

        for (int x = 0; x < count; x++) {

            double zingTime = 0.0;
            double totalTime = 0.0;

            System.out.printf("#%d ", x + 1);
            System.out.print(".");

            for (int y = 0; y < limit; y++) {
                for (int port : ports) {
                    zingTime = doZingToHost(host, port);
                    zingTimeTable[x] = zingTime;
                    totalTime += zingTime;
                } // end for(port)
            } // end for(limit)

            System.out.print(".");
            double time = getTotalTime(totalTime, ports.length, limit);

            System.out.print(".");
            report(time); // time = -1.0d, absent, else active

        } // end for(count)

        long timeZingClose = System.currentTimeMillis();

        //get min, max, avg
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE, avg = 0.0;
        for (int x = 0; x < count; x++) {

            if (min > zingTimeTable[x]) {
                min = zingTimeTable[x];
            }//end if
            if (max < zingTimeTable[x]) {
                max = zingTimeTable[x];
            }//end if
            avg += zingTimeTable[x];

        }//end for

        avg = avg / (double) count;

        double std_dev = stddev(avg, zingTimeTable);

        System.out.printf("%n--- zing summary for %s/%s ---%n", hostName, hostAddr);
        System.out.printf("%d total ops used; total time: %d ms%n", (ports.length * limit * count), (timeZingClose - timeZingStart));

        System.out.printf("total-time min/avg/max/stddev = %.3f/%.3f/%.3f/%.3f ms", min, avg, max, std_dev);
        System.out.printf("%n%n");

        System.exit(0);

    }// end main

}// end class Zing
