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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.user;

import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.Nullable;

import static is.codion.common.utilities.Configuration.integerValue;
import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a username and password.
 * Factory for {@link User} instances.
 * Note that a {@link User} instance is mutable as the password can be cleared.
 * @see #user(String)
 * @see #user(String, char[])
 * @see #parse(String)
 */
public interface User {

	/**
	 * Specifies the maximum allowed username length.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 256
	 * </ul>
	 */
	PropertyValue<Integer> MAXIMUM_USERNAME_LENGTH = integerValue(User.class.getName() + ".maximumUsernameLength", 256);

	/**
	 * Specifies the maximum allowed password length.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 1024
	 * </ul>
	 */
	PropertyValue<Integer> MAXIMUM_PASSWORD_LENGTH = integerValue(User.class.getName() + ".maximumPasswordLength", 1024);

	/**
	 * @return the username
	 */
	String username();

	/**
	 * @return the password, an empty array in case of an empty password
	 */
	char[] password();

	/**
	 * Clears the password
	 * @return this User instance
	 */
	User clearPassword();

	/**
	 * @return a copy of this User
	 */
	User copy();

	/**
	 * Creates a new User with an empty password.
	 * @param username the username
	 * @return a new User
	 * @throws IllegalArgumentException in case username is an empty string or exceeds {@link #MAXIMUM_USERNAME_LENGTH}
	 */
	static User user(String username) {
		return user(username, null);
	}

	/**
	 * Creates a new User.
	 * @param username the username
	 * @param password the password
	 * @return a new User
	 * @throws IllegalArgumentException in case username is an empty string, username exceeds {@link #MAXIMUM_USERNAME_LENGTH},
	 * or password exceeds {@link #MAXIMUM_PASSWORD_LENGTH}
	 */
	static User user(String username, char @Nullable [] password) {
		return new DefaultUser(username, password);
	}

	/**
	 * Parses a User from a string, containing a username and password with a single ':' as delimiter, i.e. "user:pass"
	 * or "user:" for en empty password. If no delimiter is found the whole string is assumed to be the username
	 * and the password empty. The username portion is trimmed. Any delimeters beyond the initial one are assumed
	 * to be part of the password.
	 * @param userPassword the username and password string
	 * @return a User with the given username and password
	 * @throws IllegalArgumentException in case the username portion is empty, username exceeds {@link #MAXIMUM_USERNAME_LENGTH},
	 * or password exceeds {@link #MAXIMUM_PASSWORD_LENGTH}
	 */
	static User parse(String userPassword) {
		String[] split = requireNonNull(userPassword).split(":", 2);
		if (split.length == 1) {
			return new DefaultUser(split[0].trim(), null);
		}

		//here split is of length 2, as per split limit
		return new DefaultUser(split[0].trim(), split[1].toCharArray());
	}
}