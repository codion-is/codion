/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.rmi;

import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.ServerInformation;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a remote entity connection.
 * @see RemoteEntityConnectionProvider#builder()
 */
final class DefaultRemoteEntityConnectionProvider extends AbstractEntityConnectionProvider
        implements RemoteEntityConnectionProvider{

  private static final Logger LOG = LoggerFactory.getLogger(RemoteEntityConnectionProvider.class);

  private Server<RemoteEntityConnection, ServerAdmin> server;
  private ServerInformation serverInformation;
  private boolean truststoreResolved = false;

  private final String serverHostName;
  private final int serverPort;
  private final int registryPort;

  DefaultRemoteEntityConnectionProvider(DefaultRemoteEntityConnectionProviderBuilder builder) {
    super(builder);
    this.serverHostName = requireNonNull(builder.serverHostName, "serverHostName");
    this.serverPort = builder.serverPort;
    this.registryPort = builder.registryPort;
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
    return serverInformation.getServerName() + "@" + serverHostName;
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
  public ServerInformation getServerInformation() {
    return serverInformation;
  }

  @Override
  protected EntityConnection connect() {
    if (!truststoreResolved) {
      Clients.resolveTrustStore();
      truststoreResolved = true;
    }
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
              new Class[] {EntityConnection.class}, new RemoteEntityConnectionHandler(
                      getServer().connect(ConnectionRequest.builder()
                              .user(getUser())
                              .clientId(getClientId())
                              .clientTypeId(getClientTypeId())
                              .clientVersion(getClientVersion())
                              .parameter(REMOTE_CLIENT_DOMAIN_TYPE, getDomainTypeName(getDomainClassName()))
                              .build())));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(EntityConnection connection) {
    try {
      server.disconnect(getClientId());
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return connects to and returns the Server instance
   * @throws java.rmi.NotBoundException if no server is reachable or if the servers found are not using the specified port
   * @throws java.rmi.RemoteException in case of remote exceptions
   */
  private Server<RemoteEntityConnection, ServerAdmin> getServer() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null) {
        this.server.getServerLoad();
      }//just to check the connection
    }
    catch (RemoteException e) {
      LOG.info("{} was unreachable, {} - {} reconnecting...", serverInformation.getServerName(), getUser(), getClientId());
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      LOG.info("ClientID: {}, {} connected to server: {}", getUser(), getClientId(), serverInformation.getServerName());
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    this.server = Server.Locator.locator().getServer(serverHostName, ServerConfiguration.SERVER_NAME_PREFIX.get(), registryPort, serverPort);
    this.serverInformation = this.server.getServerInformation();
  }

  private static final class RemoteEntityConnectionHandler implements InvocationHandler {

    private final Map<Method, Method> methodCache = new HashMap<>();
    private final RemoteEntityConnection remoteConnection;

    private RemoteEntityConnectionHandler(RemoteEntityConnection remoteConnection) {
      this.remoteConnection = remoteConnection;
    }

    @Override
    public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Exception {
      String methodName = method.getName();
      if (methodName.equals(IS_CONNECTED)) {
        return isConnected();
      }

      Method remoteMethod = methodCache.computeIfAbsent(method, RemoteEntityConnectionHandler::getRemoteMethod);
      try {
        return remoteMethod.invoke(remoteConnection, args);
      }
      catch (InvocationTargetException e) {
        LOG.error(e.getMessage(), e);
        throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
      }
      catch (Exception e) {
        LOG.error(e.getMessage(), e);
        throw e;
      }
    }

    private Object isConnected() throws RemoteException {
      try {
        return remoteConnection.isConnected();
      }
      catch (NoSuchObjectException | ConnectException e) {
        return false;
      }
    }

    private static Method getRemoteMethod(Method method) {
      try {
        return RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
      }
      catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
