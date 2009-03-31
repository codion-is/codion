/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.DbException;
import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IEntityDbRemote extends IEntityDb, Remote {

  /** {@inheritDoc} */
  public User getUser() throws RemoteException;

  /** {@inheritDoc} */
  public boolean isConnected() throws RemoteException;

  /** {@inheritDoc} */
  public void logout() throws RemoteException;

  /**
   * @throws RemoteException in case of a remote exception
   * @return true if this db connection is valid
   */
  public boolean isConnectionValid() throws RemoteException;

  /**
   * @throws RemoteException in case of a remote exception
   * @return true if a transaction is open, false otherwise
   */
  public boolean isTransactionOpen() throws RemoteException;

  /**
   * Starts a transaction on this connection
   * @throws RemoteException in case of a remote exception
   * @throws IllegalStateException if a transaction is already open
   */
  public void startTransaction() throws IllegalStateException, RemoteException;

  /**
   * Ends the transaction on this connection
   * @param rollback if true then a rollback is performed, otherwise commit
   * @throws IllegalStateException if a transaction is not open
   * @throws java.sql.SQLException in case of a sql exception
   * @throws RemoteException in case of a remote exception
   */
  public void endTransaction(final boolean rollback) throws IllegalStateException, SQLException, RemoteException;

  /**
   * @param checkDependencies true if dependencies should be checked before a delete is performed
   * @throws java.rmi.RemoteException
   */
  public void setCheckDependencies(final boolean checkDependencies) throws RemoteException;

  /**
   * @return true if dependencies should be checked before a delete is performed
   * @throws java.rmi.RemoteException
   */
  public boolean isCheckDependencies() throws RemoteException;

  /**
   * Executes the given statement
   * @param statement the statement to execute
   * @throws DbException in case of a database error
   * @throws RemoteException in case of remote exception
   */
  public void executeStatement(final String statement) throws DbException, RemoteException;

  /**
   * Executes the given statement
   * @param statement the statement to execute
   * @param outParamType the type of the output param, if any, java.sql.Types.*
   * @throws DbException in case of a database error
   * @throws RemoteException in case of a remote exception
   * @return the return paramter if any, otherwise null
   */
  public Object executeCallable(final String statement, final int outParamType) throws DbException, RemoteException;

  /**
   * Inserts the given entities, returning a list containing the
   * primary keys of the inserted records
   * @param entities the entities to insert
   * @return the primary key values of the inserted entities
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public List<EntityKey> insert(final List<Entity> entities) throws DbException, RemoteException;

  /**
   * Updates the given entities according to their properties
   * @param entities the entities to update
   * @return the updated entities
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public List<Entity> update(final List<Entity> entities) throws DbException, RemoteException;

  /**
   * Deletes the given entities
   * @param entities the entities to delete
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public void delete(final List<Entity> entities) throws DbException, RemoteException;

  /**
   * Selects a single entity
   * @param entityID the Class of the entity to select
   * @param propertyID the ID of the property to use as a condition
   * @param value the value to use in the condition
   * @return an entity of the type <code>entityID</code>, having the
   * value of <code>propertyID</code> as <code>value</code>
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public Entity selectSingle(final String entityID, final String propertyID,
                             final Object value) throws DbException, RemoteException;

  /**
   * Selects the non-null values of the given property for the given entity, returning them distinct and/or ordered
   * @param entityID the class of the Entity
   * @param propertyID the ID of the property
   * @param distinct if true then distinct values are returned
   * @param order if true then the result is ordered according to the entities ordering colomns
   * @return the values in the given column in the given table
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public List<?> selectPropertyValues(final String entityID, final String propertyID,
                                      final boolean distinct, final boolean order) throws DbException, RemoteException;

  /**
   * Selects a single entity
   * @param key the key used in the condition
   * @return an entity of the type <code>entityID</code>, having the key <code>key</code>
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public Entity selectSingle(final EntityKey key) throws DbException, RemoteException;

  /**
   * Selects for update the entity with the given key.
   * The update lock is released when the entity is subsequently updated or via endTransaction(true);
   * @param primaryKeys the keys of the entities to select for update
   * @return the entities selected for update
   * @throws org.jminor.common.db.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.DbException in case the entity is already locked by another user
   * @throws Exception in case of exception
   * @throws RemoteException in case of a remote exception
   */
  public List<Entity> selectForUpdate(final List<EntityKey> primaryKeys) throws Exception;

  /**
   * Selects a single entity according to the specified criteria, throws a DbException
   * if the criteria results in more than one entity
   * @param criteria the criteria specifying the entity to select
   * @return the entities according to the given criteria
   * @throws DbException if an exception occurs
   * @throws org.jminor.common.db.RecordNotFoundException if the entity was not found
   * @throws RemoteException
   */
  public Entity selectSingle(final EntityCriteria criteria) throws DbException, RemoteException;

  /**
   * Returns entities according to <code>keys</code>
   * @param keys the keys used in the condition
   * @return entities according to <code>keys</code>
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public List<Entity> selectMany(final List<EntityKey> keys) throws DbException, RemoteException;

  /**
   * Selects entities according to the specified criteria
   * @param criteria the criteria specifying which entities to select
   * @return entities according to the given criteria
   * @throws DbException
   * @throws RemoteException
   */
  public List<Entity> selectMany(final EntityCriteria criteria) throws DbException, RemoteException;

  /**
   * Selects entities according to one property (<code>propertyID</code>), using <code>values</code> as a condition
   * @param entityID the Class of the entities to select
   * @param propertyID the ID of the condition property
   * @param values the property values to use as condition
   * @return entities of the type <code>entityID</code> according to <code>propertyID</code> and <code>values</code>
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public List<Entity> selectMany(final String entityID, final String propertyID,
                                 final Object... values) throws DbException, RemoteException;

  /**
   * Selects all the entities of the given type
   * @param entityID the Class of the entities to select
   * @return all entities of the given type
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public List<Entity> selectAll(final String entityID) throws DbException, RemoteException;

  /**
   * Selects all the entities of the given type
   * @param entityID the Class of the entities to select
   * @param order if true the result is ordered by the ordering column of <code>entityID</code>
   * @return all entities of the given type
   * @throws DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public List<Entity> selectAll(final String entityID, final boolean order) throws DbException, RemoteException;

  /**
   * Returns the entities that depend on the given entities via foreign keys
   * @param entities the entities for which to retrieve dependencies
   * @return the entities that depend on <code>entities</code>
   * @throws DbException in case of a db exception
   * @throws org.jminor.common.model.UserException in case of a user exception
   * @throws RemoteException in case of a remote exception
   */
  public Map<String,List<Entity>> getDependentEntities(final List<Entity> entities)
          throws DbException, UserException, RemoteException;

  /**
   * Selects the number of rows returned according to the given criteria
   * @param criteria the search criteria
   * @return the number of rows fitting the given criteria
   * @throws org.jminor.common.db.DbException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  public int selectRowCount(final EntityCriteria criteria) throws DbException, RemoteException;

  /**
   * Takes a JasperReport object and returns an initialized JasperPrint object
   * @param report the report to fill
   * @param reportParams the report parameters
   * @return an initialized JasperPrint object
   * @throws net.sf.jasperreports.engine.JRException in case of a report exception
   * @throws RemoteException in case of a remote exception
   */
  public JasperPrint fillReport(final JasperReport report, final Map reportParams)
          throws JRException, RemoteException;
}
