/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.ConnectionValidationException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.UUID;

/**
 * A server for serving remote interfaces
 * @param <C> the type of remote interface this server supplies to clients
 * @param <A> the type of the admin interface this server supplies
 */
public interface Server<C extends Remote, A extends ServerAdmin> extends Remote {

  /**
   * Specifies the client host connection parameter
   */
  String CLIENT_HOST_KEY = "clientHost";

  /**
   * Establishes a connection to this Server
   * @param connectionRequest the information required for establishing a connection
   * @return a remote connection instance
   * @throws RemoteException in case of a communication error
   * @throws ConnectionNotAvailableException in case the server isn't accepting more connections
   * @throws LoginException in case the login fails
   * @throws ConnectionValidationException in case connection validation fails
   */
  C connect(ConnectionRequest connectionRequest) throws RemoteException,
          ConnectionNotAvailableException, LoginException, ConnectionValidationException;

  /**
   * Returns the admin interface used to administer this server
   * @param user the admin user credentials
   * @return the admin interface
   * @throws RemoteException in case of a communication error
   * @throws ServerAuthenticationException in case authentication fails
   */
  A getServerAdmin(User user) throws RemoteException, ServerAuthenticationException;

  /**
   * Disconnects the connection identified by the given key.
   * @param clientId the UUID identifying the client that should be disconnected
   * @throws RemoteException in case of a communication error
   */
  void disconnect(UUID clientId) throws RemoteException;

  /**
   * @return static information about this server
   * @throws RemoteException in case of an exception
   */
  ServerInformation getServerInformation() throws RemoteException;

  /**
   * @return the server load as number of service requests per second
   * @throws RemoteException in case of an exception
   */
  int getServerLoad() throws RemoteException;

  /**
   * @return true if there are connections available
   * @throws RemoteException in case of an exception
   */
  boolean connectionsAvailable() throws RemoteException;

  /**
   * Locates {@link Server}s by name on a registry.
   */
  interface Locator {

    /**
     * Initializes a Registry if one is not running
     * @param port the port on which to look for (or create) a registry
     * @return the Registry
     * @throws java.rmi.RemoteException in case of an exception
     */
    Registry initializeRegistry(int port) throws RemoteException;

    /**
     * Retrieves a Server from a registry running on the given host, using the
     * given server name prefix as a condition. Returns the first server satisfying the condition.
     * @param serverHostName the name of the host
     * @param serverNamePrefix the server name prefix, an empty string results in all servers being returned
     * @param registryPort the port on which to lookup the registry
     * @param requestedServerPort the required server port, -1 for any port
     * @param <T> the Remote object type served by the server
     * @param <A> the server admin type supplied by the server
     * @return the servers having a name with the given prefix
     * @throws RemoteException in case of a remote exception
     * @throws NotBoundException in case no such server is found
     */
    <T extends Remote, A extends ServerAdmin> Server<T, A> getServer(String serverHostName,
                                                                     String serverNamePrefix,
                                                                     int registryPort,
                                                                     int requestedServerPort)
            throws RemoteException, NotBoundException;

    /**
     * Returns a {@link Locator} instance.
     * @return a {@link Locator} instance.
     */
    static Locator locator() {
      return new DefaultServerLocator();
    }
  }
}
