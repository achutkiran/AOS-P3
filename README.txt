CS6378 Advanced Operating Systems
Project - 3
Team Members:
	Name: Pruthvi Vooka Net-id:pxv162030
	Name: Rohith Reddy Krishnareddi Gari Net-id: rxk152130

Steps:
1. Extract the archive

2. Compile the java files using javac Node.java either in the local machine or in the server(dcxx machine)

3. Place the Source Code class files in the Project Directory($PROGRAM_PATH)

4. Give execute permissions to launcher.sh & cleanup.sh

5. Start the system using: ./launcher.sh <name-of-config-file> <net-id> <protocol>
	For <protocol> Give '1' for Lamport Mutex Protocol
		       Give '2' for Ricart and Agarwala Mutex Protocol

6. After all processes terminate, Cleanup using: ./cleanup.sh <name-of-config-file> <net-id>

7. Now the output logs will be in results.out

8. To test the correctness of the output we provided a test.java file which checks if No. of critical section requests 
   for each node are satisfied and no two processes are in critical section at the same time, thus ensuring mutex.

9. Compile test file using javac test.java

10. Run it using java test results.out <No.-of-nodes>

Note: A sample results.out file has been provided.

Sample run of test.java
No. of Critical Sections executed for each node:
Node-0: 50
Node-1: 50
Node-2: 50
Node-3: 50
Node-4: 50
MUTEX Sucess: No two processes are in critical section at the same time
