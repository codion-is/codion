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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
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

	private static final InitScripts INIT_SCRIPTS = new H2InitScripts();

	static final String SEQUENCE_VALUE_QUERY = "select next value for ";
	static final String SYSADMIN_USERNAME = "sa";

	private static final String COLUMN = "column \"";
	private static final String CONSTRAINT_VIOLATION = "constraint violation: \"";
	private static final String SQL_STATEMENT = "; SQL statement:";

	private static final Map<Integer, ErrorType> ERROR_TYPES = new HashMap<>();

	static {
		// the error codes not covered by the sql state defaults
		ERROR_TYPES.put(23503, ErrorType.CHILD_EXISTS);// referential integrity violated, child exists
		ERROR_TYPES.put(23506, ErrorType.PARENT_MISSING);// referential integrity violated, parent missing
		ERROR_TYPES.put(50200, ErrorType.ROW_LOCKED);// lock timeout, a timeout exception when NOWAIT is used
		ERROR_TYPES.put(40001, ErrorType.DEADLOCK);// deadlock detected, a serializable transaction's conflict reported as one too
		ERROR_TYPES.put(57014, ErrorType.TIMEOUT);// statement was canceled
		ERROR_TYPES.put(42102, ErrorType.TABLE_NOT_FOUND);
		ERROR_TYPES.put(42104, ErrorType.TABLE_NOT_FOUND);// the database being empty
		ERROR_TYPES.put(90079, ErrorType.TABLE_NOT_FOUND);// a table qualified by a schema not found, as PostgreSQL has it
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
	public String limitOffsetClause(@Nullable Integer limit, @Nullable Integer offset, boolean ordered) {
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
	protected @Nullable ErrorType errorType(SQLException exception) {
		ErrorType errorType = ERROR_TYPES.get(exception.getErrorCode());

		return errorType == null ? super.errorType(exception) : errorType;
	}

	@Override
	protected @Nullable String errorDetail(SQLException exception, ErrorType errorType) {
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
	protected @Nullable String message(SQLException exception) {
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

	private void initializeEmbeddedDatabase(List<String> scriptPaths) {
		// refused before the database is opened, a file database not created then
		List<String> sanitizedPaths = H2InitScripts.sanitized(scriptPaths);
		boolean create = isEmbeddedInMemory() || !databaseFileExists();
		if (create || !sanitizedPaths.isEmpty()) {
			Properties properties = new Properties();
			properties.put(USER, SYSADMIN_USERNAME);
			// an in-memory database stays open once the connection closes, until shut down, H2 discarding it otherwise, but
			// not a file database: H2 keeps the delay as the database's own setting, so one created with it would stay open
			// once its last connection closes, in every later process too
			try (Connection connection = DriverManager.getConnection(url() + (isEmbeddedInMemory() ? ";DB_CLOSE_DELAY=-1" : ""), properties)) {
				if (!sanitizedPaths.isEmpty()) {
					INIT_SCRIPTS.run(connection, sanitizedPaths, !create);
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

	static void validateInitScripts(Connection connection, List<String> scriptPaths) throws SQLException {
		INIT_SCRIPTS.validate(connection, scriptPaths);
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

	private static final class H2InitScripts extends InitScripts {

		// In a schema of its own, out of the way of an application keeping its tables in PUBLIC
		private static final String CREATE_SCHEMA = "create schema if not exists codion";
		private static final String CREATE_TABLE = "create table if not exists codion.init_script (" +
						"path varchar(1000) primary key, " +
						"checksum varbinary(32) not null, " +
						"run_at timestamp default current_timestamp not null)";
		private static final String SELECT_TABLE =
						"select 1 from information_schema.tables where table_schema = 'CODION' and table_name = 'INIT_SCRIPT'";
		private static final String FILE_UTILS_CLASS_NAME = "org.h2.store.fs.FileUtils";

		private H2InitScripts() {
			super("codion.init_script");
		}

		private static List<String> sanitized(List<String> scriptPaths) {
			return sanitize(scriptPaths);
		}

		@Override
		protected void create(Statement statement) throws SQLException {
			statement.execute(CREATE_SCHEMA);
			statement.execute(CREATE_TABLE);
		}

		@Override
		protected boolean exists(Connection connection) throws SQLException {
			try (Statement statement = connection.createStatement();
					 ResultSet resultSet = statement.executeQuery(SELECT_TABLE)) {
				return resultSet.next();
			}
		}

		// The script as RUNSCRIPT reads it, through H2's own file resolution, classpath: included. Not by H2's FILE_READ,
		// which reads no more than the size H2 finds for the file, zero for a classpath resource it can not open as a file
		// system, on Android for one
		@Override
		protected InputStream open(String scriptPath) throws IOException {
			try {
				return (InputStream) Class.forName(FILE_UTILS_CLASS_NAME)
								.getMethod("newInputStream", String.class)
								.invoke(null, scriptPath);
			}
			catch (InvocationTargetException e) {
				if (e.getCause() instanceof IOException) {
					throw (IOException) e.getCause();
				}
				throw new IOException(String.valueOf(e.getCause()), e.getCause());
			}
			catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}

		// H2's DDL commits, so a script failing may leave part of its work behind, not recorded, to run again
		@Override
		protected void run(Statement statement, String scriptPath, String script) throws SQLException {
			statement.execute("runscript from '" + scriptPath + "'");
		}
	}
}
