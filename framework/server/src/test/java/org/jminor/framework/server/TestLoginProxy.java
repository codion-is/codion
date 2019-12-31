/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.User;
import org.jminor.common.remote.LoginProxy;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.Servers;
import org.jminor.common.remote.exception.ServerAuthenticationException;

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
  public RemoteClient doLogin(final RemoteClient remoteClient) throws ServerAuthenticationException {
    authenticateUser(remoteClient.getUser());

    final RemoteClient authenticatedClient = Servers.remoteClient(remoteClient.getConnectionRequest(), databaseUser);
    authenticatedClient.setClientHost(remoteClient.getClientHost());

    return authenticatedClient;
  }

  @Override
  public void doLogout(final RemoteClient remoteClient) {}

  @Override
  public void close() {
    users.clear();
  }

  private void authenticateUser(final User user) throws ServerAuthenticationException {
    final String password = users.get(user.getUsername());
    if (password == null || !Arrays.equals(password.toCharArray(), user.getPassword())) {
      throw new ServerAuthenticationException("Wrong username or password");
    }
  }
}
