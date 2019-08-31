package net.tiny.ws.auth;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.security.auth.Subject;

public class SecureKey {
	private static final String DEFAULT_ALGORITHM = "DES";
	private String algorithm = DEFAULT_ALGORITHM;
	private boolean ignoreCase = true;

	private final ReentrantLock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	private String publicKey;
	private String privateKey;
	private String exponent;
	private String modulus;
	private Subject subject;
	private Map<String, String> tickets;

	public SecureKey() {
		this(DEFAULT_ALGORITHM, 4);
	}

	protected SecureKey(String algorithm, int syncmax) {
		this.tickets = Collections.synchronizedMap(createCacheMap(syncmax));
		this.algorithm = algorithm;
	}

	public Subject getSubject() {
		return subject;
	}
	public void setSubject(Subject subject) {
		this.subject = subject;
	}
	public String getExponent() {
		return exponent;
	}
	public void setExponent(String exponent) {
		this.exponent = exponent;
	}
	public String getModulus() {
		return modulus;
	}
	public void setModulus(String modulus) {
		this.modulus = modulus;
	}

	public void cacheCaptcha(String captchaId, String captchaToken) throws IOException {
		this.tickets.put(captchaId, captchaToken);
	}

	public boolean verifyCaptcha(String captchaId, String captcha) {
		boolean ret = false;
		if(ignoreCase) {
			ret = captcha.equalsIgnoreCase(this.tickets.get(captchaId));
		} else {
			ret = captcha.equals(this.tickets.get(captchaId));
		}
		if(ret) {
			this.tickets.remove(captchaId);
		}
		return ret;
	}

	public String decrypt(String encrypted) {
		// lock
		this.lock.lock();
		try {
			return Crypt.decryptByPrivateKey(this.privateKey, encrypted);
		} finally {
			this.lock.unlock();
		}
	}

	public String encrypt(String data) {
		this.lock.lock();
		try {
			return Crypt.encryptByPrivateKey(this.privateKey, data);
		} finally {
			this.lock.unlock();
		}
	}

	public Map<String, String> createPublicKeys() {
		waitUnlock();
		reset();
		// BASE64编码后的公钥 模数和公钥 放入JSON数据
		Map<String, String> data = new HashMap<>();
		data.put(Crypt.MODULUS_NAME,  modulus);
		data.put(Crypt.EXPONENT_NAME, exponent);
		data.put(Crypt.PUBLIC_KEY_NAME, publicKey);
		return data;
	}

	private void reset() {
		// lock list
		this.lock.lock();
		try {
			final Crypt crypt =  Crypt.create(this.algorithm);
			Map<String, String> keys = Crypt.generate(crypt);
			this.modulus   =  keys.get(Crypt.MODULUS_NAME);
			this.exponent  = keys.get(Crypt.EXPONENT_NAME);
			this.publicKey  = keys.get(Crypt.PUBLIC_KEY_NAME);
			this.privateKey = Base64.getEncoder().encodeToString(crypt.decrypt(Base64.getDecoder().decode(keys.get(Crypt.PRIVATE_KEY_NAME))));
			// Notice other process
			this.condition.signal();
		} finally {
			this.lock.unlock();
		}
	}

	private void waitUnlock() {
		while (this.lock.isLocked()) {
			try {
				this.condition.await();
			} catch (InterruptedException ex) {
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(hashCode() + "#");
		return sb.toString();
	}

	static <K, V> Map<K,V> createCacheMap(final int max) {
		return new LinkedHashMap<K,V> (max*10/7, 0.7f, true) {
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
				return size() > max;
			}
		};
	}
}
