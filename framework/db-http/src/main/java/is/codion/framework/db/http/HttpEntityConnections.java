/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.identity.DomainIdentity;

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
  public static HttpEntityConnection createConnection(final DomainIdentity domainId, final String serverHostName,
                                                      final int serverPort, final User user,
                                                      final String clientTypeId, final UUID clientId) {
    return new HttpEntityConnection(domainId, serverHostName, serverPort, ClientHttps.FALSE, user, clientTypeId, clientId,
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
  public static HttpEntityConnection createSecureConnection(final DomainIdentity domainId, final String serverHostName,
                                                            final int serverPort, final User user,
                                                            final String clientTypeId, final UUID clientId) {
    return new HttpEntityConnection(domainId, serverHostName, serverPort, ClientHttps.TRUE, user, clientTypeId, clientId,
            new BasicHttpClientConnectionManager());
  }
}
