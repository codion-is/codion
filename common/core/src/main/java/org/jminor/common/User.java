/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

/**
 * A class encapsulating a username and password.
 */
public final class User implements Serializable {

  private static final long serialVersionUID = 1;

  private String username;
  private String password;

  /**
   * Instantiates a new User.
   * @param username the username
   * @param password the password
   */
  public User(final String username, final String password) {
    Objects.requireNonNull(username, "username");
    this.username = username;
    this.password = password;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param password the password
   */
  public void setPassword(final String password) {
    this.password = password;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "User: " + username;
  }

  /** User objects are equal if the usernames match */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof User && ((User) obj).username.equals(username);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return username.hashCode();
  }

  /**
   * Parses a User from a string, containing the username and password with a ':' as delimiter, f.ex. "user:pass".
   * Both username and password must be non-empty.
   * @param userPassword the username and password string
   * @return a User with the given username and password
   */
  public static User parseUser(final String userPassword) {
    final String[] split = Objects.requireNonNull(userPassword).split(":");
    if (split.length < 2) {
      throw new IllegalArgumentException("Expecting a string with a single ':' as delimiter");
    }
    if (split[0].isEmpty() || split[1].isEmpty()) {
      throw new IllegalArgumentException("Both username and password are required");
    }

    return new User(split[0], split[1]);
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(username);
    stream.writeObject(password);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    this.username = (String) stream.readObject();
    this.password = (String) stream.readObject();
  }
}