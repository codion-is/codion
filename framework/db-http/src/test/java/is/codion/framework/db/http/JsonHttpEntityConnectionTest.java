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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.report.ReportException;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.exception.UpdateEntityException;
import is.codion.framework.db.http.TestDomain.Department;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.*;

public final class JsonHttpEntityConnectionTest extends AbstractHttpEntityConnectionTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	public JsonHttpEntityConnectionTest() {
		super(createConnection());
	}

	@Test
	void serverStackFramesDoNotCrossTheWire() {
		EntityConnection connection = createConnection();
		Entity department = connection.selectSingle(Department.NAME.equalTo("SALES"));
		ReferentialIntegrityException exception = assertThrows(ReferentialIntegrityException.class,
						() -> connection.delete(key(department.primaryKey())));
		//the exception is constructed client side from the envelope, the server's frames stay on the server
		assertTrue(stream(exception.getStackTrace())
						.map(StackTraceElement::getClassName)
						.noneMatch(className -> className.startsWith("is.codion.framework.db.local")
										|| className.startsWith("is.codion.framework.servlet")
										|| className.startsWith("org.eclipse.jetty")
										|| className.startsWith("org.h2")));
		assertNull(exception.getCause());
	}

	@Test
	void unregisteredReturnType() {
		//the client resolves the return type before the request, so the function never runs
		IllegalStateException exception = assertThrows(IllegalStateException.class,
						() -> createConnection().execute(TestDomain.UNREGISTERED_RETURN_FUNCTION, null));
		assertEquals("No json return type for function: unregisteredReturnFunction registered, "
						+ "register one via EntityObjectMapper.returnType(functionType).set(..)", exception.getMessage());
	}

	@Test
	void unregisteredParameterType() {
		//the server resolves the parameter type, so this message crosses the wire in an error envelope
		IllegalStateException exception = assertThrows(IllegalStateException.class,
						() -> createConnection().execute(TestDomain.UNREGISTERED_PARAMETER_FUNCTION, "a parameter"));
		assertEquals("No json parameter type for function: unregisteredParameterFunction registered, "
						+ "register one via EntityObjectMapper.parameter(functionType).set(..)", exception.getMessage());
	}

	@Test
	void unregisteredReportReturnType() {
		//the client resolves the return type before the request, so the report never runs
		IllegalStateException exception = assertThrows(IllegalStateException.class,
						() -> createConnection().report(TestDomain.UNREGISTERED_RETURN_REPORT, "a parameter"));
		assertEquals("No json return type for report: unregisteredReturnReport registered, "
						+ "register one via EntityObjectMapper.returnType(reportType).set(..)", exception.getMessage());
	}

	@Test
	void unknownErrorKind() {
		//a client older than the server, an unknown kind must be inert rather than resolved by name
		String envelope = "{\"kind\":\"KIND_FROM_THE_FUTURE\",\"message\":\"something happened\","
						+ "\"correlationId\":\"abc\",\"fieldFromTheFuture\":42}";
		Exception exception = decodeError(500, envelope);
		assertEquals(DatabaseException.class, exception.getClass());
		assertEquals("something happened", exception.getMessage());
	}

	@Test
	void unknownReferentialOperation() {
		//an Operation named by a newer server; decoding must return an exception, not throw one
		String envelope = "{\"kind\":\"CONFLICT_REFERENTIAL\",\"message\":\"blocked\",\"correlationId\":\"abc\","
						+ "\"detail\":{\"operation\":\"MERGE\"}}";
		Exception exception = decodeError(409, envelope);
		assertEquals(DatabaseException.class, exception.getClass());
		assertEquals("blocked", exception.getMessage());

		//and a missing operation detail
		exception = decodeError(409, "{\"kind\":\"CONFLICT_REFERENTIAL\",\"message\":\"blocked\",\"correlationId\":\"abc\"}");
		assertEquals(DatabaseException.class, exception.getClass());
	}

	@Test
	void unknownModifiedColumn() {
		//a column named by a newer server's domain, reached through columns(), which the
		//IOException catch did not cover; degrade to the supertype like a missing detail does
		String envelope = "{\"kind\":\"CONFLICT_MODIFIED\",\"message\":\"changed\",\"correlationId\":\"abc\","
						+ "\"detail\":{\"entity\":{\"entityType\":\"employees.department\",\"values\":{\"deptno\":10}},"
						+ "\"modified\":null,\"columns\":[\"employees.department.columnFromTheFuture\"]}}";
		Exception exception = decodeError(409, envelope);
		assertEquals(UpdateEntityException.class, exception.getClass());
		assertEquals("changed", exception.getMessage());
	}

	@Test
	void reportError() {
		//a report failure crosses as ErrorKind.REPORT, its message already stripped of engine
		//types by the plugin, and is reconstructed as a ReportException, which lives in common-db
		String envelope = "{\"kind\":\"REPORT\",\"message\":\"PDF exporter extension not found\",\"correlationId\":\"abc\"}";
		Exception exception = decodeError(500, envelope);
		assertEquals(ReportException.class, exception.getClass());
		assertEquals("PDF exporter extension not found", exception.getMessage());
	}

	@Test
	void errorBodyIsNotAnEnvelope() {
		//a proxy error page or an unmatched route
		Exception exception = decodeError(502, "<html>Bad Gateway</html>");
		assertEquals(DatabaseException.class, exception.getClass());
		assertTrue(exception.getMessage().contains("502"), exception.getMessage());
	}

	private static Exception decodeError(int status, String body) {
		return ((JsonHttpEntityConnection) createConnection())
						.decodeError(new HttpTransport.Response(status, body.getBytes(UTF_8)));
	}

	private static EntityConnection createConnection() {
		return HttpEntityConnection.builder()
						.json(true)
						.domain(TestDomain.DOMAIN)
						.user(UNIT_TEST_USER)
						.clientType("JsonHttpEntityConnectionTest")
						.clientId(UUID.randomUUID())
						.build();
	}
}
