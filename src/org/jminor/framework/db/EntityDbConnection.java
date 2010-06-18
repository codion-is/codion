/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.IdSource;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntityKeyCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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
public class EntityDbConnection extends DbConnection implements EntityDb {

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

  public EntityDbConnection(final Connection connection, final Database database, final User user) throws SQLException {
    super(database, user, connection);
  }

  public boolean isOptimisticLocking() {
    return optimisticLocking;
  }

  public void setOptimisticLocking(final boolean optimisticLocking) {
    this.optimisticLocking = optimisticLocking;
  }

  public boolean isLimitForeignKeyFetchDepth() {
    return limitForeignKeyFetchDepth;
  }

  public void setLimitForeignKeyFetchDepth(final boolean limitForeignKeyFetchDepth) {
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
  }

  /** {@inheritDoc} */
  public List<Entity.Key> insert(final List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0) {
      return new ArrayList<Entity.Key>();
    }

    final List<Entity.Key> keys = new ArrayList<Entity.Key>(entities.size());
    PreparedStatement statement = null;
    String insertQuery = null;
    try {
      final List<Property> insertProperties = new ArrayList<Property>();
      final List<Object> statementValues = new ArrayList<Object>();
      for (final Entity entity : entities) {
        if (EntityRepository.isReadOnly(entity.getEntityID())) {
          throw new DbException("Can not insert a read only entity: " + entity.getEntityID());
        }

        final IdSource idSource = EntityRepository.getIdSource(entity.getEntityID());
        final Property.PrimaryKeyProperty firstPrimaryKeyProperty = entity.getPrimaryKey().getFirstKeyProperty();
        if (idSource.isQueried() && entity.getPrimaryKey().isNull()) {
          entity.setValue(firstPrimaryKeyProperty, queryNewIdValue(entity.getEntityID(), idSource, firstPrimaryKeyProperty));
        }

        addInsertProperties(entity, insertProperties);
        insertQuery = getInsertSQL(entity.getEntityID(), insertProperties);
        statement = getConnection().prepareStatement(insertQuery);
        for (final Property property : insertProperties) {
          statementValues.add(entity.getValue(property));
        }

        executePreparedUpdate(statement, insertQuery, statementValues, insertProperties);

        if (idSource.isAutoIncrement() && entity.getPrimaryKey().isNull()) {
          entity.setValue(firstPrimaryKeyProperty, queryInteger(getDatabase().getAutoIncrementValueSQL(
                  EntityRepository.getEntityIdSource(entity.getEntityID()))));
        }

        keys.add(entity.getPrimaryKey());

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
      cleanup(statement, null);
    }
  }

  /** {@inheritDoc} */
  public List<Entity> update(List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0) {
      return entities;
    }

