/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;

import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.util.UUID;

/**
 * A factory class providing http based EntityConnection instances.
 */
public final class HttpEntityConnections {

  private HttpEntityConnections() {}

  /**
   * Instantiates a new http based {@link EntityConnection} instance
   * @param domainTypeName the name of the domain model type
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   * @param json true if json serialization should be used
   * @return a http based EntityConnection
   */
  public static EntityConnection createConnection(String domainTypeName, String serverHostName,
                                                  int serverPort, User user, String clientTypeId,
                                                  UUID clientId, boolean json) {
    return json ?
            new HttpJsonEntityConnection(domainTypeName, serverHostName, serverPort, ClientHttps.FALSE, user, clientTypeId, clientId, new BasicHttpClientConnectionManager()) :
            new HttpEntityConnection(domainTypeName, serverHostName, serverPort, ClientHttps.FALSE, user, clientTypeId, clientId, new BasicHttpClientConnectionManager());
  }

  /**
   * Instantiates a new https based {@link EntityConnection} instance
   * @param domainTypeName the name of the domain model type
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   * @param json true if json serialization should be used
   * @return a http based EntityConnection
   */
  public static EntityConnection createSecureConnection(String domainTypeName, String serverHostName,
                                                        int serverPort, User user, String clientTypeId,
                                                        UUID clientId, boolean json) {
    return json ?
            new HttpJsonEntityConnection(domainTypeName, serverHostName, serverPort, ClientHttps.TRUE, user, clientTypeId, clientId, new BasicHttpClientConnectionManager()) :
            new HttpEntityConnection(domainTypeName, serverHostName, serverPort, ClientHttps.TRUE, user, clientTypeId, clientId, new BasicHttpClientConnectionManager());
  }
}
