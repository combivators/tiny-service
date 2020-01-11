package net.tiny.ws.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * <p>
 * This class implements the <code>Principal</code> interface and represents a
 * Sample user.
 *
 * <p>
 * Principals such as this <code>UserPrincipal</code> may be associated with
 * a particular <code>Subject</code> to augment that <code>Subject</code>
 * with an additional identity. Refer to the <code>Subject</code> class for
 * more information on how to achieve this. Authorization decisions can then be
 * based upon the Principals associated with a <code>Subject</code>.
 *
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class UserPrincipal implements Principal, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * @serial
	 */
	private final String name;

	/**
	 * Create a UserPrincipal with a Sample username.
	 *
	 * <p>
	 *
	 * @param name
	 *            the Sample username for this user.
	 *
	 * @exception NullPointerException
	 *                if the <code>name</code> is <code>null</code>.
	 */
	public UserPrincipal(String name) {
		if (name == null)
			throw new NullPointerException("illegal null name");
		this.name = name;
	}

	/**
	 * Return the Sample username for this <code>UserPrincipal</code>.
	 *
	 * <p>
	 *
	 * @return the Sample username for this <code>UserPrincipal</code>
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Return a string representation of this <code>UserPrincipal</code>.
	 *
	 * <p>
	 *
	 * @return a string representation of this <code>UserPrincipal</code>.
	 */
	@Override
	public String toString() {
		return String.format("UserPrincipal[%1$s]",name);
	}

	/**
	 * Compares the specified Object with this <code>UserPrincipal</code>
	 * for equality. Returns true if the given object is also a
	 * <code>UserPrincipal</code> and the two UserPrincipals have the same
	 * username.
	 *
	 * <p>
	 *
	 * @param o
	 *            Object to be compared for equality with this
	 *            <code>UserPrincipal</code>.
	 *
	 * @return true if the specified Object is equal equal to this
	 *         <code>UserPrincipal</code>.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;

		if (!(o instanceof UserPrincipal))
			return false;
		UserPrincipal that = (UserPrincipal) o;

		return (this.getName().equals(that.getName()));
	}

	/**
	 * Return a hash code for this <code>UserPrincipal</code>.
	 *
	 * <p>
	 *
	 * @return a hash code for this <code>UserPrincipal</code>.
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
