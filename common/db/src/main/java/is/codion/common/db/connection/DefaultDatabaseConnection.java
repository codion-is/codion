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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A default DatabaseConnection implementation, which wraps a standard JDBC Connection object.
 * This class is not thread-safe.
 */
final class DefaultDatabaseConnection implements DatabaseConnection {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultDatabaseConnection.class);

	private static final Map<String, User> META_DATA_USER_CACHE = new ConcurrentHashMap<>();

	private final User user;
	private final Database database;

	private @Nullable Connection connection;
	private boolean transactionOpen = false;

	/**
	 * Constructs a new DefaultDatabaseConnection instance, initialized and ready for use.
	 * @param database the database
	 * @param user the user to base this database connection on
	 * @throws DatabaseException in case there is a problem connecting to the database
	 * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
	 */
	DefaultDatabaseConnection(Database database, User user) {
		this.database = requireNonNull(database);
		this.connection = disableAutoCommit(database.createConnection(user));
		this.user = requireNonNull(user);
	}

	/**
	 * Constructs a new DefaultDatabaseConnection instance, based on the given Connection object.
	 * NB. auto commit is disabled on the Connection that is provided.
	 * @param database the database
	 * @param connection the Connection object to base this DefaultDatabaseConnection on
	 * @throws DatabaseException in case of an exception while retrieving the username from the connection meta-data
	 */
	DefaultDatabaseConnection(Database database, Connection connection) {
		this.database = requireNonNull(database);
		this.connection = disableAutoCommit(connection);
		this.user = user(connection);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + user.username();
	}

	@Override
	public User user() {
		return user;
	}

	@Override
	public void close() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.rollback();
			}
		}
		catch (SQLException ex) {
			LOG.warn("Failed to rollback during connection close, connection may be invalid: {}", ex.getMessage());
		}
		try {
			if (connection != null) {
				connection.close();
			}
		}
		catch (Exception ignored) {/*ignored*/}
		connection = null;
		transactionOpen = false;
	}

	@Override
	public boolean connected() {
		return connection != null;
	}

	@Override
	public boolean valid() {
		return connection != null && database.connectionValid(connection);
	}

	@Override
	public void setConnection(@Nullable Connection connection) {
		this.connection = connection;
	}

	@Override
	public Connection getConnection() {
		return verifyOpenConnection();
	}

	@Override
	public Database database() {
		return database;
	}

	@Override
	public void startTransaction() {
		if (transactionOpen) {
			throw new IllegalStateException("Transaction already open");
		}
		connection = verifyOpenConnection();
		transactionOpen = true;
	}

	@Override
	public void rollbackTransaction() throws SQLException {
		if (!transactionOpen) {
			throw new IllegalStateException("Transaction is not open");
		}
		connection = verifyOpenConnection();
		try {
			connection.rollback();
		}
		finally {
			transactionOpen = false;
		}
	}

	@Override
	public void commitTransaction() throws SQLException {
		if (!transactionOpen) {
			throw new IllegalStateException("Transaction is not open");
		}
		connection = verifyOpenConnection();
		try {
			connection.commit();
		}
		finally {
			transactionOpen = false;
		}
	}

	@Override
	public boolean transactionOpen() {
		return transactionOpen;
	}

	@Override
	public void commit() throws SQLException {
		if (transactionOpen) {
			throw new IllegalStateException("Can not perform a commit during an open transaction, use 'commitTransaction()'");
		}
		connection = verifyOpenConnection();
		try {
			connection.commit();
		}
		catch (SQLException e) {
			LOG.error("Exception during commit for user {}: {}", user.username(), e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public void rollback() throws SQLException {
		if (transactionOpen) {
			throw new IllegalStateException("Can not perform a rollback during an open transaction, use 'rollbackTransaction()'");
		}
		connection = verifyOpenConnection();
		try {
			connection.rollback();
		}
		catch (SQLException e) {
			LOG.error("Exception during rollback for user {}: {}", user.username(), e.getMessage(), e);
			throw e;
		}
	}

	private Connection verifyOpenConnection() {
		if (connection == null) {
			throw new IllegalStateException("Connection is closed");
		}

		return connection;
	}

	/**
	 * Disables auto-commit on the given connection and returns it.
	 * @param connection the connection
	 * @return the connection with auto-commit disabled
	 * @throws DatabaseException in case disabling auto-commit fails
	 */
	private static Connection disableAutoCommit(Connection connection) {
		requireNonNull(connection);
		try {
			connection.setAutoCommit(false);

			return connection;
		}
		catch (SQLException e) {
			LOG.error("Unable to disable auto commit on connection, assuming invalid state", e);
			throw new DatabaseException(e, "Failed to configure database connection: Unable to disable auto-commit");
		}
	}

	/**
	 * Returns a User with the username from the meta-data retrieved from the given connection
	 * @param connection the connection
	 * @return a user based on the information gleamed from the given connection
	 * @throws DatabaseException in case of an exception while retrieving the username from the connection meta-data
	 * @see java.sql.DatabaseMetaData#getUserName()
	 */
	private static User user(Connection connection) {
		try {
			return META_DATA_USER_CACHE.computeIfAbsent(connection.getMetaData().getUserName(), User::user);
		}
		catch (SQLException e) {
			throw new DatabaseException(e, "Failed to retrieve database username from connection metadata. Connection may be invalid or database may not support getUserName()");
		}
	}
}