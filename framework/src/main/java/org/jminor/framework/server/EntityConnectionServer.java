package org.jminor.framework.server;

import org.jminor.common.model.User;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.framework.db.RemoteEntityConnection;

import java.rmi.RemoteException;

/**
 * Specifies the methods provided by a EntityConnectionServer
 */
public interface EntityConnectionServer extends Server<RemoteEntityConnection> {

  /**
   * @param user the admin user
   * @return the admin interface for the server
   * @throws RemoteException in case of a communication exception
   * @throws ServerException.AuthenticationException if the user is not authenicated
   */
  EntityConnectionServerAdmin getServerAdmin(final User user) throws RemoteException, ServerException.AuthenticationException;
}
