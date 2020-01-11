package net.tiny.ws.auth;

import java.security.Principal;
import java.security.acl.Group;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

public enum RolePolicy {

    PREFIXED_ROLES("prefix") {
        public void handleRoles(Subject subject,Set<Principal> principals,String discriminator) {
            for(Principal p : principals) {
                if(p instanceof RolePrincipal){
                    RolePrincipal rolePrincipal = new RolePrincipal(discriminator + p.getName());
                    subject.getPrincipals().add(rolePrincipal);
                } else {
                    subject.getPrincipals().add(p);
                }
            }
        }
    },
    GROUP_ROLES("group") {
        public void handleRoles(Subject subject,Set<Principal> principals,String discriminator) {
            Group group = new GroupPrincipal(discriminator);
            for(Principal p : principals) {
                if(p instanceof RolePrincipal) {
                    group.addMember(p);
                } else {
                    subject.getPrincipals().add(p);
                }
            }
            subject.getPrincipals().add(group);
        }
    };

    private String value;

    private static final Map<String, RolePolicy> policies = new HashMap<String, RolePolicy>();

    static {
        for (RolePolicy s : EnumSet.allOf(RolePolicy.class)) {
            policies.put(s.getValue(), s);
        }
    }

    private RolePolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public static RolePolicy getPolicy(String code) {
        return policies.get(code);
    }

    public abstract void handleRoles(Subject subject,Set<Principal> principals,String discriminator);

}