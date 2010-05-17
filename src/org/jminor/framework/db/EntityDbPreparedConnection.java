/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.IdSource;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.apache.log4j.Logger;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 15.5.2010
 * Time: 14:19:29
 */
public class EntityDbPreparedConnection extends EntityDbConnection implements EntityDb {

  private static final Logger log = Util.getLogger(EntityDbPreparedConnection.class);

  private static final Criteria.ValueProvider PREPARED_VALUE_PROVIDER = new Criteria.ValueProvider() {
    public String getSQLString(final Database database, final Object columnKey, final Object value) {
      return "?";
    }
  };

  public EntityDbPreparedConnection(final Database database, final User user) throws ClassNotFoundException, SQLException {
    super(database, user);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = "";
    try {
      selectQuery = initializeSelectQuery(criteria, PREPARED_VALUE_PROVIDER,
              EntityRepository.getSelectColumnsString(criteria.getEntityID()), criteria.getOrderByClause());

      statement = getConnection().prepareStatement(selectQuery);
      final List<?> values = criteria.getValues();
      setStatementValues(statement, values, criteria.getValueProperties());

      resultSet = executePreparedSelect(statement, selectQuery, values);

      final List<Entity> result = getEntityResultPacker(criteria.getEntityID()).pack(resultSet, criteria.getFetchCount());

      setForeignKeyValues(result, criteria);

      return result;
    }
    catch (SQLException exception) {
      log.info(selectQuery);
      log.error(this, exception);
      throw new DbException(exception, selectQuery, getDatabase().getErrorMessage(exception));
    }
    finally {
      cleanup(statement, resultSet);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int selectRowCount(final EntityCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = "";
    try {
      selectQuery = EntityRepository.getSelectQuery(criteria.getEntityID());
      selectQuery = getSelectSQL(selectQuery == null ? EntityRepository.getSelectTableName(criteria.getEntityID()) :
              "(" + selectQuery + " " + criteria.getWhereClause(getDatabase(), PREPARED_VALUE_PROVIDER,
                      !selectQuery.toLowerCase().contains("where")) + ") alias", "count(*)", null, null);
      selectQuery += " " + criteria.getWhereClause(null, PREPARED_VALUE_PROVIDER, !containsWhereKeyword(selectQuery));

      statement = getConnection().prepareStatement(selectQuery);
      final List<?> values = criteria.getValues();
      setStatementValues(statement, values, criteria.getValueProperties());

      resultSet = executePreparedSelect(statement, selectQuery, values);

      final List<Integer> result = INT_PACKER.pack(resultSet, -1);

      if (result.size() == 0)
        throw new RecordNotFoundException("Record count query returned no value");
      else if (result.size() > 1)
        throw new DbException("Record count query returned multiple values");

      return result.get(0);
    }
    catch (SQLException e) {
      log.info(selectQuery);
      log.error(this, e);
      throw new DbException(e, selectQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      cleanup(statement, resultSet);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final List<Entity.Key> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return;

    PreparedStatement statement = null;
    String deleteQuery = "";
    try {
      final Map<String, Collection<Entity.Key>> hashed = EntityUtil.hashKeysByEntityID(entities);
      for (final String entityID : hashed.keySet())
        if (EntityRepository.isReadOnly(entityID))
          throw new DbException("Cannot delete a read only entity: " + entityID);

      for (final Map.Entry<String, Collection<Entity.Key>> entry : hashed.entrySet()) {
        final List<Property.PrimaryKeyProperty> primaryKeyProperties = EntityRepository.getPrimaryKeyProperties(entry.getKey());
        deleteQuery = "delete from " + EntityRepository.getTableName(entry.getKey()) + getWhereCondition(primaryKeyProperties);
        statement = getConnection().prepareStatement(deleteQuery);
        final List<Object> values = new ArrayList<Object>(primaryKeyProperties.size());
        final List<Property> properties = new ArrayList<Property>(primaryKeyProperties.size());
        for (final Entity.Key key : entry.getValue()) {
          values.clear();
          properties.clear();
          for (final Property property : primaryKeyProperties) {
            values.add(key.getOriginalValue(property.getPropertyID()));
            properties.add(property);
          }
          setStatementValues(statement, values, properties);
          executePreparedUpdate(statement, deleteQuery, values);
        }
        statement.close();
      }
      if (!isTransactionOpen())
        getConnection().commit();
    }
    catch (SQLException e) {
      if (!isTransactionOpen())
        rollbackQuietly();
      log.info(deleteQuery);
      log.error(this, e);
      throw new DbException(e, deleteQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      cleanup(statement, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    String deleteQuery = "";
    try {
      if (EntityRepository.isReadOnly(criteria.getEntityID()))
        throw new DbException("Cannot delete a read only entity: " + criteria.getEntityID());

      deleteQuery = getDeleteSQL(getDatabase(), criteria, PREPARED_VALUE_PROVIDER);
      statement = getConnection().prepareStatement(deleteQuery);

      final List<?> values = criteria.getValues();
      setStatementValues(statement, values, criteria.getValueProperties());
      executePreparedUpdate(statement, deleteQuery, values);

      if (!isTransactionOpen())
        getConnection().commit();
    }
    catch (SQLException e) {
      if (!isTransactionOpen())
        rollbackQuietly();
      log.info(deleteQuery);
      log.error(this, e);
      throw new DbException(e, deleteQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      cleanup(statement, null);
    }
  }

  /** {@inheritDoc} */
  @Override
   public List<Entity> update(List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return entities;

    String updateQuery = "";
    PreparedStatement statement = null;
    try {
      final Map<String, Collection<Entity>> hashed = EntityUtil.hashByEntityID(entities);
      for (final String entityID : hashed.keySet())
        if (EntityRepository.isReadOnly(entityID))
          throw new DbException("Cannot update a read only entity: " + entityID);

      for (final Map.Entry<String, Collection<Entity>> entry : hashed.entrySet()) {
        final List<Property.PrimaryKeyProperty> primaryKeyProperties = EntityRepository.getPrimaryKeyProperties(entry.getKey());
        for (final Entity entity : entry.getValue()) {
          if (!entity.isModified())
            throw new DbException("Trying to update non-modified entity: " + entity);
          if (isOptimisticLocking())
            checkIfModified(entity);

          final Collection<Property> updateProperties = getUpdateProperties(entity);
          updateQuery = getUpdateSQL(entity, updateProperties, primaryKeyProperties);
          final List<Object> values = new ArrayList<Object>(updateProperties.size());
          final List<Property> properties = new ArrayList<Property>(updateProperties.size());
          for (final Property property : updateProperties) {
            values.add(entity.getValue(property.getPropertyID()));
            properties.add(property);
          }
          for (final Property.PrimaryKeyProperty primaryKeyProperty : primaryKeyProperties) {
            values.add(entity.getPrimaryKey().getOriginalValue(primaryKeyProperty.getPropertyID()));
            properties.add(primaryKeyProperty);
          }

          statement = getConnection().prepareStatement(updateQuery);
          setStatementValues(statement, values, properties);
          executePreparedUpdate(statement, updateQuery, values);
          statement.close();
        }
      }
      if (!isTransactionOpen())
        getConnection().commit();

      return selectMany(EntityUtil.getPrimaryKeys(entities));
    }
    catch (SQLException e) {
      if (!isTransactionOpen())
        rollbackQuietly();
      log.error(this, e);
      throw new DbException(e, updateQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      cleanup(statement, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return new ArrayList<Entity.Key>();

    final List<Entity.Key> keys = new ArrayList<Entity.Key>(entities.size());
    PreparedStatement statement = null;
    String insertQuery = "";
    try {
      for (final Entity entity : entities) {
        if (EntityRepository.isReadOnly(entity.getEntityID()))
          throw new DbException("Cannot insert a read only entity: " + entity.getEntityID());
        final IdSource idSource = EntityRepository.getIdSource(entity.getEntityID());
        if (idSource.isQueried() && entity.getPrimaryKey().isNull())
          entity.setValue(entity.getPrimaryKey().getFirstKeyProperty().getPropertyID(),
                  queryNewIdValue(entity.getEntityID(), idSource));

        final Collection<Property> insertProperties = getInsertProperties(entity);
        insertQuery = getInsertSQL(entity.getEntityID(), insertProperties);
        final List<Object> values = new ArrayList<Object>(insertProperties.size());
        statement = getConnection().prepareStatement(insertQuery);
        int i = 1;
        for (final Property property : insertProperties) {
          final Object value = entity.getValue(property);
          values.add(value);
          setStatementValue(statement, i++, value, property);
        }

        executePreparedUpdate(statement, insertQuery, values);

        if (idSource.isAutoIncrement() && entity.getPrimaryKey().isNull())
          entity.setValue(entity.getPrimaryKey().getFirstKeyProperty().getPropertyID(),
                  queryInteger(getDatabase().getAutoIncrementValueSQL(EntityRepository.getEntityIdSource(entity.getEntityID()))));

        keys.add(entity.getPrimaryKey());

        statement.close();
      }
      if (!isTransactionOpen())
        getConnection().commit();

      return keys;
    }
    catch (SQLException e) {
      if (!isTransactionOpen())
        rollbackQuietly();
      log.info(insertQuery);
      log.error(this, e);
      throw new DbException(e, insertQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      cleanup(statement, null);
    }
  }

  /**
   * @param entity the Entity instance
   * @param properties the properties being updated
   * @param primaryKeyProperties the primary key properties for the given entity
   * @return a query for updating this entity instance
   * @throws DbException in case the entity is unmodified or it contains no modified updatable properties
   */
  static String getUpdateSQL(final Entity entity, final Collection<Property> properties,
                             final List<Property.PrimaryKeyProperty> primaryKeyProperties) throws DbException {
    if (!entity.isModified())
      throw new DbException("Can not get update sql for an unmodified entity");

    final StringBuilder sql = new StringBuilder("update ");
    sql.append(EntityRepository.getTableName(entity.getEntityID())).append(" set ");
    int columnIndex = 0;
    for (final Property property : properties) {
      sql.append(property.getColumnName()).append(" = ?");
      if (columnIndex++ < properties.size() - 1)
        sql.append(", ");
    }

    return sql.append(getWhereCondition(primaryKeyProperties)).toString();
  }

  /**
   * @param entityID the entityID
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting this entity instance
   */
  static String getInsertSQL(final String entityID, final Collection<Property> insertProperties) {
    final StringBuilder sql = new StringBuilder("insert into ");
    sql.append(EntityRepository.getTableName(entityID)).append("(");
    final StringBuilder columnValues = new StringBuilder(") values(");
    int columnIndex = 0;
    for (final Property property : insertProperties) {
      sql.append(property.getColumnName());
      columnValues.append("?");
      if (columnIndex++ < insertProperties.size() - 1) {
        sql.append(", ");
        columnValues.append(", ");
      }
    }

    return sql.append(columnValues).append(")").toString();
  }

  private void executePreparedUpdate(final PreparedStatement statement, final String sql,
                                     final List<?> values) throws SQLException {
    SQLException exception = null;
    try {
      methodLogger.logAccess("executePreparedUpdate", new Object[] {sql, values});
      statement.executeUpdate();
      return;
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      methodLogger.logExit("executePreparedUpdate", exception, null);
    }

    throw exception;
  }

  private ResultSet executePreparedSelect(final PreparedStatement statement, final String sql,
                                          final List<?> values) throws SQLException {
    SQLException exception = null;
    try {
      methodLogger.logAccess("executePreparedSelect", new Object[] {sql, values});
      return statement.executeQuery();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      methodLogger.logExit("executePreparedSelect", exception, null);
    }

    throw exception;
  }

  /**
   * @param properties the properties
   * @return a where clause according to this entity ID,
   * e.g. " where (idCol = ?)", " where (idCol1 = ?) and (idCol2 = ?)"
   */
  private static String getWhereCondition(final List<? extends Property> properties) {
    final StringBuilder ret = new StringBuilder(" where (");
    int i = 0;
    for (final Property property : properties) {
      ret.append(property.getPropertyID()).append(" = ?");
      if (i++ < properties.size() - 1)
        ret.append(" and ");
    }

    return ret.append(")").toString();
  }

  private void rollbackQuietly() {
    try {
      getConnection().rollback();
    }
    catch (SQLException e) {/**/}
  }

  private void setStatementValues(final PreparedStatement statement, final Collection<?> values, final List<Property> properties) throws SQLException {
    if (values == null || values.size() == 0)
      return;
    if (properties == null || properties.size() != values.size())
      throw new RuntimeException("Properties not specified: " + (properties == null ? "no types" : ("expected: " + values.size() + ", got: " + properties.size())));

    if (statement.getParameterMetaData().getParameterCount() == 0)
      return;

    int i = 1;
    for (final Object value : values) {
      setStatementValue(statement, i, value, properties.get(i-1));
      i++;
    }
  }

  private void setStatementValue(final PreparedStatement statement, final int index, final Object value, final Property property) throws SQLException {
    final int columnType = translateType(property);
    final Object columnValue = translateValue(property, value);
    try {
      if (columnValue == null)
        statement.setNull(index, columnType);
      else
       statement.setObject(index, columnValue, columnType);
    }
    catch (SQLException e) {
      System.out.println("Unable to set property value: " + property + ", value: " + value + ", value class: " + (value == null ? "null" : value.getClass()));
      throw e;
    }
  }

  private Object translateValue(final Property property, final Object value) {
    if (property.isBoolean()) {
      if (property instanceof Property.BooleanProperty)
        return ((Property.BooleanProperty) property).toSQLValue((Boolean) value);
      else
        return value == null ? null : ((Boolean) value ?
                Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_TRUE) :
                Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_FALSE));
    }
    else if (value instanceof java.util.Date)
      return new Date(((java.util.Date) value).getTime());

    return value;
  }

  private int translateType(final Property property) {
    if (property.isBoolean()) {
      if (property instanceof Property.BooleanProperty)
        return ((Property.BooleanProperty) property).getColumnType();
      else
        return Types.INTEGER;
    }

    return property.getType();
  }

  private void cleanup(final Statement statement, final ResultSet resultSet) {
    try {
      if (resultSet != null)
        resultSet.close();
    }
    catch (SQLException e) {/**/}
    try {
      if (statement != null)
        statement.close();
    }
    catch (SQLException e) {/**/}
  }
}
