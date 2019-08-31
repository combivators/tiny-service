package net.tiny.ws.auth;

import java.io.File;
import java.security.Security;
import java.util.HashMap;

import javax.security.auth.callback.CallbackHandler;


public class CallbackContext {

	public static final String JAAS_CONFIG = "java.security.auth.login.config";
	public static final String JAAS_CRYPT  = "java.security.auth.login.algorithm";

	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_ADDRESS  = "address";
	private static final String KEY_TOKEN    = "token";
	private static final String KEY_REMEMBER = "remember";

	protected HashMap<Object, Object> share = new HashMap<>();

	private CallbackHandler callbackHandler;
	private Options options;

	public static void setConfig(String jaas) {
    	String config = System.getProperty(JAAS_CONFIG);
    	if(null == config || !config.equals(jaas)) {
    		File configFile = new File(jaas);
    		System.setProperty(JAAS_CONFIG, configFile.getAbsolutePath());
    	}
	}

    public CallbackContext join(CallbackContext context) {
    	this.share.putAll(context.share);
    	return this;
    }

    public void clear() {
    	this.share.clear();
    }

	public Object[] getSharedKeys() {
		return this.share.keySet().toArray(new Object[this.share.size()]);
	}

	public Object getValue(Object key) {
		return this.share.get(key);
	}
	public CallbackContext setValue(Object key, Object value) {
		this.share.put(key, value);
		return this;
	}
	public String getStringValue(String name) {
		Object value = this.share.get(name);
		if(value != null) {
			return value.toString();
		} else {
			return null;
		}
	}
	public boolean getBooleanValue(String name) {
		Object value = this.share.get(name);
		if(value != null) {
			return Boolean.parseBoolean(value.toString().toLowerCase());
		} else {
			return false;
		}
	}

	public <T> CallbackContext setTarget(Class<T> type, T value) {
		this.share.put(type, value);
		return this;
	}

	public <T> boolean hasTarget(Class<T> type) {
		return this.share.containsKey(type);
	}

	@SuppressWarnings("unchecked")
	public <T> T getTarget(Class<T> type) {
		Object value = this.share.get(type);
		if(value != null && type.isInstance(value) ) {
			return (T)value;
		} else {
			return null;
		}
	}

	public CallbackHandler getCallbackHandler() {
		if(this.callbackHandler == null) {
			String handler = Security.getProperty("auth.login.defaultCallbackHandler");
			if(null != handler) {
				try {
					this.callbackHandler = CallbackHandler.class.cast(Class.forName(handler).newInstance());
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		}
		if(this.callbackHandler != null && this.callbackHandler instanceof CallbackContext) {
			((CallbackContext)this.callbackHandler).join(this);
		}
		return this.callbackHandler; // This is ClientCallbackHandler instance.
	}

	public void setCallbackHandler(CallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
		if(this.callbackHandler instanceof LoginCallbackHandler) {
			((LoginCallbackHandler)this.callbackHandler).setCallbackContext(this);
		}
	}

	public String getUsername() {
		return getStringValue(KEY_USERNAME);
	}
	public CallbackContext setUsername(String username) {
		this.share.put(KEY_USERNAME, username);
		return this;
	}
	public String getPassword() {
		return getStringValue(KEY_PASSWORD);
	}
	public CallbackContext setPassword(String password) {
		this.share.put(KEY_PASSWORD, password);
		return this;
	}
	public String getAddress() {
		return getStringValue(KEY_ADDRESS);
	}
	public CallbackContext setAddress(String address) {
		this.share.put(KEY_ADDRESS, address);
		return this;
	}
	public boolean getRemember() {
		return getBooleanValue(KEY_REMEMBER);
	}

	public CallbackContext setRemember(boolean enable) {
		this.share.put(KEY_REMEMBER, Boolean.toString(enable));
		return this;
	}
	public String getToken() {
		return getStringValue(KEY_TOKEN);
	}
	public CallbackContext setToken(String token) {
		this.share.put(KEY_TOKEN, token);
		return this;
	}

	public Options getOptions() {
		return this.options;
	}
	public void setOptions(Options options) {
		this.options = options;
	}

}
