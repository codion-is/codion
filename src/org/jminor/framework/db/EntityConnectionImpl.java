/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnectionImpl;
import org.jminor.common.db.Databases;
import org.jminor.common.db.PoolableConnection;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.exception.DatabaseException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EntityConnection implementation based on a local JDBC connection.
 * <pre>
 * Database database = new H2Database("pathToDb");
 * User user = new User("scott", "tiger");
 *
 * EntityConnection connection = new EntityConnectionImpl(database, user);
 *
 * List<Entity> entities = connection.selectAll("entityID");
 *
 * connection.disconnect();
 * </pre>
 */
final class EntityConnectionImpl extends DatabaseConnectionImpl implements EntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(EntityConnectionImpl.class);

  private final Map<String, EntityResultPacker> entityResultPackers = new HashMap<String, EntityResultPacker>();
  private final Map<Integer, ResultPacker> propertyResultPackers = new HashMap<Integer, ResultPacker>();
  private boolean optimisticLocking = Configuration.getBooleanValue(Configuration.USE_OPTIMISTIC_LOCKING);
  private boolean limitForeignKeyFetchDepth = Configuration.getBooleanValue(Configuration.LIMIT_FOREIGN_KEY_FETCH_DEPTH);

  /**
   * Constructs a new EntityConnectionImpl instance
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @throws SQLException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   */
  EntityConnectionImpl(final Database database, final User user) throws SQLException, ClassNotFoundException {
    super(database, user);
  }

  /**
   * Constructs a new EntityConnectionImpl instance
   * @param connection the connection object to base the entity db connection on
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @throws SQLException in case there is a problem connecting to the database
   */
  EntityConnectionImpl(final Connection connection, final Database database, final User user) throws SQLException {
    super(database, user, connection);
  }

  /** {@inheritDoc} */
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException {
    if (entities == null || entities.isEmpty()) {
      return new ArrayList<Entity.Key>();
    }
    checkReadOnly(entities);

    final List<Entity.Key> insertedKeys = new ArrayList<Entity.Key>(entities.size());
    PreparedStatement statement = null;
    try {
      final List<Property.ColumnProperty> statementProperties = new ArrayList<Property.ColumnProperty>();
      final List<Object> statementValues = new ArrayList<Object>();
      for (final Entity entity : entities) {
        final String entityID = entity.getEntityID();
        final Property.PrimaryKeyProperty firstPrimaryKeyProperty = Entities.getPrimaryKeyProperties(entityID).get(0);
        final IdSource idSource = Entities.getIdSource(entityID);
        final String idValueSource = Entities.getIdValueSource(entityID);
        final boolean includePrimaryKeyProperties = !idSource.isAutoIncrement();
        final boolean includeReadOnly = false;
        final boolean includeNonUpdatable = true;
        final List<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID,
                includePrimaryKeyProperties, includeReadOnly, includeNonUpdatable);

        final boolean queryPrimaryKeyValue = idSource.isQueried() && entity.isValueNull(firstPrimaryKeyProperty);
        if (queryPrimaryKeyValue) {
          final int primaryKeyValue = queryNewPrimaryKeyValue(entityID, idSource, idValueSource, firstPrimaryKeyProperty);
          entity.setValue(firstPrimaryKeyProperty, primaryKeyValue);
        }

        final boolean inserting = true;
        populateStatementPropertiesAndValues(inserting, entity, columnProperties, statementProperties, statementValues);

        final String insertSQL = getInsertSQL(entityID, statementProperties);
        statement = getConnection().prepareStatement(insertSQL);
        executePreparedUpdate(statement, insertSQL, statementValues, statementProperties);

        if (idSource.isAutoIncrement()) {
          final int primaryKeyValue = queryInteger(getDatabase().getAutoIncrementValueSQL(idValueSource));
          entity.setValue(firstPrimaryKeyProperty, primaryKeyValue);
        }

        insertedKeys.add(entity.getPrimaryKey());

        statement.close();
        statementProperties.clear();
        statementValues.clear();
      }
      if (!isTransactionOpen()) {
        commit();
      }

      return insertedKeys;
    }
    catch (SQLException e) {
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  public List<Entity> update(final List<Entity> entities) throws DatabaseException {
    if (entities == null || entities.isEmpty()) {
      return entities;
    }
    checkReadOnly(entities);

    PreparedStatement statement = null;
    try {
      final Map<String, Collection<Entity>> hashedEntities = EntityUtil.hashByEntityID(entities);
      if (optimisticLocking) {
        lockAndCheckForUpdate(hashedEntities);
      }

      final List<Object> statementValues = new ArrayList<Object>();
      final List<Property.ColumnProperty> statementProperties = new ArrayList<Property.ColumnProperty>();
      for (final Map.Entry<String, Collection<Entity>> hashedEntitiesMapEntry : hashedEntities.entrySet()) {
        final String entityID = hashedEntitiesMapEntry.getKey();
        final boolean includePrimaryKeyProperties = true;
        final boolean includeReadOnlyProperties = false;
        final boolean includeNonUpdatable = false;
        final List<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID,
                includePrimaryKeyProperties, includeReadOnlyProperties, includeNonUpdatable);

        for (final Entity entity : hashedEntitiesMapEntry.getValue()) {
          final boolean updating = true;
          populateStatementPropertiesAndValues(updating, entity, columnProperties, statementProperties, statementValues);

          final List<Property.PrimaryKeyProperty> primaryKeyProperties = Entities.getPrimaryKeyProperties(entityID);
          final String updateSQL = getUpdateSQL(entity, statementProperties, primaryKeyProperties);
          statementProperties.addAll(primaryKeyProperties);
          for (final Property.PrimaryKeyProperty primaryKeyProperty : primaryKeyProperties) {
            statementValues.add(entity.getOriginalValue(primaryKeyProperty.getPropertyID()));
          }
          statement = getConnection().prepareStatement(updateSQL);
          executePreparedUpdate(statement, updateSQL, statementValues, statementProperties);

          statement.close();
          statementProperties.clear();
          statementValues.clear();
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
      throw e;
    }
    catch (SQLException e) {
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  public void delete(final EntityCriteria criteria) throws DatabaseException {
    checkReadOnly(criteria.getEntityID());
    PreparedStatement statement = null;
    try {
      final String deleteQuery = getDeleteSQL(criteria);
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
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  public void delete(final List<Entity.Key> entityKeys) throws DatabaseException {
    if (entityKeys == null || entityKeys.isEmpty()) {
      return;
    }

    PreparedStatement statement = null;
    try {
      final Map<String, Collection<Entity.Key>> hashedKeys = EntityUtil.hashKeysByEntityID(entityKeys);
      for (final String entityID : hashedKeys.keySet()) {
        checkReadOnly(entityID);
      }
      final ArrayList<Entity.Key> criteriaKeys = new ArrayList<Entity.Key>();
      for (final Map.Entry<String, Collection<Entity.Key>> hashedKeysEntry : hashedKeys.entrySet()) {
        criteriaKeys.addAll(hashedKeysEntry.getValue());
        final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(criteriaKeys);
        final String deleteQuery = "delete from " + Entities.getTableName(hashedKeysEntry.getKey()) + " " + criteria.getWhereClause();
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
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DatabaseException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, value));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(key));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final EntitySelectCriteria criteria) throws DatabaseException {
    final List<Entity> entities = selectMany(criteria);
    if (entities.isEmpty()) {
      throw new RecordNotFoundException(FrameworkMessages.get(FrameworkMessages.RECORD_NOT_FOUND));
    }
    if (entities.size() > 1) {
      throw new DatabaseException(FrameworkMessages.get(FrameworkMessages.MANY_RECORDS_FOUND));
    }

    return entities.get(0);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException {
    if (keys == null || keys.isEmpty()) {
      return new ArrayList<Entity>(0);
    }

    return selectMany(EntityCriteriaUtil.selectCriteria(keys));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final String entityID, final String propertyID, final Object... values) throws DatabaseException {
    return selectMany(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, values));
  }

  /** {@inheritDoc} */
  public List<Entity> selectAll(final String entityID) throws DatabaseException {
    return selectMany(EntityCriteriaUtil.selectCriteria(entityID, Entities.getOrderByClause(entityID)));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DatabaseException {
    return doSelectMany(criteria, 0);
  }

  /** {@inheritDoc} */
  public List<Object> selectPropertyValues(final String entityID, final String propertyID, final boolean order) throws DatabaseException {
    try {
      if (Entities.getSelectQuery(entityID) != null) {
        throw new UnsupportedOperationException("selectPropertyValues is not implemented for entities with custom select queries");
      }

      final Property.ColumnProperty property = (Property.ColumnProperty) Entities.getProperty(entityID, propertyID);
      final String columnName = property.getColumnName();
      final String sql = createSelectSQL(Entities.getSelectTableName(entityID),
              new StringBuilder("distinct ").append(columnName).toString(),
              new StringBuilder("where ").append(columnName).append(" is not null").toString(), order ? columnName : null);

      //noinspection unchecked
      return query(sql, getPropertyResultPacker(property), -1);
    }
    catch (SQLException e) {
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
  }

  /** {@inheritDoc} */
  public int selectRowCount(final EntityCriteria criteria) throws DatabaseException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      String selectQuery = Entities.getSelectQuery(criteria.getEntityID());
      selectQuery = createSelectSQL(selectQuery == null ? Entities.getSelectTableName(criteria.getEntityID()) :
              "(" + selectQuery + " " + criteria.getWhereClause(!selectQuery.toLowerCase().contains("where")) + ") alias", "count(*)", null, null);
      selectQuery += " " + criteria.getWhereClause(!containsWhereKeyword(selectQuery));
      statement = getConnection().prepareStatement(selectQuery);
      resultSet = executePreparedSelect(statement, selectQuery, criteria.getValues(), criteria.getValueProperties());
      final List<Integer> result = Databases.INT_PACKER.pack(resultSet, -1);

      if (result.isEmpty()) {
        throw new RecordNotFoundException("Record count query returned no value");
      }

      return result.get(0);
    }
    catch (SQLException e) {
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(statement);
      Util.closeSilently(resultSet);
    }
  }

  /** {@inheritDoc} */
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException {
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
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException {
    return reportWrapper.fillReport(getConnection());
  }

  /** {@inheritDoc} */
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final String dataDescription,
                        final byte[] blobData) throws DatabaseException {
    if (isTransactionOpen()) {
      throw new DatabaseException("Can not save blob within an open transaction");
    }

    String statement = null;
    try {
      boolean success = false;
      try {
        beginTransaction();
        final Property.BlobProperty property =
                (Property.BlobProperty) Entities.getProperty(primaryKey.getEntityID(), blobPropertyID);

        final String whereCondition = getWhereCondition(primaryKey.getProperties());

        statement = new StringBuilder("update ").append(primaryKey.getEntityID()).append(" set ").append(property.getColumnName())
                .append(" = '").append(dataDescription).append("' where ").append(whereCondition).toString();

        execute(statement);

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
    catch (SQLException e) {
      if (getMethodLogger().isEnabled()) {
        LOG.debug(createLogMessage(getUser(), statement, null, e, null));
      }
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
  }

  /** {@inheritDoc} */
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DatabaseException {//todo does not work as is
    try {
      final Property.BlobProperty property =
              (Property.BlobProperty) Entities.getProperty(primaryKey.getEntityID(), blobPropertyID);

      return readBlobField(Entities.getTableName(primaryKey.getEntityID()), property.getBlobColumnName(),
              getWhereCondition(primaryKey.getProperties()));
    }
    catch (SQLException e) {
      if (getMethodLogger().isEnabled()) {
        LOG.debug(createLogMessage(getUser(), null, null, e, null));
      }
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
  }

  /** {@inheritDoc} */
  public PoolableConnection getPoolableConnection() {
    return this;
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
   * @return true if foreign key fetch depths are being limited
   */
  public boolean isLimitForeignKeyFetchDepth() {
    return limitForeignKeyFetchDepth;
  }

  /**
   * @param limitForeignKeyFetchDepth false to override the fetch depth limit provided by criteria
   * @see org.jminor.framework.db.criteria.EntitySelectCriteria#setForeignKeyFetchDepthLimit(int)
   */
  public void setLimitForeignKeyFetchDepth(final boolean limitForeignKeyFetchDepth) {
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
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
   * Selects the given entities for update and checks if they have been modified by comparing
   * the property values to the current values in the database.
   * The calling method is responsible for releasing the select for update lock.
   * @param entities the entities to check, hashed by entityID
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws RecordNotFoundException in case a entity has been deleted
   * @throws RecordModifiedException in case a entity has been modified
   */
  private void lockAndCheckForUpdate(final Map<String, Collection<Entity>> entities) throws DatabaseException {
    for (final Map.Entry<String, Collection<Entity>> entry : entities.entrySet()) {
      final List<Entity.Key> originalKeys = EntityUtil.getPrimaryKeys(entry.getValue(), true);
      final EntitySelectCriteria selectForUpdateCriteria = EntityCriteriaUtil.selectCriteria(originalKeys);
      selectForUpdateCriteria.setSelectForUpdate(true).setForeignKeyFetchDepthLimit(0);
      final List<Entity> currentValues = doSelectMany(selectForUpdateCriteria, 0);
      final Map<Entity.Key, Entity> hashedEntites = EntityUtil.hashByPrimaryKey(currentValues);
      for (final Entity entity : entry.getValue()) {
        final Entity current = hashedEntites.get(entity.getOriginalPrimaryKey());
        for (final String propertyID : current.getValueKeys()) {
          if (!entity.containsValue(propertyID) || !Util.equal(current.getValue(propertyID), entity.getOriginalValue(propertyID))) {
            throw new RecordModifiedException(entity, current);
          }
        }
      }
    }
  }

  private List<Entity> doSelectMany(final EntitySelectCriteria criteria, final int currentForeignKeyFetchDepth) throws DatabaseException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectSQL = null;
    try {
      selectSQL = getSelectSQL(criteria, Entities.getSelectColumnsString(criteria.getEntityID()), criteria.getOrderByClause());
      statement = getConnection().prepareStatement(selectSQL);
      resultSet = executePreparedSelect(statement, selectSQL, criteria.getValues(), criteria.getValueProperties());
      List<Entity> result = null;
      SQLException packingException = null;
      try {
        getMethodLogger().logAccess("packResult", new Object[0]);
        result = getEntityResultPacker(criteria.getEntityID()).pack(resultSet, criteria.getFetchCount());
      }
      catch (SQLException e) {
        packingException = e;
        throw e;
      }
      finally {
        final String message = result != null ? "row count: " + result.size() : "";
        getMethodLogger().logExit("packResult", packingException, null, message);
      }
      setForeignKeyValues(result, criteria, currentForeignKeyFetchDepth);

      return result;
    }
    catch (SQLException e) {
      throw new DatabaseException(getDatabase().getErrorMessage(e), selectSQL);
    }
    finally {
      Util.closeSilently(statement);
      Util.closeSilently(resultSet);
    }
  }

  /**
   * Selects the entities referenced by the given entities via foreign keys and sets those
   * as their respective foreign key values. This is done recursively for the entities referenced
   * by the foreign keys as well, until the qriteria fetch depth limit is reached.
   * @param entities the entities for which to set the foreign key entity values
   * @param criteria the criteria
   * @param currentForeignKeyFetchDepth the current foreign key fetch depth
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @see #setLimitForeignKeyFetchDepth(boolean)
   * @see org.jminor.framework.db.criteria.EntitySelectCriteria#setForeignKeyFetchDepthLimit(int)
   */
  private void setForeignKeyValues(final List<Entity> entities, final EntitySelectCriteria criteria, final int currentForeignKeyFetchDepth) throws DatabaseException {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entities.get(0).getEntityID());
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      final int criteriaFetchDepthLimit = criteria.getForeignKeyFetchDepthLimit(foreignKeyProperty.getPropertyID());
      if (!limitForeignKeyFetchDepth || currentForeignKeyFetchDepth < criteriaFetchDepthLimit) {
        final List<Entity.Key> referencedPrimaryKeys = getReferencedPrimaryKeys(entities, foreignKeyProperty);
        if (!referencedPrimaryKeys.isEmpty()) {
          final EntitySelectCriteria referencedEntitiesCriteria = EntityCriteriaUtil.selectCriteria(referencedPrimaryKeys);
          referencedEntitiesCriteria.setForeignKeyFetchDepthLimit(criteriaFetchDepthLimit);
          final List<Entity> referencedEntities = doSelectMany(referencedEntitiesCriteria, currentForeignKeyFetchDepth + 1);
          final Map<Entity.Key, Entity> hashedReferencedEntities = EntityUtil.hashByPrimaryKey(referencedEntities);
          for (final Entity entity : entities) {
            entity.initializeValue(foreignKeyProperty, hashedReferencedEntities.get(entity.getReferencedPrimaryKey(foreignKeyProperty)));
          }
        }
      }
    }
  }

  private int queryNewPrimaryKeyValue(final String entityID, final IdSource idSource, final String idValueSource,
                                      final Property.PrimaryKeyProperty primaryKeyProperty) throws DatabaseException {
    final String sql;
    switch (idSource) {
      case MAX_PLUS_ONE:
        sql = new StringBuilder("select max(").append(primaryKeyProperty.getColumnName())
                .append(") + 1 from ").append(Entities.getTableName(entityID)).toString();
        break;
      case QUERY:
        sql = idValueSource;
        break;
      case SEQUENCE:
        sql = getDatabase().getSequenceSQL(idValueSource);
        break;
      default:
        throw new IllegalArgumentException(idSource + " does not represent a queried ID source");
    }
    try {
      return queryInteger(sql);
    }
    catch (SQLException e) {
      throw new DatabaseException(getDatabase().getErrorMessage(e));
    }
  }

  private void executePreparedUpdate(final PreparedStatement statement, final String sqlStatement,
                                     final List<?> values, final List<Property.ColumnProperty> properties) throws SQLException {
    SQLException exception = null;
    try {
      Databases.QUERY_COUNTER.count(sqlStatement);
      getMethodLogger().logAccess("executePreparedUpdate", new Object[] {sqlStatement, values});
      setParameterValues(statement, values, properties);
      statement.executeUpdate();
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      final LogEntry entry = getMethodLogger().logExit("executePreparedUpdate", exception, null);
      if (getMethodLogger().isEnabled()) {
        LOG.debug(createLogMessage(getUser(), sqlStatement, values, exception, entry));
      }
    }
  }

  private ResultSet executePreparedSelect(final PreparedStatement statement, final String sqlStatement,
                                          final List<?> values, final List<Property.ColumnProperty> properties) throws SQLException {
    SQLException exception = null;
    try {
      Databases.QUERY_COUNTER.count(sqlStatement);
      getMethodLogger().logAccess("executePreparedSelect", values == null ? new Object[] {sqlStatement} : new Object[] {sqlStatement, values});
      setParameterValues(statement, values, properties);
      return statement.executeQuery();
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      final LogEntry entry = getMethodLogger().logExit("executePreparedSelect", exception, null);
      if (getMethodLogger().isEnabled()) {
        LOG.debug(createLogMessage(getUser(), sqlStatement, values, exception, entry));
      }
    }
  }

  private void rollbackQuietly() {
    try {
      rollback();
    }
    catch (SQLException e) {/**/}
  }

  private static String createLogMessage(final User user, final String sqlStatement, final List<?> values, final SQLException exception, final LogEntry entry) {
    final StringBuilder logMessage = new StringBuilder(user.toString()).append("\n");
    if (entry == null) {
      logMessage.append(sqlStatement).append(", ").append(Util.getCollectionContentsAsString(values, false));
    }
    else {
      logMessage.append(entry.toString(1));
    }
    if (exception != null) {
      logMessage.append("\n").append(" [Exception: ").append(exception.getMessage()).append("]");
    }

    return logMessage.toString();
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
    for (final Property.ColumnProperty property : parameterProperties) {
      setParameterValue(statement, i + 1, values.get(i++), property);
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
    else if (property.isDate() && value instanceof java.util.Date) {
      return new java.sql.Date(((java.util.Date) value).getTime());
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

  private String getSelectSQL(final EntitySelectCriteria criteria, final String columnsString, final String orderByClause) {
    String selectSQL = Entities.getSelectQuery(criteria.getEntityID());
    if (selectSQL == null) {
      selectSQL = createSelectSQL(Entities.getSelectTableName(criteria.getEntityID()), columnsString, null, null);
    }

    final StringBuilder queryBuilder = new StringBuilder(selectSQL);
    final String whereClause = criteria.getWhereClause(!containsWhereKeyword(selectSQL));
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

  /**
   * @param selectQuery the query to check
   * @return true if the query contains the WHERE keyword after the last FROM keyword instance
   * todo too simplistic, wont this fail at some point?
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
   * @throws org.jminor.common.db.exception.DatabaseException in case the entity is unmodified or it contains no modified updatable properties
   */
  private static String getUpdateSQL(final Entity entity, final Collection<Property.ColumnProperty> properties,
                                     final List<Property.PrimaryKeyProperty> primaryKeyProperties) throws DatabaseException {
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
   * @return a where clause according to the given properties without the 'where' keyword
   * e.g. "(idCol = ?)" or in case of multiple properties "(idCol1 = ? and idCol2 = ?)"
   */
  private static String getWhereCondition(final List<Property.PrimaryKeyProperty> properties) {
    final StringBuilder stringBuilder = new StringBuilder(" where (");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : properties) {
      stringBuilder.append(property.getColumnName()).append(" = ?");
      if (i++ < properties.size() - 1) {
        stringBuilder.append(" and ");
      }
    }

    return stringBuilder.append(")").toString();
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
  private static String createSelectSQL(final String table, final String columns, final String whereCondition,
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
   * @param inserting if true then non-null properties are added, if false then update is assumed and all
   * modified properties are added.
   * @param entity the Entity instance
   * @param columnProperties the column properties the entity type is based on
   * @param properties the collection to add the properties to
   * @param values the values are added to this collection
   */
  private static void populateStatementPropertiesAndValues(final boolean inserting, final Entity entity,
                                                           final List<Property.ColumnProperty> columnProperties,
                                                           final Collection<Property.ColumnProperty> properties,
                                                           final Collection<Object> values) {
    for (final Property.ColumnProperty property : columnProperties) {
      final boolean insertingAndNonNull = inserting && !entity.isValueNull(property.getPropertyID());
      final boolean updatingAndModified = !inserting && entity.isModified(property.getPropertyID());
      if (insertingAndNonNull || updatingAndModified) {
        properties.add(property);
        values.add(entity.getValue(property));
      }
    }
  }

  private static void checkReadOnly(final Collection<Entity> entities) throws DatabaseException {
    for (final Entity entity : entities) {
      checkReadOnly(entity.getEntityID());
    }
  }

  private static void checkReadOnly(final String entityID) throws DatabaseException {
    if (Entities.isReadOnly(entityID)) {
      throw new DatabaseException("Entities of type: " + entityID + " are read only");
    }
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

    private static Object getValue(final ResultSet resultSet, final Property.ColumnProperty property) throws SQLException {
      if (property.isBoolean()) {
        return getBoolean(resultSet, property);
      }
      else {
        return getValue(resultSet, property.getType(), property.getSelectIndex());
      }
    }

    private static Boolean getBoolean(final ResultSet resultSet, final Property.ColumnProperty property) throws SQLException {
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

    private static Object getValue(final ResultSet resultSet, final int sqlType, final int selectIndex) throws SQLException {
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

    private static Integer getInteger(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final int value = resultSet.getInt(columnIndex);

      return resultSet.wasNull() ? null : value;
    }

    private static Double getDouble(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final double value = resultSet.getDouble(columnIndex);

      return resultSet.wasNull() ? null : value;
    }

    private static String getString(final ResultSet resultSet, final int columnIndex) throws SQLException {
      final String string = resultSet.getString(columnIndex);

      return resultSet.wasNull() ? null : string;
    }

    private static Boolean getBoolean(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getBoolean(columnIndex);
    }

    private static java.util.Date getDate(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getDate(columnIndex);
    }

    private static Timestamp getTimestamp(final ResultSet resultSet, final int columnIndex) throws SQLException {
      return resultSet.getTimestamp(columnIndex);
    }
  }
}
