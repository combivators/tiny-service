package net.tiny.ws.auth;

import java.io.IOException;
import java.util.Base64;
import java.util.StringTokenizer;


public class RequestAuthCallback extends HeadlessCallback {

	private static final String AUTHORIZATION_PROPERTY = "Authorization";
	private static final String AUTHENTICATION_SCHEME  = "Basic";
	private static final String REMOTE_ADDRESS         = "remote";
	private static final String KEY_TYPE  = "type";

	public static enum Type {
    	basic,
    	header,
    	cooike
    }

	@Override
	protected void process() throws IOException {
		String type = super.getOption(KEY_TYPE, "basic");
		Type authType = Type.valueOf(type);
		switch(authType) {
		case basic:
			basicAuth();
			break;
		case header:
			//headerAuth();
			break;
		case cooike:
			//cooikeAuth();
			break;
		}

	}

	void basicAuth() {
		CallbackContext context = super.getCallbackContext();
        // Get Authorization header
		String auth = context.getStringValue(AUTHORIZATION_PROPERTY);
        if (auth != null && auth.startsWith(AUTHENTICATION_SCHEME)) {
	        String userpassEncoded = auth.substring(6).trim();
	        String usernameAndPassword = new String(Base64.getDecoder().decode(userpassEncoded));
	        //Split username and password tokens
	        final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
	        context.setUsername(tokenizer.nextToken());
	        context.setPassword(tokenizer.nextToken());
	        String clientAddress = context.getStringValue(REMOTE_ADDRESS);
	        context.setAddress(clientAddress);
        }
	}
}
