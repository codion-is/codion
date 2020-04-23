/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

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
  private final Collection<String> connectionValidatorClassNames = new HashSet<>();
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
  public Collection<String> getConnectionValidatorClassNames() {
    return connectionValidatorClassNames;
  }

  @Override
  public Collection<String> getAuxiliaryServerClassNames() {
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
  public DefaultServerConfiguration setServerNameProvider(final Supplier<String> serverNameProvider) {
    this.serverNameProvider = requireNonNull(serverNameProvider);
    return this;
  }

  @Override
  public DefaultServerConfiguration setServerName(final String serverName) {
    this.serverName = requireNonNull(serverName);
    return this;
  }

  @Override
  public DefaultServerConfiguration setSharedLoginProxyClassNames(final Collection<String> sharedLoginProxyClassNames) {
    this.sharedLoginProxyClassNames.addAll(requireNonNull(sharedLoginProxyClassNames));
    return this;
  }

  @Override
  public DefaultServerConfiguration setLoginProxyClassNames(final Collection<String> loginProxyClassNames) {
    this.loginProxyClassNames.addAll(requireNonNull(loginProxyClassNames));
    return this;
  }

  @Override
  public DefaultServerConfiguration setConnectionValidatorClassNames(final Collection<String> connectionValidatorClassNames) {
    this.connectionValidatorClassNames.addAll(requireNonNull(connectionValidatorClassNames));
    return this;
  }

  @Override
  public ServerConfiguration setAuxiliaryServerClassNames(final Collection<String> auxiliaryServerClassNames) {
    this.auxiliaryServerClassNames.addAll(requireNonNull(auxiliaryServerClassNames));
    return this;
  }

  @Override
  public ServerConfiguration setSslEnabled(final Boolean sslEnabled) {
    this.sslEnabled = requireNonNull(sslEnabled);
    if (sslEnabled) {
      setRmiClientSocketFactory(new SslRMIClientSocketFactory());
      setRmiServerSocketFactory(new SslRMIServerSocketFactory());
    }
    else {
      setRmiClientSocketFactory(null);
      setRmiServerSocketFactory(null);
    }
    return this;
  }

  @Override
  public DefaultServerConfiguration setRmiClientSocketFactory(final RMIClientSocketFactory rmiClientSocketFactory) {
    this.rmiClientSocketFactory = rmiClientSocketFactory;
    return this;
  }

  @Override
  public DefaultServerConfiguration setRmiServerSocketFactory(final RMIServerSocketFactory rmiServerSocketFactory) {
    this.rmiServerSocketFactory = rmiServerSocketFactory;
    return this;
  }

  @Override
  public ServerConfiguration setSerializationFilterWhitelist(final String serializationFilterWhitelist) {
    this.serializationFilterWhitelist = requireNonNull(serializationFilterWhitelist);
    return this;
  }

  @Override
  public ServerConfiguration setSerializationFilterDryRun(final Boolean serializationFilterDryRun) {
    this.serializationFilterDryRun = requireNonNull(serializationFilterDryRun);
    return this;
  }
}
