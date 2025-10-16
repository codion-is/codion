/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.user;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

final class DefaultUser implements User, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private static final int MAX_USERNAME_LENGTH = MAXIMUM_USERNAME_LENGTH.getOrThrow();
	private static final int MAX_PASSWORD_LENGTH = MAXIMUM_PASSWORD_LENGTH.getOrThrow();

	private String username;
	private char[] password;

	DefaultUser(String username, char @Nullable [] password) {
		requireNonNull(username);
		validateUsernameAndPassword(username, password);
		this.username = username;
		this.password = createPassword(password);
	}

	@Override
	public String username() {
		return username;
	}

	@Override
	public char[] password() {
		return Arrays.copyOf(password, password.length);
	}

	@Override
	public User clearPassword() {
		Arrays.fill(password, (char) 0);
		this.password = createPassword(null);
		return this;
	}

	@Override
	public User copy() {
		return new DefaultUser(username, password());
	}

	@Override
	public String toString() {
		return "User: " + username;
	}

	/**
	 * User objects are equal if the usernames are equal, ignoring case
	 */
	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof User && ((User) obj).username().equalsIgnoreCase(username);
	}

	@Override
	public int hashCode() {
		return username.hashCode();
	}

	@Serial
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeObject(username);
		stream.writeObject(password);
	}

	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		String deserializedUsername = (String) stream.readObject();
		char[] deserializedPassword = (char[]) stream.readObject();

		validateUsernameAndPassword(deserializedUsername, deserializedPassword);
		this.username = deserializedUsername;
		this.password = createPassword(deserializedPassword);
	}

	private static void validateUsernameAndPassword(String username, char @Nullable [] password) {
		if (username.isEmpty()) {
			throw new IllegalArgumentException("Username must be non-empty");
		}
		if (username.length() > MAX_USERNAME_LENGTH) {
			throw new IllegalArgumentException("Username length (" + username.length() +
							") exceeds maximum allowed length (" + MAX_USERNAME_LENGTH + ")");
		}
		if (password != null && password.length > MAX_PASSWORD_LENGTH) {
			throw new IllegalArgumentException("Password length (" + password.length +
							") exceeds maximum allowed length (" + MAX_PASSWORD_LENGTH + ")");
		}
	}

	private static char[] createPassword(char @Nullable [] password) {
		return password == null ? new char[0] : Arrays.copyOf(password, password.length);
	}
}
