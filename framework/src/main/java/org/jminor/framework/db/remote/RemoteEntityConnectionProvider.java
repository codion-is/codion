/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.remote;

import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientUtil;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerInfo;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.RemoteEntityConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * A class responsible for managing a remote entity connection.
 */
public final class RemoteEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteEntityConnectionProvider.class);

  private final String serverHostName;
  private final UUID clientID;
  private final String clientTypeID;
  private final Version clientVersion;
  private Server<RemoteEntityConnection> server;
  private ServerInfo serverInfo;

  /**
   * Instantiates a new RemoteEntityConnectionProvider.
   * @param serverHostName the server host name
   * @param user the user to use when initializing connections
   * @param clientID a UUID identifying the client
   * @param clientTypeID a string identifying the client type
   */
  public RemoteEntityConnectionProvider(final String serverHostName, final User user, final UUID clientID,
                                        final String clientTypeID) {
    this(serverHostName, user, clientID, clientTypeID, null);
  }

  /**
   * Instantiates a new RemoteEntityConnectionProvider.
   * @param serverHostName the server host name
   * @param user the user to use when initializing connections
   * @param clientID a UUID identifying the client
   * @param clientTypeID a string identifying the client type
   * @param clientVersion the client version
   */
  public RemoteEntityConnectionProvider(final String serverHostName, final User user, final UUID clientID,
                                        final String clientTypeID, final Version clientVersion) {
    this(serverHostName, user, clientID, clientTypeID, clientVersion, true);
  }

  /**
   * Instantiates a new RemoteEntityConnectionProvider.
   * @param serverHostName the server host name
   * @param user the user to use when initializing connections
   * @param clientID a UUID identifying the client
   * @param clientTypeID a string identifying the client type
   * @param clientVersion the client version, if any
   * @param scheduleValidityCheck if true then a periodic validity check is performed on the connection
   */
  public RemoteEntityConnectionProvider(final String serverHostName, final User user, final UUID clientID,
                                        final String clientTypeID, final Version clientVersion,
                                        final boolean scheduleValidityCheck) {
    super(user, scheduleValidityCheck);
    this.serverHostName = Util.rejectNullValue(serverHostName, "serverHostName");
    this.clientID = Util.rejectNullValue(clientID, "clientID");
    this.clientTypeID = Util.rejectNullValue(clientTypeID, "clientTypeID");
    this.clientVersion = clientVersion;
    Util.resolveTrustStoreFromClasspath(clientTypeID);
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnection.Type getConnectionType() {
    return EntityConnection.Type.REMOTE;
  }

  /**
   * @return a string describing the server connection
   */
  @Override
  public String getDescription() {
    if (!isConnectionValid()) {
      return serverHostName + " - " + Messages.get(Messages.NOT_CONNECTED);
    }

    return serverInfo.getServerName() + "@" + serverHostName;
  }

  /** {@inheritDoc} */
  @Override
  public String getServerHostName() {
    return serverHostName;
  }

  /**
   * @return the client ID
   */
  public UUID getClientID() {
    return clientID;
  }

  /** {@inheritDoc} */
  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      final RemoteEntityConnection remote = getServer().connect(
              ClientUtil.connectionInfo(getUser(), clientID, clientTypeID, clientVersion));

      return Util.initializeProxy(EntityConnection.class, new RemoteEntityConnectionHandler(remote));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void doDisconnect() {
    try {
      server.disconnect(clientID);
    }
    catch (final RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return connects to and returns the Server instance
   * @throws java.rmi.NotBoundException if no server is reachable or if the servers found are not using the specified port
   * @throws java.rmi.RemoteException in case of remote exceptions
   */
  private Server<RemoteEntityConnection> getServer() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null) {
        this.server.getServerLoad();
      }//just to check the connection
    }
    catch (final RemoteException e) {
      LOG.info("{} was unreachable, {} - {} reconnecting...", new Object[] {serverInfo.getServerName(), getUser(), clientID});
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      LOG.info("ClientID: {}, {} connected to server: {}", new Object[] {getUser(), clientID, serverInfo.getServerName()});
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    Integer serverPort = (Integer) Configuration.getValue(Configuration.SERVER_PORT);
    if (serverPort == null) {
      serverPort = -1;
    }
    final int registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT);
    this.server = ServerUtil.getServer(serverHostName,
            Configuration.getStringValue(Configuration.SERVER_NAME_PREFIX), registryPort, serverPort);
    this.serverInfo = this.server.getServerInfo();
    final Version serverVersion = this.serverInfo.getServerVersion();
    if (serverVersion != null) {
      Configuration.setValue(Configuration.REMOTE_SERVER_VERSION, serverVersion);
    }
    else {
      Configuration.clearValue(Configuration.REMOTE_SERVER_VERSION);
    }
  }

  private static final class RemoteEntityConnectionHandler implements InvocationHandler {
    private final RemoteEntityConnection remote;

    private RemoteEntityConnectionHandler(final RemoteEntityConnection remote) {
      this.remote = remote;
    }

    @Override
    public synchronized Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      if (methodName.equals(IS_CONNECTED)) {
        return isConnected();
      }

      final Method remoteMethod = RemoteEntityConnection.class.getMethod(methodName, method.getParameterTypes());
      try {
        return remoteMethod.invoke(remote, args);
      }
      catch (final Exception e) {
        throw Util.unwrapAndLog(e, InvocationTargetException.class, null);
      }
    }

    private Object isConnected() throws RemoteException {
      try {
        return remote.isConnected();
      }
      catch (final NoSuchObjectException e) {
        return false;
      }
    }
  }
}
