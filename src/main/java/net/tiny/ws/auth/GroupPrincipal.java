package net.tiny.ws.auth;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Hashtable;

public class GroupPrincipal implements Group {

    private String name;
    private Hashtable<String,Principal> members = new Hashtable<String, Principal>();

    public GroupPrincipal(String name) {
        this.name = name;
    }

    @Override
    public boolean addMember(Principal member) {
        members.put(member.getName(), member);
        return true;
    }

    @Override
    public boolean removeMember(Principal member) {
        members.remove(member.getName());
        return true;
    }

    @Override
    public boolean isMember(Principal member) {
        return members.containsKey(member.getName());
    }

    @Override
    public Enumeration<? extends Principal> members() {
        return members.elements();
    }

    @Override
    public String getName() {
        return name;
    }

	@Override
	public String toString() {
		return String.format("GroupPrincipal[%1$s]:members(%2$d)",name, members.size());
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
