/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.registry.Registry;
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
  private Integer serverAdminPort;
  private boolean sslEnabled = true;
  private String serverName;
  private Supplier<String> serverNameProvider = () -> serverName;
  private RMIClientSocketFactory rmiClientSocketFactory = new SslRMIClientSocketFactory();
  private RMIServerSocketFactory rmiServerSocketFactory = new SslRMIServerSocketFactory();
  private String serializationFilterWhitelist;
  private Boolean serializationFilterDryRun = false;
  private Integer connectionMaintenanceIntervalMs = DEFAULT_CONNECTION_MAINTENANCE_INTERVAL;

  DefaultServerConfiguration(final int serverPort) {
    this(serverPort, Registry.REGISTRY_PORT);
  }

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
  public Integer getServerAdminPort() {
    return serverAdminPort;
  }

  @Override
  public Collection<String> getAuxiliaryServerFactoryClassNames() {
    return auxiliaryServerFactoryClassNames;
  }

  @Override
  public Boolean getSslEnabled() {
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
  public Boolean getSerializationFilterDryRun() {
    return serializationFilterDryRun;
  }

  @Override
  public Integer getConnectionMaintenanceInterval() {
    return connectionMaintenanceIntervalMs;
  }

  @Override
  public void setServerAdminPort(final Integer adminPort) {
    this.serverAdminPort = requireNonNull(adminPort);
  }

  @Override
  public void setServerNameProvider(final Supplier<String> serverNameProvider) {
    this.serverNameProvider = requireNonNull(serverNameProvider);
  }

  @Override
  public void setServerName(final String serverName) {
    this.serverName = requireNonNull(serverName);
  }

  @Override
  public void setAuxiliaryServerFactoryClassNames(final Collection<String> auxiliaryServerFactoryClassNames) {
    this.auxiliaryServerFactoryClassNames.addAll(requireNonNull(auxiliaryServerFactoryClassNames));
  }

  @Override
  public void setSslEnabled(final Boolean sslEnabled) {
    this.sslEnabled = requireNonNull(sslEnabled);
    if (sslEnabled) {
      setRmiClientSocketFactory(new SslRMIClientSocketFactory());
      setRmiServerSocketFactory(new SslRMIServerSocketFactory());
    }
    else {
      setRmiClientSocketFactory(null);
      setRmiServerSocketFactory(null);
    }
  }

  @Override
  public void setRmiClientSocketFactory(final RMIClientSocketFactory rmiClientSocketFactory) {
    this.rmiClientSocketFactory = rmiClientSocketFactory;
  }

  @Override
  public void setRmiServerSocketFactory(final RMIServerSocketFactory rmiServerSocketFactory) {
    this.rmiServerSocketFactory = rmiServerSocketFactory;
  }

  @Override
  public void setSerializationFilterWhitelist(final String serializationFilterWhitelist) {
    this.serializationFilterWhitelist = requireNonNull(serializationFilterWhitelist);
  }

  @Override
  public void setSerializationFilterDryRun(final Boolean serializationFilterDryRun) {
    this.serializationFilterDryRun = requireNonNull(serializationFilterDryRun);
  }

  @Override
  public void setConnectionMaintenanceIntervalMs(final Integer connectionMaintenanceIntervalMs) {
    this.connectionMaintenanceIntervalMs = connectionMaintenanceIntervalMs;
  }
}
