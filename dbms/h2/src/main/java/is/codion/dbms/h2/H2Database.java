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
package is.codion.dbms.h2;

import is.codion.common.db.database.AbstractDatabase;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ErrorType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static is.codion.common.utilities.user.User.user;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the H2 database.
 */
final class H2Database extends AbstractDatabase {

	private static final Set<String> INITIALIZED_DATABASES = new HashSet<>();

	private static final String JDBC_URL_PREFIX = "jdbc:h2:";
	private static final String JDBC_URL_PREFIX_MEM = "jdbc:h2:mem:";
	private static final String JDBC_URL_PREFIX_TCP = "jdbc:h2:tcp://";
	private static final String JDBC_URL_PREFIX_SSL = "jdbc:h2:ssl:";
	private static final String JDBC_URL_PREFIX_ZIP = "jdbc:h2:zip:";

	private static final String FILE_SUFFIX_PAGESTORE = ".h2.db";
	private static final String FILE_SUFFIX_MVSTORE = ".mv.db";
	// The file systems a database is opened through, stackable, retry:nio: for example, the file being the same whichever
	// it is: H2 takes a single character before a colon for a Windows drive, unless a parameter, as in nioMemLZF:1:
	private static final Pattern FILE_SYSTEM_PREFIXES = Pattern.compile("^(?:[a-zA-Z0-9]{2,}:|[0-9]:)+");
	private static final String USER_HOME_DIRECTORY = "~";
	private static final String SHUTDOWN = "SHUTDOWN";

	// In a schema of its own, out of the way of an application keeping its tables in PUBLIC
	private static final String INIT_SCRIPT_TABLE = "codion.init_script";
	private static final String CREATE_INIT_SCRIPT_SCHEMA = "create schema if not exists codion";
	private static final String CREATE_INIT_SCRIPT_TABLE = "create table if not exists " + INIT_SCRIPT_TABLE + " (" +
					"path varchar(1000) primary key, " +
					"checksum varbinary(32) not null, " +
					"run_at timestamp default current_timestamp not null)";
	private static final String SELECT_INIT_SCRIPT_TABLE =
					"select 1 from information_schema.tables where table_schema = 'CODION' and table_name = 'INIT_SCRIPT'";
	private static final String SELECT_INIT_SCRIPTS = "select path, checksum from " + INIT_SCRIPT_TABLE;
	private static final String INSERT_INIT_SCRIPT = "insert into " + INIT_SCRIPT_TABLE + " (path, checksum) values (?, ?)";
	private static final String SELECT_CHECKSUM = "select hash('SHA-256', replace(file_read(?, 'UTF-8'), char(13)))";

	static final String SEQUENCE_VALUE_QUERY = "select next value for ";
	static final String SYSADMIN_USERNAME = "sa";

	private static final String COLUMN = "column \"";
	private static final String CONSTRAINT_VIOLATION = "constraint violation: \"";
	private static final String SQL_STATEMENT = "; SQL statement:";
	private static final String INIT_SCRIPT = "Init script ";

	private static final Map<Integer, ErrorType> ERROR_TYPES = new HashMap<>();

	static {
		// the error codes not covered by the sql state defaults
		ERROR_TYPES.put(23503, ErrorType.CHILD_EXISTS);// referential integrity violated, child exists
		ERROR_TYPES.put(23506, ErrorType.PARENT_MISSING);// referential integrity violated, parent missing
		ERROR_TYPES.put(50200, ErrorType.ROW_LOCKED);// lock timeout, a timeout exception when NOWAIT is used
		ERROR_TYPES.put(57014, ErrorType.TIMEOUT);// statement was canceled
		ERROR_TYPES.put(42102, ErrorType.TABLE_NOT_FOUND);
		ERROR_TYPES.put(42104, ErrorType.TABLE_NOT_FOUND);// the database being empty
		ERROR_TYPES.put(90096, ErrorType.MISSING_PRIVILEGES);// not enough rights for object
	}

	private final boolean server;

	H2Database(String url) {
		this(url, emptyList());
	}

	H2Database(String url, List<String> scriptPaths) {
		super(url);
		this.server = startsWith(url, JDBC_URL_PREFIX_TCP) || startsWith(url, JDBC_URL_PREFIX_SSL);
		if (!server && !startsWith(url, JDBC_URL_PREFIX_ZIP)) {
			// a database on a server is not ours to initialize and one in a zip file is read only
			synchronized (INITIALIZED_DATABASES) {
				if (!INITIALIZED_DATABASES.contains(initializedKey())) {
					initializeEmbeddedDatabase(scriptPaths);
				}
			}
		}
	}

	@Override
	public String name() {
		return databaseName(url());
	}

	@Override
	public String selectForUpdateClause(Lock lock) {
		return lock == Lock.NOWAIT ? FOR_UPDATE_NOWAIT : FOR_UPDATE;
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
		return createLimitOffsetClause(limit, offset);
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		throw new UnsupportedOperationException("H2 provides no function for querying the last generated value, " +
						"use an identity based generator, relying on Statement.getGeneratedKeys(), instead of an automatic one");
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return SEQUENCE_VALUE_QUERY + requireNonNull(sequenceName);
	}

