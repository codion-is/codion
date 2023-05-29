/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;

import java.util.UUID;

/**
 * A factory class for http based EntityConnection builder.
 */
public interface HttpEntityConnection extends EntityConnection {

  /**
   * @return a new builder instance
   */
  static Builder builder() {
    return new DefaultHttpEntityConnection.DefaultBuilder();
  }

  /**
   * Builds a http based EntityConnection
   */
  interface Builder {

    /**
     * @param domainTypeName the name of the domain model type
     * @return this builder instance
     */
    Builder domainTypeName(String domainTypeName);

    /**
     * @param serverHostName the http server host name
     * @return this builder instance
     */
    Builder serverHostName(String serverHostName);

    /**
     * @param serverPort the http server port
     * @return this builder instance
     */
    Builder serverPort(int serverPort);

    /**
     * @param https true if https should be used
     * @return this builder instance
     */
    Builder https(boolean https);

    /**
     * @param json true if json serialization should be used
     * @return this builder instance
     */
    Builder json(boolean json);

    /**
     * @param user the user
     * @return this builder instance
     */
    Builder user(User user);

    /**
     * @param clientTypeId the client type id
     * @return this builder instance
     */
    Builder clientTypeId(String clientTypeId);

    /**
     * @param clientId the client id
     * @return this builder instance
     */
    Builder clientId(UUID clientId);

    /**
     * @return a http based EntityConnection
     */
    EntityConnection build();
  }
}
