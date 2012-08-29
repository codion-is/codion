/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.provider;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.RemoteServer;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.provider.AbstractEntityConnectionProvider;
import org.jminor.framework.server.RemoteEntityConnection;

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
  private static final String IS_CONNECTED = "isConnected";

  private final String serverHostName;
  private final UUID clientID;
  private final String clientTypeID;
  private RemoteServer server;
  private String serverName;

  /**
   * Instantiates a new RemoteEntityConnectionProvider.
   * @param user the user to base the db provider on
   * @param clientID the client ID
   * @param clientTypeID a string identifying the client type
   */
  public RemoteEntityConnectionProvider(final User user, final UUID clientID, final String clientTypeID) {
    super(user);
    Util.rejectNullValue(user, "user");
    this.serverHostName = Configuration.getStringValue(Configuration.SERVER_HOST_NAME);
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
    Util.resolveTrustStore(clientTypeID);
  }

  /**
   * @return if no server connection has been established, 'serverHostName -
   */
  @Override
  public String getDescription() {
    try {
      if (!isConnectionValid()) {
        return serverHostName + " - " + Messages.get(Messages.NOT_CONNECTED);
      }

      return server.getServerName() + "@" + serverHostName;
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getHostName() {
    return serverHostName;
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    try {
      if (getConnectionInternal() != null && isConnectionValid()) {
        server.disconnect(clientID);
      }
      setConnection(null);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
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
      final RemoteEntityConnection remote = (RemoteEntityConnection) getRemoteEntityServer().connect(getUser(), clientID, clientTypeID);

      return Util.initializeProxy(EntityConnection.class, new RemoteEntityConnectionHandler(remote));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected boolean isConnectionValid() {
    if (getConnectionInternal() == null) {
      return false;
    }
    try {
      return getConnectionInternal().isConnected();
    }
    catch (Exception e) {
      LOG.debug("Remote connection invalid", e);
      return false;
    }
  }

  /**
   * @return the RemoteEntityServer instance, with an established connection
   * @throws java.rmi.NotBoundException if no server is reachable or if the servers found are not using the specified port
   * @throws java.rmi.RemoteException in case of remote exceptions
   */
  private RemoteServer getRemoteEntityServer() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null) {
        this.server.getServerPort();
      }//just to check the connection
    }
    catch (RemoteException e) {
      LOG.info("{} was unreachable, {} - {} reconnecting...", new Object[] {serverName, getUser(), clientID});
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      LOG.info("ClientID: {}, {} connected to server: {}", new Object[] {getUser(), clientID, serverName});
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    Integer serverPort = (Integer) Configuration.getValue(Configuration.SERVER_PORT);
    if (serverPort == null) {
      serverPort = -1;
    }
    final int registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT_NUMBER);
    this.server = ServerUtil.getServer(serverHostName,
            Configuration.getStringValue(Configuration.SERVER_NAME_PREFIX), registryPort, serverPort);
    this.serverName = this.server.getServerName();
  }

  private static final class RemoteEntityConnectionHandler implements InvocationHandler {
    private final RemoteEntityConnection remote;

    private RemoteEntityConnectionHandler(final RemoteEntityConnection remote) {
      this.remote = remote;
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      if (method.getName().equals(IS_CONNECTED)) {
        return isConnected();
      }
      final Method remoteMethod = RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
      try {
        return remoteMethod.invoke(remote, args);
      }
      catch (Exception e) {
        throw Util.unwrapAndLog(e, InvocationTargetException.class, null);
      }
    }

    private Object isConnected() throws RemoteException {
      try {
        return remote.isConnected();
      }
      catch (NoSuchObjectException e) {
        return false;
      }
    }
  }
}
