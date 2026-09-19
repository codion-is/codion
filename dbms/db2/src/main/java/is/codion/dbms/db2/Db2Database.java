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
import is.codion.common.db.exception.ErrorType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A Db2 database implementation.
 */
final class Db2Database extends AbstractDatabase {

	private static final Map<Integer, ErrorType> ERROR_TYPES = new HashMap<>();

	static {
		// the error codes not covered by the sql state defaults
		ERROR_TYPES.put(-530, ErrorType.PARENT_MISSING);// 23503, a child row being referenced reported with 23504
		ERROR_TYPES.put(-911, ErrorType.ROW_LOCKED);// rolled back due to a deadlock or lock timeout
		ERROR_TYPES.put(-913, ErrorType.ROW_LOCKED);// unsuccessful due to a deadlock or lock timeout
		ERROR_TYPES.put(-952, ErrorType.TIMEOUT);// processing cancelled due to an interrupt
		ERROR_TYPES.put(-551, ErrorType.MISSING_PRIVILEGES);
		ERROR_TYPES.put(-30082, ErrorType.AUTHENTICATION);// security processing failed, -4214 being covered by its sql state
		ERROR_TYPES.put(-204, ErrorType.TABLE_NOT_FOUND);
	}

	private static final String APPLICATION_NAME = "ApplicationName";
	private static final String CLIENT_USER = "ClientUser";
	private static final String CLIENT_HOSTNAME = "ClientHostname";
	private static final String JDBC_URL_PREFIX = "jdbc:db2:";

	Db2Database(String url) {
		super(url);
	}

	@Override
	public String name() {
		// jdbc:db2://host:port/database:property=value; or jdbc:db2:database
		String name = databaseOrHost(removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX));
		int propertiesIndex = name.indexOf(':');

		return propertiesIndex == -1 ? name : name.substring(0, propertiesIndex);
	}

	@Override
	public String autoIncrementQuery(String idSource) {
		return "VALUES PREVIOUS VALUE FOR " + requireNonNull(idSource);
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		return "VALUES NEXT VALUE FOR " + requireNonNull(sequenceName);
	}

	@Override
	public String selectForUpdateClause() {
		return FOR_UPDATE;
	}

	@Override
	public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
		return createOffsetFetchNextClause(limit, offset);
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

	/**
	 * Note that the messages contain tokens only, {@code SQLCODE=-407, SQLSTATE=23502, SQLERRMC=TBSPACEID=2, TABLEID=4, COLNO=1},
	 * unless the {@code retrieveMessagesFromServerOnGetMessage} driver property is enabled, so no detail is provided.
	 */
	@Override
	protected ErrorType errorType(SQLException exception) {
		ErrorType errorType = ERROR_TYPES.get(exception.getErrorCode());

		return errorType == null ? super.errorType(exception) : errorType;
	}
}
