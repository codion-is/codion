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
package is.codion.dbms.db2;

import is.codion.common.db.database.AbstractDatabase;
import is.codion.common.db.database.ClientInfo;

import java.sql.Connection;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Db2 database implementation.
 */
final class Db2Database extends AbstractDatabase {

	private static final int REFERENTIAL_INTEGRITY_INSERT_UPDATE = -530;
	private static final int REFERENTIAL_INTEGRITY_DELETE_1 = -532;
	private static final int REFERENTIAL_INTEGRITY_DELETE_2 = -536;
	private static final int UNIQUE_CONSTRAINT = -803;
	private static final int AUTHENTICATION_ERROR = -4214;
	private static final int TIMEOUT_ERROR_1 = -911;
	private static final int TIMEOUT_ERROR_2 = -913;

	private static final String APPLICATION_NAME = "ApplicationName";
	private static final String CLIENT_USER = "ClientUser";
	private static final String CLIENT_HOSTNAME = "ClientHostname";
	private static final String JDBC_URL_PREFIX = "jdbc:db2:";

	Db2Database(String url) {
		super(url);
	}

	@Override
	public String name() {
		String name = removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX);
		if (name.contains("/")) {
			name = name.substring(name.lastIndexOf('/') + 1);
		}

		return name;
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		return "SELECT PREVIOUS VALUE FOR " + requireNonNull(idSource);
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return "SELECT NEXT VALUE FOR " + requireNonNull(sequenceName);
	}

	@Override
	public String selectForUpdateClause() {
		return FOR_UPDATE;
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset) {
		return createLimitOffsetClause(limit, offset);
	}

	/**
	 * <p>Db2 carries this better than most, the driver mapping the standard properties onto the client
	 * special registers - {@code CURRENT CLIENT_USERID}, {@code CURRENT CLIENT_APPLNAME} and
	 * {@code CURRENT CLIENT_WRKSTNNAME} - which are readable from SQL, so a trigger auditing on behalf of a
	 * shared database user can name the actual user. The driver sends them with the next request rather
	 * than on their own, so the stamp costs no round trip.
	 * <p>Verified against JCC 4.33.31 and the {@code ibmcom/db2} image, 2026-08-22.
	 */
	@Override
	public void clientInfo(Connection connection, ClientInfo clientInfo) {
		clientInfoProperty(connection, CLIENT_USER, clientInfo.user());
		clientInfoProperty(connection, APPLICATION_NAME, clientInfo.clientType());
		clientInfoProperty(connection, CLIENT_HOSTNAME, clientInfo.host().orElse(""));
	}

	@Override
	public boolean isReferentialIntegrityException(SQLException exception) {
		int errorCode = requireNonNull(exception).getErrorCode();

		return errorCode == REFERENTIAL_INTEGRITY_INSERT_UPDATE ||
						errorCode == REFERENTIAL_INTEGRITY_DELETE_1 ||
						errorCode == REFERENTIAL_INTEGRITY_DELETE_2;
	}

	@Override
	public boolean isUniqueConstraintException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == UNIQUE_CONSTRAINT;
	}

	@Override
	public boolean isAuthenticationException(SQLException exception) {
		return requireNonNull(exception).getErrorCode() == AUTHENTICATION_ERROR;
	}

	@Override
	public boolean isTimeoutException(SQLException exception) {
		int errorCode = requireNonNull(exception).getErrorCode();

		return errorCode == TIMEOUT_ERROR_1 ||
						errorCode == TIMEOUT_ERROR_2;
	}
}
