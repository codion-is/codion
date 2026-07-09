/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A remote {@link EntityConnection}, wrapping the connection running on the server.
 * <p>
 * This is the wire contract implemented by the server and adapted to {@link EntityConnection}
 * on the client side. Each method mirrors its {@link EntityConnection} counterpart, adding
 * {@link RemoteException}; refer to {@link EntityConnection} for the behavioral contract.
 */
public interface RemoteEntityConnection extends Remote, AutoCloseable {

	/**
	 * @return the underlying domain entities
	 * @throws RemoteException in case of an exception
	 * @see EntityConnection#entities()
	 */
	Entities entities() throws RemoteException;

	/**
	 * @return the user being used by this connection
	 * @throws RemoteException in case of an exception
	 * @see EntityConnection#user()
	 */
	User user() throws RemoteException;

	/**
	 * @return true if this connection has been established and is valid
	 * @throws RemoteException in case of an exception
	 * @see EntityConnection#connected()
	 */
	boolean connected() throws RemoteException;

	/**
	 * Closes this connection.
	 * @throws RemoteException in case of an exception
	 * @see EntityConnection#close()
	 */
	@Override
	void close() throws RemoteException;

	/**
	 * @return true if a transaction is open, false otherwise
	 * @throws RemoteException in case of exception
	 * @see EntityConnection#transactionOpen()
	 */
	boolean transactionOpen() throws RemoteException;

	/**
	 * Starts a transaction on this connection.
	 * @throws IllegalStateException if a transaction is already open
	 * @throws RemoteException in case of exception
	 * @see EntityConnection#startTransaction()
	 */
	void startTransaction() throws RemoteException;

	/**
	 * Performs a rollback and ends the current transaction.
	 * @throws DatabaseException in case the rollback failed
	 * @throws IllegalStateException in case a transaction is not open
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#rollbackTransaction()
	 */
	void rollbackTransaction() throws RemoteException;

	/**
	 * Performs a commit and ends the current transaction.
	 * @throws DatabaseException in case the commit failed
	 * @throws IllegalStateException in case a transaction is not open
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#commitTransaction()
	 */
	void commitTransaction() throws RemoteException;

	/**
	 * Executes the function with the given type with no parameter.
	 * @param functionType the function type
	 * @param <C> the connection type
	 * @param <P> the parameter type
	 * @param <R> the return value type
	 * @return the function return value
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#execute(FunctionType)
	 */
	<C extends EntityConnection, P, R> @Nullable R execute(FunctionType<C, P, R> functionType) throws RemoteException;

	/**
	 * Executes the function with the given type.
	 * @param functionType the function type
	 * @param parameter the function parameter
	 * @param <C> the connection type
	 * @param <P> the parameter type
	 * @param <R> the return value type
	 * @return the function return value
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#execute(FunctionType, Object)
	 */
	<C extends EntityConnection, P, R> @Nullable R execute(FunctionType<C, P, R> functionType, @Nullable P parameter) throws RemoteException;

	/**
	 * Executes the procedure with the given type with no parameter.
	 * @param procedureType the procedure type
	 * @param <C> the connection type
	 * @param <P> the procedure parameter type
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#execute(ProcedureType)
	 */
	<C extends EntityConnection, P> void execute(ProcedureType<C, P> procedureType) throws RemoteException;

	/**
	 * Executes the procedure with the given type.
	 * @param procedureType the procedure type
	 * @param parameter the procedure parameter
	 * @param <C> the connection type
	 * @param <P> the procedure parameter type
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#execute(ProcedureType, Object)
	 */
	<C extends EntityConnection, P> void execute(ProcedureType<C, P> procedureType, @Nullable P parameter) throws RemoteException;

	/**
	 * Inserts the given entity, returning the primary key.
	 * @param entity the entity to insert
	 * @return the primary key of the inserted entity
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#insert(Entity)
	 */
	Entity.Key insert(Entity entity) throws RemoteException;

	/**
	 * Inserts the given entity, returning the inserted entity.
	 * @param entity the entity to insert
	 * @return the inserted entity
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#insertSelect(Entity)
	 */
	Entity insertSelect(Entity entity) throws RemoteException;

	/**
	 * Inserts the given entities, returning the primary keys.
	 * @param entities the entities to insert
	 * @return the primary keys of the inserted entities
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#insert(Collection)
	 */
	Collection<Entity.Key> insert(Collection<Entity> entities) throws RemoteException;

