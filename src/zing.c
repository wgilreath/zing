/*
  Name: zing.c

  Title: Zing - Zero packet pING network utility implemented in C90.

  Description: Zero packet PING utility that checks host by name or ip-address is active,
                 and time to reach that host. The port list is required to be explicit on
                 the command-line interface, no default ports.

  Author William F. Gilreath (will@wfgilreath.xyz)
  Version 1.1  07/09/23

  Copyright   2023 All Rights Reserved.

  License: This software is subject to the terms of the GNU General Public License (GPL)
  version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.
  You must accept the terms of the GNU General Public License (GPL) license agreement
  to use this software.

*/

#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <math.h>
#include <netdb.h>
#include <netinet/in.h>
#include <poll.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

#define OP_VAL 5
#define COUNT_VAL 4
#define TIME_VAL 3
#define HOST "localhost"
#define TCP_VAL '0'

struct timeval timeStart;
struct timeval timeClose;
struct timeval time_zing_start, time_zing_close;

int param_limit = OP_VAL;
int param_count = COUNT_VAL;
int param_time = TIME_VAL;
char *param_host = HOST;
char *param_ports = NULL;
char param_tcp = TCP_VAL;

static double stddev(const double avg, const long values[]) {

  double dv = 0;
  int x = -1;
  for (x = 0; x < param_count; x++) {
    double dbl = (double) values[x];
    double dm = dbl - avg;
    dv += dm * dm;
  }//end for

  return sqrt(dv / param_count);

}//end stddev

int connect_with_timeout(int sockfd, const struct sockaddr *addr, socklen_t addrlen, unsigned int timeout_ms) {

  int rc = 0;
  int sockfd_flags_before;

  if ((sockfd_flags_before = fcntl(sockfd, F_GETFL, 0) < 0))
    return -1;
  if (fcntl(sockfd, F_SETFL, sockfd_flags_before | O_NONBLOCK) < 0)
    return -1;

  do {
    if (connect(sockfd, addr, addrlen) < 0) {
      if ((errno != EWOULDBLOCK) && (errno != EINPROGRESS)) {
        rc = -1;
      } else {
        struct timespec now;
        if (clock_gettime(CLOCK_MONOTONIC, &now) < 0) {
          rc = -1;
          break;
        }
        struct timespec deadline = {.tv_sec = now.tv_sec,.tv_nsec = now.tv_nsec + timeout_ms * 1000000l};

        do {
          if (clock_gettime(CLOCK_MONOTONIC, &now) < 0) {
            rc = -1;
            break;
          }
          int ms_until_deadline = (int) ((deadline.tv_sec - now.tv_sec) * 1000l
                                         + (deadline.tv_nsec - now.tv_nsec) / 1000000l);
          if (ms_until_deadline < 0) {
            rc = 0;
            break;
          }
          struct pollfd pfds[] = {{.fd = sockfd, .events = POLLOUT}};
          rc = poll(pfds, 1, ms_until_deadline);
          if (rc > 0) {
            int error = 0;
            socklen_t len = sizeof(error);
            int retval = getsockopt(sockfd, SOL_SOCKET, SO_ERROR, &error, &len);
            if (retval == 0)
              errno = error;
            if (error != 0)
              rc = -1;
          }//end if
        } while (rc == -1 && errno == EINTR);

        if (rc == 0) {
          errno = ETIMEDOUT;
          rc = -1;
        }//end if
      }//end if
    }//end if
  } while (0);

  if (fcntl(sockfd, F_SETFL, sockfd_flags_before) < 0)
    return -1;

  return rc;

}//end connect_with_timeout

void *get_in_addr(struct sockaddr *sa) {
  if (sa->sa_family == AF_INET) {
    return &(((struct sockaddr_in *) sa)->sin_addr);
  }//end if

  return &(((struct sockaddr_in6 *) sa)->sin6_addr);
}//end get_in_addr

void usage(void) {
  printf("usage: zing ( [-4|-6] [-c <count>] | [-op <limit>] | [-t <timeout>] -p (<port>)+ <host> | -h ) \n\r");
  printf("zing -p 80,443 1.1.1.1");
  printf("\n\r");
  printf("zing -4 -c 6 -op 4 -t 3000 -p 80,443 google.com");
  printf("\n\r");
  exit(0);
}//end usage

char **str_split(char *a_str, const char a_delim) {

  char **result = 0;
  size_t count = 0;
  char *tmp = a_str;
  char *last_comma = 0;
  char delim[2];
  delim[0] = a_delim;
  delim[1] = 0;

  while (*tmp) {
    if (a_delim == *tmp) {
      count++;
      last_comma = tmp;
    }
    tmp++;
  }

  count += last_comma < (a_str + strlen(a_str) - 1);

  count++;

  result = malloc(sizeof(char *) * count);

  if (result) {
    size_t idx = 0;
    char *token = strtok(a_str, delim);

    while (token) {
      *(result + idx++) = strdup(token);
      token = strtok(0, delim);
    }
    *(result + idx) = 0;
  }

  return result;
}//end str_split

