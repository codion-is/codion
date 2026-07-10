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
package is.codion.framework.json.db;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public final class ErrorEnvelopeTest {

	@Test
	void roundTrip() throws IOException {
		ObjectNode detail = JsonNodeFactory.instance.objectNode()
						.put(ErrorEnvelope.OPERATION, "DELETE");
		ErrorEnvelope envelope = new ErrorEnvelope(ErrorKind.CONFLICT_REFERENTIAL.name(),
						"Delete failed", "correlation-id", detail);

		ErrorEnvelope parsed = ErrorEnvelope.fromJson(envelope.toJson().getBytes(UTF_8));
		assertEquals(ErrorKind.CONFLICT_REFERENTIAL, parsed.errorKind().orElseThrow());
		assertEquals("Delete failed", parsed.message());
		assertEquals("correlation-id", parsed.correlationId());
		assertEquals("DELETE", parsed.detail().get(ErrorEnvelope.OPERATION).asText());
	}

	@Test
	void detailIsOmittedWhenAbsent() throws IOException {
		ErrorEnvelope envelope = new ErrorEnvelope(ErrorKind.INTERNAL.name(), "Internal server error", "id", null);
		assertFalse(envelope.toJson().contains("detail"));
		assertNull(ErrorEnvelope.fromJson(envelope.toJson().getBytes(UTF_8)).detail());
	}

	@Test
	void unknownKindAndFieldsTolerated() throws IOException {
		//a client older than the server
		String json = "{\"kind\":\"KIND_FROM_THE_FUTURE\",\"message\":\"m\",\"correlationId\":\"id\",\"future\":1}";
		ErrorEnvelope envelope = ErrorEnvelope.fromJson(json.getBytes(UTF_8));
		assertEquals("KIND_FROM_THE_FUTURE", envelope.kind());
		assertFalse(envelope.errorKind().isPresent());
		assertEquals("m", envelope.message());
	}

	@Test
	void kindOf() {
		assertEquals(ErrorKind.NOT_FOUND, ErrorKind.of("NOT_FOUND").orElseThrow());
		assertFalse(ErrorKind.of("not_found").isPresent());
		assertFalse(ErrorKind.of("").isPresent());
	}

	@Test
	void statusAndSeverity() {
		assertEquals(401, ErrorKind.AUTHENTICATION.status());
		assertEquals(ErrorKind.Severity.DEBUG, ErrorKind.AUTHENTICATION.severity());
		assertEquals(500, ErrorKind.INTERNAL.status());
		assertEquals(ErrorKind.Severity.ERROR, ErrorKind.INTERNAL.severity());
		assertEquals(409, ErrorKind.CONFLICT_MODIFIED.status());
		assertEquals(503, ErrorKind.CONNECTION_UNAVAILABLE.status());
		assertEquals(ErrorKind.Severity.WARN, ErrorKind.CONNECTION_UNAVAILABLE.severity());
	}
}
