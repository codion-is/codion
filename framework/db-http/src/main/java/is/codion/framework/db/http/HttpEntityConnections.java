/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.http;

import dev.codion.common.user.User;
import dev.codion.framework.db.EntityConnection;

import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.util.UUID;

/**
 * A factory class providing http based EntityConnection instances.
 */
public final class HttpEntityConnections {

  private HttpEntityConnections() {}

  /**
   * Instantiates a new http based {@link EntityConnection} instance
   * @param domainId the id of the domain model
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   * @return a http based EntityConnection
   */
  public static HttpEntityConnection createConnection(final String domainId, final String serverHostName,
                                                      final int serverPort, final User user,
                                                      final String clientTypeId, final UUID clientId) {
    return new HttpEntityConnection(domainId, serverHostName, serverPort, false, user, clientTypeId, clientId,
            new BasicHttpClientConnectionManager());
  }

  /**
   * Instantiates a new https based {@link EntityConnection} instance
   * @param domainId the id of the domain model
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   * @return a http based EntityConnection
   */
  public static HttpEntityConnection createSecureConnection(final String domainId, final String serverHostName,
                                                            final int serverPort, final User user,
                                                            final String clientTypeId, final UUID clientId) {
    return new HttpEntityConnection(domainId, serverHostName, serverPort, true, user, clientTypeId, clientId,
            new BasicHttpClientConnectionManager());
  }
}
