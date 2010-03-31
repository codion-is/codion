/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

/**
 * A class encapsulating a username and password.
 */
public class User implements Serializable {

  private static final long serialVersionUID = 1;

  private final String username;
  private final int hashCode;
  private String password;

  public User(final String username, final String password) {
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

  @Override
  public boolean equals(final Object object) {
    return this == object || object instanceof User && ((User) object).username.equals(username);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}