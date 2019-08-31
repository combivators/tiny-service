package net.tiny.ws.auth;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;


public class UserToken {

	protected static final Logger LOGGER =  Logger.getLogger(UserToken.class.getName());

	public final static String TOKEN = "t";
	public final static long TIME_OUT = 24L * 60L * 60L * 1000L; // 24H
	private static final String DEFAULT_ALGORITHM = "AES";
	private final static SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

	private String username;
    private int credential;
	private String address;
	private int issuer = 0;
	private HashSet<String> roles = new HashSet<>();
	private boolean hashRole = false;
	private long expired; //yyyyMMddHHmmss
    private String token;


	private UserToken(String name, int credential, long expired) {
		this.username  = name;
		this.credential = credential;
		this.expired = expired;
	}

	public UserToken(String name, int credential, long currentTime, long timeout) {
		this(name, credential, expiredTime(currentTime + timeout));
	}

	public UserToken(String name, int credential) {
		this(name, credential, System.currentTimeMillis(), TIME_OUT);
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public int getCredential() {
		return credential;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address.toLowerCase();
	}
	public int getIssuer() {
		return issuer;
	}
	public void setIssuer(int issuer) {
		this.issuer = issuer;
	}
	public long getExpired() {
		return expired;
	}

	public void expired() {
		expired = 19700101090000L;
	}

	public boolean isExpired() {
		return expired < expiredTime(System.currentTimeMillis());
	}

	public String keepAlive(long time) {
		if(!isExpired()) {
			expired = expiredTime(time);
		}
		return getToken();
	}

	public String keepAlive() {
		return keepAlive(System.currentTimeMillis() + TIME_OUT);
	}

	public Set<String> getRoles() {
		return roles;
	}
	public void setRoles(Set<String> roleSet) {
		setRoles(roleSet, false);
	}

	protected void setRoles(Set<String> roleSet, boolean hash) {
		this.hashRole = hash;
		this.roles.clear();
		this.roles.addAll(roleSet);
	}

	Collection<Integer> getRoleIds(Set<String> roleSet, boolean hash) {
		Collection<Integer> ids = new TreeSet<Integer>();
    	for(String role : roleSet) {
    		if(hash) {
    			ids.add(Integer.valueOf(role));
    		} else {
    			ids.add(role.hashCode());
    		}
    	}
		return ids;
	}
	public boolean inRole(Set<String> allowedRoles) {
		if(this.roles.isEmpty()) {
			return true;
		}
		Collection<Integer> allowedIds = getRoleIds(allowedRoles, false);
		Collection<Integer> ids = getRoleIds(this.roles, this.hashRole);
    	return ids.containsAll(allowedIds);
	}
	public boolean verify(int odd) {
		return (hashCode() == odd);
	}

	public String getToken() {
		return getToken(true);
	}

	public String getToken(boolean update) {
		if (update) {
			int[] values = new int[this.roles.size()+6];
			values[0] = (int)(expired / 1000000L);
			values[1] = (int)(expired % 1000000L);
			values[2] = credential;
			values[3] = issuer;
			values[4] = address2Integer(address);
			values[5] = hashCode();
			int i = 6;
			Collection<Integer> ids = getRoleIds(roles, hashRole);
	    	for(Integer rid : ids) {
	    		values[i++] = rid;
	    	}
			String encodedValues = Codec.encodeNumbers(values);
			String singned = String.format("%1$s/%2$s", username, encodedValues);
			token = Crypt.create(DEFAULT_ALGORITHM).encrypt(singned); //TODO
		}
		return token;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !(obj instanceof UserToken) ) {
			return false;
		}
		UserToken ut = (UserToken)obj;
		return (this.username.equals(ut.username)
				&& (this.credential == ut.credential)
				&& this.address.equals(ut.address));
	}

	@Override
	public int hashCode() {
		int value = 17;
		value = value * 31 + this.username.hashCode();
		value = value * 31 + this.credential;
		value = value * 31 + this.address.hashCode();
		value = value * 31 + (int)((this.expired/1000L) % 17L);
		return value;
	}

	@Override
	public String toString() {
		return String.format("%s:%d:%s:%d:%d:roles(%d)", username, credential, address, issuer, expired, roles.size());
	}


