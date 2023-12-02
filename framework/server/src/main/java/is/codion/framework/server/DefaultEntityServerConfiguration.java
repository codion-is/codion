/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link EntityServer}.
 */
final class DefaultEntityServerConfiguration implements EntityServerConfiguration {

  private final ServerConfiguration serverConfiguration;

  private final Database database;
  private final User adminUser;
  private final int connectionLimit;
  private final boolean clientLogging;
  private final int idleConnectionTimeout;
  private final String connectionPoolFactory;
  private final Collection<String> domainClassNames;
  private final Collection<User> connectionPoolUsers;
  private final Map<String, Integer> clientTypeIdleConnectionTimeouts;

  DefaultEntityServerConfiguration(DefaultEntityServerConfiguration.DefaultBuilder builder) {
    this.serverConfiguration = requireNonNull(builder.serverConfigurationBuilder.build());
    this.database = builder.database;
    this.adminUser = builder.adminUser;
    this.connectionLimit = builder.connectionLimit;
    this.clientLogging = builder.clientLogging;
    this.idleConnectionTimeout = builder.idleConnectionTimeout;
    this.connectionPoolFactory = builder.connectionPoolFactory;
    this.domainClassNames = unmodifiableCollection(builder.domainClassNames);
    this.connectionPoolUsers = unmodifiableCollection(builder.connectionPoolUsers);
    this.clientTypeIdleConnectionTimeouts = unmodifiableMap(builder.clientTypeIdleConnectionTimeouts);
  }

  @Override
  public String serverName() {
    return serverConfiguration.serverName();
  }

  @Override
  public int port() {
    return serverConfiguration.port();
  }

  @Override
  public Collection<String> auxiliaryServerFactoryClassNames() {
    return serverConfiguration.auxiliaryServerFactoryClassNames();
  }

  @Override
  public boolean sslEnabled() {
    return serverConfiguration.sslEnabled();
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
  public boolean serializationFilterDryRun() {
    return serverConfiguration.serializationFilterDryRun();
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
  public int adminPort() {
    return serverConfiguration.adminPort();
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
  public boolean clientLogging() {
    return clientLogging;
  }

  @Override
  public int idleConnectionTimeout() {
    return idleConnectionTimeout;
  }

  @Override
  public String connectionPoolFactory() {
    return connectionPoolFactory;
  }

  @Override
  public Collection<String> domainClassNames() {
    return domainClassNames;
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
    private int connectionLimit = DEFAULT_CONNECTION_LIMIT;
    private boolean clientLogging = CLIENT_LOGGING.get();
    private int idleConnectionTimeout = ServerConfiguration.IDLE_CONNECTION_TIMEOUT.get();
    private String connectionPoolFactory;
    private final Collection<String> domainClassNames = new HashSet<>();
    private final Collection<User> connectionPoolUsers = new HashSet<>();
    private final Map<String, Integer> clientTypeIdleConnectionTimeouts = new HashMap<>();

    DefaultBuilder(int serverPort, int registryPort) {
      serverConfigurationBuilder = ServerConfiguration.builder(serverPort, registryPort);
      serverConfigurationBuilder.serverName(() -> {
        if (database == null) {
          throw new IllegalStateException("Database must be set before initializing server name");
        }

        return ServerConfiguration.SERVER_NAME_PREFIX.get() + " " +
                Version.versionString() + "@" + database.name().toUpperCase();
      });
    }

    @Override
    public Builder serverName(Supplier<String> serverNameSupplier) {
      serverConfigurationBuilder.serverName(serverNameSupplier);
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
    public Builder clientLogging(boolean clientLogging) {
      this.clientLogging = clientLogging;
      return this;
    }

    @Override
    public Builder idleConnectionTimeout(int idleConnectionTimeout) {
      this.idleConnectionTimeout = idleConnectionTimeout;
      return this;
    }

    @Override
    public Builder connectionPoolFactory(String connectionPoolFactory) {
      this.connectionPoolFactory = requireNonNull(connectionPoolFactory);
      return this;
    }

    @Override
    public Builder domainClassNames(Collection<String> domainClassNames) {
      this.domainClassNames.addAll(requireNonNull(domainClassNames));
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
      return new DefaultEntityServerConfiguration(this);
    }
  }
}
