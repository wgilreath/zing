/*
	Name: Zing.go

	Title: Zing - Zero packet pING network utility implemented in Golang.

	Description: Zero packet PING utility that checks host by name or ip-address is active, and time to reach.

	Author William F. Gilreath (will@wfgilreath.xyz)
	Version 1.1  01/15/23

	Copyright © 2023 All Rights Reserved.

	License: This software is subject to the terms of the GNU General Public License (GPL)
	version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.
	You must accept the terms of the GNU General Public License (GPL) license agreement
	to use this software.
*/

package main

import (
	"fmt"
	"math"
	"net"
	"os"
	"strconv"
	"strings"
	"time"
)

const (
	ZingUsage   = "Usage: zing -h | [-4|-6] [-c count] [-op ops] [-p ports] [-t timeout] host"
	ZingExample = "zing -4 -c 4 -op 4 -p 80,443 -t 4000 google.com"

	ConstHostName = "localhost"
	ConstHostAddr = "127.0.0.1"
	ConstTimeout  = 3000
	ConstCount    = 4
	ConstLimit    = 4
	ConstTcpMode  = "tcp" //"tcp"  "tcp4", "tcp5"
	ConstPorts    = "80,443"
)

type ZingArgs struct {
	netHostName string //= "localhost"
	netHostAddr string //= "127.0.0.1"
	netPorts    string //= ports_default
	gLimit      int    //= ConstLimit
	gCount      int    //= ConstCount
	tcpMode     string //= ConstTcpMode
	timeout     int    //= ConstTimeout
} //end struct ZingArgs

func getTotalTime(totalTime float64, portsLength int, limit int) float64 {

	var timeResult = totalTime / float64(portsLength) / float64(limit)
	return timeResult

} // end getTotalTime

func report(time float64, portCount int, args ZingArgs) {

	fmt.Printf(" %d ops to %s (%s): ", args.gLimit*portCount, args.netHostName, args.netHostAddr)

	if time >= 0.0 {
		fmt.Print("Active ")
		fmt.Printf("time = %.3f ms\n", time)
	} else {
		fmt.Println("Absent!")
		os.Exit(0)
	} // end if

} //end report

func processArgs(argv ZingArgs) ZingArgs {

	for idx, arg := range os.Args[1:] {

		switch arg {

		case "-4":
			argv.tcpMode = "tcp4"
			break

		case "-6":
			argv.tcpMode = "tcp6"
			break

		case "-c":
			lCount, err := strconv.ParseInt(os.Args[idx+2], 10, 32)
			argv.gCount = int(lCount)
			if err != nil {
				fmt.Printf("\nError: %s\n\n", err)
				fmt.Printf("Using default value of %d for -c param.\n\n", ConstCount)
				argv.gCount = ConstCount
			} //end if
			idx++
			break

		case "-op":
			gLimit, err := strconv.ParseInt(os.Args[idx+2], 10, 32)
			argv.gLimit = int(gLimit)

			if err != nil {
				fmt.Printf("\nError: %s\n\n", err)
				fmt.Printf("Using default value of %d for -op param.\n\n", ConstCount)
				argv.gLimit = ConstLimit
			} //end if
			idx++
			break

		case "-p":
			var lPorts = os.Args[idx+2]
			argv.netPorts = lPorts
			break

		case "-t":
			timeout, err := strconv.ParseInt(os.Args[idx+2], 10, 32)
			argv.timeout = int(timeout)
			if err != nil {
				fmt.Printf("\nError: %s\n\n", err)
				fmt.Printf("Using default value of %d for -t param.\n\n", ConstTimeout)
			}
			idx++
			break

		case "-h":
			usage()
			os.Exit(0)

		default:
			if arg[0] == '-' {
				fmt.Printf("Error '%s' is invalid command-line parameter!\n\n", arg)
				os.Exit(1)
			} else {
				argv.netHostName = arg
				argv.netHostAddr = "" //const EMPTY_STRING
			} //end if
		} //end switch

	} //end for

	return argv

} //end processArgs

func usage() {

	fmt.Printf("\n%s\n%s\n\n", ZingUsage, ZingExample)

} //end usage

func stddev(avg float64, values []float64) float64 {

	var dv = 0.0
	var dm = 0.0
	var size = len(values)

	for _, dbl := range values {

		dm = dbl - avg
		dv += dm * dm
	} //end for

	return math.Sqrt(dv / float64(size))

} //end stddev

