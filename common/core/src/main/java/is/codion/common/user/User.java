/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.user;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a username and password.
 * Factory class for {@link User} instances.
 * Note that a {@link User} instance is mutable as the password can be cleared.
 * @see #user(String)
 * @see #user(String, char[])
 * @see #parse(String)
 */
public interface User {

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
   * @throws IllegalArgumentException in case username is an empty string
   */
  static User user(String username) {
    return user(username, null);
  }

  /**
   * Creates a new User.
   * @param username the username
   * @param password the password
   * @return a new User
   * @throws IllegalArgumentException in case username is an empty string
   */
  static User user(String username, char[] password) {
    return new DefaultUser(username, password);
  }

  /**
   * Parses a User from a string, containing a username and password with a single ':' as delimiter, i.e. "user:pass"
   * or "user:" for en empty password. If no delimiter is found the whole string is assumed to be the username
   * and the password empty. The username portion is trimmed. Any delimeters beyond the initial one are assumed
   * to be part of the password.
   * @param userPassword the username and password string
   * @return a User with the given username and password
   * @throws IllegalArgumentException in case the username portion is empty
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