/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.server;


import dev.codion.common.Configuration;
import dev.codion.common.Text;
import dev.codion.common.db.database.Database;
import dev.codion.common.db.database.Databases;
import dev.codion.common.rmi.server.ServerConfiguration;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.common.value.PropertyValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static dev.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Configuration values for a {@link EntityServer}.
 */
public interface EntityServerConfiguration extends ServerConfiguration {

  Logger LOG = LoggerFactory.getLogger(EntityServerConfiguration.class);

  int DEFAULT_SERVER_CONNECTION_LIMIT = -1;

  /**
   * Specifies maximum number of concurrent connections the server accepts<br>
   * -1 indicates no limit and 0 indicates a closed server.
   * Value type: Integer<br>
   * Default value: -1
   */
  PropertyValue<Integer> SERVER_CONNECTION_LIMIT = Configuration.integerValue("codion.server.connectionLimit", DEFAULT_SERVER_CONNECTION_LIMIT);

  /**
   * Specifies the class name of the connection pool provider to user, if none is specified
   * the internal connection pool is used if necessary<br>
   * Value type: String<br>
   * Default value: none
   * @see dev.codion.common.db.pool.ConnectionPoolProvider
   */
  PropertyValue<String> SERVER_CONNECTION_POOL_PROVIDER_CLASS = Configuration.stringValue(
          "codion.server.pooling.poolProviderClass", null);

  /**
   * Specifies the default client connection timeout (ms) in a comma separated list.
   * Example: dev.codion.demos.empdept.client.ui.EmpDeptAppPanel:60000,dev.codion.demos.chinook.ui.ChinookAppPanel:120000
   * Value type: String<br>
   * Default value: none
   */
  PropertyValue<String> SERVER_CLIENT_CONNECTION_TIMEOUT = Configuration.stringValue("codion.server.clientConnectionTimeout", null);

  /**
   * The initial connection logging status on the server, either true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> SERVER_CLIENT_LOGGING_ENABLED = Configuration.booleanValue("codion.server.clientLoggingEnabled", false);

  /**
   * Specifies a comma separated list of username:password combinations for which to create connection pools on startup
   * Example: scott:tiger,john:foo,paul:bar
   */
  PropertyValue<String> SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS = Configuration.stringValue("codion.server.pooling.startupPoolUsers", null);

  /**
   * Specifies a comma separated list of domain model class names, these classes must be
   * available on the server classpath
   */
  PropertyValue<String> SERVER_DOMAIN_MODEL_CLASSES = Configuration.stringValue("codion.server.domain.classes", null);

  /**
   * @return the registry port to use
   */
  int getRegistryPort();

  /**
   * @return the port on which to make the server admin interface accessible
   */
  Integer getServerAdminPort();

  /**
   * @return the Database implementation
   */
  Database getDatabase();

  /**
   * @return the admin user
   */
  User getAdminUser();

  /**
   * @return the maximum number of concurrent connections, -1 for no limit
   */
  Integer getConnectionLimit();

  /**
   * @return true if client logging should be enabled on startup
   */
  Boolean getClientLoggingEnabled();

  /**
   * @return the idle connection timeout
   */
  Integer getConnectionTimeout();

  /**
   * @return the connection pool provider classname
   */
  String getConnectionPoolProvider();

  /**
   * @return the domain model classes to load on startup
   */
  Collection<String> getDomainModelClassNames();

  /**
   * @return the users for which to initialize connection pools on startup
   */
  Collection<User> getStartupPoolUsers();

  /**
   * @return client specific connection timeouts, mapped to clientTypeId
   */
  Map<String, Integer> getClientSpecificConnectionTimeouts();

  /**
   * @param adminPort the port on which to make the server admin interface accessible
   */
  void setAdminPort(Integer adminPort);

  /**
   * @param database the Database implementation
   */
  void setDatabase(Database database);

  /**
   * @param adminUser the admin user
   */
  void setAdminUser(User adminUser);

  /**
   * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
   */
  void setConnectionLimit(Integer connectionLimit);

