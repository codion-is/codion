/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.Configuration;
import org.jminor.common.Text;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.Databases;
import org.jminor.common.remote.server.Server;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.common.value.PropertyValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * Configuration values for a {@link EntityConnectionServer}.
 */
public final class ServerConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ServerConfiguration.class);

  private static final int DEFAULT_SERVER_CONNECTION_LIMIT = -1;

  /**
   * The serialization whitelist file to use if any
   */
  public static final PropertyValue<String> SERIALIZATION_FILTER_WHITELIST = Configuration.stringValue("jminor.server.serializationFilterWhitelist", null);

  /**
   * If true then the serialization whitelist specified by {@link #SERIALIZATION_FILTER_WHITELIST} is populated
   * with the names of all deserialized classes on server shutdown. Note this overwrites the file if it already exists.
   */
  public static final PropertyValue<Boolean> SERIALIZATION_FILTER_DRYRUN = Configuration.booleanValue("jminor.server.serializationFilterDryRun", false);

  /**
   * Specifies the class name of the connection pool provider to user, if none is specified
   * the internal connection pool is used if necessary<br>
   * Value type: String<br>
   * Default value: none
   * @see org.jminor.common.db.pool.ConnectionPoolProvider
   */
  public static final PropertyValue<String> SERVER_CONNECTION_POOL_PROVIDER_CLASS = Configuration.stringValue("jminor.server.pooling.poolProviderClass", null);

  /**
   * Specifies maximum number of concurrent connections the server accepts<br>
   * -1 indicates no limit and 0 indicates a closed server.
   * Value type: Integer<br>
   * Default value: -1
   */
  public static final PropertyValue<Integer> SERVER_CONNECTION_LIMIT = Configuration.integerValue("jminor.server.connectionLimit", DEFAULT_SERVER_CONNECTION_LIMIT);

  /**
   * Specifies the default client connection timeout (ms) in a comma separated list.
   * Example: org.jminor.demos.empdept.client.ui.EmpDeptAppPanel:60000,org.jminor.demos.chinook.ui.ChinookAppPanel:120000
   * Value type: String<br>
   * Default value: none
   */
  public static final PropertyValue<String> SERVER_CLIENT_CONNECTION_TIMEOUT = Configuration.stringValue("jminor.server.clientConnectionTimeout", null);

  /**
   * The initial connection logging status on the server, either true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> SERVER_CLIENT_LOGGING_ENABLED = Configuration.booleanValue("jminor.server.clientLoggingEnabled", false);

  /**
   * Specifies a comma separated list of username:password combinations for which to create connection pools on startup
   * Example: scott:tiger,john:foo,paul:bar
   */
  public static final PropertyValue<String> SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS = Configuration.stringValue("jminor.server.pooling.startupPoolUsers", null);

  /**
   * Specifies a comma separated list of ConnectionValidator class names, which should be initialized on server startup,
   * these classes must be available on the server classpath and contain a parameterless constructor
   * @see org.jminor.common.remote.server.ConnectionValidator
   */
  public static final PropertyValue<String> SERVER_CONNECTION_VALIDATOR_CLASSES = Configuration.stringValue("jminor.server.connectionValidatorClasses", null);

  /**
   * Specifies a comma separated list of LoginProxy class names, which should be initialized on server startup,
   * these classes must be available on the server classpath and contain a parameterless constructor
   * @see org.jminor.common.remote.server.LoginProxy
   */
  public static final PropertyValue<String> SERVER_LOGIN_PROXY_CLASSES = Configuration.stringValue("jminor.server.loginProxyClasses", null);

  /**
   * Specifies a comma separated list of domain model class names, these classes must be
   * available on the server classpath
   */
  public static final PropertyValue<String> SERVER_DOMAIN_MODEL_CLASSES = Configuration.stringValue("jminor.server.domain.classes", null);

  private final int serverPort;
  private final int registryPort;
  private Integer serverAdminPort;
  private Database database;
  private User adminUser;
  private Boolean sslEnabled = true;
  private Integer connectionLimit = DEFAULT_SERVER_CONNECTION_LIMIT;
  private Boolean clientLoggingEnabled = false;
  private Integer connectionTimeout = Server.DEFAULT_SERVER_CONNECTION_TIMEOUT;
  private String serializationFilterWhitelist;
  private Boolean serializationFilterDryRun = false;
  private String connectionPoolProvider;
  private final Collection<String> domainModelClassNames = new HashSet<>();
  private final Collection<String> loginProxyClassNames = new HashSet<>();
  private final Collection<String> connectionValidatorClassNames = new HashSet<>();
  private final Collection<User> startupPoolUsers = new HashSet<>();
  private final Collection<String> auxiliaryServerClassNames = new HashSet<>();
  private final Map<String, Integer> clientSpecificConnectionTimeouts = new HashMap<>();

  /**
   * @param serverPort the port on which to make the server accessible
   * @param registryPort the registry port to use
   */
  public ServerConfiguration(final int serverPort, final int registryPort) {
    this.serverPort = serverPort;
    this.registryPort = registryPort;
  }

  /**
   * @param adminPort the port on which to make the server admin interface accessible
   * @return this configuration instance
   */
  public ServerConfiguration adminPort(final Integer adminPort) {
    this.serverAdminPort = requireNonNull(adminPort);
    return this;
  }

  /**
   * @param database the Database implementation
   * @return this configuration instance
   */
  public ServerConfiguration database(final Database database) {
    this.database = requireNonNull(database);
    return this;
  }

  /**
   * @param adminUser the admin user
   * @return this configuration instance
   */
  public ServerConfiguration adminUser(final User adminUser) {
    this.adminUser = requireNonNull(adminUser);
    return this;
  }

  /**
   * @param sslEnabled if true then ssl is enabled
   * @return this configuration instance
   */
  public ServerConfiguration sslEnabled(final Boolean sslEnabled) {
    this.sslEnabled = requireNonNull(sslEnabled);
    return this;
  }

  /**
   * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
   * @return this configuration instance
   */
  public ServerConfiguration connectionLimit(final Integer connectionLimit) {
    this.connectionLimit = requireNonNull(connectionLimit);
    return this;
  }

  /**
   * @param clientLoggingEnabled if true then client logging is enabled on startup
   * @return this configuration instance
   */
  public ServerConfiguration clientLoggingEnabled(final Boolean clientLoggingEnabled) {
    this.clientLoggingEnabled = requireNonNull(clientLoggingEnabled);
    return this;
  }

  /**
   * @param connectionTimeout the idle connection timeout
   * @return this configuration instance
   */
  public ServerConfiguration connectionTimeout(final Integer connectionTimeout) {
    this.connectionTimeout = requireNonNull(connectionTimeout);
    return this;
  }

  /**
   * @param connectionPoolProvider the connection pool provider classname
   * @return this configuration instance
   */
  public ServerConfiguration connectionPoolProvider(final String connectionPoolProvider) {
    this.connectionPoolProvider = requireNonNull(connectionPoolProvider);
    return this;
  }

  /**
   * @param serializationFilterWhitelist the serialization whitelist
   * @return this configuration instance
   */
  public ServerConfiguration serializationFilterWhitelist(final String serializationFilterWhitelist) {
    this.serializationFilterWhitelist = requireNonNull(serializationFilterWhitelist);
    return this;
  }

  /**
   * @param serializationFilterDryRun true if serialization filter dry run is active
   * @return this configuration instance
   */
  public ServerConfiguration serializationFilterDryRun(final Boolean serializationFilterDryRun) {
    this.serializationFilterDryRun = requireNonNull(serializationFilterDryRun);
    return this;
  }

  /**
   * @param domainModelClassNames the domain model classes to load on startup
   * @return this configuration instance
   */
  public ServerConfiguration domainModelClassNames(final Collection<String> domainModelClassNames) {
    this.domainModelClassNames.addAll(requireNonNull(domainModelClassNames));
    return this;
  }

  /**
   * @param loginProxyClassNames the login proxy classes to initialize on startup
   * @return this configuration instance
   */
  public ServerConfiguration loginProxyClassNames(final Collection<String> loginProxyClassNames) {
    this.loginProxyClassNames.addAll(requireNonNull(loginProxyClassNames));
    return this;
  }

  /**
   * @param connectionValidatorClassNames the connection validation classes to initialize on startup
   * @return this configuration instance
   */
  public ServerConfiguration connectionValidatorClassNames(final Collection<String> connectionValidatorClassNames) {
    this.connectionValidatorClassNames.addAll(requireNonNull(connectionValidatorClassNames));
    return this;
  }

  /**
   * @param startupPoolUsers the users for which to initialize connection pools on startup
   * @return this configuration instance
   */
  public ServerConfiguration startupPoolUsers(final Collection<User> startupPoolUsers) {
    this.startupPoolUsers.addAll(requireNonNull(startupPoolUsers));
    return this;
  }

  /**
   * @param auxiliaryServerClassNames the class names of auxiliary servers to run alongside this server
   * @return this configuration instance
   */
  public ServerConfiguration auxiliaryServerClassNames(final Collection<String> auxiliaryServerClassNames) {
    this.auxiliaryServerClassNames.addAll(requireNonNull(auxiliaryServerClassNames));
    return this;
  }

  /**
   * @param clientSpecificConnectionTimeouts client specific connection timeouts, mapped to clientTypeId
   * @return this configuration instance
   */
  public ServerConfiguration clientSpecificConnectionTimeouts(final Map<String, Integer> clientSpecificConnectionTimeouts) {
    this.clientSpecificConnectionTimeouts.putAll(requireNonNull(clientSpecificConnectionTimeouts));
    return this;
  }

  /**
   * Parses configuration from system properties.
   * @return the server configuration according to system properties
   */
  public static ServerConfiguration fromSystemProperties() {
    final ServerConfiguration configuration = new ServerConfiguration(
            requireNonNull(Server.SERVER_PORT.get(), Server.SERVER_PORT.toString()),
            requireNonNull(Server.REGISTRY_PORT.get(), Server.REGISTRY_PORT.toString()));
    configuration.adminPort(requireNonNull(Server.SERVER_ADMIN_PORT.get(), Server.SERVER_ADMIN_PORT.toString()));
    configuration.sslEnabled(Server.SERVER_CONNECTION_SSL_ENABLED.get());
    configuration.connectionLimit(SERVER_CONNECTION_LIMIT.get());
    configuration.database(Databases.getInstance());
    if (SERIALIZATION_FILTER_WHITELIST.get() != null) {
      configuration.serializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN.get());
    }
    if (SERIALIZATION_FILTER_DRYRUN.get() != null) {
      configuration.serializationFilterWhitelist(SERIALIZATION_FILTER_WHITELIST.get());
    }
    configuration.domainModelClassNames(Text.parseCommaSeparatedValues(SERVER_DOMAIN_MODEL_CLASSES.get()));
    configuration.loginProxyClassNames(Text.parseCommaSeparatedValues(SERVER_LOGIN_PROXY_CLASSES.get()));
    configuration.connectionValidatorClassNames(Text.parseCommaSeparatedValues(SERVER_CONNECTION_VALIDATOR_CLASSES.get()));
    configuration.startupPoolUsers(getPoolUsers(Text.parseCommaSeparatedValues(SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS.get())));
    configuration.auxiliaryServerClassNames(Text.parseCommaSeparatedValues(Server.AUXILIARY_SERVER_CLASS_NAMES.get()));
    configuration.clientLoggingEnabled(SERVER_CLIENT_LOGGING_ENABLED.get());
    configuration.connectionTimeout(Server.SERVER_CONNECTION_TIMEOUT.get());
    configuration.clientSpecificConnectionTimeouts(getClientTimeoutValues());
    final String adminUserString = Server.SERVER_ADMIN_USER.get();
    final User adminUser = nullOrEmpty(adminUserString) ? null : Users.parseUser(adminUserString);
    if (adminUser == null) {
      LOG.info("No admin user specified");
    }
    else {
      LOG.info("Admin user: " + adminUser);
      configuration.adminUser(adminUser);
    }

    return configuration;
  }

  /**
   * @return the port on which to make the server accessible
   */
  int getServerPort() {
    return serverPort;
  }

  /**
   * @return the registry port to use
   */
  int getRegistryPort() {
    return registryPort;
  }

  /**
   * @return the port on which to make the server admin interface accessible
   */
  Integer getServerAdminPort() {
    return serverAdminPort;
  }

  /**
   * @return the Database implementation
   */
  Database getDatabase() {
    return database;
  }

  /**
   * @return the admin user
   */
  User getAdminUser() {
    return adminUser;
  }

  /**
   * @return true if ssl is enabled
   */
  Boolean getSslEnabled() {
    return sslEnabled;
  }

  /**
   * @return the maximum number of concurrent connections, -1 for no limit
   */
  Integer getConnectionLimit() {
    return connectionLimit;
  }

  /**
   * @return true if client logging should be enabled on startup
   */
  Boolean getClientLoggingEnabled() {
    return clientLoggingEnabled;
  }

  /**
   * @return the idle connection timeout
   */
  Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * @return the connection pool provider classname
   */
  String getConnectionPoolProvider() {
    return connectionPoolProvider;
  }

  /**
   * @return the serialization whitelist to use, if any
   */
  String getSerializationFilterWhitelist() {
    return serializationFilterWhitelist;
  }

  /**
   * @return true if a serialization filter dry run should be active
   */
  Boolean getSerializationFilterDryRun() {
    return serializationFilterDryRun;
  }

  /**
   * @return the domain model classes to load on startup
   */
  Collection<String> getDomainModelClassNames() {
    return domainModelClassNames;
  }

  /**
   * @return the login proxy classes to initialize on startup
   */
  Collection<String> getLoginProxyClassNames() {
    return loginProxyClassNames;
  }

  /**
   * @return the connection validation classes to initialize on startup
   */
  Collection<String> getConnectionValidatorClassNames() {
    return connectionValidatorClassNames;
  }

  /**
   * @return the users for which to initialize connection pools on startup
   */
  Collection<User> getStartupPoolUsers() {
    return startupPoolUsers;
  }

  /**
   * @return the class names of auxiliary servers to run alongside this server
   */
  Collection<String> getAuxiliaryServerClassNames() {
    return auxiliaryServerClassNames;
  }

  /**
   * @return client specific connection timeouts, mapped to clientTypeId
   */
  Map<String, Integer> getClientSpecificConnectionTimeouts() {
    return clientSpecificConnectionTimeouts;
  }

  private static Collection<User> getPoolUsers(final Collection<String> poolUsers) {
    return poolUsers.stream().map(Users::parseUser).collect(toList());
  }

  private static Map<String, Integer> getClientTimeoutValues() {
    final Collection<String> values = Text.parseCommaSeparatedValues(SERVER_CLIENT_CONNECTION_TIMEOUT.get());

    return getClientTimeouts(values);
  }

  private static Map<String, Integer> getClientTimeouts(final Collection<String> values) {
    final Map<String, Integer> timeoutMap = new HashMap<>();
    for (final String clientTimeout : values) {
      final String[] split = clientTimeout.split(":");
      if (split.length < 2) {
        throw new IllegalArgumentException("Expecting a ':' delimiter");
      }
      timeoutMap.put(split[0], Integer.parseInt(split[1]));
    }

    return timeoutMap;
  }
}
