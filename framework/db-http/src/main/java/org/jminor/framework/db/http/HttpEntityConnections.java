/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.http;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnection;

import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.util.UUID;

/**
 * A factory class providing http based EntityConnection instances.
 */
public final class HttpEntityConnections {

  private HttpEntityConnections() {}

  /**
   * Instantiates a new http based {@link EntityConnection} instance
   * @param domainId the id of the domain entities
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param httpsEnabled true if https should be used
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   * @return a http based EntityConnection
   */
  public static HttpEntityConnection createConnection(final String domainId, final String serverHostName,
                                                      final int serverPort, final boolean httpsEnabled, final User user,
                                                      final String clientTypeId, final UUID clientId) {
    return new HttpEntityConnection(domainId, serverHostName, serverPort, httpsEnabled, user, clientTypeId, clientId,
            new BasicHttpClientConnectionManager());
  }
}
