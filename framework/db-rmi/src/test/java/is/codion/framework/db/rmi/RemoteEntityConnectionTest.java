/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.rmi;

import is.codion.framework.db.EntityConnection;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.fail;

public class RemoteEntityConnectionTest {

  /* A sanity check since {@link RemoteEntityConnection} can not extend {@link EntityConnection}. */
  @Test
  void entityConnectionCompatibility() throws Exception {
    List<Method> remoteEntityConnectionMethods = Arrays.stream(RemoteEntityConnection.class.getDeclaredMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers())).collect(Collectors.toList());
    List<Method> entityConnectionMethods = Arrays.stream(EntityConnection.class.getDeclaredMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers())).collect(Collectors.toList());
    if (remoteEntityConnectionMethods.size() != entityConnectionMethods.size()) {
      fail("Method count mismatch");
    }
    for (Method entityConnectionMethod : entityConnectionMethods) {
      if (remoteEntityConnectionMethods.stream().noneMatch(remoteConnectionMethod ->
              remoteConnectionMethod.getReturnType().equals(entityConnectionMethod.getReturnType())
                      && remoteConnectionMethod.getName().equals(entityConnectionMethod.getName())
                      && Arrays.equals(remoteConnectionMethod.getParameterTypes(), entityConnectionMethod.getParameterTypes())
                      && asList(remoteConnectionMethod.getExceptionTypes()).containsAll(asList(entityConnectionMethod.getExceptionTypes())))) {
        fail(EntityConnection.class.getSimpleName() + " method " + entityConnectionMethod.getName()
                + " not found in " + RemoteEntityConnection.class.getSimpleName());
      }
    }
  }
}
