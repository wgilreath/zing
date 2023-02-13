/*
 * @(#)Zing2.java
 *
 * Title: Zing2 - Zero packet pING network utility Java 17 edition.
 *
 * Description: Zero packet PING utility that checks host by name or ip-address
 *     is active, and time in milliseconds to reach.
 *
 * @author William F. Gilreath (will@wfgilreath.xyz)
 * @co-author Mateusz Zacki (mzacki.github.io)
 * @version 1.2.3  02/13/23
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
package xyz.wfgilreath.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

final class Zing2 {


    private static final String ZING_USAGE = "Usage: zing -h | [-4|-6] [-c count] [-op ops] [-p ports] [-t timeout] host";
    private static final String ZING_EXAMPLE = "zing -4 -c 4 -op 4 -p 80,443 -t 4000 google.com";

    private static InetAddress inetAddr = null; // network address name for hostname
    private static int timeout = 4000; // default socket time 4000 ms = 4-seconds
    private static int count = 4;    // default count of times to perform ops
    private static Integer[] ports = new Integer[]{80, 443}; // default ports http, https
    private static String host = "localhost";// default host name is localhost or 127.0.0.1
    private static String hostAddr = "";         // result host address from DNS query
    private static boolean hostFlag = true;       // default is host is present, available
    private static int limit = 4;          // default limit on number of ops

    static boolean tcp4Flag = true; // default tcp4 ip-address
    static String hostName = "";         // result host name from DNS query

    /**
     * Private constructor to prevent instantiating this class except internally.
     */
    private Zing2() {
    }

    /**
     * The main method is the central start method of the zing network utility that invokes other methods to zing a host
     * computer system on a network.
     *
     * @param args - command-line arguments to the zing network utility.
     */
    public static void main(final String[] args) {

        if (args.length == 0) {
            logUsageAndQuit();
        }

        processArgs(args);
        inetAddr = getHostAddrName(host);

        System.out.printf("ZING: %s (%s): %d ports used, %d ops per cycle%n",
                hostName, hostAddr, ports.length,
                (limit * ports.length));

        System.out.printf("ZING: %s (%s): %d ports used, %d ops per cycle%n",
                hostName, hostAddr, ports.length, (limit * ports.length));

        long timeZingStart = System.currentTimeMillis();

        double[] zingTimeTable = new double[count];

        for (int x = 0; x < count; x++) {

            double zingTime;
            double totalTime = 0.0;

            System.out.printf("#%d ", x + 1);
            System.out.print(".");

            for (int y = 0; y < limit; y++) {
                for (int port : ports) {
                    zingTime = doZingToHost(host, port);
                    zingTimeTable[x] = zingTime;
                    totalTime += zingTime;
                }
            }

            System.out.print(".");
            double time = getTotalTime(totalTime, ports.length, limit);

            System.out.print(".");
            report(time); // time = -1.0d, absent, else active

        }

        long timeZingClose = System.currentTimeMillis();

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double avg = 0.0;

        for (int x = 0; x < count; x++) {

            if (min > zingTimeTable[x]) {
                min = zingTimeTable[x];
            }
            if (max < zingTimeTable[x]) {
                max = zingTimeTable[x];
            }
            avg += zingTimeTable[x];

        }

        avg = avg / count;

        double stdDev = stddev(avg, zingTimeTable);

        System.out.printf("%n--- zing summary for %s/%s ---%n", hostName, hostAddr);
        System.out.printf("%d total ops used; total time: %d ms%n", (ports.length * limit * count),
                (timeZingClose - timeZingStart));

        System.out.printf("total-time min/avg/max/stddev = %.3f/%.3f/%.3f/%.3f ms", min, avg, max, stdDev);
        System.out.printf("%n%n");

        System.exit(0);

    }

    private static void processArgs(final String[] args) {

        try {
            for (int idx = 0; idx < args.length; idx++) {
                System.out.println("Current index: " + idx);
                String arg = args[idx];
                switch (arg) {
                    case "-4" -> setTcp4Flag(true);
                    case "-6" -> setTcp4Flag(false);
                    case "-c" -> setCount(args, idx);
                    case "-op" -> setLimit(args, idx);
                    case "-p" -> setPorts(args, idx);
                    case "-t" -> setTimeout(args, idx);
                    case "-h" -> logUsageAndQuit();
                    default -> setHost(args, idx);
                }
            }
        } catch (Exception e) {
            logParamErrorAndQuit();
        }

    }


    /**
     * Get TCP/IP 4 32-bit address from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return instance of TCP/IP-4 32-bit address for host name.
     */
    private static Inet4Address getIPv4Addr(final String hostName) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(hostName);
        for (InetAddress addr : addresses) {
            if (addr instanceof Inet4Address inet4Address) {
                return inet4Address;
            }
        }
        return null;
    }

    /**
     * Get TCP/IP 6 128-bit address from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return instance of TCP/IP-6 128-bit address for host name.
     */
    private static Inet6Address getIPv6Addr(final String hostName) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(hostName);
        for (InetAddress addr : addresses) {
            if (addr instanceof Inet6Address inet6Address) {
                return inet6Address;
            }
        }
        return null;
    }

    /**
     * Get InetAddress object containing TCP/IP data from a given host name.
     *
     * @param hostName - host name of computer system on a network.
     * @return - return InetAddress which is either TCP/IP-4 or TCP/IP-6 address.
     */
    private static InetAddress getHostAddrName(final String hostName) {

        InetAddress iaddr = null;

        try {

            if (Zing2.tcp4Flag) {
                iaddr = Zing2.getIPv4Addr(hostName);
            } else {
                iaddr = Zing2.getIPv6Addr(hostName);
            }

            if (hostFlag) {
                Zing2.hostName = iaddr.getHostName();
                Zing2.hostAddr = iaddr.getHostAddress();
                Zing2.hostFlag = false;
            }
        } catch (Exception e) {
            System.out.printf(".. Error: Cannot resolve %s: Unknown host.%n", host);
            System.exit(1);
        }

        return iaddr;
    }

    /**
     * Zing a given host on the network at a specific port on the host.
     *
     * @param host - host name of computer system on a network.
     * @param port - port on the computer system on a network.
     * @return double - total socket time to zing computer system or -1.0d for not available.
     */
    private static double doZingToHost(final String host, final int port) {

        if (inetAddr == null) {
            inetAddr = getHostAddrName(host);
        } // end if

        try {
            if (inetAddr.isReachable(timeout)) { // command-line option -timeout
                System.out.printf(".. Error: Host is unreachable.%n", host, inetAddr.getHostAddress());
                System.exit(1);
            } // end if

        } catch (Exception e) {
            System.out.printf(".. Error: Host contact timeout.%n", host, inetAddr.getHostAddress());
            System.exit(1);
        }

        boolean presentFlag = true;

        long socketTimeStart = 0;
        long socketTimeClose = 0;
        long socketTimeTotal = 0;

        try {

            socketTimeStart = System.currentTimeMillis();

            try (Socket socket = new Socket(host, port)) {
                socket.setSoTimeout(timeout);
            }
            socketTimeClose = System.currentTimeMillis();

        } catch (SocketTimeoutException e) {
            System.out.printf("Timed out after %d ms waiting for host.%n", timeout);
            presentFlag = false;
        } catch (Exception e) {
            presentFlag = false;
        }

        if (presentFlag) {
            socketTimeTotal = socketTimeTotal + (socketTimeClose - socketTimeStart);
        } else {
            System.out.print(".");
            return -1.0d;
        }

        return socketTimeTotal;

    }

    /**
     * Get total time to zing using equation: time = (double) timeTotal / (double) ports.length / (double) limit;
     *
     * @param - totalTime is the accumulated time overall.
     * @param - portsLength is the total number of ports.
     * @param - limit is the limit on the number of ops per zing request.
     * @return - total time to zing host averaged with equation.
     */
    private static double getTotalTime(final double totalTime, final int portsLength, final int limit) {
        return totalTime / portsLength / limit;
    }

    /**
     * Report the time and if the host computer system is active or alive on the network.
     *
     * @param time - overall time to zing the host computer system on the network.
     */
    private static void report(final double time) {

        System.out.printf(" %d ops to %s (%s): ", limit * ports.length, hostName, hostAddr);

        //if time == -1.0 no timing statistics, unable to zing host computer system
        if (time >= 0.0d) {
            System.out.print("Active ");
            System.out.printf("time = %,.3f ms%n", time);
        } else {
            System.out.println("Absent!");
            System.exit(0);
        }

    }

    /**
     * Calculate the standard deviation using the average time and the table of zing times.Standard deviation is the
     * statistical measure of variability.
     *
     * @param avg - average time to zing a computer system on the network.
     * @param values - the zing table of times for a count of each time to zing a computer system.
     */
    private static double stddev(final double avg, double[] values) {

        double dv = 0;

        for (double dbl : values) {
            double dm = dbl - avg;
            dv += dm * dm;
        }

        return Math.sqrt(dv / values.length);

    }

    private static void logUsageAndQuit() {
        System.out.println(ZING_USAGE);
        System.out.println(ZING_EXAMPLE);
        System.exit(0);
    }

    private static void setTcp4Flag(boolean flag) {
        tcp4Flag = flag;
    }

    private static void setCount(String[] args, int index) {
        count = parseArgValueToInt(args, index);
    }

    private static void setLimit(String[] args, int index) {
        limit = parseArgValueToInt(args, index);
    }

    private static void setTimeout(String[] args, int index) {
        timeout = parseArgValueToInt(args, index);
    }

    private static void setHost(String[] args, int index) {
        // check if arg has "-" at char[0], if so log error invalid command-line parameter
        var arg = args[index];
        if (arg.charAt(0) == '-') {
            logInvalidParamAndQuit(arg);
        }
        // do not temporarily assign flag's value as host, it should remain localhost until valid host passed
        if (index > 0 && args[index - 1].charAt(0) == '-') {
            return;
        }
        host = arg;
    }

    private static void setPorts(String[] args, int idx) {
        String[] tmpPorts = args[idx + 1].split(",");
        ports = new Integer[tmpPorts.length];
        for (int x = 0; x < tmpPorts.length; x++) {
            ports[x] = Integer.valueOf(tmpPorts[x]); // trim any spaces for a , b , c ??
        }
    }

    private static int parseArgValueToInt(String[] args, int index) {
        System.out.println("Inside method: " + index);
        return Integer.parseInt(args[index + 1]);
    }

    private static void logInvalidParamAndQuit(String arg) {
        System.out.printf("Error '%s' is invalid command-line parameter!%n", arg);
        System.exit(1);
    }

    private static void logParamErrorAndQuit() {
        System.out.printf("Error with command-line arguments!%n");
        System.exit(1);
    }

}