	/**
	 * Inserts the given entities, returning the inserted entities.
	 * @param entities the entities to insert
	 * @return the inserted entities
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#insertSelect(Collection)
	 */
	Collection<Entity> insertSelect(Collection<Entity> entities) throws RemoteException;

	/**
	 * Updates the given entity based on its attribute values.
	 * @param entity the entity to update
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#update(Entity)
	 */
	void update(Entity entity) throws RemoteException;

	/**
	 * Updates the given entity based on its attribute values, returning the updated entity.
	 * @param entity the entity to update
	 * @return the updated entity
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#updateSelect(Entity)
	 */
	Entity updateSelect(Entity entity) throws RemoteException;

	/**
	 * Updates the given entities based on their attribute values.
	 * @param entities the entities to update
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#update(Collection)
	 */
	void update(Collection<Entity> entities) throws RemoteException;

	/**
	 * Updates the given entities based on their attribute values, returning the updated entities.
	 * @param entities the entities to update
	 * @return the updated entities, in no particular order
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#updateSelect(Collection)
	 */
	Collection<Entity> updateSelect(Collection<Entity> entities) throws RemoteException;

	/**
	 * Performs an update based on the given update.
	 * @param update the update to perform
	 * @return the number of affected rows
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#update(Update)
	 */
	int update(Update update) throws RemoteException;

	/**
	 * Convenience overload accepting an {@link Update.Builder} or any {@link Supplier} of {@link Update},
	 * removing the need for a trailing {@link Update.Builder#build()} call.
	 * @param update the update supplier, typically an {@link Update.Builder}
	 * @return the number of affected rows
	 * @throws RemoteException in case of a remote exception
	 * @see #update(Update)
	 */
	default int update(Supplier<Update> update) throws RemoteException {
		return update(requireNonNull(update).get());
	}

	/**
	 * Deletes the entity with the given primary key.
	 * @param key the primary key of the entity to delete
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#delete(Entity.Key)
	 */
	void delete(Entity.Key key) throws RemoteException;

	/**
	 * Deletes the entities with the given primary keys.
	 * @param keys the primary keys of the entities to delete
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#delete(Collection)
	 */
	void delete(Collection<Entity.Key> keys) throws RemoteException;

	/**
	 * Deletes the entities specified by the given condition.
	 * @param condition the condition specifying the entities to delete
	 * @return the number of deleted rows
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#delete(Condition)
	 */
	int delete(Condition condition) throws RemoteException;

	/**
	 * Selects ordered and distinct non-null values of the given column.
	 * @param column the column
	 * @param <T> the value type
	 * @return all the values of the given column
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#select(Column)
	 */
	<T> List<T> select(Column<T> column) throws RemoteException;

	/**
	 * Selects distinct non-null values of the given column.
	 * @param column column
	 * @param condition the condition
	 * @param <T> the value type
	 * @return the values of the given column
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#select(Column, Condition)
	 */
	<T> List<T> select(Column<T> column, Condition condition) throws RemoteException;

	/**
	 * Selects distinct non-null values of the given column.
	 * @param column the column
	 * @param select the select to perform
	 * @param <T> the value type
	 * @return the values of the given column
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#select(Column, Select)
	 */
	<T> List<T> select(Column<T> column, Select select) throws RemoteException;

	/**
	 * Convenience overload accepting a {@link Select.Builder} or any {@link Supplier} of {@link Select},
	 * removing the need for a trailing {@link Select.Builder#build()} call.
	 * @param column the column for which to retrieve the values
	 * @param select the select supplier, typically a {@link Select.Builder}
	 * @param <T> the column value type
	 * @return the values of the given column
	 * @throws RemoteException in case of a remote exception
	 * @see #select(Column, Select)
	 */
	default <T> List<T> select(Column<T> column, Supplier<Select> select) throws RemoteException {
		return select(column, requireNonNull(select).get());
	}

	/**
	 * Selects an entity by key.
	 * @param key the key of the entity to select
	 * @return an entity having the key {@code key}
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#select(Entity.Key)
	 */
	Entity select(Entity.Key key) throws RemoteException;

	/**
	 * Selects a single entity based on the specified condition.
	 * @param condition the condition specifying the entity to select
	 * @return the entity based on the given condition
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#selectSingle(Condition)
	 */
	Entity selectSingle(Condition condition) throws RemoteException;

	/**
	 * Selects a single entity based on the specified select.
	 * @param select the select to perform
	 * @return the entity based on the given select
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#selectSingle(Select)
	 */
	Entity selectSingle(Select select) throws RemoteException;

