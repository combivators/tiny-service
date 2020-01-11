package net.tiny.ws.auth;

import java.io.Serializable;
import java.security.Principal;

public class RolePrincipal implements Principal, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * @serial
	 */
	private final String name;

	/**
	 * Create a RolePrincipal with a user name.
	 *
	 * <p>
	 *
	 * @param name
	 *            the Sample username for this user.
	 *
	 * @exception NullPointerException
	 *                if the <code>name</code> is <code>null</code>.
	 */
	public RolePrincipal(String name) {
		if (name == null)
			throw new NullPointerException("illegal null name");
		this.name = name;
	}

	/**
	 * Return the Sample username for this <code>RolePrincipal</code>.
	 *
	 * <p>
	 *
	 * @return the Sample username for this <code>RolePrincipal</code>
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Return a string representation of this <code>RolePrincipal</code>.
	 *
	 * <p>
	 *
	 * @return a string representation of this <code>RolePrincipal</code>.
	 */
	@Override
	public String toString() {
		return String.format("RolePrincipal[%1$s]",name);
	}

	/**
	 * Compares the specified Object with this <code>RolePrincipal</code>
	 * for equality. Returns true if the given object is also a
	 * <code>RolePrincipal</code> and the two RolePrincipals have the same
	 * username.
	 *
	 * <p>
	 *
	 * @param o
	 *            Object to be compared for equality with this
	 *            <code>RolePrincipal</code>.
	 *
	 * @return true if the specified Object is equal equal to this
	 *         <code>RolePrincipal</code>.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;

		if (!(o instanceof RolePrincipal))
			return false;
		RolePrincipal that = (RolePrincipal) o;

		return (this.getName().equals(that.getName()));
	}

	/**
	 * Return a hash code for this <code>RolePrincipal</code>.
	 *
	 * <p>
	 *
	 * @return a hash code for this <code>RolePrincipal</code>.
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
