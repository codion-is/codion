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
public final class AbstractServerConfiguration {

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

  /**
   * @param serverPort the port on which to make the server accessible
   */
  public AbstractServerConfiguration(final int serverPort) {
    this.serverPort = serverPort;
  }

  /**
   * @return a configuration according to system properties.
   */
  public static AbstractServerConfiguration fromSystemProperties() {
    return new AbstractServerConfiguration(requireNonNull(Server.SERVER_PORT.get(), Server.SERVER_PORT.getProperty()));
  }

  /**
   * @return the server name
   * @see #initializeServerName()
   */
  public String getServerName() {
    if (serverName == null) {
      serverName = serverNameProvider.get();
    }

    return serverName;
  }

  /**
   * @return the server port
   */
  public int getServerPort() {
    return serverPort;
  }

  /**
   * @return the shared login proxy classnames
   */
  public Collection<String> getSharedLoginProxyClassNames() {
    return sharedLoginProxyClassNames;
  }

  /**
   * @return the login proxy classnames
   */
  public Collection<String> getLoginProxyClassNames() {
    return loginProxyClassNames;
  }

  /**
   * @return the connection validator classnames
   */
  public Collection<String> getConnectionValidatorClassNames() {
    return connectionValidatorClassNames;
  }

  /**
   * @return the rmi client socket factory to use, null for default
   */
  public RMIClientSocketFactory getRmiClientSocketFactory() {
    return rmiClientSocketFactory;
  }

  /**
   * @return the rmi server socket factory to use, null for default
   */
  public RMIServerSocketFactory getRmiServerSocketFactory() {
    return rmiServerSocketFactory;
  }

  /**
   * @param serverNameProvider the server name provider
   * @return this configuration instance
   */
  public AbstractServerConfiguration setServerNameProvider(final Supplier<String> serverNameProvider) {
    this.serverNameProvider = requireNonNull(serverNameProvider);
    return this;
  }

  /**
   * @param serverName the server name
   * @return this configuration instance
   */
  public AbstractServerConfiguration setServerName(final String serverName) {
    this.serverName = requireNonNull(serverName);
    return this;
  }

  /**
   * @param sharedLoginProxyClassNames the shared login proxy classnames
   * @return this configuration instance
   */
  public AbstractServerConfiguration setSharedLoginProxyClassNames(final Collection<String> sharedLoginProxyClassNames) {
    this.sharedLoginProxyClassNames.addAll(requireNonNull(sharedLoginProxyClassNames));
    return this;
  }

  /**
   * @param loginProxyClassNames the login proxy classes to initialize on startup
   * @return this configuration instance
   */
  public AbstractServerConfiguration setLoginProxyClassNames(final Collection<String> loginProxyClassNames) {
    this.loginProxyClassNames.addAll(requireNonNull(loginProxyClassNames));
    return this;
  }

  /**
   * @param connectionValidatorClassNames the connection validation classes to initialize on startup
   * @return this configuration instance
   */
  public AbstractServerConfiguration setConnectionValidatorClassNames(final Collection<String> connectionValidatorClassNames) {
    this.connectionValidatorClassNames.addAll(requireNonNull(connectionValidatorClassNames));
    return this;
  }

  /**
   * @param rmiClientSocketFactory the rmi client socket factory to use
   * @return this configuration instance
   */
  public AbstractServerConfiguration setRmiClientSocketFactory(final RMIClientSocketFactory rmiClientSocketFactory) {
    this.rmiClientSocketFactory = requireNonNull(rmiClientSocketFactory);
    return this;
  }

  /**
   * @param rmiServerSocketFactory the rmi server socket factory to use
   * @return this configuration instance
   */
  public AbstractServerConfiguration setRmiServerSocketFactory(final RMIServerSocketFactory rmiServerSocketFactory) {
    this.rmiServerSocketFactory = requireNonNull(rmiServerSocketFactory);
    return this;
  }
}
