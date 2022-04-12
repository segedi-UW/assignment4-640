rargs=

TCPend.class: TCPend.java
	javac TCPend.java

run: TCPend.class
	java TCPend $(rargs)
