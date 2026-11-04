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
package is.codion.framework.db.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class HttpTransportTest {

	private static final int TIMEOUT = 200;
	private static final String[] HEADERS = {"Content-Type", "text/plain"};

	@Test
	void timeout_jdk() throws IOException {
		timeout(new JdkHttpTransport(TIMEOUT, TIMEOUT));
	}

	@Test
	void timeout_urlConnection() throws IOException {
		timeout(new UrlConnectionTransport(TIMEOUT, TIMEOUT));
	}

	// A server never answering, the connection accepted by the system's backlog alone, each transport reporting it alike
	private static void timeout(HttpTransport transport) throws IOException {
		try (ServerSocket silent = new ServerSocket(0)) {
			assertThrows(SocketTimeoutException.class, () ->
							transport.post("http://localhost:" + silent.getLocalPort() + "/", HEADERS, null));
		}
	}
}
