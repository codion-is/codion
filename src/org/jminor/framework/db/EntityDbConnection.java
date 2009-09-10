/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.AuthenticationException;
import org.jminor.common.db.Database;
import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbException;
import org.jminor.common.db.DbUtil;
import org.jminor.common.db.IdSource;
import org.jminor.common.db.RecordNotFoundException;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.User;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntityKeyCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityKey;
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

  private final Map<String, EntityResultPacker> resultPackers = new HashMap<String, EntityResultPacker>();

  /**Used by the EntityDbConnectionPool class*/
  long poolTime = -1;

  /**
   * Constructs a new EntityDbConnection instance
   * @param user the user used for connecting to the database
   * @throws AuthenticationException in case the user credentials are not accepted
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   */
  public EntityDbConnection(final User user) throws AuthenticationException, ClassNotFoundException {
    super(user);
  }

  /** {@inheritDoc} */
  public User getUser() {
    return super.getConnectionUser();
  }

  /** {@inheritDoc} */
  public void logout() {
    disconnect();
  }

  /** {@inheritDoc} */
  public List<EntityKey> insert(final List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return new ArrayList<EntityKey>();

    final List<EntityKey> ret = new ArrayList<EntityKey>();
    String sql = null;//so we can include it in the exception
    try {
      for (final Entity entity : entities) {
        final String entityID = entity.getEntityID();
        if (EntityRepository.isReadOnly(entityID))
          throw new DbException("Can not insert a read only entity");

        final IdSource idSource = EntityRepository.getIdSource(entityID);
        if (idSource == IdSource.MAX_PLUS_ONE || idSource == IdSource.SEQUENCE || idSource == IdSource.QUERY)
          entity.setValue(entity.getPrimaryKey().getFirstKeyProperty(), getNextIdValue(entityID, idSource), false);

        execute(sql = getInsertSQL(entity));

        if (idSource == IdSource.AUTO_INCREMENT)
          entity.setValue(entity.getPrimaryKey().getFirstKeyProperty(),
                  queryInteger(Database.get().getAutoIncrementValueSQL(
                          EntityRepository.getEntityIdSource(entityID))), false);

        ret.add(entity.getPrimaryKey());
      }

      if (!isTransactionOpen())
        commit();

      return ret;
    }
    catch (SQLException e) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.error(this, ex);
      }
      throw new DbException(e, sql);
    }
  }

  /** {@inheritDoc} */
  public List<Entity> update(final List<Entity> entities) throws DbException {
    if (entities.size() == 0)
      return entities;

    final List<String> statements = new ArrayList<String>();
    for (final Entity entity : entities) {
      if (EntityRepository.isReadOnly(entity.getEntityID()))
        throw new DbException("Can not update a read only entity");
      if (!entity.isModified())
        throw new DbException("Trying to update non-modified entity: " + entity);
      else
        statements.add(getUpdateSQL(entity));
    }

    execute(statements);

    return selectMany(EntityUtil.getPrimaryKeys(entities));
  }

  /** {@inheritDoc} */
  public void delete(final List<EntityKey> entityKeys) throws DbException {
    if (entityKeys == null || entityKeys.size() == 0)
      return;

    final List<String> statements = new ArrayList<String>();
    for (final EntityKey entityKey : entityKeys) {
      if (EntityRepository.isReadOnly(entityKey.getEntityID()))
        throw new DbException("Can not delete a read only entity");
      statements.add(0, getDeleteSQL(entityKey));
    }

    execute(statements);
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DbException {
    return selectSingle(new EntityCriteria(entityID,
            new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID), SearchType.LIKE, value)));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final EntityKey primaryKey) throws DbException {
    return selectSingle(new EntityCriteria(primaryKey.getEntityID(), new EntityKeyCriteria(primaryKey)));
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final EntityCriteria criteria) throws DbException {
    final List<Entity> ret = selectMany(criteria);
    if (ret.size() == 0)
      throw new RecordNotFoundException(FrameworkMessages.get(FrameworkMessages.RECORD_NOT_FOUND));
    if (ret.size() > 1)
      throw new DbException(FrameworkMessages.get(FrameworkMessages.MANY_RECORDS_FOUND));

    return ret.get(0);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectMany(final List<EntityKey> primaryKeys) throws DbException {
    if (primaryKeys == null || primaryKeys.size() == 0)
      return new ArrayList<Entity>(0);

    return selectMany(new EntityCriteria(primaryKeys.get(0).getEntityID(), new EntityKeyCriteria(primaryKeys)));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final String entityID, final String propertyID, final Object... values) throws DbException {
    return selectMany(new EntityCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            values != null && values.length > 1 ? SearchType.IN : SearchType.LIKE, values)));
  }

  /** {@inheritDoc} */
  public List<Entity> selectAll(final String entityID) throws DbException {
    return selectMany(new EntityCriteria(entityID, null, EntityRepository.getOrderByClause(entityID)));
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectMany(final EntityCriteria criteria) throws DbException {
    addCacheQueriesRequest();

    String sql = null;
    try {
      final String datasource = EntityRepository.getSelectTableName(criteria.getEntityID());
      sql = DbUtil.generateSelectSql(datasource, EntityRepository.getSelectColumnsString(criteria.getEntityID()),
              criteria.getWhereClause(!datasource.toUpperCase().contains("WHERE")), criteria.getOrderByClause());

      final List<Entity> result = (List<Entity>) query(sql, getResultPacker(criteria.getEntityID()), criteria.getFetchCount());

      if (!lastQueryResultCached())
        setForeignKeyValues(result);

      return result;
    }
    catch (SQLException sqle) {
      log.info(sql);
      log.error(this, sqle);
      throw new DbException(sqle, sql);
    }
    finally {
      removeCacheQueriesRequest();
    }
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectForUpdate(final List<EntityKey> primaryKeys) throws DbException {
    if (primaryKeys == null || primaryKeys.size() == 0)
      throw new IllegalArgumentException("Can not select for update without keys");
    if (isTransactionOpen())
      throw new IllegalStateException("Can not use select for update within an open transaction");

    final String entityID = primaryKeys.get(0).getEntityID();
    final StringBuilder sql = new StringBuilder();
    try {
      final String datasource = EntityRepository.getSelectTableName(entityID);
      sql.append(DbUtil.generateSelectSql(datasource, EntityRepository.getSelectColumnsString(entityID),
              new EntityCriteria(entityID, new EntityKeyCriteria(primaryKeys)).getWhereClause(
                      !datasource.toUpperCase().contains("WHERE")), null));
      sql.append(" for update").append((Database.get().supportsNoWait() ? " nowait" : ""));

      final List<Entity> result = (List<Entity>) query(sql.toString(), getResultPacker(entityID), -1);
      if (result.size() == 0)
        throw new RecordNotFoundException(FrameworkMessages.get(FrameworkMessages.RECORD_NOT_FOUND));
      if (result.size() != primaryKeys.size()) {
        try {//this means we got the lock, but for a different number of records than was intended, better release it right away
          endTransaction(false);
        }
        catch (SQLException e) {/**/}
        throw new DbException(FrameworkMessages.get(FrameworkMessages.MANY_RECORDS_FOUND));
      }

      if (!lastQueryResultCached())
        setForeignKeyValues(result);

      return result;
    }
    catch (SQLException sqle) {
      log.info(sql);
      log.error(this, sqle);
      throw new DbException(sqle, sql.toString());
    }
  }

  /** {@inheritDoc} */
  public List<?> selectPropertyValues(final String entityID, final String columnName, final boolean order) throws DbException {
    String sql = null;
    try {
      sql = DbUtil.generateSelectSql(EntityRepository.getSelectTableName(entityID),
              new StringBuilder("distinct ").append(columnName).toString(),
              new StringBuilder("where ").append(columnName).append(" is not null").toString(), order ? columnName : null);

      return query(sql, getPacker(EntityRepository.getProperty(entityID, columnName).propertyType), -1);
    }
    catch (SQLException sqle) {
      throw new DbException(sqle, sql);
    }
  }

  /** {@inheritDoc} */
  public List<List> selectRows(final String statement, final int fetchCount) throws DbException {
    try {
      return queryObjects(statement, fetchCount);
    }
    catch (SQLException sqle) {
      throw new DbException(sqle, statement);
    }
  }

  /** {@inheritDoc} */
  public int selectRowCount(final EntityCriteria criteria) throws DbException {
    String sql = "";
    try {
      return queryInteger(sql = DbUtil.generateSelectSql(
              EntityRepository.getSelectTableName(criteria.getEntityID()), "count(*)",
              criteria.getWhereClause(), null));
    }
    catch (SQLException sqle) {
      throw new DbException(sqle, sql);
    }
  }

  /** {@inheritDoc} */
  public Map<String, List<Entity>> selectDependentEntities(final List<Entity> entities) throws DbException {
    final Map<String, List<Entity>> ret = new HashMap<String, List<Entity>>();
    if (entities == null || entities.size() == 0)
      return ret;

    try {
      addCacheQueriesRequest();
      final Set<Dependency> dependencies = resolveEntityDependencies(entities.get(0).getEntityID());
      for (final Dependency dependency : dependencies) {
        final List<Entity> dependentEntities = selectMany(new EntityCriteria(dependency.entityID,
                new EntityKeyCriteria(dependency.foreignKeyProperties, EntityUtil.getPrimaryKeys(entities))));
        if (dependentEntities.size() > 0)
          ret.put(dependency.entityID, dependentEntities);
      }

      return ret;
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
    catch (SQLException sqle) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.info(statement);
        log.error(this, ex);
      }
      throw new DbException(sqle, statement);
    }
  }

  /** {@inheritDoc} */
  public Object executeStatement(final String statement, final int outParamType) throws DbException {
    try {
      final Object ret = executeCallableStatement(statement, outParamType);
      if (!isTransactionOpen())
        commit();

      return ret;
    }
    catch (SQLException sqle) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.info(statement);
        log.error(this, ex);
      }
      throw new DbException(sqle, statement);
    }
  }

  /** {@inheritDoc} */
  public JasperPrint fillReport(final JasperReport report, final Map reportParams) throws JRException {
    return JasperFillManager.fillReport(report, reportParams, getConnection());
  }

  /** {@inheritDoc} */
  public Entity writeBlob(final Entity entity, final String propertyID, final byte[] blobData) throws DbException {
    if (isTransactionOpen())
      throw new DbException("Can not save blob within an open transaction");

    try {
      boolean success = false;
      try {
        beginTransaction();
        final Property.BlobProperty property = (Property.BlobProperty) entity.getProperty(propertyID);

        final String whereCondition = EntityUtil.getWhereCondition(entity);

        execute(new StringBuilder("update ").append(entity.getEntityID()).append(" set ").append(property.propertyID)
                .append(" = '").append(entity.getStringValue(propertyID)).append("' ").append(whereCondition).toString());

        writeBlobField(blobData, EntityRepository.getTableName(entity.getEntityID()),
                property.getBlobColumnName(), whereCondition);
        success = true;

        return entity;
      }
      finally {
        endTransaction(success);
      }
    }
    catch (SQLException sqle) {
      throw new DbException(sqle);
    }
  }

  /** {@inheritDoc} */
  public byte[] readBlob(final Entity entity, final String propertyID) throws Exception {
    try {
      final Property.BlobProperty property = (Property.BlobProperty) entity.getProperty(propertyID);

      return readBlobField(EntityRepository.getTableName(entity.getEntityID()), property.getBlobColumnName(),
              EntityUtil.getWhereCondition(entity));
    }
    catch (SQLException sqle) {
      throw new DbException(sqle);
    }
  }

  /**
   * @param entity the Entity instance
   * @return a query for inserting this entity instance
   */
  static String getInsertSQL(final Entity entity) {
    final StringBuilder sql = new StringBuilder("insert into ");
    sql.append(EntityRepository.getTableName(entity.getEntityID())).append("(");
    final StringBuilder columnValues = new StringBuilder(") values(");
    final List<Property> insertProperties = EntityUtil.getInsertProperties(entity);
    int columnIndex = 0;
    for (final Property property : insertProperties) {
      sql.append(property.propertyID);
      columnValues.append(EntityUtil.getSQLStringValue(property, entity.getValue(property.propertyID)));
      if (columnIndex++ < insertProperties.size()-1) {
        sql.append(", ");
        columnValues.append(", ");
      }
    }

    return sql.append(columnValues).append(")").toString();
  }

  /**
   * @param entity the Entity instance
   * @return a query for updating this entity instance
   * @throws DbException in case the entity is unmodified
   */
  static String getUpdateSQL(final Entity entity) throws DbException{
    if (!entity.isModified())
      throw new RuntimeException("Can not get update sql for an unmodified entity");

    final StringBuilder sql = new StringBuilder("update ");
    sql.append(EntityRepository.getTableName(entity.getEntityID())).append(" set ");
    final Collection<Property> properties = EntityUtil.getUpdateProperties(entity);
    if (properties.size() == 0)
      throw new DbException("No modified updateable properties found in entity: " + entity);
    int columnIndex = 0;
    for (final Property property : properties) {
      sql.append(property.propertyID).append(" = ").append(EntityUtil.getSQLStringValue(property, entity.getValue(property.propertyID)));
      if (columnIndex++ < properties.size() - 1)
        sql.append(", ");
    }

    return sql.append(EntityUtil.getWhereCondition(entity)).toString();
  }

  /**
   * @param entityKey the EntityKey instance
   * @return a query for deleting the entity having the given primary key
   */
  static String getDeleteSQL(final EntityKey entityKey) {
    return new StringBuilder("delete from ").append(EntityRepository.getTableName(entityKey.getEntityID()))
            .append(EntityUtil.getWhereCondition(entityKey)).toString();
  }

  private void execute(final List<String> statements) throws DbException {
    String sql = null;
    try {
      for (final String statement : statements)
        execute(sql = statement);
      if (!isTransactionOpen())
        commit();
    }
    catch (SQLException sqle) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.info(sql);
        log.error(this, ex);
      }

      throw new DbException(sqle, sql);
    }
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
      final List<EntityKey> referencedPrimaryKeys = getPrimaryKeysOfEntityValues(entities, foreignKeyProperty);
      final Map<EntityKey, Entity> hashedReferencedEntities = foreignKeyProperty.lazyLoading
              ? initLazyLoaded(referencedPrimaryKeys)
              : EntityUtil.hashByPrimaryKey(selectMany(referencedPrimaryKeys));
      for (final Entity entity : entities)
        entity.initializeValue(foreignKeyProperty, hashedReferencedEntities.get(entity.getReferencedPrimaryKey(foreignKeyProperty)));
    }
  }

  private Map<EntityKey, Entity> initLazyLoaded(final List<EntityKey> referencedPrimaryKeys) {
    final Map<EntityKey, Entity> ret = new HashMap<EntityKey, Entity>();
    for (final EntityKey key : referencedPrimaryKeys)
      ret.put(key, new Entity(key));

    return ret;
  }

  private static List<EntityKey> getPrimaryKeysOfEntityValues(final List<Entity> entities,
                                                              final Property.ForeignKeyProperty foreignKeyProperty) {
    final Set<EntityKey> ret = new HashSet<EntityKey>(entities.size());
    for (final Entity entity : entities) {
      final EntityKey key = entity.getReferencedPrimaryKey(foreignKeyProperty);
      if (key != null)
        ret.add(key);
    }

    return new ArrayList<EntityKey>(ret);
  }

  private EntityResultPacker getResultPacker(final String entityID) {
    EntityResultPacker packer = resultPackers.get(entityID);
    if (packer == null)
      resultPackers.put(entityID, packer = new EntityResultPacker(entityID, EntityRepository.getDatabaseProperties(entityID)));

    return packer;
  }

  private int getNextIdValue(final String entityID, final IdSource idSource) throws DbException {
    String sql;
    switch (idSource) {
      case MAX_PLUS_ONE:
        sql = new StringBuilder("select max(").append(EntityRepository.getPrimaryKeyProperties(entityID).get(0).propertyID)
                .append(") + 1 from ").append(EntityRepository.getTableName(entityID)).toString();
        break;
      case QUERY:
        sql = EntityRepository.getEntityIdSource(entityID);
        break;
      case SEQUENCE:
        sql = Database.get().getSequenceSQL(EntityRepository.getEntityIdSource(entityID));
        break;
      default:
        throw new IllegalArgumentException(idSource + " is not a valid auto-increment ID source constant");
    }
    try {
      return queryInteger(sql);
    }
    catch (SQLException sqle) {
      throw new DbException(sqle, sql);
    }
  }

  private static ResultPacker getPacker(final Type propertyType) {
    return new ResultPacker() {
      public List pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
        final List<Object> ret = new ArrayList<Object>(50);
        int counter = 0;
        while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
          switch(propertyType) {
            case INT:
              ret.add(resultSet.getInt(1));
              break;
            case DOUBLE:
              ret.add(resultSet.getDouble(1));
              break;
            default:
              ret.add(resultSet.getObject(1));
          }
        }
        return ret;
      }
    };
  }

  private Set<Dependency> resolveEntityDependencies(final String entityID) {
    final String[] entityIDs = EntityRepository.getInitializedEntities();
    final Set<Dependency> dependencies = new HashSet<Dependency>();
    for (final String entityIDToCheck : entityIDs) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(entityIDToCheck))
        if (foreignKeyProperty.referenceEntityID.equals(entityID))
          dependencies.add(new Dependency(entityIDToCheck, foreignKeyProperty.referenceProperties));
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