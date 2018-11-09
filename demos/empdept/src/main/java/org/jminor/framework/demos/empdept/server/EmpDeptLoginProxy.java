/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.User;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.RemoteClient;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.Servers;

import java.util.HashMap;
import java.util.Map;

public final class EmpDeptLoginProxy implements LoginProxy {

  private final Map<String, String> users = new HashMap<>();
  private final User databaseUser = new User("scott", "tiger".toCharArray());

  public EmpDeptLoginProxy() {
    users.put("scott", "tiger");
    users.put("john", "hello");
    users.put("helen", "juno");
  }

  @Override
  public String getClientTypeId() {
    return "org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel";
  }

  @Override
  public RemoteClient doLogin(final RemoteClient remoteClient) throws ServerException.LoginException {
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

  private void authenticateUser(final User user) throws ServerException.LoginException {
    final String password = users.get(user.getUsername());
    if (password == null || !password.equals(user.getPassword())) {
      throw ServerException.loginException("Wrong username or password");
    }
  }
}
