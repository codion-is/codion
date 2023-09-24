/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
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
