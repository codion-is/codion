/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.db.http;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.i18n.Messages;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

/**
 * A class responsible for managing a httpConnection entity connection.
 */
public final class HttpEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnectionProvider.class);

  private final String serverHostName;
  private final UUID clientId;

  /**
   * Instantiates a new HttpEntityConnectionProvider.
   * @param entities the domain model entities
   * @param serverHostName the server host name
   * @param user the user to use when initializing connections
   * @param clientId a UUID identifying the client
   */
  public HttpEntityConnectionProvider(final Entities entities, final String serverHostName, final User user, final UUID clientId) {
    super(entities, user, false);
    this.serverHostName = Objects.requireNonNull(serverHostName, "serverHostName");
    this.clientId = Objects.requireNonNull(clientId, "clientId");
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnection.Type getConnectionType() {
    return EntityConnection.Type.HTTP;
  }

  /**
   * @return a string describing the server connection
   */
  @Override
  public String getDescription() {
    if (!isConnectionValid()) {
      return serverHostName + " - " + Messages.get(Messages.NOT_CONNECTED);
    }

    return serverHostName;
  }

  /** {@inheritDoc} */
  @Override
  public String getServerHostName() {
    return serverHostName;
  }

  /**
   * @return the client ID
   */
  public UUID getClientId() {
    return clientId;
  }

  /** {@inheritDoc} */
  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());

      final DefaultHttpEntityConnection connection = new DefaultHttpEntityConnection(getEntities(), getUser());

      return Util.initializeProxy(EntityConnection.class, new HttpEntityConnectionHandler(connection));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void disconnect(final EntityConnection connection) {
    connection.disconnect();
  }

  private static final class HttpEntityConnectionHandler implements InvocationHandler {
    private final HttpEntityConnection httpConnection;

    private HttpEntityConnectionHandler(final HttpEntityConnection httpConnection) {
      this.httpConnection = httpConnection;
    }

    @Override
    public synchronized Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      if (methodName.equals(IS_CONNECTED)) {
        return isConnected();
      }

      final Method remoteMethod = HttpEntityConnection.class.getMethod(methodName, method.getParameterTypes());
      try {
        return remoteMethod.invoke(httpConnection, args);
      }
      catch (final InvocationTargetException e) {
        final Exception exception = (Exception) e.getCause();
        LOG.error(exception.getMessage(), exception);
        throw exception;
      }
      catch (final Exception e) {
        LOG.error(e.getMessage(), e);
        throw e;
      }
    }

    private Object isConnected() {
      try {
        return httpConnection.isConnected();
      }
      catch (final IOException e) {
        return false;
      }
    }
  }
}
