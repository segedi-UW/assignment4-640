cargs=
rsargs=-p 4700 -s localhost -a 5000 -f test.txt -m 1500 -c 5
rrargs=-p 5000 -m 1500 -c 5 -f intest.txt

TCPend.class : *.java
	javac TCPend.java $(cargs)

snd : TCPend.class
	java -ea TCPend $(rsargs)

rcv : TCPend.class
	java -ea TCPend $(rrargs)

.PHONY : clean
clean :
	@-rm -f *.class

