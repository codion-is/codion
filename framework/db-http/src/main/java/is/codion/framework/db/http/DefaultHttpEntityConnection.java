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
 * Copyright (c) 2017 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static is.codion.common.Serializer.serialize;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService
 */
final class DefaultHttpEntityConnection extends AbstractHttpEntityConnection {

	DefaultHttpEntityConnection(DefaultBuilder builder) {
		super(builder, "/entities/serial/");
	}

	@Override
	public boolean transactionOpen() {
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("isTransactionOpen")));
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public void setQueryCacheEnabled(boolean queryCacheEnabled) {
		try {
			synchronized (httpClient) {
				handleResponse(execute(createRequest("setQueryCacheEnabled", serialize(queryCacheEnabled))));
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public boolean isQueryCacheEnabled() {
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("isQueryCacheEnabled")));
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public Collection<Entity.Key> insert(Collection<Entity> entities) throws DatabaseException {
		requireNonNull(entities);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("insert", serialize(entities))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public Collection<Entity> insertSelect(Collection<Entity> entities) throws DatabaseException {
		requireNonNull(entities);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("insertSelect", serialize(entities))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public void update(Collection<Entity> entities) throws DatabaseException {
		requireNonNull(entities);
		try {
			synchronized (httpClient) {
				handleResponse(execute(createRequest("update", serialize(entities))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public Collection<Entity> updateSelect(Collection<Entity> entities) throws DatabaseException {
		requireNonNull(entities);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("updateSelect", serialize(entities))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public int update(Update update) throws DatabaseException {
		requireNonNull(update);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("updateByCondition", serialize(update))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public void delete(Collection<Entity.Key> keys) throws DatabaseException {
		requireNonNull(keys);
		try {
			synchronized (httpClient) {
				throwIfError(execute(createRequest("deleteByKey", serialize(keys))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public int delete(Condition condition) throws DatabaseException {
		requireNonNull(condition);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("delete", serialize(condition))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public <T> List<T> select(Column<T> column, Select select) throws DatabaseException {
		requireNonNull(column);
		requireNonNull(select);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("values", serialize(asList(column, select)))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public List<Entity> select(Collection<Entity.Key> keys) throws DatabaseException {
		requireNonNull(keys);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("selectByKey", serialize(keys))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public List<Entity> select(Select select) throws DatabaseException {
		requireNonNull(select);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("select", serialize(select))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities) throws DatabaseException {
		requireNonNull(entities);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("dependencies", serialize(entities))));
			}
		}
		catch (DatabaseException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw logAndWrap(e);
		}
		catch (Exception e) {
			throw logAndWrap(e);
		}
	}

	@Override
	public int count(Count count) throws DatabaseException {
		requireNonNull(count);
		try {
			synchronized (httpClient) {
				return handleResponse(execute(createRequest("count", serialize(count))));
			}
		}
		catch (DatabaseException e) {
			throw e;
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
