/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.User;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityKey;

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
public interface EntityDb {

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
  public void disconnect() throws Exception;

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
   * Begins a transaction on this connection
   * @throws IllegalStateException if a transaction is already open
   */
  public void beginTransaction() throws Exception;

  /**
   * Ends the transaction on this connection, if no transaction is open
   * then only commit or rollback is performed
   * @param commit if true then a commit is performed, otherwise rollback
   * @throws java.sql.SQLException in case of a sql exception
   */
  public void endTransaction(final boolean commit) throws Exception;

  /**
   * Executes the given statement.
   * This method does not handle select statements.
   * @param statement the statement to execute
   * @throws org.jminor.common.db.DbException in case of a database exception
   */
  public void executeStatement(final String statement) throws Exception;

  /**
   * Executes the given statement.
   * This method does not handle select statements.
   * @param statement the statement to execute
   * @param outParamType the type of the output param, if any, java.sql.Types.*
   * @throws org.jminor.common.db.DbException in case of a database error
   * @return the return parameter if any, otherwise null
   */
  public Object executeStatement(final String statement, final int outParamType) throws Exception;

  /**
   * Inserts the given entities, returning a list containing the primary keys of the inserted entities
   * in the same order as they were received
   * @param entities the entities to insert
   * @return the primary key values of the inserted entities
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public List<EntityKey> insert(final List<Entity> entities) throws Exception;

  /**
   * Updates the given entities according to their properties
   * @param entities the entities to update
   * @return the updated entities
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws org.jminor.framework.db.exception.EntityModifiedException in case an entity has been modified by another user
   */
  public List<Entity> update(final List<Entity> entities) throws Exception;

  /**
   * Deletes the entities according to the given primary keys
   * @param entityKeys the primary keys of the entities to delete
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public void delete(final List<EntityKey> entityKeys) throws Exception;

  /**
   * Selects distinct non-null values of the given property of the given entity
   * @param entityID the class of the Entity
   * @param propertyID the ID of the property
   * @param order if true then the result is ordered
   * @return the values in the given column (Property) in the given table (Entity)
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public List<?> selectPropertyValues(final String entityID, final String propertyID, final boolean order) throws Exception;

  /**
   * Selects a single entity
   * @param entityID the Class of the entity to select
   * @param propertyID the ID of the property to use as a condition
   * @param value the value to use in the condition
   * @return an entity of the type <code>entityID</code>, having the
   * value of <code>propertyID</code> as <code>value</code>
   * @throws org.jminor.common.db.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws Exception;

  /**
   * Selects a single entity
   * @param key the key used in the condition
   * @return an entity of the type <code>entityID</code>, having the key <code>key</code>
   * @throws org.jminor.common.db.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public Entity selectSingle(final EntityKey key) throws Exception;

  /**
   * Selects a single entity according to the specified criteria, throws a DbException
   * if the criteria results in more than one entity
   * @param criteria the criteria specifying the entity to select
   * @return the entities according to the given criteria
   * @throws org.jminor.common.db.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.DbException if an exception occurs
   */
  public Entity selectSingle(final EntityCriteria criteria) throws Exception;

  /**
   * Returns entities according to <code>keys</code>
   * @param keys the keys used in the condition
   * @return entities according to <code>keys</code>
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public List<Entity> selectMany(final List<EntityKey> keys) throws Exception;

  /**
   * Selects entities according to the specified criteria
   * @param criteria the criteria specifying which entities to select
   * @return entities according to the given criteria
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public List<Entity> selectMany(final EntityCriteria criteria) throws Exception;

  /**
   * Selects entities according to one property (<code>propertyID</code>), using <code>values</code> as a condition
   * @param entityID the Class of the entities to select
   * @param propertyID the ID of the condition property
   * @param values the property values to use as condition
   * @return entities of the type <code>entityID</code> according to <code>propertyID</code> and <code>values</code>
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public List<Entity> selectMany(final String entityID, final String propertyID,
                                 final Object... values) throws Exception;

  /**
   * Selects all the entities of the given type
   * @param entityID the Class of the entities to select
   * @return all entities of the given type
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public List<Entity> selectAll(final String entityID) throws Exception;

  /**
   * Returns the entities that depend on the given entities via foreign keys
   * @param entities the entities for which to retrieve dependencies, mapped to corresponding entityIDs
   * @return the entities that depend on <code>entities</code>
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public Map<String,List<Entity>> selectDependentEntities(final List<Entity> entities) throws Exception;

  /**
   * Selects the number of rows returned according to the given criteria
   * @param criteria the search criteria
   * @return the number of rows fitting the given criteria
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public int selectRowCount(final EntityCriteria criteria) throws Exception;

  /**
   * Takes a JasperReport object using a JDBC datasource and returns an initialized JasperPrint object
   * @param report the report to fill
   * @param reportParams the report parameters
   * @return an initialized JasperPrint object
   * @throws net.sf.jasperreports.engine.JRException in case of a report exception
   */
  public JasperPrint fillReport(final JasperReport report, final Map reportParams) throws Exception;

  /**
   * Executes the given statement and returns the result in a List of rows, where each row
   * is represented by a List of Objects
   * @param statement the sql statement to execute
   * @param fetchCount the maximum number of records to retrieve, -1 for all
   * @return a List of rows represented by a List of Objects
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public List<List> selectRows(final String statement, final int fetchCount) throws Exception;

  /**
   * Writes <code>blobData</code> in the blob field specified by the property identified by <code>propertyID</code>
   * for the given entity
   * @param entity the entity for which to write the blob
   * @param propertyID the ID of the blob property
   * @param blobData the blob data
   * @return the entity
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public Entity writeBlob(final Entity entity, final String propertyID, final byte[] blobData) throws Exception;

  /**
   * Reads the blob specified by the property identified by <code>propertyID</code> from the given entity
   * @param entity the entity
   * @param propertyID the ID of the blob property
   * @return a byte array containing the blob data
   * @throws org.jminor.common.db.DbException in case of a db exception
   */
  public byte[] readBlob(final Entity entity, final String propertyID) throws Exception;
}
