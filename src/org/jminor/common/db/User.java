/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;
import java.util.Properties;

/**
 * A class encapsulating a username, password and a set of properties
 */
public class User implements Serializable {

  private static final long serialVersionUID = 1;

  private final String username;
  private final Properties properties = new Properties();
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

  public Object setProperty(final String key, final String value) {
    return properties.setProperty(key, value);
  }

  public Object getProperty(final String key) {
    return properties.get(key);
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof User && ((User) obj).hashCode == hashCode;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public Properties getProperties() {
    return properties;
  }
}