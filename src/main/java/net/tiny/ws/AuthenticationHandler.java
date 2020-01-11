package net.tiny.ws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.auth.AuthenticationService;
import net.tiny.ws.auth.CallbackContext;
import net.tiny.ws.auth.DefaultCallbackHandler;
import net.tiny.ws.auth.UserToken;

public class AuthenticationHandler extends BaseWebService {

	public final static long DEFAULT_EXPIRED = 60L * 60L; // 3600s (1H)

	private String realm = "default";
	private long expired = DEFAULT_EXPIRED;
	private AuthenticationService service;
	private ServerRepository repository;

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public void setService(AuthenticationService service) {
		this.service = service;
	}

    public void setServerRepository(ServerRepository repository) {
    	this.repository = repository;
    }

	@Override
	protected boolean doGetOnly() {
		return true;
	}

	@Override
	protected void execute(HTTP_METHOD method, HttpExchange exchange) throws IOException {
    	if (null == service) {
    		exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
    		exchange.close();
    		return;
    	}

    	RequestHelper request = HttpHandlerHelper.getRequestHelper(exchange);
        String cmd = request.getParameter(0);
        if(cmd.startsWith("login")) {
        	login(request, exchange);
        } else if(cmd.startsWith("logout")) {
        	logout(request, exchange);
        } else {
    		exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
    		exchange.close();
        }
	}

	/**
	 * JAAS方式登录认证，认证成功: 加密的权限放入Cookie，返回 HTTP OK 200
	 *
	 * @param authCookie 加密权限Cookie
	 * @return JSON消息
	 */
    void login(RequestHelper request, HttpExchange he) throws IOException {
		String clientAddress = request.getRemoteAddress();
    	String authToken = request.getCookie(COOKIE_AUTHTOKEN, true);
    	try {
        	// If has valid authentication token
	    	if (null != authToken) {
	    		UserToken userToken = UserToken.create(authToken, clientAddress);
	    		// Remember me skip login
	    		if(null == userToken) {
	    			throw new LoginException("Authorization Required");
	    		} else if(userToken.isExpired()) {
	    			throw new LoginException("Authorization Expired");
	    		} else {
	    			if( userToken.getExpired() < (System.currentTimeMillis() + expired*1000L)) {
	    				//Keep token alive
	    				authToken = userToken.keepAlive();
	    			}
	    		}
	    	} else {
	    		final String[] credentials = getCredentials(request);
	    		// Do logon
	    		authToken = logon(credentials[0], credentials[1], clientAddress);
	    	}

			final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
			if (repository != null) {
	    		// HttpSession enable to create a new session
				ServerSession session = repository.createSession();
		        header.setCookie(String.format("%s=%s;path=/", COOKIE_SESSION, session.getId()));
			}

	        header.setCookie(String.format("%s=%s; path=/", COOKIE_AUTHTOKEN, authToken));
	        header.setContentType(MIME_TYPE.JSON);
	        header.set("Cache-Control", "no-cache, no-store");
	        String message = String.format("{\"token\" : \"%s\"}", authToken);
	        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, message.length());
	        he.getResponseBody().write(message.getBytes());
        } catch (LoginException ex) {
        	if (authToken != null) {
        		final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        		//Set Max-Age = 0 to remove authentication token cookie
        		header.setCookie(String.format("%s=%s; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; max-age=0", COOKIE_AUTHTOKEN, authToken));
        	}
        	he.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1);
        } catch (SecurityException ex) {
	        he.sendResponseHeaders(HttpURLConnection.HTTP_FORBIDDEN, -1);
        } catch (RuntimeException ex) {
	        he.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
        } finally {
        	he.close();
        }
    }
	/**
	 * 从HTTP Header取得Basic认证，
	 * 从HTTP URL取得认证信息
	 * @return
	 */
    String[] getCredentials(RequestHelper request) throws LoginException {
    	// Lookup HTTP header Basic authorization
		String[] credentials = request.getBasicAuthorization();
		if( null != credentials) {
			return credentials;
		}

		// Lookup HTTP parameter "/account/login/Hoge/password"
		String username = request.getParameter(1);
		String password = request.getParameter(2);
		if(!username.isEmpty() && !password.isEmpty()) {
			return new String[] {username, password};
		}

		// Lookup HTTP Query "/account/login?u=Hoge&p=password"
		final Map<String, List<String>> query = request.getParameters();
		List<String> values = query.get("u");
		if (null != values) {
			username = values.get(0);
		}
		values = query.get("p");
		if (null != values) {
			password = values.get(0);
		}
		if(!username.isEmpty() && !password.isEmpty()) {
			return new String[] {username, password};
		}

		throw new LoginException();
    }
	/**
	 * JAAS方式登录认证 ，认证成功  返回加密的权限
	 * @return
	 */
	private String logon(String username, String password, String address) throws LoginException {
		CallbackContext callbackContext = new CallbackContext()
				.setUsername(username)
				.setPassword(password)
				.setAddress(address);

		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, service);
		final CallbackHandler callbackHandler = callbackContext.getCallbackHandler();
		// Create a LoginContext with a callback handler
		final LoginContext context = new LoginContext(realm, callbackHandler);
		// Perform authentication
	    context.login();
        final Subject subject = context.getSubject();
        final Set<String> tokens = subject.getPrivateCredentials(String.class);
        return tokens.iterator().next();
	}

	/**
	 * 退出登录 ，删除Session数据和Cookie中的加密的权限数据
	 */
	void logout(RequestHelper request, HttpExchange he) throws IOException {
		int stats = HttpURLConnection.HTTP_OK;
    	// If has valid authentication token
    	String authToken = request.getCookie(COOKIE_AUTHTOKEN, true);
    	if (null != authToken) {
    		String clientAddress = request.getRemoteAddress();
    		UserToken userToken = UserToken.create(authToken, clientAddress);
    		if(null != userToken) {
    	        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
    			if (repository != null) {
    				// Clear session
    				String sessionId = request.getCookie(COOKIE_SESSION, true);
    				if (null != sessionId) {
    					repository.closeSession(sessionId)
    						.ifPresent(s -> s.clear());
    					//Set Max-Age = 0 to remove session cookie
    	    	        header.setCookie(String.format("%s=%s; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; max-age=0", COOKIE_SESSION, sessionId));
    				}
    			}
    			//Set Max-Age = 0 to remove authentication token cookie
    	        header.setCookie(String.format("%s=%s; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; max-age=0", COOKIE_AUTHTOKEN, authToken));
    		} else {
    			// An bad authentication token
    			stats = HttpURLConnection.HTTP_FORBIDDEN;
    		}
    	} else {
    		// Not authoritative
    		stats = HttpURLConnection.HTTP_UNAUTHORIZED;
    	}
        he.sendResponseHeaders(stats, -1);
        he.close();
	}
}
