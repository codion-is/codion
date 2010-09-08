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
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.provider.AbstractEntityDbProvider;
import org.jminor.framework.server.EntityDbRemote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * A class responsible for managing a remote db connection.
 */
public final class EntityDbRemoteProvider extends AbstractEntityDbProvider {

  private static final Logger LOG = LoggerFactory.getLogger(EntityDbRemoteProvider.class);

  private final String serverHostName;
  private final UUID clientID;
  private final String clientTypeID;
  private RemoteServer server;
  private String serverName;

  /**
   * Instantiates a new EntityDbRemoteProvider.
   * @param user the user to base the db provider on
   * @param clientID the client ID
   * @param clientTypeID a string identifying the client type
   */
  public EntityDbRemoteProvider(final User user, final UUID clientID, final String clientTypeID) {
    super(user);
    Util.rejectNullValue(user, "user");
    serverHostName = Configuration.getStringValue(Configuration.SERVER_HOST_NAME);
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
    Configuration.resolveTruststoreProperty(clientTypeID);
  }

  /** {@inheritDoc} */
  public String getDescription() {
    try {
      if (server == null) {
        return serverHostName + " - " + Messages.get(Messages.NOT_CONNECTED);
      }

      return server.getServerName() + "@" + serverHostName;
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  public void disconnect() {
    try {
      if (getEntityDbInternal() != null && isConnectionValid()) {
        server.disconnect(clientID);
      }
      setEntityDb(null);
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
  protected EntityDb connect() {
    try {
      LOG.debug("Initializing connection for " + getUser());
      final EntityDbRemote remote = (EntityDbRemote) getRemoteEntityDbServer().connect(getUser(), clientID, clientTypeID);

      return Util.initializeProxy(EntityDb.class, new EntityDbRemoteHandler(remote));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected boolean isConnectionValid() {
    if (!isConnected()) {
      return false;
    }
    try {
      //could be a call to any method, simply checking if remote connection is valid
      getEntityDbInternal().isConnected();

      return true;
    }
    catch (Exception e) {
      LOG.debug("Remote connection invalid: " + e.getMessage());
      return false;
    }
  }

  /**
   * @return the EntityDbRemoveServer instance, with an established connection
   * @throws java.rmi.NotBoundException if no server is reachable or if the servers found are not using the specified port
   * @throws java.rmi.RemoteException in case of remote exceptions
   */
  private RemoteServer getRemoteEntityDbServer() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null) {
        this.server.getServerPort();
      }//just to check the connection
    }
    catch (RemoteException e) {
      LOG.info(serverName + " was unreachable, " + getUser() + " - " + clientID + " reconnecting...");
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      LOG.debug("ClientID: " + clientID + ", user: " + getUser() + " connected to server: " + serverName);
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    final String portNumber = Configuration.getStringValue(Configuration.SERVER_PORT);
    final int port = portNumber == null || portNumber.isEmpty() ? -1 : Integer.parseInt(portNumber);
    this.server = ServerUtil.getServer(serverHostName,
            (String) Configuration.getValue(Configuration.SERVER_NAME_PREFIX), port);
    this.serverName = this.server.getServerName();
  }

  private static final class EntityDbRemoteHandler implements InvocationHandler {
    private final EntityDbRemote remote;

    private EntityDbRemoteHandler(final EntityDbRemote remote) {
      this.remote = remote;
    }

    /** {@inheritDoc} */
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final Method remoteMethod = EntityDbRemote.class.getMethod(method.getName(), method.getParameterTypes());
      try {
        return remoteMethod.invoke(remote, args);
      }
      catch (Exception e) {
        throw Util.unwrapAndLog(e, InvocationTargetException.class, null);
      }
    }
  }
}
