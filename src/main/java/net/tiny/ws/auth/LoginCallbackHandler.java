package net.tiny.ws.auth;

import javax.security.auth.callback.CallbackHandler;

public interface LoginCallbackHandler extends CallbackHandler {
	CallbackContext getCallbackContext();
	void setCallbackContext(CallbackContext callbackContext);
}
