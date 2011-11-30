package org.jminor.framework.demos.empdept.server;

import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.ServerException;

import java.util.HashMap;
import java.util.Map;

public final class EmpDeptLoginProxy implements LoginProxy {

  private final Map<String, String> users = new HashMap<String, String>();
  private final User databaseUser = new User("scott", "tiger");

  public EmpDeptLoginProxy() {
    users.put("scott", "tiger");
    users.put("john", "hello");
    users.put("helen", "juno");
  }

  public String getClientTypeID() {
    return "org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel";
  }

  public ClientInfo doLogin(final ClientInfo clientInfo) throws ServerException.LoginException {
    authenticateUser(clientInfo.getUser());

    return new ClientInfo(clientInfo.getClientID(), clientInfo.getClientTypeID(), clientInfo.getUser(), databaseUser);
  }

  private void authenticateUser(final User user) throws ServerException.LoginException {
    final String password = users.get(user.getUsername());
    if (password == null || !password.equals(user.getPassword())) {
      throw ServerException.loginException("Wrong username or password");
    }
  }

  public void close() {
    users.clear();
  }
}
