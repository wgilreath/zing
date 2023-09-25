#!/usr/bin/env bash
#
# A bash script implementation of zing, the zero packet Internet groper 
# for network host/port
#
# Copyright (C) 2023 William F. Gilreath <will@wfgilreath.xyz>
#
# This file is part of zing <https://github.com/wgilreath/zing/>.
#
# Zing is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Zing is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
# See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with zing.  If not, see <http://www.gnu.org/licenses/>.
#

# function reports usage list CLI parameters
usage()
{
  echo "Usage: $0 ([-c <count>][-op <limit>][-p <port>][-t timeout] <hostname>|(-h|--help))"
}

# function computes standard deviation 'awk' on POSIX, 'mawk' on Windows
stddev()
{
  val=$(echo ${time_array[*]} | 
  mawk 'NF { sum=0;
            ssq=0;
            for (i=1;i<=NF;i++){
              sum+=$i;
              ssq+=$i^2
            }; 
            print (ssq/NF-(sum/NF)^2)^0.5
          }')
  printf "%.${2:-2}f" "$val"
} 

# initialize defaults for variables 
 
var_time="5"         # default timeout is 5-seconds
var_count="6"        # default count is 6-ops
var_limit="4"        # default limit is 4-zings per op
var_host="localhost" # default host is localhost
var_ports="80,443"   # default port list is 80-http, 443-https

total_time="0"
 
# check if have at least 1-CLI arg, if not report usage

if [ "$#" -lt 1 ]; then
    echo
    echo "Error: $0 needs at least one command-line argument!"
    echo
    usage
    echo
    exit 1
fi

# check whether user had supplied -h or --help; if so display usage
if [[ ( $@ == "--help") ||  $@ == "-h" ]]
then 
  usage
  exit 1
fi 

while [ "$#" -gt 0 ]; do
  case "$1" in
     -c) var_count="$2"; shift 2;;
    -op) var_limit="$2"; shift 2;;
     -p) var_ports="$2"; shift 2;;
     -t) var_time="$2";  shift 2;;   
     -*) echo "unknown option: $1" >&2; exit 1;;
      *) var_host="$1";  shift 1;;
  esac
done

if [ $var_host == "localhost" ]
then
  echo "Warning: No host address or name using locahost for host!"
  echo
fi

hosttext=`nc -v -z -w $var_time $var_host 80 2>&1 | tr -s '\n' ' '`

gtext=$(echo $hosttext | grep 'Warning' | cut -f1 -d'`')

if [[ $gtext != "" ]]; then
  echo "$gtext$var_host; unable to continue!"
  exit 1
fi

etext=$(echo $hosttext | grep 'Error')

if [[ $etext != "" ]]; then
  echo
  echo "$etext!"
  echo
  exit 1
fi


# get host address, actual name using netcat 

hostaddr=$(echo "$hosttext" | cut -f8 -d ' ' | tr '[]' ' ')
hostname=$(echo "$hosttext" | cut -f4 -d ' ')

var_ports=$(echo "$var_ports" | tr ',' ' ')

acc_time="0"
time_array=()

# walk through the port list, and do count ops, with limit of operations per zing
for port in $var_ports
do
  echo
  var_status=`nc -v -z -w $var_time $var_host $port &>/dev/null && echo "Active" || echo "Inactive"`
  
  echo -n "ZING:$hostaddr/ $hostname on $port is: $var_status"
  if [ $var_status == "Inactive" ]; then
    echo "! Aborting."
    echo    
    exit 1
  fi
  echo ". Continue."
  echo

  for (( i=1 ; i<=$var_count ; i++ )); #for count--number of ops
  do

    for (( j=1 ; j<=$var_limit ; j++ )); #for limit--number of cmds for op
    do
      result=""

      echo -n "Port: $port: "
      start_time=`date +%s%N` # gdate is for MacOS else 'date' on POSIX

      result=`nc -v -z -w $var_time $var_host $port 2>&1 | cut -f6-9 -d' '`

      close_time=`date +%s%N`

      rtext=$(echo $result | grep 'Error')
      if [[ $rtext != "" ]]; then
        echo "$rtext!"
        echo 
        exit 1
      fi

      ns_time=`expr $close_time - $start_time`

      total_time=`expr $total_time + $ns_time`
      op_time=`expr $total_time / 1000000`

      echo -n "op $i.$j. "
      fmt_result="$hostname $hostaddr"

      echo -n "$fmt_result Time: "
      echo -n "$op_time"
      echo " ms."
    done
    total_time="0"

    op_time=$(expr $op_time / $var_limit)
    acc_time=`expr $acc_time + $op_time`
    time_array+=( "$op_time" )
  done
done

acc_time=$(expr $acc_time / $var_count)
min_time="${time_array[0]}"
max_time="${time_array[0]}"

for tval in "${time_array[@]}"
do
  if [[ $tval -lt $min_time ]]; then
    min_time="$tval"
  fi

  if [[ $tval -gt $max_time ]]; then
    max_time="$tval"
  fi
done

dev_time=$(stddev)

echo 
echo "--- ZING: host at ip-address:$hostaddr"
echo "--- host name: $hostaddr/$hostname "
echo "--- for $var_count ops at limit of $var_limit per op; statistics:"
echo 
echo "Time for min/avg/max/stddev=$min_time/$acc_time/$max_time/$dev_time-ms"
echo

exit 0
