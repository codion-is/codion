/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.framework.db.EntityConnection;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.fail;

public class RemoteEntityConnectionTest {

  @Test
  public void entityConnectionCompatibility() throws Exception {
    final Class<RemoteEntityConnection> remoteConnectionClass = RemoteEntityConnection.class;
    final Class<EntityConnection> connectionClass = EntityConnection.class;
    if (remoteConnectionClass.getDeclaredMethods().length != connectionClass.getDeclaredMethods().length) {
      fail("Method count mismatch");
    }
    for (final Method localDbMethod : connectionClass.getDeclaredMethods()) {
      final Class[] parameterTypes = localDbMethod.getParameterTypes();
      boolean found = false;
      for (final Method remoteDbMethod : remoteConnectionClass.getDeclaredMethods()) {
        final Collection<Class<?>> exceptionTypes = Arrays.asList(remoteDbMethod.getExceptionTypes());
        if (remoteDbMethod.getReturnType().equals(localDbMethod.getReturnType())
                && remoteDbMethod.getName().equals(localDbMethod.getName())
                && Arrays.equals(remoteDbMethod.getParameterTypes(), parameterTypes)
                && exceptionTypes.containsAll(Arrays.asList(localDbMethod.getExceptionTypes()))) {
          found = true;
        }
      }
      if (!found) {
        fail(EntityConnection.class.getSimpleName() + " method " + localDbMethod.getName() + " not found in " + RemoteEntityConnection.class.getSimpleName());
      }
    }
  }
}
