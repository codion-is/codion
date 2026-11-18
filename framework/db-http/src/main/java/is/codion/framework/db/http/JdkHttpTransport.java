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

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.zip.GZIPInputStream;

/**
 * The default {@link HttpTransport} implementation, based on {@link java.net.http.HttpClient}.
 * This is the only place {@code java.net.http} is referenced, isolating it from platforms where
 * the module is unavailable.
 * <p>{@link java.net.http.HttpClient} neither asks for a compressed response nor decompresses one, so this transport
 * does both: it asks for gzip, which the EntityService compresses a response with when asked, entities in JSON
 * shrinking to about a tenth, and decompresses a gzip response.
 */
final class JdkHttpTransport implements HttpTransport {

	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	private static final String CONTENT_ENCODING = "Content-Encoding";
	private static final String GZIP = "gzip";

	private final HttpClient httpClient;
	private final @Nullable Duration socketTimeout;

	JdkHttpTransport(int connectTimeout, int socketTimeout) {
		HttpClient.Builder builder = HttpClient.newBuilder()
						.executor(new SynchronousExecutor())
						.cookieHandler(new CookieManager());
		// Both builders reject a non-positive duration, so zero (no timeout) is expressed by leaving the value
		// unset - which is the HttpClient default in either case.
		if (connectTimeout > 0) {
			builder.connectTimeout(Duration.ofMillis(connectTimeout));
		}
		this.httpClient = builder.build();
		this.socketTimeout = socketTimeout > 0 ? Duration.ofMillis(socketTimeout) : null;
	}

	@Override
	public Response post(String url, String[] headers, byte @Nullable [] body) throws IOException {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.POST(body == null ? BodyPublishers.noBody() : BodyPublishers.ofByteArray(body))
						.headers(headers)
						.header(ACCEPT_ENCODING, GZIP);
		if (socketTimeout != null) {
			builder.timeout(socketTimeout);
		}
		HttpRequest request = builder.build();
		try {
			HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());

			return new Response(response.statusCode(), body(response));
		}
		catch (HttpTimeoutException e) {
			// As HttpURLConnection reports it, callers not having to know of java.net.http
			SocketTimeoutException timeout = new SocketTimeoutException(e.getMessage());
			timeout.initCause(e);

			throw timeout;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	private static byte[] body(HttpResponse<byte[]> response) throws IOException {
		byte[] body = response.body();
		boolean gzip = response.headers().firstValue(CONTENT_ENCODING)
						.map(GZIP::equalsIgnoreCase)
						.orElse(false);
		if (!gzip || body.length == 0) {
			return body;
		}
		try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(body))) {
			return input.readAllBytes();
		}
	}

	private static final class SynchronousExecutor implements Executor {

		@Override
		public void execute(Runnable command) {
			command.run();
		}
	}
}
