/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.rmi.server.LoginProxy;
import org.jminor.common.rmi.server.RemoteClient;
import org.jminor.common.rmi.server.exception.LoginException;
import org.jminor.common.rmi.server.exception.ServerAuthenticationException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;

import java.util.HashMap;
import java.util.Map;

public final class EmpDeptLoginProxy implements LoginProxy {

  private final Map<String, String> users = new HashMap<>();
  private final User databaseUser = Users.parseUser("scott:tiger");

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
  public RemoteClient doLogin(final RemoteClient remoteClient) throws LoginException {
    authenticateUser(remoteClient.getUser());

    return RemoteClient.remoteClient(remoteClient, databaseUser);
  }

  @Override
  public void doLogout(final RemoteClient remoteClient) {}

  @Override
  public void close() {
    users.clear();
  }

  private void authenticateUser(final User user) throws LoginException {
    final String password = users.get(user.getUsername());
    if (password == null || !password.equals(String.valueOf(user.getPassword()))) {
      throw new ServerAuthenticationException("Wrong username or password");
    }
  }
}
