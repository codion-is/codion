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
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static is.codion.common.Serializer.serialize;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService
 */
final class DefaultHttpEntityConnection extends AbstractHttpEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(), Locale.getDefault());

  private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpEntityConnection.class);

  DefaultHttpEntityConnection(DefaultBuilder builder) {
    super(builder, "/entities/ser/");
  }

  @Override
  public boolean transactionOpen() {
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("isTransactionOpen")));
      }
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
    catch (RuntimeException e) {
      throw e;
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
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Collection<Entity.Key> insert(Collection<? extends Entity> entities) throws DatabaseException {
    requireNonNull(entities);
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("insert", serialize(entities))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Collection<Entity> insertSelect(Collection<? extends Entity> entities) throws DatabaseException {
    requireNonNull(entities);
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("insertSelect", serialize(entities))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void update(Collection<? extends Entity> entities) throws DatabaseException {
    requireNonNull(entities);
    try {
      synchronized (httpClient) {
        handleResponse(execute(createRequest("update", serialize(entities))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Collection<Entity> updateSelect(Collection<? extends Entity> entities) throws DatabaseException {
    requireNonNull(entities);
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("updateSelect", serialize(entities))));
      }
    }
    catch (DatabaseException e) {
      throw e;
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
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public List<Entity> select(Collection<Entity.Key> keys) throws DatabaseException {
    requireNonNull(keys, "keys");
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("selectByKey", serialize(keys))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public List<Entity> select(Select select) throws DatabaseException {
    requireNonNull(select, "select");
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("select", serialize(select))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Map<EntityType, Collection<Entity>> dependencies(Collection<? extends Entity> entities) throws DatabaseException {
    requireNonNull(entities, "entities");
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("dependencies", serialize(entities))));
      }
    }
    catch (DatabaseException e) {
      throw e;
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
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }
}
