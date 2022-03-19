/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.Configuration;
import is.codion.common.Text;
import is.codion.common.properties.PropertyValue;

import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Configuration values for a {@link Server}.
 */
public interface ServerConfiguration {

  int DEFAULT_SERVER_CONNECTION_TIMEOUT = 120_000;

  int DEFAULT_CONNECTION_MAINTENANCE_INTERVAL = 30_000;

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
   * Default value: -1
   */
  PropertyValue<Integer> SERVER_PORT = Configuration.integerValue("codion.server.port", -1);

  /**
   * The port on which to locate the server registry<br>
   * Value type: Integer<br>
   * Default value: Registry.REGISTRY_PORT (1099)
   */
  PropertyValue<Integer> REGISTRY_PORT = Configuration.integerValue("codion.server.registryPort", Registry.REGISTRY_PORT);

  /**
   * The rmi ssl keystore to use on the classpath, this will be resolved to a temporary file and set
   * as the javax.net.ssl.keyStore system property on server start<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> CLASSPATH_KEYSTORE = Configuration.stringValue("codion.server.classpathKeyStore");

  /**
   * The rmi ssl keystore to use<br>
   * Value type: String
   * Default value: null
   * @see #CLASSPATH_KEYSTORE
   */
  PropertyValue<String> KEYSTORE = Configuration.stringValue(JAVAX_NET_KEYSTORE);

  /**
   * The rmi ssl keystore password to use<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> KEYSTORE_PASSWORD = Configuration.stringValue(JAVAX_NET_KEYSTORE_PASSWORD);

  /**
   * The port on which the server should export the remote admin interface<br>
   * Value type: Integer<br>
   * Default value: null
   */
  PropertyValue<Integer> SERVER_ADMIN_PORT = Configuration.integerValue("codion.server.admin.port");

  /**
   * Specifies a username:password combination representing the server admin user<br>
   * Example: scott:tiger
   */
  PropertyValue<String> SERVER_ADMIN_USER = Configuration.stringValue("codion.server.admin.user");

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
  PropertyValue<String> AUXILIARY_SERVER_FACTORY_CLASS_NAMES = Configuration.stringValue("codion.server.auxiliaryServerFactoryClassNames");

  /**
   * The serialization whitelist file to use if any
   */
  PropertyValue<String> SERIALIZATION_FILTER_WHITELIST = Configuration.stringValue("codion.server.serializationFilterWhitelist");

  /**
   * If true then the serialization whitelist specified by {@link #SERIALIZATION_FILTER_WHITELIST} is populated
   * with the names of all deserialized classes on server shutdown. Note this overwrites the file if it already exists.
   */
  PropertyValue<Boolean> SERIALIZATION_FILTER_DRYRUN = Configuration.booleanValue("codion.server.serializationFilterDryRun", false);

  /**
   * Specifies the interval between server connection maintenance runs, in milliseconds.<br>
   * Value type: Integer<br>
   * Default value: 30_000ms (30 seconds)
   */
  PropertyValue<Integer> CONNECTION_MAINTENANCE_INTERVAL_MS = Configuration.integerValue("codion.server.connectionMaintenanceIntervalMs", DEFAULT_CONNECTION_MAINTENANCE_INTERVAL);

  /**
   * @return the server name
   * @see Builder#serverNameProvider(Supplier)
   */
  String getServerName();

  /**
   * @return the server port
   */
  int getServerPort();

  /**
   * @return the registry port to use
   */
  int getRegistryPort();

  /**
   * @return the port on which to make the server admin interface accessible
   */
  int getServerAdminPort();

  /**
   * @return the class names of auxiliary server factories, providing the servers to run alongside this server
   */
  Collection<String> getAuxiliaryServerFactoryClassNames();

  /**
   * @return true if ssl is enabled
   */
  boolean isSslEnabled();

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
  boolean isSerializationFilterDryRun();

  /**
   * @return the interval between server connection maintenance runs, in milliseconds.
   */
  int getConnectionMaintenanceInterval();

