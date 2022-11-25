// ==++==
// 
//   Copyright (c) 2022 by William F. Gilreath (will@wfgilreath.xyz).
//   All rights reserved.
//
//   License: This software is subject to the terms of the GNU General Public License (GPL)  
//   version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.
//
//   You must accept the terms of the GNU General Public License (GPL) license agreement
//   to use this software.
//
// ==--==
/*============================================================
**
** Class: Zing - Zero packet pING network utility.
**
**
** Purpose: Zero packet PING utility that checks host by name or ip-address 
**          is active, and time in milliseconds to reach.
**
** Version: 1.2.2 November 2022
**
**
============================================================*/
namespace xyz.wfgilreath.net
{
    using System.Net;
    using System.Net.Sockets;

    public sealed class Zing : Object
    {

        private static IPAddress? inet_addr = null; // network address name for hostname

        private static bool tcp4Flag = true; // default tcp4 ip-address
        private static int  timeout  = 4000; // default socket time 4000 ms = 4-seconds
        private static int  count    = 4;    // default count of times to perform ops

        private static int[] ports   = new int[] { 80, 443 }; // default ports http, https

        private static String  host     = "localhost";   // default host name is localhost or 127.0.0.1
        private static String? hostName = String.Empty;  // result host name from DNS query
        private static String? hostAddr = String.Empty;  // result host address from DNS query
        private static bool    hostFlag = true;          // default is host is present, available
        private static int     limit    = 4;             // default limit on number of ops

        // zing usage and example parameters from defaults defined in code
        private const String   ZING_USAGE   = "Usage: zing /h | [/4|/6] [/c count] [/op ops] [/p ports] [/t timeout] host";
        private const String ZING_EXAMPLE   = "zing /4 /c 4 /op 4 /p 80,443 /t 4000 microsoft.com";

        private const String TIME_FORMAT    = "{0:0.000}"; //time format for reporting zing host time

        private static void dumpArgs()
        {
            Console.Write("tcp4Flag: "); Console.WriteLine(tcp4Flag.ToString());
            Console.Write("timeout:  "); Console.WriteLine( timeout.ToString());
            Console.Write("count:    "); Console.WriteLine(   count.ToString());
            Console.Write("host:     "); Console.WriteLine(    host.ToString());
            Console.Write("limit:    "); Console.WriteLine(   limit.ToString());

            Console.Write("ports:    ");

            foreach (int port in ports)
            {
                Console.Write(port);
                Console.Write(" ");
            }//end foreach

            Console.WriteLine();

        }//end dumpArgs

        private static void Usage()
        {
            Console.WriteLine();
            Console.WriteLine(ZING_USAGE);
            Console.WriteLine();
            Console.WriteLine(ZING_EXAMPLE);
            Console.WriteLine();
            Console.WriteLine();

        }//end usage

        private static void ProcessArgs(String[] args)
        {
            try
            {
                for (int idx = 0; idx < args.Length; idx++)
                {
                    String arg = args[idx];
                    switch (arg)
                    {

                        case "-4":
                        case "/4":
                            tcp4Flag = true;
                            break;

                        case "-6":
                        case "/6":
                            tcp4Flag = false;
                            break;

                        case "-c":
                        case "/c":
                            count = int.Parse(args[idx + 1]);
                            idx++;
                            break;

                        case "-op":
                        case "/op":
                            limit = int.Parse(args[idx + 1]);
                            idx++;
                            break;

                        case "-p":
                        case "/p":

                            String[] tmpPorts = args[idx + 1].Split(","); // split comma delimited list

                            ports = new int[tmpPorts.Length];
                            for (int x = 0; x < tmpPorts.Length; x++)
                            {
                                ports[x] = int.Parse(tmpPorts[x]); 
                            } // end for

                            break;


                        case "-t":
                        case "/t":
                            timeout = int.Parse(args[idx + 1]);
                            idx++;
                            break;

                        case "-h":
                        case "/h":
                            Usage();
                            Environment.Exit(0);
                            break;

                        // check if arg has "/" or "-" at char[0], if so error invalid command-line parameter
                        default:

                            char prefixChar = arg.ToCharArray()[0];

                            if(prefixChar == '/' || prefixChar == '-')
                            {
                                Console.Write("Error ");
                                Console.Write(arg);
                                Console.WriteLine(" is invalid command-line parameter!");
                                Environment.Exit(1);
                            }
                            else
                            {
                                host = arg;
                            } // end if

                            break;

                    }// end switch

                }//end for
            }
            catch (Exception)
            {
                Console.WriteLine("Error with command-line arguments!%n");
                Environment.Exit(1);

            }//end try

        }//end processArgs

