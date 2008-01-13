/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.User;
import org.jminor.framework.FrameworkConstants;

import java.util.Random;

/**
 * A factory class for handing out IEntityDbProviders according to system properties
 */
public class EntityDbProviderFactory {

  /**
   * Returns a IEntityDbProvider according to system properties
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @return a IEntityDbProvider
   */
  public static IEntityDbProvider createEntityDbProvider(final User user, final String clientTypeID) {
    return createEntityDbProvider(user, Long.toOctalString(new Random().nextLong()), clientTypeID);
  }

  /**
   * Returns a remote or local IEntityDbProvider according to system properties.
   * Loads classes by name, so these need to available on the classpath
   * @param user the user for the connection
   * @param clientKey a unique client key
   * @param clientTypeID the client type id
   * @return a IEntityDbProvider
   * @see FrameworkConstants#CLIENT_CONNECTION_TYPE
   * @see EntityDbProvider
   * @see org.jminor.framework.server.EntityDbRemoteProvider
   */
  public static IEntityDbProvider createEntityDbProvider(final User user, final String clientKey,
                                                         final String clientTypeID) {
    try {
      if (System.getProperty(FrameworkConstants.CLIENT_CONNECTION_TYPE,
              FrameworkConstants.CONNECTION_TYPE_LOCAL).equals(FrameworkConstants.CONNECTION_TYPE_REMOTE))
        return (IEntityDbProvider) Class.forName("org.jminor.framework.server.EntityDbRemoteProvider").getConstructor(User.class, String.class, String.class).newInstance(user, clientKey, clientTypeID);
      else
        return (IEntityDbProvider) Class.forName("org.jminor.framework.db.EntityDbProvider").getConstructor(User.class).newInstance(user);
    }
    catch (Exception e) {
      throw new RuntimeException("Exception while initializing db provider", e);
    }
  }
}
