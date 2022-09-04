# Zing - the Zero packet Ping network utility

About
=============================

Zing is a lightweight, zero packet utility similar to ping, that provides similar functionality as the ubiquitous ping network utility.

Features
========

* No data payload is sent or received, to avoid network congestion.
* Ability to check both for a host and the ports are alive and available.
* Calculate the time to reach a host via connect and disconnect.
* Available to run on multiple platforms in this case Windows, macOS, and Linux.
* Work with Internet Protocol version 4 and version 6 addresses.
* Similar parameters and report of data as ping--a familiar look and feel to the utility.

Screenshots
===========

Here are some example screenshots showing both **ping** and **zing** in action on the Internet. The ping network utility fails but zing succeeds.

* Ping TCP/IP v4 nist.gov

An example ping failure using **ping** utility to ping "nist.gov" which fails...

![Ping TCP/IP v4 nist.gov](https://raw.github.com/wgilreath/zing/master/screenshot_ping_tcp4_nistgov.png?raw=true "Ping nist.gov")

* Zing IPv4 nist.gov

An example zing success using **zing** utility to zing "nist.gov" which succeeds...

![Zing TCP/IP v4 nist.gov](https://raw.github.com/wgilreath/zing/master/screenshot_zing_tcp4_nistgov.png?raw=true "Zing nist.gov")

* Zing and Ping IPv4 nist.gov

A "side-by-side" comparison of both **ping** and **zing** to the same address of nist.gov...

![Ping TCP/IP v4 nist.gov](https://raw.github.com/wgilreath/zing/master/screenshot_zing_ping_tcp4_nistgov.png?raw=true "Zing and ping nist.gov")

* Zing IPv6 one.one.one.one

An example of **ping** to open-DNS server one.one.one.one using TCP/IP v6 to the host...

![Zing TCP/IP v6 one.one.one.one](https://raw.github.com/wgilreath/zing/master/screenshot_zing_tcp6_oneoneoneone.png?raw=true "Zing one.one.one.one")


License
===============================

This compiler is licensed under the GNU General Public License version 3. The user book is licensed under the Creative Commons share alike attribution license version 4.

