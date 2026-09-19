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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static java.util.Objects.requireNonNull;

/**
 * The body of a JSON error response, the wire form of a server side exception.
 * <p>Carries a closed {@link ErrorKind}, a message, a correlation id linking the response to a server log entry,
 * and a kind specific {@code detail} node for the kinds whose exception cannot be reconstructed from a message alone.
 * <p>A database exception based on an error type carries the name of the {@code errorType} and the {@code errorDetail},
 * if any, with which the client puts the message together in its own language, the {@code message} being in the language
 * of the server.
 * <p>Neither a stack trace nor a cause chain is ever carried, in either direction. A client reconstructing an
 * exception from an envelope maps the {@link #kind()} to a known constructor; nothing on the wire names a class.
 * {@snippet :
 * {
 *   "kind" : "CONFLICT_REFERENTIAL",
 *   "message" : "This record is referenced by records in other tables, delete those first: EMP_DEPT_FK",
 *   "correlationId" : "b1f4c8e2-3a1d-4f0b-9c5e-7d2a6f8b1c3e",
 *   "detail" : { "operation" : "DELETE" },
 *   "errorType" : "CHILD_EXISTS",
 *   "errorDetail" : "EMP_DEPT_FK"
 * }
 *}
 * @see ErrorKind
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ErrorEnvelope {

	/**
	 * The {@code detail} field naming the operation a {@link ErrorKind#CONFLICT_REFERENTIAL} prevented.
	 */
	public static final String OPERATION = "operation";

	/**
	 * The {@code detail} field carrying the entity a {@link ErrorKind#CONFLICT_MODIFIED} was thrown for.
	 */
	public static final String ENTITY = "entity";

	/**
	 * The {@code detail} field carrying the current state of a {@link ErrorKind#CONFLICT_MODIFIED} entity, null if deleted.
	 */
	public static final String MODIFIED = "modified";

	/**
	 * The {@code detail} field listing the modified columns of a {@link ErrorKind#CONFLICT_MODIFIED} entity,
	 * as {@code entityTypeName.columnName}.
	 */
	public static final String COLUMNS = "columns";

	private static final ObjectMapper MAPPER = JsonMapper.builder()
					//a client older than the server must tolerate fields it does not know
					.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
					//(de)serialize through the public creator and getters alone, never the private fields. On the
					//module path Jackson can not setAccessible() on a private field of a package this module does not
					//open to it, and this envelope, being all public accessors, need never make it try
					.visibility(PropertyAccessor.FIELD, NONE)
					.disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
					.build();

	private final String kind;
	private final String message;
	private final String correlationId;
	private final @Nullable JsonNode detail;
	private final @Nullable String errorType;
	private final @Nullable String errorDetail;

	/**
	 * @param kind the {@link ErrorKind} name, a string rather than the enum so an unknown kind does not fail to parse
	 * @param message the error message
	 * @param correlationId identifies the server log entry describing this error
	 * @param detail kind specific detail, null if the kind carries none
	 * @param errorType the name of the {@link is.codion.common.db.exception.ErrorType} a database exception is based on,
	 * a string rather than the enum so an unknown error type does not fail to parse, null if it is not based on one
	 * @param errorDetail the error detail of a database exception based on an error type, null if none
	 */
	@JsonCreator
	public ErrorEnvelope(@JsonProperty("kind") String kind,
											 @JsonProperty("message") String message,
											 @JsonProperty("correlationId") String correlationId,
											 @JsonProperty("detail") @Nullable JsonNode detail,
											 @JsonProperty("errorType") @Nullable String errorType,
											 @JsonProperty("errorDetail") @Nullable String errorDetail) {
		this.kind = requireNonNull(kind);
		this.message = requireNonNull(message);
		this.correlationId = requireNonNull(correlationId);
		this.detail = detail;
		this.errorType = errorType;
		this.errorDetail = errorDetail;
	}

	/**
	 * @return the {@link ErrorKind} name, see {@link #errorKind()}
	 */
	@JsonProperty("kind")
	public String kind() {
		return kind;
	}

	/**
	 * @return the error message
	 */
	@JsonProperty("message")
	public String message() {
		return message;
	}

	/**
	 * @return the id identifying the server log entry describing this error
	 */
	@JsonProperty("correlationId")
	public String correlationId() {
		return correlationId;
	}

	/**
	 * @return the kind specific detail, or null if the kind carries none
	 */
	@JsonProperty("detail")
	public @Nullable JsonNode detail() {
		return detail;
	}

	/**
	 * Returns the name of the error type a database exception is based on. With it the client puts the message together
	 * in its own language, {@link #message()} being in the language of the server. A client not knowing the error type,
	 * one older than the server, or one not written in Java and not localizing, uses {@link #message()}.
	 * @return the name of the {@link is.codion.common.db.exception.ErrorType}, null if the exception is not based on one
	 * @see is.codion.common.db.exception.DatabaseException#errorType()
	 */
	@JsonProperty("errorType")
	public @Nullable String errorType() {
		return errorType;
	}

	/**
	 * @return the error detail, the name of a constraint, column or table for example, null if none
	 * @see is.codion.common.db.exception.DatabaseException#detail()
	 */
	@JsonProperty("errorDetail")
	public @Nullable String errorDetail() {
		return errorDetail;
	}

	/**
	 * @return the {@link ErrorKind}, an empty {@link Optional} in case this envelope carries a kind unknown here,
	 * which a client older than the server encounters
	 */
	public Optional<ErrorKind> errorKind() {
		return ErrorKind.of(kind);
	}

	@Override
	public String toString() {
		return kind + ": " + message + " [" + correlationId + "]";
	}

	/**
	 * @return this envelope as a JSON string
	 * @throws JsonProcessingException in case of an error
	 */
	public String toJson() throws JsonProcessingException {
		return MAPPER.writeValueAsString(this);
	}

	/**
	 * @param json the JSON to parse
	 * @return the {@link ErrorEnvelope} the given JSON represents
	 * @throws IOException in case the given JSON is not an error envelope
	 */
	public static ErrorEnvelope fromJson(byte[] json) throws IOException {
		return MAPPER.readValue(requireNonNull(json), ErrorEnvelope.class);
	}
}
