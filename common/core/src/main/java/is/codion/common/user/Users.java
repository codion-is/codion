/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.user;

import static java.util.Objects.requireNonNull;

/**
 * Factory class for {@link User} instances.
 */
public final class Users {

  private Users() {}

  /**
   * Instantiates a new User with an empty password.
   * @param username the username
   * @return a new User
   */
  public static User user(final String username) {
    return user(username, null);
  }

  /**
   * Instantiates a new User.
   * @param username the username
   * @param password the password
   * @return a new User
   */
  public static User user(final String username, final char[] password) {
    return new DefaultUser(username, password);
  }

  /**
   * Parses a User from a string, containing the username and password with a ':' as delimiter, i.e. "user:pass".
   * Both username and password must be non-empty.
   * @param userPassword the username and password string
   * @return a User with the given username and password
   */
  public static User parseUser(final String userPassword) {
    final String[] split = requireNonNull(userPassword).split(":");
    if (split.length != 2) {
      throw new IllegalArgumentException("Expecting a non-empty username and password with a single ':' as delimiter");
    }
    if (split[0].isEmpty() || split[1].isEmpty()) {
      throw new IllegalArgumentException("Both username and password are required");
    }

    return new DefaultUser(split[0], split[1].toCharArray());
  }
}
