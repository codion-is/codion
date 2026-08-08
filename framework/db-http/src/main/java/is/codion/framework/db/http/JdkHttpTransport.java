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

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * The default {@link HttpTransport} implementation, based on {@link java.net.http.HttpClient}.
 * This is the only place {@code java.net.http} is referenced, isolating it from platforms where
 * the module is unavailable.
 */
final class JdkHttpTransport implements HttpTransport {

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
						.headers(headers);
		if (socketTimeout != null) {
			builder.timeout(socketTimeout);
		}
		HttpRequest request = builder.build();
		try {
			HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());

			return new Response(response.statusCode(), response.body());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	private static final class SynchronousExecutor implements Executor {

		@Override
		public void execute(Runnable command) {
			command.run();
		}
	}
}
