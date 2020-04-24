/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.remote;

import org.jminor.common.i18n.Messages;
import org.jminor.common.rmi.client.Clients;
import org.jminor.common.rmi.client.ConnectionRequest;
import org.jminor.common.rmi.server.Server;
import org.jminor.common.rmi.server.ServerConfiguration;
import org.jminor.common.rmi.server.ServerInformation;
import org.jminor.common.rmi.server.Servers;
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
import java.util.HashMap;
import java.util.Map;

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

  private Server<RemoteEntityConnection, Remote> server;
  private ServerInformation serverInformation;
  private boolean truststoreResolved = false;

  private String serverHostName;
  private Integer serverPort;
  private Integer registryPort;

  /**
   * Instantiates a new unconfigured {@link RemoteEntityConnectionProvider}.
   */
  public RemoteEntityConnectionProvider() {}

  /**
   * Instantiates a new {@link RemoteEntityConnectionProvider}.
   * @param serverHostName the server host name
   * @param serverPort the server port, -1 if no specific port is required
   * @param registryPort the registry port
   */
  public RemoteEntityConnectionProvider(final String serverHostName, final Integer serverPort, final Integer registryPort) {
    this.serverHostName = requireNonNull(serverHostName, "serverHostName");
    this.serverPort = requireNonNull(serverPort, "serverPort");
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

    return serverInformation.getServerName() + "@" + serverHostName;
  }

  /**
   * @return the name of the host of the server providing the connection
   */
  public String getServerHostName() {
    if (serverHostName == null) {
      serverHostName = ServerConfiguration.SERVER_HOST_NAME.get();
    }

    return serverHostName;
  }

  /**
   * @return the info on the server last connected to
   */
  public ServerInformation getServerInformation() {
    return serverInformation;
  }

  @Override
  protected EntityConnection connect() {
    if (!truststoreResolved) {
      Clients.resolveTrustStoreFromClasspath(getClientTypeId());
      truststoreResolved = true;
    }
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
              new Class[] {EntityConnection.class}, new RemoteEntityConnectionHandler(
                      getServer().connect(ConnectionRequest.connectionRequest(getUser(), getClientId(), getClientTypeId(),
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
      LOG.info("{} was unreachable, {} - {} reconnecting...", new Object[] {serverInformation.getServerName(), getUser(), getClientId()});
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      LOG.info("ClientID: {}, {} connected to server: {}", new Object[] {getUser(), getClientId(), serverInformation.getServerName()});
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    this.server = Servers.getServer(getServerHostName(), ServerConfiguration.SERVER_NAME_PREFIX.get(), getRegistryPort(), getServerPort());
    this.serverInformation = this.server.getServerInformation();
  }

  private Integer getServerPort() {
    if (serverPort == null) {
      serverPort = ServerConfiguration.SERVER_PORT.get();
      if (serverPort == null) {
        serverPort = -1;
      }
    }

    return serverPort;
  }

  private Integer getRegistryPort() {
    if (registryPort == null) {
      registryPort = ServerConfiguration.REGISTRY_PORT.get();
    }

    return registryPort;
  }

  private static final class RemoteEntityConnectionHandler implements InvocationHandler {

    private final Map<Method, Method> methodCache = new HashMap<>();
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

      final Method remoteMethod = methodCache.computeIfAbsent(method, RemoteEntityConnectionHandler::getRemoteMethod);
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

    private static Method getRemoteMethod(final Method method) {
      try {
        return RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
      }
      catch (final NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
