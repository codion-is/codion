/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.database.Database;
import org.jminor.common.remote.server.ServerConfiguration;
import org.jminor.common.user.User;
import org.jminor.common.version.Versions;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link EntityConnectionServer}.
 */
final class DefaultEntityConnectionServerConfiguration implements EntityConnectionServerConfiguration {

  private final ServerConfiguration serverConfiguration;

  private final int registryPort;
  private Integer serverAdminPort;
  private Database database;
  private User adminUser;
  private Integer connectionLimit = DEFAULT_SERVER_CONNECTION_LIMIT;
  private Boolean clientLoggingEnabled = false;
  private Integer connectionTimeout = ServerConfiguration.DEFAULT_SERVER_CONNECTION_TIMEOUT;
  private String connectionPoolProvider;
  private final Collection<String> domainModelClassNames = new HashSet<>();
  private final Collection<User> startupPoolUsers = new HashSet<>();
  private final Map<String, Integer> clientSpecificConnectionTimeouts = new HashMap<>();

  /**
   * @param serverConfiguration the server configuration
   * @param registryPort the registry port
   */
  DefaultEntityConnectionServerConfiguration(final int serverPort, final int registryPort) {
    this.serverConfiguration = ServerConfiguration.configuration(serverPort);
    this.registryPort = registryPort;
    this.serverConfiguration.setServerNameProvider(() -> {
      if (database == null) {
        throw new IllegalStateException("Database must be set before initializing server name");
      }
      final String databaseHost = database.getHost();
      final String sid = database.getSid();

      return ServerConfiguration.SERVER_NAME_PREFIX.get() + " " + Versions.getVersionString()
              + "@" + (sid != null ? sid.toUpperCase() : databaseHost.toUpperCase());
    });
  }

  @Override
  public ServerConfiguration getServerConfiguration() {
    return serverConfiguration;
  }

  @Override
  public String getServerName() {
    return serverConfiguration.getServerName();
  }

  @Override
  public int getServerPort() {
    return serverConfiguration.getServerPort();
  }

  @Override
  public Collection<String> getSharedLoginProxyClassNames() {
    return serverConfiguration.getSharedLoginProxyClassNames();
  }

  @Override
  public Collection<String> getLoginProxyClassNames() {
    return serverConfiguration.getLoginProxyClassNames();
  }

  @Override
  public Collection<String> getConnectionValidatorClassNames() {
    return serverConfiguration.getConnectionValidatorClassNames();
  }

  @Override
  public Collection<String> getAuxiliaryServerClassNames() {
    return serverConfiguration.getAuxiliaryServerClassNames();
  }

  @Override
  public Boolean getSslEnabled() {
    return serverConfiguration.getSslEnabled();
  }

  @Override
  public RMIClientSocketFactory getRmiClientSocketFactory() {
    return serverConfiguration.getRmiClientSocketFactory();
  }

  @Override
  public RMIServerSocketFactory getRmiServerSocketFactory() {
    return serverConfiguration.getRmiServerSocketFactory();
  }

  @Override
  public String getSerializationFilterWhitelist() {
    return serverConfiguration.getSerializationFilterWhitelist();
  }

  @Override
  public Boolean getSerializationFilterDryRun() {
    return serverConfiguration.getSerializationFilterDryRun();
  }

  @Override
  public int getRegistryPort() {
    return registryPort;
  }

  @Override
  public Integer getServerAdminPort() {
    return serverAdminPort;
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public User getAdminUser() {
    return adminUser;
  }

  @Override
  public Integer getConnectionLimit() {
    return connectionLimit;
  }

  @Override
  public Boolean getClientLoggingEnabled() {
    return clientLoggingEnabled;
  }

  @Override
  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  @Override
  public String getConnectionPoolProvider() {
    return connectionPoolProvider;
  }

  @Override
  public Collection<String> getDomainModelClassNames() {
    return domainModelClassNames;
  }

  @Override
  public Collection<User> getStartupPoolUsers() {
    return startupPoolUsers;
  }

  @Override
  public Map<String, Integer> getClientSpecificConnectionTimeouts() {
    return clientSpecificConnectionTimeouts;
  }

  @Override
  public EntityConnectionServerConfiguration setServerNameProvider(final Supplier<String> serverNameProvider) {
    serverConfiguration.setServerNameProvider(serverNameProvider);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setServerName(final String serverName) {
    serverConfiguration.setServerName(serverName);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setSharedLoginProxyClassNames(final Collection<String> sharedLoginProxyClassNames) {
    serverConfiguration.setSharedLoginProxyClassNames(sharedLoginProxyClassNames);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setLoginProxyClassNames(final Collection<String> loginProxyClassNames) {
    serverConfiguration.setLoginProxyClassNames(loginProxyClassNames);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setConnectionValidatorClassNames(final Collection<String> connectionValidatorClassNames) {
    serverConfiguration.setConnectionValidatorClassNames(connectionValidatorClassNames);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setAuxiliaryServerClassNames(final Collection<String> auxiliaryServerClassNames) {
    serverConfiguration.setAuxiliaryServerClassNames(auxiliaryServerClassNames);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setSslEnabled(final Boolean sslEnabled) {
    serverConfiguration.setSslEnabled(sslEnabled);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setRmiClientSocketFactory(final RMIClientSocketFactory rmiClientSocketFactory) {
    serverConfiguration.setRmiClientSocketFactory(rmiClientSocketFactory);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setRmiServerSocketFactory(final RMIServerSocketFactory rmiServerSocketFactory) {
    serverConfiguration.setRmiServerSocketFactory(rmiServerSocketFactory);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setSerializationFilterWhitelist(final String serializationFilterWhitelist) {
    serverConfiguration.setSerializationFilterWhitelist(serializationFilterWhitelist);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setSerializationFilterDryRun(final Boolean serializationFilterDryRun) {
    serverConfiguration.setSerializationFilterDryRun(serializationFilterDryRun);
    return this;
  }

  @Override
  public DefaultEntityConnectionServerConfiguration setAdminPort(final Integer adminPort) {
    this.serverAdminPort = requireNonNull(adminPort);
    return this;
  }

  @Override
  public DefaultEntityConnectionServerConfiguration setDatabase(final Database database) {
    this.database = requireNonNull(database);
    return this;
  }

  @Override
  public DefaultEntityConnectionServerConfiguration setAdminUser(final User adminUser) {
    this.adminUser = requireNonNull(adminUser);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setConnectionLimit(final Integer connectionLimit) {
    this.connectionLimit = requireNonNull(connectionLimit);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setClientLoggingEnabled(final Boolean clientLoggingEnabled) {
    this.clientLoggingEnabled = requireNonNull(clientLoggingEnabled);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setConnectionTimeout(final Integer connectionTimeout) {
    this.connectionTimeout = requireNonNull(connectionTimeout);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setConnectionPoolProvider(final String connectionPoolProvider) {
    this.connectionPoolProvider = requireNonNull(connectionPoolProvider);
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setDomainModelClassNames(final Collection<String> domainModelClassNames) {
    this.domainModelClassNames.addAll(requireNonNull(domainModelClassNames));
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setStartupPoolUsers(final Collection<User> startupPoolUsers) {
    this.startupPoolUsers.addAll(requireNonNull(startupPoolUsers));
    return this;
  }

  @Override
  public EntityConnectionServerConfiguration setClientSpecificConnectionTimeouts(final Map<String, Integer> clientSpecificConnectionTimeouts) {
    this.clientSpecificConnectionTimeouts.putAll(requireNonNull(clientSpecificConnectionTimeouts));
    return this;
  }
}
