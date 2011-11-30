/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.User;
import org.jminor.framework.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public final class EntityConnectionProviders {

  private EntityConnectionProviders() {}

  private static final String REMOTE_CONNECTION_PROVIDER =
          Configuration.getStringValue(Configuration.REMOTE_CONNECTION_PROVIDER);
  private static final String LOCAL_CONNECTION_PROVIDER =
          Configuration.getStringValue(Configuration.LOCAL_CONNECTION_PROVIDER);

  /**
   * Returns a EntityConnectionProvider according to system properties
   * @param user the user for the connection
   * @param clientTypeID the client type id
   * @return a EntityConnectionProvider
   */
  public static EntityConnectionProvider createConnectionProvider(final User user, final String clientTypeID) {
    return createConnectionProvider(user, UUID.randomUUID(), clientTypeID);
  }

  /**
   * Returns a remote or local EntityConnectionProvider according to system properties.
   * Loads classes by name, so these need to available on the classpath
   * @param user the user for the connection
   * @param clientID a unique client ID
   * @param clientTypeID the client type id
   * @return a EntityConnectionProvider
   * @see org.jminor.framework.Configuration#CLIENT_CONNECTION_TYPE
   * @see org.jminor.framework.Configuration#REMOTE_CONNECTION_PROVIDER
   * @see org.jminor.framework.Configuration#LOCAL_CONNECTION_PROVIDER
   * @see LocalEntityConnectionProvider
   * @see org.jminor.framework.server.provider.RemoteEntityConnectionProvider
   */
  public static EntityConnectionProvider createConnectionProvider(final User user, final UUID clientID,
                                                                  final String clientTypeID) {
    try {
      if (Configuration.getStringValue(Configuration.CLIENT_CONNECTION_TYPE).equals(Configuration.CONNECTION_TYPE_REMOTE)) {
        return (EntityConnectionProvider) Class.forName(REMOTE_CONNECTION_PROVIDER).getConstructor(
                User.class, UUID.class, String.class).newInstance(user, clientID, clientTypeID);
      }
      else {
        return (EntityConnectionProvider) Class.forName(LOCAL_CONNECTION_PROVIDER).getConstructor(
                User.class).newInstance(user);
      }
    }
    catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof RuntimeException) {
        throw (RuntimeException) ite.getTargetException();
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
