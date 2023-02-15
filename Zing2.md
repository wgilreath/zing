#### Changes in Zing2:

- as of release 17, all floating-point expressions are evaluated strictly and 'strictfp' is not required
- enhanced switch used
- ```host = args[0]``` assignment is not necessary
- increasing index from within the switch statement is considerend bad practice, so it's been removed
- as a result, for loop iterates without jumping off the flag values; given that args list is limited, it should not affect performance
- if we kept ```idx++``` variant, then it should be also present in ```case "-p"``` statement (missing from original code)
- setter methods added as value assignment in switch statement is considered a bad practice
- refactored to match functional-like style
- removed unnecessary comments

#### Running Zing2 as Java code:

- as of JDK 9, one can run Zing2 using terminal command ```java Zing2.java``` (skipping explicit compilation stage)

#### Running Zing2 as script (still written in Java, but not JavaScript!):

- as of JDK 9, it is possible to run the Java code as Shell script. The script file can have any extension. 
This one has .sj extension to differentiate it from JavaScript .js.

To create executable script in Java, start a file with shebang ```#!```
and full path to java, like ```/home/[PATH]/bin/java``` or a path to Java symbolic link, redirecting to Java location in file system:
```#!/usr/bin/java```. Either of them should be followed by ```--source 17```.

The file is prepared for Linux Shell:

```shell
#!/usr/bin/java --source 17
```

- add executable permission to the file

```shell
chmod a+x ZingScript.sj
```

- then run it like a Shell script
- 
```shell
./ZingScript.sj
```

See README.md for user manual.