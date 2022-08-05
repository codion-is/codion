/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

/**
 * A base class for remote connections served by a {@link EntityServer}.
 * Handles logging of service calls and database connection pooling.
 */
public abstract class AbstractRemoteEntityConnection extends UnicastRemoteObject {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteEntityConnection.class);

  /**
   * A Proxy for logging method calls
   */
  protected final transient EntityConnection connectionProxy;

  /**
   * The proxy connection handler
   */
  private final transient LocalConnectionHandler connectionHandler;

  /**
   * An event notified when this connection is disconnected
   */
  private final transient Event<AbstractRemoteEntityConnection> disconnectedEvent = Event.event();

  /**
   * Instantiates a new AbstractRemoteEntityConnection and exports it on the given port number
   * @param domain the domain model
   * @param database defines the underlying database
   * @param remoteClient information about the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param clientSocketFactory the client socket factory to use, null for default
   * @param serverSocketFactory the server socket factory to use, null for default
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   */
  protected AbstractRemoteEntityConnection(Domain domain, Database database,
                                           RemoteClient remoteClient, int port,
                                           RMIClientSocketFactory clientSocketFactory,
                                           RMIServerSocketFactory serverSocketFactory)
          throws DatabaseException, RemoteException {
    super(port, clientSocketFactory, serverSocketFactory);
    this.connectionHandler = new LocalConnectionHandler(domain, remoteClient, database);
    this.connectionProxy = (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
            new Class[] {EntityConnection.class}, connectionHandler);
  }

  /**
   * @return the user this connection is using
   */
  public final User getUser() {
    return connectionHandler.getRemoteClient().user();
  }

  /**
   * @return true if this connection is connected
   */
  public final boolean isConnected() {
    synchronized (connectionProxy) {
      return connectionHandler.isConnected();
    }
  }

  /**
   * Disconnects this connection
   */
  public final void close() {
    synchronized (connectionProxy) {
      if (connectionHandler.isClosed()) {
        return;
      }
      try {
        UnicastRemoteObject.unexportObject(this, true);
      }
      catch (NoSuchObjectException e) {
        LOG.error(e.getMessage(), e);
      }
      connectionHandler.close();
    }
    disconnectedEvent.onEvent(this);
  }

  /**
   * @return the remote client using this remote connection
   */
  final RemoteClient getRemoteClient() {
    return connectionHandler.getRemoteClient();
  }

  /**
   * @return a ClientLog instance containing information about this connection's recent activity
   */
  final ClientLog getClientLog() {
    return connectionHandler.getClientLog();
  }

  /**
   * @param timeout the number of milliseconds
   * @return true if this connection has been inactive for {@code timeout} milliseconds or longer
   */
  final boolean hasBeenInactive(int timeout) {
    return System.currentTimeMillis() - connectionHandler.getLastAccessTime() > timeout;
  }

  final void setLoggingEnabled(boolean status) {
    connectionHandler.getMethodLogger().setEnabled(status);
  }

  final boolean isLoggingEnabled() {
    return connectionHandler.getMethodLogger().isEnabled();
  }

  /**
   * @return true during a remote method call
   */
  final boolean isActive() {
    return connectionHandler.isActive();
  }

  final void addDisconnectListener(EventDataListener<AbstractRemoteEntityConnection> listener) {
    disconnectedEvent.addDataListener(listener);
  }

  static int getRequestsPerSecond() {
    return LocalConnectionHandler.REQUEST_COUNTER.getRequestsPerSecond();
  }
}
