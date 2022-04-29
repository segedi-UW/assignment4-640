JFLAGS = -g
JC = javac
rsargs=-p 4700 -s localhost -a 5000 -f test.txt -m 1500 -c 5
rrargs=-p 5000 -m 1500 -c 5 -f intest.txt


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

test : classes
	java Test

.PHONY : clean
clean :
	$(RM) *.class

