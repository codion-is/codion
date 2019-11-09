/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

/**
 * A class encapsulating a username and password.
 */
public final class User implements Serializable {

  private static final long serialVersionUID = 1;

  private String username;
  private char[] password;

  /**
   * Instantiates a new User.
   * @param username the username
   * @param password the password
   */
  public User(final String username, final char[] password) {
    requireNonNull(username, "username");
    this.username = username;
    setPassword(password);
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
  public void setPassword(final char[] password) {
    this.password = password == null ? new char[0] : password;
  }

  /**
   * @return the password
   */
  public char[] getPassword() {
    return password;
  }

  /**
   * Clears the password
   */
  public void clearPassword() {
    Arrays.fill(password, (char) 0);
    setPassword(null);
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
    final String[] split = requireNonNull(userPassword).split(":");
    if (split.length < 2) {
      throw new IllegalArgumentException("Expecting a string with a single ':' as delimiter");
    }
    if (split[0].isEmpty() || split[1].isEmpty()) {
      throw new IllegalArgumentException("Both username and password are required");
    }

    return new User(split[0], split[1].toCharArray());
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(username);
    stream.writeObject(password);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    this.username = (String) stream.readObject();
    final char[] pass = (char[]) stream.readObject();
    this.password = pass == null ? new char[0] : pass;
  }
}