cargs=
rargs=-ea

TCPend.class : TCPend.java
	javac TCPend.java $(cargs)

run : TCPend.class
	java TCPend $(rargs)

.PHONY : clean
clean :
	@-rm -f *.class