	@Override
	protected ErrorType errorType(SQLException exception) {
		ErrorType errorType = ERROR_TYPES.get(exception.getErrorCode());

		return errorType == null ? super.errorType(exception) : errorType;
	}

	@Override
	protected String errorDetail(SQLException exception, ErrorType errorType) {
		String message = exception.getMessage();
		if (message == null) {
			return null;
		}
		switch (errorType) {
			case NULL_VALUE:
				// NULL not allowed for column "NAME"; SQL statement: ...
				return between(message, COLUMN, "\"");
			case VALUE_TOO_LARGE:
				// Value too long for column "NAME CHARACTER VARYING(10)": "'abcdefghijklmnop' (16)"; SQL statement: ...
				String column = between(message, COLUMN, "\"");

				return column == null || column.indexOf(' ') == -1 ? column : column.substring(0, column.indexOf(' '));
			case PARENT_MISSING:
			case CHILD_EXISTS:
				// Referential integrity constraint violation: "CHILD_FK: PUBLIC.CHILD FOREIGN KEY(PARENT_ID) REFERENCES PUBLIC.PARENT(ID) (99)"; SQL statement: ...
			case CHECK_CONSTRAINT:
				// Check constraint violation: "TABLE_CK: "; SQL statement: ...
				return between(message, CONSTRAINT_VIOLATION, ":");
			default:
				return null;
		}
	}

	/**
	 * The statement is appended to each message
	 */
	@Override
	protected String message(SQLException exception) {
		String message = exception.getMessage();
		if (message == null) {
			return null;
		}
		int statementIndex = message.indexOf(SQL_STATEMENT);

		return statementIndex == -1 ? message : message.substring(0, statementIndex);
	}

	@Override
	protected void closeDatabase() throws SQLException {
		if (server) {
			return;
		}
		synchronized (INITIALIZED_DATABASES) {
			try (Connection connection = createConnection(user(SYSADMIN_USERNAME));
					 Statement statement = connection.createStatement()) {
				statement.execute(SHUTDOWN);
			}
			finally {
				INITIALIZED_DATABASES.remove(initializedKey());
			}
		}
	}

	static String databaseName(String url) {
		String name = startsWith(url, JDBC_URL_PREFIX_TCP) || startsWith(url, JDBC_URL_PREFIX_MEM) ||
						startsWith(url, JDBC_URL_PREFIX_SSL) || startsWith(url, JDBC_URL_PREFIX_ZIP) ?
						removeUrlPrefixOptionsAndParameters(url, JDBC_URL_PREFIX_TCP, JDBC_URL_PREFIX_MEM,
										JDBC_URL_PREFIX_SSL, JDBC_URL_PREFIX_ZIP) : filePath(url);

		return name.isEmpty() ? "private" : name;
	}

	/**
	 * @param url an embedded file database's url
	 * @return the path of the database's files, without their suffix, as H2 finds them
	 */
	static String databasePath(String url) {
		String path = filePath(url);
		if (path.equals(USER_HOME_DIRECTORY) || path.startsWith(USER_HOME_DIRECTORY + "/") || path.startsWith(USER_HOME_DIRECTORY + "\\")) {
			return System.getProperty("user.home") + path.substring(USER_HOME_DIRECTORY.length());
		}

		return path;
	}

	private static String filePath(String url) {
		return FILE_SYSTEM_PREFIXES.matcher(removeUrlPrefixOptionsAndParameters(url, JDBC_URL_PREFIX)).replaceFirst("");
	}

	/**
	 * Sanitizes script paths to prevent SQL injection in H2 INIT parameters.
	 * @param scriptPath the script path to sanitize
	 * @return sanitized script path safe for use in H2 URL parameters
	 * @throws SecurityException if the script path contains potential injection attempts
	 */
	private static String sanitizeScriptPath(String scriptPath) {
		if (scriptPath == null || scriptPath.trim().isEmpty()) {
			throw new SecurityException("Script path cannot be null or empty");
		}

		String trimmedPath = scriptPath.trim();

		// Check for SQL injection attempts
		if (trimmedPath.contains("'") || trimmedPath.contains("\"") ||
						trimmedPath.contains(";") || trimmedPath.contains("--") ||
						trimmedPath.contains("/*") || trimmedPath.contains("*/")) {
			throw new SecurityException("Script path contains potentially dangerous characters: " + scriptPath);
		}

		// Normalize path separators for consistency
		return trimmedPath.replace("\\", "/");
	}

	private void initializeEmbeddedDatabase(List<String> scriptPaths) {
		List<String> sanitizedPaths = new ArrayList<>(scriptPaths.size());
		for (String scriptPath : scriptPaths) {
			sanitizedPaths.add(sanitizeScriptPath(scriptPath));
		}
		boolean create = isEmbeddedInMemory() || !databaseFileExists();
		if (create || !sanitizedPaths.isEmpty()) {
			Properties properties = new Properties();
			properties.put(USER, SYSADMIN_USERNAME);
			// a database created here stays open once the connection closes, until shut down
			try (Connection connection = DriverManager.getConnection(url() + (create ? ";DB_CLOSE_DELAY=-1" : ""), properties)) {
				if (!sanitizedPaths.isEmpty()) {
					runInitScripts(connection, sanitizedPaths, !create);
				}
			}
			catch (SQLException e) {
				// a database refusing sa without a password, its admin having one or being another, is administered, not
				// ours to initialize, as one on a server is not, which a database being created never is
				if (errorType(e) != ErrorType.AUTHENTICATION) {
					throw new DatabaseException(e);
				}
			}
		}
		INITIALIZED_DATABASES.add(initializedKey());
	}

