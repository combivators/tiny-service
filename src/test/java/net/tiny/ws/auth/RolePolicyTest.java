package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.security.acl.Group;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;


public class RolePolicyTest {

    @Test
	public void testRolePolicy() throws Exception {
    	RolePolicy policy = RolePolicy.getPolicy(null);
    	assertNull(policy);
    	policy = RolePolicy.getPolicy("");
    	assertNull(policy);

		String rolePolicy = "prefix";
    	policy = RolePolicy.getPolicy(rolePolicy);
    	assertEquals(RolePolicy.PREFIXED_ROLES, policy);
    }

    @Test
	public void testGroupRolePolicy() throws Exception {

    	/**
    	 * jaas.conf Example:
    	 * jaas { YourAuthModule required debug=true logging.level=INFO
    	 *     role.policy = "group"
    	 *     role.discriminator = "admin";
    	 * };
    	 */
		boolean readOnly = false;
		Set<Principal> principals = new HashSet<Principal>();
		principals.add(new UserPrincipal("Hoge"));
		Set<?> pubCredentials = new HashSet<Object>();
		Set<?> privCredentials = new HashSet<Object>();
		Subject subject = new Subject(readOnly,
			       principals,
			       pubCredentials,
			       privCredentials);

    	String roleDiscriminator = "admin";
    	String rolePolicy = "group";
    	RolePolicy policy = RolePolicy.getPolicy(rolePolicy);
    	assertEquals(RolePolicy.GROUP_ROLES, policy);


    	System.out.println(subject.toString());
    	Set<Principal> rolePrincipals = new HashSet<Principal>();
    	rolePrincipals.add(new RolePrincipal("admin:product"));
    	rolePrincipals.add(new RolePrincipal("admin:productCategory"));
    	policy.handleRoles(subject, rolePrincipals, roleDiscriminator);
    	System.out.println(subject.toString());

    	Set<Group> groups = subject.getPrincipals(Group.class);
    	assertFalse(groups.isEmpty());
    	Group group = groups.iterator().next();
    	assertTrue(group.implies(subject));
    	assertTrue(group.isMember(new RolePrincipal("admin:product")));
    	assertTrue(group.isMember(new RolePrincipal("admin:productCategory")));
    	assertFalse(group.isMember(new RolePrincipal("member:review")));
    }


    @Test
	public void testPrefixRolePolicy() throws Exception {

    	/**
    	 * jaas.conf Example:
    	 * jaas { YourAuthModule required debug=true logging.level=INFO
    	 *     role.policy = "prefix"
    	 *     role.discriminator = "admin:";
    	 * };
    	 */
		boolean readOnly = false;
		Set<Principal> principals = new HashSet<Principal>();
		principals.add(new UserPrincipal("Hoge"));
		Set<?> pubCredentials = new HashSet<Object>();
		Set<?> privCredentials = new HashSet<Object>();
		Subject subject = new Subject(readOnly,
			       principals,
			       pubCredentials,
			       privCredentials);

    	String roleDiscriminator = "admin:";
    	String rolePolicy = "prefix";
    	RolePolicy policy = RolePolicy.getPolicy(rolePolicy);
    	assertEquals(RolePolicy.PREFIXED_ROLES, policy);


    	System.out.println(subject.toString());
    	Set<Principal> rolePrincipals = new HashSet<Principal>();
    	rolePrincipals.add(new RolePrincipal("product"));
    	rolePrincipals.add(new RolePrincipal("productCategory"));
    	policy.handleRoles(subject, rolePrincipals, roleDiscriminator);
    	System.out.println(subject.toString());

    	Set<RolePrincipal> roles = subject.getPrincipals(RolePrincipal.class);
    	assertFalse(roles.isEmpty());
    	assertEquals(2, roles.size());
    	assertTrue(roles.contains(new RolePrincipal("admin:product")));
    	assertFalse(roles.contains(new RolePrincipal("member")));
    }
}
