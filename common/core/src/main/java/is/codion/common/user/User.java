/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.user;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a username and password.
 * Factory class for {@link User} instances.
 */
public interface User {

  /**
   * @return the username
   */
  String getUsername();

  /**
   * @param password the password
   */
  void setPassword(char[] password);

  /**
   * @return the password
   */
  char[] getPassword();

  /**
   * Clears the password
   */
  void clearPassword();

  /**
   * Instantiates a new User with an empty password.
   * @param username the username
   * @return a new User
   */
  static User user(String username) {
    return user(username, null);
  }

  /**
   * Instantiates a new User.
   * @param username the username
   * @param password the password
   * @return a new User
   */
  static User user(String username, char[] password) {
    return new DefaultUser(username, password);
  }

  /**
   * Parses a User from a string, containing a username and password with a single ':' as delimiter, i.e. "user:pass"
   * or "user:" for en empty password. If no delimiter is found the whole string is assumed to be the username
   * and the password empty. The username portion is trimmed. Any delimeter beyond the initial one are assumed
   * to be part of the password.
   * @param userPassword the username and password string
   * @return a User with the given username and password
   */
  static User parse(String userPassword) {
    String[] split = requireNonNull(userPassword).split(":", 2);
    if (split.length == 1) {
      return new DefaultUser(split[0].trim(), null);
    }
    if (split.length != 2) {
      throw new IllegalArgumentException("Expecting a username and password with a single ':' as delimiter, multiple delimiters found");
    }
    if (split[0].isEmpty() || split[1].isEmpty()) {
      throw new IllegalArgumentException("Both username and password are required");
    }

    return new DefaultUser(split[0].trim(), split[1].toCharArray());
  }
}