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
import is.codion.framework.db.local.TestDomain.Department;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static org.junit.jupiter.api.Assertions.*;

public final class SavepointTest {

	private static final User UNIT_TEST_USER = User.parse(System.getProperty("codion.test.user", "scott:tiger"));
	private static final Database DATABASE = Database.instance();
	private static final TestDomain DOMAIN = new TestDomain();

	private LocalEntityConnection connection;

	@BeforeEach
	void setup() {
		connection = localEntityConnection(DATABASE, DOMAIN, UNIT_TEST_USER);
	}

	@AfterEach
	void tearDown() {
		if (connection.transactionOpen()) {
			connection.rollbackTransaction();
		}
		connection.close();
		try (LocalEntityConnection cleanup = localEntityConnection(DATABASE, DOMAIN, UNIT_TEST_USER)) {
			cleanup.delete(Department.DEPTNO.between(61, 69));
		}
	}

	@Test
	void rollback() {
		connection.startTransaction();
		insert(61);
		Savepoint savepoint = connection.savepoint();
		insert(62);
		connection.rollback(savepoint);
		assertTrue(connection.transactionOpen());
		assertEquals(1, departments(connection));
		// ended by the rollback, as on HSQLDB and SQL Server, which do not keep it
		assertThrows(IllegalStateException.class, () -> connection.rollback(savepoint));
		connection.commitTransaction();
		assertEquals(1, committed());
	}

	@Test
	void rollbackEndsLaterSavepoints() {
		connection.startTransaction();
		Savepoint first = connection.savepoint();
		insert(61);
		Savepoint second = connection.savepoint();
		insert(62);
		connection.rollback(first);
		assertEquals(0, departments(connection));
		// H2 would still roll back to it
		assertThrows(IllegalStateException.class, () -> connection.rollback(second));
		assertThrows(IllegalStateException.class, () -> connection.release(second));
	}

	@Test
	void release() {
		connection.startTransaction();
		Savepoint first = connection.savepoint();
		insert(61);
		Savepoint second = connection.savepoint();
		insert(62);
		connection.release(first);
		assertEquals(2, departments(connection));
		assertThrows(IllegalStateException.class, () -> connection.rollback(first));
		// H2 would still roll back to it
		assertThrows(IllegalStateException.class, () -> connection.rollback(second));
		connection.commitTransaction();
		assertEquals(2, committed());
	}

	@Test
	void nested() {
		connection.startTransaction();
		Savepoint first = connection.savepoint();
		insert(61);
		Savepoint second = connection.savepoint();
		insert(62);
		connection.rollback(second);
		assertEquals(1, departments(connection));
		connection.release(first);
		connection.commitTransaction();
		assertEquals(1, committed());
	}

	@Test
	void requiresTransaction() {
		Savepoint other = Savepoint.savepoint(UUID.randomUUID());
		assertThrows(IllegalStateException.class, connection::savepoint);
		assertThrows(IllegalStateException.class, () -> connection.rollback(other));
		assertThrows(IllegalStateException.class, () -> connection.release(other));
	}

	@Test
	void endsWithTransaction() {
		connection.startTransaction();
		Savepoint committed = connection.savepoint();
		connection.commitTransaction();
		connection.startTransaction();
		// MariaDB and SQL Server silently accept one whose transaction has ended
		assertThrows(IllegalStateException.class, () -> connection.rollback(committed));
		Savepoint rolledBack = connection.savepoint();
		connection.rollbackTransaction();
		connection.startTransaction();
		assertThrows(IllegalStateException.class, () -> connection.release(rolledBack));
		connection.rollbackTransaction();

		connection.startTransaction();
		Savepoint closed = connection.savepoint();
		connection.close();
		assertThrows(IllegalStateException.class, () -> connection.rollback(closed));
	}

	@Test
	void otherConnections() {
		try (LocalEntityConnection other = localEntityConnection(DATABASE, DOMAIN, UNIT_TEST_USER)) {
			other.startTransaction();
			Savepoint savepoint = other.savepoint();
			connection.startTransaction();
			assertThrows(IllegalStateException.class, () -> connection.rollback(savepoint));
			other.rollbackTransaction();
		}
	}

	@Test
	void identifiedById() {
		connection.startTransaction();
		insert(61);
		Savepoint savepoint = connection.savepoint();
		insert(62);
		// as a remote connection receives it
		Savepoint received = Savepoint.savepoint(savepoint.id());
		assertEquals(savepoint, received);
		connection.rollback(received);
		assertEquals(1, departments(connection));
	}

	@Test
	void recovery() {
		connection.startTransaction();
		insert(61);
		Savepoint savepoint = connection.savepoint();
		insert(62);
		assertThrows(DatabaseException.class, () -> insert(61));
		connection.rollback(savepoint);
		insert(63);
		connection.commitTransaction();
		try (LocalEntityConnection other = localEntityConnection(DATABASE, DOMAIN, UNIT_TEST_USER)) {
			assertEquals(0, other.count(Count.where(Department.DEPTNO.equalTo(62))));
			assertEquals(2, other.count(Count.where(Department.DEPTNO.in(61, 63))));
		}
	}

	@Test
	void scoped() {
		assertThrows(IllegalStateException.class, () -> EntityConnection.savepoint(connection, () -> insert(61)));

		connection.startTransaction();
		EntityConnection.savepoint(connection, () -> insert(61));
		assertEquals(1, departments(connection));

		DatabaseException exception = assertThrows(DatabaseException.class, () ->
						EntityConnection.savepoint(connection, () -> {
							insert(62);
							insert(61);
						}));
		assertEquals(0, exception.getSuppressed().length);
		assertTrue(connection.transactionOpen());
		assertEquals(1, departments(connection));

		assertEquals(Integer.valueOf(63), EntityConnection.savepoint(connection, () -> {
			insert(63);

			return 63;
		}));

		RuntimeException wrapped = assertThrows(RuntimeException.class, () ->
						EntityConnection.savepoint(connection, () -> {
							insert(64);
							throw new IOException();
						}));
		assertInstanceOf(IOException.class, wrapped.getCause());
		assertEquals(2, departments(connection));
		connection.commitTransaction();
		assertEquals(2, committed());
	}

	@Test
	void scopedRollbackFails() {
		connection.startTransaction();
		// the work ends the transaction, so rolling back to the savepoint fails
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
						EntityConnection.savepoint(connection, () -> {
							connection.rollbackTransaction();
							throw new IllegalArgumentException();
						}));
		assertEquals(1, exception.getSuppressed().length);
		assertInstanceOf(IllegalStateException.class, exception.getSuppressed()[0]);
	}

	private void insert(int deptno) {
		connection.insert(connection.entities().entity(Department.TYPE)
						.with(Department.DEPTNO, deptno)
						.with(Department.DNAME, "SAVEPOINT" + deptno)
						.build());
	}

	private static int departments(EntityConnection connection) {
		return connection.count(Count.where(Department.DEPTNO.between(61, 69)));
	}

	private static int committed() {
		try (LocalEntityConnection other = localEntityConnection(DATABASE, DOMAIN, UNIT_TEST_USER)) {
			return departments(other);
		}
	}
}
