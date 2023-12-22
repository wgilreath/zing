/*
 * Zing - a Zero-packet InterNet Groper network utility in Rust.
 *
 * The zing network utility checks host by name or ip-address
 *   exists, active, present, and time in milliseconds to reach.
 *
 * William F. Gilreath (will@wfgilreath.xyz)
 * Version 1.2  12/22/23
 *
 * Copyright © 2023 All Rights Reserved.
 *
 * License: This software is subject to the terms of the GNU General Public License (GPL)
 *     version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.
 *
 * You must accept the terms of the GNU General Public License (GPL) license agreement
 *     to use this software.
 *
 */

use std::env;
use std::net::{Shutdown, TcpStream};
use std::net::{SocketAddr, ToSocketAddrs};
use std::num::ParseIntError;
use std::ops::Add;
use std::process::exit;
use std::string::ToString;
use std::time::{Duration, SystemTime};

const EXIT_SUCCESS: i32 = 0;
const EXIT_FAILURE: i32 = 1;

const DEFAULT_COUNT: u16 = 6;
const DEFAULT_LIMIT: u16 = 8;
const DEFAULT_TIMEOUT: u16 = 3000;
const DEFAULT_PORTS: &str = "80,443";
const DEFAULT_HOST: &str = "localhost";
const DEFAULT_TCPV: char = '4';

const ZING_USAGE: &str = "Usage: zing [ -h | [-4|-6] [-c count] [-op ops] [-p ports] [-t timeout] ] host";
const ZING_EXAMPLE: &str = "zing -4 -c 6 -op 8 -p 80,443 -t 3000 google.com";
const ZING_VERSION: &str = "zing Rust 1.2";

struct ZingParam {
    tcpv: char,     //tcp-version '-4' or '-6'
    count: u16,     //-c count
    time: u16,      //-t timeout
    limit: u16,     //-op limit
    host: String,   //host
    ports: String,  //-p port list
}//end struct ZingParam

fn zing_stat(vec: Vec<u128>) -> Vec<f64> {
    let mut stats: Vec<f64> = Vec::new();

    let sdev: f64 = fx_stddev(vec.clone());

    let mean: f64 = fx_mean(vec.clone());

    let mut max: u128 = u128::MIN;
    let mut min: u128 = u128::MAX;

    for val in vec.clone() {
        if min > val {
            min = val;
            continue;
        }//end if

        if max < val {
            max = val;
            continue;
        }//end if
    }//end for

    stats.push(min as f64);  //min :0
    stats.push(mean);        //avg :1
    stats.push(max as f64);  //max :2
    stats.push(sdev);        //sdev:3

    return stats;
}//end zing_stat

fn fx_mean(vec: Vec<u128>) -> f64 {
    let sum = vec.iter().sum::<u128>() as f64;
    let retval: f64 = sum / vec.len() as f64;
    return retval;
}//end fx_mean

fn fx_variance(vec: Vec<u128>, mean: f64) -> f64 {
    let mut variance: f64 = 0.0;

    for val in &vec {
        let diff: f64 = (*val as f64) - mean;

        variance = variance + (diff * diff);
    }//end for

    variance = variance / vec.len() as f64;

    return variance;
}//end fx_variance

fn fx_stddev(vec: Vec<u128>) -> f64 {
    let mean: f64 = fx_mean(vec.clone());
    let vari: f64 = fx_variance(vec.clone(), mean);
    let sdev: f64 = vari.sqrt();

    return sdev;
}//end fx_stddev

fn zing_op(limit: u16, _sock_addr: SocketAddr, _time_out: Duration) -> u128 {
    let mut total_time: u128 = 0;
    let mut limit_time: u128;

    for _idx in 0..limit {
        limit_time = condist(_sock_addr, _time_out);
        total_time = total_time + limit_time;
    }//end for
    total_time = total_time / limit as u128;

    return total_time;
}//end zing_op

fn condist(sock_addr: SocketAddr, time_out: Duration) -> u128 {
    let retval: u128;
    let now = SystemTime::now();
    let stream = TcpStream::connect_timeout(&sock_addr, time_out);
    match stream {
        Ok(stream) => {
            let status = stream.shutdown(Shutdown::Both);

            match status {
                Ok(..) => {
                    match now.elapsed() {
                        Ok(elapsed) => {
                            retval = elapsed.as_micros();
                        }//end Ok
                        Err(e) => {
                            // an error occurred!
                            eprintln!("Error: System Timer... {e:?}!");
                            exit(EXIT_FAILURE);
                        }//end Err
                    }//end match
                }//end Ok(status)
                Err(status) => {
                    eprintln!("Error: Failure Closing Socket... {status:?}!");
                    exit(EXIT_FAILURE);
                }//end Err(status)
            }//end match status
        }//end Ok(stream)
        Err(e) => {
            eprintln!("Error: Failed to Connect... {}!", e);
            exit(EXIT_FAILURE);
        }//end Err(stream)
    }//end match stream

    return retval;
}//end condist

fn get_ip_addr(host_port: String, tcpv: char) -> SocketAddr {
    let addr_iter = host_port.to_socket_addrs();

    if addr_iter.is_err() {
        eprintln!("Error: Socket address... {}!", addr_iter.err().unwrap());
        exit(EXIT_FAILURE);
    }//end if

    for sock_addr in addr_iter.expect("Error: Unable to get socket address from iterator!").as_ref() {
        if sock_addr.is_ipv4() && tcpv == '4' {
            return *sock_addr;
        } else if sock_addr.is_ipv6() && tcpv == '6' {
            return *sock_addr;
        }//end if
    }//end for

    eprintln!("Error: Failed to get ip-address for... {} !", host_port);
    exit(EXIT_FAILURE);
}//end get_ip_addr

