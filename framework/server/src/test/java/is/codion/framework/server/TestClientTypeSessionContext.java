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
import java.util.Optional;

import static is.codion.framework.server.TestSessionContext.CALLS;

/**
 * A client type specific counterpart to {@link TestSessionContext}, recording into the same list so that
 * the order the two are applied in is observable. Armed separately, leaving the tests which predate it
 * seeing only the shared one.
 */
public final class TestClientTypeSessionContext implements SessionContext {

	static final String CLIENT_TYPE = "ClientTypeSessionContextTest";

	private static volatile boolean armed = false;

	static void arm() {
		armed = true;
	}

	static void disarm() {
		armed = false;
	}

	@Override
	public Optional<String> clientType() {
		return Optional.of(CLIENT_TYPE);
	}

	@Override
	public void prepare(ClientInfo clientInfo, Connection connection) {
		if (armed) {
			CALLS.add("prepare:specific");
		}
	}

	@Override
	public void release(ClientInfo clientInfo, Connection connection) {
		if (armed) {
			CALLS.add("release:specific");
		}
	}
}
