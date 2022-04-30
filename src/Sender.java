import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Sender extends Transport {
	final private String rip; // remote ip
	private FileInputStream reader;
	private byte[] buf;
	private int bufn;
	private int nextBufSeq;
	private int currentSeq;
	private int currentSeqAcks;
	// ArrayList buffer (protected)
	// constructor fields (protected)
	// Udp Socket (protected)

	public Sender(int lp, int rp, String rip, String filename, int mtu, int sws) throws SocketException {
		super(lp, rp, filename, mtu, sws);
		this.rip = rip;
		try {
			this.addr = InetAddress.getByName(rip);
			buf = new byte[sws];
			bufn = 0;
			reader = new FileInputStream(filename);
		} catch (UnknownHostException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} 
	}

	private void bufferWindow() throws IOException {
		bufn = Math.min(reader.available(), buf.length);

		for (int i = 0; i < bufn; i++)
			buf[i] = (byte) reader.read();

		nextBufSeq += sws;
	}

	/*
	 * A            			B
	 * |						|
	 * | SYN seq=x ->			|
	 * |						|
	 * |   <- SYN seq=y ACK=x+1	| Set ACK and set ackNum to x+1
	 * | 						|
	 * | ACK=y+1 ->				|
	 * |						|
	 */
	@Override
	protected DatagramPacket initConnection() {
		try {
			TCPpacket init = new TCPpacket();
			init.setSyn();
			init.setSeq(3); // FIXME random within reason - look up details
			init.setCurrentTime();
			DatagramPacket bufdp = new DatagramPacket(new byte[mtu], mtu, addr, rp);

			sendData(bufdp, init);

			System.out.println("Reading");
			TCPpacket initRsp = receiveData(bufdp, init);
			System.out.println("Received packet");
			if (!initRsp.isSyn() || !initRsp.isAck()) {
				System.err.println("Expected SYN and ACK to be set");
				return null;
			} else if (initRsp.getAckNum() != init.getSeq()+1) {
				System.err.printf("Ack Expected (%d) Actual (%d)\n", init.getSeq()+1, initRsp.getAckNum());
				return null;
			}

			TCPpacket rspAck = new TCPpacket();
			rspAck.setAck();
			rspAck.setAckNum(initRsp.getSeq()+1);
			rspAck.setCurrentTime();
			sendData(bufdp, rspAck);
			System.out.println("Sent Acknowledgement");
			return bufdp;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	private boolean fillBuffer(FileInputStream in, int[] seqs) throws IOException {
		TCPpacket tmp;
		byte[] dataBuffer = new byte[maxDataSize];
		for(int i=0; i<this.buffer.length; i++){
			if(buffer[i] != null){
				continue;
			}
			int rc = in.read(dataBuffer);
			if(rc == -1){
				return true;
			}
			tmp = new TCPpacket();
			tmp.setData(dataBuffer, 0, rc);
			tmp.setAck();
			tmp.setAckNum(1);
			tmp.setSeq(currentSeq);
			seqs[i] = currentSeq;
			this.buffer[i] = tmp;
			System.out.println("Buffer "+i+" filled with "+ rc+" bytes of data with Seq: "+ currentSeq);
			currentSeq += rc;
			currentSeqAcks = 0;
		}
		return false;
	}

	private TCPpacket sendBuffer() {
		for(int i = 0; i<this.buffer.length; i++) {
			if(buffer[i] == null){
				continue;
			}
			buffer[i].setCurrentTime();
			sendData(buffer[i]);
		}
		TCPpacket incoming = receiveDataTransfer(buffer[0]);
		return incoming;
	}

	private void moveBufferWindow(int[] seqs, int ackNum) {
		boolean found = false;
		for(int i = 0; i<seqs.length; i++){
			if (seqs[i]==ackNum){
				moveWindow(i, seqs);
				found = true;
			}
		}
		if(found == false) {
			for(int i = 0; i<buffer.length; i++) {
				buffer[i] = null;
			}
		}
	}

	private void moveWindow(int toFree, int[] seqs) {
		int ind = 0;
		for(int i = toFree; i<buffer.length;i++){
			buffer[ind] = buffer[i];
			seqs[ind] = seqs[i];
			ind += 1;
		}
		while(ind < buffer.length){
			buffer[ind] = null;
			seqs[ind] = -1;
			ind += 1;
		}
	}

	@Override
	protected TCPpacket transferData() {
		System.out.println("Starting Transfer");
		currentSeq = 1;
		boolean endReached = false;
		int[] seqs = new int[buffer.length];
		try (FileInputStream in = new FileInputStream(this.filename);) {
			while(!endReached){
				endReached = fillBuffer(in, seqs);
				TCPpacket incoming = sendBuffer();
				// System.out.println("Incoming ACK: "+ incoming.getAckNum());
				moveBufferWindow(seqs, incoming.getAckNum());
				this.currentAck = incoming.getAckNum();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	@Override
	protected void termConnection(TCPpacket finPacket) { //TODO: Need to add time wait state
		System.out.println("Starting Termination");
		TCPpacket finInit = new TCPpacket();
		finInit.setAck();
		finInit.setFin();
		finInit.setSeq(this.currentAck);
		finInit.setAckNum(1);
		finInit.setCurrentTime();
		sendData(finInit);


		TCPpacket prev = receiveData(finInit);
		this.currentAck = prev.getAckNum();
		if(!prev.isFin() || !prev.isAck()){
			System.out.println("Got bad fin Packet back from reciever");
			System.exit(1);
		}

		TCPpacket finFinal = new TCPpacket();
		finFinal.setAck();
		finFinal.setAckNum(2);
		finFinal.setFin();
		finFinal.setSeq(this.currentAck);
		finFinal.setCurrentTime();
		sendData(finFinal);
		System.out.println("Connection Terminated on Sender");
	}
}
