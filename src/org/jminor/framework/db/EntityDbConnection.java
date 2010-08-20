/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.DbConnectionImpl;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.IdSource;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the database layer accessible to the client.
 */
public final class EntityDbConnection extends DbConnectionImpl implements EntityDb {

  private static final Logger LOG = Util.getLogger(EntityDbConnection.class);

  private final Map<String, EntityResultPacker> entityResultPackers = new HashMap<String, EntityResultPacker>();
  private final Map<Integer, ResultPacker> propertyResultPackers = new HashMap<Integer, ResultPacker>();
  private boolean optimisticLocking = Configuration.getBooleanValue(Configuration.USE_OPTIMISTIC_LOCKING);
  private boolean limitForeignKeyFetchDepth = Configuration.getBooleanValue(Configuration.LIMIT_FOREIGN_KEY_FETCH_DEPTH);

  /**
   * Constructs a new EntityDbConnection instance
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @throws SQLException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   */
  public EntityDbConnection(final Database database, final User user) throws SQLException, ClassNotFoundException {
    super(database, user);
  }

  /**
   * Constructs a new EntityDbConnection instance
   * @param connection the connection object to base the entity db connection on
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @throws SQLException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   */
  public EntityDbConnection(final Connection connection, final Database database, final User user) throws SQLException {
    super(database, user, connection);
  }

  /**
   * @return true if optimistic locking is enabled
   */
  public boolean isOptimisticLocking() {
    return optimisticLocking;
  }

  /**
   * @param optimisticLocking true if optimistic locking should be enabled
   */
  public void setOptimisticLocking(final boolean optimisticLocking) {
    this.optimisticLocking = optimisticLocking;
  }

  /**
   * @return true if foreign key fetch depth is being limited
   */
  public boolean isLimitForeignKeyFetchDepth() {
    return limitForeignKeyFetchDepth;
  }

  /**
   * @param limitForeignKeyFetchDepth true if foreign key fetch depth should be limited
   */
  public void setLimitForeignKeyFetchDepth(final boolean limitForeignKeyFetchDepth) {
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
  }

