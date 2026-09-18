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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
	private static final String JDBC_URL_PREFIX_FILE = "jdbc:h2:file:";
	private static final String JDBC_URL_PREFIX_TCP = "jdbc:h2:tcp://";
	private static final String JDBC_URL_PREFIX_SSL = "jdbc:h2:ssl:";
	private static final String JDBC_URL_PREFIX_ZIP = "jdbc:h2:zip:";

	private static final String FILE_SUFFIX_PAGESTORE = ".h2.db";
	private static final String FILE_SUFFIX_MVSTORE = ".mv.db";
	private static final String SHUTDOWN = "SHUTDOWN";

	static final String SEQUENCE_VALUE_QUERY = "select next value for ";
	static final String SYSADMIN_USERNAME = "sa";

	private static final String COLUMN = "column \"";
	private static final String SQL_STATEMENT = "; SQL statement:";

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

	private final boolean nowait;
	private final boolean server;

	H2Database(String url) {
		this(url, emptyList());
	}

	H2Database(String url, List<String> scriptPaths) {
		this(url, scriptPaths, true);
	}

	H2Database(String url, List<String> scriptPaths, boolean nowait) {
		super(url);
		this.nowait = nowait;
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
	public String selectForUpdateClause() {
		if (nowait) {
			return FOR_UPDATE_NOWAIT;
		}

		return FOR_UPDATE;
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
		String name = removeUrlPrefixOptionsAndParameters(url, JDBC_URL_PREFIX_TCP, JDBC_URL_PREFIX_FILE,
						JDBC_URL_PREFIX_MEM, JDBC_URL_PREFIX_SSL, JDBC_URL_PREFIX_ZIP, JDBC_URL_PREFIX);

		return name.isEmpty() ? "private" : name;
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
		if ((isEmbeddedInMemory() || !databaseFileExists())) {
			Properties properties = new Properties();
			properties.put(USER, SYSADMIN_USERNAME);
			if (scriptPaths.isEmpty()) {
				initialize(properties, ";DB_CLOSE_DELAY=-1");
			}
			else {
				for (String scriptPath : scriptPaths) {
					String sanitizedPath = sanitizeScriptPath(scriptPath);
					initialize(properties, ";DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM '" + sanitizedPath + "'");
				}
			}
		}
		INITIALIZED_DATABASES.add(initializedKey());
	}

	private String initializedKey() {
		return url().toLowerCase(Locale.ROOT);
	}

	private String databasePath() {
		return removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX_FILE, JDBC_URL_PREFIX);
	}

	private boolean isEmbeddedInMemory() {
		return startsWith(url(), JDBC_URL_PREFIX_MEM);
	}

	private boolean databaseFileExists() {
		return Files.exists(Paths.get(databasePath() + FILE_SUFFIX_PAGESTORE)) ||
						Files.exists(Paths.get(databasePath() + FILE_SUFFIX_MVSTORE));
	}

	private static boolean startsWith(String url, String prefix) {
		return url.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private static String between(String message, String prefix, String suffix) {
		int prefixIndex = message.indexOf(prefix);
		if (prefixIndex == -1) {
			return null;
		}
		int beginIndex = prefixIndex + prefix.length();
		int endIndex = message.indexOf(suffix, beginIndex);

		return endIndex == -1 ? null : message.substring(beginIndex, endIndex);
	}

	private void initialize(Properties properties, String appendToUrl) {
		try {
			DriverManager.getConnection(url() + appendToUrl, properties).close();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}
}
