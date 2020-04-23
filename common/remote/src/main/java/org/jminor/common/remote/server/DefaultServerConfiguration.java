/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

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
  private String serverName;
  private Supplier<String> serverNameProvider = new Supplier<String>() {
    @Override
    public String get() {
      return serverName;
    }
  };
  private RMIClientSocketFactory rmiClientSocketFactory;
  private RMIServerSocketFactory rmiServerSocketFactory;

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
  public RMIClientSocketFactory getRmiClientSocketFactory() {
    return rmiClientSocketFactory;
  }

  @Override
  public RMIServerSocketFactory getRmiServerSocketFactory() {
    return rmiServerSocketFactory;
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
  public DefaultServerConfiguration setRmiClientSocketFactory(final RMIClientSocketFactory rmiClientSocketFactory) {
    this.rmiClientSocketFactory = requireNonNull(rmiClientSocketFactory);
    return this;
  }

  @Override
  public DefaultServerConfiguration setRmiServerSocketFactory(final RMIServerSocketFactory rmiServerSocketFactory) {
    this.rmiServerSocketFactory = requireNonNull(rmiServerSocketFactory);
    return this;
  }
}
