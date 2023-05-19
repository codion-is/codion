/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.rmi;

import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.common.rmi.server.ServerInformation;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entities;

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
        implements RemoteEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteEntityConnectionProvider.class);

  private static final String IS_CONNECTED = "isConnected";
  private static final String ENTITIES = "entities";

  private Server<RemoteEntityConnection, ServerAdmin> server;
  private ServerInformation serverInformation;
  private boolean truststoreResolved = false;

  private final String serverHostName;
  private final int serverPort;
  private final int registryPort;
  private final String serverNamePrefix;

  DefaultRemoteEntityConnectionProvider(DefaultRemoteEntityConnectionProviderBuilder builder) {
    super(builder);
    this.serverHostName = requireNonNull(builder.serverHostName, "serverHostName");
    this.serverPort = builder.serverPort;
    this.registryPort = builder.registryPort;
    this.serverNamePrefix = builder.serverNamePrefix;
  }

  @Override
  public String connectionType() {
    return CONNECTION_TYPE_REMOTE;
  }

  /**
   * @return a string describing the server connection
   */
  @Override
  public String description() {
    return serverInformation.serverName() + "@" + serverHostName;
  }

  /**
   * @return the name of the host of the server providing the connection
   */
  @Override
  public String serverHostName() {
    return serverHostName;
  }

  @Override
  protected EntityConnection connect() {
    if (!truststoreResolved) {
      Clients.resolveTrustStore();
      truststoreResolved = true;
    }
    try {
      LOG.debug("Initializing connection for {}", user());
      return (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
              new Class[] {EntityConnection.class}, new RemoteEntityConnectionHandler(
                      server().connect(ConnectionRequest.builder()
                              .user(user())
                              .clientId(clientId())
                              .clientTypeId(clientTypeId())
                              .clientVersion(clientVersion())
                              .parameter(REMOTE_CLIENT_DOMAIN_TYPE, domainTypeName(domainClassName()))
                              .build())));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(EntityConnection connection) {
    try {
      server.disconnect(clientId());
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
  private Server<RemoteEntityConnection, ServerAdmin> server() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null) {
        this.server.serverLoad();
      }//just to check the connection
    }
    catch (RemoteException e) {
      LOG.info("{} was unreachable, {} - {} reconnecting...", serverInformation.serverName(), user(), clientId());
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      LOG.info("ClientID: {}, {} connected to server: {}", user(), clientId(), serverInformation.serverName());
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    this.server = Server.Locator.builder()
            .serverHostName(serverHostName)
            .serverNamePrefix(serverNamePrefix)
            .registryPort(registryPort)
            .serverPort(serverPort)
            .build()
            .locateServer();
    this.serverInformation = this.server.serverInformation();
  }

  private static final class RemoteEntityConnectionHandler implements InvocationHandler {

    private final Map<Method, Method> methodCache = new HashMap<>();
    private final RemoteEntityConnection remoteConnection;

    private Entities entities;

    private RemoteEntityConnectionHandler(RemoteEntityConnection remoteConnection) {
      this.remoteConnection = remoteConnection;
    }

    @Override
    public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Exception {
      String methodName = method.getName();
      if (methodName.equals(IS_CONNECTED)) {
        return isConnected();
      }
      if (methodName.equals(ENTITIES)) {
        return entities();
      }

      Method remoteMethod = methodCache.computeIfAbsent(method, RemoteEntityConnectionHandler::remoteMethod);
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

    private Entities entities() throws RemoteException {
      if (entities == null) {
        entities = remoteConnection.entities();
      }

      return entities;
    }

    private static Method remoteMethod(Method method) {
      try {
        return RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
      }
      catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
