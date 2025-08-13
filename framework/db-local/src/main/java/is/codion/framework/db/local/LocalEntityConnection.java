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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;

import java.sql.Connection;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.integerValue;

/**
 * EntityConnection implementation based on a local JDBC connection.
 * {@snippet :
 * Domain domain = new Domain();
 * Database database = new H2DatabaseFactory().createDatabase("jdbc:h2:file:/path/to/database");
 * User user = User.parse("scott:tiger");
 *
 * try (EntityConnection connection = localEntityConnection(database, domain, user)) {
 *   List<Entity> customers = connection.select(Condition.all(Customer.TYPE));
 * }
 *}
 * A factory for LocalEntityConnection instances.
 */
public interface LocalEntityConnection extends EntityConnection {

	/**
	 * The default number of log entries to keep.
	 */
	int DEFAULT_CONNECTION_LOG_SIZE = 40;

	/**
	 * Specifies the size of the (circular) log that is kept in memory for each connection
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 40
	 * </ul>
	 */
	PropertyValue<Integer> CONNECTION_LOG_SIZE = integerValue("codion.db.connectionLogSize", DEFAULT_CONNECTION_LOG_SIZE);

	/**
	 * Specifies the query timeout in seconds
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 120
	 * </ul>
	 */
	PropertyValue<Integer> QUERY_TIMEOUT = integerValue("codion.db.queryTimeout", 120);

	/**
	 * Specifies whether optimistic locking should be performed, that is, if entities should
	 * be selected for update and checked for modification before being updated
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> OPTIMISTIC_LOCKING = booleanValue("codion.db.optimisticLocking", true);

	/**
	 * Specifies whether the foreign key value graph should be fully populated instead of
	 * being limited by the foreign key reference depth setting
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> LIMIT_FOREIGN_KEY_REFERENCE_DEPTH = booleanValue("codion.db.limitForeignKeyReferenceDepth", true);

	/**
	 * @return the underlying connection
	 */
	DatabaseConnection databaseConnection();

	/**
	 * Returns a result set iterator based on the given query condition.
	 * Remember to use try with resources or to call {@link ResultIterator#close()} in order to close underlying resources.
	 * @param condition the query condition
	 * @return an iterator for the given query condition
	 * @throws DatabaseException in case of an exception
	 */
	ResultIterator<Entity> iterator(Condition condition);

	/**
	 * Returns a result set iterator based on the given select.
	 * Remember to use try with resources or to call {@link ResultIterator#close()} in order to close underlying resources.
	 * @param select the query select
	 * @return an iterator for the given query select
	 * @throws DatabaseException in case of an exception
	 */
	ResultIterator<Entity> iterator(Select select);

	/**
	 * @return true if optimistic locking is enabled
	 */
	boolean optimisticLocking();

	/**
	 * @param optimisticLocking true if optimistic locking should be enabled
	 */
	void optimisticLocking(boolean optimisticLocking);

	/**
	 * @return true if foreign key reference depths are being limited
	 */
	boolean limitForeignKeyReferenceDepth();

	/**
	 * @param limitForeignKeyReferenceDepth false to override the reference depth limit specified by conditions or entities
	 * @see Select.Builder#referenceDepth(int)
	 */
	void limitForeignKeyReferenceDepth(boolean limitForeignKeyReferenceDepth);

	/**
	 * @return the default query timeout being used
	 */
	int queryTimeout();

	/**
	 * @param queryTimeout the query timeout in seconds
	 */
	void queryTimeout(int queryTimeout);

	/**
	 * Constructs a new LocalEntityConnection instance
	 * @param database the Database instance
	 * @param domain the domain model
	 * @param user the user used for connecting to the database
	 * @return a new LocalEntityConnection instance
	 * @throws DatabaseException in case there is a problem connecting to the database
	 * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
	 */
	static LocalEntityConnection localEntityConnection(Database database, Domain domain, User user) {
		return new DefaultLocalEntityConnection(database, domain, user);
	}

	/**
	 * Constructs a new LocalEntityConnection instance
	 * @param database the Database instance
	 * @param domain the domain model
	 * @param connection the connection object to base the entity connection on, it is assumed to be in a valid state
	 * @return a new LocalEntityConnection instance, wrapping the given connection
	 * @throws DatabaseException in case there is a problem with the supplied connection
	 */
	static LocalEntityConnection localEntityConnection(Database database, Domain domain, Connection connection) {
		return new DefaultLocalEntityConnection(database, domain, connection);
	}

	/**
	 * Runs the database configuration for the given domain on the given database.
	 * Prevents multiple runs for the same domain/database combination.
	 * @param database the database to configure
	 * @param domain the domain doing the configuring
	 * @return the Database instance
	 * @throws DatabaseException in case of an exception
	 */
	static Database configureDatabase(Database database, Domain domain) {
		return DefaultLocalEntityConnection.configureDatabase(database, domain);
	}
}
