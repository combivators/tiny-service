package net.tiny.ws.auth;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Random;


public class Codec {
	/**
	 * All possible chars for representing a number as a String
	 */
	private static final char[] DIGIT_ALL = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final char[] DIGITS    = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	private static final String ASCII_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	private static final String ENCODING = "ISO-8859-1";

	/**
	 * @param text , text in plain format
	 * @param algorithm   MD5 OR SHA1
	 * @return hash in algorithm
	 */
	private static String getHash(String text, String algorithm) {
		return encodeString(digest(text, algorithm));
	}

	public static byte[] digest(String text, String algorithm) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			return md.digest(text.getBytes());
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	public static String md5(String txt) {
		return getHash(txt, "MD5");
	}

	public static String sha1(String txt) {
		return getHash(txt, "SHA1");
	}

	public static final String encodeHex(byte bytes[]) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bytes.length; ++i) {
			sb.append(Integer.toHexString((bytes[i] & 0xFF) | 0x100).substring(1, 3));
		}
		return sb.toString();
	}

	public static final byte[] decodeHex(String hex) {
		char chars[] = hex.toCharArray();
		byte bytes[] = new byte[chars.length / 2];
		int byteCount = 0;
		for (int i = 0; i < chars.length; i += 2) {
			int newByte = 0;
			newByte |= hexCharToByte(chars[i]);
			newByte <<= 4;
			newByte |= hexCharToByte(chars[i + 1]);
			bytes[byteCount] = (byte) newByte;
			byteCount++;
		}
		return bytes;
	}

	public static String encodeBase64(String data) {
		byte bytes[] = null;
		try {
			bytes = data.getBytes(ENCODING);
			return encodeBase64(bytes);
		} catch (UnsupportedEncodingException uee) {
			return null;
		}
	}

	public static String encodeBase64(byte data[]) {
		int len = data.length;
		StringBuffer ret = new StringBuffer((len / 3 + 1) * 4);
		for (int i = 0; i < len; i++) {
			int c = data[i] >> 2 & 0x3f;
			ret.append(ASCII_LETTERS.charAt(c));
			c = data[i] << 4 & 0x3f;
			if (++i < len)
				c |= data[i] >> 4 & 0xf;
			ret.append(ASCII_LETTERS.charAt(c));
			if (i < len) {
				c = data[i] << 2 & 0x3f;
				if (++i < len)
					c |= data[i] >> 6 & 3;
				ret.append(ASCII_LETTERS.charAt(c));
			} else {
				i++;
				ret.append('=');
			}
			if (i < len) {
				c = data[i] & 0x3f;
				ret.append(ASCII_LETTERS.charAt(c));
			} else {
				ret.append('=');
			}
		}

		return ret.toString();
	}

	public static String decodeBase64(String data) {
		byte bytes[] = null;
		try {
			bytes = data.getBytes(ENCODING);
			return decodeBase64(bytes);
		} catch (UnsupportedEncodingException uee) {
			return null;
		}
	}

	public static String decodeBase64(byte data[]) {
		int len = data.length;
		StringBuffer ret = new StringBuffer((len * 3) / 4);
		for (int i = 0; i < len; i++) {
			int c = ASCII_LETTERS.indexOf(data[i]);
			i++;
			int c1 = ASCII_LETTERS.indexOf(data[i]);
			c = c << 2 | c1 >> 4 & 3;
			ret.append((char) c);
			if (++i < len) {
				c = data[i];
				if (61 == c)
					break;
				c = ASCII_LETTERS.indexOf(c);
				c1 = c1 << 4 & 0xf0 | c >> 2 & 0xf;
				ret.append((char) c1);
			}
			if (++i >= len)
				continue;
			c1 = data[i];
			if (61 == c1)
				break;
			c1 = ASCII_LETTERS.indexOf(c1);
			c = c << 6 & 0xc0 | c1;
			ret.append((char) c);
		}

		return ret.toString();
	}

	public static String encodeString(byte[] data) {
		return encodeString(data, 0);
	}

	public static byte[] decodeString(String data) {
		return decodeString(data, 0);
	}

	public static String encodeString(byte[] data, int index) {
		return toBigIntegerString(new BigInteger(attachOddEven(data)), getDigits(DIGIT_ALL, index));
	}

	public static byte[] decodeString(String data, int index) {
		return verifyOddEven(toBigInteger(data, getDigits(DIGIT_ALL, index)).toByteArray());
	}

	public static String encodeString(byte[] data, char[] digits) {
		return toBigIntegerString(new BigInteger(attachOddEven(data)), digits);
	}

	public static byte[] decodeString(String data, char[] digits) {
		return verifyOddEven(toBigInteger(data, digits).toByteArray());
	}

	public static String encodeNumbers(int[] data) {
		int[] value = new int[data.length + 1];
		value[0] = data.length;
		System.arraycopy(data, 0, value, 1, data.length);
		ByteBuffer byteBuffer = ByteBuffer.allocate(value.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(value);
        byte[] bytes = byteBuffer.array();
		return encodeString(bytes, 0);
	}

	public static int[] decodeNumbers(String data) {
		byte[] bytes = decodeString(data, 0);
		byte[] buf = new byte[4];
		System.arraycopy(bytes, 0, buf, 0, buf.length);
		int size = ByteBuffer.wrap(buf).getInt();
		int[] values = new int[size];
		for(int i=0; i<values.length; i++) {
			System.arraycopy(bytes, (i+1)*4, buf, 0, buf.length);
			values[i] = ByteBuffer.wrap(buf).getInt();
		}
		return values;
	}

	private static byte[] attachOddEven(byte[] value) {
		// Add 1 byte odd even data on header
		byte[] odd = new byte[] {(byte)(value.length%2+1)};
		byte[] data = new byte[value.length + 1];
		System.arraycopy(odd, 0, data, 0, odd.length);
		System.arraycopy(value, 0, data, odd.length, value.length);
		return data;
	}

	private static byte[] verifyOddEven(byte[] data) {
		//Cut 1 byte odd even data.
		int odd = (int)(((data.length - 1)%2) + 1);
		if((int)data[0] != odd) {
			throw new IllegalArgumentException("Odd-even error.");
		}
		byte[] value = new byte[data.length-1];
		System.arraycopy(data, 1, value, 0, value.length);
		return value;
	}

	private  static String toBigIntegerString(BigInteger big, char[] digits) {
		int radix = digits.length;
		StringBuffer sb = new StringBuffer();
		if (big.signum() == -1) {
			sb.append('-');
		}
		BigInteger i = new BigInteger(big.abs().toByteArray());
		BigInteger r = new BigInteger(Integer.toString(radix));
		do {
			BigInteger dr[] = i.divideAndRemainder(r);
			sb.insert(0, digits[dr[1].intValue()]);
			i = dr[0];
		} while (!i.equals(BigInteger.ZERO));
		return sb.toString();
	}

	private static BigInteger toBigInteger(String value, char[] digits)
	{
		int radix = digits.length;
		int signum = 1;
		BigInteger big = new BigInteger(BigInteger.ZERO.toByteArray());
		BigInteger r = new BigInteger(Integer.toString(radix));
		char buf[] = value.toCharArray();
		if (buf[buf.length - 1] == '-') {
			signum = -1;
			char temp[] = new char[buf.length - 1];
			System.arraycopy(buf, 0, temp, 0, buf.length - 1);
			buf = temp;
		}
		for (int i = buf.length - 1; i >= 0; i--) {
			int num = binarySearch(digits, buf[i]);
			BigInteger v = new BigInteger(Integer.toString(num));
			big = big.add(v.multiply(r.pow(buf.length - 1 - i)));
		}
		if (signum == -1)
			return big.negate();
		else
			return big;
	}

	static int binarySearch(char[] array, char key) {
		int i= 0;
		for(char c : array) {
			if(key == c) {
				return i;
			} else {
				i++;
			}
		}
		return -1;
	}

	private static int[] getIntegerArray(int num, int index)
    {
    	if(num < index) {
    		throw new IllegalArgumentException(index + " > " + num );
    	}
		int[] array = new int[num];
		for(int i=0; i<index; i++) {
			array[i] = num - index + i;
		}
		for(int i=index; i<num; i++) {
			array[i] = i-index;
		}
        return array;
    }

    static char[] getDigits(char[] objs, int index)
    {
		int num = objs.length;
		int[] r = getIntegerArray(num, index);
		char[] ret   = new char[num];
		for(int i=0; i<num; i++) {
			ret[i] = objs[ r[i] ];
		}
        return ret;
	}

    static char[] getDigits(int index) {
    	return getDigits(DIGITS, index);
	}

	private static final byte hexCharToByte(char ch) {
		switch (ch) {
		case 48: // '0'
			return 0;

		case 49: // '1'
			return 1;

		case 50: // '2'
			return 2;

		case 51: // '3'
			return 3;

		case 52: // '4'
			return 4;

		case 53: // '5'
			return 5;

		case 54: // '6'
			return 6;

		case 55: // '7'
			return 7;

		case 56: // '8'
			return 8;

		case 57: // '9'
			return 9;

		case 97: // 'a'
			return 10;

		case 98: // 'b'
			return 11;

		case 99: // 'c'
			return 12;

		case 100: // 'd'
			return 13;

		case 101: // 'e'
			return 14;

		case 102: // 'f'
			return 15;

		case 58: // ':'
		case 59: // ';'
		case 60: // '<'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
		case 66: // 'B'
		case 67: // 'C'
		case 68: // 'D'
		case 69: // 'E'
		case 70: // 'F'
		case 71: // 'G'
		case 72: // 'H'
		case 73: // 'I'
		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
		case 77: // 'M'
		case 78: // 'N'
		case 79: // 'O'
		case 80: // 'P'
		case 81: // 'Q'
		case 82: // 'R'
		case 83: // 'S'
		case 84: // 'T'
		case 85: // 'U'
		case 86: // 'V'
		case 87: // 'W'
		case 88: // 'X'
		case 89: // 'Y'
		case 90: // 'Z'
		case 91: // '['
		case 92: // '\\'
		case 93: // ']'
		case 94: // '^'
		case 95: // '_'
		case 96: // '`'
		default:
			return 0;
		}
	}

    ///////////////////////////////////////////////////
    static protected Random random;
    static
    {
		Calendar now = Calendar.getInstance();
		long seed = now.getTime().getTime();
        random = new Random( seed );
    }

    public static int getRandom(int min, int max) {
		int tmin, tmax;
		if(max<min) {
			tmin = max;
			tmax = min;
		} else {
			tmin = min;
			tmax = max;
		}
		if(max==min)
			return max;
        int ret = random.nextInt(tmax-tmin+1);
        return tmin + ret;
    }

    public static int[] getRandomArray(int n) {
		int[] array = new int[n];
		for(int i=0; i<n; i++)
		{
			array[i] =i;
		}
		for(int i=0; i<n; i++)
		{
			int r = getRandom(i, (n-1));
			int tmp = array[i];
			array[i] = array[r];
			array[r] = tmp;
		}
        return array;
    }

    public static int[] getRandomArrayArray(int n, int num) {
		int[] array = getRandomArray(n);
		int[] ret   = new int[num];
		for(int i=0; i<num; i++)
		{
			if(i < n)
			{
				ret[i] = array[i];
			} else
			{
				int r = getRandom(0, n-1);
				ret[i] = array[r];
			}
		}
        return ret;
    }

    public static <T> T[] getRandomArray(Class<T> type, T[] objs) {
		int n = objs.length;
		if(n <= 1)
			return objs;
		int[] r = getRandomArray(n);
		for(int i=0; i<n; i++) {
			T obj = objs[i];
			objs[i] = objs[ r[i] ];
			objs[ r[i] ] = obj;
		}
        return objs;
	}
/*
    public static <T> T[] getRandomArray(Class<T> type, T[] objs, int num) {
		int n = objs.length;
		if(n <= 1)
			return objs;
		//int[] r = getIntegerArray(n, num);
		T[] array = getRandomArray(type, objs);
		T[] ret   = new T[num];
		for(int i=0; i<num; i++) {
			ret[i] = array[ r[i] ];
		}
        return objs;
	}
*/
    /**
     *
     * @param format 9: Z: z: X: *:
     * @return
     */
    public static String getRandom(String format)
    {
        StringBuffer sb = new StringBuffer();
        int length = format.length();
        for(int i=0 ; i<length; i++ )
        {
			char f = format.charAt(i);
	        char ch;
	        if(f=='9') {
           		ch = getDigitChar();
			} else if(f=='X') {
           		ch = getASCIIChar();
			} else if(f=='Z') {
           		ch = Character.toUpperCase( getASCIIChar() );
			} else if(f=='z') {
           		ch = Character.toLowerCase( getASCIIChar() );
			} else if(f=='*') {
           		ch = getCodeChar();
			} else {
           		ch = f;
           	}
            sb.append(ch);
        }
        return sb.toString();
    }

    public static boolean getRandomBoolean()
    {
		boolean[] b = new boolean[] {true, false};
		int r = random.nextInt(2);
        return b[r];
    }

    static char getASCIIChar()
    {
		char[] chars = new char[] {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','1','2','3','4','5','6','7','8','9','0'};
		int r = random.nextInt(36);
		char ch;
		boolean b = getRandomBoolean();
		if(b) {
			ch = Character.toUpperCase(chars[r]);
		} else {
			ch = chars[r];
		}
        return ch;
    }

    static char getDigitChar()
    {
		int r = random.nextInt(10);
		char ch = (new Integer(r)).toString().charAt(0);
        return ch;
    }

    static char getCodeChar()
    {
		char[] chars = new char[] {'!','\"','#','$','%','&','\'','(',')','*','+',',','-','.','/',':',';','<','=','>','?','[','\\',']','^','_','{','|','}','~',' '};
		int r = random.nextInt(31);
		char ch = chars[r];
        return ch;
    }
}