import java.util.List;

import java.net.StandardSocketOptions;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *  The Transport class handles the bulk of the TCP general
 *  processing. Specifics are implemented via the handlePacket(DatagramPacket)
 *  method of the implementing class.
 */
public abstract class Transport {
	final protected int lp; // local port
	final protected String filename;
	final protected int mtu;   // max transmission unit
	final protected int sws;    // sliding window size
	final protected TCPpacket[] buffer;
	final protected double a = .875;	// timout var
	final protected double b = 1 - a;	// timeout var
	final protected int maxDataSize;

	protected int rp; // remote port
	private DatagramPacket bufferdp;
	private byte[] arraydp;
	protected int currentAck;
	protected int currentSeq;
	protected DatagramSocket socket;
	protected DatagramChannel channel;
	protected InetAddress addr;
	protected long timeOut = 5000;
	protected double ERTT;	// timeout var
	protected double EDEV;	// timeout var
	protected int dataTransferred = 0;
	protected int packetsTransferred = 0;
	protected int outOfSequencePackets = 0;
	protected int incorrectChecksum = 0;
	protected int numRetransmissions = 0;
	protected int dupAcks = 0;

	protected Transport(int lp, int rp, String filename, int mtu, int sws) throws SocketException {
		this.lp = lp;
		this.rp = rp;
		this.filename = filename;
		this.mtu = mtu;
		this.sws = sws;
		this.buffer = new TCPpacket[sws];
		this.maxDataSize = mtu - 20 - 8 - 24; // includes our header, used to split file into chunks
		arraydp = new byte[mtu - 20 - 8]; // does not include our header!
		try {
			this.channel = DatagramChannel.open();
			this.socket = channel.socket();
			this.socket.bind(new InetSocketAddress(lp));
			if (this.socket.getSendBufferSize() < mtu * 10)
				this.channel.setOption(StandardSocketOptions.SO_SNDBUF, mtu * 10);
			if (this.socket.getReceiveBufferSize() < mtu * 10)
				this.channel.setOption(StandardSocketOptions.SO_RCVBUF, mtu * 10);
			// System.out.println("snd buffer size: " + this.socket.getSendBufferSize());
			// System.out.println("rcv buffer size: " + this.socket.getReceiveBufferSize());
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public boolean transfer() {
		// TODO Loop while we have bytes to send / rcv that have not been acked
		// each time loading the packet using the handlePacket method
		// that is implemented in the Sender and Receiver class
		if ((bufferdp = initConnection()) == null) {
			System.err.println("Failed to connect");
			return false;
		}

		try {
			TCPpacket last = TCPpacket.deserialize(bufferdp.getData());
			channel.connect(bufferdp.getSocketAddress());
			currentAck = last.getAckNum();
		} catch (SerialException e) {
			System.err.println("Bad initConnection() - no readable ack");
			return false;
		} catch (IOException e) {
			System.err.println("Failed to connect DatagramChannel() " + e.getMessage());
			return false;
		}

		// Set these just in case
		rp = bufferdp.getPort();
		addr = bufferdp.getAddress();

		TCPpacket fin = transferData();

		termConnection(fin);
		String msg = "Data Transferred: " + this.dataTransferred;
		msg += "\nPackets sent: " + this.packetsTransferred;
		msg += "\nOut of Sequence Packets: " + outOfSequencePackets;
		msg += "\nBad Checksum Packets: " + incorrectChecksum;
		msg += "\nNumber of Retransmissions: " + numRetransmissions;
		msg += "\nDuplicate Acknowledgements: " + dupAcks;
		System.out.println(msg);

		try {
			if (channel.isOpen())
				channel.close();
		} catch (IOException e) {
			System.err.println("Failed to close channel gracefully. Leaving it up to the OS");
		}

		return true;
	}

	public void printPacket(TCPpacket p, boolean isSending){
		String msg = ""; 
		if (isSending) msg += "snd ";
		else msg += "rcv ";
		msg += p.getTime();
		if(p.isSyn()) msg += " S";
		else msg += " -";
		if(p.isAck()) msg += " A";
		else msg += " -";
		if(p.isFin()) msg += " F";
		else msg += " -";
		if(p.getDataLen() > 0) msg += " D ";
		else msg += " - ";
		msg += p.getSeq() + " " + p.getDataLen() + " " + p.getAckNum();
		System.out.println(msg); 
		packetsTransferred += 1;
	}

	private void updateTimeOut(TCPpacket p) {
		long S = p.getSeq();
		long T = p.getTime();
		long C = System.nanoTime();
		if (S == 0){
			//System.out.println("Setting init");
			ERTT = (C - T) / 1000000;
			EDEV = 0;
			timeOut = (long) (2*ERTT);
		}
		else{
			double SRTT = (C - T) / 1000000;
			double SDEV = Math.abs(SRTT - ERTT);
			ERTT = a*ERTT + (1-a)*SRTT;
			EDEV = b*EDEV + (1-b)*SDEV;
			timeOut = (long) (ERTT + 4*EDEV);
		}
	}

	private int getTimeOut() {
		return (int) this.timeOut;
	}

	protected boolean sendData(DatagramPacket indp, TCPpacket p) {
		if (indp == null)
			throw new NullPointerException("buffer DatagramPacket is not initialized. Likely called sendData(TCPpacket) before or in initConnection()");
		indp.setData(p.serialize());
		try {

			socket.send(indp);
			printPacket(p, true);
			return true;
		} catch (PortUnreachableException e) {
			return false;
		}catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // FIXME may change to return boolean etc
			return false;
		}
	}

	/**
	 * NOTE Should only be called after initConnection returns
	 *
	 */
	protected boolean sendData(TCPpacket p) {
		if (bufferdp == null)
			throw new NullPointerException("Channel is not init");
		return sendData(bufferdp, p);
	}

	/**
	 * NOTE Should only be called after initConnection returns
	 *
	 */
	protected TCPpacket receiveData(TCPpacket out) {
		return receiveData(bufferdp, out);
	}

	public TCPpacket receiveData(DatagramPacket indp, TCPpacket out) {
		if (indp == null) throw new NullPointerException("buffer DatagramPacket is not initialized. Likely called receiveData(TCPpacket) before or in initConnection()");
		int reTransmissions = 0;
		try {
			int to = getTimeOut();
			socket.setSoTimeout(getTimeOut());
			// System.out.println("timeout: " + to);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// set bufferdp
		while(reTransmissions < 16) {
			try {
				indp.setData(arraydp);
				socket.receive(indp);
				TCPpacket p = TCPpacket.deserialize(indp.getData());
				if(p.getAckNum() == this.currentAck){
					dupAcks += 1;
				}
				updateTimeOut(p);
				printPacket(p, false);
				return p;
			} catch (SocketTimeoutException e) {
				// resend
				try {
					// System.out.println("Retransmitting");
					out.setCurrentTime();
					indp.setData(out.serialize());
					socket.send(indp); // should be set with data already
					reTransmissions += 1;
					numRetransmissions += 1;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				continue;
			}
			catch (ChecksumException e) {
				incorrectChecksum += 1;
				System.out.println("Discarding Packet because of bad checksum");
			}
			catch (Exception e) {
				System.out.println(indp.getLength());
				System.out.println(TCPpacket.toString(indp.getData()));
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println("Tried Retransmitting 16 times");
		throw new IllegalStateException("Lost Connection");
	}

	protected TCPpacket receiveDataTransfer(TCPpacket out) {
		return receiveDataTransfer(bufferdp, out);
	}

	public TCPpacket receiveDataTransfer(DatagramPacket indp, TCPpacket out) {
		if (indp == null) throw new NullPointerException("buffer DatagramPacket is not initialized. Likely called receiveData(TCPpacket) before or in initConnection()");
		int reTransmissions = 0;
		try {
			int to = getTimeOut();
			socket.setSoTimeout(getTimeOut());
			//System.out.println("timeout: " + to);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int duplicateAcks = 0;
		// set bufferdp
		while(reTransmissions < 16) {
			try {
				indp.setData(arraydp);
				socket.receive(indp);
				TCPpacket p = TCPpacket.deserialize(indp.getData());
				if(p.getAckNum() <= this.currentAck){ // discard packet
					if(p.getAckNum() == this.currentAck){
						dupAcks += 1;
						duplicateAcks += 1;
						if(duplicateAcks%3 == 0) // Fast Retransmit with 3 duplicate ACKs
							throw new SocketTimeoutException();
					}
					else
						outOfSequencePackets += 1;
					throw new IllegalArgumentException("Discarding packet due to bad ACK NUM: "+p.getAckNum());
				}
				updateTimeOut(p);
				printPacket(p, false);
				return p;
			} catch (SocketTimeoutException e) {
				// resend
				try {
					// System.out.println("Retransmitting");
					out.setCurrentTime();
					indp.setData(out.serialize());
					socket.send(indp); // should be set with data already
					reTransmissions += 1;
					numRetransmissions += 1;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				continue;
			}
			catch (IllegalArgumentException e) {
				// System.out.println(e.getMessage());
			}catch (ChecksumException e) {
				incorrectChecksum += 1;
				// System.out.println("Discarding Packet because of bad checksum");
			}
			catch (Exception e) {
				System.out.println(indp.getLength());
				System.out.println(TCPpacket.toString(indp.getData()));
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println("Tried Retransmitting 16 times");
		System.exit(1);
		return null;
	}

	/**
	 * The connection should be initialized on return
	 * if the init is successful, performing
	 * the respective half of the three-way handshake.
	 *
	 * @return the response Datagram with appropriate
	 * set size for reuse.
	 */
	protected abstract DatagramPacket initConnection();

	protected abstract TCPpacket transferData();

	/**
	 * After this returns the connection should
	 * be terminated. Perform termination
	 * procedure, then close sockets.
	 */
	protected abstract void termConnection(TCPpacket finPacket);

}
