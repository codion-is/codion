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
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
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
	void unknownErrorKind() {
		//a client older than the server, an unknown kind must be inert rather than resolved by name
		String envelope = "{\"kind\":\"KIND_FROM_THE_FUTURE\",\"message\":\"something happened\","
						+ "\"correlationId\":\"abc\",\"fieldFromTheFuture\":42}";
		Exception exception = decodeError(500, envelope);
		assertEquals(DatabaseException.class, exception.getClass());
		assertEquals("something happened", exception.getMessage());
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
