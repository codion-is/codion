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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A {@link HttpTransport} based on {@link HttpURLConnection}, the universally available (JVM and Android) HTTP
 * client. Used as the default when {@code java.net.http} is absent — notably Android, where {@link HttpURLConnection}
 * runs on the platform's built-in engine (OkHttp). HTTP/1.1 only, which is plenty for the EntityService's
 * request/response traffic. A per-instance {@link CookieManager} carries the session cookie across requests.
 */
final class UrlConnectionTransport implements HttpTransport {

	private static final Map<String, List<String>> NO_REQUEST_HEADERS = Collections.emptyMap();

	private final CookieManager cookieManager = new CookieManager();
	private final int connectTimeout;
	private final int socketTimeout;

	UrlConnectionTransport(int connectTimeout, int socketTimeout) {
		this.connectTimeout = connectTimeout;
		this.socketTimeout = socketTimeout;
	}

	@Override
	public Response post(String url, String[] headers, byte @Nullable [] body) throws IOException {
		URI uri = URI.create(url);
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(socketTimeout);
		// headers arrive as a flat [name, value, name, value...] array
		for (int i = 0; i < headers.length - 1; i += 2) {
			connection.addRequestProperty(headers[i], headers[i + 1]);
		}
		addCookies(connection, uri);
		if (body != null) {
			connection.setDoOutput(true);
			connection.setFixedLengthStreamingMode(body.length);
			try (OutputStream outputStream = connection.getOutputStream()) {
				outputStream.write(body);
			}
		}
		int statusCode = connection.getResponseCode();
		cookieManager.put(uri, connection.getHeaderFields());

		return new Response(statusCode, responseBody(connection, statusCode));
	}

	private void addCookies(HttpURLConnection connection, URI uri) throws IOException {
		for (Map.Entry<String, List<String>> cookie : cookieManager.get(uri, NO_REQUEST_HEADERS).entrySet()) {
			for (String value : cookie.getValue()) {
				connection.addRequestProperty(cookie.getKey(), value);
			}
		}
	}

	// Non-2xx/3xx bodies come from the error stream; getInputStream() would throw. Read manually rather than via
	// InputStream.readAllBytes(), which is API 33+ on Android (this transport's main home).
	private static byte[] responseBody(HttpURLConnection connection, int statusCode) throws IOException {
		try (InputStream inputStream = statusCode < HttpURLConnection.HTTP_BAD_REQUEST
						? connection.getInputStream() : connection.getErrorStream()) {
			if (inputStream == null) {
				return new byte[0];
			}
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				bytes.write(buffer, 0, read);
			}

			return bytes.toByteArray();
		}
	}
}
