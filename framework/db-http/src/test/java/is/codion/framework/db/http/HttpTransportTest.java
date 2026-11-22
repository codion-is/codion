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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

public final class HttpTransportTest {

	private static final int TIMEOUT = 200;
	private static final int SERVER_TIMEOUT = 10_000;
	private static final String[] HEADERS = {"Content-Type", "text/plain"};

	@Test
	void r8RulesIgnoreHttpClient() throws IOException {
		//java.net.http is absent on Android, where JdkHttpTransport is never loaded, the module's R8 rules not warning of it
		String rules;
		try (InputStream input = HttpTransport.class.getResourceAsStream("/META-INF/proguard/codion-framework-db-http.pro")) {
			rules = new String(requireNonNull(input).readAllBytes(), UTF_8);
		}
		assertTrue(rules.contains("-dontwarn " + HttpClient.class.getPackageName() + ".**\n"));
	}

	@Test
	void timeout_jdk() throws IOException {
		timeout(new JdkHttpTransport(TIMEOUT, TIMEOUT));
	}

	@Test
	void timeout_urlConnection() throws IOException {
		timeout(new UrlConnectionTransport(TIMEOUT, TIMEOUT));
	}

	@Test
	void gzip_jdk() throws IOException {
		// a response compressed when asked for, as the EntityService compresses one
		byte[] body = body();
		AtomicReference<String> acceptEncoding = new AtomicReference<>();
		HttpServer server = server(exchange -> {
			acceptEncoding.set(exchange.getRequestHeaders().getFirst("Accept-Encoding"));
			exchange.getResponseHeaders().add("Content-Encoding", "gzip");
			respond(exchange, gzip(body));
		});
		try {
			HttpTransport.Response response = new JdkHttpTransport(SERVER_TIMEOUT, SERVER_TIMEOUT)
							.post("http://localhost:" + server.getAddress().getPort() + "/", HEADERS, null);
			assertEquals("gzip", acceptEncoding.get());
			assertEquals(200, response.statusCode());
			assertArrayEquals(body, response.body());
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void uncompressed_jdk() throws IOException {
		// a response left uncompressed, though asked for, returned as it is
		byte[] body = body();
		HttpServer server = server(exchange -> respond(exchange, body));
		try {
			HttpTransport.Response response = new JdkHttpTransport(SERVER_TIMEOUT, SERVER_TIMEOUT)
							.post("http://localhost:" + server.getAddress().getPort() + "/", HEADERS, null);
			assertArrayEquals(body, response.body());
		}
		finally {
			server.stop(0);
		}
	}

	// A server never answering, the connection accepted by the system's backlog alone, each transport reporting it alike
	private static void timeout(HttpTransport transport) throws IOException {
		try (ServerSocket silent = new ServerSocket(0)) {
			assertThrows(SocketTimeoutException.class, () ->
							transport.post("http://localhost:" + silent.getLocalPort() + "/", HEADERS, null));
		}
	}

	private static HttpServer server(HttpHandler handler) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/", handler);
		server.start();

		return server;
	}

	private static void respond(HttpExchange exchange, byte[] body) throws IOException {
		exchange.getRequestBody().readAllBytes();
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(body);
		}
	}

	private static byte[] body() {
		StringBuilder body = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			body.append("{\"entityType\":\"employees.employee\",\"values\":{\"empno\":").append(i).append("}}");
		}

		return body.toString().getBytes(UTF_8);
	}

	private static byte[] gzip(byte[] bytes) throws IOException {
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		try (GZIPOutputStream output = new GZIPOutputStream(compressed)) {
			output.write(bytes);
		}

		return compressed.toByteArray();
	}
}
