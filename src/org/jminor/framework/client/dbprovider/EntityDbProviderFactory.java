/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.dbprovider;

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
   * Returns a remote or local IEntityDbProvider according to system properties
   * @param user the user for the connection
   * @param clientKey a unique client key
   * @param clientTypeID the client type id
   * @return a IEntityDbProvider
   * @see FrameworkConstants#CLIENT_CONNECTION_TYPE
   */
  public static IEntityDbProvider createEntityDbProvider(final User user, final String clientKey,
                                                         final String clientTypeID) {
    if (System.getProperty(FrameworkConstants.CLIENT_CONNECTION_TYPE,
            FrameworkConstants.CONNECTION_TYPE_LOCAL).equals(FrameworkConstants.CONNECTION_TYPE_REMOTE))
      return new RMIEntityDbProvider(user, clientKey, clientTypeID);
    else
      return new LocalEntityDbProvider(user);
  }
}
