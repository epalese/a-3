Welcome to REDS, the REconfigurable Dispatching System!

REQUIREMENTS:

REDS is written in Java, you need Java2 installed. If you do not have it, you
can download it from http://java.sun.com for your platform.

Any recent Java2 JDK should work. Our preferred version is JDK 1.6.x, but prior JDKs
should also work, with minor modifications to the sources.

DIRECTORY STRUCTURE:

The distribution is organized as follow: 

- The root folder includes the jar of the library (reds.jar) and the jar of
  the examples (reds-examples.jar). It also includes the ant build file (see
  below) that you can use to build REDS yourself.
  
- Folder "src" contains the main sources of REDS (both library and examples).

- Folder "doc" contains the javadoc documentation of the REDS library.


BUILDING REDS YOURSELF:

To compile the library yourself use ant (http://www.ant.org). Use:
   ant -projecthelp
to see the targets provided by the REDS build file. 

Basically you can use:
   ant jars
to build everything and prepare the jars. Similarly,
   ant doc
builds the javadoc documentation of the REDS library.


USAGE:

To use the REDS library to compile/run REDS applications include it into your
classpath or copy it into the extension folder of you j2se installation (i.e.,
the "JAVA_HOME/jre/lib/ext/" folder).

To run the examples try:
   java -cp reds.jar:reds-examples.jar polimi.reds.examples.Broker
and
   java -cp reds.jar:reds-examples.jar polimi.reds.examples.Client

Other examples are included into the folder src/polimi/reds/examples (and they
are included into the reds-examples.jar file).


CODEBASE:

To use your own filters and messages you need to specify a codebase like in
RMI to let remote brokers to download the code of you classes. The
"polimi.reds.client.codebase" property can be used to specify a location from
which any such class can be downloaded. The codebase property can refer to:

* The URL of a directory in which the classes are organized in package-named
  sub-directories

* The URL of a JAR file in which the classes are organized in package-named
  directories

* A space-delimited string containing multiple instances of JAR files and/or
  directories that meet the criteria above

Note: When the codebase property value is set to the URL of a directory, the
value must be terminated by a "/".

For example:
    java -cp reds.jar:reds-examples.jar -Dpolimi.reds.client.codebase=<yourCodebase>
	 <yourApp>
where <yourCodebase> means the URL of a location where it is possible to find
the .class of your messages and filters and <yourApp> is the Main class of your
application.

Note: if you want to use a codebase you must grant the brokers and the client the 
necessary rights to download the classes. At this purpose you must specify a 
Security Manager and a security policy.

For example:	
	java -cp reds.jar:reds-examples.jar -Djava.security.manager 
		-Djava.security.policy=<yourPolicyFile> polimi.reds.examples.Broker 


ECLIPSE USERS:

REDS sources require source 1.6 compatibility. To enable source 1.6
compatibility in Eclipse go to:
    windows->preferences->compiler->compliance&classfiles 
and set "Generated .class compatibility" to 1.6 and then "Source
Compatibility" to 1.6


-------------------------------------------------------------------
REDS - REconfigurable Dispatching System
Copyright (C) 2003 Politecnico di Milano
<mailto: cugola@elet.polimi.it> <mailto: picco@elet.polimi.it>

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
-------------------------------------------------------------------
