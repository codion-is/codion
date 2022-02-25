/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  private int connectionLimit;
  private boolean clientLoggingEnabled;
  private int connectionTimeout;
  private String connectionPoolProvider;
  private final Collection<String> domainModelClassNames = new HashSet<>();
  private final Collection<User> startupPoolUsers = new HashSet<>();
  private final Map<String, Integer> clientSpecificConnectionTimeouts = new HashMap<>();

  DefaultEntityServerConfiguration(final ServerConfiguration serverConfiguration) {
    this.serverConfiguration = requireNonNull(serverConfiguration);
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
  public boolean isSslEnabled() {
    return serverConfiguration.isSslEnabled();
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
  public boolean isSerializationFilterDryRun() {
    return serverConfiguration.isSerializationFilterDryRun();
  }

  @Override
  public int getConnectionMaintenanceInterval() {
    return serverConfiguration.getConnectionMaintenanceInterval();
  }

  @Override
  public int getRegistryPort() {
    return serverConfiguration.getRegistryPort();
  }

  @Override
  public int getServerAdminPort() {
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
  public int getConnectionLimit() {
    return connectionLimit;
  }

  @Override
  public boolean getClientLoggingEnabled() {
    return clientLoggingEnabled;
  }

  @Override
  public int getConnectionTimeout() {
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

  static final class DefaultBuilder implements Builder {

    private final ServerConfiguration.Builder<?> serverConfigurationBuilder;

    private Database database;
    private User adminUser;
    private int connectionLimit = DEFAULT_SERVER_CONNECTION_LIMIT;
    private boolean clientLoggingEnabled = false;
    private int connectionTimeout = ServerConfiguration.DEFAULT_SERVER_CONNECTION_TIMEOUT;
    private String connectionPoolProvider;
    private final Collection<String> domainModelClassNames = new HashSet<>();
    private final Collection<User> startupPoolUsers = new HashSet<>();
    private final Map<String, Integer> clientSpecificConnectionTimeouts = new HashMap<>();

    DefaultBuilder(final int serverPort, final int registryPort) {
      serverConfigurationBuilder = ServerConfiguration.builder(serverPort, registryPort);
      serverConfigurationBuilder.serverNameProvider(() -> {
        if (database == null) {
          throw new IllegalStateException("Database must be set before initializing server name");
        }

        return ServerConfiguration.SERVER_NAME_PREFIX.get() + " " +
                Version.getVersionString() + "@" + database.getName().toUpperCase();
      });
    }

    @Override
    public Builder serverNameProvider(final Supplier<String> serverNameProvider) {
      serverConfigurationBuilder.serverNameProvider(serverNameProvider);
      return this;
    }

    @Override
    public Builder serverName(final String serverName) {
      serverConfigurationBuilder.serverName(serverName);
      return this;
    }

    @Override
    public Builder auxiliaryServerFactoryClassNames(final Collection<String> auxiliaryServerFactoryClassNames) {
      serverConfigurationBuilder.auxiliaryServerFactoryClassNames(auxiliaryServerFactoryClassNames);
      return this;
    }

    @Override
    public Builder sslEnabled(final boolean sslEnabled) {
      serverConfigurationBuilder.sslEnabled(sslEnabled);
      return this;
    }

    @Override
    public Builder rmiClientSocketFactory(final RMIClientSocketFactory rmiClientSocketFactory) {
      serverConfigurationBuilder.rmiClientSocketFactory(rmiClientSocketFactory);
      return this;
    }

    @Override
    public Builder rmiServerSocketFactory(final RMIServerSocketFactory rmiServerSocketFactory) {
      serverConfigurationBuilder.rmiServerSocketFactory(rmiServerSocketFactory);
      return this;
    }

    @Override
    public Builder serializationFilterWhitelist(final String serializationFilterWhitelist) {
      serverConfigurationBuilder.serializationFilterWhitelist(serializationFilterWhitelist);
      return this;
    }

    @Override
    public Builder serializationFilterDryRun(final boolean serializationFilterDryRun) {
      serverConfigurationBuilder.serializationFilterDryRun(serializationFilterDryRun);
      return this;
    }

    @Override
    public Builder connectionMaintenanceIntervalMs(final int connectionMaintenanceIntervalMs) {
      serverConfigurationBuilder.connectionMaintenanceIntervalMs(connectionMaintenanceIntervalMs);
      return this;
    }

    @Override
    public Builder adminPort(final int adminPort) {
      serverConfigurationBuilder.adminPort(adminPort);
      return this;
    }

    @Override
    public Builder database(final Database database) {
      this.database = requireNonNull(database);
      return this;
    }

    @Override
    public Builder adminUser(final User adminUser) {
      this.adminUser = requireNonNull(adminUser);
      return this;
    }

    @Override
    public Builder connectionLimit(final int connectionLimit) {
      this.connectionLimit = connectionLimit;
      return this;
    }

    @Override
    public Builder clientLoggingEnabled(final boolean clientLoggingEnabled) {
      this.clientLoggingEnabled = clientLoggingEnabled;
      return this;
    }

    @Override
    public Builder connectionTimeout(final int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    @Override
    public Builder connectionPoolProvider(final String connectionPoolProvider) {
      this.connectionPoolProvider = requireNonNull(connectionPoolProvider);
      return this;
    }

    @Override
    public Builder domainModelClassNames(final Collection<String> domainModelClassNames) {
      this.domainModelClassNames.addAll(requireNonNull(domainModelClassNames));
      return this;
    }

    @Override
    public Builder startupPoolUsers(final Collection<User> startupPoolUsers) {
      this.startupPoolUsers.addAll(requireNonNull(startupPoolUsers));
      return this;
    }

    @Override
    public Builder clientSpecificConnectionTimeouts(final Map<String, Integer> clientSpecificConnectionTimeouts) {
      this.clientSpecificConnectionTimeouts.putAll(requireNonNull(clientSpecificConnectionTimeouts));
      return this;
    }

    @Override
    public EntityServerConfiguration build() {
      DefaultEntityServerConfiguration configuration = new DefaultEntityServerConfiguration(serverConfigurationBuilder.build());
      configuration.database = database;
      configuration.adminUser = adminUser;
      configuration.connectionLimit = connectionLimit;
      configuration.clientLoggingEnabled = clientLoggingEnabled;
      configuration.connectionTimeout = connectionTimeout;
      configuration.connectionPoolProvider = connectionPoolProvider;
      configuration.domainModelClassNames.addAll(domainModelClassNames);
      configuration.startupPoolUsers.addAll(startupPoolUsers);
      configuration.clientSpecificConnectionTimeouts.putAll(clientSpecificConnectionTimeouts);
      
      return configuration;
    }
  }
}
