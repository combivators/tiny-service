package net.tiny.ws.auth;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public abstract class BaseModule implements LoginModule {

	protected static final Logger LOGGER =
	        Logger.getLogger(BaseModule.class.getName());

	protected static final long REMEMBER_ALIVE = 31L * 3L * 24L * 3600L * 1000L; // 3 months
	protected static final String DEFAULT_AUTH_MODULE = "UserAuth";
	protected static final String KEY_EXPIRED  = "expired";
	protected static final String KEY_CALLBACK = "callback";

	// initial state
	protected Subject subject;
	protected CallbackHandler callbackHandler;
	protected Map<String, ?> sharedState;
	protected Options options;
    protected RolePolicy rolePolicy;
    protected String roleDiscriminator;
    protected List<Callback> callbacks;

    private CallbackContext callbackContext = null;

	// user token
	private UserToken userToken;
	// the authentication status
	private boolean succeeded = false;
	private boolean commitSucceeded = false;
	// configurable option
	private boolean debug = false;
	private Level level = Level.FINE;

	abstract protected UserToken verify(String username, char[] password, String address, LocalDateTime time) throws LoginException;

	/**
	 * Initialize this <code>LoginModule</code>.
	 *
	 * @param subject
	 *            the <code>Subject</code> to be authenticated.
	 *
	 * @param callbackHandler
	 *            a <code>CallbackHandler</code> for communicating with the
	 *            end user (prompting for user names and passwords, for
	 *            example).
	 *
	 * @param sharedState
	 *            shared <code>LoginModule</code> state.
	 *
	 * @param options
	 *            options specified in the login <code>Configuration</code>
	 *            for this particular <code>LoginModule</code>.
	 *
	 * @see LoginModule#initialize(Subject, CallbackHandler, Map, Map)
	 */
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		// initialize any configured options
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		this.options = Options.valueOf(options);
		this.debug = this.options.isDebug();
        this.level = this.options.getLogLevel();
        String classeValue = this.options.getOption(KEY_CALLBACK, null);

        if(null == classeValue) {
        	classeValue = SimpleCallback.class.getName();
        }
        this.callbacks = new ArrayList<>();
        String[] callbackClasses = classeValue.split(",");
        for(String className : callbackClasses) {
        	try {
        		Object bean = Class.forName(className).newInstance();
        		if(bean instanceof HeadlessCallback) {
        			HeadlessCallback hcb = (HeadlessCallback)bean;
        			hcb.setOptions(this.options);
        			//hcb.setCallbackContext(this.callbackContext);
        			this.callbacks.add(hcb);
        		}
        	} catch (Exception ex) {
        		LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        	}
        }
		this.rolePolicy = RolePolicy.getPolicy(getOption("role.policy"));
        this.roleDiscriminator = getOption("role.discriminator");
		if (debug && null != subject && null != callbackHandler) {
			LOGGER.fine(String.format("[JAAS] Module: %1$s \tCallback: %2$s \r\n%3$s   ",
							toString(), classeValue, subject.toString()));
		}
	}

	/**
	 * Authenticate the user by prompting for a user name and password.
	 *
	 * <p>
	 *
	 * @return true in all cases since this <code>LoginModule</code> should
	 *         not be ignored.
	 *
	 * @exception FailedLoginException
	 *                if the authentication fails.
	 *
	 *
	 * @exception LoginException
	 *                if this <code>LoginModule</code> is unable to perform
	 *                the authentication.
	 * <p>
	 */
	public boolean login() throws LoginException {
		// prompt for a user name and password
		Callback[] callbacks = getCallbacks();
		try {
			this.callbackHandler.handle(callbacks);
		} catch (IOException | UnsupportedCallbackException ex) {
			throw new LoginException("CallbackHandler error: " + ex.getMessage());
		} catch (SecurityException ex) {
			throw new FailedLoginException(ex.getMessage());
		}

		PasswordCallback passwordCallback = null;
		String username = null;
		char[] password = new char[0];
		String address = "localhost";
		for(Callback callback : callbacks) {
			if(callback instanceof HeadlessCallback) {
				HeadlessCallback headlessCallback = (HeadlessCallback)callback;
				CallbackContext context = headlessCallback.getCallbackContext();
				if(this.callbackContext == null) {
					this.callbackContext = context;
				}
				username = context.getUsername();
				String pass = context.getPassword();
				if(null != pass) {
					password = pass.toCharArray();
				} else {
					password = new char[0];
				}
				address  = context.getAddress();
			} else  if(callback instanceof NameCallback) {
				username = ((NameCallback)callback).getName();
			} else if(callback instanceof PasswordCallback) {
				passwordCallback =  (PasswordCallback) callback;
				password = passwordCallback.getPassword();
			}
		}

		// print debugging information
		if (debug) {
			LOGGER.fine(String.format("[JAAS] Login '%1$s' from %2$s", username, address));
		}
		succeeded = false;

		this.userToken = verify(username, password, address, LocalDateTime.now());
		// authentication succeeded!!!
		succeeded = true;
		if(null != passwordCallback) {
			passwordCallback.clearPassword();
		}
		return true;
	}

	/**
	 * <p>
	 * This method is called if the LoginContext's overall authentication
	 * succeeded (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
	 * LoginModules succeeded).
	 *
	 * <p>
	 * If this LoginModule's own authentication attempt succeeded (checked by
	 * retrieving the private state saved by the <code>login</code> method),
	 * then this method associates a <code>SamplePrincipal</code> with the
	 * <code>Subject</code> located in the <code>LoginModule</code>. If
	 * this LoginModule's own authentication attempted failed, then this method
	 * removes any state that was originally saved.
	 *
	 * <p>
	 *
	 * @exception LoginException
	 *                if the commit fails.
	 *
	 * @return true if this LoginModule's own login and commit attempts
	 *         succeeded, or false otherwise.
	 */
	public boolean commit() throws LoginException {
		if (succeeded == false) {
			return false;
		}
		// add a Principal (authenticated identity)  to the Subject
		// assume the user we authenticated is the UserPrincipal
		Principal userPrincipal = new UserPrincipal(this.userToken.getUsername());
		Set<Principal> userPrincipals = new HashSet<Principal>();
		userPrincipals.add(userPrincipal);
		if(this.subject == null) {
			boolean readOnly = false;
			this.subject = new Subject(readOnly,
					userPrincipals,
				    new HashSet<Object>(),
				    new HashSet<Object>());
		}

		Set<Principal> rolePrincipals = new HashSet<>();
		Set<String> roles = this.userToken.getRoles();
		for(String role : roles) {
			rolePrincipals.add(new RolePrincipal(role));
		}

        if (rolePolicy != null && this.roleDiscriminator != null) {
        	rolePolicy.handleRoles(this.subject, rolePrincipals, this.roleDiscriminator);
        } else {
        	this.subject.getPrincipals().addAll(userPrincipals);
        	this.subject.getPrincipals().addAll(rolePrincipals);
        }
		this.subject.getPrivateCredentials().add(this.userToken.getToken());
		// in any case, clean out state
		commitSucceeded = true;
		if (debug) {
			LOGGER.fine(String.format("[JAAS] Commit token: %1$s\r\n%2$s",
					userToken.toString(), subject.toString()));
		}
		return true;

	}

	/**
	 * This method is called if the LoginContext's overall authentication
	 * failed. (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
	 * LoginModules did not succeed).
	 *
	 * If this LoginModule's own authentication attempt succeeded (checked by
	 * retrieving the private state saved by the <code>login</code> and
	 * <code>commit</code> methods), then this method cleans up any state that
	 * was originally saved.
	 *
	 * @exception LoginException
	 *                if the abort fails.
	 *
	 * @return false if this LoginModule's own login and/or commit attempts
	 *         failed, and true otherwise.
	 */
	public boolean abort() throws LoginException {
		if (succeeded == false) {
			return false;
		} else if (succeeded == true && commitSucceeded == false) {
			// login succeeded but overall authentication failed
			destory();
		} else {
			// overall authentication succeeded and commit succeeded,
			// but someone else's commit failed
			logout();
		}
		return true;
	}

	/**
	 * Logout the user.
	 *
	 * This method removes the <code>SamplePrincipal</code> that was added by
	 * the <code>commit</code> method.
	 *
	 * @exception LoginException
	 *                if the logout fails.
	 *
	 * @return true in all cases since this <code>LoginModule</code> should
	 *         not be ignored.
	 */
	public boolean logout() throws LoginException {
		if (debug) {
			LOGGER.fine(String.format("[JAAS] Logout %1$s" , subject.toString()));
		}
		destory();
		return true;
	}

	private void destory() {
		this.succeeded = false;
		this.commitSucceeded = false;
		this.userToken = null;
		if(this.callbackContext != null) {
			this.callbackContext.clear();
		}
		if(this.subject != null) {
			this.subject.getPrincipals().clear();
			this.subject.getPublicCredentials().clear();
			this.subject.getPrivateCredentials().clear();
			this.subject = null;
		}
	}

	protected boolean isDebug() {
		return this.debug;
	}
	protected Level getLogLevel() {
		return this.level;
	}
	protected String getOption(String name, String value) {
		return this.options.getOption(name, value);
	}
	protected String getOption(String name) {
		return this.options.getOption(name);
	}

	protected CallbackContext getCallbackContext() {
		return this.callbackContext;
	}

	private Callback[] getCallbacks() {
		return this.callbacks.toArray(new Callback[this.callbacks.size()]);
	}
}