	// Runs the scripts not yet run, in the order listed, each recorded with a checksum of its content once run, after
	// checking that each one run is unchanged and still listed, and that none not yet run is listed before one that has,
	// so that a database brought up to date one list at a time ends up as one created from the last
	private static void runInitScripts(Connection connection, List<String> scriptPaths, boolean existing) throws SQLException {
		connection.setAutoCommit(false);
		// An existing database without the record was created before the scripts were recorded, or with none listed, and
		// has run none since, so those listed are recorded as run, leaving it as it was, those added later then running.
		// The checksums before the table, whose creation commits, so a script missing leaves no empty record behind.
		Map<String, byte[]> baseline = new LinkedHashMap<>();
		if (existing && !initScriptTableExists(connection)) {
			for (String scriptPath : scriptPaths) {
				baseline.put(scriptPath, checksum(connection, scriptPath));
			}
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute(CREATE_INIT_SCRIPT_SCHEMA);
			statement.execute(CREATE_INIT_SCRIPT_TABLE);
		}
		for (Map.Entry<String, byte[]> script : baseline.entrySet()) {
			record(connection, script.getKey(), script.getValue());
		}
		connection.commit();
		Map<String, byte[]> run = initScriptsRun(connection);
		List<String> pending = new ArrayList<>();
		for (String scriptPath : scriptPaths) {
			byte[] checksum = run.remove(scriptPath);
			if (checksum == null) {
				pending.add(scriptPath);
			}
			else if (!pending.isEmpty()) {
				throw new DatabaseException(INIT_SCRIPT + pending.get(0) + " has not been run, but " + scriptPath + ", listed after it, has");
			}
			else if (!Arrays.equals(checksum, checksum(connection, scriptPath))) {
				throw new DatabaseException(INIT_SCRIPT + scriptPath + " has changed since it was run");
			}
		}
		if (!run.isEmpty()) {
			throw new DatabaseException("Init scripts run, but no longer listed: " + String.join(", ", run.keySet()));
		}
		for (String scriptPath : pending) {
			runInitScript(connection, scriptPath);
		}
	}

	// H2's DDL commits, so a script failing may leave part of its work behind, not recorded, to run again
	private static void runInitScript(Connection connection, String scriptPath) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			byte[] checksum = checksum(connection, scriptPath);
			statement.execute("runscript from '" + scriptPath + "'");
			record(connection, scriptPath, checksum);
			connection.commit();
		}
		catch (SQLException e) {
			connection.rollback();
			throw new DatabaseException(e, INIT_SCRIPT + scriptPath + " failed: " + e.getMessage());
		}
	}

	private static void record(Connection connection, String scriptPath, byte[] checksum) throws SQLException {
		try (PreparedStatement insert = connection.prepareStatement(INSERT_INIT_SCRIPT)) {
			insert.setString(1, scriptPath);
			insert.setBytes(2, checksum);
			insert.executeUpdate();
		}
	}

	private static boolean initScriptTableExists(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				 ResultSet resultSet = statement.executeQuery(SELECT_INIT_SCRIPT_TABLE)) {
			return resultSet.next();
		}
	}

	private static Map<String, byte[]> initScriptsRun(Connection connection) throws SQLException {
		Map<String, byte[]> run = new HashMap<>();
		try (Statement statement = connection.createStatement();
				 ResultSet resultSet = statement.executeQuery(SELECT_INIT_SCRIPTS)) {
			while (resultSet.next()) {
				run.put(resultSet.getString(1), resultSet.getBytes(2));
			}
		}

		return run;
	}

	// The script as RUNSCRIPT reads it, through H2's own file resolution, classpath: included, its carriage returns
	// removed, so the line endings of a checkout, on Windows say, do not change it
	private static byte[] checksum(Connection connection, String scriptPath) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SELECT_CHECKSUM)) {
			statement.setString(1, scriptPath);
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();

				return resultSet.getBytes(1);
			}
		}
	}

	private String initializedKey() {
		return url().toLowerCase(Locale.ROOT);
	}

	private boolean isEmbeddedInMemory() {
		return startsWith(url(), JDBC_URL_PREFIX_MEM);
	}

	private boolean databaseFileExists() {
		String databasePath = databasePath(url());

		return Files.exists(Paths.get(databasePath + FILE_SUFFIX_PAGESTORE)) ||
						Files.exists(Paths.get(databasePath + FILE_SUFFIX_MVSTORE));
	}

	private static boolean startsWith(String url, String prefix) {
		return url.regionMatches(true, 0, prefix, 0, prefix.length());
	}
}
