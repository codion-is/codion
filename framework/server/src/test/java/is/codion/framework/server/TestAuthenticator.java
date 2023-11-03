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
 * Copyright (c) 2011 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.rmi.server.Authenticator;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static is.codion.common.rmi.server.RemoteClient.remoteClient;

public final class TestAuthenticator implements Authenticator {

  private final Map<String, String> users = new HashMap<>();
  private final User databaseUser = User.parse("scott:tiger");

  public TestAuthenticator() {
    users.put("scott", "tiger");
    users.put("john", "hello");
    users.put("helen", "juno");
  }

  @Override
  public Optional<String> clientTypeId() {
    return Optional.of("TestAuthenticator");
  }

  @Override
  public RemoteClient login(RemoteClient remoteClient) throws ServerAuthenticationException {
    authenticateUser(remoteClient.user());

    return remoteClient(remoteClient.connectionRequest(), databaseUser, remoteClient.clientHost());
  }

  @Override
  public void logout(RemoteClient remoteClient) {}

  @Override
  public void close() {
    users.clear();
  }

  private void authenticateUser(User user) throws ServerAuthenticationException {
    String password = users.get(user.username());
    if (password == null || !Arrays.equals(password.toCharArray(), user.password())) {
      throw new ServerAuthenticationException("Wrong username or password");
    }
  }
}
