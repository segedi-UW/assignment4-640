JFLAGS = -g
JC = javac
rsargs=-p 4700 -s localhost -a 5000 -f test.txt -m 1500 -c 5
rrargs=-p 5000 -m 1500 -c 5 -f intest.txt
minisargs=-p 4700 -s 10.130.180.21 -a 5000 -f test.txt -m 1500 -c 5
sargs=-p 4700 -s 10.0.0.1 -a 5000 -f test.txt -m 1500 -c 5


.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = *.java

default: classes

classes: $(CLASSES:.java=.class)

snd : classes
	java -ea TCPend $(rsargs)

rcv : classes
	java -ea TCPend $(rrargs)

sndMini : classes
	java -ea TCPend $(minisargs)

snd2 : classes
	java -ea TCPend $(sargs)

test : classes
	java Test

.PHONY : clean
clean :
	$(RM) *.class

