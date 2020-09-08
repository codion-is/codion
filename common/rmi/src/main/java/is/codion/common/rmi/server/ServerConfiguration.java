/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.Configuration;
import is.codion.common.Text;
import is.codion.common.value.PropertyValue;

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

  int DEFAULT_SERVER_CONNECTION_TIMEOUT = 120000;

  /**
   * The system property key for specifying a ssl keystore
   */
  String JAVAX_NET_KEYSTORE = "javax.net.ssl.keyStore";

  /**
   * The system property key for specifying a ssl keystore password
   */
  String JAVAX_NET_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

  /**
   * Localhost
   */
  String LOCALHOST = "localhost";

  /**
   * Specifies the rmi server hostname<br>
   * Note that this is the standard Java property 'java.rmi.server.hostname'<br>
   * Value type: String<br>
   * Default value: localhost
   */
  PropertyValue<String> RMI_SERVER_HOSTNAME = Configuration.stringValue("java.rmi.server.hostname", LOCALHOST);

  /**
   * Specifies the prefix used when exporting/looking up the Codion server<br>
   * Value type: String<br>
   * Default value: Codion Server
   */
  PropertyValue<String> SERVER_NAME_PREFIX = Configuration.stringValue("codion.server.namePrefix", "Codion Server");

  /**
   * The port on which the server is made available to clients.<br>
   * If specified on the client side, the client will only connect to a server running on this port,
   * use -1 or no value if the client should connect to any available server<br>
   * Value type: Integer<br>
   * Default value: none
   */
  PropertyValue<Integer> SERVER_PORT = Configuration.integerValue("codion.server.port", null);

  /**
   * The port on which to locate the server registry<br>
   * Value type: Integer<br>
   * Default value: Registry.REGISTRY_PORT (1099)
   */
  PropertyValue<Integer> REGISTRY_PORT = Configuration.integerValue("codion.server.registryPort", Registry.REGISTRY_PORT);

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
   * The port on which the server should export the remote admin interface<br>
   * Value type: Integer<br>
   * Default value: none
   */
  PropertyValue<Integer> SERVER_ADMIN_PORT = Configuration.integerValue("codion.server.admin.port", null);

  /**
   * Specifies a username:password combination representing the server admin user<br>
   * Example: scott:tiger
   */
  PropertyValue<String> SERVER_ADMIN_USER = Configuration.stringValue("codion.server.admin.user", null);

  /**
   * Specifies whether the server should establish connections using a secure sockets layer, true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> SERVER_CONNECTION_SSL_ENABLED = Configuration.booleanValue("codion.server.connection.sslEnabled", true);

  /**
   * Specifies the default client connection inactivity timeout in milliseconds.
   * Value type: Integer<br>
   * Default value: 120000ms (2 minutes)
   */
  PropertyValue<Integer> SERVER_CONNECTION_TIMEOUT = Configuration.integerValue("codion.server.connectionTimeout", DEFAULT_SERVER_CONNECTION_TIMEOUT);

  /**
   * A comma separated list of auxiliary server factories, providing servers to run alongside this Server<br>
   * Those must extend {@link AuxiliaryServerFactory}.<br>
   * Value type: String<br>
   * Default value: none
   * @see AuxiliaryServer
   */
  PropertyValue<String> AUXILIARY_SERVER_FACTORY_CLASS_NAMES = Configuration.stringValue("codion.server.auxiliaryServerFactoryClassNames", null);

  /**
   * The serialization whitelist file to use if any
   */
  PropertyValue<String> SERIALIZATION_FILTER_WHITELIST = Configuration.stringValue("codion.server.serializationFilterWhitelist", null);

  /**
   * If true then the serialization whitelist specified by {@link #SERIALIZATION_FILTER_WHITELIST} is populated
   * with the names of all deserialized classes on server shutdown. Note this overwrites the file if it already exists.
   */
  PropertyValue<Boolean> SERIALIZATION_FILTER_DRYRUN = Configuration.booleanValue("codion.server.serializationFilterDryRun", false);

  /**
   * Specifies a comma separated list of LoginProxy class names, which should be initialized on server startup,
   * these classes must be available on the server classpath and contain a parameterless constructor
   * @see is.codion.common.rmi.server.LoginProxy
   */
  PropertyValue<String> SERVER_LOGIN_PROXY_CLASSES = Configuration.stringValue("codion.server.loginProxyClasses", null);

  /**
   * @return the server name
   * @see #setServerNameProvider(Supplier)
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
   * @return the class names of auxiliary server factories, providing the servers to run alongside this server
   */
  Collection<String> getAuxiliaryServerFactoryClassNames();

  /**
   * @return true if ssl is enabled
   */
  Boolean getSslEnabled();

  /**
   * @return the rmi client socket factory to use, null for default
   */
  RMIClientSocketFactory getRmiClientSocketFactory();

  /**
   * @return the rmi server socket factory to use, null for default
   */
  RMIServerSocketFactory getRmiServerSocketFactory();

  /**
   * @return the serialization whitelist to use, if any
   */
  String getSerializationFilterWhitelist();

  /**
   * @return true if a serialization filter dry run should be active
   */
  Boolean getSerializationFilterDryRun();

  /**
   * @param serverNameProvider the server name provider
   */
  void setServerNameProvider(Supplier<String> serverNameProvider);

  /**
   * @param serverName the server name
   */
  void setServerName(String serverName);

  /**
   * @param sharedLoginProxyClassNames the shared login proxy classnames
   */
  void setSharedLoginProxyClassNames(Collection<String> sharedLoginProxyClassNames);

  /**
   * @param loginProxyClassNames the login proxy classes to initialize on startup
   */
  void setLoginProxyClassNames(Collection<String> loginProxyClassNames);

  /**
   * @param auxiliaryServerFactoryClassNames the class names of auxiliary server factories,
   * providing the servers to run alongside this server
   */
  void setAuxiliaryServerFactoryClassNames(Collection<String> auxiliaryServerFactoryClassNames);

  /**
   * When set to true this also sets the rmi client/server socket factories.
   * @param sslEnabled if true then ssl is enabled
   * @see #setRmiClientSocketFactory(RMIClientSocketFactory)
   * @see #setRmiServerSocketFactory(RMIServerSocketFactory)
   */
  void setSslEnabled(Boolean sslEnabled);

  /**
   * @param rmiClientSocketFactory the rmi client socket factory to use
   */
  void setRmiClientSocketFactory(RMIClientSocketFactory rmiClientSocketFactory);

  /**
   * @param rmiServerSocketFactory the rmi server socket factory to use
   */
  void setRmiServerSocketFactory(RMIServerSocketFactory rmiServerSocketFactory);

  /**
   * @param serializationFilterWhitelist the serialization whitelist
   */
  void setSerializationFilterWhitelist(String serializationFilterWhitelist);

  /**
   * @param serializationFilterDryRun true if serialization filter dry run is active
   */
  void setSerializationFilterDryRun(Boolean serializationFilterDryRun);

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
  static ServerConfiguration fromSystemProperties() {
    final DefaultServerConfiguration configuration =
            new DefaultServerConfiguration(requireNonNull(SERVER_PORT.get(), SERVER_PORT.getPropertyName()));
    configuration.setAuxiliaryServerFactoryClassNames(Text.parseCommaSeparatedValues(ServerConfiguration.AUXILIARY_SERVER_FACTORY_CLASS_NAMES.get()));
    configuration.setSslEnabled(ServerConfiguration.SERVER_CONNECTION_SSL_ENABLED.get());
    configuration.setLoginProxyClassNames(Text.parseCommaSeparatedValues(SERVER_LOGIN_PROXY_CLASSES.get()));
    if (SERIALIZATION_FILTER_WHITELIST.get() != null) {
      configuration.setSerializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN.get());
    }
    if (SERIALIZATION_FILTER_DRYRUN.get() != null) {
      configuration.setSerializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN.get());
    }

    return configuration;
  }
}
