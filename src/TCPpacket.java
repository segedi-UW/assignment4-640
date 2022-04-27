import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

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

    private long sequenceNumber;
    private long ack;
    private long timestamp;
    private long lengthFlags; // need to bit shift left 3 times
    private long checksum; // make sure to set first 4 bytes to 0
    private byte[] data;

    /**
     * Creates a new TCPPacket
     * With no data
     */
    public TCPpacket() {
        data = new byte[0];
        checksum = 0;
    }

    /**
     * Reads in a TCPPacket from
     * a byte[] src (from a sent
     * TCPPacket).
     */
    public TCPpacket(byte[] src) {
        deserialize(src);
    }

    /*
    Sets the sequence number for the TCP header
    */
    public void setSeq(long num){
        this.sequenceNumber = num;
    }

    public long getSeq() {
        return this.sequenceNumber;
    }

    public void setTime(long stamp){
        this.timestamp = stamp;
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

    /**
     * Sets the data for the TCPPacket.
     */
    public void setData(byte[] data) {
        this.data = data;
        long flags = this.lengthFlags & 0x7; // 0x7 = 0111
        this.lengthFlags = data.length;
        if ((this.lengthFlags & 0xE) > 0) // 0xE = 1110, want to make sure we can shift
            throw new IllegalArgumentException("Data provided is too long for checksum width!");
        this.lengthFlags = this.lengthFlags << 3;
        // reset the flags
        this.lengthFlags += flags; 
    }

    public DatagramPacket getPacket(InetAddress addr, int rp) {
        final byte[] packet = serialize();
        return new DatagramPacket(packet, packet.length, addr, rp);
    }

    /**
     * Byte Sequence Number [8]
     * Acknowledgment [8]
     * Timestamp [8]
     * Length | (0 | S | F | A) [7, 1]
     * All zeroes | Checksum (split evenly) [8]
     */
    private byte[] serialize() {
        final int len = HEADERN + data.length;
        ByteBuffer buf = ByteBuffer.allocate(len);
        buf.putLong(sequenceNumber);
        buf.putLong(ack);
        timestamp = System.nanoTime();
        buf.putLong(timestamp);
        buf.putLong(lengthFlags);
        buf.mark();     // position at start of checksum
        buf.putLong(0); // placeholder for checksum
        buf.put(data);
        if (buf.remaining() % 2 == 1)
            buf.put((byte)0x00); // pad buffer with 1 zeroed byte
        checksum = calcChecksum(buf.duplicate());
        buf.reset();    // rewrite at checksum
        buf.putLong(checksum);
        return buf.array();
    }

    private void deserialize(byte[] src) {
        System.out.println("Reached");
        ByteBuffer buf = ByteBuffer.wrap(src);
        this.sequenceNumber = buf.getLong();
        this.ack = buf.getLong();
        this.timestamp = buf.getLong();
        System.out.println(timestamp);
        this.lengthFlags = buf.getLong();
        this.checksum = buf.getLong();
        if (buf.remaining() > 0) {
            this.data = new byte[buf.remaining()];
            buf.get(this.data, buf.position(), buf.remaining());
        } else this.data = new byte[0];
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
    private long calcChecksum(ByteBuffer buf) {
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

    public boolean isSyn() {
        return (0x4 & this.lengthFlags) > 0; // 0100
    }

    public boolean isFin() {
        return (0x2 & this.lengthFlags) > 0; // 0010
    }

    public boolean isAck() {
        return (0x1 & this.lengthFlags) > 0; // 0001
    }

    public void setSyn(boolean isSyn) {
        this.lengthFlags = isSyn ? this.lengthFlags | 0x4 : this.lengthFlags & 0x4;
    }

    public void setFin(boolean isFin) {
        this.lengthFlags = isFin ? this.lengthFlags | 0x2 : this.lengthFlags & 0x2;
    }

    public void setAck(boolean isAck) {
        this.lengthFlags = isAck ? this.lengthFlags | 0x1 : this.lengthFlags & 0x1;
    }

    public void clearFlags() {
        // 0x8 = 1000
        this.lengthFlags &= 0x8;
    }

}
