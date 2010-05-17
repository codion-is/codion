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
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.apache.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    String sql = "";
    try {
      final String selectQuery = EntityRepository.getSelectQuery(criteria.getEntityID());
      if (selectQuery == null) {
        final String datasource = EntityRepository.getSelectTableName(criteria.getEntityID());

        sql = EntityDbConnection.getSelectSQL(datasource, EntityRepository.getSelectColumnsString(criteria.getEntityID()),
                criteria.getWhereClause(null, PREPARED_VALUE_PROVIDER,
                        !datasource.toLowerCase().contains("where")), criteria.getOrderByClause());
      }
      else {
        sql = selectQuery + " " + criteria.getWhereClause(null, PREPARED_VALUE_PROVIDER,
                !selectQuery.toLowerCase().contains("where"));
      }
      statement = getConnection().prepareStatement(sql);

      final List<?> values = criteria.getValues();
      setValues(statement, values, criteria.getTypes());

      resultSet = executePreparedSelect(statement, sql, values);

      final List<Entity> result = getEntityResultPacker(criteria.getEntityID()).pack(resultSet, criteria.getFetchCount());

      setForeignKeyValues(result, criteria);

      return result;
    }
    catch (SQLException exception) {
      log.info(sql);
      log.error(this, exception);
      throw new DbException(exception, sql, getDatabase().getErrorMessage(exception));
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
    try {
      final String selectQuery = EntityRepository.getSelectQuery(criteria.getEntityID());
      String sql;
      if (selectQuery == null) {
        final String datasource = EntityRepository.getSelectTableName(criteria.getEntityID());
        sql = getSelectSQL(datasource, "count(*)", criteria.getWhereClause(getDatabase(), PREPARED_VALUE_PROVIDER,
                !datasource.toLowerCase().contains("where")), null);

      }
      else {
        sql = getSelectSQL("(" + selectQuery + " " + criteria.getWhereClause(getDatabase(), PREPARED_VALUE_PROVIDER,
                !selectQuery.toLowerCase().contains("where")) + ") alias", "count(*)", null, null);
      }

      statement = getConnection().prepareStatement(sql);
      final List<?> values = criteria.getValues();
      setValues(statement, values, criteria.getTypes());

      resultSet = executePreparedSelect(statement, sql, values);

      final List<Integer> result = INT_PACKER.pack(resultSet, -1);
      if (result.size() == 0)
        throw new RecordNotFoundException("Record count query returned no value");
      else if (result.size() > 1)
        throw new DbException("Record count query returned multiple values");

      return result.get(0);
    }
    catch (SQLException e) {
      log.error(this, e);
      throw new DbException(e, null, getDatabase().getErrorMessage(e));
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
    try {
      final Map<String, Collection<Entity.Key>> hashed = EntityUtil.hashKeysByEntityID(entities);
      for (final String entityID : hashed.keySet())
        if (EntityRepository.isReadOnly(entityID))
          throw new DbException("Cannot delete a read only entity: " + entityID);

      for (final Map.Entry<String, Collection<Entity.Key>> entry : hashed.entrySet()) {
        final List<Property.PrimaryKeyProperty> properties = EntityRepository.getPrimaryKeyProperties(entry.getKey());
        final String sql = "delete from " + EntityRepository.getTableName(entry.getKey())
                + getWhereCondition(properties);
        statement = getConnection().prepareStatement(sql);
        final List<Object> values = new ArrayList<Object>(properties.size());
        final List<Integer> types = new ArrayList<Integer>(properties.size());
        for (final Entity.Key key : entry.getValue()) {
          values.clear();
          types.clear();
          for (final Property property : properties) {
            values.add(key.getOriginalValue(property.getPropertyID()));
            types.add(property.getType());
          }
          setValues(statement, values, types);
          executePreparedUpdate(statement, sql, values);
        }
        statement.close();
      }
      if (!isTransactionOpen())
        getConnection().commit();
    }
    catch (SQLException e) {
      rollbackQuietly();
      log.error(this, e);
      throw new DbException(e, null, getDatabase().getErrorMessage(e));
    }
    finally {
      cleanup(statement, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    try {
      if (EntityRepository.isReadOnly(criteria.getEntityID()))
        throw new DbException("Cannot delete a read only entity: " + criteria.getEntityID());

      final String sql = getDeleteSQL(getDatabase(), criteria, PREPARED_VALUE_PROVIDER);
      statement = getConnection().prepareStatement(sql);
      final List<?> values = criteria.getValues();
      setValues(statement, values, criteria.getTypes());
      executePreparedUpdate(statement, sql, values);

      if (!isTransactionOpen())
        getConnection().commit();
    }
    catch (SQLException e) {
      rollbackQuietly();
      log.error(this, e);
      throw new DbException(e, null, getDatabase().getErrorMessage(e));
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

    PreparedStatement statement = null;
    try {
      final Map<String, Collection<Entity>> hashed = EntityUtil.hashByEntityID(entities);
      for (final String entityID : hashed.keySet())
        if (EntityRepository.isReadOnly(entityID))
          throw new DbException("Cannot update a read only entity: " + entityID);

      for (final Map.Entry<String, Collection<Entity>> entry : hashed.entrySet()) {
        final List<Property.PrimaryKeyProperty> pkProperties = EntityRepository.getPrimaryKeyProperties(entry.getKey());
        for (final Entity entity : entry.getValue()) {
          if (!entity.isModified())
            throw new DbException("Trying to update non-modified entity: " + entity);
          if (isOptimisticLocking())
            checkIfModified(entity);
          
          final Collection<Property> updateProperties = getUpdateProperties(entity);
          final String sql = getUpdateSQL(entity, updateProperties, pkProperties);
          final List<Object> values = new ArrayList<Object>(updateProperties.size());
          final List<Integer> types = new ArrayList<Integer>(updateProperties.size());
          for (final Property property : updateProperties) {
            values.add(entity.getValue(property.getPropertyID()));
            types.add(property.getType());
          }
          for (final Property.PrimaryKeyProperty pkProperty : pkProperties) {
            values.add(entity.getPrimaryKey().getOriginalValue(pkProperty.getPropertyID()));
            types.add(pkProperty.getType());
          }

          statement = getConnection().prepareStatement(sql);
          setValues(statement, values, types);
          executePreparedUpdate(statement, sql, values);
          statement.close();
        }
      }
      if (!isTransactionOpen())
        getConnection().commit();

      return selectMany(EntityUtil.getPrimaryKeys(entities));
    }
    catch (SQLException e) {
      rollbackQuietly();
      log.error(this, e);
      throw new DbException(e, null, getDatabase().getErrorMessage(e));
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
    try {
      for (final Entity entity : entities) {
        if (EntityRepository.isReadOnly(entity.getEntityID()))
          throw new DbException("Cannot insert a read only entity: " + entity.getEntityID());
        final IdSource idSource = EntityRepository.getIdSource(entity.getEntityID());
        if (idSource.isQueried() && entity.getPrimaryKey().isNull())
          entity.setValue(entity.getPrimaryKey().getFirstKeyProperty().getPropertyID(),
                  queryNewIdValue(entity.getEntityID(), idSource));

        final Collection<Property> insertProperties = getInsertProperties(entity);
        final String sql = getInsertSQL(entity.getEntityID(), insertProperties);
        final List<Object> values = new ArrayList<Object>(insertProperties.size());
        statement = getConnection().prepareStatement(sql);
        int i = 1;
        for (final Property property : insertProperties) {
          final Object value = entity.getValue(property);
          values.add(value);
          setStatementValue(statement, i++, value, property.getType());
        }

        executePreparedUpdate(statement, sql, values);

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
      rollbackQuietly();
      log.error(this, e);
      throw new DbException(e, null, getDatabase().getErrorMessage(e));
    }
    finally {
      cleanup(statement, null);
    }
  }

  /**
   * @param entity the Entity instance
   * @return a query for updating this entity instance
   * @throws DbException in case the entity is unmodified or it contains no modified updatable properties
   */
  static String getUpdateSQL(final Entity entity, final Collection<Property> properties,
                             final List<Property.PrimaryKeyProperty> pkProperties) throws DbException {
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

    return sql.append(getWhereCondition(pkProperties)).toString();
  }

  /**
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
      if (!isTransactionOpen())
        getConnection().rollback();
    }
    catch (SQLException e) {/**/}
  }

  private void setValues(final PreparedStatement statement, final Collection<?> values, final List<Integer> types) throws SQLException {
    if (values == null || values.size() == 0)
      return;
    if (types == null || types.size() != values.size())
      throw new RuntimeException("Types not specified: " + (types == null ? "no types" : ("expected: " + values.size() + ", got: " + types.size())));

    if (statement.getParameterMetaData().getParameterCount() == 0)
      return;

    int i = 1;
    for (final Object value : values) {
      setStatementValue(statement, i, value, types.get(i-1));
      i++;
    }
  }

  private void setStatementValue(final PreparedStatement statement, final int index, final Object value, final int type) throws SQLException {
    if (value == null)
      statement.setNull(index, type);
    else
     statement.setObject(index, value, type);
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
