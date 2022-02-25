/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.server;

import is.codion.common.rmi.server.LoginProxy;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;

import java.util.HashMap;
import java.util.Map;

public final class EmpDeptLoginProxy implements LoginProxy {

  private final Map<String, String> users = new HashMap<>();
  private final User databaseUser = User.parseUser("scott:tiger");

  public EmpDeptLoginProxy() {
    users.put("scott", "tiger");
    users.put("john", "hello");
    users.put("helen", "juno");
  }

  @Override
  public String getClientTypeId() {
    return "is.codion.framework.demos.empdept.ui.EmpDeptAppPanel";
  }

  @Override
  public RemoteClient login(RemoteClient remoteClient) throws LoginException {
    authenticateUser(remoteClient.getUser());

    return remoteClient.withDatabaseUser(databaseUser);
  }

  @Override
  public void logout(RemoteClient remoteClient) {}

  @Override
  public void close() {
    users.clear();
  }

  private void authenticateUser(User user) throws LoginException {
    String password = users.get(user.getUsername());
    if (password == null || !password.equals(String.valueOf(user.getPassword()))) {
      throw new ServerAuthenticationException("Wrong username or password");
    }
  }
}
