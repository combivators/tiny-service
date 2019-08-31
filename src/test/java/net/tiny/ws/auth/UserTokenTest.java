package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserTokenTest {

	@Test
	public void testExpiredTime() throws Exception {
		long currentTime = new Date(0L).getTime();
		long expired = UserToken.expiredTime(currentTime);
		System.out.println(currentTime + " " + expired);

		currentTime = 1565222979000L;
		expired = UserToken.expiredTime(currentTime);
		System.out.println(currentTime + " " + expired);
		assertEquals(20190808090939L, expired);

		assertEquals(20190808, (int)(expired / 1000000L));
		assertEquals(90939, (int)(expired % 1000000L));

		expired = 19780101010101L;
		assertEquals(19780101, (int)(expired / 1000000L));
		assertEquals(10101, (int)(expired % 1000000L));

		expired = 21991231123459L;
		assertEquals(21991231, (int)(expired / 1000000L));
		assertEquals(123459, (int)(expired % 1000000L));
	}

	@Test
	public void testCurrentTime() throws Exception {
		Crypt  crypt = Crypt.create(Crypt.AES);
		String singned = "Hoge/CwoqFdHtNyS0JzkAneJ2yzApTINwUZHXo1IUci";
		String token = crypt.encrypt(singned);
		String decrypted = crypt.decrypt(token);
		assertEquals(singned, decrypted);
		System.out.println(token.length() + "\t" + token); //

		// An fast EnDecoder
		singned = "Hoge/CwoqFdHtNyS0JzkAneJ2yzApTINwUZHXo1IUci";
		token = Codec.encodeString(singned.getBytes());
		decrypted = new String(Codec.decodeString(token));
		assertEquals(singned, decrypted);
		System.out.println(token.length() + "\t" + token); //

		String username = "Hoge";
		String password = "password";
		String address = "192.168.1.100";
		UserToken userToken = UserToken.create(username, password, address);
		assertNotNull(userToken);
		userToken.setIssuer(999);
		String lastToken = userToken.getToken();
		System.out.println(lastToken);
		System.out.println(userToken.toString());
		UserToken ut = UserToken.create(lastToken, "192.168.1.100");
		assertNotNull(ut);
		assertEquals("Hoge", ut.getUsername());
		assertTrue(userToken.equals(ut));
		assertFalse(userToken.isExpired());
	}

	@Test
	public void testCreateUserTokenWithoutRoles() throws Exception {
		String username = "Hoge";
		String password = "password";
		String address = "192.168.1.100";
		UserToken userToken = UserToken.create(username, password, address);
		userToken.setIssuer(999);
		assertNotNull(userToken);
		String lastToken = userToken.getToken();
		System.out.println(lastToken);
		for(int i=0; i<5; i++) {
			Thread.sleep(250L);
			userToken = UserToken.create(username, password, address);
			String token = userToken.getToken();
			assertNotSame(token, lastToken);
			lastToken = token;
			System.out.println(lastToken);
		}

		System.out.println("Expired: " + userToken.getExpired() + "  " + userToken.hashCode());
		System.out.println(userToken.toString());

		UserToken ut = UserToken.create(lastToken, "192.168.1.100");
		assertNotNull(ut);
		assertEquals("Hoge", ut.getUsername());
		assertTrue(userToken.equals(ut));
		assertFalse(userToken.isExpired());

		ut = UserToken.create(lastToken, "192.168.1.200");
		assertNull(ut);
	}

	@Test
	public void testSimpleUserToken() throws Exception {
		String username = "Hoge";
		String password = "password";
		String address = "192.168.1.100";
		UserToken userToken = UserToken.create(username, password, address);
		String lastToken = userToken.getToken();
		UserToken token = UserToken.create(lastToken, "192.168.1.100");
		assertNotNull(token);
		System.out.println(token.toString());
		Set<String> allowedRoles = new HashSet<>();
		allowedRoles.add("admin:product");
		allowedRoles.add("admin:productCategory");
		assertTrue(token.inRole(allowedRoles));
	}

	@Test
	public void testExpiredUserToken() throws Exception {
		String username = "Hoge";
		String password = "password";
		String address = "192.168.1.100";
		UserToken userToken = UserToken.create(username, password, address);
		userToken.setIssuer(999);
		Set<String> roleSet = new HashSet<>();
		roleSet.add("admin");
		userToken.setRoles(roleSet);

		String lastToken = userToken.getToken();
		UserToken token = UserToken.create(lastToken, "192.168.1.100");
		assertNotNull(token);
		Set<String> allowedRoles = new HashSet<>();
		allowedRoles.add("admin");
		assertTrue(token.inRole(allowedRoles));
		assertFalse(token.isExpired());

		System.out.println(token.toString());
		String at = token.getToken();
		System.out.println(String.format("Token: '%s'", at));

		Thread.sleep(1500L);
		assertFalse(token.isExpired());
		token.keepAlive();
		String before = token.toString();
		System.out.println(before);
		String nt = token.getToken();
		System.out.println(String.format("Token: '%s'", nt));
		assertNotEquals(at, nt);

		token = UserToken.create(nt, "192.168.1.100");
		String after = token.toString();
		System.out.println(before);
		System.out.println(after);
		assertEquals(before, after);
	}

	@Test
	public void testCreateUserTokenWithRoles() throws Exception {
		String username = "Hoge";
		String password = "pssword9999";
		String address = "192.168.1.100";
		UserToken userToken = UserToken.create(username, password, address);
		userToken.setIssuer(999);
		Set<String> roleSet = new HashSet<>();
		roleSet.add("admin:product");
		roleSet.add("admin:productCategory");
		roleSet.add("admin:productNotify");
		userToken.setRoles(roleSet);
		assertNotNull(userToken);
		String lastToken = userToken.getToken();

		System.out.println(userToken.toString() + "\t" + lastToken);
		for(int i=0; i<5; i++) {
			Thread.sleep(250L);
			userToken = UserToken.create(username, password, address);
			userToken.setIssuer(999);
			userToken.setRoles(roleSet);
			String token = userToken.getToken();
			assertNotSame(token, lastToken);
			lastToken = token;
			System.out.println(lastToken);
		}

		UserToken ut = UserToken.create(lastToken, "192.168.1.100");
		assertNotNull(ut);
		assertEquals("Hoge", ut.getUsername());
		assertTrue(userToken.equals(ut));
		assertFalse(userToken.isExpired());
		assertEquals(999, ut.getIssuer());

		roleSet = new HashSet<>();
		roleSet.add("admin:product");
		roleSet.add("admin:productCategory");
		assertTrue(userToken.inRole(roleSet));

		roleSet = new HashSet<>();
		roleSet.add("admin:product");
		roleSet.add("admin:members");
		assertFalse(userToken.inRole(roleSet));
		System.out.println(userToken.toString());
	}

	@Test
	public void testBenchmarkUserToken() throws Exception {
		String username = "Hoge";
		String password = "pssword";
		String address = "192.168.1.100";
		int issuer = 999;
		Set<String> adminRoleSet = new HashSet<>();
		adminRoleSet.add("admin:product");
		adminRoleSet.add("admin:productCategory");
		adminRoleSet.add("admin:productNotify");

		Set<String> memberRoleSet = new HashSet<>();
		memberRoleSet.add("member");

		List<String> tokens = new ArrayList<>();
		int number = 100;

		for(int i=0; i<number; i++) {
			UserToken userToken = UserToken.create(username+i, password, address);
			userToken.setAddress(address);
			userToken.setIssuer(issuer);
			userToken.setRoles(adminRoleSet);
			String token = userToken.getToken();
			tokens.add(token);
		}

		number = 9900;
		username = "Fuga";
		for(int i=0; i<number; i++) {
			UserToken userToken = UserToken.create(username+i, password, address);
			userToken.setAddress(address);
			userToken.setIssuer(issuer);
			userToken.setRoles(memberRoleSet);
			String token = userToken.getToken();
			tokens.add(token);
		}

		System.out.println("测试开始，循环次数：" + tokens.size());
		System.out.println("----------------------------------------------------------------------------------");
		long currTime = System.currentTimeMillis();
		for(String token : tokens) {
			UserToken userToken = UserToken.create(token, address);
			assertNotNull(userToken);
		}
		System.out.println("执行结束，耗时" + (System.currentTimeMillis() - currTime) + "ms");
	}
}