  /** {@inheritDoc} */
  public List<Entity.Key> insert(final List<Entity> entities) throws DbException {
    if (entities == null || entities.isEmpty()) {
      return new ArrayList<Entity.Key>();
    }

    final List<Entity.Key> keys = new ArrayList<Entity.Key>(entities.size());
    PreparedStatement statement = null;
    String insertQuery = null;
    try {
      final List<Property.ColumnProperty> insertProperties = new ArrayList<Property.ColumnProperty>();
      final List<Object> statementValues = new ArrayList<Object>();
      for (final Entity entity : entities) {
        if (Entities.isReadOnly(entity.getEntityID())) {
          throw new DbException("Can not insert a read only entity: " + entity.getEntityID());
        }

        final IdSource idSource = Entities.getIdSource(entity.getEntityID());
        final Property.PrimaryKeyProperty firstPrimaryKeyProperty = entity.getPrimaryKey().getFirstKeyProperty();
        if (idSource.isQueried() && entity.getPrimaryKey().isNull()) {
          entity.setValue(firstPrimaryKeyProperty, queryNewIdValue(entity.getEntityID(), idSource, firstPrimaryKeyProperty));
        }

        addInsertProperties(entity, insertProperties, statementValues);
        insertQuery = getInsertSQL(entity.getEntityID(), insertProperties);
        statement = getConnection().prepareStatement(insertQuery);

        executePreparedUpdate(statement, insertQuery, statementValues, insertProperties);

        final Entity.Key primaryKey = entity.getPrimaryKey();
        if (idSource.isAutoIncrement() && entity.getPrimaryKey().isNull()) {
          primaryKey.setValue(firstPrimaryKeyProperty.getPropertyID(), queryInteger(getDatabase().getAutoIncrementValueSQL(
                  Entities.getEntityIdSource(entity.getEntityID()))));
        }

        keys.add(primaryKey);

        statement.close();
        insertProperties.clear();
        statementValues.clear();
      }
      if (!isTransactionOpen()) {
        commit();
      }

      return keys;
    }
    catch (SQLException e) {
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      LOG.info(insertQuery);
      LOG.error(this, e);
      throw new DbException(e, insertQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  public List<Entity> update(final List<Entity> entities) throws DbException {
    if (entities == null || entities.isEmpty()) {
      return entities;
    }

    String updateQuery = null;
    PreparedStatement statement = null;
    try {
      final Map<String, Collection<Entity>> hashedEntities = EntityUtil.hashByEntityID(entities);
      for (final String entityID : hashedEntities.keySet()) {
        if (Entities.isReadOnly(entityID)) {
          throw new DbException("Can not update a read only entity: " + entityID);
        }
      }
      final List<Object> statementValues = new ArrayList<Object>();
      final List<Property.ColumnProperty> statementProperties = new ArrayList<Property.ColumnProperty>();
      for (final Map.Entry<String, Collection<Entity>> entry : hashedEntities.entrySet()) {
        final List<Property.PrimaryKeyProperty> primaryKeyProperties = Entities.getPrimaryKeyProperties(entry.getKey());
        for (final Entity entity : entry.getValue()) {
          if (!entity.isModified()) {
            throw new DbException("Can not update an unmodified entity: " + entity);
          }
          addUpdateProperties(entity, statementProperties, statementValues);
          updateQuery = getUpdateSQL(entity, statementProperties, primaryKeyProperties);

          statementProperties.addAll(primaryKeyProperties);
          for (final Property.PrimaryKeyProperty primaryKeyProperty : primaryKeyProperties) {
            statementValues.add(entity.getOriginalValue(primaryKeyProperty.getPropertyID()));
          }
          if (optimisticLocking) {
            lockAndCheckForUpdate(entity);
          }
          statement = getConnection().prepareStatement(updateQuery);
          executePreparedUpdate(statement, updateQuery, statementValues, statementProperties);
          statement.close();
          statementValues.clear();
          statementProperties.clear();
        }
      }
      if (!isTransactionOpen()) {
        commit();
      }

      return selectMany(EntityUtil.getPrimaryKeys(entities));
    }
    catch (RecordModifiedException e) {
      if (optimisticLocking && !isTransactionOpen()) {
        rollbackQuietly();//releasing the select for update lock
      }
      LOG.error(this, e);
      throw e;
    }
    catch (SQLException e) {
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      LOG.error(this, e);
      throw new DbException(e, updateQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  public void delete(final EntityCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    String deleteQuery = null;
    try {
      if (Entities.isReadOnly(criteria.getEntityID())) {
        throw new DbException("Can not delete a read only entity: " + criteria.getEntityID());
      }
      deleteQuery = getDeleteSQL(criteria);
      statement = getConnection().prepareStatement(deleteQuery);
      executePreparedUpdate(statement, deleteQuery, criteria.getValues(), criteria.getValueProperties());

      if (!isTransactionOpen()) {
        commit();
      }
    }
    catch (SQLException e) {
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      LOG.info(deleteQuery);
      LOG.error(this, e);
      throw new DbException(e, deleteQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  public void delete(final List<Entity.Key> entityKeys) throws DbException {
    if (entityKeys == null || entityKeys.isEmpty()) {
      return;
    }

    PreparedStatement statement = null;
    String deleteQuery = null;
    try {
      final Map<String, Collection<Entity.Key>> hashedKeys = EntityUtil.hashKeysByEntityID(entityKeys);
      for (final String entityID : hashedKeys.keySet()) {
        if (Entities.isReadOnly(entityID)) {
          throw new DbException("Can not delete a read only entity: " + entityID);
        }
      }
      final ArrayList<Entity.Key> criteriaKeys = new ArrayList<Entity.Key>();
      for (final Map.Entry<String, Collection<Entity.Key>> entry : hashedKeys.entrySet()) {
        criteriaKeys.addAll(entry.getValue());
        final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(criteriaKeys);
        deleteQuery = "delete from " + Entities.getTableName(entry.getKey()) + " " + criteria.getWhereClause();
        statement = getConnection().prepareStatement(deleteQuery);
        executePreparedUpdate(statement, deleteQuery, criteria.getValues(), criteria.getValueProperties());
        statement.close();
        criteriaKeys.clear();
      }
      if (!isTransactionOpen()) {
        commit();
      }
    }
    catch (SQLException e) {
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      LOG.info(deleteQuery);
      LOG.error(this, e);
      throw new DbException(e, deleteQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DbException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, value));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final Entity.Key key) throws DbException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(key));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final EntitySelectCriteria criteria) throws DbException {
    final List<Entity> entities = selectMany(criteria);
    if (entities.isEmpty()) {
      throw new RecordNotFoundException(FrameworkMessages.get(FrameworkMessages.RECORD_NOT_FOUND));
    }
    if (entities.size() > 1) {
      throw new DbException(FrameworkMessages.get(FrameworkMessages.MANY_RECORDS_FOUND));
    }

    return entities.get(0);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DbException {
    if (keys == null || keys.isEmpty()) {
      return new ArrayList<Entity>(0);
    }

    return selectMany(EntityCriteriaUtil.selectCriteria(keys));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final String entityID, final String propertyID, final Object... values) throws DbException {
    return selectMany(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, values));
  }

  /** {@inheritDoc} */
  public List<Entity> selectAll(final String entityID) throws DbException {
    return selectMany(EntityCriteriaUtil.selectCriteria(entityID, Entities.getOrderByClause(entityID)));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = null;
    try {
      selectQuery = initializeSelectQuery(criteria, Entities.getSelectColumnsString(criteria.getEntityID()), criteria.getOrderByClause());
      statement = getConnection().prepareStatement(selectQuery);
      resultSet = executePreparedSelect(statement, selectQuery, criteria.getValues(), criteria.getValueProperties());
      final List<Entity> result = getEntityResultPacker(criteria.getEntityID()).pack(resultSet, criteria.getFetchCount());
      setForeignKeyValues(result, criteria);

      return result;
    }
    catch (SQLException exception) {
      final DbException dbException = new DbException(exception, selectQuery, getDatabase().getErrorMessage(exception));
      LOG.error(this, dbException);
      throw dbException;
    }
    finally {
      Util.closeSilently(statement);
      Util.closeSilently(resultSet);
    }
  }

  /** {@inheritDoc} */
  public List<Object> selectPropertyValues(final String entityID, final String propertyID, final boolean order) throws DbException {
    String sql = null;
    try {
      if (Entities.getSelectQuery(entityID) != null) {
        throw new RuntimeException("selectPropertyValues is not implemented for entities with custom select queries");
      }

      final Property.ColumnProperty property = (Property.ColumnProperty) Entities.getProperty(entityID, propertyID);
      final String columnName = property.getColumnName();
      sql = getSelectSQL(Entities.getSelectTableName(entityID),
              new StringBuilder("distinct ").append(columnName).toString(),
              new StringBuilder("where ").append(columnName).append(" is not null").toString(), order ? columnName : null);

      //noinspection unchecked
      return query(sql, getPropertyResultPacker(property), -1);
    }
    catch (SQLException exception) {
      throw new DbException(exception, sql, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public List<List> selectRows(final String statement, final int fetchCount) throws DbException {
    try {
      return queryObjects(statement, fetchCount);
    }
    catch (SQLException exception) {
      throw new DbException(exception, statement, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public int selectRowCount(final EntityCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = null;
    try {
      selectQuery = Entities.getSelectQuery(criteria.getEntityID());
      selectQuery = getSelectSQL(selectQuery == null ? Entities.getSelectTableName(criteria.getEntityID()) :
              "(" + selectQuery + " " + criteria.getWhereClause(!selectQuery.toLowerCase().contains("where")) + ") alias", "count(*)", null, null);
      selectQuery += " " + criteria.getWhereClause(!containsWhereKeyword(selectQuery));
      statement = getConnection().prepareStatement(selectQuery);
      resultSet = executePreparedSelect(statement, selectQuery, criteria.getValues(), criteria.getValueProperties());
      final List<Integer> result = INT_PACKER.pack(resultSet, -1);

      if (result.isEmpty()) {
        throw new RecordNotFoundException("Record count query returned no value");
      }
      else if (result.size() > 1) {
        throw new DbException("Record count query returned multiple values");
      }

      return result.get(0);
    }
    catch (SQLException e) {
      LOG.info(selectQuery);
      LOG.error(this, e);
      throw new DbException(e, selectQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
      Util.closeSilently(resultSet);
    }
  }

  /** {@inheritDoc} */
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DbException {
    final Map<String, Collection<Entity>> dependencyMap = new HashMap<String, Collection<Entity>>();
    if (entities == null || entities.isEmpty()) {
      return dependencyMap;
    }

    final Set<Dependency> dependencies = resolveEntityDependencies(entities.iterator().next().getEntityID());
    for (final Dependency dependency : dependencies) {
      final List<Entity> dependentEntities = selectMany(EntityCriteriaUtil.selectCriteria(dependency.getEntityID(),
              dependency.getForeignKeyProperties(), EntityUtil.getPrimaryKeys(entities)));
      if (!dependentEntities.isEmpty()) {
        dependencyMap.put(dependency.entityID, dependentEntities);
      }
    }

    return dependencyMap;
  }

  /** {@inheritDoc} */
  public void executeStatement(final String statement) throws DbException {
    try {
      execute(statement);
      if (!isTransactionOpen()) {
        commit();
      }
    }
    catch (SQLException exception) {
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      LOG.info(statement);
      LOG.error(this, exception);
      throw new DbException(exception, statement, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public Object executeStatement(final String statement, final int outParameterType) throws DbException {
    try {
      final Object result = executeCallableStatement(statement, outParameterType);
      if (!isTransactionOpen()) {
        commit();
      }

      return result;
    }
    catch (SQLException exception) {
      LOG.info(statement);
      LOG.error(this, exception);
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      throw new DbException(exception, statement, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException {
    return reportWrapper.fillReport(getConnection());
  }

  /** {@inheritDoc} */
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final String dataDescription,
                        final byte[] blobData) throws DbException {
    if (isTransactionOpen()) {
      throw new DbException("Can not save blob within an open transaction");
    }

    try {
      boolean success = false;
      try {
        beginTransaction();
        final Property.BlobProperty property =
                (Property.BlobProperty) Entities.getProperty(primaryKey.getEntityID(), blobPropertyID);

        final String whereCondition = getWhereCondition(primaryKey.getProperties());

        execute(new StringBuilder("update ").append(primaryKey.getEntityID()).append(" set ").append(property.getColumnName())
                .append(" = '").append(dataDescription).append("' where ").append(whereCondition).toString());

        writeBlobField(blobData, Entities.getTableName(primaryKey.getEntityID()),
                property.getBlobColumnName(), whereCondition);
        success = true;
      }
      finally {
        if (success) {
          commitTransaction();
        }
        else {
          rollbackTransaction();
        }
      }
    }
    catch (SQLException exception) {
      throw new DbException(exception, null, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws Exception {//todo does not work as is
    try {
      final Property.BlobProperty property =
              (Property.BlobProperty) Entities.getProperty(primaryKey.getEntityID(), blobPropertyID);

      return readBlobField(Entities.getTableName(primaryKey.getEntityID()), property.getBlobColumnName(),
              getWhereCondition(primaryKey.getProperties()));
    }
    catch (SQLException exception) {
      throw new DbException(exception, null, getDatabase().getErrorMessage(exception));
    }
  }

  private EntityResultPacker getEntityResultPacker(final String entityID) {
    EntityResultPacker packer = entityResultPackers.get(entityID);
    if (packer == null) {
      packer = new EntityResultPacker(entityID, Entities.getColumnProperties(entityID),
              Entities.getTransientProperties(entityID));
      entityResultPackers.put(entityID, packer);
    }

    return packer;
  }

  private ResultPacker getPropertyResultPacker(final Property property) {
    ResultPacker packer = propertyResultPackers.get(property.getType());
    if (packer == null) {
      packer  = new PropertyResultPacker(property);
      propertyResultPackers.put(property.getType(), packer);
    }

    return packer;
  }

  /**
   * Selects the given entity for update and checks if the it has been modified by comparing
   * the property values to the current values in the database
   * @param entity the entity to check
   * @throws DbException in case of a database exception
   * @throws RecordNotFoundException in case the entity has been deleted
   * @throws RecordModifiedException in case the entity has been modified
   */
  private void lockAndCheckForUpdate(final Entity entity) throws DbException {
    final Entity.Key originalKey = entity.getOriginalPrimaryKey();
    final Entity current = selectSingle(EntityCriteriaUtil.selectCriteria(originalKey).setSelectForUpdate(true).setFetchDepthForAll(0));
    for (final String propertyID : current.getValueKeys()) {
      if (!entity.containsValue(propertyID) || !Util.equal(current.getValue(propertyID), entity.getOriginalValue(propertyID))) {
        throw new RecordModifiedException(entity, current);
      }
    }
  }

  /**
   * @param entities the entities for which to set the reference entities
   * @param criteria the criteria
   * @throws DbException in case of a database exception
   */
  private void setForeignKeyValues(final List<Entity> entities, final EntitySelectCriteria criteria) throws DbException {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    //Any sufficiently complex algorithm is indistinguishable from evil
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entities.get(0).getEntityID());
    for (final Property.ForeignKeyProperty property : foreignKeyProperties) {
      final int maxFetchDepth = criteria.getCurrentFetchDepth() == 0 ? criteria.getFetchDepth(property.getPropertyID()) : criteria.getFetchDepth();
      if (!limitForeignKeyFetchDepth || criteria.getCurrentFetchDepth() < maxFetchDepth) {
        final List<Entity.Key> referencedKeys = getReferencedPrimaryKeys(entities, property);
        if (!referencedKeys.isEmpty()) {
          final EntitySelectCriteria referenceCriteria = EntityCriteriaUtil.selectCriteria(referencedKeys)
                  .setCurrentFetchDepth(criteria.getCurrentFetchDepth() + 1)
                  .setFetchDepth(maxFetchDepth);
          final List<Entity> referencedEntities = selectMany(referenceCriteria);
          final Map<Entity.Key, Entity> hashedReferencedEntities = EntityUtil.hashByPrimaryKey(referencedEntities);
          for (final Entity entity : entities) {
            entity.initializeValue(property, hashedReferencedEntities.get(entity.getReferencedPrimaryKey(property)));
          }
        }
      }
    }
  }

  private int queryNewIdValue(final String entityID, final IdSource idSource, final Property.PrimaryKeyProperty primaryKeyProperty) throws DbException {
    final String sql;
    switch (idSource) {
      case MAX_PLUS_ONE:
        sql = new StringBuilder("select max(").append(primaryKeyProperty.getColumnName())
                .append(") + 1 from ").append(Entities.getTableName(entityID)).toString();
        break;
      case QUERY:
        sql = Entities.getEntityIdSource(entityID);
        break;
      case SEQUENCE:
        sql = getDatabase().getSequenceSQL(Entities.getEntityIdSource(entityID));
        break;
      default:
        throw new IllegalArgumentException(idSource + " does not represent a queried ID source");
    }
    try {
      return queryInteger(sql);
    }
    catch (SQLException exception) {
      throw new DbException(exception, sql, getDatabase().getErrorMessage(exception));
    }
  }

  private void executePreparedUpdate(final PreparedStatement statement, final String sqlStatement,
                                     final List<?> values, final List<Property.ColumnProperty> properties) throws SQLException {
    SQLException exception = null;
    try {
      QUERY_COUNTER.count(sqlStatement);
      getMethodLogger().logAccess("executePreparedUpdate", new Object[] {sqlStatement, values});
      setParameterValues(statement, values, properties);
      statement.executeUpdate();
      return;
    }
    catch (SQLException e) {
      LOG.debug(sqlStatement);
      LOG.error(this, e);
      exception = e;
    }
    finally {
      final LogEntry entry = getMethodLogger().logExit("executePreparedUpdate", exception, null);
      LOG.debug(createLogMessage(sqlStatement, values, exception, entry));
    }

    throw exception;
  }

  private ResultSet executePreparedSelect(final PreparedStatement statement, final String sqlStatement,
                                          final List<?> values, final List<Property.ColumnProperty> properties) throws SQLException {
    SQLException exception = null;
    try {
      QUERY_COUNTER.count(sqlStatement);
      getMethodLogger().logAccess("executePreparedSelect", values == null ? new Object[] {sqlStatement} : new Object[] {sqlStatement, values});
      setParameterValues(statement, values, properties);
      return statement.executeQuery();
    }
    catch (SQLException e) {
      LOG.debug(sqlStatement);
      LOG.error(this, e);
      exception = e;
    }
    finally {
      final LogEntry entry = getMethodLogger().logExit("executePreparedSelect", exception, null);
      LOG.debug(createLogMessage(sqlStatement, values, exception, entry));
    }

    throw exception;
  }

  private String initializeSelectQuery(final EntitySelectCriteria criteria, final String columnsString, final String orderByClause) {
    String selectQuery = Entities.getSelectQuery(criteria.getEntityID());
    if (selectQuery == null) {
      selectQuery = getSelectSQL(Entities.getSelectTableName(criteria.getEntityID()), columnsString, null, null);
    }

    final StringBuilder queryBuilder = new StringBuilder(selectQuery);
    final String whereClause = criteria.getWhereClause(!containsWhereKeyword(selectQuery));
    if (!whereClause.isEmpty()) {
      queryBuilder.append(" ").append(whereClause);
    }
    if (orderByClause != null) {
      queryBuilder.append(" order by ").append(orderByClause);
    }
    if (criteria.isSelectForUpdate()) {
      queryBuilder.append(" for update");
      if (getDatabase().supportsNowait()) {
        queryBuilder.append(" nowait");
      }
    }

    return queryBuilder.toString();
  }

  private String createLogMessage(final String sqlStatement, final List<?> values, final SQLException exception, final LogEntry entry) {
    final StringBuilder logMessage = new StringBuilder();
    if (entry == null) {
      logMessage.append(sqlStatement).append(", ").append(Util.getCollectionContentsAsString(values, false));
    }
    else {
      logMessage.append(entry.toString(2));
    }
    if (exception != null) {
      logMessage.append(" [Exception: ").append(exception.getMessage()).append("]");
    }

    return logMessage.toString();
  }

  private void rollbackQuietly() {
    try {
      rollback();
    }
    catch (SQLException e) {/**/}
  }

  private static void setParameterValues(final PreparedStatement statement, final List<?> values,
                                         final List<Property.ColumnProperty> parameterProperties) throws SQLException {
    if (values == null || values.isEmpty() || statement.getParameterMetaData().getParameterCount() == 0) {
      return;
    }
    if (parameterProperties == null || parameterProperties.size() != values.size()) {
      throw new SQLException("Parameter properties not specified: " + (parameterProperties == null ?
              "no properties" : ("expected: " + values.size() + ", got: " + parameterProperties.size())));
    }

    int i = 0;
    for (final Object value : values) {
      setParameterValue(statement, i + 1, value, parameterProperties.get(i));
      i++;
    }
  }

  private static void setParameterValue(final PreparedStatement statement, final int parameterIndex,
                                        final Object value, final Property property) throws SQLException {
    final int columnType = translateType(property);
    final Object columnValue = translateValue(property, value);
    try {
      if (columnValue == null) {
        statement.setNull(parameterIndex, columnType);
      }
      else {
        statement.setObject(parameterIndex, columnValue, columnType);
      }
    }
    catch (SQLException e) {
      LOG.error("Unable to set parameter: " + property + ", value: " + value + ", value class: " + (value == null ? "null" : value.getClass()), e);
      throw e;
    }
  }

  private static Object translateValue(final Property property, final Object value) {
    if (property.isBoolean()) {
      if (property instanceof Property.BooleanProperty) {
        return ((Property.BooleanProperty) property).toSQLValue((Boolean) value);
      }
      else {
        return value == null ? null : ((Boolean) value ?
                Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_TRUE) :
                Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_FALSE));
      }
    }
    else if (value instanceof java.util.Date) {
      return new Date(((java.util.Date) value).getTime());
    }

    return value;
  }

  private static int translateType(final Property property) {
    if (property.isBoolean()) {
      if (property instanceof Property.BooleanProperty) {
        return ((Property.BooleanProperty) property).getColumnType();
      }
      else {
        return Types.INTEGER;
      }
    }

    return property.getType();
  }

  private static List<Entity.Key> getReferencedPrimaryKeys(final List<Entity> entities,
                                                           final Property.ForeignKeyProperty foreignKeyProperty) {
    final Set<Entity.Key> keySet = new HashSet<Entity.Key>(entities.size());
    for (final Entity entity : entities) {
      final Entity.Key key = entity.getReferencedPrimaryKey(foreignKeyProperty);
      if (key != null) {
        keySet.add(key);
      }
    }

    return new ArrayList<Entity.Key>(keySet);
  }

  /**
   * @param selectQuery the query to check
   * @return true if the query contains the WHERE keyword after the last FROM keyword instance
   */
  private static boolean containsWhereKeyword(final String selectQuery) {
    final String lowerCaseQuery = selectQuery.toLowerCase();
    int lastFromIndex = lowerCaseQuery.lastIndexOf("from");
    if (lastFromIndex < 0) {
      lastFromIndex = 0;
    }

    return selectQuery.substring(lastFromIndex, lowerCaseQuery.length()-1).contains("where");
  }

  /**
   * @param entity the Entity instance
   * @param properties the properties being updated
   * @param primaryKeyProperties the primary key properties for the given entity
   * @return a query for updating this entity instance
   * @throws DbException in case the entity is unmodified or it contains no modified updatable properties
   */
  private static String getUpdateSQL(final Entity entity, final Collection<Property.ColumnProperty> properties,
                                     final List<Property.PrimaryKeyProperty> primaryKeyProperties) throws DbException {
    final StringBuilder sql = new StringBuilder("update ");
    sql.append(Entities.getTableName(entity.getEntityID())).append(" set ");
    int columnIndex = 0;
    for (final Property.ColumnProperty property : properties) {
      sql.append(property.getColumnName()).append(" = ?");
      if (columnIndex++ < properties.size() - 1) {
        sql.append(", ");
      }
    }

    return sql.append(getWhereCondition(primaryKeyProperties)).toString();
  }

  /**
   * @param entityID the entityID
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting this entity instance
   */
  private static String getInsertSQL(final String entityID, final Collection<Property.ColumnProperty> insertProperties) {
    final StringBuilder sql = new StringBuilder("insert into ");
    sql.append(Entities.getTableName(entityID)).append("(");
    final StringBuilder columnValues = new StringBuilder(") values(");
    int columnIndex = 0;
    for (final Property.ColumnProperty property : insertProperties) {
      sql.append(property.getColumnName());
      columnValues.append("?");
      if (columnIndex++ < insertProperties.size() - 1) {
        sql.append(", ");
        columnValues.append(", ");
      }
    }

    return sql.append(columnValues).append(")").toString();
  }

  /**
   * Constructs a where condition based on the given primary key properties and the values provide by <code>valueProvider</code>
   * @param properties the properties to use when constructing the condition
   * @return a where clause according to the given properties and the values provided by <code>valueProvider</code>,
   * without the 'where' keyword
   * e.g. "(idCol = 42)" or in case of multiple properties "(idCol1 = 42) and (idCol2 = 24)"
   */
  private static String getWhereCondition(final List<Property.PrimaryKeyProperty> properties) {
    final StringBuilder stringBuilder = new StringBuilder(" where (");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : properties) {
      stringBuilder.append(getQueryString(property.getPropertyID(), "?"));
      if (i++ < properties.size() - 1) {
        stringBuilder.append(" and ");
      }
    }

    return stringBuilder.append(")").toString();
  }

  /**
   * @param columnName the columnName
   * @param sqlStringValue the sql string value
   * @return a query comparison string, e.g. "columnName = sqlStringValue"
   * or "columnName is null" in case sqlStringValue is 'null'
   */
  private static String getQueryString(final String columnName, final String sqlStringValue) {
    return new StringBuilder(columnName).append(sqlStringValue.equalsIgnoreCase("null") ?
            " is " : " = ").append(sqlStringValue).toString();
  }

  /**
   * @param criteria the EntityCriteria instance
   * @return a query for deleting the entities specified by the given criteria
   */
  private static String getDeleteSQL(final EntityCriteria criteria) {
    return new StringBuilder("delete from ").append(Entities.getTableName(criteria.getEntityID())).append(" ")
            .append(criteria.getWhereClause()).toString();
  }

  /**
   * Generates a sql select query with the given parameters
   * @param table the name of the table from which to select
   * @param columns the columns to select, example: "col1, col2"
   * @param whereCondition the where condition
   * @param orderByClause a string specifying the columns 'ORDER BY' clause,
   * "col1, col2" as input results in the following order by clause "order by col1, col2"
   * @return the generated sql query
   */
  private static String getSelectSQL(final String table, final String columns, final String whereCondition,
                                     final String orderByClause) {
    final StringBuilder sql = new StringBuilder("select ");
    sql.append(columns);
    sql.append(" from ");
    sql.append(table);
    if (!Util.nullOrEmpty(whereCondition)) {
      sql.append(" ").append(whereCondition);
    }
    if (!Util.nullOrEmpty(orderByClause)) {
      sql.append(" order by ");
      sql.append(orderByClause);
    }

    return sql.toString();
  }

  /**
   * Returns the properties used when inserting an instance of this entity, leaving out properties with null values
   * @param entity the entity
   * @param properties the properties are added to this collection
   * @param values the values are added to this collection
   */
  private static void addInsertProperties(final Entity entity, final Collection<Property.ColumnProperty> properties,
                                          final Collection<Object> values) {
    for (final Property.ColumnProperty property : Entities.getColumnProperties(entity.getEntityID(),
            Entities.getIdSource(entity.getEntityID()) != IdSource.AUTO_INCREMENT, false, true, false, false)) {
      if (!entity.isValueNull(property.getPropertyID())) {
        properties.add(property);
        values.add(entity.getValue(property));
      }
    }
  }

  /**
   * @param entity the Entity instance
   * @param properties the collection to add the properties to
   * @return the properties used to update this entity, properties that have had their values modified that is
   * @param values the values are added to this collection
   */
  private static Collection<Property.ColumnProperty> addUpdateProperties(final Entity entity,
                                                                         final Collection<Property.ColumnProperty> properties,
                                                                         final Collection<Object> values) {
    for (final Property.ColumnProperty property : Entities.getColumnProperties(entity.getEntityID(), true, false, false, false)) {
      if (entity.isModified(property.getPropertyID())) {
        properties.add(property);
        values.add(entity.getValue(property));
      }
    }

    return properties;
  }

  private static Set<Dependency> resolveEntityDependencies(final String entityID) {
    final Collection<String> entityIDs = Entities.getDefinedEntities();
    final Set<Dependency> dependencies = new HashSet<Dependency>();
    for (final String entityIDToCheck : entityIDs) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityIDToCheck)) {
        if (foreignKeyProperty.getReferencedEntityID().equals(entityID)) {
          dependencies.add(new Dependency(entityIDToCheck, foreignKeyProperty.getReferenceProperties()));
        }
      }
    }

    return dependencies;
  }

  private static final class Dependency {
    private final String entityID;
    private final List<Property.ColumnProperty> foreignKeyProperties;

    Dependency(final String entityID, final List<Property.ColumnProperty> foreignKeyProperties) {
      this.entityID = entityID;
      this.foreignKeyProperties = foreignKeyProperties;
    }

    public String getEntityID() {
      return entityID;
    }

    public List<Property.ColumnProperty> getForeignKeyProperties() {
      return foreignKeyProperties;
    }
  }

  private static final class PropertyResultPacker implements ResultPacker {
    private final Property property;

    private PropertyResultPacker(final Property property) {
      this.property = property;
    }

    /** {@inheritDoc} */
    public List pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Object> result = new ArrayList<Object>(50);
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        if (property.isInteger()) {
          result.add(resultSet.getInt(1));
        }
        else if (property.isDouble()) {
          result.add(resultSet.getDouble(1));
        }
        else {
          result.add(resultSet.getObject(1));
        }
      }
      return result;
    }
  }

  /**
   * Handles packing Entity query results.
   * Loads all database property values except for foreign key properties (Property.ForeignKeyProperty).
   */
  private static final class EntityResultPacker implements ResultPacker<Entity> {

    private final String entityID;
    private final Collection<Property.ColumnProperty> properties;
    private final Collection<Property.TransientProperty> transientProperties;

    /**
     * Instantiates a new EntityResultPacker.
     * @param entityID the ID of the entities this packer packs
     * @param properties the column properties that should be packed by this packer
     * @param transientProperties the transient properties associated with the entity ID
     */
    private EntityResultPacker(final String entityID, final Collection<Property.ColumnProperty> properties,
                       final Collection<Property.TransientProperty> transientProperties) {
      Util.rejectNullValue(entityID, "entityID");
      Util.rejectNullValue(properties, "properties");
      this.entityID = entityID;
      this.properties = properties;
      this.transientProperties = transientProperties;
    }

    /**
     * Packs the contents of <code>resultSet</code> into a List of Entity objects.
     * The resulting entities do not contain values for foreign key properties (Property.ForeignKeyProperty).
     * This method does not close the ResultSet object.
     * @param resultSet the ResultSet object
     * @param fetchCount the maximum number of records to retrieve from the result set
     * @return a List of Entity objects representing the contents of <code>resultSet</code>
     * @throws java.sql.SQLException in case of an exception
     */
    public List<Entity> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      Util.rejectNullValue(resultSet, "resultSet");
      final List<Entity> entities = new ArrayList<Entity>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        entities.add(loadEntity(resultSet));
      }

      return entities;
    }

    private Entity loadEntity(final ResultSet resultSet) throws SQLException {
      final Entity entity = Entities.entityInstance(entityID);
      if (transientProperties != null && !transientProperties.isEmpty()) {
        for (final Property.TransientProperty transientProperty : transientProperties) {
          if (!(transientProperty instanceof Property.DenormalizedViewProperty)
                  && !(transientProperty instanceof Property.DerivedProperty)) {
            entity.initializeValue(transientProperty, null);
          }
        }
      }
      for (final Property.ColumnProperty property : properties) {
        try {
          entity.initializeValue(property, getValue(resultSet, property));
        }
        catch (Exception e) {
          throw new SQLException("Unable to load property: " + property, e);
        }
      }

      return entity;
    }

    private Object getValue(final ResultSet resultSet, final Property.ColumnProperty property) throws SQLException {
      if (property.isBoolean()) {
        return getBoolean(resultSet, property);
      }
      else {
        return getValue(resultSet, property.getType(), property.getSelectIndex());
      }
    }

    private Boolean getBoolean(final ResultSet resultSet, final Property.ColumnProperty property) throws SQLException {
      if (property instanceof Property.BooleanProperty) {
        return ((Property.BooleanProperty) property).toBoolean(
                getValue(resultSet, ((Property.BooleanProperty) property).getColumnType(), property.getSelectIndex()));
      }
      else {
        final Integer result = getInteger(resultSet, property.getSelectIndex());
        if (result == null) {
          return null;
        }

        switch (result) {
          case 0: return false;
          case 1: return true;
          default: return null;
        }
      }
    }

    private Object getValue(final ResultSet resultSet, final int sqlType, final int selectIndex) throws SQLException {
      switch (sqlType) {
        case Types.INTEGER:
          return getInteger(resultSet, selectIndex);
        case Types.DOUBLE:
          return getDouble(resultSet, selectIndex);
        case Types.DATE:
          return getDate(resultSet, selectIndex);
        case Types.TIMESTAMP:
          return getTimestamp(resultSet, selectIndex);
        case Types.VARCHAR:
          return getString(resultSet, selectIndex);
        case Types.BOOLEAN:
          return getBoolean(resultSet, selectIndex);
        case Types.CHAR: {
          final String val = getString(resultSet, selectIndex);
          if (!Util.nullOrEmpty(val)) {
            return val.charAt(0);
          }
          else {
            return null;
          }
        }
      }

      throw new IllegalArgumentException("Unknown value type: " + sqlType);
    }

    private Integer getInteger(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final int value = resultSet.getInt(columnIndex);

      return resultSet.wasNull() ? null : value;
    }

    private Double getDouble(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final double value = resultSet.getDouble(columnIndex);

      return resultSet.wasNull() ? null : value;
    }

    private String getString(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final String string = resultSet.getString(columnIndex);

      return resultSet.wasNull() ? null : string;
    }

    private Boolean getBoolean(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getBoolean(columnIndex);
    }

    private java.util.Date getDate(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getDate(columnIndex);
    }

    private Timestamp getTimestamp(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getTimestamp(columnIndex);
    }
  }
}