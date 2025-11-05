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

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;

import org.jspecify.annotations.Nullable;

import java.sql.Connection;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.integerValue;

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
	 * Specifies the number of log traces to keep while tracing is enabled.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 50
	 * </ul>
	 */
	PropertyValue<Integer> TRACES = integerValue("codion.db.traces", 50);

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
	 * @throws DatabaseException in case this connection is closed
	 */
	Connection connection();

	/**
	 * @return the underlying database
	 */
	Database database();

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
	 * Sets the internal connection to use, note that no validation or transaction checking is performed
	 * on the connection and auto-commit is assumed to be disabled. The connection is simply used 'as is'.
	 * Note that setting the connection to null causes all methods requiring it to throw a {@link DatabaseException}
	 * until a non-null connection is set.
	 * @param connection the connection
	 */
	void setConnection(@Nullable Connection connection);

	/**
	 * Returns the underlying connection object.
	 * Use {@link #connected()} to verify that the connection is available and valid.
	 * @return the underlying connection object
	 */
	@Nullable Connection getConnection();

	/**
	 * Constructs a new {@link LocalEntityConnection} instance
	 * @param database the Database instance
	 * @param domain the domain model
	 * @param user the user used for connecting to the database
	 * @return a new {@link LocalEntityConnection} instance
	 * @throws DatabaseException in case there is a problem connecting to the database
	 * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
	 */
	static LocalEntityConnection localEntityConnection(Database database, Domain domain, User user) {
		return new DefaultLocalEntityConnection(database, domain, user);
	}

	/**
	 * Constructs a new {@link LocalEntityConnection} instance.
	 * Note that auto-commit is disabled on the given connection.
	 * @param database the Database instance
	 * @param domain the domain model
	 * @param connection the connection object to base the entity connection on, it is assumed to be in a valid state
	 * @return a new {@link LocalEntityConnection} instance, wrapping the given connection
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
