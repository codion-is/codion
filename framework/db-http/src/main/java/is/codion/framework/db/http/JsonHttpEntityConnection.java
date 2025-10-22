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

import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.domain.EntityObjectMapperFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
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
 * A Http based {@link EntityConnection} implementation based on EntityJsonService
 */
final class JsonHttpEntityConnection extends AbstractHttpEntityConnection {

	private final ObjectMapper objectMapper;

	JsonHttpEntityConnection(DefaultBuilder builder) {
		super(builder, "/entities/json/");
		this.objectMapper = databaseObjectMapper(EntityObjectMapperFactory.instance(entities().domainType()).entityObjectMapper(entities));
	}

	@Override
	public boolean transactionOpen() {
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("isTransactionOpen")),
								objectMapper, Boolean.class);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public void queryCache(boolean queryCache) {
		synchronized (httpClient) {
			try {
				handleResponse(execute(createJsonRequest("setQueryCacheEnabled",
								objectMapper.writeValueAsString(queryCache))));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public boolean queryCache() {
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("isQueryCacheEnabled")),
								objectMapper, Boolean.class);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public Collection<Entity.Key> insert(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("insert",
								objectMapper.writeValueAsString(entities))), objectMapper, KEY_LIST_REFERENCE);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public Collection<Entity> insertSelect(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("insertSelect",
								objectMapper.writeValueAsString(entities))), objectMapper, ENTITY_LIST_REFERENCE);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public void update(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (httpClient) {
			try {
				throwIfError(execute(createJsonRequest("update",
								objectMapper.writeValueAsString(entities))));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public Collection<Entity> updateSelect(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("updateSelect",
								objectMapper.writeValueAsString(entities))), objectMapper, ENTITY_LIST_REFERENCE);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public int update(Update update) {
		requireNonNull(update);
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("updateByCondition",
								objectMapper.writeValueAsString(update))), objectMapper, Integer.class);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public void delete(Collection<Entity.Key> keys) {
		requireNonNull(keys);
		synchronized (httpClient) {
			try {
				throwIfError(execute(createJsonRequest("deleteByKey",
								objectMapper.writeValueAsString(keys))));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public int delete(Condition condition) {
		requireNonNull(condition);
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("delete",
								objectMapper.writeValueAsString(condition))), objectMapper, Integer.class);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
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
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("values", node.toString())),
								objectMapper, objectMapper.getTypeFactory().constructCollectionType(List.class, column.type().valueClass()));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public List<Entity> select(Collection<Entity.Key> keys) {
		requireNonNull(keys);
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("selectByKey",
								objectMapper.writeValueAsString(keys))), objectMapper, ENTITY_LIST_REFERENCE);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public List<Entity> select(Select select) {
		requireNonNull(select);
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("select",
								objectMapper.writeValueAsString(select))), objectMapper, ENTITY_LIST_REFERENCE);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities) {
		requireNonNull(entities);
		synchronized (httpClient) {
			try {
				Map<EntityType, Collection<Entity>> dependencies = new HashMap<>();
				DomainType domainType = entities().domainType();
				Map<String, Collection<Entity>> dependencyMap = handleJsonResponse(execute(createJsonRequest("dependencies",
								objectMapper.writeValueAsString(entities))), objectMapper, new TypeReference<Map<String, Collection<Entity>>>() {});
				dependencyMap.forEach((entityTypeName, deps) ->
								dependencies.put(domainType.entityType(entityTypeName), deps));

				return dependencies;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	@Override
	public int count(Count count) {
		requireNonNull(count);
		synchronized (httpClient) {
			try {
				return handleJsonResponse(execute(createJsonRequest("count",
								objectMapper.writeValueAsString(count))), objectMapper, Integer.class);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw logAndWrap(e);
			}
			catch (Exception e) {
				throw logAndWrap(e);
			}
		}
	}

	private HttpRequest createJsonRequest(String path) {
		return HttpRequest.newBuilder()
						.timeout(socketTimeout)
						.uri(URI.create(baseurl + path))
						.POST(BodyPublishers.noBody())
						.headers(headers)
						.build();
	}

	private HttpRequest createJsonRequest(String path, String data) {
		return HttpRequest.newBuilder()
						.timeout(socketTimeout)
						.uri(URI.create(baseurl + path))
						.POST(BodyPublishers.ofString(data))
						.headers(headers)
						.build();
	}

	private static <T> T handleJsonResponse(HttpResponse<?> response, ObjectMapper mapper, TypeReference<T> typeReference) throws Exception {
		throwIfError(response);

		return mapper.readValue(new String((byte[]) response.body(), UTF_8), typeReference);
	}

	private static <T> T handleJsonResponse(HttpResponse<?> response, ObjectMapper mapper, Class<T> valueClass) throws Exception {
		throwIfError(response);

		return mapper.readValue(new String((byte[]) response.body(), UTF_8), valueClass);
	}

	private static <T> T handleJsonResponse(HttpResponse<?> response, ObjectMapper mapper, JavaType javaType) throws Exception {
		throwIfError(response);

		return mapper.readValue(new String((byte[]) response.body(), UTF_8), javaType);
	}
}
