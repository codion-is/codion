/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.User;
import org.jminor.framework.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

/**
 * A factory class for handing out IEntityDbProviders according to system properties
 */
public class EntityDbProviderFactory {

  private static String remoteConnectionProviderClassName =
          (String) Configuration.getValue(Configuration.REMOTE_CONNECTION_PROVIDER);
  private static String localConnectionProviderClassName =
          (String) Configuration.getValue(Configuration.LOCAL_CONNECTION_PROVIDER);

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
   * @see org.jminor.framework.Configuration#CLIENT_CONNECTION_TYPE
   * @see org.jminor.framework.Configuration#REMOTE_CONNECTION_PROVIDER
   * @see org.jminor.framework.Configuration#LOCAL_CONNECTION_PROVIDER
   * @see EntityDbLocalProvider
   * @see org.jminor.framework.server.provider.EntityDbRemoteProvider
   */
  public static IEntityDbProvider createEntityDbProvider(final User user, final String clientKey,
                                                         final String clientTypeID) {
    try {
      if (System.getProperty(Configuration.CLIENT_CONNECTION_TYPE,
              Configuration.CONNECTION_TYPE_LOCAL).equals(Configuration.CONNECTION_TYPE_REMOTE))
        return (IEntityDbProvider) Class.forName(remoteConnectionProviderClassName).getConstructor(
                User.class, String.class, String.class).newInstance(user, clientKey, clientTypeID);
      else
        return (IEntityDbProvider) Class.forName(localConnectionProviderClassName).getConstructor(
                User.class).newInstance(user);
    }
    catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof RuntimeException)
        throw (RuntimeException) ((InvocationTargetException) ite).getTargetException();

      throw new RuntimeException("Exception while initializing db provider", ite);
    }
    catch (Exception e) {
      if (e instanceof RuntimeException)
        throw (RuntimeException) e;

      throw new RuntimeException("Exception while initializing db provider", e);
    }
  }
}
