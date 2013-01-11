/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnectionImpl;
import org.jminor.common.db.DatabaseUtil;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.model.tools.MethodLogger;
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

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
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
  private static final String CRITERIA_PARAM_NAME = "criteria";

  private final Map<String, EntityResultPacker> entityResultPackers = new HashMap<String, EntityResultPacker>();
  private final Map<Integer, ResultPacker> propertyResultPackers = new HashMap<Integer, ResultPacker>();
  private boolean optimisticLocking = Configuration.getBooleanValue(Configuration.USE_OPTIMISTIC_LOCKING);
  private boolean limitForeignKeyFetchDepth = Configuration.getBooleanValue(Configuration.LIMIT_FOREIGN_KEY_FETCH_DEPTH);

  /**
   * Constructs a new EntityConnectionImpl instance
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @throws DatabaseException in case there is a problem connecting to the database,
   * such as a wrong username or password being provided
   */
  EntityConnectionImpl(final Database database, final User user) throws DatabaseException {
    super(database, user);
  }

  /**
   * Constructs a new EntityConnectionImpl instance
   * @param database the Database instance
   * @param connection the connection object to base this entity connection on
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see org.jminor.common.db.Database#supportsIsValid()
   */
  EntityConnectionImpl(final Database database, final Connection connection) throws DatabaseException {
    super(database, connection);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException {
    if (Util.nullOrEmpty(entities)) {
      return new ArrayList<Entity.Key>();
    }
    checkReadOnly(entities);

    final List<Entity.Key> insertedKeys = new ArrayList<Entity.Key>(entities.size());
    final List<Object> statementValues = new ArrayList<Object>();
    PreparedStatement statement = null;
    String insertSQL = null;
    try {
      final List<Property.ColumnProperty> statementProperties = new ArrayList<Property.ColumnProperty>();
      for (final Entity entity : entities) {
        final String entityID = entity.getEntityID();
        final Property.PrimaryKeyProperty firstPrimaryKeyProperty = Entities.getPrimaryKeyProperties(entityID).get(0);
        final Entity.KeyGenerator keyGenerator = Entities.getKeyGenerator(entityID);
        final boolean includeReadOnly = false;
        final boolean includeNonUpdatable = true;
        final List<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID,
                !keyGenerator.isAutoIncrement(), includeReadOnly, includeNonUpdatable);
        keyGenerator.beforeInsert(entity, firstPrimaryKeyProperty, this);

        final boolean inserting = true;
        populateStatementPropertiesAndValues(inserting, entity, columnProperties, statementProperties, statementValues);

        insertSQL = createInsertSQL(entityID, statementProperties);
        statement = getConnection().prepareStatement(insertSQL);
        executePreparedUpdate(statement, insertSQL, statementValues, statementProperties);
        keyGenerator.afterInsert(entity, firstPrimaryKeyProperty, this);

        insertedKeys.add(entity.getPrimaryKey());

        statement.close();
        statementProperties.clear();
        statementValues.clear();
      }
      commitIfTransactionIsNotOpen();

      return insertedKeys;
    }
    catch (SQLException e) {
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), insertSQL, statementValues, e, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e));
    }
    finally {
      DatabaseUtil.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> update(final List<Entity> entities) throws DatabaseException {
    if (Util.nullOrEmpty(entities)) {
      return entities;
    }
    checkReadOnly(entities);

    final List<Object> statementValues = new ArrayList<Object>();
    PreparedStatement statement = null;
    String updateSQL = null;
    try {
      final Map<String, Collection<Entity>> hashedEntities = EntityUtil.hashByEntityID(entities);
      if (optimisticLocking) {
        try {
          lockAndCheckForUpdate(hashedEntities);
        }
        catch (RecordModifiedException e) {
          rollbackQuietlyIfTransactionIsNotOpen();//releasing the select for update lock
          throw e;
        }
      }

      final List<Property.ColumnProperty> statementProperties = new ArrayList<Property.ColumnProperty>();
      for (final Map.Entry<String, Collection<Entity>> hashedEntitiesMapEntry : hashedEntities.entrySet()) {
        final String entityID = hashedEntitiesMapEntry.getKey();
        final boolean includePrimaryKeyProperties = true;
        final boolean includeReadOnlyProperties = false;
        final boolean includeNonUpdatableProperties = false;
        final List<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID,
                includePrimaryKeyProperties, includeReadOnlyProperties, includeNonUpdatableProperties);

        for (final Entity entity : hashedEntitiesMapEntry.getValue()) {
          final boolean inserting = false;
          populateStatementPropertiesAndValues(inserting, entity, columnProperties, statementProperties, statementValues);

          final List<Property.PrimaryKeyProperty> primaryKeyProperties = Entities.getPrimaryKeyProperties(entityID);
          updateSQL = createUpdateSQL(entity, statementProperties, primaryKeyProperties);
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
      commitIfTransactionIsNotOpen();
    }
    catch (SQLException e) {
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), updateSQL, statementValues, e, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e));
    }
    finally {
      DatabaseUtil.closeSilently(statement);
    }

    return selectMany(EntityUtil.getPrimaryKeys(entities));
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void delete(final EntityCriteria criteria) throws DatabaseException {
    Util.rejectNullValue(criteria, CRITERIA_PARAM_NAME);
    checkReadOnly(criteria.getEntityID());

    PreparedStatement statement = null;
    String deleteSQL = null;
    try {
      deleteSQL = createDeleteSQL(criteria);
      statement = getConnection().prepareStatement(deleteSQL);
      executePreparedUpdate(statement, deleteSQL, criteria.getValues(), criteria.getValueProperties());
      commitIfTransactionIsNotOpen();
    }
    catch (SQLException e) {
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), deleteSQL, criteria.getValues(), e, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e));
    }
    finally {
      DatabaseUtil.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void delete(final List<Entity.Key> entityKeys) throws DatabaseException {
    if (Util.nullOrEmpty(entityKeys)) {
      return;
    }

    PreparedStatement statement = null;
    String deleteSQL = null;
    try {
      final Map<String, Collection<Entity.Key>> hashedKeys = EntityUtil.hashKeysByEntityID(entityKeys);
      for (final String entityID : hashedKeys.keySet()) {
        checkReadOnly(entityID);
      }
      final List<Entity.Key> criteriaKeys = new ArrayList<Entity.Key>();
      for (final Map.Entry<String, Collection<Entity.Key>> hashedKeysEntry : hashedKeys.entrySet()) {
        criteriaKeys.addAll(hashedKeysEntry.getValue());
        final EntityCriteria criteria = EntityCriteriaUtil.criteria(criteriaKeys);
        deleteSQL = "delete from " + Entities.getTableName(hashedKeysEntry.getKey()) + " " + criteria.getWhereClause();
        statement = getConnection().prepareStatement(deleteSQL);
        executePreparedUpdate(statement, deleteSQL, criteria.getValues(), criteria.getValueProperties());
        statement.close();
        criteriaKeys.clear();
      }
      commitIfTransactionIsNotOpen();
    }
    catch (SQLException e) {
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), deleteSQL, entityKeys, e, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e));
    }
    finally {
      DatabaseUtil.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DatabaseException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, value));
  }

  /** {@inheritDoc} */
  @Override
  public synchronized Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(key));
  }

  /** {@inheritDoc} */
  @Override
  public synchronized Entity selectSingle(final EntitySelectCriteria criteria) throws DatabaseException {
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
  @Override
  public synchronized List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException {
    if (Util.nullOrEmpty(keys)) {
      return new ArrayList<Entity>(0);
    }

    return selectMany(EntityCriteriaUtil.selectCriteria(keys));
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> selectMany(final String entityID, final String propertyID, final Object... values) throws DatabaseException {
    return selectMany(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, values));
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> selectAll(final String entityID) throws DatabaseException {
    return selectMany(EntityCriteriaUtil.selectCriteria(entityID, Entities.getOrderByClause(entityID)));
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> selectMany(final EntitySelectCriteria criteria) throws DatabaseException {
    try {
      final List<Entity> result = doSelectMany(criteria, 0);
      if (!isTransactionOpen() && !criteria.isForUpdate()) {
        commitQuietly();
      }

      return result;
    }
    catch (DatabaseException dbe) {
      rollbackQuietlyIfTransactionIsNotOpen();
      throw dbe;
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Object> selectPropertyValues(final String entityID, final String propertyID, final boolean order) throws DatabaseException {
    String selectSQL = null;
    try {
      if (Entities.getSelectQuery(entityID) != null) {
        throw new UnsupportedOperationException("selectPropertyValues is not implemented for entities with custom select queries");
      }

      final Property.ColumnProperty property = Entities.getColumnProperty(entityID, propertyID);
      final String columnName = property.getColumnName();
      selectSQL = createSelectSQL(Entities.getSelectTableName(entityID), "distinct " + columnName,
              "where " + columnName + " is not null", order ? columnName : null);

      //noinspection unchecked
      final List<Object> result = query(selectSQL, getPropertyResultPacker(property), -1);
      commitIfTransactionIsNotOpen();

      return result;
    }
    catch (SQLException e) {
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), selectSQL, Arrays.asList(entityID, propertyID, order), e, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e));
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized int selectRowCount(final EntityCriteria criteria) throws DatabaseException {
    Util.rejectNullValue(criteria, CRITERIA_PARAM_NAME);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectSQL = null;
    try {
      selectSQL = Entities.getSelectQuery(criteria.getEntityID());
      selectSQL = createSelectSQL(selectSQL == null ? Entities.getSelectTableName(criteria.getEntityID()) :
              "(" + selectSQL + " " + criteria.getWhereClause(!selectSQL.toLowerCase().contains("where ")) + ") alias", "count(*)", null, null);
      selectSQL += " " + criteria.getWhereClause(!containsWhereClause(selectSQL));
      statement = getConnection().prepareStatement(selectSQL);
      resultSet = executePreparedSelect(statement, selectSQL, criteria.getValues(), criteria.getValueProperties());
      final List<Integer> result = DatabaseUtil.INTEGER_RESULT_PACKER.pack(resultSet, -1);
      commitIfTransactionIsNotOpen();
      if (result.isEmpty()) {
        throw new SQLException("Record count query returned no value");
      }

      return result.get(0);
    }
    catch (SQLException e) {
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), selectSQL, criteria.getValues(), e, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e));
    }
    finally {
      DatabaseUtil.closeSilently(resultSet);
      DatabaseUtil.closeSilently(statement);
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException {
    final Map<String, Collection<Entity>> dependencyMap = new HashMap<String, Collection<Entity>>();
    if (Util.nullOrEmpty(entities)) {
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
  @Override
  public synchronized List<?> executeFunction(final String functionID, final Object... arguments) throws DatabaseException {
    DatabaseException exception = null;
    try {
      logAccess("executeFunction: " + functionID, arguments);
      if (isTransactionOpen()) {
        throw new DatabaseException("Can not execute a function within an open transaction");
      }
      final List<Object> returnArguments = Databases.getFunction(functionID).execute(this, arguments);
      if (isTransactionOpen()) {
        rollbackTransaction();
        throw new DatabaseException("Function with ID: " + functionID + " did not end its transaction, rollback was performed");
      }

      return returnArguments;
    }
    catch (DatabaseException e) {
      exception = e;
      LOG.error(DatabaseUtil.createLogMessage(getUser(), functionID, arguments == null ? null : Arrays.asList(arguments), e, null));
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executeFunction: " + functionID, exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(DatabaseUtil.createLogMessage(getUser(), "", arguments == null ? null : Arrays.asList(arguments), exception, entry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void executeProcedure(final String procedureID, final Object... arguments) throws DatabaseException {
    DatabaseException exception = null;
    try {
      logAccess("executeProcedure: " + procedureID, arguments);
      if (isTransactionOpen()) {
        throw new DatabaseException("Can not execute a procedure within an open transaction");
      }
      Databases.getProcedure(procedureID).execute(this, arguments);
      if (isTransactionOpen()) {
        rollbackTransaction();
        throw new DatabaseException("Procedure with ID: " + procedureID + " did not end its transaction, rollback was performed");
      }
    }
    catch (DatabaseException e) {
      exception = e;
      LOG.error(DatabaseUtil.createLogMessage(getUser(), procedureID, arguments == null ? null : Arrays.asList(arguments), e, null));
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executeProcedure: " + procedureID, exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(DatabaseUtil.createLogMessage(getUser(), "", arguments == null ? null : Arrays.asList(arguments), exception, entry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException {
    ReportException exception = null;
    try {
      logAccess("fillReport", new Object[]{reportWrapper.getReportName()});
      final ReportResult result = reportWrapper.fillReport(getConnection());
      if (!isTransactionOpen()) {
        commitQuietly();
      }

      return result;
    }
    catch (ReportException e) {
      exception = e;
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), null, Arrays.asList(reportWrapper.getReportName()), e, null));
      throw e;
    }
    finally {
      final MethodLogger.Entry logEntry = logExit("fillReport", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(DatabaseUtil.createLogMessage(getUser(), null, Arrays.asList(reportWrapper.getReportName()), exception, logEntry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final byte[] blobData) throws DatabaseException {
    final Property.ColumnProperty property = Entities.getColumnProperty(primaryKey.getEntityID(), blobPropertyID);
    if (property.getColumnType() != Types.BLOB) {
      throw new IllegalArgumentException("Property " + property.getPropertyID() + " in entity " +
              primaryKey.getEntityID() + " does not have column type BLOB");
    }
    SQLException exception = null;
    ByteArrayInputStream inputStream = null;
    PreparedStatement statement = null;
    final EntityCriteria criteria = EntityCriteriaUtil.criteria(primaryKey);
    final String sql = "update " + Entities.getTableName(primaryKey.getEntityID()) + " set " + property.getColumnName() +
            " = ? " + criteria.getWhereClause();
    final List<Object> values = new ArrayList<Object>();
    final List<Property.ColumnProperty> properties = new ArrayList<Property.ColumnProperty>();
    Databases.QUERY_COUNTER.count(sql);
    try {
      logAccess("writeBlob", new Object[]{sql});
      values.add(null);//the blob value, set explicitly later
      values.addAll(criteria.getValues());
      properties.add(property);
      properties.addAll(criteria.getValueProperties());

      statement = getConnection().prepareStatement(sql);
      setParameterValues(statement, values, properties);
      inputStream = new ByteArrayInputStream(blobData);
      statement.setBinaryStream(1, inputStream);
      statement.executeUpdate();
      commitIfTransactionIsNotOpen();
    }
    catch (SQLException e) {
      exception = e;
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), sql, values, exception, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e));
    }
    finally {
      Util.closeSilently(inputStream);
      DatabaseUtil.closeSilently(statement);
      final MethodLogger.Entry logEntry = logExit("writeBlob", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(DatabaseUtil.createLogMessage(getUser(), sql, values, exception, logEntry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DatabaseException {
    final Property.ColumnProperty property = Entities.getColumnProperty(primaryKey.getEntityID(), blobPropertyID);
    if (property.getColumnType() != Types.BLOB) {
      throw new IllegalArgumentException("Property " + property.getPropertyID() + " in entity " +
              primaryKey.getEntityID() + " does not have column type BLOB");
    }
    PreparedStatement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    final EntityCriteria criteria = EntityCriteriaUtil.criteria(primaryKey);
    final String sql = "select " + property.getColumnName() + " from " +
            Entities.getTableName(primaryKey.getEntityID()) + " " + criteria.getWhereClause();
    Databases.QUERY_COUNTER.count(sql);
    try {
      logAccess("readBlob", new Object[]{sql});
      statement = getConnection().prepareStatement(sql);
      setParameterValues(statement, criteria.getValues(), criteria.getValueProperties());

      resultSet = statement.executeQuery();
      final List<Blob> result = BLOB_RESULT_PACKER.pack(resultSet, 1);
      final Blob blob = result.get(0);
      final byte[] byteResult = blob.getBytes(1, (int) blob.length());
      commitIfTransactionIsNotOpen();

      return byteResult;
    }
    catch (SQLException e) {
      exception = e;
      rollbackQuietlyIfTransactionIsNotOpen();
      LOG.error(DatabaseUtil.createLogMessage(getUser(), sql, criteria.getValues(), exception, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e));
    }
    finally {
      DatabaseUtil.closeSilently(statement);
      DatabaseUtil.closeSilently(resultSet);
      final MethodLogger.Entry logEntry = logExit("readBlob", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(DatabaseUtil.createLogMessage(getUser(), sql, criteria.getValues(), exception, logEntry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseConnection getDatabaseConnection() {
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
      packer = new EntityResultPacker(entityID);
      entityResultPackers.put(entityID, packer);
    }

    return packer;
  }

  private ResultPacker getPropertyResultPacker(final Property.ColumnProperty property) {
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
   * @throws RecordModifiedException in case an entity has been modified, if an entity has been deleted,
   * the <code>modifiedRow</code> provided by the exception is null
   * @throws SQLException in case of an exception
   */
  private void lockAndCheckForUpdate(final Map<String, Collection<Entity>> entities) throws DatabaseException, SQLException {
    for (final Map.Entry<String, Collection<Entity>> entry : entities.entrySet()) {
      final List<Entity.Key> originalKeys = EntityUtil.getPrimaryKeys(entry.getValue(), true);
      final EntitySelectCriteria selectForUpdateCriteria = EntityCriteriaUtil.selectCriteria(originalKeys);
      selectForUpdateCriteria.setForeignKeyFetchDepthLimit(0);
      selectForUpdate(selectForUpdateCriteria);
      final List<Entity> currentValues = doSelectMany(selectForUpdateCriteria, 0);
      final Map<Entity.Key, Entity> hashedEntities = EntityUtil.hashByPrimaryKey(currentValues);
      for (final Entity entity : entry.getValue()) {
        final Entity current = hashedEntities.get(entity.getOriginalPrimaryKey());
        if (current == null) {
          throw new RecordModifiedException(entity, null);
        }
        for (final String propertyID : current.getValueKeys()) {
          if (!entity.containsValue(propertyID) || !Util.equal(current.getValue(propertyID), entity.getOriginalValue(propertyID))) {
            throw new RecordModifiedException(entity, current);
          }
        }
      }
    }
  }

  private List<Entity> doSelectMany(final EntitySelectCriteria criteria, final int currentForeignKeyFetchDepth) throws DatabaseException {
    Util.rejectNullValue(criteria, CRITERIA_PARAM_NAME);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectSQL = null;
    try {
      selectSQL = getSelectSQL(criteria, Entities.getSelectColumnsString(criteria.getEntityID()),
              criteria.getOrderByClause(), Entities.getGroupByClause(criteria.getEntityID()), criteria.isForUpdate());
      statement = getConnection().prepareStatement(selectSQL);
      resultSet = executePreparedSelect(statement, selectSQL, criteria.getValues(), criteria.getValueProperties());
      List<Entity> result = null;
      SQLException packingException = null;
      try {
        logAccess("packResult", new Object[0]);
        result = getEntityResultPacker(criteria.getEntityID()).pack(resultSet, criteria.getFetchCount());
      }
      catch (SQLException e) {
        packingException = e;
        throw e;
      }
      finally {
        final String message = result != null ? "row count: " + result.size() : "";
        logExit("packResult", packingException, message);
      }
      setForeignKeyValues(result, criteria, currentForeignKeyFetchDepth);

      return result;
    }
    catch (SQLException e) {
      LOG.error(DatabaseUtil.createLogMessage(getUser(), selectSQL, criteria.getValues(), e, null));
      throw new DatabaseException(e, getDatabase().getErrorMessage(e), selectSQL);
    }
    finally {
      DatabaseUtil.closeSilently(resultSet);
      DatabaseUtil.closeSilently(statement);
    }
  }

  /**
   * Performs a select for update on the entities specified by the given criteria, does not parse the result set
   * @param criteria the criteria
   * @throws SQLException in case of an exception
   */
  private void selectForUpdate(final EntitySelectCriteria criteria) throws SQLException {
    Util.rejectNullValue(criteria, CRITERIA_PARAM_NAME);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectSQL = null;
    try {
      selectSQL = getSelectSQL(criteria, "*", null, null, true);
      statement = getConnection().prepareStatement(selectSQL);
      resultSet = executePreparedSelect(statement, selectSQL, criteria.getValues(), criteria.getValueProperties());
    }
    catch (SQLException e) {
      LOG.error(DatabaseUtil.createLogMessage(getUser(), selectSQL, criteria.getValues(), e, null));
      throw e;
    }
    finally {
      DatabaseUtil.closeSilently(resultSet);
      DatabaseUtil.closeSilently(statement);
    }
  }

  /**
   * Selects the entities referenced by the given entities via foreign keys and sets those
   * as their respective foreign key values. This is done recursively for the entities referenced
   * by the foreign keys as well, until the criteria fetch depth limit is reached.
   * @param entities the entities for which to set the foreign key entity values
   * @param criteria the criteria
   * @param currentForeignKeyFetchDepth the current foreign key fetch depth
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @see #setLimitForeignKeyFetchDepth(boolean)
   * @see org.jminor.framework.db.criteria.EntitySelectCriteria#setForeignKeyFetchDepthLimit(int)
   */
  private void setForeignKeyValues(final List<Entity> entities, final EntitySelectCriteria criteria, final int currentForeignKeyFetchDepth) throws DatabaseException {
    if (Util.nullOrEmpty(entities)) {
      return;
    }
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entities.get(0).getEntityID());
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      final int criteriaFetchDepthLimit = criteria.getForeignKeyFetchDepthLimit(foreignKeyProperty.getPropertyID());
      if (!limitForeignKeyFetchDepth || currentForeignKeyFetchDepth < criteriaFetchDepthLimit) {
        final Collection<Entity.Key> referencedPrimaryKeys = getReferencedPrimaryKeys(entities, foreignKeyProperty);
        if (referencedPrimaryKeys.isEmpty()) {
          for (final Entity entity : entities) {
            entity.setValue(foreignKeyProperty, null, false);
          }
        }
        else {
          final EntitySelectCriteria referencedEntitiesCriteria = EntityCriteriaUtil.selectCriteria(referencedPrimaryKeys);
          referencedEntitiesCriteria.setForeignKeyFetchDepthLimit(criteriaFetchDepthLimit);
          final List<Entity> referencedEntities = doSelectMany(referencedEntitiesCriteria, currentForeignKeyFetchDepth + 1);
          final Map<Entity.Key, Entity> hashedReferencedEntities = EntityUtil.hashByPrimaryKey(referencedEntities);
          for (final Entity entity : entities) {
            entity.setValue(foreignKeyProperty, hashedReferencedEntities.get(entity.getReferencedPrimaryKey(foreignKeyProperty)), false);
          }
        }
      }
    }
  }

  private void executePreparedUpdate(final PreparedStatement statement, final String sqlStatement,
                                     final List<?> values, final List<Property.ColumnProperty> properties) throws SQLException {
    SQLException exception = null;
    Databases.QUERY_COUNTER.count(sqlStatement);
    try {
      logAccess("executePreparedUpdate", new Object[]{sqlStatement, values});
      setParameterValues(statement, values, properties);
      statement.executeUpdate();
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executePreparedUpdate", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(DatabaseUtil.createLogMessage(getUser(), sqlStatement, values, exception, entry));
      }
    }
  }

  private ResultSet executePreparedSelect(final PreparedStatement statement, final String sqlStatement,
                                          final List<?> values, final List<Property.ColumnProperty> properties) throws SQLException {
    SQLException exception = null;
    Databases.QUERY_COUNTER.count(sqlStatement);
    try {
      logAccess("executePreparedSelect", values == null ? new Object[]{sqlStatement} : new Object[]{sqlStatement, values});
      setParameterValues(statement, values, properties);
      return statement.executeQuery();
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executePreparedSelect", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(DatabaseUtil.createLogMessage(getUser(), sqlStatement, values, exception, entry));
      }
    }
  }

  private void commitQuietly() {
    try {
      commit();
    }
    catch (SQLException ignored) {
      LOG.error("Exception while performing a quiet commit", ignored);
    }
  }

  private void rollbackQuietly() {
    try {
      rollback();
    }
    catch (SQLException ignored) {
      LOG.error("Exception while performing a quiet rollback", ignored);
    }
  }

  private void commitIfTransactionIsNotOpen() throws SQLException {
    if (!isTransactionOpen()) {
      commit();
    }
  }

  private void rollbackQuietlyIfTransactionIsNotOpen() {
    if (!isTransactionOpen()) {
      rollbackQuietly();
    }
  }

  private static void setParameterValues(final PreparedStatement statement, final List<?> values,
                                         final List<Property.ColumnProperty> parameterProperties) throws SQLException {
    if (Util.nullOrEmpty(values) || statement.getParameterMetaData().getParameterCount() == 0) {
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
                                        final Object value, final Property.ColumnProperty property) throws SQLException {
    final Object columnValue = property.toColumnValue(value);
    try {
      if (columnValue == null) {
        statement.setNull(parameterIndex, property.getColumnType());
      }
      else {
        statement.setObject(parameterIndex, columnValue, property.getColumnType());
      }
    }
    catch (SQLException e) {
      LOG.error("Unable to set parameter: " + property + ", value: " + value + ", value class: " + (value == null ? "null" : value.getClass()), e);
      throw e;
    }
  }

  private static Collection<Entity.Key> getReferencedPrimaryKeys(final List<Entity> entities,
                                                                 final Property.ForeignKeyProperty foreignKeyProperty) {
    final Set<Entity.Key> keySet = new HashSet<Entity.Key>(entities.size());
    for (final Entity entity : entities) {
      final Entity.Key key = entity.getReferencedPrimaryKey(foreignKeyProperty);
      if (key != null) {
        keySet.add(key);
      }
    }

    return keySet;
  }

  private String getSelectSQL(final EntitySelectCriteria criteria, final String columnsString, final String orderByClause,
                              final String groupByClause, final boolean selectForUpdate) {
    String selectSQL = Entities.getSelectQuery(criteria.getEntityID());
    if (selectSQL == null) {
      final String tableName;
      if (selectForUpdate) {
        tableName = Entities.getTableName(criteria.getEntityID());
      }
      else {
        tableName = Entities.getSelectTableName(criteria.getEntityID());
      }
      selectSQL = createSelectSQL(tableName, columnsString, null, null);
    }

    final StringBuilder queryBuilder = new StringBuilder(selectSQL);
    final String whereClause = criteria.getWhereClause(!containsWhereClause(selectSQL));
    if (!whereClause.isEmpty()) {
      queryBuilder.append(" ").append(whereClause);
    }
    if (groupByClause != null) {
      queryBuilder.append(" group by ").append(groupByClause);
    }
    if (orderByClause != null) {
      queryBuilder.append(" order by ").append(orderByClause);
    }
    if (selectForUpdate) {
      queryBuilder.append(" for update");
      if (getDatabase().supportsNowait()) {
        queryBuilder.append(" nowait");
      }
    }

    return queryBuilder.toString();
  }

  /**
   * @param selectQuery the query to check
   * @return true if the query contains the WHERE clause after the last FROM keyword instance
   * todo too simplistic, wont this fail at some point?
   */
  private static boolean containsWhereClause(final String selectQuery) {
    final String lowerCaseQuery = selectQuery.toLowerCase();
    int lastFromIndex = lowerCaseQuery.lastIndexOf("from ");
    if (lastFromIndex < 0) {
      lastFromIndex = 0;
    }

    return selectQuery.substring(lastFromIndex, lowerCaseQuery.length()-1).contains("where ");
  }

  /**
   * @param entity the Entity instance
   * @param properties the properties being updated
   * @param primaryKeyProperties the primary key properties for the given entity
   * @return a query for updating this entity instance
   */
  private static String createUpdateSQL(final Entity entity, final Collection<Property.ColumnProperty> properties,
                                        final List<Property.PrimaryKeyProperty> primaryKeyProperties) {
    final StringBuilder sql = new StringBuilder("update ");
    sql.append(Entities.getTableName(entity.getEntityID())).append(" set ");
    int columnIndex = 0;
    for (final Property.ColumnProperty property : properties) {
      sql.append(property.getColumnName()).append(" = ?");
      if (columnIndex++ < properties.size() - 1) {
        sql.append(", ");
      }
    }

    return sql.append(createWhereCondition(primaryKeyProperties)).toString();
  }

  /**
   * @param entityID the entityID
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting this entity instance
   */
  private static String createInsertSQL(final String entityID, final Collection<Property.ColumnProperty> insertProperties) {
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
   * @param criteria the EntityCriteria instance
   * @return a query for deleting the entities specified by the given criteria
   */
  private static String createDeleteSQL(final EntityCriteria criteria) {
    Util.rejectNullValue(criteria, CRITERIA_PARAM_NAME);
    return "delete from " + Entities.getTableName(criteria.getEntityID()) + " " + criteria.getWhereClause();
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
   * Constructs a where condition based on the given primary key properties
   * @param properties the properties to use when constructing the condition
   * @return a where clause according to the given properties
   * e.g. " where (idCol = ?)" or in case of multiple properties " where (idCol1 = ? and idCol2 = ?)"
   */
  private static String createWhereCondition(final List<Property.PrimaryKeyProperty> properties) {
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
   * @param inserting if true then only properties with non-null values are added,
   * otherwise update is assumed and all properties with modified values are added.
   * @param entity the Entity instance
   * @param columnProperties the column properties the entity type is based on
   * @param properties afterwards this collection will contain the properties on which to base the statement
   * @param values afterwards this collection will contain the values to use in the statement
   * @throws java.sql.SQLException if no properties to populate the values for were found
   */
  private static void populateStatementPropertiesAndValues(final boolean inserting, final Entity entity,
                                                           final List<Property.ColumnProperty> columnProperties,
                                                           final Collection<Property.ColumnProperty> properties,
                                                           final Collection<Object> values) throws SQLException {
    for (final Property.ColumnProperty property : columnProperties) {
      final boolean insertingAndNonNull = inserting && !entity.isValueNull(property.getPropertyID());
      final boolean updatingAndModified = !inserting && entity.isModified(property.getPropertyID());
      if (insertingAndNonNull || updatingAndModified) {
        properties.add(property);
        values.add(entity.getValue(property));
      }
    }
    if (properties.isEmpty()) {
      if (inserting) {
        throw new SQLException("Unable to insert entity " + entity.getEntityID() + ", only null values found");
      }
      else {
        throw new SQLException("Unable to update entity " + entity.getEntityID() + ", no modified values found");
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

    private Dependency(final String entityID, final List<Property.ColumnProperty> foreignKeyProperties) {
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

  private static final class PropertyResultPacker implements ResultPacker<Object> {
    private final Property.ColumnProperty property;

    private PropertyResultPacker(final Property.ColumnProperty property) {
      this.property = property;
    }

    /** {@inheritDoc} */
    @Override
    public List<Object> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Object> result = new ArrayList<Object>(50);
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        if (property.isInteger()) {
          final int value = resultSet.getInt(1);
          result.add(resultSet.wasNull() ? null : value);
        }
        else if (property.isDouble()) {
          final double value = resultSet.getDouble(1);
          result.add(resultSet.wasNull() ? null : value);
        }
        else {
          result.add(resultSet.getObject(1));
        }
      }
      return result;
    }
  }

  /**
   * A result packer for fetching blobs from a result set containing a single blob column
   */
  private static final ResultPacker<Blob> BLOB_RESULT_PACKER = new ResultPacker<Blob>() {
    /** {@inheritDoc} */
    @Override
    public List<Blob> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Blob> blobs = new ArrayList<Blob>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        blobs.add(resultSet.getBlob(1));
      }

      return blobs;
    }
  };

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
     */
    private EntityResultPacker(final String entityID) {
      Util.rejectNullValue(entityID, "entityID");
      this.entityID = entityID;
      this.properties = Entities.getColumnProperties(entityID);
      this.transientProperties = Entities.getTransientProperties(entityID);
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
    @Override
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
      final Entity entity = Entities.entity(entityID);
      if (!Util.nullOrEmpty(transientProperties)) {
        for (final Property.TransientProperty transientProperty : transientProperties) {
          if (!(transientProperty instanceof Property.DenormalizedViewProperty)
                  && !(transientProperty instanceof Property.DerivedProperty)) {
            entity.setValue(transientProperty, null, false);
          }
        }
      }
      for (final Property.ColumnProperty property : properties) {
        try {
          entity.setValue(property, property.fetchValue(resultSet), false);
        }
        catch (Exception e) {
          throw new SQLException("Unable to fetch value for: " + property + ", entityID: " + entityID, e);
        }
      }

      return entity;
    }
  }
}
