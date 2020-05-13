/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.server;

import dev.codion.common.db.database.Database;
import dev.codion.common.rmi.server.ServerConfiguration;
import dev.codion.common.user.User;
import dev.codion.common.version.Versions;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link EntityServer}.
 */
final class DefaultEntityServerConfiguration implements EntityServerConfiguration {

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
  DefaultEntityServerConfiguration(final int serverPort, final int registryPort) {
    this.serverConfiguration = ServerConfiguration.configuration(serverPort);
    this.registryPort = registryPort;
    this.serverConfiguration.setServerNameProvider(() -> {
      if (database == null) {
        throw new IllegalStateException("Database must be set before initializing server name");
      }

      return ServerConfiguration.SERVER_NAME_PREFIX.get() + " " +
              Versions.getVersionString() + "@" + database.getName().toUpperCase();
    });
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
  public Collection<String> getAuxiliaryServerProviderClassNames() {
    return serverConfiguration.getAuxiliaryServerProviderClassNames();
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
  public void setServerNameProvider(final Supplier<String> serverNameProvider) {
    serverConfiguration.setServerNameProvider(serverNameProvider);
  }

  @Override
  public void setServerName(final String serverName) {
    serverConfiguration.setServerName(serverName);
  }

  @Override
  public void setSharedLoginProxyClassNames(final Collection<String> sharedLoginProxyClassNames) {
    serverConfiguration.setSharedLoginProxyClassNames(sharedLoginProxyClassNames);
  }

  @Override
  public void setLoginProxyClassNames(final Collection<String> loginProxyClassNames) {
    serverConfiguration.setLoginProxyClassNames(loginProxyClassNames);
  }

  @Override
  public void setAuxiliaryServerProviderClassNames(final Collection<String> auxiliaryServerProviderClassNames) {
    serverConfiguration.setAuxiliaryServerProviderClassNames(auxiliaryServerProviderClassNames);
  }

  @Override
  public void setSslEnabled(final Boolean sslEnabled) {
    serverConfiguration.setSslEnabled(sslEnabled);
  }

  @Override
  public void setRmiClientSocketFactory(final RMIClientSocketFactory rmiClientSocketFactory) {
    serverConfiguration.setRmiClientSocketFactory(rmiClientSocketFactory);
  }

  @Override
  public void setRmiServerSocketFactory(final RMIServerSocketFactory rmiServerSocketFactory) {
    serverConfiguration.setRmiServerSocketFactory(rmiServerSocketFactory);
  }

  @Override
  public void setSerializationFilterWhitelist(final String serializationFilterWhitelist) {
    serverConfiguration.setSerializationFilterWhitelist(serializationFilterWhitelist);
  }

  @Override
  public void setSerializationFilterDryRun(final Boolean serializationFilterDryRun) {
    serverConfiguration.setSerializationFilterDryRun(serializationFilterDryRun);
  }

  @Override
  public void setAdminPort(final Integer adminPort) {
    this.serverAdminPort = requireNonNull(adminPort);
  }

  @Override
  public void setDatabase(final Database database) {
    this.database = requireNonNull(database);
  }

  @Override
  public void setAdminUser(final User adminUser) {
    this.adminUser = requireNonNull(adminUser);
  }

  @Override
  public void setConnectionLimit(final Integer connectionLimit) {
    this.connectionLimit = requireNonNull(connectionLimit);
  }

  @Override
  public void setClientLoggingEnabled(final Boolean clientLoggingEnabled) {
    this.clientLoggingEnabled = requireNonNull(clientLoggingEnabled);
  }

  @Override
  public void setConnectionTimeout(final Integer connectionTimeout) {
    this.connectionTimeout = requireNonNull(connectionTimeout);
  }

  @Override
  public void setConnectionPoolProvider(final String connectionPoolProvider) {
    this.connectionPoolProvider = requireNonNull(connectionPoolProvider);
  }

  @Override
  public void setDomainModelClassNames(final Collection<String> domainModelClassNames) {
    this.domainModelClassNames.addAll(requireNonNull(domainModelClassNames));
  }

  @Override
  public void setStartupPoolUsers(final Collection<User> startupPoolUsers) {
    this.startupPoolUsers.addAll(requireNonNull(startupPoolUsers));
  }

  @Override
  public void setClientSpecificConnectionTimeouts(final Map<String, Integer> clientSpecificConnectionTimeouts) {
    this.clientSpecificConnectionTimeouts.putAll(requireNonNull(clientSpecificConnectionTimeouts));
  }
}
