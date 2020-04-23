/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.remote.server.LoginProxy;
import org.jminor.common.remote.server.RemoteClient;
import org.jminor.common.remote.server.exception.ServerAuthenticationException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class TestLoginProxy implements LoginProxy {

  private final Map<String, String> users = new HashMap<>();
  private final User databaseUser = Users.parseUser("scott:tiger");

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

    final RemoteClient authenticatedClient = RemoteClient.remoteClient(remoteClient.getConnectionRequest(), databaseUser);
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