    String updateQuery = null;
    PreparedStatement statement = null;
    try {
      final Map<String, Collection<Entity>> hashedEntities = EntityUtil.hashByEntityID(entities);
      for (final String entityID : hashedEntities.keySet()) {
        if (EntityRepository.isReadOnly(entityID)) {
          throw new DbException("Can not update a read only entity: " + entityID);
        }
      }

      final List<Object> statementValues = new ArrayList<Object>();
      final List<Property> statementProperties = new ArrayList<Property>();
      for (final Map.Entry<String, Collection<Entity>> entry : hashedEntities.entrySet()) {
        final List<Property.PrimaryKeyProperty> primaryKeyProperties = EntityRepository.getPrimaryKeyProperties(entry.getKey());
        for (final Entity entity : entry.getValue()) {
          if (!entity.isModified()) {
            throw new DbException("Can not update an unmodified entity: " + entity);
          }
          if (isOptimisticLocking()) {
            checkIfModified(entity);
          }

          addUpdateProperties(entity, statementProperties);
          updateQuery = getUpdateSQL(entity, statementProperties, primaryKeyProperties);
          for (final Property property : statementProperties) {
            statementValues.add(entity.getValue(property.getPropertyID()));
          }
          statementProperties.addAll(primaryKeyProperties);
          for (final Property.PrimaryKeyProperty primaryKeyProperty : primaryKeyProperties) {
            statementValues.add(entity.getPrimaryKey().getOriginalValue(primaryKeyProperty.getPropertyID()));
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
    catch (SQLException e) {
      if (!isTransactionOpen()) {
        rollbackQuietly();
      }
      LOG.error(this, e);
      throw new DbException(e, updateQuery, getDatabase().getErrorMessage(e));
    }
    finally {
      cleanup(statement, null);
    }
  }

  /** {@inheritDoc} */
  public void delete(final EntityCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    String deleteQuery = null;
    try {
      if (EntityRepository.isReadOnly(criteria.getEntityID())) {
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
      cleanup(statement, null);
    }
  }

  /** {@inheritDoc} */
  public void delete(final List<Entity.Key> entities) throws DbException {
    if (entities == null || entities.size() == 0) {
      return;
    }

    PreparedStatement statement = null;
    String deleteQuery = null;
    try {
      final Map<String, Collection<Entity.Key>> hashedEntities = EntityUtil.hashKeysByEntityID(entities);
      for (final String entityID : hashedEntities.keySet()) {
        if (EntityRepository.isReadOnly(entityID)) {
          throw new DbException("Can not delete a read only entity: " + entityID);
        }
      }

      for (final Map.Entry<String, Collection<Entity.Key>> entry : hashedEntities.entrySet()) {
        final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(new ArrayList<Entity.Key>(entry.getValue()));
        deleteQuery = "delete from " + EntityRepository.getTableName(entry.getKey()) + " " + criteria.getWhereClause();
        statement = getConnection().prepareStatement(deleteQuery);
        executePreparedUpdate(statement, deleteQuery, criteria.getValues(), criteria.getValueProperties());
        statement.close();
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
      cleanup(statement, null);
    }
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DbException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, value));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final Entity.Key primaryKey) throws DbException {
    return selectSingle(EntityCriteriaUtil.selectCriteria(primaryKey));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final EntitySelectCriteria criteria) throws DbException {
    final List<Entity> entities = selectMany(criteria);
    if (entities.size() == 0) {
      throw new RecordNotFoundException(FrameworkMessages.get(FrameworkMessages.RECORD_NOT_FOUND));
    }
    if (entities.size() > 1) {
      throw new DbException(FrameworkMessages.get(FrameworkMessages.MANY_RECORDS_FOUND));
    }

    return entities.get(0);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectMany(final List<Entity.Key> primaryKeys) throws DbException {
    if (primaryKeys == null || primaryKeys.size() == 0) {
      return new ArrayList<Entity>(0);
    }

    return selectMany(EntityCriteriaUtil.selectCriteria(primaryKeys));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final String entityID, final String propertyID, final Object... values) throws DbException {
    return selectMany(EntityCriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, values));
  }

  /** {@inheritDoc} */
  public List<Entity> selectAll(final String entityID) throws DbException {
    return selectMany(new EntitySelectCriteria(entityID, null, EntityRepository.getOrderByClause(entityID)));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DbException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = null;
    try {
      selectQuery = initializeSelectQuery(criteria, EntityRepository.getSelectColumnsString(criteria.getEntityID()), criteria.getOrderByClause());
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
      cleanup(statement, resultSet);
    }
  }

  /** {@inheritDoc} */
  public List<Object> selectPropertyValues(final String entityID, final String propertyID, final boolean order) throws DbException {
    String sql = null;
    try {
      if (EntityRepository.getSelectQuery(entityID) != null) {
        throw new RuntimeException("selectPropertyValues is not implemented for entities with custom select queries");
      }

      final Property property = EntityRepository.getProperty(entityID, propertyID);
      final String columnName = property.getColumnName();
      sql = getSelectSQL(EntityRepository.getSelectTableName(entityID),
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
      selectQuery = EntityRepository.getSelectQuery(criteria.getEntityID());
      selectQuery = getSelectSQL(selectQuery == null ? EntityRepository.getSelectTableName(criteria.getEntityID()) :
              "(" + selectQuery + " " + criteria.getWhereClause(!selectQuery.toLowerCase().contains("where")) + ") alias", "count(*)", null, null);
      selectQuery += " " + criteria.getWhereClause(!containsWhereKeyword(selectQuery));
      statement = getConnection().prepareStatement(selectQuery);
      resultSet = executePreparedSelect(statement, selectQuery, criteria.getValues(), criteria.getValueProperties());
      final List<Integer> result = INT_PACKER.pack(resultSet, -1);

      if (result.size() == 0) {
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
      cleanup(statement, resultSet);
    }
  }

  /** {@inheritDoc} */
  public Map<String, List<Entity>> selectDependentEntities(final List<Entity> entities) throws DbException {
    final Map<String, List<Entity>> dependencyMap = new HashMap<String, List<Entity>>();
    if (entities == null || entities.size() == 0) {
      return dependencyMap;
    }

    final Set<Dependency> dependencies = resolveEntityDependencies(entities.get(0).getEntityID());
    for (final Dependency dependency : dependencies) {
      final List<Entity> dependentEntities = selectMany(new EntitySelectCriteria(dependency.entityID,
              new EntityKeyCriteria(dependency.foreignKeyProperties, EntityUtil.getPrimaryKeys(entities))));
      if (dependentEntities.size() > 0) {
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
  public ReportResult fillReport(final ReportWrapper reportWrapper, final Map reportParameters) throws ReportException {
    return reportWrapper.fillReport(reportParameters, getConnection());
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
                (Property.BlobProperty) EntityRepository.getProperty(primaryKey.getEntityID(), blobPropertyID);

        final String whereCondition = getWhereCondition(primaryKey.getProperties());

        execute(new StringBuilder("update ").append(primaryKey.getEntityID()).append(" set ").append(property.getColumnName())
                .append(" = '").append(dataDescription).append("' where ").append(whereCondition).toString());

        writeBlobField(blobData, EntityRepository.getTableName(primaryKey.getEntityID()),
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
              (Property.BlobProperty) EntityRepository.getProperty(primaryKey.getEntityID(), blobPropertyID);

      return readBlobField(EntityRepository.getTableName(primaryKey.getEntityID()), property.getBlobColumnName(),
              getWhereCondition(primaryKey.getProperties()));
    }
    catch (SQLException exception) {
      throw new DbException(exception, null, getDatabase().getErrorMessage(exception));
    }
  }

  protected EntityResultPacker getEntityResultPacker(final String entityID) {
    EntityResultPacker packer = entityResultPackers.get(entityID);
    if (packer == null) {
      entityResultPackers.put(entityID, packer = new EntityResultPacker(entityID,
              EntityRepository.getDatabaseProperties(entityID), EntityRepository.getTransientProperties(entityID)));
    }

    return packer;
  }

  protected ResultPacker getPropertyResultPacker(final Property property) {
    ResultPacker packer = propertyResultPackers.get(property.getType());
    if (packer == null) {
      propertyResultPackers.put(property.getType(), packer = new ResultPacker() {
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
      });
    }

    return packer;
  }

  /**
   * Checks if the given entity has been modified by comparing the property values to the values in the database
   * @param entity the entity to check
   * @throws DbException in case of a database exception
   * @throws RecordNotFoundException in case the entity has been deleted
   * @throws RecordModifiedException in case the entity has been modified
   */
  protected void checkIfModified(final Entity entity) throws DbException {
    final Entity current = selectSingle(EntityCriteriaUtil.selectCriteria(
            entity.getPrimaryKey()).setFetchDepthForAll(0));

    if (!current.getPrimaryKey().equals(entity.getPrimaryKey().getOriginalCopy())) {
      throw new RecordModifiedException(entity, current);
    }

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
  protected void setForeignKeyValues(final List<Entity> entities, final EntitySelectCriteria criteria) throws DbException {
    if (entities == null || entities.size() == 0) {
      return;
    }
    //Any sufficiently complex algorithm is indistinguishable from evil
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = EntityRepository.getForeignKeyProperties(entities.get(0).getEntityID());
    for (final Property.ForeignKeyProperty property : foreignKeyProperties) {
      final int maxFetchDepth = criteria.getCurrentFetchDepth() == 0 ? criteria.getFetchDepth(property.getPropertyID()) : criteria.getFetchDepth();
      if (!limitForeignKeyFetchDepth || criteria.getCurrentFetchDepth() < maxFetchDepth) {
        final List<Entity.Key> referencedKeys = getReferencedPrimaryKeys(entities, property);
        if (referencedKeys.size() > 0) {
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

  protected int queryNewIdValue(final String entityID, final IdSource idSource, final Property.PrimaryKeyProperty primaryKeyProperty) throws DbException {
    String sql;
    switch (idSource) {
      case MAX_PLUS_ONE:
        sql = new StringBuilder("select max(").append(primaryKeyProperty.getColumnName())
                .append(") + 1 from ").append(EntityRepository.getTableName(entityID)).toString();
        break;
      case QUERY:
        sql = EntityRepository.getEntityIdSource(entityID);
        break;
      case SEQUENCE:
        sql = getDatabase().getSequenceSQL(EntityRepository.getEntityIdSource(entityID));
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
                                     final List<?> values, final List<? extends Property> properties) throws SQLException {
    SQLException exception = null;
    try {
      QUERY_COUNTER.count(sqlStatement);
      getMethodLogger().logAccess("executePreparedUpdate", new Object[] {sqlStatement, values});
      setParameterValues(statement, values, properties);
      statement.executeUpdate();
      return;
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      getMethodLogger().logExit("executePreparedUpdate", exception, null);
    }

    throw exception;
  }

  private ResultSet executePreparedSelect(final PreparedStatement statement, final String sqlStatement,
                                          final List<?> values, final List<Property> properties) throws SQLException {
    SQLException exception = null;
    try {
      QUERY_COUNTER.count(sqlStatement);
      getMethodLogger().logAccess("executePreparedSelect", values == null ? new Object[] {sqlStatement} : new Object[] {sqlStatement, values});
      setParameterValues(statement, values, properties);
      return statement.executeQuery();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      getMethodLogger().logExit("executePreparedSelect", exception, null);
    }

    throw exception;
  }

  private void rollbackQuietly() {
    try {
      rollback();
    }
    catch (SQLException e) {/**/}
  }

  private static void setParameterValues(final PreparedStatement statement, final List<?> values,
                                         final List<? extends Property> parameterProperties) throws SQLException {
    if (values == null || values.size() == 0 || statement.getParameterMetaData().getParameterCount() == 0) {
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
      System.out.println("Unable to set parameter: " + property + ", value: " + value + ", value class: " + (value == null ? "null" : value.getClass()));
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

  private static void cleanup(final Statement statement, final ResultSet resultSet) {
    try {
      if (resultSet != null) {
        resultSet.close();
      }
    }
    catch (SQLException e) {/**/}
    try {
      if (statement != null) {
        statement.close();
      }
    }
    catch (SQLException e) {/**/}
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

  private static String initializeSelectQuery(final EntityCriteria criteria, final String columnsString, final String orderByClause) {
    String selectQuery = EntityRepository.getSelectQuery(criteria.getEntityID());
    if (selectQuery == null) {
      selectQuery = getSelectSQL(EntityRepository.getSelectTableName(criteria.getEntityID()), columnsString, null, null);
    }

    final StringBuilder queryBuilder = new StringBuilder(selectQuery);
    final String whereClause = criteria.getWhereClause(!containsWhereKeyword(selectQuery));
    if (whereClause.length() > 0) {
      queryBuilder.append(" ").append(whereClause);
    }
    if (orderByClause != null) {
      queryBuilder.append(" order by ").append(orderByClause);
    }

    return queryBuilder.toString();
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
  private static String getUpdateSQL(final Entity entity, final Collection<Property> properties,
                                     final List<Property.PrimaryKeyProperty> primaryKeyProperties) throws DbException {
    if (!entity.isModified()) {
      throw new DbException("Can not get update sql for an unmodified entity");
    }

    final StringBuilder sql = new StringBuilder("update ");
    sql.append(EntityRepository.getTableName(entity.getEntityID())).append(" set ");
    int columnIndex = 0;
    for (final Property property : properties) {
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
  private static String getInsertSQL(final String entityID, final Collection<Property> insertProperties) {
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
    return new StringBuilder("delete from ").append(EntityRepository.getTableName(criteria.getEntityID())).append(" ")
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
    if (whereCondition != null && whereCondition.length() > 0) {
      sql.append(" ").append(whereCondition);
    }
    if (orderByClause != null && orderByClause.length() > 0) {
      sql.append(" order by ");
      sql.append(orderByClause);
    }

    return sql.toString();
  }

  /**
   * Returns the properties used when inserting an instance of this entity, leaving out properties with null values
   * @param entity the entity
   * @param properties the properties are added to this collection
   */
  private static void addInsertProperties(final Entity entity, final Collection<Property> properties) {
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(),
            EntityRepository.getIdSource(entity.getEntityID()) != IdSource.AUTO_INCREMENT, false, true, false, false)) {
      if (!entity.isValueNull(property.getPropertyID())) {
        properties.add(property);
      }
    }
  }

  /**
   * @param entity the Entity instance
   * @param properties the collection to add the properties to
   * @return the properties used to update this entity, properties that have had their values modified that is
   */
  private static Collection<Property> addUpdateProperties(final Entity entity, final Collection<Property> properties) {
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, false, false, false)) {
      if (entity.isModified(property.getPropertyID())) {
        properties.add(property);
      }
    }

    return properties;
  }

  private static Set<Dependency> resolveEntityDependencies(final String entityID) {
    final Collection<String> entityIDs = EntityRepository.getDefinedEntities();
    final Set<Dependency> dependencies = new HashSet<Dependency>();
    for (final String entityIDToCheck : entityIDs) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(entityIDToCheck)) {
        if (foreignKeyProperty.getReferencedEntityID().equals(entityID)) {
          dependencies.add(new Dependency(entityIDToCheck, foreignKeyProperty.getReferenceProperties()));
        }
      }
    }

    return dependencies;
  }

  private static class Dependency {
    final String entityID;
    final List<Property> foreignKeyProperties;

    public Dependency(final String entityID, final List<Property> foreignKeyProperties) {
      this.entityID = entityID;
      this.foreignKeyProperties = foreignKeyProperties;
    }
  }
}