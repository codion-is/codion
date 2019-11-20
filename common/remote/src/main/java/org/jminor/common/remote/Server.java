/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.jminor.common.Configuration;
import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.remote.exception.ConnectionNotAvailableException;
import org.jminor.common.remote.exception.ConnectionValidationException;
import org.jminor.common.remote.exception.LoginException;
import org.jminor.common.remote.exception.ServerAuthenticationException;
import org.jminor.common.value.PropertyValue;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * A server for serving remote interfaces
 * @param <T> the type of remote interface this server supplies to clients
 * @param <A> the type of the admin interface this server supplies
 */
public interface Server<T extends Remote, A extends Remote> extends Remote {

  int DEFAULT_SERVER_CONNECTION_TIMEOUT = 120000;

  /**
   * Specifies the client host connection parameter
   */
  String CLIENT_HOST_KEY = "clientHost";

  /**
   * The system property key for specifying a ssl keystore
   */
  String JAVAX_NET_KEYSTORE = "javax.net.ssl.keyStore";

  /**
   * The system property key for specifying a ssl keystore password
   */
  String JAVAX_NET_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

  /**
   * The system property key for specifying a ssl truststore
   */
  String JAVAX_NET_TRUSTSTORE = "javax.net.ssl.trustStore";

  /**
   * The system property key for specifying a ssl truststore password
   */
  String JAVAX_NET_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

  /**
   * Localhost
   */
  String LOCALHOST = "localhost";

  /**
   * The host on which to locate the server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  PropertyValue<String> SERVER_HOST_NAME = Configuration.stringValue("jminor.server.hostname", LOCALHOST);

  /**
   * Specifies the rmi server hostname<br>
   * Note that this is the standard Java property 'java.rmi.server.hostname'<br>
   * Value type: String<br>
   * Default value: localhost
   */
  PropertyValue<String> RMI_SERVER_HOSTNAME = Configuration.stringValue("java.rmi.server.hostname", LOCALHOST);

  /**
   * Specifies the prefix used when exporting/looking up the JMinor server<br>
   * Value type: String<br>
   * Default value: JMinor Server
   */
  PropertyValue<String> SERVER_NAME_PREFIX = Configuration.stringValue("jminor.server.namePrefix", "JMinor Server");

  /**
   * The port on which the server is made available to clients.<br>
   * If specified on the client side, the client will only connect to a server running on this port,
   * use -1 or no value if the client should connect to any available server<br>
   * Value type: Integer<br>
   * Default value: none
   */
  PropertyValue<Integer> SERVER_PORT = Configuration.integerValue("jminor.server.port", null);

  /**
   * The port on which to locate the server registry<br>
   * Value type: Integer<br>
   * Default value: Registry.REGISTRY_PORT (1099)
   */
  PropertyValue<Integer> REGISTRY_PORT = Configuration.integerValue("jminor.server.registryPort", Registry.REGISTRY_PORT);

  /**
   * The rmi ssl keystore to use<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> KEYSTORE = Configuration.stringValue(JAVAX_NET_KEYSTORE, null);

  /**
   * The rmi ssl keystore password to use<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> KEYSTORE_PASSWORD = Configuration.stringValue(JAVAX_NET_KEYSTORE_PASSWORD, null);

  /**
   * The rmi ssl truststore to use<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> TRUSTSTORE = Configuration.stringValue(JAVAX_NET_TRUSTSTORE, null);

  /**
   * The rmi ssl truststore password to use<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> TRUSTSTORE_PASSWORD = Configuration.stringValue(JAVAX_NET_TRUSTSTORE_PASSWORD, null);

  /**
   * The port on which the server should export the remote admin interface<br>
   * Value type: Integer<br>
   * Default value: none
   */
  PropertyValue<Integer> SERVER_ADMIN_PORT = Configuration.integerValue("jminor.server.admin.port", null);

  /**
   * Specifies a username:password combination representing the server admin user<br>
   * Example: scott:tiger
   */
  PropertyValue<String> SERVER_ADMIN_USER = Configuration.stringValue("jminor.server.admin.user", null);

  /**
   * Specifies whether the server should establish connections using a secure sockets layer, true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> SERVER_CONNECTION_SSL_ENABLED = Configuration.booleanValue("jminor.server.connection.sslEnabled", true);

  /**
   * Specifies a specific connection timeout for different client types
   * Value type: Integer<br>
   * Default value: 120000ms (2 minutes)
   */
  PropertyValue<Integer> SERVER_CONNECTION_TIMEOUT = Configuration.integerValue("jminor.server.connectionTimeout", DEFAULT_SERVER_CONNECTION_TIMEOUT);

  /**
   * A comma separated list of auxiliary servers to run alongside this Server<br>
   * Those must extend {@link AuxiliaryServer}.<br>
   * Value type: String<br>
   * Default value: none
   * @see AuxiliaryServer
   */
  PropertyValue<String> AUXILIARY_SERVER_CLASS_NAMES = Configuration.stringValue("jminor.server.auxiliaryServerClassNames", null);

  /**
   * Establishes a connection to this Server
   * @param connectionRequest the information required for establishing a connection
   * @return a remote connection instance
   * @throws RemoteException in case of a communitation error
   * @throws ConnectionNotAvailableException in case the server isn't accepting more connections
   * @throws LoginException in case the login fails
   * @throws ConnectionValidationException in case connection validation fails
   */
  T connect(final ConnectionRequest connectionRequest) throws RemoteException,
          ConnectionNotAvailableException, LoginException, ConnectionValidationException;

  /**
   * Returns the admin intarface used to administer this server
   * @param user the admin user credentials
   * @return the admin interface
   * @throws RemoteException in case of a communitation error
   * @throws ServerAuthenticationException in case authentication fails
   */
  A getServerAdmin(final User user) throws RemoteException, ServerAuthenticationException;

  /**
   * Disconnects the connection identified by the given key.
   * @param clientId the UUID identifying the client that should be disconnected
   * @throws RemoteException in case of a communication error
   */
  void disconnect(final UUID clientId) throws RemoteException;

  /**
   * @return static information about this server
   * @throws RemoteException in case of an exception
   */
  ServerInfo getServerInfo() throws RemoteException;

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
   * Encapsulates static server information
   */
  interface ServerInfo {
    /**
     * @return the server name
     */
    String getServerName();

    /**
     * @return a unique identifier for this server
     */
    UUID getServerId();

    /**
     * @return the server Version
     */
    Version getServerVersion();

    /**
     * @return the server port
     */
    int getServerPort();

    /**
     * @return the time of server startup
     */
    ZonedDateTime getStartTime();

    /**
     * @return the server locale
     */
    Locale getLocale();

    /**
     * @return the server time zone
     */
    ZoneId getTimeZone();
  }

  /**
   * Auxiliary servers to be run in conjunction with a Server must implement this interface,
   * as well as provide a constructor with the following signature: (Server) for the associated server
   */
  interface AuxiliaryServer {

    /**
     * Starts the server, returns when the server has completed the startup
     * @throws Exception in case of an exception
     */
    void startServer() throws Exception;

    /**
     * Stops the server, returns when the server has completed shutdown
     * @throws Exception in case of an exception
     */
    void stopServer() throws Exception;
  }
}