  /**
   * @param clientLoggingEnabled if true then client logging is enabled on startup
   */
  void setClientLoggingEnabled(Boolean clientLoggingEnabled);

  /**
   * @param connectionTimeout the idle connection timeout
   */
  void setConnectionTimeout(Integer connectionTimeout);

  /**
   * @param connectionPoolProvider the connection pool provider classname
   */
  void setConnectionPoolProvider(String connectionPoolProvider);

  /**
   * @param domainModelClassNames the domain model classes to load on startup
   */
  void setDomainModelClassNames(Collection<String> domainModelClassNames);

  /**
   * @param startupPoolUsers the users for which to initialize connection pools on startup
   */
  void setStartupPoolUsers(Collection<User> startupPoolUsers);

  /**
   * @param clientSpecificConnectionTimeouts client specific connection timeouts, mapped to clientTypeId
   */
  void setClientSpecificConnectionTimeouts(Map<String, Integer> clientSpecificConnectionTimeouts);

  /**
   * @param serverPort the server port
   * @param registryPort the registry port
   * @return a default entity connection server configuration
   */
  static EntityServerConfiguration configuration(final int serverPort, final int registryPort) {
    return new DefaultEntityServerConfiguration(serverPort, registryPort);
  }

  /**
   * Parses configuration from system properties.
   * @return the server configuration according to system properties
   */
  static EntityServerConfiguration fromSystemProperties() {
    final DefaultEntityServerConfiguration configuration = new DefaultEntityServerConfiguration(
            requireNonNull(SERVER_PORT.get(), SERVER_PORT.getProperty()),
            requireNonNull(REGISTRY_PORT.get(), REGISTRY_PORT.toString()));
    configuration.setAuxiliaryServerProviderClassNames(Text.parseCommaSeparatedValues(AUXILIARY_SERVER_CLASS_NAMES.get()));
    configuration.setSslEnabled(SERVER_CONNECTION_SSL_ENABLED.get());
    configuration.setLoginProxyClassNames(Text.parseCommaSeparatedValues(SERVER_LOGIN_PROXY_CLASSES.get()));
    if (SERIALIZATION_FILTER_WHITELIST.get() != null) {
      configuration.setSerializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN.get());
    }
    if (SERIALIZATION_FILTER_DRYRUN.get() != null) {
      configuration.setSerializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN.get());
    }
    configuration.setAdminPort(requireNonNull(SERVER_ADMIN_PORT.get(), SERVER_ADMIN_PORT.toString()));
    configuration.setConnectionLimit(SERVER_CONNECTION_LIMIT.get());
    configuration.setDatabase(Databases.getInstance());
    configuration.setDomainModelClassNames(Text.parseCommaSeparatedValues(SERVER_DOMAIN_MODEL_CLASSES.get()));
    configuration.setStartupPoolUsers(Text.parseCommaSeparatedValues(SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS.get())
            .stream().map(Users::parseUser).collect(toList()));
    configuration.setClientLoggingEnabled(SERVER_CLIENT_LOGGING_ENABLED.get());
    configuration.setConnectionTimeout(SERVER_CONNECTION_TIMEOUT.get());
    final Map<String, Integer> timeoutMap = new HashMap<>();
    for (final String clientTimeout : Text.parseCommaSeparatedValues(SERVER_CLIENT_CONNECTION_TIMEOUT.get())) {
      final String[] split = clientTimeout.split(":");
      if (split.length < 2) {
        throw new IllegalArgumentException("Expecting a ':' delimiter");
      }
      timeoutMap.put(split[0], Integer.parseInt(split[1]));
    }
    configuration.setClientSpecificConnectionTimeouts(timeoutMap);
    final String adminUserString = SERVER_ADMIN_USER.get();
    final User adminUser = nullOrEmpty(adminUserString) ? null : Users.parseUser(adminUserString);
    if (adminUser == null) {
      EntityServerConfiguration.LOG.info("No admin user specified");
    }
    else {
      EntityServerConfiguration.LOG.info("Admin user: " + adminUser);
      configuration.setAdminUser(adminUser);
    }

    return configuration;
  }
}
