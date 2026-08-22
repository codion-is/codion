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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.ClientInfo;
import is.codion.common.db.database.SessionContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.synchronizedList;

/**
 * <p>Registered with the {@link java.util.ServiceLoader}, and therefore present for every test in this
 * module, so it does nothing at all until a test arms it. The contexts are loaded once per JVM, which is
 * why arming is a static switch rather than an instance the test hands over.
 */
public final class TestSessionContext implements SessionContext {

	static final List<String> CALLS = synchronizedList(new ArrayList<>());

	/**
	 * The physical connection behind each prepare, so that a test can tell a discarded connection from a
	 * reused one - identity being the only way to see the difference from out here.
	 */
	static final List<Connection> CONNECTIONS = synchronizedList(new ArrayList<>());

	private static volatile boolean armed = false;
	private static volatile boolean failOnPrepare = false;
	private static volatile boolean failOnRelease = false;

	static void arm() {
		CALLS.clear();
		CONNECTIONS.clear();
		failOnPrepare = false;
		failOnRelease = false;
		armed = true;
	}

	static void armFailing(boolean onPrepare) {
		arm();
		failOnPrepare = onPrepare;
		failOnRelease = !onPrepare;
	}

	static void disarm() {
		armed = false;
		CONNECTIONS.clear();
		failOnPrepare = false;
		failOnRelease = false;
		CALLS.clear();
	}

	@Override
	public void prepare(ClientInfo clientInfo, Connection connection) throws SQLException {
		if (armed) {
			CALLS.add("prepare:" + clientInfo.user() + "@" + clientInfo.clientType());
			//prove the connection is usable, this being the point of receiving it
			connection.createStatement().close();
			CONNECTIONS.add(connection.unwrap(Connection.class));
			if (failOnPrepare) {
				throw new SQLException("preparing failed");
			}
		}
	}

	@Override
	public void release(ClientInfo clientInfo, Connection connection) throws SQLException {
		if (armed) {
			CALLS.add("release:" + clientInfo.user() + "@" + clientInfo.clientType());
			if (failOnRelease) {
				throw new SQLException("releasing failed");
			}
		}
	}
}
