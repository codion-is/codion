/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.User;
import org.jminor.framework.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * A factory class for handing out EntityDbProviders according to system properties.
 */
public final class EntityDbProviderFactory {

  private EntityDbProviderFactory() {}

  private static String remoteConnectionProviderClassName =
          (String) Configuration.getValue(Configuration.REMOTE_CONNECTION_PROVIDER);
  private static String localConnectionProviderClassName =
          (String) Configuration.getValue(Configuration.LOCAL_CONNECTION_PROVIDER);

  /**
   * Returns a EntityDbProvider according to system properties
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @return a EntityDbProvider
   */
  public static EntityDbProvider createEntityDbProvider(final User user, final String clientTypeID) {
    return createEntityDbProvider(user, UUID.randomUUID(), clientTypeID);
  }

  /**
   * Returns a remote or local EntityDbProvider according to system properties.
   * Loads classes by name, so these need to available on the classpath
   * @param user the user for the connection
   * @param clientID a unique client ID
   * @param clientTypeID the client type id
   * @return a EntityDbProvider
   * @see org.jminor.framework.Configuration#CLIENT_CONNECTION_TYPE
   * @see org.jminor.framework.Configuration#REMOTE_CONNECTION_PROVIDER
   * @see org.jminor.framework.Configuration#LOCAL_CONNECTION_PROVIDER
   * @see EntityDbLocalProvider
   * @see org.jminor.framework.server.provider.EntityDbRemoteProvider
   */
  public static EntityDbProvider createEntityDbProvider(final User user, final UUID clientID,
                                                        final String clientTypeID) {
    try {
      if (System.getProperty(Configuration.CLIENT_CONNECTION_TYPE,
              Configuration.CONNECTION_TYPE_LOCAL).equals(Configuration.CONNECTION_TYPE_REMOTE)) {
        return (EntityDbProvider) Class.forName(remoteConnectionProviderClassName).getConstructor(
                User.class, UUID.class, String.class).newInstance(user, clientID, clientTypeID);
      }
      else {
        return (EntityDbProvider) Class.forName(localConnectionProviderClassName).getConstructor(
                User.class).newInstance(user);
      }
    }
    catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof RuntimeException) {
        throw (RuntimeException) ((InvocationTargetException) ite).getTargetException();
      }

      throw new RuntimeException("Exception while initializing db provider", ite);
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException("Exception while initializing db provider", e);
    }
  }
}