	/**
	 * Convenience overload accepting a {@link Select.Builder} or any {@link Supplier} of {@link Select},
	 * removing the need for a trailing {@link Select.Builder#build()} call.
	 * @param select the select supplier, typically a {@link Select.Builder}
	 * @return the entity based on the given select
	 * @throws RemoteException in case of a remote exception
	 * @see #selectSingle(Select)
	 */
	default Entity selectSingle(Supplier<Select> select) throws RemoteException {
		return selectSingle(requireNonNull(select).get());
	}

	/**
	 * Selects entities based on the given {@code keys}.
	 * @param keys the keys used in the condition
	 * @return entities based on {@code keys}
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#select(Collection)
	 */
	Collection<Entity> select(Collection<Entity.Key> keys) throws RemoteException;

	/**
	 * Selects entities based on the given condition.
	 * @param condition the condition specifying which entities to select
	 * @return entities based on the given condition
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#select(Condition)
	 */
	List<Entity> select(Condition condition) throws RemoteException;

	/**
	 * Selects entities based on the given select.
	 * @param select the select to perform
	 * @return entities based on the given select
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#select(Select)
	 */
	List<Entity> select(Select select) throws RemoteException;

	/**
	 * Convenience overload accepting a {@link Select.Builder} or any {@link Supplier} of {@link Select},
	 * removing the need for a trailing {@link Select.Builder#build()} call.
	 * @param select the select supplier, typically a {@link Select.Builder}
	 * @return entities based on the given select
	 * @throws RemoteException in case of a remote exception
	 * @see #select(Select)
	 */
	default List<Entity> select(Supplier<Select> select) throws RemoteException {
		return select(requireNonNull(select).get());
	}

	/**
	 * Selects the entities that depend on the given entities via (non-soft) foreign keys, mapped to corresponding entityTypes.
	 * @param entities the entities for which to retrieve dependencies
	 * @return the entities that depend on {@code entities}
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#dependencies(Collection)
	 */
	Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities) throws RemoteException;

	/**
	 * Counts the number of rows returned based on the given count conditions.
	 * @param count the count conditions
	 * @return the number of rows fitting the given count conditions
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#count(Count)
	 */
	int count(Count count) throws RemoteException;

	/**
	 * Convenience overload accepting a {@link Count.Builder} or any {@link Supplier} of {@link Count},
	 * removing the need for a trailing {@link Count.Builder#build()} call.
	 * @param count the count supplier, typically a {@link Count.Builder}
	 * @return the number of rows fitting the given count conditions
	 * @throws RemoteException in case of a remote exception
	 * @see #count(Count)
	 */
	default int count(Supplier<Count> count) throws RemoteException {
		return count(requireNonNull(count).get());
	}

	/**
	 * Fills the given report using a JDBC datasource and returns the result.
	 * @param reportType the report to fill
	 * @param parameter the report parameter, if any
	 * @param <T> the report type
	 * @param <P> the report parameters type
	 * @param <R> the report result type
	 * @return the filled result object
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#report(ReportType, Object)
	 */
	<T, P, R> R report(ReportType<T, P, R> reportType, @Nullable P parameter) throws RemoteException;

	/**
	 * Returns a result set iterator based on the given query condition.
	 * Note that the returned iterator is wrapped to present the {@link EntityResultIterator}
	 * interface to client code.
	 * @param condition the query condition
	 * @return an iterator for the given query condition
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#iterator(Condition)
	 */
	RemoteEntityResultIterator iterator(Condition condition) throws RemoteException;

	/**
	 * Returns a result set iterator based on the given select.
	 * Note that the returned iterator is wrapped to present the {@link EntityResultIterator}
	 * interface to client code.
	 * @param select the query select
	 * @return an iterator for the given query select
	 * @throws RemoteException in case of a remote exception
	 * @see EntityConnection#iterator(Select)
	 */
	RemoteEntityResultIterator iterator(Select select) throws RemoteException;

	/**
	 * Convenience overload accepting a {@link Select.Builder} or any {@link Supplier} of {@link Select},
	 * removing the need for a trailing {@link Select.Builder#build()} call.
	 * @param select the select supplier, typically a {@link Select.Builder}
	 * @return an iterator for the given query select
	 * @throws RemoteException in case of a remote exception
	 * @see #iterator(Select)
	 */
	default RemoteEntityResultIterator iterator(Supplier<Select> select) throws RemoteException {
		return iterator(requireNonNull(select).get());
	}
}
