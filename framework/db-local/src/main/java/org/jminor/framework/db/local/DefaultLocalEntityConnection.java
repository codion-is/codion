/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Conjunction;
import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.ResultIterator;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.MultipleRecordsFoundException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.db.exception.ReferentialIntegrityException;
import org.jminor.common.db.exception.UniqueConstraintException;
import org.jminor.common.db.exception.UpdateException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.condition.WhereCondition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.SubqueryProperty;
import org.jminor.framework.domain.property.TransientProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.common.db.ConditionType.LIKE;
import static org.jminor.common.db.ConditionType.NOT_LIKE;
import static org.jminor.common.db.DatabaseConnections.createConnection;
import static org.jminor.common.db.Databases.*;
import static org.jminor.framework.db.condition.Conditions.*;
import static org.jminor.framework.domain.Entities.*;

/**
 * A default LocalEntityConnection implementation
 */
final class DefaultLocalEntityConnection implements LocalEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalEntityConnection.class.getName(), Locale.getDefault());
  private static final String RECORD_MODIFIED_EXCEPTION = "record_modified_exception";

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalEntityConnection.class);
  private static final String CONDITION_PARAM_NAME = "condition";
  private static final String WHERE_SPACE_PREFIX_POSTFIX = " where ";

  /**
   * A result packer for fetching blobs from a result set containing a single blob column
   */
  private static final ResultPacker<Blob> BLOB_RESULT_PACKER = new BlobPacker();

  private final Domain domain;
  private final DatabaseConnection connection;
  private final Map<String, List<ColumnProperty>> insertProperties = new HashMap<>();
  private final Map<String, List<ColumnProperty>> updateProperties = new HashMap<>();
  private final Map<String, List<ForeignKeyProperty>> foreignKeyReferenceMap = new HashMap<>();
  private final Map<String, String[]> writableColumnPropertyIds = new HashMap<>();

  private boolean optimisticLocking = true;
  private boolean limitForeignKeyFetchDepth = true;

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @param validityCheckTimeout specifies the timeout in seconds when validating this connection
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  DefaultLocalEntityConnection(final Domain domain, final Database database, final User user,
                               final int validityCheckTimeout) throws DatabaseException {
    this.domain = new Domain(requireNonNull(domain, "domain"));
    this.connection = createConnection(database, user, validityCheckTimeout);
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param connection the Connection object to base this EntityConnection on, it is assumed to be in a valid state
   * @param validityCheckTimeout specifies the timeout in seconds when validating this connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see org.jminor.common.db.Database#supportsIsValid()
   */
  DefaultLocalEntityConnection(final Domain domain, final Database database, final Connection connection,
                               final int validityCheckTimeout) throws DatabaseException {
    this.domain = new Domain(requireNonNull(domain, "domain"));
    this.connection = createConnection(database, connection, validityCheckTimeout);
  }

  /** {@inheritDoc} */
  @Override
  public LocalEntityConnection setMethodLogger(final MethodLogger methodLogger) {
    synchronized (connection) {
      connection.setMethodLogger(methodLogger);
    }

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public MethodLogger getMethodLogger() {
    synchronized (connection) {
      return connection.getMethodLogger();
    }
  }

  /** {@inheritDoc} */
  @Override
  public Domain getDomain() {
    return domain;
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
    if (nullOrEmpty(entities)) {
      return emptyList();
    }
    checkReadOnly(entities);

    final List<Entity.Key> insertedKeys = new ArrayList<>(entities.size());
    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String insertSQL = null;
    synchronized (connection) {
      try {
        final List<ColumnProperty> statementProperties = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
          final Entity entity = entities.get(i);
          final String entityId = entity.getEntityId();
          final Entity.Definition entityDefinition = getEntityDefinition(entityId);
          final Entity.KeyGenerator keyGenerator = entityDefinition.getKeyGenerator();
          keyGenerator.beforeInsert(entity, connection);

          final List<ColumnProperty> insertableProperties =
                  getInsertableProperties(entityDefinition, !keyGenerator.getType().isAutoIncrement());

          populateStatementPropertiesAndValues(true, entity, insertableProperties, statementProperties, statementValues);

          final String[] returnColumns = keyGenerator.returnPrimaryKeyValues() ? getPrimaryKeyColumnNames(entityDefinition) : null;
          insertSQL = createInsertSQL(entityDefinition.getTableName(), statementProperties);
          statement = prepareStatement(insertSQL, returnColumns);
          executePreparedUpdate(statement, insertSQL, statementProperties, statementValues);
          keyGenerator.afterInsert(entity, connection, statement);

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
        LOG.error(createLogMessage(getUser(), insertSQL, statementValues, e, null));
        throw translateInsertUpdateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> update(final List<Entity> entities) throws DatabaseException {
    if (nullOrEmpty(entities)) {
      return entities;
    }
    final Map<String, List<Entity>> entitiesByEntityId = mapToEntityId(entities);
    for (final String entityId : entitiesByEntityId.keySet()) {
      checkReadOnly(entityId);
    }

    final List<Object> propertyValuesToSet = new ArrayList<>();
    PreparedStatement statement = null;
    String updateSQL = null;
    synchronized (connection) {
      try {
        lockAndCheckForModification(entitiesByEntityId);

        final List<ColumnProperty> propertiesToUpdate = new ArrayList<>();
        final List<Entity> updatedEntities = new ArrayList<>(entities.size());
        for (final Map.Entry<String, List<Entity>> entityIdEntities : entitiesByEntityId.entrySet()) {
          final Entity.Definition entityDefinition = getEntityDefinition(entityIdEntities.getKey());
          final List<ColumnProperty> updatableProperties = getUpdatableProperties(entityDefinition);

          final List<Entity> entitiesToUpdate = entityIdEntities.getValue();
          for (final Entity entityToUpdate : entitiesToUpdate) {
            populateStatementPropertiesAndValues(false, entityToUpdate, updatableProperties, propertiesToUpdate, propertyValuesToSet);

            final WhereCondition updateCondition =
                    whereCondition(entityCondition(entityToUpdate.getOriginalKey()), entityDefinition);
            updateSQL = createUpdateSQL(entityDefinition.getTableName(), propertiesToUpdate, updateCondition.getWhereClause());
            propertiesToUpdate.addAll(updateCondition.getColumnProperties());
            propertyValuesToSet.addAll(updateCondition.getValues());
            statement = prepareStatement(updateSQL);
            final int updatedRows = executePreparedUpdate(statement, updateSQL, propertiesToUpdate, propertyValuesToSet);
            if (updatedRows == 0) {
              throw new UpdateException("Update did not affect any rows");
            }

            statement.close();
            propertiesToUpdate.clear();
            propertyValuesToSet.clear();
          }
          final List<Entity> selected = doSelect(entitySelectCondition(getKeys(entitiesToUpdate)));
          if (selected.size() != entitiesToUpdate.size()) {
            throw new UpdateException(entitiesToUpdate.size() + " updated rows expected, query returned " +
                    selected.size() + " entityId: " + entityIdEntities.getKey());
          }
          updatedEntities.addAll(selected);
        }

        commitIfTransactionIsNotOpen();

        return updatedEntities;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(getUser(), updateSQL, propertyValuesToSet, e, null));
        throw translateInsertUpdateSQLException(e);
      }
      catch (final RecordModifiedException e) {
        rollbackQuietlyIfTransactionIsNotOpen();//releasing the select for update lock
        LOG.debug(e.getMessage(), e);
        throw e;
      }
      catch (final UpdateException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(getUser(), updateSQL, propertyValuesToSet, e, null));
        throw e;
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCondition condition) throws DatabaseException {
    requireNonNull(condition, CONDITION_PARAM_NAME);
    checkReadOnly(condition.getEntityId());

    final Entity.Definition entityDefinition = getEntityDefinition(condition.getEntityId());
    final WhereCondition whereCondition = whereCondition(condition, entityDefinition);
    PreparedStatement statement = null;
    String deleteSQL = null;
    synchronized (connection) {
      try {
        deleteSQL = createDeleteSQL(entityDefinition.getTableName(), whereCondition.getWhereClause());
        statement = prepareStatement(deleteSQL);
        executePreparedUpdate(statement, deleteSQL, whereCondition.getColumnProperties(), whereCondition.getValues());
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(getUser(), deleteSQL, whereCondition.getValues(), e, null));
        throw translateDeleteSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final List<Entity.Key> entityKeys) throws DatabaseException {
    if (nullOrEmpty(entityKeys)) {
      return;
    }
    final Map<String, List<Entity.Key>> keysByEntityId = mapKeysToEntityId(entityKeys);
    for (final String entityId : keysByEntityId.keySet()) {
      checkReadOnly(entityId);
    }
    PreparedStatement statement = null;
    String deleteSQL = null;
    synchronized (connection) {
      try {
        for (final Map.Entry<String, List<Entity.Key>> entityIdKeys : keysByEntityId.entrySet()) {
          final Entity.Definition entityDefinition = getEntityDefinition(entityIdKeys.getKey());
          final WhereCondition whereCondition = whereCondition(entityCondition(entityIdKeys.getValue()), entityDefinition);
          deleteSQL = createDeleteSQL(entityDefinition.getTableName(), whereCondition.getWhereClause());
          statement = prepareStatement(deleteSQL);
          executePreparedUpdate(statement, deleteSQL, whereCondition.getColumnProperties(), whereCondition.getValues());
          statement.close();
        }
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(getUser(), deleteSQL, entityKeys, e, null));
        throw translateDeleteSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final String entityId, final String propertyId, final Object value) throws DatabaseException {
    return selectSingle(entitySelectCondition(entityId, propertyId, LIKE, value));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(entitySelectCondition(key));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException {
    final List<Entity> entities = select(condition);
    if (entities.isEmpty()) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (entities.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("many_records_found"));
    }

    return entities.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> select(final List<Entity.Key> keys) throws DatabaseException {
    final List<Entity> result = new ArrayList<>();
    if (nullOrEmpty(keys)) {
      return result;
    }

    synchronized (connection) {
      try {
        for (final List<Entity.Key> entityIdKeys : mapKeysToEntityId(keys).values()) {
          result.addAll(doSelect(entitySelectCondition(entityIdKeys)));
        }
        if (!isTransactionOpen()) {
          commitQuietly();
        }

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> select(final String entityId, final String propertyId, final Object... values) throws DatabaseException {
    return select(entitySelectCondition(entityId, propertyId, LIKE, values == null ? null : asList(values)));
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> select(final EntitySelectCondition condition) throws DatabaseException {
    requireNonNull(condition, CONDITION_PARAM_NAME);
    synchronized (connection) {
      try {
        final List<Entity> result = doSelect(condition);
        if (!isTransactionOpen() && !condition.isForUpdate()) {
          commitQuietly();
        }

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Object> selectValues(final String propertyId, final EntityCondition condition) throws DatabaseException {
    requireNonNull(condition, CONDITION_PARAM_NAME);
    final Entity.Definition entityDefinition = getEntityDefinition(condition.getEntityId());
    if (entityDefinition.getSelectQuery() != null) {
      throw new UnsupportedOperationException("selectValues is not implemented for entities with custom select queries");
    }
    final WhereCondition combinedCondition = whereCondition(entityCondition(condition.getEntityId(), conditionSet(Conjunction.AND,
            expand(condition.getCondition(), entityDefinition),
            Conditions.propertyCondition(propertyId, NOT_LIKE, null))), entityDefinition);
    final ColumnProperty propertyToSelect = entityDefinition.getColumnProperty(propertyId);
    final String columnName = propertyToSelect.getColumnName();
    final String selectSQL = createSelectSQL(entityDefinition.getSelectTableName(),
            "distinct " + columnName, combinedCondition.getWhereClause(), columnName);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    QUERY_COUNTER.count(selectSQL);
    synchronized (connection) {
      try {
        statement = prepareStatement(selectSQL);
        resultSet = executePreparedSelect(statement, selectSQL, combinedCondition);
        final List<Object> result = propertyToSelect.getResultPacker().pack(resultSet, -1);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(getUser(), selectSQL, asList(propertyId, combinedCondition), e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        closeSilently(resultSet);
        closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public int selectRowCount(final EntityCondition condition) throws DatabaseException {
    requireNonNull(condition, CONDITION_PARAM_NAME);
    final Entity.Definition entityDefinition = getEntityDefinition(condition.getEntityId());
    final WhereCondition whereCondition = whereCondition(condition, entityDefinition);
    final String baseSQLQuery = createSelectSQL(whereCondition, entityDefinition.getPrimaryKeyProperties(), entityDefinition);
    final String rowCountSQLQuery = createSelectSQL("(" + baseSQLQuery + ")", "count(*)", null, null);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    QUERY_COUNTER.count(rowCountSQLQuery);
    synchronized (connection) {
      try {
        statement = prepareStatement(rowCountSQLQuery);
        resultSet = executePreparedSelect(statement, rowCountSQLQuery, whereCondition);
        final List<Integer> result = INTEGER_RESULT_PACKER.pack(resultSet, -1);
        commitIfTransactionIsNotOpen();
        if (result.isEmpty()) {
          throw new SQLException("Row count query returned no value");
        }

        return result.get(0);
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(getUser(), rowCountSQLQuery, whereCondition.getValues(), e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        closeSilently(resultSet);
        closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Collection<Entity>> selectDependencies(final Collection<Entity> entities) throws DatabaseException {
    final Map<String, Collection<Entity>> dependencyMap = new HashMap<>();
    if (nullOrEmpty(entities)) {
      return dependencyMap;
    }

    final Collection<ForeignKeyProperty> foreignKeyReferences = getForeignKeyReferences(
            entities.iterator().next().getEntityId());
    for (final ForeignKeyProperty foreignKeyReference : foreignKeyReferences) {
      if (!foreignKeyReference.isSoftReference()) {
        final List<Entity> dependencies = select(entitySelectCondition(foreignKeyReference.getEntityId(),
                foreignKeyReference.getPropertyId(), LIKE, entities));
        if (!dependencies.isEmpty()) {
          dependencyMap.put(foreignKeyReference.getEntityId(), dependencies);
        }
      }
    }

    return dependencyMap;
  }

  /** {@inheritDoc} */
  @Override
  public List executeFunction(final String functionId, final Object... arguments) throws DatabaseException {
    DatabaseException exception = null;
    try {
      logAccess("executeFunction: " + functionId, arguments);
      synchronized (connection) {
        return domain.getFunction(functionId).execute(this, arguments);
      }
    }
    catch (final DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(getUser(), functionId, arguments == null ? null : asList(arguments), e, null));
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executeFunction: " + functionId, exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(createLogMessage(getUser(), "", arguments == null ? null : asList(arguments), exception, entry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void executeProcedure(final String procedureId, final Object... arguments) throws DatabaseException {
    DatabaseException exception = null;
    try {
      logAccess("executeProcedure: " + procedureId, arguments);
      synchronized (connection) {
        domain.getProcedure(procedureId).execute(this, arguments);
      }
    }
    catch (final DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(getUser(), procedureId, arguments == null ? null : asList(arguments), e, null));
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executeProcedure: " + procedureId, exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(createLogMessage(getUser(), "", arguments == null ? null : asList(arguments), exception, entry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException {
    ReportException exception = null;
    synchronized (connection) {
      try {
        logAccess("fillReport", new Object[] {reportWrapper.getReportName()});
        final ReportResult result = reportWrapper.fillReport(connection.getConnection());
        if (!isTransactionOpen()) {
          commitQuietly();
        }

        return result;
      }
      catch (final ReportException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(getUser(), null, singletonList(reportWrapper.getReportName()), e, null));
        throw e;
      }
      finally {
        final MethodLogger.Entry logEntry = logExit("fillReport", exception, null);
        if (LOG.isDebugEnabled()) {
          LOG.debug(createLogMessage(getUser(), null, singletonList(reportWrapper.getReportName()), exception, logEntry));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyId, final byte[] blobData) throws DatabaseException {
    requireNonNull(blobData, "blobData");
    final Entity.Definition entityDefinition = getEntityDefinition(requireNonNull(primaryKey, "primaryKey").getEntityId());
    checkReadOnly(entityDefinition.getEntityId());
    final ColumnProperty blobProperty = entityDefinition.getColumnProperty(blobPropertyId);
    if (blobProperty.getColumnType() != Types.BLOB) {
      throw new IllegalArgumentException("Property " + blobProperty.getPropertyId() + " in entity " +
              primaryKey.getEntityId() + " does not have column type BLOB");
    }
    final WhereCondition whereCondition = whereCondition(entityCondition(primaryKey), entityDefinition);
    final String updateSQL = "update " + entityDefinition.getTableName() + " set " + blobProperty.getColumnName() + " = ?" +
            WHERE_SPACE_PREFIX_POSTFIX + whereCondition.getWhereClause();
    final List<Object> statementValues = new ArrayList<>();
    statementValues.add(null);//the blob value, set explicitly later
    statementValues.addAll(whereCondition.getValues());
    final List<ColumnProperty> statementProperties = new ArrayList<>();
    statementProperties.add(blobProperty);
    statementProperties.addAll(whereCondition.getColumnProperties());
    QUERY_COUNTER.count(updateSQL);
    synchronized (connection) {
      SQLException exception = null;
      PreparedStatement statement = null;
      try {
        logAccess("writeBlob", new Object[] {updateSQL});
        statement = prepareStatement(updateSQL);
        setParameterValues(statement, statementProperties, statementValues);
        statement.setBinaryStream(1, new ByteArrayInputStream(blobData));//no need to close ByteArrayInputStream
        if (statement.executeUpdate() > 1) {
          throw new UpdateException("Blob write updated more than one row, key: " + primaryKey);
        }
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(getUser(), updateSQL, statementValues, exception, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        closeSilently(statement);
        final MethodLogger.Entry logEntry = logExit("writeBlob", exception, null);
        if (LOG.isDebugEnabled()) {
          LOG.debug(createLogMessage(getUser(), updateSQL, statementValues, exception, logEntry));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws DatabaseException {
    final Entity.Definition entityDefinition = getEntityDefinition(requireNonNull(primaryKey, "primaryKey").getEntityId());
    final ColumnProperty blobProperty = entityDefinition.getColumnProperty(blobPropertyId);
    if (blobProperty.getColumnType() != Types.BLOB) {
      throw new IllegalArgumentException("Property " + blobProperty.getPropertyId() + " in entity " +
              primaryKey.getEntityId() + " does not have column type BLOB");
    }
    PreparedStatement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    final WhereCondition whereCondition = whereCondition(entityCondition(primaryKey), entityDefinition);
    final String updateSQL = "select " + blobProperty.getColumnName() + " from " +
            entityDefinition.getTableName() + WHERE_SPACE_PREFIX_POSTFIX + whereCondition.getWhereClause();
    QUERY_COUNTER.count(updateSQL);
    synchronized (connection) {
      try {
        logAccess("readBlob", new Object[] {updateSQL});
        statement = prepareStatement(updateSQL);
        setParameterValues(statement, whereCondition.getColumnProperties(), whereCondition.getValues());

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
        LOG.error(createLogMessage(getUser(), updateSQL, whereCondition.getValues(), exception, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        closeSilently(statement);
        closeSilently(resultSet);
        final MethodLogger.Entry logEntry = logExit("readBlob", exception, null);
        if (LOG.isDebugEnabled()) {
          LOG.debug(createLogMessage(getUser(), updateSQL, whereCondition.getValues(), exception, logEntry));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseConnection getDatabaseConnection() {
    return connection;
  }

  /** {@inheritDoc} */
  @Override
  public ResultIterator<Entity> iterator(final EntitySelectCondition condition) throws DatabaseException {
    synchronized (connection) {
      try {
        return createIterator(condition);
      }
      catch (final SQLException e) {
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isOptimisticLocking() {
    return optimisticLocking;
  }

  /** {@inheritDoc} */
  @Override
  public LocalEntityConnection setOptimisticLocking(final boolean optimisticLocking) {
    this.optimisticLocking = optimisticLocking;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLimitForeignKeyFetchDepth() {
    return limitForeignKeyFetchDepth;
  }

  /** {@inheritDoc} */
  @Override
  public LocalEntityConnection setLimitForeignKeyFetchDepth(final boolean limitForeignKeyFetchDepth) {
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
    return this;
  }

  /**
   * Selects the given entities for update and checks if they have been modified by comparing
   * the property values to the current values in the database. Note that this does not
   * include BLOB properties or properties that are readOnly.
   * The calling method is responsible for releasing the select for update lock.
   * @param entitiesByEntityId the entities to check, mapped to entityId
   * @throws SQLException in case of exception
   * @throws RecordModifiedException in case an entity has been modified, if an entity has been deleted,
   * the {@code modifiedRow} provided by the exception is null
   */
  private void lockAndCheckForModification(final Map<String, List<Entity>> entitiesByEntityId) throws SQLException, RecordModifiedException {
    if (!optimisticLocking) {
      return;
    }
    for (final Map.Entry<String, List<Entity>> entitiesByEntityIdEntry : entitiesByEntityId.entrySet()) {
      final List<Entity.Key> originalKeys = getOriginalKeys(entitiesByEntityIdEntry.getValue());
      final EntitySelectCondition selectForUpdateCondition = entitySelectCondition(originalKeys);
      selectForUpdateCondition.setSelectPropertyIds(getWritableColumnPropertyIds(entitiesByEntityIdEntry.getKey()));
      selectForUpdateCondition.setForUpdate(true);
      final List<Entity> currentEntities = doSelect(selectForUpdateCondition);
      final Map<Entity.Key, Entity> currentEntitiesByKey = mapToKey(currentEntities);
      for (final Entity entity : entitiesByEntityIdEntry.getValue()) {
        final Entity current = currentEntitiesByKey.get(entity.getOriginalKey());
        if (current == null) {
          throw new RecordModifiedException(entity, null, MESSAGES.getString(RECORD_MODIFIED_EXCEPTION)
                  + ", " + entity.getOriginalCopy() + " " + MESSAGES.getString("has_been_deleted"));
        }
        final List<ColumnProperty> modified = getModifiedColumnProperties(entity, current, false);
        if (!modified.isEmpty()) {
          throw new RecordModifiedException(entity, current, createModifiedExceptionMessage(entity, current, modified));
        }
      }
    }
  }

  private List<Entity> doSelect(final EntitySelectCondition condition) throws SQLException {
    return doSelect(condition, 0);
  }

  private List<Entity> doSelect(final EntitySelectCondition condition, final int currentForeignKeyFetchDepth) throws SQLException {
    final List<Entity> result;
    try (final ResultIterator<Entity> iterator = createIterator(condition)) {
      result = packResult(iterator);
    }
    if (!condition.isForUpdate()) {
      setForeignKeys(result, condition, currentForeignKeyFetchDepth);
    }

    return result;
  }

  /**
   * Selects the entities referenced by the given entities via foreign keys and sets those
   * as their respective foreign key values. This is done recursively for the entities referenced
   * by the foreign keys as well, until the condition fetch depth limit is reached.
   * @param entities the entities for which to set the foreign key entity values
   * @param condition the condition
   * @param currentForeignKeyFetchDepth the current foreign key fetch depth
   * @throws SQLException in case of a database exception
   * @see #setLimitForeignKeyFetchDepth(boolean)
   * @see EntitySelectCondition#setForeignKeyFetchDepthLimit(int)
   */
  private void setForeignKeys(final List<Entity> entities, final EntitySelectCondition condition,
                              final int currentForeignKeyFetchDepth) throws SQLException {
    if (nullOrEmpty(entities)) {
      return;
    }
    final List<ForeignKeyProperty> foreignKeyProperties =
            getEntityDefinition(entities.get(0).getEntityId()).getForeignKeyProperties();
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      final ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      Integer conditionFetchDepthLimit = condition.getForeignKeyFetchDepthLimit(foreignKeyProperty.getPropertyId());
      if (conditionFetchDepthLimit == null) {//use the default one
        conditionFetchDepthLimit = foreignKeyProperty.getFetchDepth();
      }
      if (!limitForeignKeyFetchDepth || conditionFetchDepthLimit == -1 || currentForeignKeyFetchDepth < conditionFetchDepthLimit) {
        try {
          logAccess("setForeignKeys", new Object[] {foreignKeyProperty});
          final List<Entity.Key> referencedKeys = getReferencedKeys(entities, foreignKeyProperty);
          if (referencedKeys.isEmpty()) {
            for (int j = 0; j < entities.size(); j++) {
              entities.get(j).put(foreignKeyProperty, null, false);
            }
          }
          else {
            final EntitySelectCondition referencedEntitiesCondition = entitySelectCondition(referencedKeys);
            referencedEntitiesCondition.setForeignKeyFetchDepthLimit(conditionFetchDepthLimit);
            final List<Entity> referencedEntities = doSelect(referencedEntitiesCondition,
                    currentForeignKeyFetchDepth + 1);
            final Map<Entity.Key, Entity> referencedEntitiesMappedByKey = mapToKey(referencedEntities);
            for (int j = 0; j < entities.size(); j++) {
              final Entity entity = entities.get(j);
              final Entity.Key referencedKey = entity.getReferencedKey(foreignKeyProperty);
              entity.put(foreignKeyProperty, getReferencedEntity(referencedKey, referencedEntitiesMappedByKey), false);
            }
          }
        }
        finally {
          logExit("setForeignKeys", null, null);
        }
      }
    }
  }

  private Entity getReferencedEntity(final Entity.Key referencedPrimaryKey, final Map<Entity.Key, Entity> entitiesMappedByKey) {
    if (referencedPrimaryKey == null) {
      return null;
    }
    Entity referencedEntity = entitiesMappedByKey.get(referencedPrimaryKey);
    if (referencedEntity == null) {
      //if the referenced entity is not found (it's been deleted or has been filtered out of an underlying view for example),
      //we create an empty entity wrapping the primary key since that's the best we can do under the circumstances
      referencedEntity = domain.entity(referencedPrimaryKey);
    }

    return referencedEntity;
  }

  private ResultIterator<Entity> createIterator(final EntitySelectCondition selectCondition) throws SQLException {
    requireNonNull(selectCondition, CONDITION_PARAM_NAME);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectSQL = null;
    final Entity.Definition entityDefinition = getEntityDefinition(selectCondition.getEntityId());
    final WhereCondition whereCondition = whereCondition(selectCondition, entityDefinition);
    final List<ColumnProperty> propertiesToSelect = selectCondition.getSelectPropertyIds().isEmpty() ?
            entityDefinition.getSelectableColumnProperties() :
            entityDefinition.getSelectableColumnProperties(selectCondition.getSelectPropertyIds());
    try {
      selectSQL = createSelectSQL(whereCondition, propertiesToSelect, entityDefinition);
      statement = prepareStatement(selectSQL);
      resultSet = executePreparedSelect(statement, selectSQL, whereCondition);

      return new EntityResultIterator(statement, resultSet, new EntityResultPacker(
              selectCondition.getEntityId(), propertiesToSelect,
              entityDefinition.getTransientProperties()), selectCondition.getFetchCount());
    }
    catch (final SQLException e) {
      closeSilently(resultSet);
      closeSilently(statement);
      LOG.error(createLogMessage(getUser(), selectSQL, whereCondition.getValues(), e, null));
      throw e;
    }
  }

  private int executePreparedUpdate(final PreparedStatement statement, final String sqlStatement,
                                    final List<ColumnProperty> statementProperties,
                                    final List statementValues) throws SQLException {
    SQLException exception = null;
    QUERY_COUNTER.count(sqlStatement);
    try {
      logAccess("executePreparedUpdate", new Object[] {sqlStatement, statementValues});
      setParameterValues(statement, statementProperties, statementValues);
      return statement.executeUpdate();
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executePreparedUpdate", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(createLogMessage(getUser(), sqlStatement, statementValues, exception, entry));
      }
    }
  }

  private ResultSet executePreparedSelect(final PreparedStatement statement, final String sqlStatement,
                                          final WhereCondition whereCondition) throws SQLException {
    SQLException exception = null;
    QUERY_COUNTER.count(sqlStatement);
    final List statementValues = whereCondition.getValues();
    try {
      logAccess("executePreparedSelect", statementValues == null ?
              new Object[] {sqlStatement} : new Object[] {sqlStatement, statementValues});
      setParameterValues(statement, whereCondition.getColumnProperties(), statementValues);

      return statement.executeQuery();
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executePreparedSelect", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(createLogMessage(getUser(), sqlStatement, statementValues, exception, entry));
      }
    }
  }

  private PreparedStatement prepareStatement(final String sqlStatement) throws SQLException {
    return prepareStatement(sqlStatement, null);
  }

  private PreparedStatement prepareStatement(final String sqlStatement, final String[] returnColumns) throws SQLException {
    try {
      logAccess("prepareStatement", new Object[] {sqlStatement});
      if (returnColumns == null) {
        return connection.getConnection().prepareStatement(sqlStatement);
      }

      return connection.getConnection().prepareStatement(sqlStatement, returnColumns);
    }
    finally {
      logExit("prepareStatement", null, null);
    }
  }

  private static String[] getPrimaryKeyColumnNames(final Entity.Definition entityDefinition) {
    final List<ColumnProperty> primaryKeyProperties = entityDefinition.getPrimaryKeyProperties();
    final String[] columnNames = new String[primaryKeyProperties.size()];
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      columnNames[i] = primaryKeyProperties.get(i).getColumnName();
    }

    return columnNames;
  }

  /**
   * @param entityId the entityId
   * @return all foreign keys in the domain referencing entities of type {@code entityId}
   */
  private Collection<ForeignKeyProperty> getForeignKeyReferences(final String entityId) {
    return foreignKeyReferenceMap.computeIfAbsent(entityId, e -> {
      final List<ForeignKeyProperty> foreignKeyReferences = new ArrayList<>();
      for (final Entity.Definition entityDefinition : domain.getEntityDefinitions()) {
        for (final ForeignKeyProperty foreignKeyProperty : entityDefinition.getForeignKeyProperties()) {
          if (foreignKeyProperty.getForeignEntityId().equals(entityId)) {
            foreignKeyReferences.add(foreignKeyProperty);
          }
        }
      }

      return foreignKeyReferences;
    });
  }

  private List<Entity> packResult(final ResultIterator<Entity> iterator) throws SQLException {
    SQLException packingException = null;
    final List<Entity> result = new ArrayList<>();
    try {
      logAccess("packResult", null);
      while (iterator.hasNext()) {
        result.add(iterator.next());
      }

      return result;
    }
    catch (final SQLException e) {
      packingException = e;
      throw e;
    }
    finally {
      logExit("packResult", packingException, "row count: " + result.size());
    }
  }

  private List<ColumnProperty> getInsertableProperties(final Entity.Definition entityDefinition,
                                                       final boolean includePrimaryKeyProperties) {
    return insertProperties.computeIfAbsent(entityDefinition.getEntityId(), entityId ->
            entityDefinition.getWritableColumnProperties(includePrimaryKeyProperties, true));
  }

  private List<ColumnProperty> getUpdatableProperties(final Entity.Definition entityDefinition) {
    return updateProperties.computeIfAbsent(entityDefinition.getEntityId(), entityId ->
            entityDefinition.getWritableColumnProperties(true, false));
  }

  private String[] getWritableColumnPropertyIds(final String entityId) {
    return writableColumnPropertyIds.computeIfAbsent(entityId, e ->
            getEntityDefinition(entityId).getWritableColumnProperties(true, true)
                    .stream().map(Property::getPropertyId).toArray(String[]::new));
  }

  private DatabaseException translateInsertUpdateSQLException(final SQLException exception) {
    final Database database = connection.getDatabase();
    if (database.isUniqueConstraintException(exception)) {
      return new UniqueConstraintException(exception, database.getErrorMessage(exception));
    }
    else if (database.isReferentialIntegrityException(exception)) {
      return new ReferentialIntegrityException(exception, database.getErrorMessage(exception));
    }

    return new DatabaseException(exception, database.getErrorMessage(exception));
  }

  private DatabaseException translateDeleteSQLException(final SQLException exception) {
    final Database database = connection.getDatabase();
    if (database.isReferentialIntegrityException(exception)) {
      return new ReferentialIntegrityException(exception, database.getErrorMessage(exception));
    }

    return new DatabaseException(exception, database.getErrorMessage(exception));
  }

  private void commitQuietly() {
    try {
      connection.commit();
    }
    catch (final SQLException e) {
      LOG.error("Exception while performing a quiet commit", e);
    }
  }

  private void rollbackQuietly() {
    try {
      connection.rollback();
    }
    catch (final SQLException e) {
      LOG.error("Exception while performing a quiet rollback", e);
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
    final MethodLogger methodLogger = connection.getMethodLogger();
    if (methodLogger != null && methodLogger.isEnabled()) {
      return methodLogger.logExit(method, exception, exitMessage);
    }

    return null;
  }

  private void logAccess(final String method, final Object[] arguments) {
    final MethodLogger methodLogger = connection.getMethodLogger();
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method, arguments);
    }
  }

  private static void setParameterValues(final PreparedStatement statement, final List<ColumnProperty> statementProperties,
                                         final List statementValues) throws SQLException {
    if (nullOrEmpty(statementValues) || statement.getParameterMetaData().getParameterCount() == 0) {
      return;
    }
    if (statementProperties == null || statementProperties.size() != statementValues.size()) {
      throw new SQLException("Parameter property value count mismatch: " + (statementProperties == null ?
              "no properties" : ("expected: " + statementValues.size() + ", got: " + statementProperties.size())));
    }

    for (int i = 0; i < statementProperties.size(); i++) {
      setParameterValue(statement, i + 1, statementValues.get(i), statementProperties.get(i));
    }
  }

  private static void setParameterValue(final PreparedStatement statement, final int parameterIndex,
                                        final Object value, final ColumnProperty property) throws SQLException {
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

  private static List<Entity.Key> getReferencedKeys(final List<Entity> entities,
                                                    final ForeignKeyProperty foreignKeyProperty) {
    final Set<Entity.Key> keySet = new HashSet<>(entities.size());
    for (int i = 0; i < entities.size(); i++) {
      final Entity.Key key = entities.get(i).getReferencedKey(foreignKeyProperty);
      if (key != null) {
        keySet.add(key);
      }
    }

    return new ArrayList<>(keySet);
  }

  private String createSelectSQL(final WhereCondition whereCondition, final List<ColumnProperty> columnProperties,
                                 final Entity.Definition entityDefinition) {
    final EntityCondition entityCondition = whereCondition.getEntityCondition();
    final boolean isForUpdate = entityCondition instanceof EntitySelectCondition &&
            ((EntitySelectCondition) entityCondition).isForUpdate();
    boolean containsWhereClause = false;
    String selectQuery = entityDefinition.getSelectQuery();
    if (selectQuery == null) {
      selectQuery = createSelectSQL(isForUpdate ? entityDefinition.getTableName() : entityDefinition.getSelectTableName(),
              initializeSelectColumnsString(columnProperties), null, null);
    }
    else {
      containsWhereClause = entityDefinition.selectQueryContainsWhereClause();
    }

    final StringBuilder queryBuilder = new StringBuilder(selectQuery);
    final String whereClause = whereCondition.getWhereClause();
    if (whereClause.length() > 0) {
      queryBuilder.append(containsWhereClause ? " and " : WHERE_SPACE_PREFIX_POSTFIX).append(whereClause);
    }
    if (isForUpdate) {
      addForUpdate(queryBuilder, connection.getDatabase());
    }
    else {
      addGroupHavingOrderByAndLimitClauses(queryBuilder, entityCondition, entityDefinition);
    }

    return queryBuilder.toString();
  }

  private static String initializeSelectColumnsString(final List<ColumnProperty> columnProperties) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < columnProperties.size(); i++) {
      final ColumnProperty property = columnProperties.get(i);
      if (property instanceof SubqueryProperty) {
        stringBuilder.append("(").append(((SubqueryProperty) property).getSubQuery())
                .append(") as ").append(property.getColumnName());
      }
      else {
        stringBuilder.append(property.getColumnName());
      }

      if (i < columnProperties.size() - 1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
  }

  private static void addForUpdate(final StringBuilder queryBuilder, final Database database) {
    if (database.supportsSelectForUpdate()) {
      queryBuilder.append(" for update");
      if (database.supportsNowait()) {
        queryBuilder.append(" nowait");
      }
    }
  }

  private static void addGroupHavingOrderByAndLimitClauses(final StringBuilder queryBuilder,
                                                           final EntityCondition condition,
                                                           final Entity.Definition entityDefinition) {
    final String groupByClause = entityDefinition.getGroupByClause();
    if (groupByClause != null) {
      queryBuilder.append(" group by ").append(groupByClause);
    }
    final String havingClause = entityDefinition.getHavingClause();
    if (havingClause != null) {
      queryBuilder.append(" having ").append(havingClause);
    }
    if (condition instanceof EntitySelectCondition) {
      final EntitySelectCondition selectCondition = (EntitySelectCondition) condition;
      final String orderByClause = getOrderByClause(selectCondition, entityDefinition);
      if (orderByClause != null) {
        queryBuilder.append(" order by ").append(orderByClause);
      }
      if (selectCondition.getLimit() > 0) {
        queryBuilder.append(" limit ").append(selectCondition.getLimit());
        if (selectCondition.getOffset() > 0) {
          queryBuilder.append(" offset ").append(selectCondition.getOffset());
        }
      }
    }
  }

  private static String getOrderByClause(final EntitySelectCondition selectCondition,
                                         final Entity.Definition entityDefinition) {
    if (selectCondition.getOrderBy() == null) {
      return null;
    }

    final List<String> orderBys = new LinkedList<>();
    final List<Entity.OrderBy.OrderByProperty> orderByProperties = selectCondition.getOrderBy().getOrderByProperties();
    for (int i = 0; i < orderByProperties.size(); i++) {
      final Entity.OrderBy.OrderByProperty property = orderByProperties.get(i);
      orderBys.add(entityDefinition.getColumnProperty(property.getPropertyId()).getColumnName() +
              (property.isDescending() ? " desc" : ""));
    }

    return String.join(", ", orderBys);
  }

  /**
   * @param tableName the table name
   * @param updateProperties the properties being updated
   * @param whereClause the where clause, without the WHERE keyword
   * @return a query for updating this entity instance
   */
  private static String createUpdateSQL(final String tableName, final List<ColumnProperty> updateProperties,
                                        final String whereClause) {
    final StringBuilder sql = new StringBuilder("update ").append(tableName).append(" set ");
    for (int i = 0; i < updateProperties.size(); i++) {
      sql.append(updateProperties.get(i).getColumnName()).append(" = ?");
      if (i < updateProperties.size() - 1) {
        sql.append(", ");
      }
    }

    return sql.append(WHERE_SPACE_PREFIX_POSTFIX).append(whereClause).toString();
  }

  /**
   * @param tableName the table name
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting this entity instance
   */
  private static String createInsertSQL(final String tableName, final List<ColumnProperty> insertProperties) {
    final StringBuilder sql = new StringBuilder("insert ").append("into ").append(tableName).append("(");
    final StringBuilder columnValues = new StringBuilder(") values(");
    for (int i = 0; i < insertProperties.size(); i++) {
      sql.append(insertProperties.get(i).getColumnName());
      columnValues.append("?");
      if (i < insertProperties.size() - 1) {
        sql.append(", ");
        columnValues.append(", ");
      }
    }

    return sql.append(columnValues).append(")").toString();
  }

  /**
   * @param tableName the table name
   * @param whereClause the where clause
   * @return a query for deleting the entities specified by the given condition
   */
  private static String createDeleteSQL(final String tableName, final String whereClause) {
    return "delete from " + tableName + (whereClause.isEmpty() ? "" : WHERE_SPACE_PREFIX_POSTFIX + whereClause);
  }

  /**
   * Generates a sql select query with the given parameters
   * @param tableName the name of the table from which to select
   * @param columns the columns to select, example: "col1, col2"
   * @param whereClause the where condition without the WHERE keyword
   * @param orderByClause a string specifying the columns 'ORDER BY' clause,
   * "col1, col2" as input results in the following order by clause "order by col1, col2"
   * @return the generated sql query
   */
  private static String createSelectSQL(final String tableName, final String columns, final String whereClause,
                                        final String orderByClause) {
    final StringBuilder sql = new StringBuilder("select ").append(columns).append(" from ").append(tableName);
    if (!nullOrEmpty(whereClause)) {
      sql.append(WHERE_SPACE_PREFIX_POSTFIX).append(whereClause);
    }
    if (!nullOrEmpty(orderByClause)) {
      sql.append(" order by ").append(orderByClause);
    }

    return sql.toString();
  }

  /**
   * @param inserting if true then all properties with values available in {@code entity} are added,
   * otherwise update is assumed and only properties with modified values are added.
   * @param entity the Entity instance
   * @param entityProperties the column properties the entity type is based on
   * @param statementProperties the list to populate with the properties to use in the statement
   * @param statementValues the list to populate with the values to be used in the statement
   * @throws java.sql.SQLException if no properties to populate the values for were found
   */
  private static void populateStatementPropertiesAndValues(final boolean inserting, final Entity entity,
                                                           final List<ColumnProperty> entityProperties,
                                                           final List<ColumnProperty> statementProperties,
                                                           final List<Object> statementValues) throws SQLException {
    for (int i = 0; i < entityProperties.size(); i++) {
      final ColumnProperty property = entityProperties.get(i);
      if (entity.containsKey(property) && (inserting || entity.isModified(property))) {
        statementProperties.add(property);
        statementValues.add(entity.get(property));
      }
    }
    if (statementProperties.isEmpty()) {
      if (inserting) {
        throw new SQLException("Unable to insert entity " + entity.getEntityId() + ", no properties to insert");
      }

      throw new SQLException("Unable to update entity " + entity.getEntityId() + ", no modified values found");
    }
  }

  private static String createModifiedExceptionMessage(final Entity entity, final Entity modified,
                                                       final Collection<ColumnProperty> modifiedProperties) {
    final StringBuilder builder = new StringBuilder(MESSAGES.getString(RECORD_MODIFIED_EXCEPTION)).append(", ").append(entity.getEntityId());
    for (final ColumnProperty property : modifiedProperties) {
      builder.append(" \n").append(property).append(": ").append(entity.getOriginal(property)).append(" -> ").append(modified.get(property));
    }

    return builder.toString();
  }

  private void checkReadOnly(final List<Entity> entities) throws DatabaseException {
    for (int i = 0; i < entities.size(); i++) {
      checkReadOnly(entities.get(i).getEntityId());
    }
  }

  private void checkReadOnly(final String entityId) throws DatabaseException {
    if (getEntityDefinition(entityId).isReadOnly()) {
      throw new DatabaseException("Entities of type: " + entityId + " are read only");
    }
  }

  private Entity.Definition getEntityDefinition(final String entityId) {
    return domain.getDefinition(entityId);
  }

  private static final class BlobPacker implements ResultPacker<Blob> {
    @Override
    public Blob fetch(final ResultSet resultSet) throws SQLException {
      return resultSet.getBlob(1);
    }
  }

  /**
   * A {@link MethodLogger.ArgumentStringProvider} implementation tailored for EntityConnections
   */
  static final class EntityArgumentStringProvider extends MethodLogger.DefaultArgumentStringProvider {

    private final Domain domain;

    EntityArgumentStringProvider(final Domain domain) {
      this.domain = domain;
    }

    @Override
    public String toString(final Object argument) {
      if (argument == null) {
        return "";
      }

      final StringBuilder builder = new StringBuilder();
      if (argument instanceof Object[] && ((Object[]) argument).length > 0) {
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

    private String getEntityParameterString(final Entity entity) {
      final StringBuilder builder = new StringBuilder(entity.getEntityId()).append(" {");
      final List<ColumnProperty> columnProperties = domain.getDefinition(entity.getEntityId()).getColumnProperties();
      for (int i = 0; i < columnProperties.size(); i++) {
        final ColumnProperty property = columnProperties.get(i);
        final boolean modified = entity.isModified(property);
        if (property.isPrimaryKeyProperty() || modified) {
          final StringBuilder valueString = new StringBuilder();
          if (modified) {
            valueString.append(entity.getOriginal(property)).append("->");
          }
          valueString.append(entity.get(property.getPropertyId()));
          builder.append(property.getPropertyId()).append(":").append(valueString).append(",");
        }
      }
      builder.deleteCharAt(builder.length() - 1);

      return builder.append("}").toString();
    }

    private static String getEntityKeyParameterString(final Entity.Key argument) {
      return argument.getEntityId() + ", " + argument.toString();
    }
  }

  private static final class EntityResultIterator implements ResultIterator<Entity> {

    private final Statement statement;
    private final ResultSet resultSet;
    private final ResultPacker<Entity> resultPacker;
    private final int fetchCount;
    private int counter = 0;

    private EntityResultIterator(final Statement statement, final ResultSet resultSet,
                                 final ResultPacker<Entity> resultPacker, final int fetchCount) {
      this.statement = statement;
      this.resultSet = resultSet;
      this.resultPacker = resultPacker;
      this.fetchCount = fetchCount;
    }

    @Override
    public boolean hasNext() throws SQLException {
      try {
        if ((fetchCount < 0 || counter < fetchCount) && resultSet.next()) {
          return true;
        }
        close();

        return false;
      }
      catch (final SQLException e) {
        close();
        throw e;
      }
    }

    @Override
    public Entity next() throws SQLException {
      counter++;
      try {
        return resultPacker.fetch(resultSet);
      }
      catch (final SQLException e) {
        close();
        throw e;
      }
    }

    @Override
    public void close() {
      closeSilently(resultSet);
      closeSilently(statement);
    }
  }

  /**
   * Handles packing Entity query results.
   */
  private final class EntityResultPacker implements ResultPacker<Entity> {

    private final String entityId;
    private final List<ColumnProperty> columnProperties;
    private final List<TransientProperty> transientProperties;

    private EntityResultPacker(final String entityId,
                               final List<ColumnProperty> columnProperties,
                               final List<TransientProperty> transientProperties) {
      this.entityId = entityId;
      this.columnProperties = columnProperties;
      this.transientProperties = transientProperties;
    }

    @Override
    public Entity fetch(final ResultSet resultSet) throws SQLException {
      final Map<Property, Object> values = new HashMap<>(
              columnProperties.size() + transientProperties.size());
      for (int i = 0; i < transientProperties.size(); i++) {
        final TransientProperty transientProperty = transientProperties.get(i);
        if (!(transientProperty instanceof DerivedProperty)) {
          values.put(transientProperty, null);
        }
      }
      for (int i = 0; i < columnProperties.size(); i++) {
        final ColumnProperty property = columnProperties.get(i);
        try {
          values.put(property, property.fetchValue(resultSet, i + 1));
        }
        catch (final Exception e) {
          throw new SQLException("Exception fetching: " + property + ", entity: " + entityId + " [" + e.getMessage() + "]", e);
        }
      }

      return domain.entity(entityId, values, null);
    }
  }
}