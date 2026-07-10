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
 * Copyright (c) 2017 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.exception.DeleteEntityException;
import is.codion.framework.db.exception.EntityModifiedException;
import is.codion.framework.db.exception.EntityNotFoundException;
import is.codion.framework.db.exception.InsertEntityException;
import is.codion.framework.db.exception.MultipleEntitiesFoundException;
import is.codion.framework.db.exception.UpdateEntityException;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.db.DatabaseObjectMapper;
import is.codion.framework.json.db.ErrorEnvelope;
import is.codion.framework.json.db.ErrorKind;
import is.codion.framework.json.domain.EntityObjectMapperFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.framework.json.db.DatabaseObjectMapper.databaseObjectMapper;
import static is.codion.framework.json.domain.EntityObjectMapper.ENTITY_LIST_REFERENCE;
import static is.codion.framework.json.domain.EntityObjectMapper.KEY_LIST_REFERENCE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * An Http based {@link EntityConnection} implementation based on EntityService
 */
final class JsonHttpEntityConnection extends AbstractHttpEntityConnection {

	private static final String PARAMETER = "parameter";

	private final DatabaseObjectMapper objectMapper;

	JsonHttpEntityConnection(DefaultBuilder builder) {
		super(builder, "/entities/json/");
		this.objectMapper = databaseObjectMapper(EntityObjectMapperFactory.instance(entities().domainType()).entityObjectMapper(entities));
	}

