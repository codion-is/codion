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
import is.codion.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SessionContextsTest {

	private static final ClientInfo CLIENT_INFO = new ClientInfo("scott", "SessionContextsTest", "localhost");

	//the contexts under test never touch the connection, only the framework's pairing is being tested
	private static final Connection CONNECTION = (Connection) Proxy.newProxyInstance(
					Connection.class.getClassLoader(), new Class[] {Connection.class}, (proxy, method, args) -> null);

	@Test
	void empty() {
		SessionContexts contexts = new SessionContexts(CLIENT_INFO, emptyList());
		assertTrue(contexts.empty());
		contexts.prepare(CONNECTION);
		assertTrue(contexts.release(CONNECTION));
	}

	@Test
	void appliedInOrderRemovedInReverse() {
		List<String> calls = new ArrayList<>();
		SessionContexts contexts = new SessionContexts(CLIENT_INFO,
						asList(new Recording("one", calls), new Recording("two", calls)));

		contexts.prepare(CONNECTION);
		assertEquals(asList("prepare:one", "prepare:two"), calls);

		assertTrue(contexts.release(CONNECTION));
		assertEquals(asList("prepare:one", "prepare:two", "release:two", "release:one"), calls);
	}

	@Test
	void releaseOnlyUnwindsOnce() {
		List<String> calls = new ArrayList<>();
		SessionContexts contexts = new SessionContexts(CLIENT_INFO, singletonList(new Recording("one", calls)));

		contexts.prepare(CONNECTION);
		assertTrue(contexts.release(CONNECTION));
		//a second return, of a connection which is no longer prepared, must not run the contexts again
		assertTrue(contexts.release(CONNECTION));
		assertEquals(asList("prepare:one", "release:one"), calls);
	}

	@Test
	void failedPrepareUnwindsWhatItApplied() {
		List<String> calls = new ArrayList<>();
		SessionContexts contexts = new SessionContexts(CLIENT_INFO,
						asList(new Recording("one", calls), new Failing("two", calls, true), new Recording("three", calls)));

		//the client's operation fails rather than running against a half prepared connection
		assertThrows(DatabaseException.class, () -> contexts.prepare(CONNECTION));
		//'one' applied and was removed again, 'three' was never reached
		assertEquals(asList("prepare:one", "prepare:two", "release:one"), calls);

		//and nothing is left applied, so the return finds nothing to do
		assertTrue(contexts.release(CONNECTION));
		assertEquals(asList("prepare:one", "prepare:two", "release:one"), calls);
	}

	@Test
	void failedPrepareWithFailedUnwindReportsUnclean() {
		List<String> calls = new ArrayList<>();
		//'one' applies but can not be removed, 'two' fails to apply: the unwind leaves 'one' on the connection
		SessionContexts contexts = new SessionContexts(CLIENT_INFO,
						asList(new Failing("one", calls, false), new Failing("two", calls, true)));

		assertThrows(DatabaseException.class, () -> contexts.prepare(CONNECTION));
		assertEquals(asList("prepare:one", "prepare:two", "release:one"), calls);

		//so the return which follows must report the connection unclean, as it would have had
		//the failure happened on the way out, and there is nothing left for it to remove
		assertFalse(contexts.release(CONNECTION));
		assertEquals(asList("prepare:one", "prepare:two", "release:one"), calls);
		//the mark is consumed by that one release
		assertTrue(contexts.release(CONNECTION));
	}

	@Test
	void resetForgetsWithoutReleasing() {
		List<String> calls = new ArrayList<>();
		SessionContexts contexts = new SessionContexts(CLIENT_INFO, singletonList(new Recording("one", calls)));

		contexts.prepare(CONNECTION);
		//the connection has gone bad, its state died with it
		contexts.reset();
		assertTrue(contexts.release(CONNECTION));
		assertEquals(singletonList("prepare:one"), calls);

		//and a replacement starts from zero rather than from a count carried over
		contexts.prepare(CONNECTION);
		assertTrue(contexts.release(CONNECTION));
		assertEquals(asList("prepare:one", "prepare:one", "release:one"), calls);
	}

	@Test
	void failedReleaseReportsUnclean() {
		List<String> calls = new ArrayList<>();
		SessionContexts contexts = new SessionContexts(CLIENT_INFO,
						asList(new Recording("one", calls), new Failing("two", calls, false)));

		contexts.prepare(CONNECTION);
		//false tells the caller to discard the connection, and the remaining contexts still get their turn
		assertFalse(contexts.release(CONNECTION));
		assertEquals(asList("prepare:one", "prepare:two", "release:two", "release:one"), calls);
	}

	private static class Recording implements SessionContext {

		private final String name;
		private final List<String> calls;

		private Recording(String name, List<String> calls) {
			this.name = name;
			this.calls = calls;
		}

		@Override
		public void prepare(ClientInfo clientInfo, Connection connection) throws SQLException {
			calls.add("prepare:" + name);
		}

		@Override
		public void release(ClientInfo clientInfo, Connection connection) throws SQLException {
			calls.add("release:" + name);
		}
	}

	private static final class Failing extends Recording {

		private final boolean onPrepare;

		private Failing(String name, List<String> calls, boolean onPrepare) {
			super(name, calls);
			this.onPrepare = onPrepare;
		}

		@Override
		public void prepare(ClientInfo clientInfo, Connection connection) throws SQLException {
			super.prepare(clientInfo, connection);
			if (onPrepare) {
				throw new SQLException("prepare failed");
			}
		}

		@Override
		public void release(ClientInfo clientInfo, Connection connection) throws SQLException {
			super.release(clientInfo, connection);
			if (!onPrepare) {
				throw new SQLException("release failed");
			}
		}
	}
}