        public static IPAddress? GetIPv4Addr(String hostname)
        {

            // Get server related information.
            IPHostEntry heserver = Dns.GetHostEntry(hostname);

            // Loop on the AddressList
            foreach (IPAddress addr in heserver.AddressList)
            {

                if(addr.AddressFamily.ToString() == ProtocolFamily.InterNetwork.ToString())
                {
                    hostName = hostname;
                    return addr;
                }

            }//end foreach

            hostName = hostname;
            return null;

        }//end getIPv4Addr

        public static IPAddress? GetIPv6Addr(String hostname)
        {
            // Get server related information.
            IPHostEntry heserver = Dns.GetHostEntry(hostname);

            // Loop on the AddressList
            foreach (IPAddress addr in heserver.AddressList)
            {

                if (addr.AddressFamily.ToString() == ProtocolFamily.InterNetworkV6.ToString())
                {
                    //hostName = heserver.HostName;
                    hostName = hostname; 
                    return addr;
                }

            }//end foreach

            hostName = hostname;
            return null;

        }//end getIPv6Addr

        public static IPAddress? GetHostAddrName(String host) //use struct to return multiple values
        {

            IPAddress? iaddr = null;
            try
            {

                if(tcp4Flag)
                {
                    if (Socket.OSSupportsIPv4)
                    {
                        iaddr = GetIPv4Addr(host);
                    }
                    else
                    {
                        Console.WriteLine("OS does not support IPv4!"); 
                        return null;
                    }//end if
                }
                else
                {
                    if (Socket.OSSupportsIPv6)
                    {
                        iaddr = GetIPv6Addr(host);
                    }
                    else
                    {
                        Console.WriteLine("OS does not support IPv6!");
                        return null;
                    }//end if
                } // end if

                if (hostFlag)
                {
                    if (iaddr != null)
                    {
                       
                        hostAddr = iaddr.ToString();
                    }
                    else
                    {
                        throw new Exception(".. Error: Cannot resolve " + Zing.host + " Unknown host.");
                    }//end if

                    hostFlag = false;

                } // end if
            }
            catch (Exception)
            {
                Console.Write(".. Error: Cannot resolve ");
                Console.Write(Zing.host);
                Console.WriteLine(" Unknown host.");
                Environment.Exit(1);
            } // end try

            return iaddr;

        }// end getHostAddrName

        public static void Report(in double time)
        {
            Console.Write(" ");
            Console.Write(limit*ports.Length);
            Console.Write(" ops to ");
            Console.Write(hostName);
            Console.Write(" (");
            Console.Write(hostAddr);
            Console.Write("); ");

            if (time >= 0.0d)
            {
                Console.Write("Active ");
                Console.Write("time = ");
                //Console.Write(time);
                Console.Write(String.Format("{0:0.000}", time));
                Console.WriteLine(" ms");
            }
            else
            {
                Console.Write("Absent!");
                Environment.Exit(0);
            } // end if

        }// end report

        public static double GetTotalTime(in double totalTime, in int portsLength, in int limit)
        {
            double time = totalTime / (double)portsLength / (double)limit;
            return time;
        }// end getTotalTime

