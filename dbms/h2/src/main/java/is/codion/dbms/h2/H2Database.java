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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.dbms.h2;

import is.codion.common.db.database.AbstractDatabase;
import is.codion.common.resource.MessageBundle;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * A Database implementation based on the H2 database.
 */
final class H2Database extends AbstractDatabase {

	private static final MessageBundle MESSAGES =
					messageBundle(H2Database.class, getBundle(H2Database.class.getName()));

	/**
	 * The error code representing incorrect login credentials
	 */
	private static final int AUTHENTICATION_ERROR = 28000;
	private static final int REFERENTIAL_INTEGRITY_ERROR_CHILD_EXISTS = 23503;
	private static final int REFERENTIAL_INTEGRITY_ERROR_PARENT_MISSING = 23506;
	private static final int UNIQUE_CONSTRAINT_ERROR = 23505;
	private static final int TIMEOUT_ERROR = 57014;
	private static final int NULL_NOT_ALLOWED = 23502;
	private static final int CHECK_CONSTRAINT_INVALID = 23514;
	private static final int WRONG_USER_OR_PASSWORD = 28000;

	private static final Set<String> INITIALIZED_DATABASES = new HashSet<>();

	private static final String JDBC_URL_PREFIX = "jdbc:h2:";
	private static final String JDBC_URL_PREFIX_MEM = "jdbc:h2:mem:";
	private static final String JDBC_URL_PREFIX_FILE = "jdbc:h2:file:";
	private static final String JDBC_URL_PREFIX_TCP = "jdbc:h2:tcp://";
	private static final String JDBC_URL_PREFIX_SSL = "jdbc:h2:ssl:";
	private static final String JDBC_URL_PREFIX_ZIP = "jdbc:h2:zip:";

	private static final String FILE_SUFFIX_PAGESTORE = ".h2.db";
	private static final String FILE_SUFFIX_MVSTORE = ".mv.db";

	static final String AUTO_INCREMENT_QUERY = "CALL IDENTITY()";
	static final String SEQUENCE_VALUE_QUERY = "select next value for ";
	static final String SYSADMIN_USERNAME = "sa";

	private static final Map<Integer, String> ERROR_MESSAGES = new HashMap<>();

	static {
		ERROR_MESSAGES.put(UNIQUE_CONSTRAINT_ERROR, MESSAGES.getString("unique_key_error"));
		ERROR_MESSAGES.put(REFERENTIAL_INTEGRITY_ERROR_CHILD_EXISTS, MESSAGES.getString("child_record_error"));
		ERROR_MESSAGES.put(REFERENTIAL_INTEGRITY_ERROR_PARENT_MISSING, MESSAGES.getString("integrity_constraint_error"));
		ERROR_MESSAGES.put(NULL_NOT_ALLOWED, MESSAGES.getString("value_missing"));
		ERROR_MESSAGES.put(CHECK_CONSTRAINT_INVALID, MESSAGES.getString("check_constraint_invalid"));
		ERROR_MESSAGES.put(WRONG_USER_OR_PASSWORD, MESSAGES.getString("wrong_user_or_password"));
	}

	private final boolean nowait;

	H2Database(String url) {
		this(url, emptyList());
	}

	H2Database(String url, List<String> scriptPaths) {
		this(url, scriptPaths, true);
	}

	H2Database(String url, List<String> scriptPaths, boolean nowait) {
		super(url);
		this.nowait = nowait;
		synchronized (INITIALIZED_DATABASES) {
			if (!INITIALIZED_DATABASES.contains(url.toLowerCase())) {
				initializeEmbeddedDatabase(scriptPaths);
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
	public String limitOffsetClause(Integer limit, Integer offset) {
		return createLimitOffsetClause(limit, offset);
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		return AUTO_INCREMENT_QUERY;
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return SEQUENCE_VALUE_QUERY + requireNonNull(sequenceName);
	}

	@Override
	public boolean isAuthenticationException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == AUTHENTICATION_ERROR;
	}

	@Override
	public boolean isReferentialIntegrityException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == REFERENTIAL_INTEGRITY_ERROR_CHILD_EXISTS ||
						exception.getErrorCode() == REFERENTIAL_INTEGRITY_ERROR_PARENT_MISSING;
	}

	@Override
	public boolean isUniqueConstraintException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == UNIQUE_CONSTRAINT_ERROR;
	}

	@Override
	public boolean isTimeoutException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == TIMEOUT_ERROR;
	}

	@Override
	public String errorMessage(SQLException exception, Operation operation) {
		if (exception.getErrorCode() == NULL_NOT_ALLOWED) {
			// NULL not allowed for column "NAME;"
			String exceptionMessage = exception.getMessage();
			String columnName = exceptionMessage.substring(exceptionMessage.indexOf('"') + 1, exceptionMessage.lastIndexOf('"'));

			return MESSAGES.getString("value_missing") + ": " + columnName;
		}

		if (ERROR_MESSAGES.containsKey(exception.getErrorCode())) {
			return ERROR_MESSAGES.get(exception.getErrorCode());
		}

		return exception.getMessage();
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
		
		// Check for path traversal attempts
		if (trimmedPath.contains("../") || trimmedPath.contains("..\\")) {
			throw new SecurityException("Script path contains path traversal sequences: " + scriptPath);
		}
		
		// Normalize path separators for consistency
		return trimmedPath.replace("\\", "/");
	}

	private void initializeEmbeddedDatabase(List<String> scriptPaths) {
		if ((isEmbeddedInMemory() || !databaseFileExists())) {
			Properties properties = new Properties();
			properties.put(USER_PROPERTY, SYSADMIN_USERNAME);
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
		INITIALIZED_DATABASES.add(url().toLowerCase());
	}

	private String databasePath() {
		return removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX_FILE, JDBC_URL_PREFIX);
	}

	private boolean isEmbeddedInMemory() {
		return url().startsWith(JDBC_URL_PREFIX_MEM);
	}

	private boolean databaseFileExists() {
		return Files.exists(Paths.get(databasePath() + FILE_SUFFIX_PAGESTORE)) ||
						Files.exists(Paths.get(databasePath() + FILE_SUFFIX_MVSTORE));
	}

	private void initialize(Properties properties, String appendToUrl) {
		try {
			DriverManager.getConnection(url() + appendToUrl, properties).close();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
