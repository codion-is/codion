/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.user;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

final class DefaultUser implements User {

  private static final long serialVersionUID = 1;

  private String username;
  private char[] password;

  DefaultUser(final String username, final char[] password) {
    requireNonNull(username, "username");
    this.username = username;
    setPassword(password);
  }

  /** {@inheritDoc} */
  @Override
  public String getUsername() {
    return username;
  }

  /** {@inheritDoc} */
  @Override
  public void setPassword(final char[] password) {
    this.password = password == null ? new char[0] : password;
  }

  /** {@inheritDoc} */
  @Override
  public char[] getPassword() {
    return password;
  }

  /** {@inheritDoc} */
  @Override
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
    return this == obj || obj instanceof User && ((User) obj).getUsername().equals(username);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return username.hashCode();
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(username);
    stream.writeObject(password);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    this.username = (String) stream.readObject();
    setPassword((char[]) stream.readObject());
  }
}