fn zing_func(host: String, port: String, tcpv: char, count: u16, limit: u16, timeout: u16) -> Vec<u128> {
    let mut z_time: u128;
    let time_out = Duration::from_millis(timeout.into()); //-t 1500 default time-out
    let host_port = host.clone().add(":").add(&port); //port : String unneeded ?? use &str
    let sock_addr: SocketAddr = get_ip_addr(host_port.to_string(), tcpv);
    let mut time_tbl = Vec::new();

    for _idx in 0..count {
        z_time = zing_op(limit, sock_addr, time_out);
        time_tbl.push(z_time);
        println!("ZING: Port: {:5} {} [{}] Time: {:.3}-ms. ", port, host, sock_addr.ip().to_string(), (z_time as f64 / 1000.0));
    }//end for

    return time_tbl;
}//end _zing_func

fn zing_main(in_zing_param: &ZingParam) {   //main for zing

    let count = in_zing_param.count;
    let limit = in_zing_param.limit;
    let tcpv = in_zing_param.tcpv;
    let timeout = in_zing_param.time;
    let host = in_zing_param.host.clone();
    let ports = in_zing_param.ports.clone();

    let port_list: Vec<_> = ports.split(",").collect();
    let len = port_list.clone().len();
    let host_port = host.clone().add(":").add("80"); //pseudo-port 80 - HTTP
    let sock_addr: SocketAddr = get_ip_addr(host_port.to_string(), tcpv);

    println!();
    print!("ZING: {} ({}) ", host, sock_addr.ip().to_string());
    print!("{} ports used, {} ops per cycle.", len, limit);
    println!();
    println!();

    let mut time_table: Vec<u128> = Vec::new();
    let mut time_zing: Vec<u128>;
    let now = SystemTime::now();

    for port in &port_list {
        time_zing = zing_func(host.to_string(), port.to_string(), tcpv, count, limit, timeout); //return time_tbl for report

        time_table.append(&mut time_zing);
    }//end for

    let zing_time: u128;
    match now.elapsed() {
        Ok(elapsed) => {
            zing_time = elapsed.as_micros();
        }//end Ok
        Err(e) => {
            eprintln!("Error: Unknown Failure with System Timer... {e:?}!"); //unknown error
            exit(EXIT_FAILURE);
        }//end Err
    }//end match

    let stats = zing_stat(time_table);
    let min: f64 = stats[0] / 1000.0;
    let avg: f64 = stats[1] / 1000.0;
    let max: f64 = stats[2] / 1000.0;
    let sdev: f64= stats[3] / 1000.0;

    println!();
    println!("---- zing summary for {}/{} ----", host, sock_addr.ip().to_string());
    println!("{} total ops used; total time: {:.3}-ms", (count * limit * (port_list.len() as u16)), (zing_time as f64 / 1000.0));
    println!("total-time min/avg/max/stddev = {:.3}/{:.3}/{:.3}/{:.3} ms", min, avg, max, sdev);
    println!();
}//end _zing_util

fn usage() {
    println!("{}\n{}\n{}\n", ZING_VERSION, ZING_USAGE, ZING_EXAMPLE);
}//end usage

fn parse_u16(number_str: &String) -> Result<u16, ParseIntError> {
    match number_str.parse::<u16>() {
        Ok(n) => Ok(1 * n),
        Err(err) => Err(err),
    }//end match
}//end parse_u16

fn process_args(args: Vec<String>, mut zing_param: ZingParam) -> ZingParam {
    for idx in 1..args.len() {
        let arg = &args[idx];

        match arg.as_str() {
            "-h" => {
                usage();
                exit(EXIT_SUCCESS);
            },

            "-4" => {
                zing_param.tcpv = '4';
                continue;
            },

            "-6" => {
                zing_param.tcpv = '6';
                continue;
            },

            "-c" => {
                match parse_u16(&args[idx + 1]) {
                    Ok(n) => zing_param.count = n,
                    Err(err) => println!("Error: {:?}", err),
                }//end match
                continue;
            },

            "-p" => {
                zing_param.ports = args[idx + 1].to_string();
                continue;
            },

            "-op" => {
                match parse_u16(&args[idx + 1]) {
                    Ok(n) => zing_param.limit = n,
                    Err(err) => println!("Error: Failure Parsing Number... {:?}!", err),
                }//end match
                continue;
            },

            "-t" => {
                match parse_u16(&args[idx + 1]) {
                    Ok(n) => zing_param.time = n,
                    Err(err) => println!("Error: Failure Parsing Number... {:?}!", err),
                }//end match
                continue;
            },

            _ => {
                let char_vec: Vec<char> = arg.chars().collect();
                let ch = char_vec[0];

                if ch == '-' {
                    //check if arg first char -, then error
                    eprintln!("Error: Invalid parameter '{}' not recognized!", arg);
                    exit(EXIT_FAILURE);
                } else {
                    zing_param.host = args[idx + 0].to_string();
                }//end if
                continue;
            },//end _ =>
        }//end match
    }//end for

    return zing_param;
}//end process_args

fn main() {

    //create default parameters struct with default values
    let zing_p = ZingParam {
        tcpv: DEFAULT_TCPV,
        count: DEFAULT_COUNT,
        time: DEFAULT_TIMEOUT,
        limit: DEFAULT_LIMIT,
        host: DEFAULT_HOST.to_string(),
        ports: DEFAULT_PORTS.to_string(),
    };

    let args: Vec<String> = env::args().collect();

    if args.len() < 2 {
        eprintln!("Error: Invalid number of command-line parameters!");
        usage();
        exit(EXIT_FAILURE);
    }//end if

    let zing_arg = process_args(args, zing_p);

    zing_main(&zing_arg);

    exit(EXIT_SUCCESS);

}//end main
