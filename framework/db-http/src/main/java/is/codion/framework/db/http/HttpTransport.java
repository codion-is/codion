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

/**
 * Performs the actual HTTP communication for {@link HttpEntityConnection} — an internal detail of the connection.
 * <p>
 * The {@link java.net.http.HttpClient} based transport is used where that module is present, falling back to an
 * {@link java.net.HttpURLConnection} based transport where it is not (notably Android, which lacks
 * {@code java.net.http}).
 */
interface HttpTransport {

	/**
	 * Sends a POST request to the given url and returns the response.
	 * @param url the full request url
	 * @param headers the request headers, as a flat array of name, value, name, value...
	 * @param body the request body or null in case of no body
	 * @return the response
	 * @throws IOException in case of an I/O error
	 */
	Response post(String url, String[] headers, byte @Nullable [] body) throws IOException;

	/**
	 * An HTTP response.
	 * @param statusCode the HTTP status code
	 * @param body the response body
	 */
	record Response(int statusCode, byte[] body) {}

	/**
	 * Returns a {@link HttpTransport} instance: the {@link java.net.http.HttpClient} (HTTP/2) transport where that
	 * module is present, otherwise the {@link java.net.HttpURLConnection} (HTTP/1.1) transport, which is universally
	 * available — notably on Android.
	 * @param connectTimeout the connect timeout in milliseconds
	 * @param socketTimeout the socket (read) timeout in milliseconds
	 * @return a new {@link HttpTransport} instance
	 */
	static HttpTransport instance(int connectTimeout, int socketTimeout) {
		// JdkHttpTransport is the sole java.net.http reference, constructed only in the present-branch, so it stays
		// unloaded — and the module unresolved — where java.net.http is unavailable.
		if (httpClientAvailable()) {
			return new JdkHttpTransport(connectTimeout, socketTimeout);
		}

		return new UrlConnectionTransport(connectTimeout, socketTimeout);
	}

	private static boolean httpClientAvailable() {
		try {
			// false: probe for the class without initializing it, avoiding any touch of java.net.http where absent.
			Class.forName("java.net.http.HttpClient", false, HttpTransport.class.getClassLoader());

			return true;
		}
		catch (ClassNotFoundException e) {
			return false;
		}
	}
}
