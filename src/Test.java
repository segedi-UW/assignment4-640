public class Test {

	private static boolean testSerialize() {
		TCPpacket p = new TCPpacket();
		byte[] pdata = {32, 54, 62, 3, 5, 2, 5, 23, 52, 4, 5, 2, 5, 5};
		p.setSyn();
		p.setCurrentTime();
		p.setSeq(53);
		p.setAckNum(30);
		p.setData(pdata);
		byte[] ps = p.serialize();
		TCPpacket d;
		try {
			d = TCPpacket.deserialize(ps);
		} catch (SerialException e) {
			System.err.println(e.getMessage());
			return false;
		}

		byte[] ddata = d.getData();

		if (ddata.length != p.getDataLen())
			return false;

		for (int i = 0; i < ddata.length; i++) {
			if (ddata[i] != pdata[i])
				return false;
		}

		return d.isSyn() && p.getSeq() == d.getSeq() &&
			p.getTime() == d.getTime() &&
			p.getAckNum() == d.getAckNum() &&
			p.getDataLen() == d.getDataLen();
	}

	private static void handleError(String msg, boolean isExit) {
		System.err.println(msg);
		if (isExit) System.exit(1);
	}

	public static void main(String[] args) {
		boolean isExit = true;
		if (!testSerialize()) handleError("serialize()", isExit);
		System.out.println("Passed All Tests");
		return;
	}

}
