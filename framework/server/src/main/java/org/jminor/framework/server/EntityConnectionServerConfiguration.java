/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;


import org.jminor.common.Configuration;
import org.jminor.common.Text;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.Databases;
import org.jminor.common.remote.server.ServerConfiguration;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.common.value.PropertyValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * Configuration values for a {@link EntityConnectionServer}.
 */
public interface EntityConnectionServerConfiguration {

  Logger LOG = LoggerFactory.getLogger(EntityConnectionServerConfiguration.class);

  int DEFAULT_SERVER_CONNECTION_LIMIT = -1;

  /**
   * Specifies maximum number of concurrent connections the server accepts<br>
   * -1 indicates no limit and 0 indicates a closed server.
   * Value type: Integer<br>
   * Default value: -1
   */
  PropertyValue<Integer> SERVER_CONNECTION_LIMIT = Configuration.integerValue("jminor.server.connectionLimit", DEFAULT_SERVER_CONNECTION_LIMIT);

  /**
   * Specifies the class name of the connection pool provider to user, if none is specified
   * the internal connection pool is used if necessary<br>
   * Value type: String<br>
   * Default value: none
   * @see org.jminor.common.db.pool.ConnectionPoolProvider
   */
  PropertyValue<String> SERVER_CONNECTION_POOL_PROVIDER_CLASS = Configuration.stringValue(
          "jminor.server.pooling.poolProviderClass", null);

  /**
   * Specifies the default client connection timeout (ms) in a comma separated list.
   * Example: org.jminor.demos.empdept.client.ui.EmpDeptAppPanel:60000,org.jminor.demos.chinook.ui.ChinookAppPanel:120000
   * Value type: String<br>
   * Default value: none
   */
  PropertyValue<String> SERVER_CLIENT_CONNECTION_TIMEOUT = Configuration.stringValue("jminor.server.clientConnectionTimeout", null);

  /**
   * The initial connection logging status on the server, either true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> SERVER_CLIENT_LOGGING_ENABLED = Configuration.booleanValue("jminor.server.clientLoggingEnabled", false);

  /**
   * Specifies a comma separated list of username:password combinations for which to create connection pools on startup
   * Example: scott:tiger,john:foo,paul:bar
   */
  PropertyValue<String> SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS = Configuration.stringValue("jminor.server.pooling.startupPoolUsers", null);

  /**
   * Specifies a comma separated list of domain model class names, these classes must be
   * available on the server classpath
   */
  PropertyValue<String> SERVER_DOMAIN_MODEL_CLASSES = Configuration.stringValue("jminor.server.domain.classes", null);

  /**
   * @return the server configuration
   */
  ServerConfiguration getServerConfiguration();

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
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setAdminPort(Integer adminPort);

  /**
   * @param database the Database implementation
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setDatabase(Database database);

  /**
   * @param adminUser the admin user
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setAdminUser(User adminUser);

  /**
   * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setConnectionLimit(Integer connectionLimit);

  /**
   * @param clientLoggingEnabled if true then client logging is enabled on startup
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setClientLoggingEnabled(Boolean clientLoggingEnabled);

  /**
   * @param connectionTimeout the idle connection timeout
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setConnectionTimeout(Integer connectionTimeout);

  /**
   * @param connectionPoolProvider the connection pool provider classname
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setConnectionPoolProvider(String connectionPoolProvider);

  /**
   * @param domainModelClassNames the domain model classes to load on startup
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setDomainModelClassNames(Collection<String> domainModelClassNames);

  /**
   * @param startupPoolUsers the users for which to initialize connection pools on startup
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setStartupPoolUsers(Collection<User> startupPoolUsers);

  /**
   * @param clientSpecificConnectionTimeouts client specific connection timeouts, mapped to clientTypeId
   * @return this configuration instance
   */
  EntityConnectionServerConfiguration setClientSpecificConnectionTimeouts(Map<String, Integer> clientSpecificConnectionTimeouts);

  /**
   * @param serverConfiguration the server configuration
   * @param registryPort the registry port
   * @return a default entity connection server configuration
   */
  static EntityConnectionServerConfiguration configuration(final ServerConfiguration serverConfiguration, final int registryPort) {
    return new DefaultEntityConnectionServerConfiguration(serverConfiguration, registryPort);
  }

  /**
   * Parses configuration from system properties.
   * @return the server configuration according to system properties
   */
  static EntityConnectionServerConfiguration fromSystemProperties() {
    final ServerConfiguration serverConfiguration = ServerConfiguration.fromSystemProperties();
    final DefaultEntityConnectionServerConfiguration configuration = new DefaultEntityConnectionServerConfiguration(serverConfiguration,
            requireNonNull(ServerConfiguration.REGISTRY_PORT.get(), ServerConfiguration.REGISTRY_PORT.toString()));
    configuration.setAdminPort(requireNonNull(ServerConfiguration.SERVER_ADMIN_PORT.get(), ServerConfiguration.SERVER_ADMIN_PORT.toString()));
    configuration.setConnectionLimit(SERVER_CONNECTION_LIMIT.get());
    configuration.setDatabase(Databases.getInstance());
    configuration.setDomainModelClassNames(Text.parseCommaSeparatedValues(SERVER_DOMAIN_MODEL_CLASSES.get()));
    configuration.setStartupPoolUsers(Text.parseCommaSeparatedValues(SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS.get())
            .stream().map(Users::parseUser).collect(toList()));
    configuration.setClientLoggingEnabled(SERVER_CLIENT_LOGGING_ENABLED.get());
    configuration.setConnectionTimeout(ServerConfiguration.SERVER_CONNECTION_TIMEOUT.get());
    final Map<String, Integer> timeoutMap = new HashMap<>();
    for (final String clientTimeout : Text.parseCommaSeparatedValues(SERVER_CLIENT_CONNECTION_TIMEOUT.get())) {
      final String[] split = clientTimeout.split(":");
      if (split.length < 2) {
        throw new IllegalArgumentException("Expecting a ':' delimiter");
      }
      timeoutMap.put(split[0], Integer.parseInt(split[1]));
    }
    configuration.setClientSpecificConnectionTimeouts(timeoutMap);
    final String adminUserString = ServerConfiguration.SERVER_ADMIN_USER.get();
    final User adminUser = nullOrEmpty(adminUserString) ? null : Users.parseUser(adminUserString);
    if (adminUser == null) {
      EntityConnectionServerConfiguration.LOG.info("No admin user specified");
    }
    else {
      EntityConnectionServerConfiguration.LOG.info("Admin user: " + adminUser);
      configuration.setAdminUser(adminUser);
    }

    return configuration;
  }
}
