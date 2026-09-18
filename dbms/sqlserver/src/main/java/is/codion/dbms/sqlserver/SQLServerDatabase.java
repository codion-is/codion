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
package is.codion.dbms.sqlserver;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the SQL Server (2012 or higher) database.
 */
final class SQLServerDatabase extends AbstractDatabase {

	/**
	 * Reported for foreign key, reference and check constraints alike, the message naming the type
	 */
	private static final int CONSTRAINT_CONFLICT = 547;
	private static final String FOREIGN_KEY = "FOREIGN KEY";
	private static final String REFERENCE = "REFERENCE";
	private static final String CHECK = "CHECK";
	private static final String COLUMN = "column '";
	private static final String DUPLICATE_KEY_VALUE = "The duplicate key value is ";

	private static final Map<Integer, ErrorType> ERROR_TYPES = new HashMap<>();

	static {
		ERROR_TYPES.put(2601, ErrorType.UNIQUE_CONSTRAINT);// unique index
		ERROR_TYPES.put(2627, ErrorType.UNIQUE_CONSTRAINT);// unique or primary key constraint
		ERROR_TYPES.put(515, ErrorType.NULL_VALUE);
		ERROR_TYPES.put(2628, ErrorType.VALUE_TOO_LARGE);// string or binary data would be truncated, naming the column, 2019 and later
		ERROR_TYPES.put(8152, ErrorType.VALUE_TOO_LARGE);// string or binary data would be truncated
		ERROR_TYPES.put(8115, ErrorType.VALUE_TOO_LARGE);// arithmetic overflow
		ERROR_TYPES.put(229, ErrorType.MISSING_PRIVILEGES);// permission denied on object
		ERROR_TYPES.put(230, ErrorType.MISSING_PRIVILEGES);// permission denied on column
		ERROR_TYPES.put(18456, ErrorType.AUTHENTICATION);
		ERROR_TYPES.put(18470, ErrorType.ACCOUNT_LOCKED);// the account is disabled
		ERROR_TYPES.put(18486, ErrorType.ACCOUNT_LOCKED);// the account is locked out
		ERROR_TYPES.put(18487, ErrorType.PASSWORD_EXPIRED);
		ERROR_TYPES.put(18488, ErrorType.PASSWORD_EXPIRED);// the password must be changed
		ERROR_TYPES.put(1222, ErrorType.ROW_LOCKED);// lock request time out period exceeded
		ERROR_TYPES.put(208, ErrorType.TABLE_NOT_FOUND);// invalid object name
	}

	private static final String UNORDERED = "ORDER BY (SELECT NULL) ";
	private static final String UPDATE_LOCK = "WITH (UPDLOCK, ROWLOCK)";
	private static final String UPDATE_LOCK_NOWAIT = "WITH (UPDLOCK, ROWLOCK, NOWAIT)";
	private static final String JDBC_URL_PREFIX = "jdbc:sqlserver://";
	private static final String JTDS_URL_PREFIX = "jdbc:jtds:sqlserver://";
	private static final String DATABASE_NAME = "databaseName";
	private static final String DATABASE = "database";
	private static final String INSTANCE = "instance";
	private static final String SERVER_NAME = "serverName";
	/**
	 * The server accepts 2100 parameters per request, the driver using two of those for the statement itself
	 */
	private static final int MAXIMUM_STATEMENT_PARAMETERS = 2098;

	private final boolean nowait;

	SQLServerDatabase(String url) {
		this(url, true);
	}

	SQLServerDatabase(String url, boolean nowait) {
		super(url);
		this.nowait = nowait;
	}

	@Override
	public String name() {
		// jdbc:sqlserver://[host[\instance][:port]][;property=value] or jdbc:jtds:sqlserver://host[:port][/database][;property=value]
		String database = property(DATABASE_NAME);
		if (database == null) {
			database = property(DATABASE);
		}
		if (database != null) {
			return database;
		}
		String name = removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX, JTDS_URL_PREFIX);
		if (name.contains("/")) {
			return name.substring(name.lastIndexOf('/') + 1);
		}
		String instance = property(INSTANCE);
		if (instance != null) {
			return instance;
		}
		if (name.contains("\\")) {
			name = name.substring(name.lastIndexOf('\\') + 1);
		}
		if (name.contains(":")) {
			name = name.substring(0, name.indexOf(':'));
		}
		String serverName = property(SERVER_NAME);

