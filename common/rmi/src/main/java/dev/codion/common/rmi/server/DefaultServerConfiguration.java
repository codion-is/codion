/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.rmi.server;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link AbstractServer}.
 */
final class DefaultServerConfiguration implements ServerConfiguration {

  private final int serverPort;
  private final Collection<String> sharedLoginProxyClassNames = new ArrayList<>();
  private final Collection<String> loginProxyClassNames = new HashSet<>();
  private final Collection<String> auxiliaryServerClassNames = new HashSet<>();
  private boolean sslEnabled = true;
  private String serverName;
  private Supplier<String> serverNameProvider = () -> serverName;
  private RMIClientSocketFactory rmiClientSocketFactory = new SslRMIClientSocketFactory();
  private RMIServerSocketFactory rmiServerSocketFactory = new SslRMIServerSocketFactory();
  private String serializationFilterWhitelist;
  private Boolean serializationFilterDryRun = false;

  DefaultServerConfiguration(final int serverPort) {
    this.serverPort = serverPort;
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
  public Collection<String> getSharedLoginProxyClassNames() {
    return sharedLoginProxyClassNames;
  }

  @Override
  public Collection<String> getLoginProxyClassNames() {
    return loginProxyClassNames;
  }

  @Override
  public Collection<String> getAuxiliaryServerProviderClassNames() {
    return auxiliaryServerClassNames;
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
  public void setServerNameProvider(final Supplier<String> serverNameProvider) {
    this.serverNameProvider = requireNonNull(serverNameProvider);
  }

  @Override
  public void setServerName(final String serverName) {
    this.serverName = requireNonNull(serverName);
  }

  @Override
  public void setSharedLoginProxyClassNames(final Collection<String> sharedLoginProxyClassNames) {
    this.sharedLoginProxyClassNames.addAll(requireNonNull(sharedLoginProxyClassNames));
  }

  @Override
  public void setLoginProxyClassNames(final Collection<String> loginProxyClassNames) {
    this.loginProxyClassNames.addAll(requireNonNull(loginProxyClassNames));
  }

  @Override
  public void setAuxiliaryServerProviderClassNames(final Collection<String> auxiliaryServerProviderClassNames) {
    this.auxiliaryServerClassNames.addAll(requireNonNull(auxiliaryServerProviderClassNames));
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
}
