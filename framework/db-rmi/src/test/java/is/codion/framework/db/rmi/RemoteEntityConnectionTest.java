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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.Server;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.db.rmi.TestDomain.Department;
import is.codion.framework.db.rmi.TestDomain.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Collectors;

import static is.codion.framework.db.EntityConnection.Count.all;
import static is.codion.framework.db.EntityConnection.Count.where;
import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class RemoteEntityConnectionTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static Server<RemoteEntityConnection, EntityServerAdmin> server;
	private static EntityServerAdmin admin;
	private static RemoteEntityConnectionProvider connectionProvider;

	@BeforeAll
	public static void setUp() throws Exception {
		EntityServerConfiguration configuration = RemoteEntityConnectionProviderTest.configure();
		EntityServer.startServer(configuration);
		server = (Server<RemoteEntityConnection, EntityServerAdmin>)
						LocateRegistry.getRegistry(Clients.SERVER_HOSTNAME.get(), configuration.registryPort()).lookup(configuration.serverName());
		admin = server.admin(User.parse("scott:tiger"));
		connectionProvider = RemoteEntityConnectionProvider.builder()
						.hostname(Clients.SERVER_HOSTNAME.get())
						.port(configuration.port())
						.registryPort(configuration.registryPort())
						.clientType("RemoteEntityConnectionTest")
						.domain(TestDomain.DOMAIN)
						.user(UNIT_TEST_USER)
						.build();
	}

	@AfterAll
	public static void tearDown() throws RemoteException {
		admin.shutdown();
	}

	@Test
	void executeProcedure() {
		connection().execute(TestDomain.PROCEDURE_ID);
	}

	@Test
	void executeFunction() {
		assertNotNull(connection().execute(TestDomain.FUNCTION_ID));
	}

	@Test
	void report() {
		String result = connection().report(TestDomain.REPORT, "");
		assertNotNull(result);
	}

	@Test
	void insert() {
		EntityConnection connection = connection();
		Entity entity = connection.entities().entity(Department.TYPE)
						.with(Department.ID, 33L)
						.with(Department.NAME, "name")
						.with(Department.LOCATION, "loc")
						.build();
		Entity.Key key = connection.insert(entity);
		assertEquals(Long.valueOf(33), key.value());
		connection.delete(key);
	}

	@Test
	void insertMultiple() {
		EntityConnection connection = connection();
		Entity dept1 = connection.entities().entity(Department.TYPE)
						.with(Department.ID, 77L)
						.with(Department.NAME, "dept1")
						.with(Department.LOCATION, "loc1")
						.build();
		Entity dept2 = connection.entities().entity(Department.TYPE)
						.with(Department.ID, 78L)
						.with(Department.NAME, "dept2")
						.with(Department.LOCATION, "loc2")
						.build();
		Collection<Entity.Key> keys = connection.insert(asList(dept1, dept2));
		assertEquals(2, keys.size());
		connection.delete(keys);
	}

	@Test
	void insertSelectMultiple() {
		EntityConnection connection = connection();
		Entity dept1 = connection.entities().entity(Department.TYPE)
						.with(Department.ID, 79L)
						.with(Department.NAME, "dept3")
						.with(Department.LOCATION, "loc3")
						.build();
		Entity dept2 = connection.entities().entity(Department.TYPE)
						.with(Department.ID, 80L)
						.with(Department.NAME, "dept4")
						.with(Department.LOCATION, "loc4")
						.build();
		Collection<Entity> inserted = connection.insertSelect(asList(dept1, dept2));
		assertEquals(2, inserted.size());
		connection.delete(Entity.primaryKeys(inserted));
	}

	@Test
	void selectByKey() {
		EntityConnection connection = connection();
		Entity.Key key = connection.entities().primaryKey(Department.TYPE, 10L);
		Collection<Entity> depts = connection.select(singletonList(key));
		assertEquals(1, depts.size());
	}

	@Test
	void selectByKeyDifferentEntityTypes() {
		EntityConnection connection = connection();
		Entity.Key deptKey = connection.entities().primaryKey(Department.TYPE, 10L);
		Entity.Key empKey = connection.entities().primaryKey(Employee.TYPE, 8);

		Collection<Entity> selected = connection.select(asList(deptKey, empKey));
		assertEquals(2, selected.size());
	}

	@Test
	void update() {
		EntityConnection connection = connection();
		Entity department = connection.selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		department.set(Department.NAME, "TEstING");
		connection.update(department);
		department = connection.selectSingle(Department.ID.equalTo(department.get(Department.ID)));
		assertEquals("TEstING", department.get(Department.NAME));
		department.set(Department.NAME, "ACCOUNTING");
		connection.update(department);
	}

	@Test
	void updateSelect() {
		EntityConnection connection = connection();
		Entity department = connection.selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		department.set(Department.NAME, "TEstING");
		department = connection.updateSelect(department);
		department = connection.selectSingle(Department.ID.equalTo(department.get(Department.ID)));
		assertEquals("TEstING", department.get(Department.NAME));
		department.set(Department.NAME, "ACCOUNTING");
		department = connection.updateSelect(department);
	}

	@Test
	void updateMultiple() {
		EntityConnection connection = connection();
		Entity dept1 = connection.selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		Entity dept2 = connection.selectSingle(Department.NAME.equalTo("SALES"));
		dept1.set(Department.NAME, "ACC_TEST");
		dept2.set(Department.NAME, "SALES_TEST");
		connection.startTransaction();
		try {
			connection.update(asList(dept1, dept2));
			Entity updated1 = connection.selectSingle(Department.ID.equalTo(dept1.get(Department.ID)));
			Entity updated2 = connection.selectSingle(Department.ID.equalTo(dept2.get(Department.ID)));
			assertEquals("ACC_TEST", updated1.get(Department.NAME));
			assertEquals("SALES_TEST", updated2.get(Department.NAME));
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void updateSelectMultiple() {
		EntityConnection connection = connection();
		Entity dept1 = connection.selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		Entity dept2 = connection.selectSingle(Department.NAME.equalTo("SALES"));
		dept1.set(Department.NAME, "ACC_TEST2");
		dept2.set(Department.NAME, "SALES_TEST2");
		connection.startTransaction();
		try {
			Collection<Entity> updated = connection.updateSelect(asList(dept1, dept2));
			assertEquals(2, updated.size());
			for (Entity entity : updated) {
				assertTrue(entity.get(Department.NAME).endsWith("_TEST2"));
			}
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void updateByCondition() {
		EntityConnection connection = connection();
		List<Entity> entities = connection.select(Employee.COMMISSION.isNull());

		EntityConnection.Update update = EntityConnection.Update.where(Employee.COMMISSION.isNull())
						.set(Employee.COMMISSION, 500d)
						.set(Employee.SALARY, 4200d)
						.build();
		connection.startTransaction();
		try {
			connection.update(update);
			assertEquals(0, connection.count(where(Employee.COMMISSION.isNull())));
			Collection<Entity> afterUpdate = connection.select(Entity.primaryKeys(entities));
			for (Entity entity : afterUpdate) {
				assertEquals(500d, entity.get(Employee.COMMISSION));
				assertEquals(4200d, entity.get(Employee.SALARY));
			}
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void deleteByKey() {
		EntityConnection connection = connection();
		Entity employee = connection.selectSingle(Employee.NAME.equalTo("ADAMS"));
		connection.startTransaction();
		try {
			connection.delete(employee.primaryKey());
			Collection<Entity> selected = connection.select(singletonList(employee.primaryKey()));
			assertTrue(selected.isEmpty());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void deleteByKeyDifferentEntityTypes() {
		EntityConnection connection = connection();
		Entity.Key deptKey = connection.entities().primaryKey(Department.TYPE, 40L);
		Entity.Key empKey = connection.entities().primaryKey(Employee.TYPE, 1);
		connection.startTransaction();
		try {
			assertEquals(2, connection.select(asList(deptKey, empKey)).size());
			connection.delete(asList(deptKey, empKey));
			Collection<Entity> selected = connection.select(asList(deptKey, empKey));
			assertTrue(selected.isEmpty());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void dependencies() {
		EntityConnection connection = connection();
		Entity department = connection.selectSingle(Department.NAME.equalTo("SALES"));
		Map<EntityType, Collection<Entity>> dependentEntities = connection.dependencies(singletonList(department));
		assertNotNull(dependentEntities);
		assertTrue(dependentEntities.containsKey(Employee.TYPE));
		assertFalse(dependentEntities.get(Employee.TYPE).isEmpty());
	}

	@Test
	void rowCount() {
		assertEquals(4, connection().count(all(Department.TYPE)));
	}

	@Test
	void selectValues() {
		EntityConnection connection = connection();
		List<String> values = connection.select(Department.NAME);
		assertEquals(4, values.size());
		List<Long> ids = connection.select(Department.ID);
		assertInstanceOf(Long.class, ids.get(0));
	}

	@Test
	void selectColumnWithSelect() {
		List<String> names = connection().select(Department.NAME,
						Select.where(Department.LOCATION.equalTo("NEW YORK")).build());
		assertEquals(1, names.size());
		assertEquals("ACCOUNTING", names.get(0));
	}

	@Test
	void iterator() {
		try (EntityConnection connection = connection()) {
			Condition condition = Condition.all(Employee.TYPE);
			EntityResultIterator iterator = connection.iterator(condition);
			//calling hasNext() should be idempotent and not lose rows
			assertTrue(iterator.hasNext());
			assertTrue(iterator.hasNext());
			assertTrue(iterator.hasNext());
			int counter = 0;
			while (iterator.hasNext()) {
				iterator.next();
				iterator.hasNext();
				counter++;
			}
			assertThrows(NoSuchElementException.class, iterator::next);
			int rowCount = connection.count(EntityConnection.Count.where(condition));
			assertEquals(rowCount, counter);
			iterator.close();
			iterator = connection.iterator(condition);
			counter = 0;
			try {
				while (true) {
					iterator.next();
					counter++;
				}
			}
			catch (NoSuchElementException e) {
				assertEquals(rowCount, counter);
			}
		}
	}

	@Test
	void transactions() {
		EntityConnection connection = connection();
		assertFalse(connection.transactionOpen());
		connection.startTransaction();
		assertTrue(connection.transactionOpen());
		connection.rollbackTransaction();
		assertFalse(connection.transactionOpen());
		connection.startTransaction();
		assertTrue(connection.transactionOpen());
		connection.commitTransaction();
		assertFalse(connection.transactionOpen());
	}

	@Test
	void transactional() {
		EntityConnection connection = connection();
		transaction(connection, () -> {
			connection.select(Condition.all(Department.TYPE));
		});
		Collection<Entity> departments = transaction(connection, () -> connection.select(Condition.all(Department.TYPE)));
		assertFalse(departments.isEmpty());
		assertThrows(IllegalStateException.class, () -> transaction(connection, () -> transaction(connection, () -> {})));
	}

	@Test
	void writeReadBlob() {
		byte[] bytes = new byte[1024];
		new Random().nextBytes(bytes);

		EntityConnection connection = connection();
		Entity scott = connection.selectSingle(Employee.ID.equalTo(7));
		scott.set(Employee.DATA, bytes);
		connection.update(scott);
		assertArrayEquals(bytes, connection.select(Employee.DATA, key(scott.primaryKey())).get(0));
	}

	@Test
	void close() {
		EntityConnection connection = connection();
		connection.close();
		assertFalse(connection.connected());
	}

	@Test
	void deleteDepartmentWithEmployees() {
		EntityConnection connection = connection();
		Entity department = connection.selectSingle(Department.NAME.equalTo("SALES"));
		assertThrows(ReferentialIntegrityException.class, () -> connection.delete(key(department.primaryKey())));
	}

	@Test
	void foreignKeyValues() {
		Entity employee = connection().selectSingle(Employee.ID.equalTo(5));
		assertNotNull(employee.get(Employee.DEPARTMENT_FK));
		assertNotNull(employee.get(Employee.MGR_FK));
	}

	@Test
	void queryCache() {
		EntityConnection connection = connection();
		connection.queryCache(true);
		assertTrue(connection.queryCache());
		connection.queryCache(false);
		assertFalse(connection.queryCache());
	}

	@Test
	void rollbackWithNoOpenTransaction() {
		assertThrows(IllegalStateException.class, connection()::rollbackTransaction);
	}

	private static EntityConnection connection() {
		return connectionProvider.connection();
	}

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
			if (!entityConnectionMethod.getName().equals("iterator") && // these don't have the same return type
							remoteEntityConnectionMethods.stream().noneMatch(remoteConnectionMethod ->
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
