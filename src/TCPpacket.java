import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * By specification the packet is constructed as follows:
 *
 * Byte Sequence Number [4]
 * Acknowledgment [4]
 * Timestamp [8]
 * Length | S | F | A [4]
 * All zeroes | Checksum (split evenly) [4]
 * Total: 24 bytes
 * Data [specified by length]
 *
 * The width (each line above) is 8 bytes (64 bits)
 *
 * Receiver will use the sequence numbers
 */
public class TCPpacket {

	public static int HEADERN = 40;
	public static int FLAG_ACK = 0x1;
	public static int FLAG_FIN = 0x2;
	public static int FLAG_SYN = 0x4;

	private int sequenceNumber;
	private int ack;
	private long timestamp;
	private int lengthFlags; // need to bit shift left 3 times
	private int checksum; // first 4 bytes should be 0
	private byte[] data;

	/**
	 * Creates a new TCPPacket
	 * With no data
	 */
	public TCPpacket() {
		this(new byte[0]);
	}

	/**
	 * Reads in a TCPPacket from
	 * a byte[] src (from a sent
	 * TCPPacket).
	 */
	public TCPpacket(byte[] data) {
		checksum = 0;
		this.data = data;
	}

	/**
	 * The timestamp is set on serialization
	 * Format:
	 * Byte Sequence Number [4]
	 * Acknowledgment [4]
	 * Timestamp [8]
	 * Length | S | F | A [4]
	 * All zeroes | Checksum (split evenly) [4]
	 */
	public static TCPpacket deserialize(byte[] src) throws ChecksumException {
		TCPpacket p = new TCPpacket();
		ByteBuffer buf = ByteBuffer.wrap(src);
		p.sequenceNumber = buf.getInt();
		p.ack = buf.getInt();
		p.timestamp = buf.getLong();
		p.lengthFlags = buf.getInt();
		// read the checksum, then set to zero for checksum validation
		buf.mark();
		p.checksum = buf.getInt();
		buf.reset();
		buf.putInt(0);

		p.data = new byte[p.getDataLen()];
		buf.get(p.data, 0, p.getDataLen());

		// verify the checksum
		int cksm = calcChecksum(buf.duplicate());
		if (p.checksum != cksm) {
			throw new ChecksumException("Checksum was invalid: " + cksm + " != " + p.checksum);
		}
		return p;
	}

	public static void printPacket(byte[] data) {
		ByteBuffer buf = ByteBuffer.wrap(data);
		StringBuffer sb = new StringBuffer();
		sb.append("Seq: ").append(buf.getInt()).append('\n');
		sb.append("Ack: ").append(buf.getInt()).append('\n');
		sb.append("time: ").append(buf.getLong()).append('\n');
		long lengthFlags = buf.getInt();
		long length = lengthFlags >> 3;
		sb.append("length: ").append(length).append('\n');
		if ((lengthFlags & FLAG_SYN) > 0)
			sb.append("S");
		if ((lengthFlags & FLAG_ACK) > 0)
			sb.append("A");
		if ((lengthFlags & FLAG_FIN) > 0)
			sb.append("F");
		sb.append('\n');
		// read the checksum, then set to zero for checksum validation
		sb.append("chksum: ").append(buf.getInt());
		sb.append('\n');

		for (long i = 0; i < length; i++) {
			if (i > 20) {
				sb.append("\n");
				i = 0;
			}
			sb.append(' ').append(buf.get()).append(' ');
		}
		System.out.println(sb.toString());
	}

	/**
	 * The standard 1's complement checksum
	 * We treat the passed bytes as a sequence
	 * of 2 byte integers ('short' in java)
	 * and calc the checksum based on the sum
	 * of the data.
	 *
	 * The alg I use here is taken from the Systems
	 * Approach textbook, implemented in java using
	 * a ByteBuffer.
	 *
	 * I return an integer (4 bytes), but it is wrapped as if it were
	 * a 2 byte integer. Thus it returns 6 zero bytes followed by the 2 byte
	 * checksum.
	 *
	 * @see https://book.systemsapproach.org/direct/error.html
	 */
	private static int calcChecksum(ByteBuffer buf) {
		buf.rewind();
		if (buf.remaining() % 2 == 1) {
			throw new IllegalStateException("buffer needs to be padded cannot checksum odd length");
		}

		int sum = 0;
		while (buf.hasRemaining()) {
			sum += buf.getShort();
			if ((sum & 0xFFFF0000) > 0) { // if carry occured (for a short (2 bytes))
				// wrap carry
				sum &= 0xFFFF; 
				sum++;
			}
		}
		return ~(sum & 0xFFFF);
	}

