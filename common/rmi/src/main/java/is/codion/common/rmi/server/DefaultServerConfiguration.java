/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link AbstractServer}.
 */
final class DefaultServerConfiguration implements ServerConfiguration {

  private final int serverPort;
  private final int registryPort;
  private final Collection<String> auxiliaryServerFactoryClassNames = new HashSet<>();
  private int serverAdminPort;
  private boolean sslEnabled;
  private String serverName;
  private Supplier<String> serverNameProvider = () -> serverName;
  private RMIClientSocketFactory rmiClientSocketFactory;
  private RMIServerSocketFactory rmiServerSocketFactory;
  private String serializationFilterWhitelist;
  private boolean serializationFilterDryRun;
  private int connectionMaintenanceIntervalMs;

  DefaultServerConfiguration(final int serverPort, final int registryPort) {
    this.serverPort = serverPort;
    this.registryPort = registryPort;
  }

  @Override
  public String getServerName() {
    if (serverName == null) {
      serverName = serverNameProvider.get();
    }

    return serverName;
  }

  @Override
  public int getServerPort() {
    return serverPort;
  }

  @Override
  public int getRegistryPort() {
    return registryPort;
  }

  @Override
  public int getServerAdminPort() {
    return serverAdminPort;
  }

  @Override
  public Collection<String> getAuxiliaryServerFactoryClassNames() {
    return auxiliaryServerFactoryClassNames;
  }

  @Override
  public boolean isSslEnabled() {
    return sslEnabled;
  }

  @Override
  public RMIClientSocketFactory getRmiClientSocketFactory() {
    return rmiClientSocketFactory;
  }

  @Override
  public RMIServerSocketFactory getRmiServerSocketFactory() {
    return rmiServerSocketFactory;
  }

  @Override
  public String getSerializationFilterWhitelist() {
    return serializationFilterWhitelist;
  }

  @Override
  public boolean isSerializationFilterDryRun() {
    return serializationFilterDryRun;
  }

  @Override
  public int getConnectionMaintenanceInterval() {
    return connectionMaintenanceIntervalMs;
  }

  static final class DefaultBuilder implements Builder<DefaultBuilder> {

    private final int serverPort;
    private final int registryPort;
    private final Collection<String> auxiliaryServerFactoryClassNames = new HashSet<>();
    private int serverAdminPort;
    private boolean sslEnabled = true;
    private String serverName;
    private Supplier<String> serverNameProvider;
    private RMIClientSocketFactory rmiClientSocketFactory = new SslRMIClientSocketFactory();
    private RMIServerSocketFactory rmiServerSocketFactory = new SslRMIServerSocketFactory();
    private String serializationFilterWhitelist;
    private Boolean serializationFilterDryRun = false;
    private Integer connectionMaintenanceIntervalMs = DEFAULT_CONNECTION_MAINTENANCE_INTERVAL;

    DefaultBuilder(final int serverPort, final int registryPort) {
      this.serverPort = serverPort;
      this.registryPort = registryPort;
    }

    @Override
    public DefaultBuilder adminPort(final int adminPort) {
      this.serverAdminPort = adminPort;
      return this;
    }

    @Override
    public DefaultBuilder serverNameProvider(final Supplier<String> serverNameProvider) {
      this.serverNameProvider = requireNonNull(serverNameProvider);
      return this;
    }

    @Override
    public DefaultBuilder serverName(final String serverName) {
      this.serverName = requireNonNull(serverName);
      return this;
    }

    @Override
    public DefaultBuilder auxiliaryServerFactoryClassNames(final Collection<String> auxiliaryServerFactoryClassNames) {
      this.auxiliaryServerFactoryClassNames.addAll(requireNonNull(auxiliaryServerFactoryClassNames));
      return this;
    }

    @Override
    public DefaultBuilder sslEnabled(final boolean sslEnabled) {
      this.sslEnabled = sslEnabled;
      if (sslEnabled) {
        rmiClientSocketFactory(new SslRMIClientSocketFactory());
        rmiServerSocketFactory(new SslRMIServerSocketFactory());
      }
      else {
        rmiClientSocketFactory(null);
        rmiServerSocketFactory(null);
      }
      return this;
    }

    @Override
    public DefaultBuilder rmiClientSocketFactory(final RMIClientSocketFactory rmiClientSocketFactory) {
      this.rmiClientSocketFactory = rmiClientSocketFactory;
      return this;
    }

    @Override
    public DefaultBuilder rmiServerSocketFactory(final RMIServerSocketFactory rmiServerSocketFactory) {
      this.rmiServerSocketFactory = rmiServerSocketFactory;
      return this;
    }

    @Override
    public DefaultBuilder serializationFilterWhitelist(final String serializationFilterWhitelist) {
      this.serializationFilterWhitelist = requireNonNull(serializationFilterWhitelist);
      return this;
    }

    @Override
    public DefaultBuilder serializationFilterDryRun(final boolean serializationFilterDryRun) {
      this.serializationFilterDryRun = serializationFilterDryRun;
      return this;
    }

    @Override
    public DefaultBuilder connectionMaintenanceIntervalMs(final int connectionMaintenanceIntervalMs) {
      this.connectionMaintenanceIntervalMs = connectionMaintenanceIntervalMs;
      return this;
    }

    @Override
    public ServerConfiguration build() {
      final DefaultServerConfiguration configuration = new DefaultServerConfiguration(serverPort, registryPort);
      configuration.auxiliaryServerFactoryClassNames.addAll(auxiliaryServerFactoryClassNames);
      configuration.serverAdminPort = serverAdminPort;
      configuration.sslEnabled = sslEnabled;
      configuration.serverName = serverName;
      configuration.serverNameProvider = serverNameProvider;
      configuration.rmiClientSocketFactory = rmiClientSocketFactory;
      configuration.rmiServerSocketFactory = rmiServerSocketFactory;
      configuration.serializationFilterWhitelist = serializationFilterWhitelist;
      configuration.serializationFilterDryRun = serializationFilterDryRun;
      configuration.connectionMaintenanceIntervalMs = connectionMaintenanceIntervalMs;

      return configuration;
    }
  }
}
