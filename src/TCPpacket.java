import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * By specification the packet is constructed as follows:
 *
 * Byte Sequence Number [8]
 * Acknowledgment [8]
 * Timestamp [8]
 * Length | (0 | S | F | A) [7, 1]
 * All zeroes | Checksum (split evenly) [8]
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

	private long sequenceNumber;
	private long ack;
	private long timestamp;
	private long lengthFlags; // need to bit shift left 3 times
	private long checksum; // first 4 bytes should be 0
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
	 * Byte Sequence Number [8]
	 * Acknowledgment [8]
	 * Timestamp [8]
	 * Length | S | F | A) [7.25,0.75]
	 * All zeroes | Checksum (split evenly) [8]
	 */
	public static TCPpacket deserialize(byte[] src) throws ChecksumException {
		TCPpacket p = new TCPpacket();
		ByteBuffer buf = ByteBuffer.wrap(src);
		p.sequenceNumber = buf.getLong();
		p.ack = buf.getLong();
		p.timestamp = buf.getLong();
		p.lengthFlags = buf.getLong();
		// read the checksum, then set to zero for checksum validation
		buf.mark();
		p.checksum = buf.getLong();
		buf.reset();
		buf.putLong(0);

		String tmp = String.format("Remaining (%d)", buf.remaining());

		if (buf.remaining() > 0) {
			p.data = new byte[buf.remaining()];
			buf.get(p.data, 0, buf.remaining());
		} else p.data = new byte[0];

		tmp += String.format(" length (%d)", p.data.length);
		System.out.println(tmp);
		// verify the checksum
		long cksm = calcChecksum(buf.duplicate());
		if (p.checksum != cksm) {
			throw new ChecksumException("Checksum was invalid: " + cksm + " != " + p.checksum);
		}
		return p;
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
	 * I return a long integer (8 bytes), but it is wrapped as if it were
	 * a 2 byte integer. Thus it returns 6 zero bytes followed by the 2 byte
	 * checksum.
	 *
	 * @see https://book.systemsapproach.org/direct/error.html
	 */
	private static long calcChecksum(ByteBuffer buf) {
		buf.rewind();
		if (buf.remaining() % 2 == 1) {
			throw new IllegalStateException("buffer needs to be padded cannot checksum odd length");
		}

		long sum = 0;
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

	public void setSeq(long num){
		this.sequenceNumber = num;
	}

	public long getSeq() {
		return this.sequenceNumber;
	}

	public void setAckNum(long num){
		this.ack = num;
	}

	public long getAckNum() {
		return this.ack;
	}

	public long getTime() {
		return this.timestamp;
	}

	public long getDataLen() {
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
        if(addr == null){
            return new DatagramPacket(packet, packet.length, rp);
        }
		return new DatagramPacket(packet, packet.length, addr, rp);
	}

	public byte[] getData() {
		return data;
	}

	/**
	 * The timestamp is set on serialization
	 * Format:
	 * Byte Sequence Number [8]
	 * Acknowledgment [8]
	 * Timestamp [8]
	 * Length | S | F | A) [7.25,0.75]
	 * All zeroes | Checksum (split evenly) [8]
	 */
	private byte[] serialize() {
		final int len = HEADERN + data.length + (data.length % 2);
		ByteBuffer buf = ByteBuffer.allocate(len);
		buf.putLong(sequenceNumber);
		buf.putLong(ack);
		timestamp = System.nanoTime();
		buf.putLong(timestamp);
		buf.putLong(lengthFlags);
		buf.mark();     // position at start of checksum
		buf.putLong(0); // placeholder for checksum
		buf.put(data);
		// FIXME Do we need to adjust the length to account for the padding? Assuming no for now
		if (buf.remaining() % 2 == 1)
			buf.put((byte)0x00); // pad buffer with 1 zeroed byte
		checksum = calcChecksum(buf.duplicate());
		buf.reset();    // rewrite at checksum
		buf.putLong(checksum);
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
