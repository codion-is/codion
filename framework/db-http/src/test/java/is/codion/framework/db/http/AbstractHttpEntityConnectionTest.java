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
 * Copyright (c) 2017 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.report.Report;
import is.codion.common.rmi.client.Clients;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.http.TestDomain.Department;
import is.codion.framework.db.http.TestDomain.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.EntityService;
import is.codion.framework.servlet.EntityServiceFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static is.codion.framework.db.EntityConnection.Count.all;
import static is.codion.framework.db.EntityConnection.Count.where;
import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractHttpEntityConnectionTest {

	private static EntityServer server;

	private final EntityConnection connection;

	protected AbstractHttpEntityConnectionTest(EntityConnection connection) {
		this.connection = connection;
	}

	@BeforeAll
	public static void setUp() throws Exception {
		server = EntityServer.startServer(configure());
	}

	@AfterAll
	public static void tearDown() {
		server.shutdown();
		deconfigure();
	}

	@Test
	void executeProcedure() {
		connection.execute(TestDomain.PROCEDURE_ID);
	}

	@Test
	void executeFunction() {
		assertNotNull(connection.execute(TestDomain.FUNCTION_ID));
	}

	@Test
	void report() {
		String result = connection.report(TestDomain.REPORT, "");
		assertNotNull(result);
	}

	@Test
	void insert() {
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
	void selectByKey() {
		Entity.Key key = connection.entities().primaryKey(Department.TYPE, 10L);
		Collection<Entity> depts = connection.select(singletonList(key));
		assertEquals(1, depts.size());
	}

	@Test
	void selectByKeyDifferentEntityTypes() {
		Entity.Key deptKey = connection.entities().primaryKey(Department.TYPE, 10L);
		Entity.Key empKey = connection.entities().primaryKey(Employee.TYPE, 8);

		Collection<Entity> selected = connection.select(asList(deptKey, empKey));
		assertEquals(2, selected.size());
	}

	@Test
	void update() {
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
		Entity department = connection.selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		department.set(Department.NAME, "TEstING");
		department = connection.updateSelect(department);
		department = connection.selectSingle(Department.ID.equalTo(department.get(Department.ID)));
		assertEquals("TEstING", department.get(Department.NAME));
		department.set(Department.NAME, "ACCOUNTING");
		department = connection.updateSelect(department);
	}

	@Test
	void updateByCondition() {
		List<Entity> entities = connection.select(Employee.COMMISSION.isNull());

		Update update = Update.where(Employee.COMMISSION.isNull())
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
		Entity department = connection.selectSingle(Department.NAME.equalTo("SALES"));
		Map<EntityType, Collection<Entity>> dependentEntities = connection.dependencies(singletonList(department));
		assertNotNull(dependentEntities);
		assertTrue(dependentEntities.containsKey(Employee.TYPE));
		assertFalse(dependentEntities.get(Employee.TYPE).isEmpty());
	}

	@Test
	void rowCount() {
		assertEquals(4, connection.count(all(Department.TYPE)));
	}

	@Test
	void selectValues() {
		List<String> values = connection.select(Department.NAME);
		assertEquals(4, values.size());
		List<Long> ids = connection.select(Department.ID);
		assertInstanceOf(Long.class, ids.get(0));
	}

	@Test
	void transactions() {
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

		Entity scott = connection.selectSingle(Employee.ID.equalTo(7));
		scott.set(Employee.DATA, bytes);
		connection.update(scott);
		assertArrayEquals(bytes, connection.select(Employee.DATA, key(scott.primaryKey())).get(0));
	}

	@Test
	void close() {
		connection.close();
		assertFalse(connection.connected());
	}

	@Test
	void deleteDepartmentWithEmployees() {
		Entity department = connection.selectSingle(Department.NAME.equalTo("SALES"));
		assertThrows(ReferentialIntegrityException.class, () -> connection.delete(key(department.primaryKey())));
	}

	@Test
	void foreignKeyValues() {
		Entity employee = connection.selectSingle(Employee.ID.equalTo(5));
		assertNotNull(employee.get(Employee.DEPARTMENT_FK));
		assertNotNull(employee.get(Employee.MGR_FK));
	}

	@Test
	void queryCache() {
		connection.queryCache(true);
		assertTrue(connection.queryCache());
		connection.queryCache(false);
		assertFalse(connection.queryCache());
	}

	@Test
	void rollbackWithNoOpenTransaction() {
		assertThrows(IllegalStateException.class, connection::rollbackTransaction);
	}

	private static EntityServerConfiguration configure() {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.TRUSTSTORE.set("../../framework/server/src/main/config/truststore.jks");
		Clients.resolveTrustStore();
		Report.REPORT_PATH.set("report/path");
		HttpEntityConnection.SECURE.set(true);
		EntityService.HTTP_SERVER_KEYSTORE_PATH.set("../../framework/server/src/main/config/keystore.jks");
		EntityService.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");

		return EntityServerConfiguration.builder(3223, 3221)
						.adminPort(3223)
						.database(Database.instance())
						.domainClassNames(singletonList(TestDomain.class.getName()))
						.sslEnabled(false)
						.auxiliaryServerFactory(singletonList(EntityServiceFactory.class.getName()))
						.build();
	}

	private static void deconfigure() {
		Clients.SERVER_HOSTNAME.set(null);
		Clients.TRUSTSTORE.set(null);
		System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE);
		System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE_PASSWORD);
		Report.REPORT_PATH.set(null);
		EntityService.HTTP_SERVER_KEYSTORE_PATH.set(null);
		EntityService.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
		HttpEntityConnection.SECURE.set(false);
	}
}