        public static double DoZingToHost(in String host, in int port)
        {

            if (inet_addr == null)
            {
                inet_addr = GetHostAddrName(host);
            } // end if

            bool presentFlag = true; // host at socket is present, default is true

            long socketTimeStart = 0, socketTimeClose = 0, socketTimeTotal = 0;

            try
            {
                if(inet_addr == null)
                {
                    Console.WriteLine("Error: Unable to get IP address!");
                    return 0.0d;
                }

                IPEndPoint ip_ep = new(inet_addr, port);

                socketTimeStart  = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;

                Socket sock      = new(inet_addr.AddressFamily,
                                       SocketType.Stream,
                                       ProtocolType.Tcp){
                    SendTimeout  = Zing.timeout
                };

                sock.Connect(ip_ep);

                sock.Close();

                socketTimeClose = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;

            }
            catch (SocketException se)
            {
                Console.WriteLine("Socket Error : {0}", se.ToString());
                presentFlag = false;
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error : {0}", ex.ToString());
                presentFlag = false;
            }//end try

            if (presentFlag)
            {
                socketTimeTotal = socketTimeTotal + (socketTimeClose - socketTimeStart);
            }
            else
            {
                Console.Write(".");
                return -1.0d;
            } // end if

            return (double)socketTimeTotal;

        }//end doZingToHost


        public static double Stddev(in double avg, double[] values)
        {
            double dv = 0;

            foreach (double dbl in values)
            {
                double dm = dbl - avg;
                dv += dm * dm;
            }//end for
            
            return Math.Sqrt(dv / values.Length);

        }//end stddev

        public static void Main(string[] args)
        {

            if (args.Length == 0)
            {
                Usage();
                Environment.Exit(0);

            }//end if

            host = args[0];

            ProcessArgs(args);

            Zing.inet_addr = GetHostAddrName(host);

            Console.Write("ZING: ");
            Console.Write(hostName);
            Console.Write(" (");
            Console.Write(hostAddr);
            Console.Write("): ");
            Console.Write(ports.Length);
            Console.Write(" ports used, ");
            Console.Write((limit * ports.Length));
            Console.Write(" ops per cycle");
            Console.WriteLine();

            long timeZingStart = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;

            Double[] zingTimeTable = new double[count];

            for (int x = 0; x < count; x++)
            {
                double totalTime = 0.0;

                Console.Write("#");
                Console.Write(x + 1);
                Console.Write(" ");

                Console.Write(".");

                for (int y = 0; y < limit; y++)
                {
                    foreach(int port in ports)
                    {
                        double zingTime = DoZingToHost(host, port);
                        zingTimeTable[x] = zingTime;
                        totalTime += zingTime;
                    } // end for(port)
                } // end for(limit)

                Console.Write(".");

                double time = GetTotalTime(totalTime, ports.Length, limit);

                Console.Write(".");

                Report(time); // time = -1.0d, absent, else active

            } // end for(count)

            long timeZingClose = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;

            //get min, max, avg
            double min = Double.MaxValue, max = Double.MinValue, avg = 0.0d;
            for (int x = 0; x < count; x++)
            {

                if (min > zingTimeTable[x])
                {
                    min = zingTimeTable[x];
                }//end if

                if (max < zingTimeTable[x])
                {
                    max = zingTimeTable[x];
                }//end if

                avg += zingTimeTable[x];

            }//end for

            avg /= (double) count;

            double std_dev = Stddev(avg, zingTimeTable);

            Console.WriteLine();
            Console.Write("--- zing summary for ");
            Console.Write(hostName);
            Console.Write("/");
            Console.Write(hostAddr);
            Console.Write("---");
            Console.WriteLine();

            Console.Write(ports.Length * limit * count);
            Console.Write(" total ops used; total time: ");
            Console.Write(timeZingClose - timeZingStart);
            Console.WriteLine(" ms");

            Console.Write("total-time min/avg/max/stddev = ");
            Console.Write(String.Format(TIME_FORMAT, min));
            Console.Write("/");
            Console.Write(String.Format(TIME_FORMAT, avg));
            Console.Write("/");
            Console.Write(String.Format(TIME_FORMAT, max));
            Console.Write("/");
            Console.Write(String.Format(TIME_FORMAT, std_dev));
            Console.WriteLine(" ms");
            Console.WriteLine();
            Console.WriteLine();

            Environment.Exit(0);

        }//end Main

    }//end class Zing

}//end namespace