	@Override
	public boolean transactionOpen() {
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("isTransactionOpen")),
								objectMapper, Boolean.class);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}


	@Override
	public Collection<Entity.Key> insert(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("insert",
								objectMapper.writeValueAsString(entities))), objectMapper, KEY_LIST_REFERENCE);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public Collection<Entity> insertSelect(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("insertSelect",
								objectMapper.writeValueAsString(entities))), objectMapper, ENTITY_LIST_REFERENCE);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public void update(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (transport) {
			try {
				throwIfError(execute(createJsonRequest("update",
								objectMapper.writeValueAsString(entities))));
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public Collection<Entity> updateSelect(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("updateSelect",
								objectMapper.writeValueAsString(entities))), objectMapper, ENTITY_LIST_REFERENCE);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public int update(Update update) {
		requireNonNull(update);
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("updateByCondition",
								objectMapper.writeValueAsString(update))), objectMapper, Integer.class);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public void delete(Collection<Entity.Key> keys) {
		requireNonNull(keys);
		synchronized (transport) {
			try {
				throwIfError(execute(createJsonRequest("deleteByKey",
								objectMapper.writeValueAsString(keys))));
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public int delete(Condition condition) {
		requireNonNull(condition);
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("delete",
								objectMapper.writeValueAsString(condition))), objectMapper, Integer.class);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public <T> List<T> select(Column<T> column, Select select) {
		requireNonNull(column);
		requireNonNull(select);
		ObjectNode node = objectMapper.createObjectNode();
		node.set("column", objectMapper.valueToTree(column.name()));
		node.set("entityType", objectMapper.valueToTree(column.entityType().name()));
		node.set("condition", objectMapper.valueToTree(select));
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("values", node.toString())),
								objectMapper, objectMapper.getTypeFactory().constructCollectionType(List.class, column.type().valueClass()));
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public List<Entity> select(Collection<Entity.Key> keys) {
		requireNonNull(keys);
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("selectByKey",
								objectMapper.writeValueAsString(keys))), objectMapper, ENTITY_LIST_REFERENCE);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public List<Entity> select(Select select) {
		requireNonNull(select);
		synchronized (transport) {
			List<Entity> cached = cachedResult(select);
			if (cached != null) {
				return cached;
			}
			try {
				return cacheResult(select, handleJsonResponse(execute(createJsonRequest("select",
								objectMapper.writeValueAsString(select))), objectMapper, ENTITY_LIST_REFERENCE));
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (transport) {
			try {
				Map<EntityType, Collection<Entity>> dependencies = new HashMap<>();
				DomainType domainType = entities().domainType();
				Map<String, Collection<Entity>> dependencyMap = handleJsonResponse(execute(createJsonRequest("dependencies",
								objectMapper.writeValueAsString(entities))), objectMapper, new TypeReference<Map<String, Collection<Entity>>>() {});
				dependencyMap.forEach((entityTypeName, deps) ->
								dependencies.put(domainType.entityType(entityTypeName), deps));

				return dependencies;
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public int count(Count count) {
		requireNonNull(count);
		synchronized (transport) {
			try {
				return handleJsonResponse(execute(createJsonRequest("count",
								objectMapper.writeValueAsString(count))), objectMapper, Integer.class);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public <C extends EntityConnection, P, R> R execute(FunctionType<C, P, R> functionType, P parameter) {
		requireNonNull(functionType);
		synchronized (transport) {
			try {
				ObjectNode request = objectMapper.createObjectNode();
				request.put("functionType", functionType.name());
				if (parameter != null) {
					request.set(PARAMETER, objectMapper.valueToTree(parameter));
				}
				//both ends resolve the return type from the domain's object mapper,
				//so there is one source of truth and no type name on the wire
				JavaType returnType = objectMapper.entityObjectMapper().returnType(functionType).get();

				return handleJsonResponse(execute(createJsonRequest("function", request.toString())), objectMapper, returnType);
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public <C extends EntityConnection, P> void execute(ProcedureType<C, P> procedureType, P parameter) {
		requireNonNull(procedureType);
		synchronized (transport) {
			try {
				ObjectNode request = objectMapper.createObjectNode();
				request.put("procedureType", procedureType.name());
				if (parameter != null) {
					request.set(PARAMETER, objectMapper.valueToTree(parameter));
				}

				throwIfError(execute(createJsonRequest("procedure", request.toString())));
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	@Override
	public <T, P, R> R report(ReportType<T, P, R> reportType, P parameter) {
		requireNonNull(reportType);
		synchronized (transport) {
			try {
				ObjectNode request = objectMapper.createObjectNode();
				request.put("reportType", reportType.name());
				if (parameter != null) {
					request.set(PARAMETER, objectMapper.valueToTree(parameter));
				}

				return handleResponse(execute(createJsonRequest("report", request.toString())));
			}
			catch (Exception exception) {
				throw handleException(exception);
			}
		}
	}

	/**
	 * Reconstructs the exception the given error envelope describes.
	 * <p>The envelope's {@link ErrorKind} maps to a known constructor, nothing on the wire names a class. An
	 * unrecognized kind, which a client older than the server encounters, yields a plain {@link DatabaseException}.
	 * <p>Note that neither a stack trace nor a cause crosses the wire, so the returned exception's stack trace
	 * is that of the client throw site, not the server one. The {@link ErrorEnvelope#correlationId()} identifies
	 * the server log entry.
	 */
	@Override
	protected Exception decodeError(HttpTransport.Response response) {
		ErrorEnvelope envelope;
		try {
			envelope = ErrorEnvelope.fromJson(response.body());
		}
		catch (IOException e) {
			//not an envelope at all, a proxy error page or an unmatched route
			return new DatabaseException("HTTP " + response.statusCode() + ": " + new String(response.body(), UTF_8));
		}

		return envelope.errorKind()
						.map(kind -> exception(kind, envelope))
						.orElseGet(() -> new DatabaseException(envelope.message()));
	}

	private Exception exception(ErrorKind kind, ErrorEnvelope envelope) {
		String message = envelope.message();
		switch (kind) {
			case AUTHENTICATION:
				return new AuthenticationException(message);
			case BAD_REQUEST:
				return new IllegalArgumentException(message);
			case ILLEGAL_STATE:
				return new IllegalStateException(message);
			case CONFLICT_MODIFIED:
				return entityModified(envelope, message);
			case CONFLICT_REFERENTIAL:
				return referentialIntegrity(envelope, message);
			case CONFLICT_UNIQUE:
				return new UniqueConstraintException(message);
			case NOT_FOUND:
				return new EntityNotFoundException(message);
			case MULTIPLE_FOUND:
				return new MultipleEntitiesFoundException(message);
			case INSERT:
				return new InsertEntityException(message);
			case UPDATE:
				return new UpdateEntityException(message);
			case DELETE:
				return new DeleteEntityException(message);
			case QUERY_TIMEOUT:
				return new QueryTimeoutException(message);
			default:
				//CONNECTION_UNAVAILABLE, DATABASE and INTERNAL, none of which the client has a type for
				return new DatabaseException(message);
		}
	}

	/**
	 * Decoding an error must never throw, its job being to return the exception to throw. A detail this client
	 * cannot make sense of, an Operation named by a newer server, degrades to the generic exception.
	 */
	private static Exception referentialIntegrity(ErrorEnvelope envelope, String message) {
		JsonNode detail = envelope.detail();
		if (detail == null || !detail.has(ErrorEnvelope.OPERATION)) {
			return new DatabaseException(message);
		}
		try {
			return new ReferentialIntegrityException(message, Operation.valueOf(detail.get(ErrorEnvelope.OPERATION).asText()));
		}
		catch (IllegalArgumentException e) {
			return new DatabaseException(message);
		}
	}

	private Exception entityModified(ErrorEnvelope envelope, String message) {
		JsonNode detail = envelope.detail();
		if (detail == null) {
			//the server could not resolve an object mapper for the domain, keeping the supertype rather than failing
			return new UpdateEntityException(message);
		}
		try {
			return new EntityModifiedException(objectMapper.readValue(detail.get(ErrorEnvelope.ENTITY).toString(), Entity.class),
							detail.get(ErrorEnvelope.MODIFIED).isNull() ? null
											: objectMapper.readValue(detail.get(ErrorEnvelope.MODIFIED).toString(), Entity.class),
							columns(detail.get(ErrorEnvelope.COLUMNS)), message);
		}
		catch (Exception e) {
			//not only IOException, a column or entity type named by a newer server's domain
			//reaches this through columns(), which is precisely the skew this catch exists for
			return new UpdateEntityException(message);
		}
	}

	private Collection<Column<?>> columns(JsonNode columns) {
		Collection<Column<?>> modified = new ArrayList<>();
		DomainType domainType = entities().domainType();
		for (JsonNode column : columns) {
			String qualified = column.asText();
			int separator = qualified.lastIndexOf('.');
			EntityType entityType = domainType.entityType(qualified.substring(0, separator));
			modified.add((Column<?>) entities().definition(entityType).attributes().getOrThrow(qualified.substring(separator + 1)));
		}

		return modified;
	}

	private Request createJsonRequest(String path) {
		return createRequest(path);
	}

	private Request createJsonRequest(String path, String data) {
		return createRequest(path, data.getBytes(UTF_8));
	}

	private <T> T handleJsonResponse(HttpTransport.Response response, ObjectMapper mapper, TypeReference<T> typeReference) throws Exception {
		throwIfError(response);

		return mapper.readValue(new String(response.body(), UTF_8), typeReference);
	}

	private <T> T handleJsonResponse(HttpTransport.Response response, ObjectMapper mapper, Class<T> valueClass) throws Exception {
		throwIfError(response);

		return mapper.readValue(new String(response.body(), UTF_8), valueClass);
	}

	private <T> T handleJsonResponse(HttpTransport.Response response, ObjectMapper mapper, JavaType javaType) throws Exception {
		throwIfError(response);

		return mapper.readValue(new String(response.body(), UTF_8), javaType);
	}
}
