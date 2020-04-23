/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;

import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link Server}.
 */
public interface ServerConfiguration {

  /**
   * The host on which to locate the server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  PropertyValue<String> SERVER_HOST_NAME = Configuration.stringValue("jminor.server.hostname", Server.LOCALHOST);

  /**
   * Specifies the rmi server hostname<br>
   * Note that this is the standard Java property 'java.rmi.server.hostname'<br>
   * Value type: String<br>
   * Default value: localhost
   */
  PropertyValue<String> RMI_SERVER_HOSTNAME = Configuration.stringValue("java.rmi.server.hostname", Server.LOCALHOST);

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
  PropertyValue<String> KEYSTORE = Configuration.stringValue(Server.JAVAX_NET_KEYSTORE, null);

  /**
   * The rmi ssl keystore password to use<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> KEYSTORE_PASSWORD = Configuration.stringValue(Server.JAVAX_NET_KEYSTORE_PASSWORD, null);

  /**
   * The rmi ssl truststore to use<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> TRUSTSTORE = Configuration.stringValue(Server.JAVAX_NET_TRUSTSTORE, null);

  /**
   * The rmi ssl truststore password to use<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> TRUSTSTORE_PASSWORD = Configuration.stringValue(Server.JAVAX_NET_TRUSTSTORE_PASSWORD, null);

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
   * Specifies the default client connection inactivity timeout in milliseconds.
   * Value type: Integer<br>
   * Default value: 120000ms (2 minutes)
   */
  PropertyValue<Integer> SERVER_CONNECTION_TIMEOUT = Configuration.integerValue("jminor.server.connectionTimeout", Server.DEFAULT_SERVER_CONNECTION_TIMEOUT);

  /**
   * A comma separated list of auxiliary servers to run alongside this Server<br>
   * Those must extend {@link Server.AuxiliaryServer}.<br>
   * Value type: String<br>
   * Default value: none
   * @see Server.AuxiliaryServer
   */
  PropertyValue<String> AUXILIARY_SERVER_CLASS_NAMES = Configuration.stringValue("jminor.server.auxiliaryServerClassNames", null);

  /**
   * @return the server name
   * @see #initializeServerName()
   */
  String getServerName();

  /**
   * @return the server port
   */
  int getServerPort();

  /**
   * @return the shared login proxy classnames
   */
  Collection<String> getSharedLoginProxyClassNames();

  /**
   * @return the login proxy classnames
   */
  Collection<String> getLoginProxyClassNames();

  /**
   * @return the connection validator classnames
   */
  Collection<String> getConnectionValidatorClassNames();

  /**
   * @return the rmi client socket factory to use, null for default
   */
  RMIClientSocketFactory getRmiClientSocketFactory();

  /**
   * @return the rmi server socket factory to use, null for default
   */
  RMIServerSocketFactory getRmiServerSocketFactory();

  /**
   * @param serverNameProvider the server name provider
   * @return this configuration instance
   */
  ServerConfiguration setServerNameProvider(Supplier<String> serverNameProvider);

  /**
   * @param serverName the server name
   * @return this configuration instance
   */
  ServerConfiguration setServerName(String serverName);

  /**
   * @param sharedLoginProxyClassNames the shared login proxy classnames
   * @return this configuration instance
   */
  ServerConfiguration setSharedLoginProxyClassNames(Collection<String> sharedLoginProxyClassNames);

  /**
   * @param loginProxyClassNames the login proxy classes to initialize on startup
   * @return this configuration instance
   */
  ServerConfiguration setLoginProxyClassNames(Collection<String> loginProxyClassNames);

  /**
   * @param connectionValidatorClassNames the connection validation classes to initialize on startup
   * @return this configuration instance
   */
  ServerConfiguration setConnectionValidatorClassNames(Collection<String> connectionValidatorClassNames);

  /**
   * @param rmiClientSocketFactory the rmi client socket factory to use
   * @return this configuration instance
   */
  ServerConfiguration setRmiClientSocketFactory(RMIClientSocketFactory rmiClientSocketFactory);

  /**
   * @param rmiServerSocketFactory the rmi server socket factory to use
   * @return this configuration instance
   */
  ServerConfiguration setRmiServerSocketFactory(RMIServerSocketFactory rmiServerSocketFactory);

  /**
   * @param serverPort the server port
   * @return a default server configuration
   */
  static ServerConfiguration configuration(final int serverPort) {
    return new DefaultServerConfiguration(serverPort);
  }

  /**
   * @return a configuration according to system properties.
   */
  static DefaultServerConfiguration fromSystemProperties() {
    return new DefaultServerConfiguration(requireNonNull(SERVER_PORT.get(), SERVER_PORT.getProperty()));
  }
}
