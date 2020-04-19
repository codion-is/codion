/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.remote;

import org.jminor.common.i18n.Messages;
import org.jminor.common.remote.client.Clients;
import org.jminor.common.remote.server.Server;
import org.jminor.common.remote.server.Servers;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.db.EntityConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collections;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a remote entity connection.
 */
public final class RemoteEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteEntityConnectionProvider.class);

  /**
   * A key for specifying the ID of the domain required by a remote client
   */
  public static final String REMOTE_CLIENT_DOMAIN_ID = "jminor.client.domainId";

  private final String serverHostName;
  private final Integer serverPort;
  private final Integer registryPort;

  private Server<RemoteEntityConnection, Remote> server;
  private Server.ServerInfo serverInfo;
  private boolean truststoreResolved = false;

  /**
   * Instantiates a new {@link RemoteEntityConnectionProvider} using configuration values.
   * @see Server#SERVER_HOST_NAME
   * @see Server#SERVER_PORT
   * @see Server#REGISTRY_PORT
   */
  public RemoteEntityConnectionProvider() {
    this(Server.SERVER_HOST_NAME.get(), Server.SERVER_PORT.get(), Server.REGISTRY_PORT.get());
  }

  /**
   * Instantiates a new {@link RemoteEntityConnectionProvider}.
   * @param serverHostName the server host name
   * @param serverPort the server port, -1 if no specific port is required
   * @param registryPort the registry port
   */
  public RemoteEntityConnectionProvider(final String serverHostName, final Integer serverPort, final Integer registryPort) {
    this.serverHostName = requireNonNull(serverHostName, "serverHostName");
    this.serverPort = serverPort == null ? -1 : serverPort;
    this.registryPort = requireNonNull(registryPort, "registryPort");
  }

  @Override
  public String getConnectionType() {
    return CONNECTION_TYPE_REMOTE;
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

  /**
   * @return the name of the host of the server providing the connection
   */
  public String getServerHostName() {
    return serverHostName;
  }

  /**
   * @return the info on the server last connected to
   */
  public Server.ServerInfo getServerInfo() {
    return serverInfo;
  }

  @Override
  protected EntityConnection connect() {
    if (!truststoreResolved) {
      Servers.resolveTrustStoreFromClasspath(getClientTypeId());
      truststoreResolved = true;
    }
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
              new Class[] {EntityConnection.class}, new RemoteEntityConnectionHandler(
                      getServer().connect(Clients.connectionRequest(getUser(), getClientId(), getClientTypeId(),
                              getClientVersion(), Collections.singletonMap(REMOTE_CLIENT_DOMAIN_ID,
                                      getDomainId(getDomainClassName()))))));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void disconnect(final EntityConnection connection) {
    try {
      server.disconnect(getClientId());
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
  private Server<RemoteEntityConnection, Remote> getServer() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null) {
        this.server.getServerLoad();
      }//just to check the connection
    }
    catch (final RemoteException e) {
      LOG.info("{} was unreachable, {} - {} reconnecting...", new Object[] {serverInfo.getServerName(), getUser(), getClientId()});
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      LOG.info("ClientID: {}, {} connected to server: {}", new Object[] {getUser(), getClientId(), serverInfo.getServerName()});
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    this.server = Servers.getServer(getServerHostName(), Server.SERVER_NAME_PREFIX.get(), registryPort, serverPort);
    this.serverInfo = this.server.getServerInfo();
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
      catch (final InvocationTargetException e) {
        LOG.error(e.getMessage(), e);
        throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
      }
      catch (final Exception e) {
        LOG.error(e.getMessage(), e);
        throw e;
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
