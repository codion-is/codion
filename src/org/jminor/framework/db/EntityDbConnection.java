/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbException;
import org.jminor.common.db.DbUtil;
import org.jminor.common.db.IResultPacker;
import org.jminor.common.db.IdSource;
import org.jminor.common.db.RecordNotFoundException;
import org.jminor.common.db.TableStatus;
import org.jminor.common.db.User;
import org.jminor.common.db.UserAccessException;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityCriteria;
import org.jminor.framework.model.EntityDependencies;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.EntityKeyCriteria;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.PropertyCriteria;
import org.jminor.framework.model.Type;

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
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Implements the database layer accessible to the client
 */
public class EntityDbConnection extends DbConnection implements IEntityDb {

  private static final Logger log = Util.getLogger(EntityDbConnection.class);
  private static final Map<String, EntityDependencies> entityDependencies = new HashMap<String, EntityDependencies>();

  private final State stCheckDependenciesOnDelete = new State("EntityDbConnection.stCheckDependenciesOnDelete");
  private final Map<String, Map<EntityKey, Entity>> entityCache = new HashMap<String, Map<EntityKey, Entity>>();
  private final Map<String, EntityResultPacker> resultPackers = new HashMap<String, EntityResultPacker>();

  private boolean entityCacheEnabled = false;
  private FrameworkSettings settings = FrameworkSettings.get();

  private static int cachedKeyQueries = 0;
  private static int partiallyCachedKeyQueries = 0;
  private static int queriedByKey;

  private long poolTime = -1;

  public EntityDbConnection(final User user, final EntityRepository repository, final FrameworkSettings settings)
          throws UserAccessException, ClassNotFoundException {
    super(user);
    initialize(repository, settings);
  }

  public void setPoolTime(final long poolTime) {
    this.poolTime = poolTime;
  }

  public long getPoolTime() {
    return poolTime;
  }

  public int getEntityCacheSize() {
    int ret = 0;
    synchronized (entityCache) {
      for (final Map.Entry<String, Map<EntityKey, Entity>> entry : entityCache.entrySet())
        ret += entry.getValue().size();
    }

    return ret;
  }

  public static int getCachedKeyQueries() {
    return cachedKeyQueries;
  }

  public static int getPartiallyCachedKeyQueries() {
    return partiallyCachedKeyQueries;
  }

  public static int getQueriedByKey() {
    return queriedByKey;
  }

  public void clearStateData() {
    poolTime = -1;
    entityCache.clear();
  }

  /** {@inheritDoc} */
  public User getUser() throws Exception {
    return super.getConnectionUser();
  }

  /** {@inheritDoc} */
  public void logout() throws Exception {
    disconnect();
  }

  /** {@inheritDoc} */
  public void addCacheQueriesRequest() {
    super.addCacheQueriesRequest();
    if (getCacheQueriesRequests() > 0)
      this.entityCacheEnabled = true;
  }

  /** {@inheritDoc} */
  public void removeCacheQueriesRequest() {
    super.removeCacheQueriesRequest();
    if (getCacheQueriesRequests() == 0) {
      this.entityCacheEnabled = false;
      this.entityCache.clear();
    }
  }

  /** {@inheritDoc} */
  public void setCheckDependencies(final boolean checkDependencies) {
    stCheckDependenciesOnDelete.setActive(checkDependencies);
  }

  /** {@inheritDoc} */
  public boolean getCheckDependencies() {
    return stCheckDependenciesOnDelete.isActive();
  }

