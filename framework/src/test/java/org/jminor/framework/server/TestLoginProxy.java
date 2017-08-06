package org.jminor.framework.server;

import org.jminor.common.User;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.RemoteClient;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.Servers;

import java.util.HashMap;
import java.util.Map;

public final class TestLoginProxy implements LoginProxy {

  private final Map<String, String> users = new HashMap<>();
  private final User databaseUser = new User("scott", "tiger");

  public TestLoginProxy() {
    users.put("scott", "tiger");
    users.put("john", "hello");
    users.put("helen", "juno");
  }

  @Override
  public String getClientTypeID() {
    return "TestLoginProxy";
  }

  @Override
  public RemoteClient doLogin(final RemoteClient remoteClient) throws ServerException.LoginException {
    authenticateUser(remoteClient.getUser());

    return Servers.remoteClient(remoteClient.getConnectionRequest(), databaseUser);
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