func doZingToHost(args ZingArgs, hostPort string) (float64, bool) {

	var presentFlag = false
	start := time.Now()

	timeoutVal := time.Millisecond * time.Duration(args.timeout)

	netDial := net.Dialer{Timeout: timeoutVal} //field Deadline
	conn, err := netDial.Dial("tcp", hostPort)
	if err != nil {
		fmt.Printf("\n\n")
		fmt.Printf("Error: %s!\n\n", err) //work with err, create func
		return -1.0, false
	} //end if

	defer func(conn net.Conn) {
		err := conn.Close()
		if err != nil {
			fmt.Printf("Error: %s\n\n", err) //error disconnecting, continue
			fmt.Printf("Continuing zing utility...\n\n")
		} //end if
	}(conn)

	elapsed := time.Since(start)
	presentFlag = true

	if !presentFlag {
		fmt.Print(".")
		return -1.0, false
	} // end if

	return float64(elapsed.Milliseconds()), true

} //end doZingToHost

func main() {

	var defaultConfig = ZingArgs{ConstHostName,
		ConstHostAddr,
		ConstPorts,
		ConstLimit,
		ConstCount,
		ConstTcpMode,
		ConstTimeout,
	}

	if len(os.Args[1:]) < 1 { //need at least host param
		usage()
		os.Exit(0)
	} //end if

	config := processArgs(defaultConfig)

	ports := strings.Split(config.netPorts, ",")

	portCount := len(ports)

	var timeTable = make([]float64, config.gCount)

	var totalZingTime int64 = 0

	var portIndex = 0

portLoop:
	for _, port := range ports {

		lHost := config.netHostName + ":" + port

		hostAddr, err := net.ResolveTCPAddr(config.tcpMode, lHost)

		if config.netHostAddr == "" {
			config.netHostAddr = hostAddr.IP.String()
		} //end if

		if err != nil {
			fmt.Printf("\nError: %s\n\n", err)
			fmt.Printf("Unable to continue zing utility...exiting.\n\n") //unable to resolve tcp-address, exit
		} //end if

		fmt.Printf("ZING: %s (%s): %d ports used, %d ops per cycle\n", config.netHostName, config.netHostAddr, portCount, config.gLimit*portCount)

		var timeZingStart = time.Now()

		for x := 0; x < config.gCount; x++ {

			var zingTime = 0.0
			var totalTime = 0.0

			fmt.Printf("#%d ", x+1)
			fmt.Printf(".")

			var flag = true

			for y := 0; y < config.gLimit; y++ {

				zingTime, flag = doZingToHost(config, lHost) //return error flag ??

				if !flag {
					fmt.Printf("Error: Unable to connect to port: %s; continue to next port on host: '%s'...\n\n", port, config.netHostName)
					portIndex++
					continue portLoop
				} //end if

				timeTable[x] = zingTime
				totalTime += zingTime

			} //end for limit

			fmt.Print(".")
			var timeTotal = getTotalTime(totalTime, portCount, config.gLimit)

			fmt.Print(".")
			report(timeTotal, portCount, config) // time = -1.0d, absent, else active

		} //end for count

		timeZingClose := time.Since(timeZingStart)
		totalZingTime = timeZingClose.Milliseconds()
		fmt.Printf("Total time: %d ms\n", timeZingClose.Milliseconds())

	} //end idx, port

	//get min, max, avg
	var min = +math.MaxFloat64 //= +1_000_000.0
	var max = -math.MaxFloat64 //= -1_000_000.0
	var avg = 0.0

	for _, val := range timeTable {

		if min > val {
			min = val
		} //end if

		if max < val {
			max = val
		} //end if

		avg += val

	} //end for

	avg = avg / float64(config.gCount)
	var stdDev = stddev(avg, timeTable)

	fmt.Printf("\n--- zing summary for %s", config.netHostName)
	fmt.Print("/")
	fmt.Printf("%s ---\n", config.netHostAddr)
	fmt.Printf("%d total ops used; ", portCount*config.gLimit*config.gCount)
	fmt.Printf("total time: %d ms\n", totalZingTime)
	fmt.Printf("total-time min/avg/max/stddev = %.3f/%.3f/%.3f/%.3f ms", min, avg, max, stdDev)
	fmt.Printf("\n\n")

	os.Exit(0)

} //end main
