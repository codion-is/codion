/*
 * Copyright (c) 2004 - 2010, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.framework.db.EntityDb;

import org.junit.Test;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Arrays;

public class EntityDbRemoteTest {

  @Test
  public void entityDbCompatability() throws Exception {
    final Class<EntityDbRemote> entityDbRemoteClass = EntityDbRemote.class;
    final Class<EntityDb> entityDbLocalClass = EntityDb.class;
    if (entityDbRemoteClass.getDeclaredMethods().length != entityDbLocalClass.getDeclaredMethods().length) {
      fail("Method count mismatch");
    }
    for (final Method localDbMethod : entityDbLocalClass.getDeclaredMethods()) {
      final Class[] parameterTypes = localDbMethod.getParameterTypes();
      boolean found = false;
      for (final Method remoteDbMethod : entityDbRemoteClass.getDeclaredMethods()) {
        final Collection<Class<?>> exceptionTypes = Arrays.asList(remoteDbMethod.getExceptionTypes());
        if (remoteDbMethod.getReturnType().equals(localDbMethod.getReturnType())
                && remoteDbMethod.getName().equals(localDbMethod.getName())
                && Arrays.equals(remoteDbMethod.getParameterTypes(), parameterTypes)
                && exceptionTypes.containsAll(Arrays.asList(localDbMethod.getExceptionTypes()))) {
          found = true;
        }
      }
      if (!found) {
        fail(EntityDb.class.getSimpleName() + " method " + localDbMethod.getName() + " not found in " + EntityDbRemote.class.getSimpleName());
      }
    }
  }
}
