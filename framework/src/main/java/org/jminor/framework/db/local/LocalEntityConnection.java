/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnections;
import org.jminor.common.db.DatabaseUtil;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.framework.db.EntityConnection;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * EntityConnection implementation based on a local JDBC connection.
 * <pre>
 * Database database = new H2Database("pathToDb");
 * User user = new User("scott", "tiger");
 *
 * EntityConnection connection = new LocalEntityConnection(database, user, true, true, 2);
 *
 * List<Entity> entities = connection.selectMany(EntityCriteriaUtil.selectCriteria("entityID"));
 *
 * connection.disconnect();
 * </pre>
 */
final class LocalEntityConnection implements EntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnection.class);
  private static final String CRITERIA_PARAM_NAME = "criteria";
  private static final String WHERE = "where ";
  private static final String WHERE_SPACE_PREFIX = " where ";

  /**
   * A result packer for fetching blobs from a result set containing a single blob column
   */
  private static final ResultPacker<Blob> BLOB_RESULT_PACKER = new BlobPacker();

  private final DatabaseConnection connection;

  private boolean optimisticLocking;
  private boolean limitForeignKeyFetchDepth;

  private MethodLogger methodLogger;

  /**
   * Constructs a new LocalEntityConnection instance
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @param optimisticLocking if true then optimistic locking is used during updates
   * @param limitForeignKeyFetchDepth if false then there is no limiting of foreign key fetch depth
   * @param validityCheckTimeout specifies the timeout in seconds when validating this connection
   * @throws DatabaseException in case there is a problem connecting to the database,
   * such as a wrong username or password being provided
   */
  LocalEntityConnection(final Database database, final User user, final boolean optimisticLocking,
                        final boolean limitForeignKeyFetchDepth, final int validityCheckTimeout) throws DatabaseException {
    this.connection = DatabaseConnections.createConnection(database, user, validityCheckTimeout);
    this.optimisticLocking = optimisticLocking;
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param database the Database instance
   * @param connection the Connection object to base this EntityConnection on, it is assumed to be in a valid state
   * @param optimisticLocking if true then optimistic locking is used during updates
   * @param limitForeignKeyFetchDepth if false then there is no limiting of foreign key fetch depth
   * @param validityCheckTimeout specifies the timeout in seconds when validating this connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see org.jminor.common.db.Database#supportsIsValid()
   */
  LocalEntityConnection(final Database database, final Connection connection, final boolean optimisticLocking,
                        final boolean limitForeignKeyFetchDepth, final int validityCheckTimeout) throws DatabaseException {
    this.connection = DatabaseConnections.createConnection(database, connection, validityCheckTimeout);
    this.optimisticLocking = optimisticLocking;
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
  }

  /** {@inheritDoc} */
  @Override
  public void setMethodLogger(final MethodLogger methodLogger) {
    synchronized (connection) {
      this.methodLogger = methodLogger;
      connection.setMethodLogger(methodLogger);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Type getType() {
    return Type.LOCAL;
  }

  /** {@inheritDoc} */
  @Override
  public User getUser() {
    return connection.getUser();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConnected() {
    synchronized (connection) {
      return connection.isConnected();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    synchronized (connection) {
      connection.disconnect();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void beginTransaction() {
    synchronized (connection) {
      connection.beginTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTransactionOpen() {
    synchronized (connection) {
      return connection.isTransactionOpen();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void rollbackTransaction() {
    synchronized (connection) {
      connection.rollbackTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void commitTransaction() {
    synchronized (connection) {
      connection.commitTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException {
    if (Util.nullOrEmpty(entities)) {
      return new ArrayList<>();
    }
    checkReadOnly(entities);

    final List<Entity.Key> insertedKeys = new ArrayList<>(entities.size());
    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String insertSQL = null;
    synchronized (connection) {
      try {
        final List<Property.ColumnProperty> statementProperties = new ArrayList<>();
        for (final Entity entity : entities) {
          final String entityID = entity.getEntityID();
          final Property.ColumnProperty firstPrimaryKeyProperty = Entities.getPrimaryKeyProperties(entityID).get(0);
          final Entity.KeyGenerator keyGenerator = Entities.getKeyGenerator(entityID);
          final List<Property.ColumnProperty> insertColumnProperties = Entities.getColumnProperties(entityID,
                  !keyGenerator.getType().isAutoIncrement(), false, true);
          keyGenerator.beforeInsert(entity, firstPrimaryKeyProperty, connection);

          populateStatementPropertiesAndValues(true, entity, insertColumnProperties, statementProperties, statementValues);

          insertSQL = createInsertSQL(entityID, statementProperties);
          statement = prepareStatement(insertSQL);
          executePreparedUpdate(statement, insertSQL, statementProperties, statementValues);
          keyGenerator.afterInsert(entity, firstPrimaryKeyProperty, connection);

          insertedKeys.add(entity.getKey());

          statement.close();
          statementProperties.clear();
          statementValues.clear();
        }
        commitIfTransactionIsNotOpen();

        return insertedKeys;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), insertSQL, statementValues, e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        DatabaseUtil.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> update(final List<Entity> entities) throws DatabaseException {
    if (Util.nullOrEmpty(entities)) {
      return entities;
    }
    checkReadOnly(entities);

    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String updateSQL = null;
    synchronized (connection) {
      try {
        final Map<String, Collection<Entity>> mappedEntities = EntityUtil.mapToEntityID(entities);
        performOptimisticLocking(mappedEntities);

        final List<Property.ColumnProperty> statementProperties = new ArrayList<>();
        for (final Map.Entry<String, Collection<Entity>> mappedEntitiesMapEntry : mappedEntities.entrySet()) {
          final String entityID = mappedEntitiesMapEntry.getKey();
          final String tableName = Entities.getTableName(entityID);
          final List<Property.ColumnProperty> updateColumnProperties =
                  Entities.getColumnProperties(entityID, true, false, false);

          for (final Entity entity : mappedEntitiesMapEntry.getValue()) {
            populateStatementPropertiesAndValues(false, entity, updateColumnProperties, statementProperties, statementValues);

            final EntityCriteria criteria = EntityCriteriaUtil.criteria(entity.getOriginalKey());
            updateSQL = createUpdateSQL(tableName, statementProperties, criteria);
            statementProperties.addAll(criteria.getValueKeys());
            statementValues.addAll(criteria.getValues());
            statement = prepareStatement(updateSQL);
            executePreparedUpdate(statement, updateSQL, statementProperties, statementValues);

            statement.close();
            statementProperties.clear();
            statementValues.clear();
          }
        }
        commitIfTransactionIsNotOpen();

        return selectMany(EntityUtil.getKeys(entities));
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), updateSQL, statementValues, e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        DatabaseUtil.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCriteria criteria) throws DatabaseException {
    Objects.requireNonNull(criteria, CRITERIA_PARAM_NAME);
    checkReadOnly(criteria.getEntityID());

    PreparedStatement statement = null;
    String deleteSQL = null;
    synchronized (connection) {
      try {
        deleteSQL = createDeleteSQL(criteria);
        statement = prepareStatement(deleteSQL);
        executePreparedUpdate(statement, deleteSQL, criteria.getValueKeys(), criteria.getValues());
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), deleteSQL, criteria.getValues(), e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        DatabaseUtil.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final List<Entity.Key> entityKeys) throws DatabaseException {
    if (Util.nullOrEmpty(entityKeys)) {
      return;
    }

    PreparedStatement statement = null;
    String deleteSQL = null;
    synchronized (connection) {
      try {
        final Map<String, Collection<Entity.Key>> mappedKeys = EntityUtil.mapKeysToEntityID(entityKeys);
        for (final String entityID : mappedKeys.keySet()) {
          checkReadOnly(entityID);
        }
        final List<Entity.Key> criteriaKeys = new ArrayList<>();
        for (final Map.Entry<String, Collection<Entity.Key>> mappedKeysEntry : mappedKeys.entrySet()) {
          criteriaKeys.addAll(mappedKeysEntry.getValue());
          final EntityCriteria criteria = EntityCriteriaUtil.criteria(criteriaKeys);
          deleteSQL = createDeleteSQL(criteria);
          statement = prepareStatement(deleteSQL);
          executePreparedUpdate(statement, deleteSQL, criteria.getValueKeys(), criteria.getValues());
          statement.close();
          criteriaKeys.clear();
        }
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), deleteSQL, entityKeys, e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        DatabaseUtil.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DatabaseException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, value));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(key));
  }

  /** {@inheritDoc} */
  @Override
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
  @Override
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException {
    if (Util.nullOrEmpty(keys)) {
      return new ArrayList<>(0);
    }

    return selectMany(EntityCriteriaUtil.selectCriteria(keys));
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final String entityID, final String propertyID, final Object... values) throws DatabaseException {
    return selectMany(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, Arrays.asList(values)));
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DatabaseException {
    synchronized (connection) {
      try {
        final List<Entity> result = doSelectMany(criteria, 0);
        if (!isTransactionOpen() && !criteria.isForUpdate()) {
          commitQuietly();
        }

        return result;
      }
      catch (final DatabaseException dbe) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw dbe;
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Object> selectValues(final String propertyID, final EntityCriteria criteria) throws DatabaseException {
    Objects.requireNonNull(criteria, CRITERIA_PARAM_NAME);
    if (Entities.getSelectQuery(criteria.getEntityID()) != null) {
      throw new UnsupportedOperationException("selectValues is not implemented for entities with custom select queries");
    }
    final Property.ColumnProperty property = Entities.getColumnProperty(criteria.getEntityID(), propertyID);
    final String columnName = property.getColumnName();
    final EntityCriteria entityCriteria = EntityCriteriaUtil.criteria(criteria.getEntityID(),
            CriteriaUtil.criteriaSet(Conjunction.AND, criteria.getCriteria(), CriteriaUtil.stringCriteria(columnName + " is not null")));
    final String selectSQL = createSelectSQL(Entities.getSelectTableName(criteria.getEntityID()), "distinct " + columnName,
            WHERE + entityCriteria.getWhereClause(), columnName);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    DatabaseUtil.QUERY_COUNTER.count(selectSQL);
    synchronized (connection) {
      try {
        statement = prepareStatement(selectSQL);
        resultSet = executePreparedSelect(statement, selectSQL, criteria);
        final List<Object> result = property.getResultPacker().pack(resultSet, -1);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), selectSQL, Arrays.asList(propertyID, criteria), e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        DatabaseUtil.closeSilently(resultSet);
        DatabaseUtil.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public int selectRowCount(final EntityCriteria criteria) throws DatabaseException {
    Objects.requireNonNull(criteria, CRITERIA_PARAM_NAME);
    final String selectSQL;
    final String entitySelectQuery = Entities.getSelectQuery(criteria.getEntityID());
    if (entitySelectQuery == null) {
      final String whereClause = criteria.getWhereClause();
      selectSQL = createSelectSQL(Entities.getSelectTableName(criteria.getEntityID()), "count(*)",
              whereClause.length() == 0 ? "" : WHERE + whereClause, null);
    }
    else {
      final boolean containsWhereClause = Entities.selectQueryContainsWhereClause(criteria.getEntityID());
      final String whereClause = criteria.getWhereClause();
      String tableClause = "(" + entitySelectQuery;
      if (whereClause.length() > 0) {
        tableClause += containsWhereClause ? " and " + whereClause : WHERE_SPACE_PREFIX + whereClause;
      }
      tableClause += ") alias";
      selectSQL = createSelectSQL(tableClause, "count(*)", null, null);
    }
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    DatabaseUtil.QUERY_COUNTER.count(selectSQL);
    synchronized (connection) {
      try {
        statement = prepareStatement(selectSQL);
        resultSet = executePreparedSelect(statement, selectSQL, criteria);
        final List<Integer> result = DatabaseUtil.INTEGER_RESULT_PACKER.pack(resultSet, -1);
        commitIfTransactionIsNotOpen();
        if (result.isEmpty()) {
          throw new SQLException("Record count query returned no value");
        }

        return result.get(0);
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), selectSQL, criteria.getValues(), e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        DatabaseUtil.closeSilently(resultSet);
        DatabaseUtil.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException {
    final Map<String, Collection<Entity>> dependencyMap = new HashMap<>();
    if (Util.nullOrEmpty(entities)) {
      return dependencyMap;
    }

    final Set<Dependency> dependencies = resolveEntityDependencies(entities.iterator().next().getEntityID());
    for (final Dependency dependency : dependencies) {
      final List<Entity> dependentEntities = selectMany(EntityCriteriaUtil.selectCriteria(dependency.getEntityID(),
              dependency.getForeignKeyProperties(), EntityUtil.getKeys(entities)));
      if (!dependentEntities.isEmpty()) {
        dependencyMap.put(dependency.entityID, dependentEntities);
      }
    }

    return dependencyMap;
  }

  /** {@inheritDoc} */
  @Override
  public List executeFunction(final String functionID, final Object... arguments) throws DatabaseException {
    DatabaseException exception = null;
    try {
      logAccess("executeFunction: " + functionID, arguments);
      synchronized (connection) {
        return Databases.getFunction(functionID).execute(this, arguments);
      }
    }
    catch (final DatabaseException e) {
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
  public void executeProcedure(final String procedureID, final Object... arguments) throws DatabaseException {
    DatabaseException exception = null;
    try {
      logAccess("executeProcedure: " + procedureID, arguments);
      synchronized (connection) {
        Databases.getProcedure(procedureID).execute(this, arguments);
      }
    }
    catch (final DatabaseException e) {
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
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException {
    ReportException exception = null;
    synchronized (connection) {
      try {
        logAccess("fillReport", new Object[]{reportWrapper.getReportName()});
        final ReportResult result = reportWrapper.fillReport(connection.getConnection());
        if (!isTransactionOpen()) {
          commitQuietly();
        }

        return result;
      }
      catch (final ReportException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), null, Collections.singletonList(reportWrapper.getReportName()), e, null));
        throw e;
      }
      finally {
        final MethodLogger.Entry logEntry = logExit("fillReport", exception, null);
        if (LOG.isDebugEnabled()) {
          LOG.debug(DatabaseUtil.createLogMessage(getUser(), null, Collections.singletonList(reportWrapper.getReportName()), exception, logEntry));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final byte[] blobData) throws DatabaseException {
    final Property.ColumnProperty property = Entities.getColumnProperty(primaryKey.getEntityID(), blobPropertyID);
    if (property.getColumnType() != Types.BLOB) {
      throw new IllegalArgumentException("Property " + property.getPropertyID() + " in entity " +
              primaryKey.getEntityID() + " does not have column type BLOB");
    }
    final EntityCriteria criteria = EntityCriteriaUtil.criteria(primaryKey);
    final String sql = "update " + Entities.getTableName(primaryKey.getEntityID()) + " set " + property.getColumnName() +
            " = ?" + WHERE_SPACE_PREFIX + criteria.getWhereClause();
    final List<Object> values = new ArrayList<>();
    final List<Property.ColumnProperty> properties = new ArrayList<>();
    DatabaseUtil.QUERY_COUNTER.count(sql);
    synchronized (connection) {
      SQLException exception = null;
      ByteArrayInputStream inputStream = null;
      PreparedStatement statement = null;
      try {
        logAccess("writeBlob", new Object[]{sql});
        values.add(null);//the blob value, set explicitly later
        values.addAll(criteria.getValues());
        properties.add(property);
        properties.addAll(criteria.getValueKeys());

        statement = prepareStatement(sql);
        setParameterValues(statement, values, properties);
        inputStream = new ByteArrayInputStream(blobData);
        statement.setBinaryStream(1, inputStream);
        statement.executeUpdate();
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), sql, values, exception, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
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
  }

  /** {@inheritDoc} */
  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DatabaseException {
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
            Entities.getTableName(primaryKey.getEntityID()) + WHERE_SPACE_PREFIX + criteria.getWhereClause();
    DatabaseUtil.QUERY_COUNTER.count(sql);
    synchronized (connection) {
      try {
        logAccess("readBlob", new Object[]{sql});
        statement = prepareStatement(sql);
        setParameterValues(statement, criteria.getValues(), criteria.getValueKeys());

        resultSet = statement.executeQuery();
        final List<Blob> result = BLOB_RESULT_PACKER.pack(resultSet, 1);
        final Blob blob = result.get(0);
        final byte[] byteResult = blob.getBytes(1, (int) blob.length());
        commitIfTransactionIsNotOpen();

        return byteResult;
      }
      catch (final SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(DatabaseUtil.createLogMessage(getUser(), sql, criteria.getValues(), exception, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
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
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseConnection getDatabaseConnection() {
    return connection;
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

  private void performOptimisticLocking(final Map<String, Collection<Entity>> entitiesToLock) throws DatabaseException {
    if (optimisticLocking) {
      try {
        lockAndCheckForUpdate(entitiesToLock);
      }
      catch (final RecordModifiedException e) {
        rollbackQuietlyIfTransactionIsNotOpen();//releasing the select for update lock
        LOG.debug(EntityUtil.getModifiedExceptionMessage(e), e);
        throw e;
      }
      catch (final DatabaseException e) {
        rollbackQuietlyIfTransactionIsNotOpen();//releasing the select for update lock
        throw e;
      }
    }
  }

  /**
   * Selects the given entities for update and checks if they have been modified by comparing
   * the property values to the current values in the database. Note that this does not
   * include BLOB properties.
   * The calling method is responsible for releasing the select for update lock.
   * @param entities the entities to check, mapped to entityID
   * @throws DatabaseException in case of a database exception
   * @throws RecordModifiedException in case an entity has been modified, if an entity has been deleted,
   * the <code>modifiedRow</code> provided by the exception is null
   */
  private void lockAndCheckForUpdate(final Map<String, Collection<Entity>> entities) throws DatabaseException {
    for (final Map.Entry<String, Collection<Entity>> entry : entities.entrySet()) {
      final List<Entity.Key> originalKeys = EntityUtil.getKeys(entry.getValue(), true);
      final EntitySelectCriteria selectForUpdateCriteria = EntityCriteriaUtil.selectCriteria(originalKeys);
      selectForUpdateCriteria.setForUpdate(true);
      final List<Entity> currentValues = doSelectMany(selectForUpdateCriteria, 0);
      final Map<Entity.Key, Entity> mappedEntities = EntityUtil.mapToKey(currentValues);
      for (final Entity entity : entry.getValue()) {
        final Entity current = mappedEntities.get(entity.getOriginalKey());
        if (current == null) {
          throw new RecordModifiedException(entity, null);
        }
        final Property modified = EntityUtil.getModifiedProperty(entity, current);
        if (modified != null) {
          throw new RecordModifiedException(entity, current);
        }
      }
    }
  }

  private List<Entity> doSelectMany(final EntitySelectCriteria criteria, final int currentForeignKeyFetchDepth) throws DatabaseException {
    Objects.requireNonNull(criteria, CRITERIA_PARAM_NAME);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectSQL = null;
    try {
      selectSQL = getSelectSQL(criteria, connection.getDatabase());
      statement = prepareStatement(selectSQL);
      resultSet = executePreparedSelect(statement, selectSQL, criteria);
      final List<Entity> result = packResult(criteria, resultSet);
      if (!criteria.isForUpdate()) {
        setForeignKeys(result, criteria, currentForeignKeyFetchDepth);
      }

      return result;
    }
    catch (final SQLException e) {
      LOG.error(DatabaseUtil.createLogMessage(getUser(), selectSQL, criteria.getValues(), e, null));
      throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e), selectSQL);
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
   * @throws DatabaseException in case of a database exception
   * @see #setLimitForeignKeyFetchDepth(boolean)
   * @see org.jminor.framework.db.criteria.EntitySelectCriteria#setForeignKeyFetchDepthLimit(int)
   */
  private void setForeignKeys(final List<Entity> entities, final EntitySelectCriteria criteria, final int currentForeignKeyFetchDepth) throws DatabaseException {
    if (Util.nullOrEmpty(entities)) {
      return;
    }
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entities.get(0).getEntityID());
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      final int criteriaFetchDepthLimit = criteria.getForeignKeyFetchDepthLimit(foreignKeyProperty.getPropertyID());
      if (!limitForeignKeyFetchDepth || currentForeignKeyFetchDepth < criteriaFetchDepthLimit) {
        try {
          logAccess("setForeignKeys", new Object[] {foreignKeyProperty});
          final Collection<Entity.Key> referencedPrimaryKeys = getReferencedPrimaryKeys(entities, foreignKeyProperty);
          if (referencedPrimaryKeys.isEmpty()) {
            for (final Entity entity : entities) {
              entity.put(foreignKeyProperty, null, false);
            }
          }
          else {
            final EntitySelectCriteria referencedEntitiesCriteria = EntityCriteriaUtil.selectCriteria(referencedPrimaryKeys);
            referencedEntitiesCriteria.setForeignKeyFetchDepthLimit(criteriaFetchDepthLimit);
            final List<Entity> referencedEntities = doSelectMany(referencedEntitiesCriteria, currentForeignKeyFetchDepth + 1);
            final Map<Entity.Key, Entity> mappedReferencedEntities = EntityUtil.mapToKey(referencedEntities);
            for (final Entity entity : entities) {
              final Entity.Key referencedKey = entity.getReferencedKey(foreignKeyProperty);
              entity.put(foreignKeyProperty, getReferencedEntity(referencedKey, mappedReferencedEntities), false);
            }
          }
        }
        finally {
          logExit("setForeignKeys", null, null);
        }
      }
    }
  }

  private Entity getReferencedEntity(final Entity.Key referencedPrimaryKey, final Map<Entity.Key, Entity> mappedReferencedEntities) {
    if (referencedPrimaryKey == null) {
      return null;
    }
    Entity referencedEntity = mappedReferencedEntities.get(referencedPrimaryKey);
    if (referencedEntity == null) {
      //if the referenced entity is not found (it's been deleted or has been filtered out of an underlying view for example),
      //we create an empty entity wrapping the primary key since that's the best we can do under the circumstances
      referencedEntity = Entities.entity(referencedPrimaryKey);
    }

    return referencedEntity;
  }

  private void executePreparedUpdate(final PreparedStatement statement, final String sqlStatement,
                                     final List<Property.ColumnProperty> properties, final List<?> values) throws SQLException {
    SQLException exception = null;
    DatabaseUtil.QUERY_COUNTER.count(sqlStatement);
    try {
      logAccess("executePreparedUpdate", new Object[]{sqlStatement, values});
      setParameterValues(statement, values, properties);
      statement.executeUpdate();
    }
    catch (final SQLException e) {
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
                                          final EntityCriteria criteria) throws SQLException {
    SQLException exception = null;
    DatabaseUtil.QUERY_COUNTER.count(sqlStatement);
    final List<?> values = criteria.getValues();
    try {
      logAccess("executePreparedSelect", values == null ? new Object[]{sqlStatement} : new Object[]{sqlStatement, values});
      setParameterValues(statement, values, criteria.getValueKeys());

      return statement.executeQuery();
    }
    catch (final SQLException e) {
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

  private PreparedStatement prepareStatement(final String sqlStatement) throws SQLException {
    try {
      logAccess("prepareStatement", new Object[] {sqlStatement});
      return connection.getConnection().prepareStatement(sqlStatement);
    }
    finally {
      logExit("prepareStatement", null, null);
    }
  }

  private List<Entity> packResult(final EntitySelectCriteria criteria, final ResultSet resultSet) throws SQLException {
    List<Entity> result = null;
    SQLException packingException = null;
    try {
      logAccess("packResult", new Object[0]);
      result = Entities.getResultPacker(criteria.getEntityID()).pack(resultSet, criteria.getFetchCount());
    }
    catch (final SQLException e) {
      packingException = e;
      throw e;
    }
    finally {
      final String message = result != null ? "row count: " + result.size() : "";
      logExit("packResult", packingException, message);
    }

    return result;
  }

  private void commitQuietly() {
    try {
      connection.commit();
    }
    catch (final SQLException ignored) {
      LOG.error("Exception while performing a quiet commit", ignored);
    }
  }

  private void rollbackQuietly() {
    try {
      connection.rollback();
    }
    catch (final SQLException ignored) {
      LOG.error("Exception while performing a quiet rollback", ignored);
    }
  }

  private void commitIfTransactionIsNotOpen() throws SQLException {
    if (!isTransactionOpen()) {
      connection.commit();
    }
  }

  private void rollbackQuietlyIfTransactionIsNotOpen() {
    if (!isTransactionOpen()) {
      rollbackQuietly();
    }
  }

  private MethodLogger.Entry logExit(final String method, final Throwable exception, final String exitMessage) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      return methodLogger.logExit(method, exception, exitMessage);
    }

    return null;
  }

  private void logAccess(final String method, final Object[] arguments) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method, arguments);
    }
  }

  private static void setParameterValues(final PreparedStatement statement, final List<?> values,
                                         final List<Property.ColumnProperty> parameterProperties) throws SQLException {
    if (Util.nullOrEmpty(values) || statement.getParameterMetaData().getParameterCount() == 0) {
      return;
    }
    if (parameterProperties == null || parameterProperties.size() != values.size()) {
      throw new SQLException("Parameter property value count mismatch: " + (parameterProperties == null ?
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
    catch (final SQLException e) {
      LOG.error("Unable to set parameter: " + property + ", value: " + value + ", value class: " + (value == null ? "null" : value.getClass()), e);
      throw e;
    }
  }

  private static Collection<Entity.Key> getReferencedPrimaryKeys(final List<Entity> entities,
                                                                 final Property.ForeignKeyProperty foreignKeyProperty) {
    final Set<Entity.Key> keySet = new HashSet<>(entities.size());
    for (final Entity entity : entities) {
      final Entity.Key key = entity.getReferencedKey(foreignKeyProperty);
      if (key != null) {
        keySet.add(key);
      }
    }

    return keySet;
  }

  private static String getSelectSQL(final EntitySelectCriteria criteria, final Database database) {
    final String entityID = criteria.getEntityID();
    boolean containsWhereClause = false;
    String selectSQL = Entities.getSelectQuery(entityID);
    if (selectSQL == null) {
      selectSQL = createSelectSQL(Entities.getSelectTableName(entityID), Entities.getSelectColumnsString(entityID), null, null);
    }
    else {
      containsWhereClause = Entities.selectQueryContainsWhereClause(entityID);
    }

    final StringBuilder queryBuilder = new StringBuilder(selectSQL);
    final String whereClause = criteria.getWhereClause();
    if (whereClause.length() > 0) {
      queryBuilder.append(containsWhereClause ? " and " : WHERE_SPACE_PREFIX).append(whereClause);
    }
    if (criteria.isForUpdate()) {
      addForUpdate(database, queryBuilder);
    }
    else {
      addGroupHavingOrderByAndLimitClauses(criteria, queryBuilder);
    }

    return queryBuilder.toString();
  }

  private static void addForUpdate(final Database database, final StringBuilder queryBuilder) {
    queryBuilder.append(" for update");
    if (database.supportsNowait()) {
      queryBuilder.append(" nowait");
    }
  }

  private static void addGroupHavingOrderByAndLimitClauses(final EntitySelectCriteria criteria, final StringBuilder queryBuilder) {
    final String entityID = criteria.getEntityID();
    final String groupByClause = Entities.getGroupByClause(entityID);
    if (groupByClause != null) {
      queryBuilder.append(" group by ").append(groupByClause);
    }
    final String havingClause = Entities.getHavingClause(entityID);
    if (havingClause != null) {
      queryBuilder.append(" having ").append(havingClause);
    }
    final String orderByClause = criteria.getOrderByClause();
    if (orderByClause != null) {
      queryBuilder.append(" order by ").append(orderByClause);
    }
    if (criteria.getLimit() > 0) {
      queryBuilder.append(" limit ");
      queryBuilder.append(criteria.getLimit());
      if (criteria.getOffset() > 0) {
        queryBuilder.append(" offset ");
        queryBuilder.append(criteria.getOffset());
      }
    }
  }

  /**
   * @param tableName the table name
   * @param updateProperties the properties being updated
   * @param criteria the primary key criteria for the given entity
   * @return a query for updating this entity instance
   */
  private static String createUpdateSQL(final String tableName, final Collection<Property.ColumnProperty> updateProperties,
                                        final EntityCriteria criteria) {
    final StringBuilder sql = new StringBuilder("update ");
    sql.append(tableName).append(" set ");
    int columnIndex = 0;
    for (final Property.ColumnProperty property : updateProperties) {
      sql.append(property.getColumnName()).append(" = ?");
      if (columnIndex++ < updateProperties.size() - 1) {
        sql.append(", ");
      }
    }

    return sql.append(WHERE_SPACE_PREFIX).append(criteria.getWhereClause()).toString();
  }

  /**
   * @param entityID the entityID
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting this entity instance
   */
  private static String createInsertSQL(final String entityID, final Collection<Property.ColumnProperty> insertProperties) {
    final String tableName = Entities.getTableName(entityID);
    final StringBuilder sql = new StringBuilder("insert ").append(getInsertHint(tableName))
            .append("into ").append(tableName).append("(");
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
    Objects.requireNonNull(criteria, CRITERIA_PARAM_NAME);
    final String whereClause = criteria.getWhereClause();
    return "delete from " + Entities.getTableName(criteria.getEntityID()) +
            (whereClause.length() > 0 ? WHERE_SPACE_PREFIX + whereClause : "");
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
      final Object value = entity.get(property);
      final boolean insertingAndNonNull = inserting && value != null;
      final boolean updatingAndModified = !inserting && entity.isModified(property.getPropertyID());
      if (insertingAndNonNull || updatingAndModified) {
        properties.add(property);
        values.add(value);
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
    final Set<Dependency> dependencies = new HashSet<>();
    for (final String entityIDToCheck : entityIDs) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityIDToCheck)) {
        if (foreignKeyProperty.getReferencedEntityID().equals(entityID)) {
          dependencies.add(new Dependency(entityIDToCheck, foreignKeyProperty.getReferenceProperties()));
        }
      }
    }

    return dependencies;
  }

  private static String getInsertHint(final String tableName) {
    final String insertHint = Databases.getInsertHint(tableName);
    if (Util.nullOrEmpty(insertHint)) {
      return "";
    }
    else {
      return insertHint + " ";
    }
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

  private static final class BlobPacker implements ResultPacker<Blob> {
    @Override
    public List<Blob> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<java.sql.Blob> blobs = new ArrayList<>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        blobs.add(resultSet.getBlob(1));
      }

      return blobs;
    }
  }

  /**
   * A {@link MethodLogger.ArgumentStringProvider} implementation tailored for EntityConnections
   */
  static final class EntityArgumentStringProvider extends MethodLogger.DefaultArgumentStringProvider {

    @Override
    public String toString(final Object argument) {
      if (argument == null) {
        return "";
      }

      final StringBuilder builder = new StringBuilder();
      if (argument instanceof EntityCriteria) {
        builder.append(getEntityCriteriaString((EntityCriteria) argument));
      }
      else if (argument instanceof Object[] && ((Object[]) argument).length > 0) {
        builder.append("[").append(toString((Object[]) argument)).append("]");
      }
      else if (argument instanceof Collection && !((Collection) argument).isEmpty()) {
        builder.append("[").append(toString(((Collection) argument).toArray())).append("]");
      }
      else if (argument instanceof Entity) {
        builder.append(getEntityParameterString((Entity) argument));
      }
      else if (argument instanceof Entity.Key) {
        builder.append(getEntityKeyParameterString((Entity.Key) argument));
      }
      else {
        builder.append(argument.toString());
      }

      return builder.toString();
    }

    private String getEntityCriteriaString(final EntityCriteria criteria) {
      final StringBuilder builder = new StringBuilder();
      builder.append(criteria.getEntityID());
      final String whereClause = criteria.getWhereClause();
      if (!Util.nullOrEmpty(whereClause)) {
        builder.append(",").append(WHERE_SPACE_PREFIX).append(whereClause);
      }
      final List<?> values = criteria.getValues();
      if (values != null) {
        builder.append(", ").append(toString(values));
      }

      return builder.toString();
    }

    private static String getEntityParameterString(final Entity entity) {
      final StringBuilder builder = new StringBuilder();
      builder.append(entity.getEntityID()).append(" {");
      for (final Property.ColumnProperty property : Entities.getColumnProperties(entity.getEntityID(), true, true, true)) {
        final boolean modified = entity.isModified(property.getPropertyID());
        if (property.isPrimaryKeyProperty() || modified) {
          final StringBuilder valueString = new StringBuilder();
          if (modified) {
            valueString.append(entity.getOriginal(property.getPropertyID())).append("->");
          }
          valueString.append(entity.get(property.getPropertyID()));
          builder.append(property.getPropertyID()).append(":").append(valueString).append(",");
        }
      }
      builder.deleteCharAt(builder.length() - 1);

      return builder.append("}").toString();
    }

    private static String getEntityKeyParameterString(final Entity.Key argument) {
      return argument.getEntityID() + ", " + argument.toString();
    }
  }
}