/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbException;
import org.jminor.common.db.IdSource;
import org.jminor.common.db.RecordNotFoundException;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.CriteriaUtil;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntityKeyCriteria;
import org.jminor.framework.db.criteria.SelectCriteria;
import org.jminor.framework.db.exception.EntityModifiedException;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;
import org.jminor.framework.i18n.FrameworkMessages;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the database layer accessible to the client
 */
public class EntityDbConnection extends DbConnection implements EntityDb {

  private static final Logger log = Util.getLogger(EntityDbConnection.class);

  private final Map<String, EntityResultPacker> entityResultPackers = new HashMap<String, EntityResultPacker>();
  private final Map<Type, ResultPacker> propertyResultPackers = new HashMap<Type, ResultPacker>();
  private boolean optimisticLocking = (Boolean) Configuration.getValue(Configuration.USE_OPTIMISTIC_LOCKING);

  /**Used by the EntityDbConnectionPool class*/
  long poolTime = -1;

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

  public boolean isOptimisticLocking() {
    return optimisticLocking;
  }

  public void setOptimisticLocking(final boolean optimisticLocking) {
    this.optimisticLocking = optimisticLocking;
  }

  /** {@inheritDoc} */
  public List<Entity.Key> insert(final List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return new ArrayList<Entity.Key>();

    final List<Entity.Key> keys = new ArrayList<Entity.Key>(entities.size());
    String sql = null;//so we can include it in the exception
    try {
      for (final Entity entity : entities) {
        final String entityID = entity.getEntityID();
        if (EntityRepository.isReadOnly(entityID))
          throw new DbException("Can not insert a read only entity");

        final IdSource idSource = EntityRepository.getIdSource(entityID);
        if (idSource.isQueried() && entity.getPrimaryKey().isNull())
          entity.setValue(entity.getPrimaryKey().getFirstKeyProperty(), queryNextIdValue(entityID, idSource), false);

        execute(sql = getInsertSQL(getDatabase(), entity));

        if (idSource.isAutoIncrement() && entity.getPrimaryKey().isNull())
          entity.setValue(entity.getPrimaryKey().getFirstKeyProperty(),
                  queryInteger(getDatabase().getAutoIncrementValueSQL(
                          EntityRepository.getEntityIdSource(entityID))), false);

        keys.add(entity.getPrimaryKey());
      }

      if (!isTransactionOpen())
        commit();

      return keys;
    }
    catch (SQLException e) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.error(this, ex);
      }
      throw new DbException(e, sql, getDatabase().getErrorMessage(e));
    }
  }

  /** {@inheritDoc} */
  public List<Entity> update(final List<Entity> entities) throws DbException, EntityModifiedException {
    if (entities.size() == 0)
      return entities;

    final List<String> statements = new ArrayList<String>(entities.size());
    for (final Entity entity : entities) {
      if (EntityRepository.isReadOnly(entity.getEntityID()))
        throw new DbException("Can not update a read only entity");
      if (!entity.isModified())
        throw new DbException("Trying to update non-modified entity: " + entity);
      if (optimisticLocking)
        checkIfModified(entity);

      statements.add(getUpdateSQL(getDatabase(), entity));
    }

    execute(statements);

    return selectMany(EntityUtil.getPrimaryKeys(entities));
  }

  /** {@inheritDoc} */
  public void delete(final List<Entity.Key> entityKeys) throws DbException {
    if (entityKeys == null || entityKeys.size() == 0)
      return;

    final List<String> statements = new ArrayList<String>(entityKeys.size());
    for (final Entity.Key entityKey : entityKeys) {
      if (EntityRepository.isReadOnly(entityKey.getEntityID()))
        throw new DbException("Can not delete a read only entity");
      statements.add(0, getDeleteSQL(getDatabase(), entityKey));
    }

    execute(statements);
  }

  /** {@inheritDoc} */
  public void delete(final EntityCriteria criteria) throws DbException {
    if (EntityRepository.isReadOnly(criteria.getEntityID()))
      throw new DbException("Can not delete a read only entity");

    execute(Arrays.asList(getDeleteSql(getDatabase(), criteria)));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DbException {
    return selectSingle(CriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, value));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final Entity.Key primaryKey) throws DbException {
    return selectSingle(CriteriaUtil.selectCriteria(primaryKey));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final SelectCriteria criteria) throws DbException {
    final List<Entity> entities = selectMany(criteria);
    if (entities.size() == 0)
      throw new RecordNotFoundException(FrameworkMessages.get(FrameworkMessages.RECORD_NOT_FOUND));
    if (entities.size() > 1)
      throw new DbException(FrameworkMessages.get(FrameworkMessages.MANY_RECORDS_FOUND));

    return entities.get(0);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectMany(final List<Entity.Key> primaryKeys) throws DbException {
    if (primaryKeys == null || primaryKeys.size() == 0)
      return new ArrayList<Entity>(0);

    return selectMany(CriteriaUtil.selectCriteria(primaryKeys));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final String entityID, final String propertyID, final Object... values) throws DbException {
    return selectMany(CriteriaUtil.selectCriteria(entityID, propertyID, SearchType.LIKE, values));
  }

  /** {@inheritDoc} */
  public List<Entity> selectAll(final String entityID) throws DbException {
    return selectMany(new SelectCriteria(entityID, null, EntityRepository.getOrderByClause(entityID)));
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectMany(final SelectCriteria criteria) throws DbException {
    addCacheQueriesRequest();

    String sql = null;
    try {
      final String datasource = EntityRepository.getSelectTableName(criteria.getEntityID());
      sql = getSelectSql(datasource, EntityRepository.getSelectColumnsString(criteria.getEntityID()),
              criteria.getWhereClause(getDatabase(), !datasource.toLowerCase().contains("where")), criteria.getOrderByClause());

      final List<Entity> result = (List<Entity>) query(sql, getEntityResultPacker(criteria.getEntityID()), criteria.getFetchCount());

      if (!lastQueryResultCached())
        setForeignKeyValues(result);

      return result;
    }
    catch (SQLException exception) {
      log.info(sql);
      log.error(this, exception);
      throw new DbException(exception, sql, getDatabase().getErrorMessage(exception));
    }
    finally {
      removeCacheQueriesRequest();
    }
  }

  /** {@inheritDoc} */
  public List<?> selectPropertyValues(final String entityID, final String columnName, final boolean order) throws DbException {
    String sql = null;
    try {
      sql = getSelectSql(EntityRepository.getSelectTableName(entityID),
              new StringBuilder("distinct ").append(columnName).toString(),
              new StringBuilder("where ").append(columnName).append(" is not null").toString(), order ? columnName : null);

      return query(sql, getPropertyResultPacker(EntityRepository.getProperty(entityID, columnName).getPropertyType()), -1);
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
    String sql = "";
    try {
      return queryInteger(sql = getSelectSql(EntityRepository.getSelectTableName(criteria.getEntityID()), "count(*)",
              criteria.getWhereClause(getDatabase()), null));
    }
    catch (SQLException exception) {
      throw new DbException(exception, sql, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public Map<String, List<Entity>> selectDependentEntities(final List<Entity> entities) throws DbException {
    final Map<String, List<Entity>> dependencyMap = new HashMap<String, List<Entity>>();
    if (entities == null || entities.size() == 0)
      return dependencyMap;

    try {
      addCacheQueriesRequest();
      final Set<Dependency> dependencies = resolveEntityDependencies(entities.get(0).getEntityID());
      for (final Dependency dependency : dependencies) {
        final List<Entity> dependentEntities = selectMany(new SelectCriteria(dependency.entityID,
                new EntityKeyCriteria(dependency.foreignKeyProperties, EntityUtil.getPrimaryKeys(entities))));
        if (dependentEntities.size() > 0)
          dependencyMap.put(dependency.entityID, dependentEntities);
      }

      return dependencyMap;
    }
    finally {
      removeCacheQueriesRequest();
    }
  }

  /** {@inheritDoc} */
  public void executeStatement(final String statement) throws DbException {
    try {
      execute(statement);
      if (!isTransactionOpen())
        commit();
    }
    catch (SQLException exception) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.info(statement);
        log.error(this, ex);
      }
      throw new DbException(exception, statement, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public Object executeStatement(final String statement, final int outParameterType) throws DbException {
    try {
      final Object result = executeCallableStatement(statement, outParameterType);
      if (!isTransactionOpen())
        commit();

      return result;
    }
    catch (SQLException exception) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.info(statement);
        log.error(this, ex);
      }
      throw new DbException(exception, statement, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public JasperPrint fillReport(final JasperReport report, final Map reportParameters) throws JRException {
    return JasperFillManager.fillReport(report, reportParameters, getConnection());
  }

  /** {@inheritDoc} */
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final String dataDescription,
                        final byte[] blobData) throws DbException {
    if (isTransactionOpen())
      throw new DbException("Can not save blob within an open transaction");

    try {
      boolean success = false;
      try {
        beginTransaction();
        final Property.BlobProperty property =
                (Property.BlobProperty) EntityRepository.getProperty(primaryKey.getEntityID(), blobPropertyID);

        final String whereCondition = EntityUtil.getWhereCondition(getDatabase(), primaryKey);

        execute(new StringBuilder("update ").append(primaryKey.getEntityID()).append(" set ").append(property.getColumnName())
                .append(" = '").append(dataDescription).append("' ").append(whereCondition).toString());

        writeBlobField(blobData, EntityRepository.getTableName(primaryKey.getEntityID()),
                property.getBlobColumnName(), whereCondition);
        success = true;
      }
      finally {
        if (success)
          commitTransaction();
        else
          rollbackTransaction();
      }
    }
    catch (SQLException exception) {
      throw new DbException(exception, null, getDatabase().getErrorMessage(exception));
    }
  }

  /** {@inheritDoc} */
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws Exception {
    try {
      final Property.BlobProperty property =
              (Property.BlobProperty) EntityRepository.getProperty(primaryKey.getEntityID(), blobPropertyID);

      return readBlobField(EntityRepository.getTableName(primaryKey.getEntityID()), property.getBlobColumnName(),
              EntityUtil.getWhereCondition(getDatabase(), primaryKey));
    }
    catch (SQLException exception) {
      throw new DbException(exception, null, getDatabase().getErrorMessage(exception));
    }
  }

  /**
   * @param database the Database instance
   * @param entity the Entity instance
   * @return a query for inserting this entity instance
   */
  static String getInsertSQL(final Database database, final Entity entity) {
    final StringBuilder sql = new StringBuilder("insert into ");
    sql.append(EntityRepository.getTableName(entity.getEntityID())).append("(");
    final StringBuilder columnValues = new StringBuilder(") values(");
    final Collection<Property> insertProperties = EntityUtil.getInsertProperties(entity);
    int columnIndex = 0;
    for (final Property property : insertProperties) {
      sql.append(property.getColumnName());
      columnValues.append(EntityUtil.getSQLStringValue(database, property, entity.getValue(property.getPropertyID())));
      if (columnIndex++ < insertProperties.size()-1) {
        sql.append(", ");
        columnValues.append(", ");
      }
    }

    return sql.append(columnValues).append(")").toString();
  }

  /**
   * @param database the Database instance
   * @param entity the Entity instance
   * @return a query for updating this entity instance
   * @throws DbException in case the entity is unmodified or it contains no modified updatable properties
   */
  static String getUpdateSQL(final Database database, final Entity entity) throws DbException {
    if (!entity.isModified())
      throw new DbException("Can not get update sql for an unmodified entity");

    final StringBuilder sql = new StringBuilder("update ");
    sql.append(EntityRepository.getTableName(entity.getEntityID())).append(" set ");
    final Collection<Property> properties = EntityUtil.getUpdateProperties(entity);
    if (properties.size() == 0)
      throw new DbException("No modified updateable properties found in entity: " + entity);
    int columnIndex = 0;
    for (final Property property : properties) {
      sql.append(property.getColumnName()).append(" = ").append(
              EntityUtil.getSQLStringValue(database, property, entity.getValue(property.getPropertyID())));
      if (columnIndex++ < properties.size() - 1)
        sql.append(", ");
    }

    return sql.append(EntityUtil.getWhereCondition(database, entity)).toString();
  }

  /**
   * @param database the Database instance
   * @param entityKey the EntityKey instance
   * @return a query for deleting the entity having the given primary key
   */
  static String getDeleteSQL(final Database database, final Entity.Key entityKey) {
    return new StringBuilder("delete from ").append(EntityRepository.getTableName(entityKey.getEntityID()))
            .append(EntityUtil.getWhereCondition(database, entityKey)).toString();
  }

  /**
   *
   * @param database the Database instance
   * @param criteria the EntityCriteria instance
   * @return a query for deleting the entities specified by the given criteria
   */
  static String getDeleteSql(final Database database, final EntityCriteria criteria) {
    return new StringBuilder("delete ").append(EntityRepository.getTableName(criteria.getEntityID())).append(" ")
            .append(criteria.getWhereClause(database)).toString();
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
  static String getSelectSql(final String table, final String columns, final String whereCondition,
                             final String orderByClause) {
    final StringBuilder sql = new StringBuilder("select ");
    sql.append(columns);
    sql.append(" from ");
    sql.append(table);
    if (whereCondition != null && whereCondition.length() > 0)
      sql.append(" ").append(whereCondition);
    if (orderByClause != null && orderByClause.length() > 0) {
      sql.append(" order by ");
      sql.append(orderByClause);
    }

    return sql.toString();
  }

  private void execute(final List<String> statements) throws DbException {
    String sql = null;
    try {
      for (final String statement : statements)
        execute(sql = statement);
      if (!isTransactionOpen())
        commit();
    }
    catch (SQLException exception) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.info(sql);
        log.error(this, ex);
      }

      throw new DbException(exception, sql, getDatabase().getErrorMessage(exception));
    }
  }

  /**
   * Checks if the given entity has been modified by comparing the property values to the values in the database
   * @param entity the entity to check
   * @throws DbException in case of a database exception
   * @throws EntityModifiedException in case the entity has been modified
   */
  private void checkIfModified(final Entity entity) throws DbException, EntityModifiedException {
    final Entity current = selectSingle(entity.getPrimaryKey());
    if (!current.propertyValuesEqual(entity.getOriginalCopy()))
      throw new EntityModifiedException(entity, current);
  }

  /**
   * @param entities the entities for which to set the reference entities
   * @throws DbException in case of a database exception
   */
  private void setForeignKeyValues(final List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return;

    for (final Property.ForeignKeyProperty foreignKeyProperty :
            EntityRepository.getForeignKeyProperties(entities.get(0).getEntityID())) {
      final List<Entity.Key> referencedPrimaryKeys = getPrimaryKeysOfEntityValues(entities, foreignKeyProperty);
      final Map<Entity.Key, Entity> hashedReferencedEntities = foreignKeyProperty.isLazyLoading()
              ? initLazyLoaded(referencedPrimaryKeys)
              : EntityUtil.hashByPrimaryKey(selectMany(referencedPrimaryKeys));
      for (final Entity entity : entities)
        entity.initializeValue(foreignKeyProperty, hashedReferencedEntities.get(entity.getReferencedPrimaryKey(foreignKeyProperty)));
    }
  }

  private Map<Entity.Key, Entity> initLazyLoaded(final List<Entity.Key> referencedPrimaryKeys) {
    final Map<Entity.Key, Entity> entityMap = new HashMap<Entity.Key, Entity>();
    for (final Entity.Key key : referencedPrimaryKeys)
      entityMap.put(key, new Entity(key));

    return entityMap;
  }

  private static List<Entity.Key> getPrimaryKeysOfEntityValues(final List<Entity> entities,
                                                               final Property.ForeignKeyProperty foreignKeyProperty) {
    final Set<Entity.Key> keySet = new HashSet<Entity.Key>(entities.size());
    for (final Entity entity : entities) {
      final Entity.Key key = entity.getReferencedPrimaryKey(foreignKeyProperty);
      if (key != null)
        keySet.add(key);
    }

    return new ArrayList<Entity.Key>(keySet);
  }

  private int queryNextIdValue(final String entityID, final IdSource idSource) throws DbException {
    String sql;
    switch (idSource) {
      case MAX_PLUS_ONE:
        sql = new StringBuilder("select max(").append(EntityRepository.getPrimaryKeyProperties(entityID).get(0).getColumnName())
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

  private EntityResultPacker getEntityResultPacker(final String entityID) {
    EntityResultPacker packer = entityResultPackers.get(entityID);
    if (packer == null)
      entityResultPackers.put(entityID, packer = new EntityResultPacker(entityID,
              EntityRepository.getDatabaseProperties(entityID), EntityRepository.getTransientProperties(entityID)));

    return packer;
  }

  private ResultPacker getPropertyResultPacker(final Type propertyType) {
    ResultPacker packer = propertyResultPackers.get(propertyType);
    if (packer == null) {
      propertyResultPackers.put(propertyType, packer = new ResultPacker() {
        public List pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
          final List<Object> result = new ArrayList<Object>(50);
          int counter = 0;
          while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
            switch(propertyType) {
              case INT:
                result.add(resultSet.getInt(1));
                break;
              case DOUBLE:
                result.add(resultSet.getDouble(1));
                break;
              default:
                result.add(resultSet.getObject(1));
            }
          }
          return result;
        }
      });
    }

    return packer;
  }

  private Set<Dependency> resolveEntityDependencies(final String entityID) {
    final Collection<String> entityIDs = EntityRepository.getDefinedEntities();
    final Set<Dependency> dependencies = new HashSet<Dependency>();
    for (final String entityIDToCheck : entityIDs) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(entityIDToCheck))
        if (foreignKeyProperty.getReferencedEntityID().equals(entityID))
          dependencies.add(new Dependency(entityIDToCheck, foreignKeyProperty.getReferenceProperties()));
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