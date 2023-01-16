# Zing - the Zero packet Ping network utility

Zing is a lightweight, zero packet utility similar to ping, that provides similar functionality as the ubiquitous ping network utility. The Zing network utility was published in Linux magazine for December 2022 Issue 265/2022 as "Zing Me" here: https://www.linux-magazine.com/Issues/2022/265/Zing#article_i5

Features
========

* No data payload is sent or received, to avoid network congestion.
* Ability to check both for a host and the ports are alive and available.
* Calculate the time to reach a host via connect and disconnect.
* Available to run on multiple platforms in this case Windows, macOS, and Linux.
* Work with Internet Protocol version 4 and version 6 addresses.
* Similar parameters and report of data as ping--a familiar look and feel to the utility.

Installation
============

The Zing network utility requires Java 8 or JDK 8 in order to run. You can download a version of the 
OpenJDK 8 from many sources on the Internet. 

I personally use and like the Azul Systems OpenJDK 8 which you can download from: https://www.azul.com/downloads/?version=java-8-lts&package=jdk 

After downloading, then double-check the JDK is successfully installed by opening a console or terminal and running:

![Verify Java Installed](https://raw.github.com/wgilreath/zing/master//screenshot_java_install_runs.png?raw=true "Verify Java installed")

Running
=======

After successfully compiling the zing network utility, you can run it simply with java as:

* Classfile .class
* Jar file .jar

### Classfile .class


![Run Zing with classfile](https://raw.github.com/wgilreath/zing/master/screenshot_zing_run_java_classfile.png?raw=true "Run Zing with classfile")



### JAR file .jar


![Run Zing with jar file](https://raw.github.com/wgilreath/zing/master/screenshot_zing_run_java_jarfile.png?raw=true "Run Zing with jar file")



### Shell Alias

An alias in Linux/macOS can simplify running the Zing network utility. 


* Zing classfile .class alias


![Run Zing using alias for class file](https://raw.github.com/wgilreath/zing/master/screenshot_alias_class_zing_run.png?raw=true "Run Zing using alias for class file")


* Zing JAR file .jar alias


![Run Zing using alias with JAR file](https://raw.github.com/wgilreath/zing/master/screenshot_alias_jar_zing_run.png?raw=true "Run Zing using alias with JAR file")



Screenshots
===========

Here are some example screenshots showing both **ping** and **zing** in action on the Internet. The ping network utility fails but zing succeeds.

### Ping TCP/IP v4 nist.gov

An example ping failure using **ping** utility to ping "nist.gov" which fails...

![Ping TCP/IP v4 nist.gov](https://raw.github.com/wgilreath/zing/master/screenshot_ping_tcp4_nistgov.png?raw=true "Ping nist.gov")

### Zing IPv4 nist.gov

An example zing success using **zing** utility to zing "nist.gov" which succeeds...

![Zing TCP/IP v4 nist.gov](https://raw.github.com/wgilreath/zing/master/screenshot_zing_tcp4_nistgov.png?raw=true "Zing nist.gov")

### Zing and Ping IPv4 nist.gov

A "side-by-side" comparison of both **ping** and **zing** to the same address of nist.gov...

![Ping TCP/IP v4 nist.gov](https://raw.github.com/wgilreath/zing/master/screenshot_zing_ping_tcp4_nistgov.png?raw=true "Zing and ping nist.gov")

### Zing IPv6 one.one.one.one

An example of **ping** to open-DNS server one.one.one.one using TCP/IP v6 to the host...

![Zing TCP/IP v6 one.one.one.one](https://raw.github.com/wgilreath/zing/master/screenshot_zing_tcp6_oneoneoneone.png?raw=true "Zing one.one.one.one")

Ports to Other Languages
===============================

Zing has been ported as of November 2022 to C# and Python; as of January 2023 Zing has been ported to Go. If you port Zing to another programming language, please reach out and share!

License
===============================

The Zing utility is licensed under the GNU General Public License version 3. The documentation, and man page is licensed under the Creative Commons Attribution-NoDerivatives 4.0 International (CC BY-ND 4.0) license https://creativecommons.org/licenses/by-nd/4.0/

