/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.rmi.server.LoginProxy;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class TestLoginProxy implements LoginProxy {

  private final Map<String, String> users = new HashMap<>();
  private final User databaseUser = User.parseUser("scott:tiger");

  public TestLoginProxy() {
    users.put("scott", "tiger");
    users.put("john", "hello");
    users.put("helen", "juno");
  }

  @Override
  public String getClientTypeId() {
    return "TestLoginProxy";
  }

  @Override
  public RemoteClient login(RemoteClient remoteClient) throws ServerAuthenticationException {
    authenticateUser(remoteClient.getUser());

    RemoteClient authenticatedClient = RemoteClient.remoteClient(remoteClient.getConnectionRequest(), databaseUser);
    authenticatedClient.setClientHost(remoteClient.getClientHost());

    return authenticatedClient;
  }

  @Override
  public void logout(RemoteClient remoteClient) {}

  @Override
  public void close() {
    users.clear();
  }

  private void authenticateUser(User user) throws ServerAuthenticationException {
    String password = users.get(user.getUsername());
    if (password == null || !Arrays.equals(password.toCharArray(), user.getPassword())) {
      throw new ServerAuthenticationException("Wrong username or password");
    }
  }
}
