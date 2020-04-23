/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link Server}.
 */
public interface ServerConfiguration {

  /**
   * @return the server name
   * @see #initializeServerName()
   */
  String getServerName();

  /**
   * @return the server port
   */
  int getServerPort();

  /**
   * @return the shared login proxy classnames
   */
  Collection<String> getSharedLoginProxyClassNames();

  /**
   * @return the login proxy classnames
   */
  Collection<String> getLoginProxyClassNames();

  /**
   * @return the connection validator classnames
   */
  Collection<String> getConnectionValidatorClassNames();

  /**
   * @return the rmi client socket factory to use, null for default
   */
  RMIClientSocketFactory getRmiClientSocketFactory();

  /**
   * @return the rmi server socket factory to use, null for default
   */
  RMIServerSocketFactory getRmiServerSocketFactory();

  /**
   * @param serverNameProvider the server name provider
   * @return this configuration instance
   */
  ServerConfiguration setServerNameProvider(Supplier<String> serverNameProvider);

  /**
   * @param serverName the server name
   * @return this configuration instance
   */
  ServerConfiguration setServerName(String serverName);

  /**
   * @param sharedLoginProxyClassNames the shared login proxy classnames
   * @return this configuration instance
   */
  ServerConfiguration setSharedLoginProxyClassNames(Collection<String> sharedLoginProxyClassNames);

  /**
   * @param loginProxyClassNames the login proxy classes to initialize on startup
   * @return this configuration instance
   */
  ServerConfiguration setLoginProxyClassNames(Collection<String> loginProxyClassNames);

  /**
   * @param connectionValidatorClassNames the connection validation classes to initialize on startup
   * @return this configuration instance
   */
  ServerConfiguration setConnectionValidatorClassNames(Collection<String> connectionValidatorClassNames);

  /**
   * @param rmiClientSocketFactory the rmi client socket factory to use
   * @return this configuration instance
   */
  ServerConfiguration setRmiClientSocketFactory(RMIClientSocketFactory rmiClientSocketFactory);

  /**
   * @param rmiServerSocketFactory the rmi server socket factory to use
   * @return this configuration instance
   */
  ServerConfiguration setRmiServerSocketFactory(RMIServerSocketFactory rmiServerSocketFactory);

  /**
   * @param serverPort the server port
   * @return a default server configuration
   */
  static ServerConfiguration configuration(final int serverPort) {
    return new DefaultServerConfiguration(serverPort);
  }

  /**
   * @return a configuration according to system properties.
   */
  static DefaultServerConfiguration fromSystemProperties() {
    return new DefaultServerConfiguration(requireNonNull(Server.SERVER_PORT.get(), Server.SERVER_PORT.getProperty()));
  }
}
