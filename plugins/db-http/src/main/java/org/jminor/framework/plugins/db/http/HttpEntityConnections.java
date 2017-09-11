/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.db.http;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Entities;

import java.util.UUID;

/**
 * A factory class providing http based EntityConnection instances.
 */
public final class HttpEntityConnections {

  private HttpEntityConnections() {}

  /**
   * Instantiates a new http based {@link EntityConnection} instance
   * @param domain the domain entities
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param user the user
   * @param clientId the client id
   */
  public static EntityConnection createConnection(final Entities domain, final String serverHostName, final int serverPort,
                                                  final User user, final UUID clientId) {
    return new DefaultHttpEntityConnection(domain, serverHostName, serverPort, user, clientId);
  }
}
