/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.user;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

final class DefaultUser implements User, Serializable {

  private static final long serialVersionUID = 1;

  private String username;
  private char[] password;

  DefaultUser(String username, char[] password) {
    if (requireNonNull(username, "username").isEmpty()) {
      throw new IllegalArgumentException("Username must be non-empty");
    }
    this.username = username;
    setPassword(password);
  }

  @Override
  public String username() {
    return username;
  }

  @Override
  public char[] password() {
    return Arrays.copyOf(password, password.length);
  }

  @Override
  public User clearPassword() {
    Arrays.fill(password, (char) 0);
    setPassword(null);
    return this;
  }

  @Override
  public User copy() {
    return new DefaultUser(username, password());
  }

  @Override
  public String toString() {
    return "User: " + username;
  }

  /**
   * User objects are equal if the usernames are equal, ignoring case
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof User && ((User) obj).username().equalsIgnoreCase(username);
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(username);
    stream.writeObject(password);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    this.username = (String) stream.readObject();
    setPassword((char[]) stream.readObject());
  }

  private void setPassword(char[] password) {
    this.password = password == null ? new char[0] : Arrays.copyOf(password, password.length);
  }
}