	public void setCurrentTime() {
		this.timestamp = System.nanoTime();
	}

	public void setTime(long time) {
		this.timestamp = time;
	}

	public void setSeq(int num){
		this.sequenceNumber = num;
	}

	public int getSeq() {
		return this.sequenceNumber;
	}

	public void setAckNum(int num){
		this.ack = num;
	}

	public int getAckNum() {
		return this.ack;
	}

	public long getTime() {
		return this.timestamp;
	}

	public int getDataLen() {
		return this.lengthFlags >> 3;
	}

	public void setData(byte[] data) {
		this.data = Arrays.copyOf(data, data.length);
		long flags = this.lengthFlags & 0x7; // 0x7 = 0111
		this.lengthFlags = data.length;
		this.lengthFlags = this.lengthFlags << 3;
		// reset the flags
		this.lengthFlags += flags; 
	}

	public void setData(byte[] data, int offset, int length) {
		setData(Arrays.copyOfRange(data, offset, length));
	}

	public DatagramPacket getPacket(InetAddress addr, int rp) {
		final byte[] packet = serialize();
		if(addr == null)
			throw new NullPointerException("Cannot send to null address");
		System.out.println(addr.toString());
		return new DatagramPacket(packet, packet.length, addr, rp);
	}

	public byte[] getData() {
		return data;
	}

	/**
	 * Byte Sequence Number [4]
	 * Acknowledgment [4]
	 * Timestamp [8]
	 * Length | S | F | A [3.25,0.75]
	 * All zeroes | Checksum (split evenly) [4]
	 * Total: 24 bytes
	 * Data [specified by length]
	 *
	 */
	public byte[] serialize() {
		final int len = HEADERN + data.length + (data.length % 2);
		ByteBuffer buf = ByteBuffer.allocate(len);
		buf.putInt(sequenceNumber);
		buf.putInt(ack);
		buf.putLong(timestamp);
		buf.putInt(lengthFlags);
		buf.mark();     // position at start of checksum
		buf.putInt(0); // placeholder for checksum
		buf.put(data);
		// FIXME Do we need to adjust the length to account for the padding? Assuming no for now
		if (buf.remaining() % 2 == 1)
			buf.put((byte)0x00); // pad buffer with 1 zeroed byte
		checksum = calcChecksum(buf.duplicate());
		buf.reset();    // rewrite at checksum
		buf.putInt(checksum);
		return buf.array();
	}

	public void setFlag(int flag) {
		if (flag > FLAG_SYN)
			throw new IllegalArgumentException(String.format("Invalid flag: %x\n", flag));
		this.lengthFlags |= flag;
	}

	public void setSyn() {
		setFlag(FLAG_SYN);
	}

	public void setFin() {
		setFlag(FLAG_FIN);
	}

	public void setAck() {
		setFlag(FLAG_ACK);
	}

	public int getFlag() {
		if ((FLAG_ACK & lengthFlags) > 0)
			return FLAG_ACK;
		else if ((FLAG_FIN & lengthFlags) > 0)
			return FLAG_FIN;
		return FLAG_SYN;
	}

	public boolean isSyn() {
		return (FLAG_SYN & lengthFlags) > 0;
	}

	public boolean isFin() {
		return (FLAG_FIN & lengthFlags) > 0;
	}

	public boolean isAck() {
		return (FLAG_ACK & lengthFlags) > 0;
	}

	public void clearFlags() {
		// 0x8 = 1000
		this.lengthFlags &= 0x8;
	}

}