		return name.isEmpty() && serverName != null ? serverName : name;
	}

	/**
	 * @return the value of the given url property, the names being case-insensitive, null if not specified
	 */
	private String property(String property) {
		String[] properties = url().split(";");
		for (int i = 1; i < properties.length; i++) {
			int valueIndex = properties[i].indexOf('=');
			if (valueIndex != -1 && properties[i].substring(0, valueIndex).trim().equalsIgnoreCase(property)) {
				return properties[i].substring(valueIndex + 1).trim();
			}
		}

		return null;
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return "SELECT NEXT VALUE FOR " + requireNonNull(sequenceName);
	}

	/**
	 * @return an empty string, the rows being locked via {@link #selectForUpdateTableHint()}
	 */
	@Override
	public String selectForUpdateClause() {
		return "";
	}

	/**
	 * An update lock does not block readers, only others requesting an update lock, which with NOWAIT fail with a lock request timeout.
	 */
	@Override
	public String selectForUpdateTableHint() {
		return nowait ? UPDATE_LOCK_NOWAIT : UPDATE_LOCK;
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
		if (limit == null && offset == null) {
			return "";
		}
		// OFFSET and FETCH are a part of the ORDER BY clause, which is thereby required, as is the OFFSET when fetching
		String offsetFetchNext = createOffsetFetchNextClause(limit, offset == null ? Integer.valueOf(0) : offset);

		return ordered ? offsetFetchNext : UNORDERED + offsetFetchNext;
	}

	/**
	 * @return true
	 */
	@Override
	public boolean subqueryRequiresAlias() {
		return true;
	}

	@Override
	public int maximumParameters() {
		return MAXIMUM_STATEMENT_PARAMETERS;
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		// @@IDENTITY returns the value generated by a trigger, if one inserts, and SCOPE_IDENTITY() returns null from a separate statement
		throw new UnsupportedOperationException("SQL Server provides no reliable way of querying for the last generated value, " +
						"use an identity based generator, relying on Statement.getGeneratedKeys(), instead of an automatic one");
	}

	@Override
	protected ErrorType errorType(SQLException exception) {
		if (exception.getErrorCode() == CONSTRAINT_CONFLICT) {
			return constraintConflict(exception);
		}
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
				// Cannot insert the value NULL into column 'name', table 'db.dbo.table'; column does not allow nulls. INSERT fails.
			case VALUE_TOO_LARGE:
				// String or binary data would be truncated in table 'db.dbo.table', column 'name'. Truncated value: 'abc'.
				return between(message, COLUMN, "'");
			case UNIQUE_CONSTRAINT:
				// Violation of UNIQUE KEY constraint 'name'. Cannot insert duplicate key in object 'dbo.table'. The duplicate key value is (A).
				return between(message, DUPLICATE_KEY_VALUE, ").", 1);
			default:
				return null;
		}
	}

	/**
	 * The statement conflicted with the FOREIGN KEY, REFERENCE or CHECK constraint "name", the type preceding the quoted name
	 */
	private static ErrorType constraintConflict(SQLException exception) {
		String message = exception.getMessage();
		if (message == null) {
			return null;
		}
		int nameIndex = message.indexOf('"');
		String beforeName = nameIndex == -1 ? message : message.substring(0, nameIndex);
		if (beforeName.contains(FOREIGN_KEY)) {
			return ErrorType.PARENT_MISSING;
		}
		if (beforeName.contains(REFERENCE)) {
			return ErrorType.CHILD_EXISTS;
		}
		if (beforeName.contains(CHECK)) {
			return ErrorType.CHECK_CONSTRAINT;
		}

		return null;
	}

	private static String between(String message, String prefix, String suffix) {
		return between(message, prefix, suffix, 0);
	}

	/**
	 * @param suffixIncluded the number of suffix characters to include
	 */
	private static String between(String message, String prefix, String suffix, int suffixIncluded) {
		int prefixIndex = message.indexOf(prefix);
		if (prefixIndex == -1) {
			return null;
		}
		int beginIndex = prefixIndex + prefix.length();
		int endIndex = message.indexOf(suffix, beginIndex);

		return endIndex == -1 ? null : message.substring(beginIndex, endIndex + suffixIncluded);
	}
}
