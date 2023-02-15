Changes in Zing2:

- as of release 17, all floating-point expressions are evaluated strictly and 'strictfp' is not required
- enhanced switch used
- ```host = args[0]``` assignment is not necessary
- increasing index from within the switch statement is considerend bad practice, so it's been removed
- as a result, for loop iterates without jumping off the flag values; given that args list is limited, it should not affect performance
- if we kept ```idx++``` variant, then it should be also present in ```case "-p"``` statement (missing from original code)
- setter methods added as value assignment in switch statement is considered a bad practice

Running Zing2 as Java code:

- as of JDK 9, one can run Zing2 using terminal command ```java Zing2.java``` (without previous compilation)

Running Zing2 as script (still written in Java, but not JavaScript!):

// TODO adjust the file name, extension and so on...

[LINUX]

- add executable permission to the file

```shell
chmod a+x Zing2.script
```

- then run it like a Shell script
```shell
./Zing2script.sj
```