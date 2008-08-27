/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.User;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityCriteria;
import org.jminor.framework.model.EntityKey;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.util.List;
import java.util.Map;

/**
 * Defines the database methods the db layer offers
 * User: darri
 * Date: 7.1.2005
 * Time: 11:35:59
 */
public interface IEntityDb{

  /**
   * @return the user being used by this connection
   * @throws Exception in case of an exception
   */
  public User getUser() throws Exception;

  /**
   * @return true if a connection has been made
   * @throws Exception in case of an exception
   */
  public boolean isConnected() throws Exception;

  /**
   * Logs out and disconnects
   * @throws Exception in case of an exception
   */
  public void logout() throws Exception;

  /**
   * @throws Exception in case of exception
   * @return true if this db connection is valid
   */
  public boolean isConnectionValid() throws Exception;

  /**
   * @throws Exception in case of exception
   * @return true if a transaction is open, false otherwise
   */
  public boolean isTransactionOpen() throws Exception;

  /**
   * Starts a transaction on this connection
   * @throws Exception in case of exception
   * @throws IllegalStateException if a transaction is already open
   */
  public void startTransaction() throws Exception;

  /**
   * Ends the transaction on this connection, if no transaction is open
   * then only commit or rollback is performed
   * @param rollback if true then a rollback is performed, otherwise commit
   * @throws java.sql.SQLException in case of a sql exception
   * @throws Exception in case of an exception
   */
  public void endTransaction(final boolean rollback) throws Exception;

  /**
   * @param checkReferences Value to set for property 'checkDependencies'.
   * @throws Exception in case of exception
   */
  public void setCheckDependencies(final boolean checkReferences) throws Exception;

  /**
   * @return Value for property 'checkDependencies'.
   * @throws Exception in case of exception
   */
  public boolean getCheckDependencies() throws Exception;

  /**
   * Executes the given statement
   * @param statement the statement to execute
   * @throws org.jminor.common.db.DbException in case of a database error
   * @throws Exception in case of exception
   */
  public void executeStatement(final String statement) throws Exception;

  /**
   * Executes the given statement
   * @param statement the statement to execute
   * @param outParamType the type of the output param, if any, java.sql.Types.*
   * @throws org.jminor.common.db.DbException in case of a database error
   * @throws Exception in case of exception
   * @return the return paramter if any, otherwise null
   */
  public Object executeCallable(final String statement, final int outParamType) throws Exception;

  /**
   * Inserts the given entities, returning a list containing the
   * primary keys of the inserted records
   * @param entities the entities to insert
   * @return the primary key values of the inserted entities
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public List<EntityKey> insert(final List<Entity> entities) throws Exception;

  /**
   * Updates the given entities according to their properties
   * @param entities the entities to update
   * @return the updated entities
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public List<Entity> update(final List<Entity> entities) throws Exception;

  /**
   * Deletes the given entities
   * @param entities the entities to delete
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public void delete(final List<Entity> entities) throws Exception;

  /**
   * Selects the non-null values of the given property of the given entity, returning them distinct and/or ordered
   * @param entityID the class of the Entity
   * @param propertyID the ID of the property
   * @param distinct if true then distinct values are returned
   * @param order if true then the result is ordered according to the entities ordering colomns
   * @return the values in the given column (Property) in the given table (Entity)
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public List<?> selectPropertyValues(final String entityID, final String propertyID,
                                      final boolean distinct, final boolean order) throws Exception;

  /**
   * Selects a single entity
   * @param entityID the Class of the entity to select
   * @param propertyID the ID of the property to use as a condition
   * @param value the value to use in the condition
   * @return an entity of the type <code>entityID</code>, having the
   * value of <code>propertyID</code> as <code>value</code>
   * @throws org.jminor.common.db.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws Exception;

  /**
   * Selects a single entity
   * @param key the key used in the condition
   * @return an entity of the type <code>entityID</code>, having the key <code>key</code>
   * @throws org.jminor.common.db.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public Entity selectSingle(final EntityKey key) throws Exception;

  /**
   * Selects for update the entity with the given key.
   * The update lock is released when the entity is subsequently updated or via endTransaction(true);
   * @param primaryKey the key of the entity to select for update
   * @return the entity
   * @throws org.jminor.common.db.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.DbException in case the entity is already locked by another user
   * @throws Exception in case of exception
   */
  public Entity selectForUpdate(final EntityKey primaryKey) throws Exception;

  /**
   * Selects a single entity according to the specified criteria, throws a DbException
   * if the criteria results in more than one entity
   * @param criteria the criteria specifying the entity to select
   * @return the entities according to the given criteria
   * @throws org.jminor.common.db.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.DbException if an exception occurs
   * @throws Exception
   */
  public Entity selectSingle(final EntityCriteria criteria) throws Exception;

  /**
   * Returns entities according to <code>keys</code>
   * @param keys the keys used in the condition
   * @return entities according to <code>keys</code>
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public List<Entity> selectMany(final List<EntityKey> keys) throws Exception;

  /**
   * Selects entities according to the specified criteria
   * @param criteria the criteria specifying which entities to select
   * @return entities according to the given criteria
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public List<Entity> selectMany(final EntityCriteria criteria) throws Exception;

  /**
   * Selects entities according to one property (<code>propertyID</code>), using <code>values</code> as a condition
   * @param entityID the Class of the entities to select
   * @param propertyID the ID of the condition property
   * @param values the property values to use as condition
   * @return entities of the type <code>entityID</code> according to <code>propertyID</code> and <code>values</code>
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public List<Entity> selectMany(final String entityID, final String propertyID,
                                 final Object... values) throws Exception;

  /**
   * Selects all the entities of the given type
   * @param entityID the Class of the entities to select
   * @return all entities of the given type
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public List<Entity> selectAll(final String entityID) throws Exception;

  /**
   * Selects all the entities of the given type
   * @param entityID the Class of the entities to select
   * @param order if true the result is ordered by the ordering property of <code>entityID</code>
   * @return all entities of the given type
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public List<Entity> selectAll(final String entityID, final boolean order) throws Exception;

  /**
   * Returns the entities that depend on the given entities via foreign keys
   * @param entities the entities for which to retrieve dependencies, mapped to corresponding entityIDs
   * @return the entities that depend on <code>entities</code>
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws org.jminor.common.model.UserException in case of a user exception
   * @throws Exception in case of exception
   */
  public Map<String,List<Entity>> getDependentEntities(final List<Entity> entities) throws Exception;

  /**
   * Selects the number of rows returned according to the given criteria
   * @param criteria the search criteria
   * @return the number of rows fitting the given criteria
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws Exception in case of exception
   */
  public int selectRowCount(final EntityCriteria criteria) throws Exception;

  /**
   * Takes a JasperReport object using a JDBC datasource and returns an initialized JasperPrint object
   * @param report the report to fill
   * @param reportParams the report parameters
   * @return an initialized JasperPrint object
   * @throws net.sf.jasperreports.engine.JRException in case of a report exception
   * @throws Exception in case of exception
   */
  public JasperPrint fillReport(final JasperReport report, final Map reportParams) throws Exception;

  /**
   * Executes the given statement and returns the result in a List of rows, where each row
   * is represented by a List of Objects
   * @param statement the sql statement to execute
   * @param recordCount the maximum number of records to retrieve, -1 for all
   * @return a List of rows represented by a List of Objects
   * @throws Exception in case of an exception
   */
  public List<List> selectRows(final String statement, final int recordCount) throws Exception;
}
