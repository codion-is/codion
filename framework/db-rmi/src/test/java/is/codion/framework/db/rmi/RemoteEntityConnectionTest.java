/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
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
	void entityConnectionCompatibility() {
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
