package net.tiny.ws.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public abstract class AbstractCallbackHandler implements LoginCallbackHandler {

	private static final Logger LOGGER =
	        Logger.getLogger(AbstractCallbackHandler.class.getName());

    private CallbackContext callbackContext;


	/**
	 * Invoke an array of Callbacks.
	 *
	 * <p>
	 *
	 * @param callbacks
	 *            an array of <code>Callback</code> objects which contain the
	 *            information requested by an underlying security service to be
	 *            retrieved or displayed.
	 *
	 * @exception java.io.IOException
	 *                if an input or output error occurs.
	 *                <p>
	 *
	 * @exception UnsupportedCallbackException
	 *                if the implementation of this method does not support one
	 *                or more of the Callbacks specified in the
	 *                <code>callbacks</code> parameter.
	 */
	@Override
	public void handle(Callback[] callbacks)
		throws IOException, UnsupportedCallbackException {

		for (int i = 0; i < callbacks.length; i++) {
			if(callbacks[i] instanceof HeadlessCallback) {
				HeadlessCallback callback = (HeadlessCallback) callbacks[i];
				//callback.process(this);
				callback.process(this);
			} else if (callbacks[i] instanceof TextOutputCallback) {
				// display the message according to the specified type
				TextOutputCallback toc = (TextOutputCallback) callbacks[i];
				switch (toc.getMessageType()) {
					case TextOutputCallback.INFORMATION:
						LOGGER.info(toc.getMessage());
						break;
					case TextOutputCallback.WARNING:
						LOGGER.warning(toc.getMessage());
						break;
					case TextOutputCallback.ERROR:
						LOGGER.severe(toc.getMessage());
						break;
					default:
						throw new IOException("Unsupported message type: "
								+ toc.getMessageType());
				}
			} else if (callbacks[i] instanceof NameCallback) {
				// prompt the user for a username
				NameCallback nc = (NameCallback) callbacks[i];
				//WARNING: name is null.
				System.out.print(nc.getPrompt());
				System.out.flush();
				nc.setName(readLine(System.in));

			} else if (callbacks[i] instanceof PasswordCallback) {
				// prompt the user for sensitive information
				PasswordCallback pc = (PasswordCallback) callbacks[i];
				//WARNING: password is null.
				System.out.print(pc.getPrompt());
				System.out.flush();
				pc.setPassword(readLine(System.in).toCharArray());
			} else {
				throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
			}
		}
	}

	@Override
	public CallbackContext getCallbackContext() {
		return this.callbackContext;
	}

	@Override
	public void setCallbackContext(CallbackContext callbackContext) {
		this.callbackContext = callbackContext;
	}

	private String readLine(InputStream in) throws IOException {
		return (new BufferedReader
			      (new InputStreamReader(in))).readLine();
	}
}