void process_args(int argc, char *argv[]) {

  int x = -1;
  for (x = 1; x < argc; x++) {
    char chr = argv[x][0];
    if (chr == '-') {
      switch (argv[x][1]) {
        case 'o' :
          if (argv[x][2] == 'p') {
            param_limit = atoi(argv[x + 1]);
            x++;
            break;
          } else {
            printf("Error invalid CLI argument: %s\n\r", argv[x]);
            exit(1);
          }//end if
        case 'p' :
          param_ports = argv[x + 1];
          x++;
          break; //port
        case 'c' :
          param_count = atoi(argv[x + 1]);
          x++;
          break; //count
        case 'h' :
          usage();
          break;
        case 't' :
          param_time = atoi(argv[x + 1]);
          x++;
          break; //timeout
        case '4' :
          param_tcp = '4';
          x++;
          break;
        case '6':
          param_tcp = '6';
          x++;
          break;
        default:
          printf("Error: Invalid command-line argument: %s!\n\r", argv[x]);
          exit(1);
      }//end switch
    } else {
      param_host = argv[x];
    }//end if
  }//end for

  if (param_ports == NULL) {
    printf("Error: Missing explicit '-p <port0>,<port1>,...' port list!");
    printf("\n\r");
    exit(1);
  }//end if

}//end process_args

int main(int argc, char *argv[]) {

  if (argc < 2) {
    usage();
  }//end if

  process_args(argc, argv);

  int portc = 0;
  char **port_list;
  port_list = str_split(param_ports, ',');
  for (portc = 0; *(port_list + portc); portc++);

  int tbl_pos = 0;
  long *time_tbl = malloc(sizeof(long) * param_count * portc);

  int sockfd = -1;
  struct addrinfo hints, *servinfo, *p;
  int rv;
  char s[INET6_ADDRSTRLEN];

  memset(&hints, 0, sizeof hints);

  switch (param_tcp) {
    case '4' :
      hints.ai_family = AF_INET;
      break;
    case '6' :
      hints.ai_family = AF_INET6;
      break;
    default  :
      hints.ai_family = AF_UNSPEC;
      break;
  }//end switch

  hints.ai_socktype = SOCK_STREAM;

  int _idx_port = -1;
  char header_flag = 't';
  char *ip_addr = "";

  gettimeofday(&time_zing_start, NULL);

  for (_idx_port = 0; _idx_port < portc; _idx_port++) {
    char *port = port_list[_idx_port];

    if ((rv = getaddrinfo(param_host, port, &hints, &servinfo)) != 0) {
      fprintf(stderr, "Error getting host address: %s!\n\r", gai_strerror(rv));
      return 1;
    }//end if

    int counter = 0;
    int limiter = 0;

    long acc_time = 0;
    next_socket:;

    for (p = servinfo; p != NULL; p = p->ai_next) {
      if ((sockfd = socket(p->ai_family, p->ai_socktype,
                           p->ai_protocol)) == -1) {
        perror("client: socket");
        continue;
      }//end if
      break;
    }//end for

    while (counter < param_count) {
      while (limiter < param_limit) {
        int con_stat = -1;
        errno = 0;

        gettimeofday(&timeStart, NULL);
        con_stat = connect_with_timeout(sockfd, p->ai_addr, p->ai_addrlen, param_time * 1000);
        if (con_stat == -1) {
          if (errno == ETIMEDOUT) {
            printf("Error connecting to host: %s port: %s timed out!\n\r", param_host, port);
            return 2;
          }//end if
          close(sockfd);
          goto next_socket;
        }//end if

        if (p == NULL) {
          fprintf(stderr, "client: failed to connect\n");
          return 2;
        }//end if

        ip_addr = (char *) inet_ntop(p->ai_family,
                                     get_in_addr((struct sockaddr *) p->ai_addr),
                                     s, sizeof s);

        close(sockfd);
        gettimeofday(&timeClose, NULL);

        if (header_flag == 't') {
          printf("\n\rZING: %s (%s): %d ports used, %d ops per cycle.\n\r\n\r", param_host, ip_addr, portc,
                 (param_limit * portc));
          header_flag = 'f';
        }//end if

        long elapsed =
          (timeClose.tv_sec - timeStart.tv_sec) * 1000000 + timeClose.tv_usec - timeStart.tv_usec;
        double elapsed_d = (double) elapsed / 1000.0;

        acc_time = acc_time + elapsed;

        printf("ZING: Port: %-5s %s [%s] Time: %4.3f-ms.\n\r",
               port,
               param_host,
               ip_addr,
               elapsed_d);
        limiter++;
      }//end while limiter

      time_tbl[tbl_pos] = acc_time;
      tbl_pos++;
      acc_time = 0;
      counter++;
      limiter = 0;
    }//end while counter

  }//end for _idx_port

  gettimeofday(&time_zing_close, NULL);
  long time_zing_total = (time_zing_close.tv_sec - time_zing_start.tv_sec) * 1000000 + time_zing_close.tv_usec - time_zing_start.tv_usec;
  double min = time_tbl[0], max = time_tbl[0], avg = 0.0;
  int x = -1;
  for (x = 1; x < param_count; x++) {
    if (min > time_tbl[x]) {
      min = time_tbl[x];
    }//end if
    if (max < time_tbl[x]) {
      max = time_tbl[x];
    }//end if
    avg += time_tbl[x];
  }//end for

  avg = avg / (double) param_count;

  double std_dev = stddev(avg, time_tbl);

  //convert from micro-seconds to milli-seconds
  min = min / 1000.0;
  max = max / 1000.0;
  avg = avg / 1000.0;
  std_dev = std_dev / 1000.0;

  printf("\n--- zing summary for %s/%s ---\n", param_host, ip_addr);
  printf("%d total ops used; total time: %ld ms\n", (portc * param_limit * param_count),
         (time_zing_total / 1000)); //(timeZingClose - timeZingStart));

  printf("total-time min/avg/max/stddev = %.3f/%.3f/%.3f/%.3f ms", min, avg, max, std_dev);
  printf("\n\r");

  return 0;

}//end main