	/**
	 * Add the token to the url and make sure it is properly encoded.
	 *
	 * @param url
	 *            - a string representing the url
	 * @return properly encoded url string.
	 */
	public String toHttp(String url) {
		String u = url == null ? "" : url;
		if (u.indexOf("?") == -1) {
			u += "?";
		} else if (!u.endsWith("?") && !u.endsWith("&")) {
			u += "&";
		}
		LOGGER.fine("Token before encoding:" + token + " length=" + token.length());
		String utok;
		try {
			utok = URLEncoder.encode(token, "UTF-8");
			LOGGER.fine("Token after encoding:" + utok + " length=" + utok.length());
			u += TOKEN + "=" + utok;
		} catch (UnsupportedEncodingException e) {
		}
		return u;
	}

	/**
	 * Use this method instead of getToken() if you need to pass the token via
	 * HTTP.
	 *
	 * @return a URL encoded token string.
	 */
	public String getUrlEncodedToken() {
		String token = this.token;
		try {
			// encode the string to make sure no funny characters are passed in
			// request
			token = URLEncoder.encode(this.token, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		return token;
	}

	static long expiredTime(long time) {
		synchronized (TIMESTAMP_FORMAT) {
			return Long.parseLong(TIMESTAMP_FORMAT.format(new Date(time)));
		}
	}

	static int address2Integer(String address) {
		try {
			return ByteBuffer.wrap(InetAddress.getByName(address).getAddress()).getInt();
		} catch (UnknownHostException ex) {
			throw new IllegalArgumentException(String.format("Unknown Host '%1$s'", address));
		}
	}

	static String integer2Address(int ip) {
		try {
			return InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(ip).array()).getHostAddress();
		} catch (UnknownHostException ex) {
			throw new IllegalArgumentException(String.format("Unknown address '%1$s'", Integer.toHexString(ip)));
		}
	}


	/**
	 *
	 * @param username
	 * @param password
	 * @param address
	 * @return
	 */
	public static UserToken create(String username, String password, String address) {
		if (username == null || password == null) {
			return null;
		}
		UserToken ut = new UserToken(username, password.hashCode());
		try {
			ut.setAddress(address);
			return ut;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 *
	 * @param token
	 * @return
	 */
	public static UserToken valueOf(String token) {
		if (token == null) {
			throw new IllegalArgumentException("Cannot find token");
		}
		//TODO
		String decrypted = Crypt.create(DEFAULT_ALGORITHM).decrypt(token);
		if (decrypted == null) {
			// Cannot decrypt token.
			throw new IllegalArgumentException(String.format("Cannot decrypt token '%1$s'", token));
		}
		String[] tokens = decrypted.split("/");

		//String[] tokens = token.split("/");
		if(tokens.length != 2) {
			throw new IllegalArgumentException(String.format("Unknow token '%1$s'", token));
		}
		int[] values = Codec.decodeNumbers(tokens[1]);
		if(values.length < 6) {
			throw new IllegalArgumentException(String.format("Unknow token value '%1$s' of '%2$s'", tokens[1], token));
		}

		long date = (long)(values[0]) * 1000000L +  (long)values[1];

		UserToken ut = new UserToken(tokens[0], values[2], date);
		ut.setIssuer(values[3]);

		ut.setAddress(integer2Address(values[4]));
		if (ut.isExpired()) {
			throw new IllegalArgumentException(String.format("Token '%s' has expired(%d).", token, date));
		}
		//hash code
		int oddeven = values[5];
		// Validate token odd
		if (ut.hashCode() != oddeven) {
			throw new IllegalArgumentException(String.format("Token '%1$s' odd-even fail. %2$d != %3$d", token, ut.hashCode(), oddeven));
		}
		// Set roles hash code
		Set<String> roleHashCodes = new HashSet<>();
		for(int i=6; i<values.length; i++) {
			roleHashCodes.add(Integer.toString(values[i]));
		}
		ut.setRoles(roleHashCodes, true);
		ut.token = token;
		return ut;
	}

	/**
	 *
	 * @param token
	 * @param clientAddress
	 * @return
	 */
	public static UserToken create(String token, String clientAddress) {
		try {
			UserToken ut = UserToken.valueOf(token);
			// validate token IP
			if (!ut.getAddress().equalsIgnoreCase(clientAddress)) {
				LOGGER.warning(String.format("Token '%1$s' orginated from '%2$s' different '%3$s' than request.", token, ut.getAddress(), clientAddress));
				return null;
			}
			return ut;
		} catch (Exception ex) {
			LOGGER.warning(String.format("Illegal token '%1$s' from '%2$s' request. Cause:%3$s", token, clientAddress, ex.getMessage()));
			return null;
		}
	}

}
