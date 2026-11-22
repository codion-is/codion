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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.database;

import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ErrorType;
import is.codion.common.db.exception.Operation;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.common.utilities.user.User;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A default abstract implementation of the Database interface.
 */
public abstract class AbstractDatabase implements Database {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractDatabase.class);

	/**
	 * {@code FOR UPDATE}
	 */
	protected static final String FOR_UPDATE = "FOR UPDATE";

	/**
	 * {@code FOR UPDATE NOWAIT}
	 */
	protected static final String FOR_UPDATE_NOWAIT = "FOR UPDATE NOWAIT";

	private static final String USERNAME = "username";
	private static final String FETCH_NEXT = "FETCH NEXT ";
	private static final String ROWS = " ROWS";
	private static final String ONLY = " ONLY";
	private static final String OFFSET = "OFFSET ";
	private static final String LIMIT = "LIMIT ";

	static {
		DriverManager.setLoginTimeout(Database.LOGIN_TIMEOUT.getOrThrow());
	}

	private static @Nullable Database instance;

	private final Map<String, ConnectionPoolWrapper> connectionPools = new HashMap<>();
	private final int validityCheckTimeout = CONNECTION_VALIDITY_CHECK_TIMEOUT.getOrThrow();
	private final @Nullable Integer transactionIsolation = TRANSACTION_ISOLATION.get();
	private final DefaultQueryCounter queryCounter = new DefaultQueryCounter();
	private final boolean legacyJdbc = LEGACY_JDBC.getOrThrow();
	private final Map<Integer, GetValue<?>> getters = ColumnValues.getters(legacyJdbc);
	private final Map<Integer, SetValue<?>> setters = ColumnValues.setters();
	private final String url;

	private ConnectionProvider connectionProvider = new ConnectionProvider() {};

	/**
	 * Instantiates a new AbstractDatabase.
	 * @param url the jdbc url
	 */
	protected AbstractDatabase(String url) {
		this.url = requireNonNull(url);
	}

	@Override
	public final String url() {
		return url;
	}

	@Override
	public final Connection createConnection() {
		try {
			return configured(connectionProvider.connection(url));
		}
		catch (SQLException e) {
			throw exception(e, Operation.OTHER);
		}
	}

	@Override
	public final Connection createConnection(User user) {
		try {
			return configured(connectionProvider.connection(user, url));
		}
		catch (SQLException e) {
			throw exception(e, Operation.OTHER);
		}
	}

	private Connection configured(Connection connection) throws SQLException {
		try {
			if (transactionIsolation != null) {
				connection.setTransactionIsolation(transactionIsolation);
			}
			configure(connection);

			return connection;
		}
		catch (SQLException | RuntimeException e) {
			try {
				connection.close();
			}
			catch (SQLException closing) {
				e.addSuppressed(closing);
			}
			throw e;
		}
	}

	@Override
	public final boolean connectionValid(Connection connection) {
		requireNonNull(connection);
		try {
			return connection.isValid(validityCheckTimeout);
		}
		catch (SQLException e) {
			return false;
		}
	}

	@Override
	public final QueryCounter queryCounter() {
		return queryCounter;
	}

	@Override
	public final Statistics statistics() {
		return queryCounter.collectAndResetStatistics();
	}

	@Override
	public final ConnectionPoolWrapper createConnectionPool(ConnectionPoolFactory connectionPoolFactory,
																													User poolUser) {
		requireNonNull(connectionPoolFactory, "connectionPoolFactory");
		requireNonNull(poolUser, "poolUser");
		String usernameKey = poolUser.username().toLowerCase(Locale.ROOT);
		if (connectionPools.containsKey(usernameKey)) {
			throw new IllegalStateException("Connection pool for user '" + poolUser.username() +
							"' already exists. Use connectionPool(String) to retrieve existing pool.");
		}
		ConnectionPoolWrapper connectionPool = connectionPoolFactory.createConnectionPool(this, poolUser);
		connectionPools.put(usernameKey, connectionPool);

		return connectionPool;
	}

	@Override
	public final boolean containsConnectionPool(String username) {
		return connectionPools.containsKey(requireNonNull(username, USERNAME).toLowerCase(Locale.ROOT));
	}

	@Override
	public final ConnectionPoolWrapper connectionPool(String username) {
		requireNonNull(username, USERNAME);
		ConnectionPoolWrapper connectionPoolWrapper = connectionPools.get(username.toLowerCase(Locale.ROOT));
		if (connectionPoolWrapper == null) {
			throw new IllegalArgumentException("No connection pool found for user '" + username +
							"'. Available pools: " + connectionPools.keySet());
		}

		return connectionPoolWrapper;
	}

	@Override
	public final void closeConnectionPool(String username) {
		requireNonNull(username, USERNAME);
		ConnectionPoolWrapper connectionPoolWrapper = connectionPools.remove(username.toLowerCase(Locale.ROOT));
		if (connectionPoolWrapper != null) {
			connectionPoolWrapper.close();
		}
	}

	@Override
	public final void closeConnectionPools() {
		for (ConnectionPoolWrapper pool : new ArrayList<>(connectionPools.values())) {
			closeConnectionPool(pool.user().username());
		}
	}

	@Override
	public final Collection<String> connectionPoolUsernames() {
		return unmodifiableList(new ArrayList<>(connectionPools.keySet()));
	}

	@Override
	public final void connectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = requireNonNull(connectionProvider);
	}

	@Override
	public GetValue<?> getter(int sqlType) {
		GetValue<?> getter = getters.get(sqlType);
		if (getter == null) {
			throw new IllegalArgumentException("No getter available for SQL type: " + sqlType);
		}

		return getter;
	}

	@Override
	public <T> GetValue<T> getter(Class<T> type) {
		return ColumnValues.getter(requireNonNull(type), legacyJdbc);
	}

	@Override
	public SetValue<?> setter(int sqlType) {
		SetValue<?> setter = setters.get(sqlType);
		if (setter == null) {
			throw new IllegalArgumentException("No setter available for SQL type: " + sqlType);
		}

		return setter;
	}

	@Override
	public String selectForUpdateTableHint(Lock lock) {
		return "";
	}

	@Override
	public boolean releasesSavepoints() {
		return true;
	}

	@Override
	public int maximumParameters() {
		return Integer.MAX_VALUE;
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		throw new UnsupportedOperationException("Sequence support is not implemented for database type: " + getClass().getSimpleName() +
						". Use auto-increment columns or implement sequenceQuery() method.");
	}

	/**
	 * Returns an exception based on the {@link ErrorType} of the given exception, see {@link #errorType(SQLException)},
	 * carrying the error type along with the detail, if any, see {@link #errorDetail(SQLException, ErrorType)},
	 * its message the one associated with the error type, in the language of the one reading it, see
	 * {@link DatabaseException#getMessage()}, or the exception message in case the error type
	 * is not recognized, see {@link #message(SQLException)}.
	 */
	@Override
	public final DatabaseException exception(SQLException exception, Operation operation) {
		requireNonNull(exception, "exception");
		requireNonNull(operation, "operation");
		ErrorType errorType = recognize(exception);
		if (errorType == null) {
			return new DatabaseException(exception, cleanMessage(exception));
		}
		String detail = detail(exception, errorType);
		switch (errorType) {
			case UNIQUE_CONSTRAINT:
				return new UniqueConstraintException(exception, errorType, detail);
			case REFERENTIAL_INTEGRITY:
			case PARENT_MISSING:
			case CHILD_EXISTS:
				return new ReferentialIntegrityException(exception, errorType, detail, operation);
			case TIMEOUT:
				return new QueryTimeoutException(exception, errorType, detail);
			case AUTHENTICATION:
			case ACCOUNT_LOCKED:
			case PASSWORD_EXPIRED:
				return new AuthenticationException(errorType, detail);
			default:
				return new DatabaseException(exception, errorType, detail);
		}
	}

	@Override
	public final void close() {
		synchronized (AbstractDatabase.class) {
			if (AbstractDatabase.instance == this) {
				AbstractDatabase.instance = null;
			}
			try {
				closeConnectionPools();
			}
			catch (Exception e) {
				LOG.error("Error closing connection pools", e);
			}
			try {
				closeDatabase();
			}
			catch (Exception e) {
				LOG.error("Error closing database", e);
			}
		}
	}

	/**
	 * Returns the type of error the given exception represents, null if it is not recognized.
	 * This default implementation is based on the SQL states databases agree on, and on {@link SQLTimeoutException}.
	 * Databases reporting their own error codes override, falling back on this one for the ones not recognized.
	 * Note that this is called while an exception is being handled, an exception thrown here is logged and ignored.
	 * @param exception the exception
	 * @return the error type, null if not recognized
	 */
	protected @Nullable ErrorType errorType(SQLException exception) {
		if (exception instanceof SQLTimeoutException) {
			return ErrorType.TIMEOUT;
		}
		String sqlState = exception.getSQLState();
		if (sqlState == null) {
			return null;
		}
		switch (sqlState) {
			case "23505":
				return ErrorType.UNIQUE_CONSTRAINT;
			case "23503":
				return ErrorType.REFERENTIAL_INTEGRITY;
			case "23504":
				return ErrorType.CHILD_EXISTS;
			case "23502":
				return ErrorType.NULL_VALUE;
			case "23513":
			case "23514":
				return ErrorType.CHECK_CONSTRAINT;
			case "22001":
			case "22003":
				return ErrorType.VALUE_TOO_LARGE;
			case "28000":
				return ErrorType.AUTHENTICATION;
			default:
				return null;
		}
	}

	/**
	 * Returns the detail to append to the message associated with the given error type, the name of the
	 * column missing a value for example, null if none is available, which is the default.
	 * Note that this is called while an exception is being handled, an exception thrown here is logged and ignored.
	 * @param exception the exception
	 * @param errorType the error type
	 * @return the detail, null if none is available
	 */
	protected @Nullable String errorDetail(SQLException exception, ErrorType errorType) {
		return null;
	}

	/**
	 * Returns the message to present for an exception of an unrecognized type, by default the exception message.
	 * Override to remove what the driver or database adds to it, the statement or links to documentation for example.
	 * Note that this is called while an exception is being handled, an exception thrown here is logged and ignored.
	 * @param exception the exception
	 * @return the message
	 */
	protected @Nullable String message(SQLException exception) {
		return exception.getMessage();
	}

	/**
	 * Closes the database, releasing any in memory storage or other resources.
	 * @throws SQLException in case of an exception
	 */
	protected void closeDatabase() throws SQLException {}

	/**
	 * Configures a connection once created, before {@link #createConnection()} or {@link #createConnection(User)}
	 * returns it, for a database requiring setup per connection, SQLite attaching its schemas for example.
	 * The default implementation does nothing.
	 * @param connection the connection, created by the {@link ConnectionProvider}
	 * @throws SQLException in case of an exception, the connection then closed
	 */
	protected void configure(Connection connection) throws SQLException {/*for overriding*/}

	/**
	 * <p>Sets a single client info property, swallowing the exception a driver throws for a property it does
	 * not recognize - a stamp is a nicety, not a precondition for using the connection, see
	 * {@link Database#clientInfo(Connection, ClientInfo)}. For subclasses implementing that.
	 * <p>Logged at debug rather than warn: a driver which refuses one property refuses it on every check out,
	 * and a warning per database call is worse than the missing stamp.
	 * @param connection the connection
	 * @param property the client info property name
	 * @param value the value, an empty String to clear it, null values being rejected by some drivers
	 */
	protected static void clientInfoProperty(Connection connection, String property, String value) {
		try {
			connection.setClientInfo(requireNonNull(property), requireNonNull(value));
		}
		catch (RuntimeException | SQLException e) {
			LOG.debug("Unable to set client info property '{}'", property, e);
		}
	}

	static Database instance() {
		String databaseUrl = URL.getOrThrow();
		try {
			synchronized (AbstractDatabase.class) {
				if (AbstractDatabase.instance == null) {
					AbstractDatabase.instance = DatabaseFactory.instance().create(databaseUrl);
				}
				else if (!databaseUrl.equals(AbstractDatabase.instance.url())) {
					if (!URL_SCOPED_INSTANCE.getOrThrow()) {
						throw new DatabaseException("Database URL conflict: Cannot change from '" + AbstractDatabase.instance.url() +
										"' to '" + databaseUrl + "'. Enable 'codion.db.urlScopedInstance' to allow multiple database URLs.");
					}
					AbstractDatabase.instance = cleanupAndCreateInstance(databaseUrl, AbstractDatabase.instance);
				}

				return AbstractDatabase.instance;
			}
		}
		catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}

	/**
	 * Creates a limit/offset clause of the form {@code LIMIT {limit} OFFSET {offset}}.
	 * Returns a partial clause if either value is null.
	 * If both values are null, an empty string is returned.
	 * @param limit the limit, may be null
	 * @param offset the offset, may be null
	 * @return a limit/offset clause
	 */
	protected static String createLimitOffsetClause(@Nullable Integer limit, @Nullable Integer offset) {
		return createLimitOffsetClause(limit, offset, null);
	}

	/**
	 * Creates a limit/offset clause of the form {@code LIMIT {limit} OFFSET {offset}},
	 * for databases which do not accept an offset without a limit.
	 * If both values are null, an empty string is returned.
	 * @param limit the limit, may be null
	 * @param offset the offset, may be null
	 * @param noLimit the limit to use in case only the offset is specified, null for none
	 * @return a limit/offset clause
	 */
	protected static String createLimitOffsetClause(@Nullable Integer limit, @Nullable Integer offset, @Nullable String noLimit) {
		StringBuilder builder = new StringBuilder();
		if (limit != null) {
			builder.append(LIMIT).append(limit);
		}
		else if (offset != null && noLimit != null) {
			builder.append(LIMIT).append(noLimit);
		}
		if (offset != null) {
			builder.append(builder.isEmpty() ? "" : " ").append(OFFSET).append(offset);
		}

		return builder.toString();
	}

	/**
	 * Creates an offset/fetch next clause of the form {@code OFFSET {offset} ROWS FETCH NEXT {limit} ROWS ONLY}.
	 * Returns a partial clause if either value is null.
	 * If both values are null, an empty string is returned.
	 * @param limit the limit, may be null
	 * @param offset the offset, may be null
	 * @return a limit/offset clause
	 */
	protected static String createOffsetFetchNextClause(@Nullable Integer limit, @Nullable Integer offset) {
		StringBuilder builder = new StringBuilder();
		if (offset != null) {
			builder.append(OFFSET).append(offset).append(ROWS);
		}
		if (limit != null) {
			builder.append(builder.isEmpty() ? "" : " ").append(FETCH_NEXT).append(limit).append(ROWS).append(ONLY);
		}

		return builder.toString();
	}

	/**
	 * Removes the given prefixes along with any options and parameters from the given jdbc url.
	 * @param url the url
	 * @param prefixes the prefixes to remove
	 * @return the given url without prefixes, options and parameters
	 */
	protected static String removeUrlPrefixOptionsAndParameters(String url, String... prefixes) {
		String result = url;
		for (String prefix : prefixes) {
			if (url.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
				result = url.substring(prefix.length());
				break;
			}
		}
		if (result.contains(";")) {
			result = result.substring(0, result.indexOf(';'));
		}
		if (result.contains("?")) {
			result = result.substring(0, result.indexOf('?'));
		}

		return result;
	}

	/**
	 * Returns the database from a url of the form {@code //host:port/database}, stripped of prefix, options and parameters,
	 * see {@link #removeUrlPrefixOptionsAndParameters(String, String...)}, that is, what follows the last slash.
	 * In case no database is specified, {@code //host:port/} or {@code //host:port}, the host and port are returned.
	 * @param url the url, without prefix, options and parameters
	 * @return the database, or the host in case no database is specified
	 */
	protected static String databaseOrHost(String url) {
		int slashIndex = url.lastIndexOf('/');
		String database = url.substring(slashIndex + 1);
		if (!database.isEmpty() || slashIndex == -1) {
			return database;
		}
		String host = url.substring(0, slashIndex);

		return host.substring(host.lastIndexOf('/') + 1);
	}

	/**
	 * Returns the text found between the given prefix and the suffix following it, for picking the name of a
	 * constraint or column from an exception message, see {@link #errorDetail(SQLException, ErrorType)}.
	 * @param text the text
	 * @param prefix the prefix
	 * @param suffix the suffix
	 * @return the text between the first occurrence of the prefix and the suffix following it, null if either is not found
	 */
	protected static @Nullable String between(String text, String prefix, String suffix) {
		int prefixIndex = requireNonNull(text).indexOf(requireNonNull(prefix));
		if (prefixIndex == -1) {
			return null;
		}
		int beginIndex = prefixIndex + prefix.length();
		int endIndex = text.indexOf(requireNonNull(suffix), beginIndex);

		return endIndex == -1 ? null : text.substring(beginIndex, endIndex);
	}

	private @Nullable ErrorType recognize(SQLException exception) {
		try {
			return errorType(exception);
		}
		catch (RuntimeException e) {
			LOG.debug("Unable to determine the error type", e);
			return null;
		}
	}

	private @Nullable String detail(SQLException exception, ErrorType errorType) {
		try {
			return errorDetail(exception, errorType);
		}
		catch (RuntimeException e) {
			LOG.debug("Unable to extract the error detail", e);
			return null;
		}
	}

	private @Nullable String cleanMessage(SQLException exception) {
		try {
			return message(exception);
		}
		catch (RuntimeException e) {
			LOG.debug("Unable to clean the exception message", e);
			return exception.getMessage();
		}
	}

	private static Database cleanupAndCreateInstance(String databaseUrl, Database previousInstance) throws SQLException {
		Database instance = DatabaseFactory.instance().create(databaseUrl);
		if (previousInstance != null) {
			previousInstance.close();
		}

		return instance;
	}

	private static final class DefaultQueryCounter implements QueryCounter {

		private static final double THOUSAND = 1000d;

		private final AtomicLong queriesPerSecondTime = new AtomicLong(System.currentTimeMillis());
		private final AtomicInteger queriesPerSecondCounter = new AtomicInteger();
		private final AtomicInteger selectsPerSecondCounter = new AtomicInteger();
		private final AtomicInteger insertsPerSecondCounter = new AtomicInteger();
		private final AtomicInteger updatesPerSecondCounter = new AtomicInteger();
		private final AtomicInteger deletesPerSecondCounter = new AtomicInteger();
		private final AtomicInteger otherPerSecondCounter = new AtomicInteger();

		private final boolean enabled = COUNT_QUERIES.getOrThrow();

		@Override
		public void select() {
			if (enabled) {
				selectsPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		@Override
		public void insert() {
			if (enabled) {
				insertsPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		@Override
		public void update() {
			if (enabled) {
				updatesPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		@Override
		public void delete() {
			if (enabled) {
				deletesPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		@Override
		public void other() {
			if (enabled) {
				otherPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		private Database.Statistics collectAndResetStatistics() {
			long currentTime = System.currentTimeMillis();
			double seconds = (currentTime - queriesPerSecondTime.getAndSet(currentTime)) / THOUSAND;
			if (seconds > 0) {
				int queriesPerSecond = (int) (queriesPerSecondCounter.getAndSet(0) / seconds);
				int selectsPerSecond = (int) (selectsPerSecondCounter.getAndSet(0) / seconds);
				int insertsPerSecond = (int) (insertsPerSecondCounter.getAndSet(0) / seconds);
				int deletesPerSecond = (int) (deletesPerSecondCounter.getAndSet(0) / seconds);
				int updatesPerSecond = (int) (updatesPerSecondCounter.getAndSet(0) / seconds);
				int otherPerSecond = (int) (otherPerSecondCounter.getAndSet(0) / seconds);

				return new DefaultDatabaseStatistics(currentTime, queriesPerSecond, selectsPerSecond,
								insertsPerSecond, deletesPerSecond, updatesPerSecond, otherPerSecond);
			}

			return new DefaultDatabaseStatistics();
		}
	}

	/**
	 * A default Database.Statistics implementation.
	 */
	private static final class DefaultDatabaseStatistics implements Database.Statistics, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final long timestamp;
		private final int queriesPerSecond;
		private final int selectsPerSecond;
		private final int insertsPerSecond;
		private final int deletesPerSecond;
		private final int updatesPerSecond;
		private final int otherPerSecond;

		private DefaultDatabaseStatistics() {
			this(0, 0, 0, 0, 0, 0, 0);
		}

		private DefaultDatabaseStatistics(long timestamp, int queriesPerSecond, int selectsPerSecond,
																			int insertsPerSecond, int deletesPerSecond, int updatesPerSecond,
																			int otherPerSecond) {
			this.timestamp = timestamp;
			this.queriesPerSecond = queriesPerSecond;
			this.selectsPerSecond = selectsPerSecond;
			this.insertsPerSecond = insertsPerSecond;
			this.deletesPerSecond = deletesPerSecond;
			this.updatesPerSecond = updatesPerSecond;
			this.otherPerSecond = otherPerSecond;
		}

		@Override
		public int queriesPerSecond() {
			return queriesPerSecond;
		}

		@Override
		public int deletesPerSecond() {
			return deletesPerSecond;
		}

		@Override
		public int insertsPerSecond() {
			return insertsPerSecond;
		}

		@Override
		public int selectsPerSecond() {
			return selectsPerSecond;
		}

		@Override
		public int updatesPerSecond() {
			return updatesPerSecond;
		}

		@Override
		public int otherPerSecond() {
			return otherPerSecond;
		}

		@Override
		public long timestamp() {
			return timestamp;
		}
	}

	/**
	 * The init scripts of a database, {@link Database#INIT_SCRIPTS}, each run once in the database's life, for a dialect
	 * running them to provide the record of the scripts run and how a script runs.
	 * <ul>
	 *   <li>a new database runs them all, in the order listed;
	 *   <li>an existing database runs those it has not yet run, in the order listed, so a script added to the list
	 *   brings it up to date;
	 *   <li>each script run is recorded in the {@link #table()}, by its path as listed, with a SHA-256 checksum of its
	 *   content, line endings aside;
	 *   <li>a script moved, listed by another path, with the content of one recorded but no longer listed, does not run
	 *   again, its record taking the new path.
	 * </ul>
	 * The database is refused with a {@link DatabaseException}, and left as it was, where a script has changed since it ran,
	 * where a script has run but is no longer listed, or where a script not yet run is listed before one that has.
	 * <p>
	 * An existing database without the record, created before the scripts were recorded, or with none listed, ran none
	 * since its creation, so the first time it records every script listed as run, running none.
	 */
	protected abstract static class InitScripts {

		private static final String INIT_SCRIPT = "Init script ";
		private static final String CHECKSUM_ALGORITHM = "SHA-256";
		private static final int READ_BUFFER_SIZE = 8192;

		private final String table;
		private final String selectScripts;
		private final String insertScript;
		private final String updatePath;

		/**
		 * @param table the table recording the scripts run, with the columns {@code path}, a varchar primary key,
		 * {@code checksum}, 32 bytes, and {@code run_at}, a timestamp defaulting to the current one
		 */
		protected InitScripts(String table) {
			this.table = requireNonNull(table);
			this.selectScripts = "select path, checksum from " + table;
			this.insertScript = "insert into " + table + " (path, checksum) values (?, ?)";
			this.updatePath = "update " + table + " set path = ? where path = ?";
		}

		/**
		 * @return the table recording the scripts run
		 */
		public final String table() {
			return table;
		}

		/**
		 * Runs the scripts not yet run, in the order listed, each recorded with a checksum of its content once run, after
		 * checking that each one run is unchanged and still listed, and that none not yet run is listed before one that has,
		 * so that a database brought up to date one list at a time ends up as one created from the last. A database refused
		 * is left as it was, a moved script's record included.
		 * @param connection the connection, its auto-commit disabled by this method
		 * @param scriptPaths the scripts, in the order to run
		 * @param existing true in case the database existed before, false in case it is being created
		 * @throws DatabaseException in case the database is refused, a script can not be read or fails
		 * @throws SQLException in case of an exception
		 */
		public final void run(Connection connection, List<String> scriptPaths, boolean existing) throws SQLException {
			List<String> sanitizedPaths = sanitize(scriptPaths);
			connection.setAutoCommit(false);
			// An existing database without the record was created before the scripts were recorded, or with none listed,
			// and has run none since, so those listed are recorded as run, leaving it as it was, those added later then
			// running. The checksums before the table, whose creation may commit, so a script missing leaves no empty
			// record behind.
			Map<String, byte[]> baseline = new LinkedHashMap<>();
			if (existing && !exists(connection)) {
				for (String scriptPath : sanitizedPaths) {
					baseline.put(scriptPath, checksum(scriptPath));
				}
			}
			try (Statement statement = connection.createStatement()) {
				create(statement);
			}
			for (Map.Entry<String, byte[]> script : baseline.entrySet()) {
				record(connection, script.getKey(), script.getValue());
			}
			connection.commit();
			Map<String, byte[]> run = scriptsRun(connection);
			Map<String, String> moved = followMovedScripts(sanitizedPaths, run);
			List<String> pending = pendingScripts(sanitizedPaths, run);
			for (Map.Entry<String, String> script : moved.entrySet()) {
				updatePath(connection, script.getKey(), script.getValue());
			}
			connection.commit();
			for (String scriptPath : pending) {
				run(connection, scriptPath);
			}
		}

		/**
		 * Validates a database against the init scripts it would be opened with, refusing it as {@link #run(Connection, List, boolean)}
		 * would, changing nothing, so the connection may be read only. A database without the record of its scripts is
		 * never refused, nor one opened with none listed.
		 * @param connection a connection to the database
		 * @param scriptPaths the init scripts it would be opened with
		 * @throws DatabaseException in case the database would be refused, or a script listed can not be read
		 * @throws SQLException in case the record of the scripts run can not be read
		 */
		public final void validate(Connection connection, List<String> scriptPaths) throws SQLException {
			List<String> sanitizedPaths = sanitize(scriptPaths);
			if (!sanitizedPaths.isEmpty() && exists(connection)) {
				Map<String, byte[]> run = scriptsRun(connection);
				followMovedScripts(sanitizedPaths, run);
				pendingScripts(sanitizedPaths, run);
			}
		}

		/**
		 * Creates the table recording the scripts run, see {@link #table()}, in case it does not exist.
		 * @param statement the statement to use
		 * @throws SQLException in case of an exception
		 */
		protected abstract void create(Statement statement) throws SQLException;

		/**
		 * @param connection the connection
		 * @return true if the table recording the scripts run exists
		 * @throws SQLException in case of an exception
		 */
		protected abstract boolean exists(Connection connection) throws SQLException;

		/**
		 * Opens a script for reading, its path as listed.
		 * @param scriptPath the script path
		 * @return a stream reading the script
		 * @throws IOException in case the script can not be read
		 */
		protected abstract InputStream open(String scriptPath) throws IOException;

		/**
		 * Runs a script, within the transaction recording it.
		 * @param statement the statement to use
		 * @param scriptPath the script path, as listed
		 * @param script the script, as {@link #open(String)} reads it
		 * @throws SQLException in case the script fails
		 */
		protected abstract void run(Statement statement, String scriptPath, String script) throws SQLException;

		// A script listed but not recorded, with the content of one recorded but no longer listed, is that one moved, which
		// has run, so its record follows it, in run, the path it was recorded by mapped to the one listed being returned.
		// One to one, so identical scripts move alike. One moved and changed is not found
		private Map<String, String> followMovedScripts(List<String> scriptPaths, Map<String, byte[]> run) {
			Map<String, byte[]> unlisted = new HashMap<>(run);
			unlisted.keySet().removeAll(scriptPaths);
			Map<String, String> moved = new HashMap<>();
			for (String scriptPath : scriptPaths) {
				if (!unlisted.isEmpty() && !run.containsKey(scriptPath)) {
					byte[] checksum = checksum(scriptPath);
					Optional<String> recorded = unlisted.keySet().stream()
									.filter(path -> Arrays.equals(unlisted.get(path), checksum))
									.findFirst();
					if (recorded.isPresent()) {
						moved.put(recorded.get(), scriptPath);
						run.remove(recorded.get());
						run.put(scriptPath, unlisted.remove(recorded.get()));
					}
				}
			}

			return moved;
		}

		// The scripts not yet run, in the order listed, consuming run, refusing the database
		private List<String> pendingScripts(List<String> scriptPaths, Map<String, byte[]> run) {
			List<String> pending = new ArrayList<>();
			for (String scriptPath : scriptPaths) {
				byte[] checksum = run.remove(scriptPath);
				if (checksum == null) {
					pending.add(scriptPath);
				}
				else if (!pending.isEmpty()) {
					throw new DatabaseException(INIT_SCRIPT + pending.get(0) + " has not been run, but " + scriptPath + ", listed after it, has");
				}
				else if (!Arrays.equals(checksum, checksum(scriptPath))) {
					throw new DatabaseException(INIT_SCRIPT + scriptPath + " has changed since it was run");
				}
			}
			if (!run.isEmpty()) {
				throw new DatabaseException("Init scripts run, but no longer listed: " + String.join(", ", run.keySet()));
			}

			return pending;
		}

		private void updatePath(Connection connection, String scriptPath, String newScriptPath) throws SQLException {
			try (PreparedStatement update = connection.prepareStatement(updatePath)) {
				update.setString(1, newScriptPath);
				update.setString(2, scriptPath);
				update.executeUpdate();
			}
		}

		// Where a dialect's DDL commits, a script failing may leave part of its work behind, not recorded, to run again
		private void run(Connection connection, String scriptPath) throws SQLException {
			try (Statement statement = connection.createStatement()) {
				String script = read(scriptPath);
				run(statement, scriptPath, script);
				record(connection, scriptPath, digest(script));
				connection.commit();
			}
			catch (SQLException e) {
				connection.rollback();
				throw new DatabaseException(e, INIT_SCRIPT + scriptPath + " failed: " + e.getMessage());
			}
		}

		private void record(Connection connection, String scriptPath, byte[] checksum) throws SQLException {
			try (PreparedStatement insert = connection.prepareStatement(insertScript)) {
				insert.setString(1, scriptPath);
				insert.setBytes(2, checksum);
				insert.executeUpdate();
			}
		}

		private Map<String, byte[]> scriptsRun(Connection connection) throws SQLException {
			Map<String, byte[]> run = new TreeMap<>();
			try (Statement statement = connection.createStatement();
					 ResultSet resultSet = statement.executeQuery(selectScripts)) {
				while (resultSet.next()) {
					run.put(resultSet.getString(1), resultSet.getBytes(2));
				}
			}

			return run;
		}

		private byte[] checksum(String scriptPath) {
			return digest(read(scriptPath));
		}

		// The carriage returns removed, so the line endings of a checkout, on Windows say, do not change it
		private String read(String scriptPath) {
			try (InputStream input = open(scriptPath)) {
				return new String(readAll(input), StandardCharsets.UTF_8).replace("\r", "");
			}
			catch (IOException e) {
				throw new DatabaseException(INIT_SCRIPT + scriptPath + " can not be read: " + e);
			}
		}

		private static byte[] digest(String script) {
			try {
				return MessageDigest.getInstance(CHECKSUM_ALGORITHM).digest(script.getBytes(StandardCharsets.UTF_8));
			}
			catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			}
		}

		private static byte[] readAll(InputStream input) throws IOException {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[READ_BUFFER_SIZE];
			int read;
			while ((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}

			return output.toByteArray();
		}

		/**
		 * Sanitizes script paths, a dialect possibly running a script by its path in a statement of its own, for a dialect
		 * to refuse them before opening the database, {@link #run(Connection, List, boolean)} and
		 * {@link #validate(Connection, List)} sanitizing them as well.
		 * @param scriptPaths the script paths
		 * @return the paths, trimmed, their separators forward slashes
		 * @throws SecurityException if a script path is empty or contains potential injection attempts
		 */
		protected static List<String> sanitize(List<String> scriptPaths) {
			List<String> sanitizedPaths = new ArrayList<>(scriptPaths.size());
			for (String scriptPath : scriptPaths) {
				if (scriptPath == null || scriptPath.trim().isEmpty()) {
					throw new SecurityException("Script path cannot be null or empty");
				}
				String trimmedPath = scriptPath.trim();
				if (trimmedPath.contains("'") || trimmedPath.contains("\"") ||
								trimmedPath.contains(";") || trimmedPath.contains("--") ||
								trimmedPath.contains("/*") || trimmedPath.contains("*/")) {
					throw new SecurityException("Script path contains potentially dangerous characters: " + scriptPath);
				}
				// Normalize path separators for consistency
				sanitizedPaths.add(trimmedPath.replace("\\", "/"));
			}

			return sanitizedPaths;
		}
	}
}
