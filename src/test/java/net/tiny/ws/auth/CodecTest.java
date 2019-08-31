package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;


public class CodecTest {

	private void showHex(byte[] value) {
		char[] DIGIT_ALL = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    	System.out.println("values : " + value.length + " " + DIGIT_ALL.length);
		for (byte b : value) {
			   System.out.format("0x%x ", b);
		}
		System.out.println();
	}

	@Test
	public void testWhitOdd() throws Exception {
		byte[] data = new byte[] { (byte)0xff, (byte)0xff, (byte)0xd8, (byte)0x7b, (byte)0x02, (byte)0x9b, (byte)0xc6, (byte)0x66, (byte)0x00};
		String asc = Codec.encodeString(data);
		System.out.println(asc);
		byte[] value = Codec.decodeString(asc);
		showHex(data);
		showHex(value);
		System.out.println();
		assertTrue(Arrays.equals(data, value));

		data = new byte[] {(byte)0x00, (byte)0xd8, (byte)0x7b, (byte)0x02, (byte)0x9b, (byte)0xc6, (byte)0x66, (byte)0x00};
		asc = Codec.encodeString(data);
		value = Codec.decodeString(asc);
		showHex(data);
		showHex(value);
		assertTrue(Arrays.equals(data, value));
	}

	@Test
	public void testGetDigits() throws Exception {
        System.out.println("##### getDigits({0,1,2...A,B,C,D...}) #####");
        for(int i=0 ; i<36; i++) {
	       	char[] r = Codec.getDigits(i);
	       	String s = new String(r);
		   	System.out.println(s);
	    }
        assertEquals('N', Codec.getDigits(13)[0]);
        assertEquals('B', Codec.getDigits(25)[0]);
        assertEquals((36-13), Arrays.binarySearch(Codec.getDigits(0), 'N'));
        assertEquals((36-25), Arrays.binarySearch(Codec.getDigits(0), 'B'));
	}

	@Test
	public void testEncodeHex() throws Exception {
		String data = "5070942774902496337890624";
		String enc = Codec.encodeHex(data.getBytes());
		assertEquals("35303730393432373734393032343936333337383930363234", enc);
		assertEquals(data, new String(Codec.decodeHex(enc)));
	}

	@Test
	public void testHash() throws Exception {
		String data = "5070942774902496337890624";
		String enc = Codec.md5(data);
		assertEquals("7uuMh2av7p0GsmNjnhr3yo", enc);
		enc = Codec.encodeHex(Codec.digest("password", "MD5"));
		assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", enc);
		enc = Codec.encodeBase64(Codec.digest("password", "MD5"));
		assertEquals("X03MO1qnZdYdgyfeuILPmQ==", enc);
		enc = Codec.md5("password");
		assertEquals("AgtrLb6WYLt5cRrfLvKyVN", enc);

		enc = Codec.sha1(data);
		assertEquals("gebPfqGQVo7TK3PhDkRl7AXIWnF", enc);
	}

	@Test
	public void testEncodeString() throws Exception {
		String data = "5070942774902496337890624";
		String enc = Codec.encodeString(data.getBytes());
		assertEquals("PB3QihXBqQSnU1NMhQM8KWRk23PMss31uO", enc);
		assertEquals(data, new String(Codec.decodeString(enc)));

		enc = Codec.encodeString(data.getBytes(), 2);
		assertEquals("N91OgfV9oOQlSzLKfOK6IUPi01NKqq1zsM", enc);
		assertEquals(data, new String(Codec.decodeString(enc, 2)));

		data = "149145375732426";
		enc = Codec.encodeString(data.getBytes(), Codec.getDigits(0));
		assertEquals("4O76TV5NHPM4Y3E50V5L6T6E", enc);
		assertEquals(data, new String(Codec.decodeString(enc,  Codec.getDigits(0))));

		enc = Codec.encodeString(data.getBytes(), Codec.getDigits(8));
		assertEquals("WGZYLNXF9HEWQV6XSNXDYLY6", enc);
		assertEquals(data, new String(Codec.decodeString(enc,  Codec.getDigits(8))));
	}

	@Test
	public void testDecodeString() throws Exception {
		int loop = 10;
		for(int i=0; i<loop; i++) {
			int len = Codec.getRandom(5, 50);
			Character[] array = new Character[len];
			for(int n=0; n<array.length; n++) {
				if(Codec.getRandomBoolean()) {
					array[n] = '9';
				} else {
					array[n] = 'X';
				}
			}
			Character[] fmt = Codec.getRandomArray(Character.class, array);
			char[] fc = new char[fmt.length];
			for(int n=0; n<fmt.length; n++) {
				fc[n] = fmt[n];
			}
			String format = new String(fc);
			String data = Codec.getRandom(format);
			int idx = Codec.getRandom(0, 10);
			String enc = Codec.encodeString(data.getBytes(), idx);
			assertEquals(data, new String(Codec.decodeString(enc, idx)));
		}
	}

	@Test
	public void testEnDecodeNumbers() throws Exception {
		int[] data = new int[] {-2,251,24,6578};
		String enc = Codec.encodeNumbers(data);
		System.out.println(enc);
		int[] value = Codec.decodeNumbers(enc);
		assertTrue(Arrays.equals(data, value));
	}

	@Test
	public void testGetRandom() throws Exception {
		int number = 100;
		for(int i=0; i<number; i++) {
			System.out.println(Codec.getRandom("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"));
		}
	}
}
