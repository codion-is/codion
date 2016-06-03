package org.jminor.framework.server;

import org.jminor.common.model.User;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.framework.db.RemoteEntityConnection;

import java.rmi.RemoteException;

public interface EntityServer extends Server<RemoteEntityConnection> {

  EntityConnectionServerAdmin getServerAdmin(final User user) throws RemoteException, ServerException.AuthenticationException;
}
