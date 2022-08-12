/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  private int idleConnectionTimeout;
  private String connectionPoolProvider;
  private final Collection<String> domainModelClassNames = new HashSet<>();
  private final Collection<User> connectionPoolUsers = new HashSet<>();
  private final Map<String, Integer> clientTypeIdleConnectionTimeouts = new HashMap<>();

  DefaultEntityServerConfiguration(ServerConfiguration serverConfiguration) {
    this.serverConfiguration = requireNonNull(serverConfiguration);
  }

  @Override
  public String serverName() {
    return serverConfiguration.serverName();
  }

  @Override
  public int serverPort() {
    return serverConfiguration.serverPort();
  }

  @Override
  public Collection<String> auxiliaryServerFactoryClassNames() {
    return serverConfiguration.auxiliaryServerFactoryClassNames();
  }

  @Override
  public boolean isSslEnabled() {
    return serverConfiguration.isSslEnabled();
  }

  @Override
  public RMIClientSocketFactory rmiClientSocketFactory() {
    return serverConfiguration.rmiClientSocketFactory();
  }

  @Override
  public RMIServerSocketFactory rmiServerSocketFactory() {
    return serverConfiguration.rmiServerSocketFactory();
  }

  @Override
  public String serializationFilterWhitelist() {
    return serverConfiguration.serializationFilterWhitelist();
  }

  @Override
  public boolean isSerializationFilterDryRun() {
    return serverConfiguration.isSerializationFilterDryRun();
  }

  @Override
  public int connectionMaintenanceInterval() {
    return serverConfiguration.connectionMaintenanceInterval();
  }

  @Override
  public int registryPort() {
    return serverConfiguration.registryPort();
  }

  @Override
  public int serverAdminPort() {
    return serverConfiguration.serverAdminPort();
  }

  @Override
  public Database database() {
    return database;
  }

  @Override
  public User adminUser() {
    return adminUser;
  }

  @Override
  public int connectionLimit() {
    return connectionLimit;
  }

  @Override
  public boolean isClientLoggingEnabled() {
    return clientLoggingEnabled;
  }

  @Override
  public int idleConnectionTimeout() {
    return idleConnectionTimeout;
  }

  @Override
  public String connectionPoolProvider() {
    return connectionPoolProvider;
  }

  @Override
  public Collection<String> domainModelClassNames() {
    return domainModelClassNames;
  }

  @Override
  public Collection<User> connectionPoolUsers() {
    return connectionPoolUsers;
  }

  @Override
  public Map<String, Integer> clientTypeIdleConnectionTimeouts() {
    return clientTypeIdleConnectionTimeouts;
  }

  static final class DefaultBuilder implements Builder {

    private final ServerConfiguration.Builder<?> serverConfigurationBuilder;

    private Database database;
    private User adminUser;
    private int connectionLimit = DEFAULT_SERVER_CONNECTION_LIMIT;
    private boolean clientLoggingEnabled = false;
    private int clientConnectionTimeout = ServerConfiguration.IDLE_CONNECTION_TIMEOUT.get();
    private String connectionPoolProvider;
    private final Collection<String> domainModelClassNames = new HashSet<>();
    private final Collection<User> connectionPoolUsers = new HashSet<>();
    private final Map<String, Integer> clientTypeIdleConnectionTimeouts = new HashMap<>();

    DefaultBuilder(int serverPort, int registryPort) {
      serverConfigurationBuilder = ServerConfiguration.builder(serverPort, registryPort);
      serverConfigurationBuilder.serverNameProvider(() -> {
        if (database == null) {
          throw new IllegalStateException("Database must be set before initializing server name");
        }

        return ServerConfiguration.SERVER_NAME_PREFIX.get() + " " +
                Version.versionString() + "@" + database.name().toUpperCase();
      });
    }

    @Override
    public Builder serverNameProvider(Supplier<String> serverNameProvider) {
      serverConfigurationBuilder.serverNameProvider(serverNameProvider);
      return this;
    }

    @Override
    public Builder serverName(String serverName) {
      serverConfigurationBuilder.serverName(serverName);
      return this;
    }

    @Override
    public Builder auxiliaryServerFactoryClassNames(Collection<String> auxiliaryServerFactoryClassNames) {
      serverConfigurationBuilder.auxiliaryServerFactoryClassNames(auxiliaryServerFactoryClassNames);
      return this;
    }

    @Override
    public Builder sslEnabled(boolean sslEnabled) {
      serverConfigurationBuilder.sslEnabled(sslEnabled);
      return this;
    }

    @Override
    public Builder rmiClientSocketFactory(RMIClientSocketFactory rmiClientSocketFactory) {
      serverConfigurationBuilder.rmiClientSocketFactory(rmiClientSocketFactory);
      return this;
    }

    @Override
    public Builder rmiServerSocketFactory(RMIServerSocketFactory rmiServerSocketFactory) {
      serverConfigurationBuilder.rmiServerSocketFactory(rmiServerSocketFactory);
      return this;
    }

    @Override
    public Builder serializationFilterWhitelist(String serializationFilterWhitelist) {
      serverConfigurationBuilder.serializationFilterWhitelist(serializationFilterWhitelist);
      return this;
    }

    @Override
    public Builder serializationFilterDryRun(boolean serializationFilterDryRun) {
      serverConfigurationBuilder.serializationFilterDryRun(serializationFilterDryRun);
      return this;
    }

    @Override
    public Builder connectionMaintenanceIntervalMs(int connectionMaintenanceIntervalMs) {
      serverConfigurationBuilder.connectionMaintenanceIntervalMs(connectionMaintenanceIntervalMs);
      return this;
    }

    @Override
    public Builder adminPort(int adminPort) {
      serverConfigurationBuilder.adminPort(adminPort);
      return this;
    }

    @Override
    public Builder database(Database database) {
      this.database = requireNonNull(database);
      return this;
    }

    @Override
    public Builder adminUser(User adminUser) {
      this.adminUser = requireNonNull(adminUser);
      return this;
    }

    @Override
    public Builder connectionLimit(int connectionLimit) {
      this.connectionLimit = connectionLimit;
      return this;
    }

    @Override
    public Builder clientLoggingEnabled(boolean clientLoggingEnabled) {
      this.clientLoggingEnabled = clientLoggingEnabled;
      return this;
    }

    @Override
    public Builder idleConnectionTimeout(int idleConnectionTimeout) {
      this.clientConnectionTimeout = idleConnectionTimeout;
      return this;
    }

    @Override
    public Builder connectionPoolProvider(String connectionPoolProvider) {
      this.connectionPoolProvider = requireNonNull(connectionPoolProvider);
      return this;
    }

    @Override
    public Builder domainModelClassNames(Collection<String> domainModelClassNames) {
      this.domainModelClassNames.addAll(requireNonNull(domainModelClassNames));
      return this;
    }

    @Override
    public Builder connectionPoolUsers(Collection<User> connectionPoolUsers) {
      this.connectionPoolUsers.addAll(requireNonNull(connectionPoolUsers));
      return this;
    }

    @Override
    public Builder clientTypeIdleConnectionTimeouts(Map<String, Integer> clientTypeIdleConnectionTimeouts) {
      this.clientTypeIdleConnectionTimeouts.putAll(requireNonNull(clientTypeIdleConnectionTimeouts));
      return this;
    }

    @Override
    public EntityServerConfiguration build() {
      DefaultEntityServerConfiguration configuration = new DefaultEntityServerConfiguration(serverConfigurationBuilder.build());
      configuration.database = database;
      configuration.adminUser = adminUser;
      configuration.connectionLimit = connectionLimit;
      configuration.clientLoggingEnabled = clientLoggingEnabled;
      configuration.idleConnectionTimeout = clientConnectionTimeout;
      configuration.connectionPoolProvider = connectionPoolProvider;
      configuration.domainModelClassNames.addAll(domainModelClassNames);
      configuration.connectionPoolUsers.addAll(connectionPoolUsers);
      configuration.clientTypeIdleConnectionTimeouts.putAll(clientTypeIdleConnectionTimeouts);
      
      return configuration;
    }
  }
}
