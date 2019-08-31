package net.tiny.ws.auth;

import java.io.IOException;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;

public abstract class HeadlessCallback implements Callback {

	protected static final Logger LOGGER =
	        Logger.getLogger(HeadlessCallback.class.getName());

	private CallbackContext callbackContext;
	private Options options;

	protected abstract void process() throws IOException;

	public final void process(LoginCallbackHandler handler) throws IOException {
		//Set username, password and other values
		//this.callbackContext.join(handler.getCallbackContext());
		this.callbackContext = handler.getCallbackContext();
		process();
	}

	public Options getOptions() {
		return this.options;
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	public CallbackContext getCallbackContext() {
		return this.callbackContext;
	}

	public void setCallbackContext(CallbackContext context) {
		this.callbackContext = context;
	}

	protected String getOption(String name, String value) {
		return this.options.getOption(name, value);
	}

	protected String getOption(String name) {
		return this.options.getOption(name);
	}

	protected void log(String message) {
		if(this.options.isDebug()) {
			LOGGER.log(this.options.getLogLevel(), message);
		}
	}
}
