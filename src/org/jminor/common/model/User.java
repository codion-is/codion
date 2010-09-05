/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A class encapsulating a username and password.
 */
public final class User implements Serializable {

  private static final long serialVersionUID = 1;

  public static final String UNITTEST_USERNAME_PROPERTY = "jminor.unittest.username";
  public static final String UNITTEST_PASSWORD_PROPERTY = "jminor.unittest.password";

  public static final User UNIT_TEST_USER;

  private String username;
  private int hashCode;
  private String password;

  static {
    final String unitTestUserName = System.getProperty(UNITTEST_USERNAME_PROPERTY, "scott");
    final String unitTestPassword = System.getProperty(UNITTEST_PASSWORD_PROPERTY, "tiger");
    UNIT_TEST_USER = new User(unitTestUserName, unitTestPassword);
  }

  /**
   * Instantiates a new User.
   * @param username the username
   * @param password the password
   */
  public User(final String username, final String password) {
    Util.rejectNullValue(username, "username");
    this.username = username;
    this.password = password;
    this.hashCode = username.hashCode();
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

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof User && ((User) obj).username.equals(username);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return hashCode;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(username);
    stream.writeObject(password);
  }

  @SuppressWarnings({"SuspiciousMethodCalls"})
  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    this.username = (String) stream.readObject();
    this.hashCode = username.hashCode();
    this.password = (String) stream.readObject();
  }
}