  /**
   * Inserts the given entities, returning an array containing the primary keys of the inserted records
   * @param entities the entities to insert
   * @return the primary keys of the inserted entities
   * @throws DbException
   */
  public List<EntityKey> insert(final List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return new ArrayList<EntityKey>();

    final List<EntityKey> ret = new ArrayList<EntityKey>();
    String sql = null;//so we can include it in the exception
    try {
      for (final Entity entity : entities) {
        final String entityID = entity.getEntityID();
        if (Entity.repository.isReadOnly(entityID))
          throw new DbException("Cannot insert a read only entity");

        final IdSource idSource = Entity.repository.getIdSource(entityID);
        if (idSource == IdSource.ID_MAX_PLUS_ONE || idSource == IdSource.ID_SEQUENCE
                || idSource == IdSource.ID_QUERY)
          entity.getPrimaryKey().setValue(getNextIdValue(entityID, idSource));

        execute(sql = EntityUtil.getInsertSQL(entity));

        if (idSource == IdSource.ID_AUTO_INCREMENT)
          entity.getPrimaryKey().setValue(getAutoIncrementValue(
                  DbUtil.getAutoIncrementValueSQL(Entity.repository.getEntityIdSource(entityID))));

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
      catch (Exception ex) {
        log.error(this, ex);
      }
      throw new DbException(e, sql);
    }
  }

  /** {@inheritDoc} */
  public List<Entity> update(final List<Entity> entities) throws DbException {
    if (entities.size() == 0)
      throw new DbException("Empty update batch!");

    final ArrayList<String> statements = new ArrayList<String>();
    for (final Entity entity : entities) {
      if (Entity.repository.isReadOnly(entity.getEntityID()))
        throw new DbException("Cannot update a read only entity");
      if (!entity.isModified())
        throw new DbException("Trying to update non-modified entity: " + entity);
      else
        statements.add(EntityUtil.getUpdateSQL(entity));
    }

    execute(statements.toArray(new String[statements.size()]));

    final List<EntityKey> primaryKeys = new ArrayList<EntityKey>(entities.size());
    for (final Entity entity : entities)
      primaryKeys.add(entity.getPrimaryKey());

    return selectMany(entities.get(0).getEntityID(), primaryKeys);
  }

  /** {@inheritDoc} */
  public void delete(final List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return;

    if (getCheckDependencies()) {
      final HashMap<String, List<Entity>> dependencies = getDependentEntities(entities);
      if (EntityUtil.activeDependencies(dependencies))
        throw new DbException("Entity has dependencies", "", entities.get(0));
    }

    final ArrayList<String> statements = new ArrayList<String>();
    for (final Entity entity : entities) {
      if (Entity.repository.isReadOnly(entity.getEntityID()))
        throw new DbException("Cannot delete a read only entity");
      statements.add(0, EntityUtil.getDeleteSQL(entity));
    }

    execute(statements.toArray(new String[statements.size()]));
  }

  /** {@inheritDoc} */
  public List<?> selectPropertyValues(final String entityID, final String columnName,
                                      final boolean distinct, final boolean order) throws DbException {
    String sql = null;
    try {
      sql = DbUtil.generateSelectSql(Entity.repository.getSelectTableName(entityID),
              (distinct ? "distinct " : "") + columnName,
              "where " + columnName + " is not null", order ? columnName : null);

      return query(sql, getPacker(Entity.repository.getProperty(entityID, columnName).propertyType));
    }
    catch (SQLException e) {
      throw new DbException(e, sql);
    }
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final String entityID, final String propertyName, final Object value) throws Exception {
    return selectSingle(new EntityCriteria(entityID,
            new PropertyCriteria(Entity.repository.getProperty(entityID, propertyName), SearchType.LIKE, value)));
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
  public List<Entity> selectMany(final String entityID, final List<EntityKey> primaryKeys) throws DbException {
    return selectMany(entityID, null, primaryKeys);
  }

  /** {@inheritDoc} */
  public int selectRowCount(final EntityCriteria criteria) throws Exception {
    String sql = "";
    try {
      return queryInteger(sql = DbUtil.generateSelectSql(
              Entity.repository.getSelectTableName(criteria.getEntityID()), "count(*)",
              criteria.getWhereClause(), null));
    }
    catch (SQLException sqle) {
      throw new DbException(sqle, sql);
    }
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final String entityID, final String propertyName,
                                 final Object... values) throws DbException {
    return selectMany(new EntityCriteria(entityID,
            new PropertyCriteria(Entity.repository.getProperty(entityID, propertyName),
                    values != null && values.length > 1 ? SearchType.IN : SearchType.LIKE, values)));
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final EntityCriteria criteria) throws DbException {
    return selectMany(criteria, false);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public List<Entity> selectMany(final EntityCriteria criteria, final boolean order) throws DbException {
    if (criteria.isKeyCriteria())
      return selectMany(criteria.getEntityID(), ((EntityKeyCriteria) criteria.getCriteria()).getProperties(),
              ((EntityKeyCriteria) criteria.getCriteria()).getKeys());

    addCacheQueriesRequest();

    String sql = null;
    try {
      final String selectString = Entity.repository.getSelectString(criteria.getEntityID());
      String datasource = Entity.repository.getSelectTableName(criteria.getEntityID());
      final String whereCondition = criteria.getWhereClause(!datasource.toUpperCase().contains("WHERE"));
      if (settings.useQueryRange && criteria.isRangeCriteria()) {
        final String innerSubQuery = "(" + DbUtil.generateSelectSql(datasource, selectString, "",
                criteria.tableHasAuditColumns() ? "snt desc" : "rowid desc") + ")";
        datasource = "(" + DbUtil.generateSelectSql(innerSubQuery,
                selectString + ", rownum row_num", "", null) + ")";
      }
      sql = DbUtil.generateSelectSql(datasource, selectString, whereCondition,
              order ? Entity.repository.getOrderByColumnNames(criteria.getEntityID()) : null);

      final List<Entity> result = (List<Entity>) query(sql, getResultPacker(criteria.getEntityID()));

      if (!lastQueryResultCached())
        setReferencedEntities(result);

      final List<Entity> ret = new ArrayList<Entity>(result.size());
      for (final Entity entity : result)
        ret.add(entity);

      return ret;
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
  public List<Entity> selectAll(final String entityID) throws DbException {
    return selectAll(entityID, false);
  }

  /** {@inheritDoc} */
  public List<Entity> selectAll(final String entityID, final boolean order) throws DbException {
    return selectMany(new EntityCriteria(entityID), order);
  }

  /** {@inheritDoc} */
  public TableStatus getTableStatus(final String entityID,
                                    final boolean tableHasAuditColumns) throws DbException {
    final String tableName = Entity.repository.getSelectTableName(entityID);
    if (!tableHasAuditColumns)
      return getRecordCount(tableName);

    final String sql = DbUtil.isMySQL() ?
            "select count(*), greatest(max(snt), max(ifnull(sbt,snt))) last_change from " + tableName
            : "select count(*), greatest(max(snt), max(nvl(sbt,snt))) last_change from " + tableName;
    try {
      return (TableStatus) query(sql, DbUtil.TABLE_STATUS_PACKER).get(0);
    }
    catch (SQLException sqle) {
      return getRecordCount(tableName);
    }
  }

  /** {@inheritDoc} */
  public HashMap<String, List<Entity>> getDependentEntities(final List<Entity> entities) throws DbException {
    final HashMap<String, List<Entity>> ret = new HashMap<String, List<Entity>>();
    if (entities == null || entities.size() == 0)
      return ret;

    final EntityDependencies info = entityDependencies.get(entities.get(0).getEntityID());
    if (info == null)
      throw new DbException("Entity dependencies have not been initialized for " + entities.get(0).getEntityID());

    try {
      addCacheQueriesRequest();
      return getDependentEntities(info, entities);
    }
    finally {
      try {
        removeCacheQueriesRequest();
      }
      catch (Exception e) {
        log.error(this, e);
      }
    }
  }

  /** {@inheritDoc} */
  public void executeStatement(final String statement) throws Exception {
    try {
      execute(statement);
    }
    catch (SQLException e) {
      throw new DbException(e, statement);
    }
  }

  /** {@inheritDoc} */
  public JasperPrint fillReport(final JasperReport report, final HashMap reportParams) throws Exception {
    return JasperFillManager.fillReport(report, reportParams, getConnection());
  }

  /** {@inheritDoc} */
  public Object executeCallable(final String statement, final int outParamType) throws DbException {
    try {
      final Object ret = executeCallableStatement(statement, outParamType);
      if (!isTransactionOpen())
        commit();

      return ret;
    }
    catch (Exception e) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (Exception ex) {
        log.info(statement);
        log.error(this, ex);
      }

      throw new DbException(e, statement);
    }
  }

  /** {@inheritDoc} */
  protected String getDatabaseURL() {
    return DbUtil.getDatabaseURL(
            System.getProperty(FrameworkConstants.DATABASE_HOST_PROPERTY),
            System.getProperty(FrameworkConstants.DATABASE_PORT_PROPERTY),
            System.getProperty(FrameworkConstants.DATABASE_SID_PROPERTY));
  }

  void initialize(final EntityRepository repository, final FrameworkSettings settings) {
    repository.initializeAll();
    Entity.repository.add(repository);
    this.settings = settings;
    resolveEntityDependencies();
  }

  private void execute(final String[] sql) throws DbException {
    int i = 0;
    try {
      for (; i < sql.length; i++) {
        execute(sql[i]);
      }
      if (!isTransactionOpen())
        commit();
    }
    catch (SQLException e) {
      try {
        if (!isTransactionOpen())
          rollback();
      }
      catch (SQLException ex) {
        log.info(sql[i]);
        log.error(this, ex);
      }

      throw new DbException(e, sql[i]);
    }
  }

  @SuppressWarnings({"unchecked"})
  private List<Entity> selectMany(final String entityID, final List<Property> properties,
                                  final List<EntityKey> primaryKeys) throws DbException {
    if (primaryKeys == null || primaryKeys.size() == 0)
      return new ArrayList<Entity>(0);

    addCacheQueriesRequest();

    final ArrayList<EntityKey> primaryKeyList = new ArrayList<EntityKey>(primaryKeys);
    final List<Entity> returnList = new ArrayList<Entity>(primaryKeyList.size());
    if (entityCacheEnabled)
      returnList.addAll(getCachedEntities(entityID, primaryKeyList));//removes those primary keys from the list
    try {
      if (primaryKeyList.size() > 0) {
        if (primaryKeys.size() != primaryKeyList.size())
          partiallyCachedKeyQueries++;
        else
          queriedByKey++;

        String sql = null;
        try {
          final EntityCriteria criteria = new EntityCriteria(entityID,
                  new EntityKeyCriteria(properties, primaryKeyList.toArray(new EntityKey[primaryKeyList.size()])));
          final String datasource = Entity.repository.getSelectTableName(criteria.getEntityID());
          sql = DbUtil.generateSelectSql(datasource, Entity.repository.getSelectString(criteria.getEntityID()),
                  criteria.getWhereClause(!datasource.toUpperCase().contains("WHERE")), null);

          final List<Entity> result = (List<Entity>) query(sql, getResultPacker(criteria.getEntityID()));

          if (entityCacheEnabled)
            addToEntityCache(entityID, result);

          if (!lastQueryResultCached())
            setReferencedEntities(result);

          returnList.addAll(result);
        }
        catch (SQLException sqle) {
          log.info(sql);
          log.error(this, sqle);
          throw new DbException(sqle, sql);
        }
      }
      else {
        cachedKeyQueries++;
      }
    }
    finally {
      removeCacheQueriesRequest();
    }

    return returnList;
  }

  /**
   * @param entities the entities for which to set the reference entities
   * @throws DbException in case of a database exception
   */
  private void setReferencedEntities(final List<Entity> entities) throws DbException {
    if (entities == null || entities.size() == 0)
      return;

    for (final Property.EntityProperty entityProperty :
            Entity.repository.getEntityProperties(entities.get(0).getEntityID())) {
      final List<EntityKey> referencedPrimaryKeys = getPrimaryKeysOfEntityValues(entities, entityProperty);
      if (referencedPrimaryKeys.size() > 0) {
        final HashMap<EntityKey, Entity> referencedEntitiesHashed = entityProperty.isWeakReference
                ? initWeakReferences(referencedPrimaryKeys)
                : EntityUtil.hashByPrimaryKey(selectMany(entityProperty.referenceEntityID, referencedPrimaryKeys));
        for (final Entity entity : entities) {
          final EntityKey referenceEntityKey = entity.getReferencedKey(entityProperty);
          if (referenceEntityKey != null) {
            final Entity valueEntity = referencedEntitiesHashed.get(referenceEntityKey);
            if (valueEntity != null)
              entity.initializeValue(entityProperty, valueEntity);
          }
        }
      }
    }
  }

  private HashMap<EntityKey, Entity> initWeakReferences(final List<EntityKey> referencedPrimaryKeys) {
    final HashMap<EntityKey, Entity> ret = new HashMap<EntityKey, Entity>();
    for (final EntityKey key : referencedPrimaryKeys)
      ret.put(key, new Entity(key));

    return ret;
  }

  private HashMap<String, List<Entity>>getDependentEntities(final EntityDependencies entityDependencies,
                                                            final List<Entity> entities) throws DbException {
    final HashMap<String, List<Entity>> ret = new HashMap<String, List<Entity>>();
    final Set<EntityDependencies.Dependency> dependencies = entityDependencies.getDependencies();
    for (final EntityDependencies.Dependency dependency : dependencies) {
      final String dependentEntityID = dependency.getEntityID();
      if (dependentEntityID != null) {
        final ArrayList<EntityKey> primaryKeys = new ArrayList<EntityKey>(entities.size());
        for (final Entity entity : entities)
          primaryKeys.add(entity.getPrimaryKey());

        final List<Entity> dependentEntities = selectMany(new EntityCriteria(dependentEntityID,
                new EntityKeyCriteria(dependency.getDependingProperties(),
                        primaryKeys.toArray(new EntityKey[primaryKeys.size()]))));
        if (dependentEntities.size() > 0)
          ret.put(dependentEntityID, dependentEntities);
      }
    }

    return ret;
  }

  private void addToEntityCache(final String entityID, final Collection<Entity> entities) {
    if (!entityCache.containsKey(entityID))
      entityCache.put(entityID, new HashMap<EntityKey, Entity>(entities.size()));
    final Map<EntityKey, Entity> entityMap = entityCache.get(entityID);
    for (final Entity entity : entities)
      entityMap.put(entity.getPrimaryKey(), entity);
  }

  private Collection<Entity> getCachedEntities(final String entityID, final List<EntityKey> primaryKeyList) {
    final int keyCount = primaryKeyList.size();
    final Collection<Entity> ret = new ArrayList<Entity>(keyCount);
    final ListIterator<EntityKey> iterator = primaryKeyList.listIterator();
    final Map<EntityKey, Entity> cache = entityCache.get(entityID);
    int cachedCnt = 0;
    if (cache != null && cache.size() > 0 && cachedCnt < cache.size()) {
      while (iterator.hasNext()) {
        final Entity cachedEntity = cache.get(iterator.next());
        if (cachedEntity != null) {
          ret.add(cachedEntity);
          iterator.remove();
          cachedCnt++;
        }
      }
    }

    return ret;
  }

  private TableStatus getRecordCount(final String tableName) throws DbException {
    final String sql = "select count(*) from " + tableName;
    try {
      return (TableStatus) query(sql, DbUtil.TABLE_STATUS_PACKER).get(0);
    }
    catch (SQLException sqle) {
      throw new DbException(sqle, sql);
    }
  }

  private static List<EntityKey> getPrimaryKeysOfEntityValues(final List<Entity> entities,
                                                              final Property.EntityProperty property) {
    final HashSet<EntityKey> ret = new HashSet<EntityKey>(entities.size());
    for (final Entity entity : entities) {
      EntityKey key = entity.getReferencedKey(property);
      if (key != null)
        ret.add(key);
    }

    return EntityUtil.toList(ret);
  }

  private EntityResultPacker getResultPacker(String entityID) {
    EntityResultPacker packer = resultPackers.get(entityID);
    if (packer == null)
      resultPackers.put(entityID, packer = new EntityResultPacker(entityID));

    return packer;
  }

  private int getNextIdValue(final String entityID, final IdSource idSource) throws DbException {
    String sql;
    switch (idSource) {
      case ID_MAX_PLUS_ONE:
        sql = "select max(" + Entity.repository.getPrimaryKeyColumnNames(entityID)[0] + ") + 1 from " + entityID;
        break;
      case ID_QUERY:
        sql = Entity.repository.getEntityIdSource(entityID);
        break;
      case ID_SEQUENCE:
        sql = DbUtil.getSequenceSQL(Entity.repository.getEntityIdSource(entityID));
        break;
      default:
        throw new IllegalArgumentException(idSource + " is not a valid auto-increment ID source constant");
    }
    try {
      return queryInteger(sql);
    }
    catch (SQLException e) {
      throw new DbException(e, sql);
    }
  }

  private void resolveEntityDependencies() {
    final String[] entityIDs = Entity.repository.getInitializedEntities();
    for (final String entityID : entityIDs) {
      final EntityDependencies info = new EntityDependencies(entityID);
      for (final String entityCheckClass : entityIDs) {
        for (final Property.EntityProperty entityProperty : Entity.repository.getEntityProperties(entityCheckClass)) {
          final String dependentClass = entityProperty.referenceEntityID;
          if (dependentClass.equals(entityID))
            info.addDependency(entityCheckClass, entityProperty.referenceProperties);
        }
      }
      entityDependencies.put(entityID, info);
    }
  }

  private static IResultPacker getPacker(final Type propertyType) {
    return new IResultPacker() {
      public List pack(final ResultSet resultSet) throws SQLException {
        final List<Object> ret = new ArrayList<Object>(50);
        while (resultSet.next()) {
          switch(propertyType) {
            case INT:
              ret.add(resultSet.getInt(1));
              break;
            case DOUBLE:
              ret.add(resultSet.getDouble(1));
            default:
              ret.add(resultSet.getObject(1));
          }
        }
        return ret;
      }
    };
  }
}