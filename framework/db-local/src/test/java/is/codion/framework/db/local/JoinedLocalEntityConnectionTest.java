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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Savepoint;
import is.codion.framework.db.local.tracer.MethodTracer;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.operation.FunctionType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.db.local.tracer.MethodTracer.methodTracer;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

public final class JoinedLocalEntityConnectionTest {

	private static final User UNIT_TEST_USER = User.parse(System.getProperty("codion.test.user", "scott:tiger"));
	private static final Database DATABASE = Database.instance();
	private static final ParentDomain PARENT = new ParentDomain();

	private final JoinedDomain joinedDomain = new JoinedDomain();

	@AfterEach
	void cleanup() {
		try (LocalEntityConnection connection = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER)) {
			connection.delete(Department.DEPTNO.between(71, 79));
		}
		try (LocalEntityConnection connection = localEntityConnection(DATABASE, joinedDomain, UNIT_TEST_USER)) {
			connection.delete(Master.DATA.like("joined%"));
		}
	}

	@Test
	void transactionStartedOnParent() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER);
				 LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			parent.startTransaction();
			assertTrue(joined.transactionOpen());
			insertDepartment(parent, 71);
			insertMaster(joined, "joined");
			// the joined write did not commit the parent's transaction
			assertEquals(0, departments());
			assertEquals(0, masters());
			parent.rollbackTransaction();
			assertFalse(joined.transactionOpen());
			assertEquals(0, departments());
			assertEquals(0, masters());

			parent.startTransaction();
			insertDepartment(parent, 71);
			insertMaster(joined, "joined");
			parent.commitTransaction();
			assertEquals(1, departments());
			assertEquals(1, masters());
		}
	}

	@Test
	void transactionStartedOnJoined() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER);
				 LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			joined.startTransaction();
			assertTrue(parent.transactionOpen());
			assertThrows(IllegalStateException.class, parent::startTransaction);
			insertDepartment(parent, 71);
			insertMaster(joined, "joined");
			joined.rollbackTransaction();
			assertFalse(parent.transactionOpen());
			assertEquals(0, departments());
			assertEquals(0, masters());

			joined.startTransaction();
			insertDepartment(parent, 71);
			insertMaster(joined, "joined");
			joined.commitTransaction();
			assertFalse(parent.transactionOpen());
			assertEquals(1, departments());
			assertEquals(1, masters());
		}
	}

	@Test
	void outsideTransaction() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER);
				 LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			insertMaster(joined, "joined");
			assertEquals(1, masters());
		}
	}

	@Test
	void transactionHelperJoinsTheParentTransaction() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER);
				 LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			parent.startTransaction();
			EntityConnection.transaction(joined, () -> insertMaster(joined, "joined"));
			assertTrue(parent.transactionOpen());
			assertEquals(0, masters());
			parent.rollbackTransaction();
			assertEquals(0, masters());
		}
	}

	@Test
	void inFunction() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER)) {
			assertThrows(DatabaseException.class, () -> parent.execute(ParentDomain.RUN, connection ->
							EntityConnection.transaction(connection, () -> {
								LocalEntityConnection joined = localEntityConnection(joinedDomain, connection);
								insertMaster(joined, "joined");
								insertDepartment(connection, 71);
								insertDepartment(connection, 71);
							})));
			assertEquals(0, departments());
			assertEquals(0, masters());

			parent.execute(ParentDomain.RUN, connection ->
							EntityConnection.transaction(connection, () -> {
								LocalEntityConnection joined = localEntityConnection(joinedDomain, connection);
								insertMaster(joined, "joined");
								insertDepartment(connection, 71);
							}));
			assertEquals(1, departments());
			assertEquals(1, masters());
		}
	}

	@Test
	void close() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER)) {
			LocalEntityConnection joined = localEntityConnection(joinedDomain, parent);
			parent.startTransaction();
			insertDepartment(parent, 71);
			joined.close();
			assertFalse(joined.connected());
			assertFalse(joined.transactionOpen());
			assertThrows(DatabaseException.class, () -> joined.count(Count.all(Master.TYPE)));
			assertThrows(DatabaseException.class, joined::commitTransaction);
			assertThrows(DatabaseException.class, joined::connection);
			assertTrue(parent.connected());
			assertTrue(parent.transactionOpen());
			parent.commitTransaction();
			assertEquals(1, departments());

			LocalEntityConnection other = localEntityConnection(joinedDomain, parent);
			parent.close();
			assertFalse(other.connected());
			assertThrows(DatabaseException.class, () -> other.count(Count.all(Master.TYPE)));
		}
	}

	@Test
	void attachDetach() throws Exception {
		Connection first = DATABASE.createConnection(UNIT_TEST_USER);
		Connection second = DATABASE.createConnection(UNIT_TEST_USER);
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, first);
				 LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			assertEquals(0, joinedDomain.configured.get());
			joined.count(Count.all(Master.TYPE));
			joined.count(Count.all(Master.TYPE));
			assertEquals(1, joinedDomain.configured.get());
			assertSame(first, joined.connection());

			assertSame(first, ((ConnectionHolder) parent).detach());
			assertThrows(DatabaseException.class, () -> joined.count(Count.all(Master.TYPE)));
			((ConnectionHolder) parent).attach(second);
			joined.count(Count.all(Master.TYPE));
			assertEquals(2, joinedDomain.configured.get());
			assertSame(second, joined.connection());
			// the same connection attached again is configured again, on each attachment, as the parent does
			assertSame(second, ((ConnectionHolder) parent).detach());
			((ConnectionHolder) parent).attach(second);
			joined.count(Count.all(Master.TYPE));
			assertEquals(3, joinedDomain.configured.get());

			assertThrows(UnsupportedOperationException.class, () -> ((ConnectionHolder) joined).attach(first));
			assertThrows(UnsupportedOperationException.class, () -> ((ConnectionHolder) joined).detach());
		}
		finally {
			first.close();
		}
	}

	@Test
	void selfManagingParent() {
		LocalEntityConnection parent = LocalEntityConnection.builder()
						.database(DATABASE)
						.domain(PARENT)
						.user(UNIT_TEST_USER)
						.build();
		try (LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			parent.startTransaction();
			assertTrue(joined.transactionOpen());
			insertMaster(joined, "joined");
			joined.rollbackTransaction();
			assertFalse(parent.transactionOpen());
			assertEquals(0, masters());

			joined.startTransaction();
			assertTrue(parent.transactionOpen());
			insertDepartment(parent, 71);
			insertMaster(joined, "joined");
			assertEquals(0, masters());
			joined.commitTransaction();
			assertFalse(parent.transactionOpen());
			assertEquals(1, departments());
			assertEquals(1, masters());
		}
		finally {
			parent.close();
		}
		assertThrows(IllegalStateException.class, () -> localEntityConnection(joinedDomain, parent));
	}

	@Test
	void selfManagingParentReconnects() throws Exception {
		long validityCheckInterval = EntityConnection.VALIDITY_CHECK_INTERVAL.getOrThrow();
		EntityConnection.VALIDITY_CHECK_INTERVAL.set(0L);
		LocalEntityConnection parent = LocalEntityConnection.builder()
						.database(DATABASE)
						.domain(PARENT)
						.user(UNIT_TEST_USER)
						.build();
		try (LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			joined.count(Count.all(Master.TYPE));
			// the connection goes bad, and the parent re-establishes it
			parent.connection().close();
			parent.count(Count.all(Department.TYPE));
			// joined to the connection the parent held at the time, so its statements fail as on a closed
			// connection, as does starting a transaction, rather than reaching the parent's new connection
			assertFalse(joined.connected());
			assertThrows(DatabaseException.class, () -> joined.count(Count.all(Master.TYPE)));
			assertThrows(DatabaseException.class, joined::startTransaction);
			assertFalse(joined.transactionOpen());
			assertFalse(parent.transactionOpen());
			// while a transaction the parent is in can still be ended through it, as a caller recovering
			// from the failure needs to
			parent.startTransaction();
			assertFalse(joined.transactionOpen());
			joined.rollbackTransaction();
			assertFalse(parent.transactionOpen());
		}
		finally {
			parent.close();
		}
		// the parent re-establishing its connection while the joined one starts a transaction, the parent validating
		// its connection then, so the transaction would be on the new connection: rolled back and refused
		parent = LocalEntityConnection.builder()
						.database(DATABASE)
						.domain(PARENT)
						.user(UNIT_TEST_USER)
						.build();
		try (LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			joined.count(Count.all(Master.TYPE));
			parent.connection().close();
			assertThrows(DatabaseException.class, joined::startTransaction);
			assertFalse(parent.transactionOpen());
			assertFalse(joined.transactionOpen());
			assertThrows(DatabaseException.class, () -> EntityConnection.transaction(joined, () -> insertMaster(joined, "joined")));
			assertFalse(parent.transactionOpen());
		}
		finally {
			parent.close();
			EntityConnection.VALIDITY_CHECK_INTERVAL.set(validityCheckInterval);
		}
	}

	@Test
	void savepoints() throws Exception {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER);
				 LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			parent.startTransaction();
			insertDepartment(parent, 71);
			Savepoint savepoint = joined.savepoint();
			insertMaster(joined, "joined");
			insertDepartment(parent, 72);
			parent.rollback(savepoint);
			assertEquals(1, parent.count(Count.where(Department.DEPTNO.between(71, 79))));
			assertEquals(0, joined.count(Count.where(Master.DATA.like("joined%"))));
			joined.release(parent.savepoint());
			parent.commitTransaction();
			assertThrows(IllegalStateException.class, () -> joined.rollback(savepoint));
			assertEquals(1, departments());
		}
		LocalEntityConnection parent = LocalEntityConnection.builder()
						.database(DATABASE)
						.domain(PARENT)
						.user(UNIT_TEST_USER)
						.build();
		try (LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			parent.startTransaction();
			Savepoint savepoint = joined.savepoint();
			insertMaster(joined, "joined");
			parent.rollback(savepoint);
			parent.commitTransaction();
			assertEquals(0, masters());
			// the parent on another connection, which the joined one is not joined to
			long validityCheckInterval = EntityConnection.VALIDITY_CHECK_INTERVAL.getOrThrow();
			EntityConnection.VALIDITY_CHECK_INTERVAL.set(0L);
			try {
				parent.connection().close();
				parent.startTransaction();
				assertThrows(DatabaseException.class, joined::savepoint);
				Savepoint parentSavepoint = parent.savepoint();
				assertThrows(DatabaseException.class, () -> joined.rollback(parentSavepoint));
				assertThrows(DatabaseException.class, () -> joined.release(parentSavepoint));
				parent.rollbackTransaction();
			}
			finally {
				EntityConnection.VALIDITY_CHECK_INTERVAL.set(validityCheckInterval);
			}
		}
		finally {
			parent.close();
		}
	}

	@Test
	void joinedToJoined() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER)) {
			LocalEntityConnection joined = localEntityConnection(joinedDomain, parent);
			LocalEntityConnection second = localEntityConnection(joinedDomain, joined);
			joined.close();
			second.startTransaction();
			assertTrue(parent.transactionOpen());
			insertMaster(second, "joined");
			parent.commitTransaction();
			assertEquals(1, masters());
		}
	}

	@Test
	void settings() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER)) {
			parent.queryTimeout(42);
			parent.optimisticLocking(!parent.optimisticLocking());
			parent.limitReferenceDepth(!parent.limitReferenceDepth());
			parent.iteratorBufferSize(7);
			LocalEntityConnection joined = localEntityConnection(joinedDomain, parent);
			assertEquals(42, joined.queryTimeout());
			assertEquals(parent.optimisticLocking(), joined.optimisticLocking());
			assertEquals(parent.limitReferenceDepth(), joined.limitReferenceDepth());
			assertEquals(7, joined.iteratorBufferSize());
			joined.queryTimeout(1);
			assertEquals(42, parent.queryTimeout());
			assertEquals(UNIT_TEST_USER.username(), joined.user().username());
			assertSame(parent.database(), joined.database());
			assertEquals(joinedDomain.entities(), joined.entities());
		}
	}

	@Test
	void tracer() {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER);
				 LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			MethodTracer tracer = methodTracer(10);
			((MethodTracer.Traceable) parent).tracer(tracer);
			assertTrue(tracer.entries().isEmpty());
			joined.count(Count.all(Master.TYPE));
			assertFalse(tracer.entries().isEmpty());
		}
	}

	@Test
	void serialized() throws Exception {
		try (LocalEntityConnection parent = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER);
				 LocalEntityConnection joined = localEntityConnection(joinedDomain, parent)) {
			CountDownLatch entered = new CountDownLatch(1);
			CountDownLatch release = new CountDownLatch(1);
			CompletableFuture<Void> function = CompletableFuture.runAsync(() -> parent.execute(ParentDomain.RUN, connection -> {
				entered.countDown();
				try {
					release.await();
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}));
			try {
				assertTrue(entered.await(10, TimeUnit.SECONDS));
				CompletableFuture<Integer> count = CompletableFuture.supplyAsync(() -> joined.count(Count.all(Master.TYPE)));
				// waits for the function, which holds the monitor the two share
				assertThrows(TimeoutException.class, () -> count.get(200, TimeUnit.MILLISECONDS));
				release.countDown();
				function.get(10, TimeUnit.SECONDS);
				count.get(10, TimeUnit.SECONDS);
			}
			finally {
				release.countDown();
			}
		}
	}

	@Test
	void otherImplementation() {
		LocalEntityConnection other = (LocalEntityConnection) Proxy.newProxyInstance(LocalEntityConnection.class.getClassLoader(),
						new Class<?>[] {LocalEntityConnection.class}, (proxy, method, args) -> {
							throw new UnsupportedOperationException();
						});
		assertThrows(IllegalArgumentException.class, () -> localEntityConnection(joinedDomain, other));
	}

	private static void insertDepartment(EntityConnection connection, int deptno) {
		connection.insert(connection.entities().entity(Department.TYPE)
						.with(Department.DEPTNO, deptno)
						.with(Department.DNAME, "JOINED" + deptno)
						.build());
	}

	private static void insertMaster(EntityConnection connection, String data) {
		connection.insert(connection.entities().entity(Master.TYPE)
						.with(Master.DATA, data)
						.build());
	}

	private static int departments() {
		try (LocalEntityConnection connection = localEntityConnection(DATABASE, PARENT, UNIT_TEST_USER)) {
			return connection.count(Count.where(Department.DEPTNO.between(71, 79)));
		}
	}

	private static int masters() {
		try (LocalEntityConnection connection = localEntityConnection(DATABASE, new JoinedDomain(), UNIT_TEST_USER)) {
			return connection.count(Count.where(Master.DATA.like("joined%")));
		}
	}

	interface Department {
		EntityType TYPE = ParentDomain.DOMAIN.entityType("employees.department");
		Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
		Column<String> DNAME = TYPE.stringColumn("dname");
	}

	interface Master {
		EntityType TYPE = JoinedDomain.DOMAIN.entityType("employees.master");
		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> DATA = TYPE.stringColumn("data");
	}

	private static final class ParentDomain extends DomainModel {

		private static final DomainType DOMAIN = DomainType.domainType("JoinedLocalEntityConnectionTest.Parent");

		// runs the given consumer with the connection, holding its monitor
		private static final FunctionType<LocalEntityConnection, Consumer<LocalEntityConnection>, Object> RUN =
						FunctionType.functionType("run");

		private ParentDomain() {
			super(DOMAIN);
			add(Department.TYPE.as()
							.attributes(
											Department.DEPTNO.as()
															.primaryKey(),
											Department.DNAME.as()
															.column())
							.build());
			add(RUN, (connection, consumer) -> {
				requireNonNull(consumer).accept(connection);

				return null;
			});
		}
	}

	private static final class JoinedDomain extends DomainModel {

		private static final DomainType DOMAIN = DomainType.domainType("JoinedLocalEntityConnectionTest.Joined");

		private final AtomicInteger configured = new AtomicInteger();

		private JoinedDomain() {
			super(DOMAIN);
			add(Master.TYPE.as()
							.attributes(
											Master.ID.as()
															.primaryKey()
															.generator(Column.Generator.identity()),
											Master.DATA.as()
															.column())
							.build());
		}

		@Override
		public void configure(Connection connection) {
			configured.incrementAndGet();
		}
	}
}
