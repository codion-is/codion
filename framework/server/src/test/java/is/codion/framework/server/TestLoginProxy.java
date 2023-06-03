/*
 * Copyright (c) 2011 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.rmi.server.LoginProxy;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static is.codion.common.rmi.server.RemoteClient.remoteClient;

public final class TestLoginProxy implements LoginProxy {

  private final Map<String, String> users = new HashMap<>();
  private final User databaseUser = User.parse("scott:tiger");

  public TestLoginProxy() {
    users.put("scott", "tiger");
    users.put("john", "hello");
    users.put("helen", "juno");
  }

  @Override
  public String clientTypeId() {
    return "TestLoginProxy";
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