  /**
   * A Builder for ServerConfiguration
   * @param <B> the builder type
   */
  interface Builder<B extends Builder<B>> {

    /**
     * @param adminPort the port on which to make the server admin interface accessible
     * @return this builder instance
     */
    B adminPort(int adminPort);

    /**
     * @param serverNameProvider the server name provider
     * @return this builder instance
     */
    B serverNameProvider(Supplier<String> serverNameProvider);

    /**
     * @param serverName the server name
     * @return this builder instance
     */
    B serverName(String serverName);

    /**
     * @param auxiliaryServerFactoryClassNames the class names of auxiliary server factories,
     * providing the servers to run alongside this server
     * @return this builder instance
     */
    B auxiliaryServerFactoryClassNames(Collection<String> auxiliaryServerFactoryClassNames);

    /**
     * When set to true this also sets the rmi client/server socket factories.
     * @param sslEnabled if true then ssl is enabled
     * @return this builder instance
     * @see #rmiClientSocketFactory(RMIClientSocketFactory)
     * @see #rmiServerSocketFactory(RMIServerSocketFactory)
     */
    B sslEnabled(boolean sslEnabled);

    /**
     * @param rmiClientSocketFactory the rmi client socket factory to use
     * @return this builder instance
     */
    B rmiClientSocketFactory(RMIClientSocketFactory rmiClientSocketFactory);

    /**
     * @param rmiServerSocketFactory the rmi server socket factory to use
     * @return this builder instance
     */
    B rmiServerSocketFactory(RMIServerSocketFactory rmiServerSocketFactory);

    /**
     * @param serializationFilterWhitelist the serialization whitelist
     * @return this builder instance
     */
    B serializationFilterWhitelist(String serializationFilterWhitelist);

    /**
     * @param serializationFilterDryRun true if serialization filter dry run is active
     * @return this builder instance
     */
    B serializationFilterDryRun(boolean serializationFilterDryRun);

    /**
     * @param connectionMaintenanceIntervalMs the interval between server connection maintenance runs, in milliseconds.
     * @return this builder instance
     */
    B connectionMaintenanceIntervalMs(int connectionMaintenanceIntervalMs);

    /**
     * @return a new ServerConfiguration instance based on this builder
     */
    ServerConfiguration build();
  }

  /**
   * @param serverPort the server port
   * @param <B> the builder type
   * @return a default server configuration
   */
  static <B extends Builder<B>> Builder<B> builder(int serverPort) {
    return (Builder<B>) new DefaultServerConfiguration.DefaultBuilder(serverPort, Registry.REGISTRY_PORT);
  }

  /**
   * @param serverPort the server port
   * @param registryPort the registry port
   * @param <B> the builder type
   * @return a default server configuration
   */
  static <B extends Builder<B>> Builder<B> builder(int serverPort, int registryPort) {
    return (Builder<B>) new DefaultServerConfiguration.DefaultBuilder(serverPort, registryPort);
  }

  /**
   * Returns a Builder initialized with values from system properties.
   * @param <B> the builder type
   * @return a server configuration builder initialized with values from system properties.
   */
  static <B extends Builder<B>> Builder<B> builderFromSystemProperties() {
    return (Builder<B>) builder(SERVER_PORT.getOrThrow(), REGISTRY_PORT.getOrThrow())
            .auxiliaryServerFactoryClassNames(Text.parseCommaSeparatedValues(ServerConfiguration.AUXILIARY_SERVER_FACTORY_CLASS_NAMES.get()))
            .adminPort(SERVER_ADMIN_PORT.getOrThrow())
            .sslEnabled(ServerConfiguration.SERVER_CONNECTION_SSL_ENABLED.get())
            .connectionMaintenanceIntervalMs(ServerConfiguration.CONNECTION_MAINTENANCE_INTERVAL_MS.get())
            .serializationFilterWhitelist(SERIALIZATION_FILTER_WHITELIST.get())
            .serializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN.get());
  }

  /**
   * @return a configuration according to system properties.
   */
  static ServerConfiguration fromSystemProperties() {
    return builderFromSystemProperties().build();
  }
}
