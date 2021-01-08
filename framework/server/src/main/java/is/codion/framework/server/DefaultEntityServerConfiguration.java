/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.common.version.Version;

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
   * @param serverPort the server port
   * @param registryPort the registry port
   */
  DefaultEntityServerConfiguration(final int serverPort, final int registryPort) {
    this.serverConfiguration = ServerConfiguration.configuration(serverPort, registryPort);
    this.serverConfiguration.setServerNameProvider(() -> {
      if (database == null) {
        throw new IllegalStateException("Database must be set before initializing server name");
      }

      return ServerConfiguration.SERVER_NAME_PREFIX.get() + " " +
              Version.getVersionString() + "@" + database.getName().toUpperCase();
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
  public Collection<String> getAuxiliaryServerFactoryClassNames() {
    return serverConfiguration.getAuxiliaryServerFactoryClassNames();
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
  public Integer getConnectionMaintenanceInterval() {
    return serverConfiguration.getConnectionMaintenanceInterval();
  }

  @Override
  public int getRegistryPort() {
    return serverConfiguration.getRegistryPort();
  }

  @Override
  public Integer getServerAdminPort() {
    return serverConfiguration.getServerAdminPort();
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
  public void setAuxiliaryServerFactoryClassNames(final Collection<String> auxiliaryServerFactoryClassNames) {
    serverConfiguration.setAuxiliaryServerFactoryClassNames(auxiliaryServerFactoryClassNames);
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
  public void setConnectionMaintenanceIntervalMs(final Integer connectionMaintenanceIntervalMs) {
    serverConfiguration.setConnectionMaintenanceIntervalMs(connectionMaintenanceIntervalMs);
  }

  @Override
  public void setServerAdminPort(final Integer adminPort) {
    serverConfiguration.setServerAdminPort(adminPort);
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
