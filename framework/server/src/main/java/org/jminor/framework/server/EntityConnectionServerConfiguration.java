/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.Configuration;
import org.jminor.common.Text;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.Databases;
import org.jminor.common.remote.server.AbstractServerConfiguration;
import org.jminor.common.remote.server.Server;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.common.value.PropertyValue;
import org.jminor.common.version.Versions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
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
public final class EntityConnectionServerConfiguration extends AbstractServerConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(EntityConnectionServerConfiguration.class);

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
  private final Collection<User> startupPoolUsers = new HashSet<>();
  private final Collection<String> auxiliaryServerClassNames = new HashSet<>();
  private final Map<String, Integer> clientSpecificConnectionTimeouts = new HashMap<>();

  /**
   * @param serverPort the port on which to make the server accessible
   * @param registryPort the registry port to use
   */
  public EntityConnectionServerConfiguration(final int serverPort, final int registryPort) {
    super(serverPort);
    this.registryPort = registryPort;
  }

  /**
   * @return the registry port to use
   */
  public int getRegistryPort() {
    return registryPort;
  }

  /**
   * @return the port on which to make the server admin interface accessible
   */
  public Integer getServerAdminPort() {
    return serverAdminPort;
  }

  /**
   * @return the Database implementation
   */
  public Database getDatabase() {
    return database;
  }

  /**
   * @return the admin user
   */
  public User getAdminUser() {
    return adminUser;
  }

  /**
   * @return true if ssl is enabled
   */
  public Boolean getSslEnabled() {
    return sslEnabled;
  }

  /**
   * @return the maximum number of concurrent connections, -1 for no limit
   */
  public Integer getConnectionLimit() {
    return connectionLimit;
  }

  /**
   * @return true if client logging should be enabled on startup
   */
  public Boolean getClientLoggingEnabled() {
    return clientLoggingEnabled;
  }

  /**
   * @return the idle connection timeout
   */
  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * @return the connection pool provider classname
   */
  public String getConnectionPoolProvider() {
    return connectionPoolProvider;
  }

  /**
   * @return the serialization whitelist to use, if any
   */
  public String getSerializationFilterWhitelist() {
    return serializationFilterWhitelist;
  }

  /**
   * @return true if a serialization filter dry run should be active
   */
  public Boolean getSerializationFilterDryRun() {
    return serializationFilterDryRun;
  }

  /**
   * @return the domain model classes to load on startup
   */
  public Collection<String> getDomainModelClassNames() {
    return domainModelClassNames;
  }

  /**
   * @return the users for which to initialize connection pools on startup
   */
  public Collection<User> getStartupPoolUsers() {
    return startupPoolUsers;
  }

  /**
   * @return the class names of auxiliary servers to run alongside this server
   */
  public Collection<String> getAuxiliaryServerClassNames() {
    return auxiliaryServerClassNames;
  }

  /**
   * @return client specific connection timeouts, mapped to clientTypeId
   */
  public Map<String, Integer> getClientSpecificConnectionTimeouts() {
    return clientSpecificConnectionTimeouts;
  }

  /**
   * @param adminPort the port on which to make the server admin interface accessible
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setAdminPort(final Integer adminPort) {
    this.serverAdminPort = requireNonNull(adminPort);
    return this;
  }

  /**
   * @param database the Database implementation
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setDatabase(final Database database) {
    this.database = requireNonNull(database);
    return this;
  }

  /**
   * @param adminUser the admin user
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setAdminUser(final User adminUser) {
    this.adminUser = requireNonNull(adminUser);
    return this;
  }

  /**
   * When set to true this also sets the rmi client/server socket factories.
   * @param sslEnabled if true then ssl is enabled
   * @return this configuration instance
   * @see #setRmiClientSocketFactory(RMIClientSocketFactory)
   * @see #setRmiServerSocketFactory(RMIServerSocketFactory)
   */
  public EntityConnectionServerConfiguration setSslEnabled(final Boolean sslEnabled) {
    this.sslEnabled = requireNonNull(sslEnabled);
    if (sslEnabled) {
      setRmiClientSocketFactory(new SslRMIClientSocketFactory());
      setRmiServerSocketFactory(new SslRMIServerSocketFactory());
    }
    return this;
  }

  /**
   * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setConnectionLimit(final Integer connectionLimit) {
    this.connectionLimit = requireNonNull(connectionLimit);
    return this;
  }

  /**
   * @param clientLoggingEnabled if true then client logging is enabled on startup
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setClientLoggingEnabled(final Boolean clientLoggingEnabled) {
    this.clientLoggingEnabled = requireNonNull(clientLoggingEnabled);
    return this;
  }

  /**
   * @param connectionTimeout the idle connection timeout
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setConnectionTimeout(final Integer connectionTimeout) {
    this.connectionTimeout = requireNonNull(connectionTimeout);
    return this;
  }

  /**
   * @param connectionPoolProvider the connection pool provider classname
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setConnectionPoolProvider(final String connectionPoolProvider) {
    this.connectionPoolProvider = requireNonNull(connectionPoolProvider);
    return this;
  }

  /**
   * @param serializationFilterWhitelist the serialization whitelist
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setSerializationFilterWhitelist(final String serializationFilterWhitelist) {
    this.serializationFilterWhitelist = requireNonNull(serializationFilterWhitelist);
    return this;
  }

  /**
   * @param serializationFilterDryRun true if serialization filter dry run is active
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setSerializationFilterDryRun(final Boolean serializationFilterDryRun) {
    this.serializationFilterDryRun = requireNonNull(serializationFilterDryRun);
    return this;
  }

  /**
   * @param domainModelClassNames the domain model classes to load on startup
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setDomainModelClassNames(final Collection<String> domainModelClassNames) {
    this.domainModelClassNames.addAll(requireNonNull(domainModelClassNames));
    return this;
  }

  /**
   * @param startupPoolUsers the users for which to initialize connection pools on startup
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setStartupPoolUsers(final Collection<User> startupPoolUsers) {
    this.startupPoolUsers.addAll(requireNonNull(startupPoolUsers));
    return this;
  }

  /**
   * @param auxiliaryServerClassNames the class names of auxiliary servers to run alongside this server
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setAuxiliaryServerClassNames(final Collection<String> auxiliaryServerClassNames) {
    this.auxiliaryServerClassNames.addAll(requireNonNull(auxiliaryServerClassNames));
    return this;
  }

  /**
   * @param clientSpecificConnectionTimeouts client specific connection timeouts, mapped to clientTypeId
   * @return this configuration instance
   */
  public EntityConnectionServerConfiguration setClientSpecificConnectionTimeouts(final Map<String, Integer> clientSpecificConnectionTimeouts) {
    this.clientSpecificConnectionTimeouts.putAll(requireNonNull(clientSpecificConnectionTimeouts));
    return this;
  }

  /**
   * Parses configuration from system properties.
   * @return the server configuration according to system properties
   */
  public static EntityConnectionServerConfiguration fromSystemProperties() {
    final EntityConnectionServerConfiguration configuration = new EntityConnectionServerConfiguration(
            requireNonNull(Server.SERVER_PORT.get(), Server.SERVER_PORT.toString()),
            requireNonNull(Server.REGISTRY_PORT.get(), Server.REGISTRY_PORT.toString()));
    configuration.setAdminPort(requireNonNull(Server.SERVER_ADMIN_PORT.get(), Server.SERVER_ADMIN_PORT.toString()));
    configuration.setSslEnabled(Server.SERVER_CONNECTION_SSL_ENABLED.get());
    configuration.setConnectionLimit(SERVER_CONNECTION_LIMIT.get());
    configuration.setDatabase(Databases.getInstance());
    if (SERIALIZATION_FILTER_WHITELIST.get() != null) {
      configuration.setSerializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN.get());
    }
    if (SERIALIZATION_FILTER_DRYRUN.get() != null) {
      configuration.setSerializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN.get());
    }
    configuration.setDomainModelClassNames(Text.parseCommaSeparatedValues(SERVER_DOMAIN_MODEL_CLASSES.get()));
    configuration.setLoginProxyClassNames(Text.parseCommaSeparatedValues(SERVER_LOGIN_PROXY_CLASSES.get()));
    configuration.setConnectionValidatorClassNames(Text.parseCommaSeparatedValues(SERVER_CONNECTION_VALIDATOR_CLASSES.get()));
    configuration.setStartupPoolUsers(getPoolUsers(Text.parseCommaSeparatedValues(SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS.get())));
    configuration.setAuxiliaryServerClassNames(Text.parseCommaSeparatedValues(Server.AUXILIARY_SERVER_CLASS_NAMES.get()));
    configuration.setClientLoggingEnabled(SERVER_CLIENT_LOGGING_ENABLED.get());
    configuration.setConnectionTimeout(Server.SERVER_CONNECTION_TIMEOUT.get());
    configuration.setClientSpecificConnectionTimeouts(getClientTimeouts(Text.parseCommaSeparatedValues(SERVER_CLIENT_CONNECTION_TIMEOUT.get())));
    final String adminUserString = Server.SERVER_ADMIN_USER.get();
    final User adminUser = nullOrEmpty(adminUserString) ? null : Users.parseUser(adminUserString);
    if (adminUser == null) {
      LOG.info("No admin user specified");
    }
    else {
      LOG.info("Admin user: " + adminUser);
      configuration.setAdminUser(adminUser);
    }

    return configuration;
  }

  @Override
  protected String initializeServerName() {
    if (database == null) {
      throw new IllegalStateException("Database must be set before initializing server name");
    }
    final String databaseHost = database.getHost();
    final String sid = database.getSid();

    return Server.SERVER_NAME_PREFIX.get() + " " + Versions.getVersionString()
            + "@" + (sid != null ? sid.toUpperCase() : databaseHost.toUpperCase());
  }

  private static Collection<User> getPoolUsers(final Collection<String> poolUsers) {
    return poolUsers.stream().map(Users::parseUser).collect(toList());
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
