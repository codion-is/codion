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
import java.util.ServiceLoader;

/**
 * Performs the actual HTTP communication for {@link HttpEntityConnection}.
 * <p>
 * The default implementation is based on {@link java.net.http.HttpClient}. On platforms lacking the
 * {@code java.net.http} module (such as Android), an alternative implementation can be supplied via a
 * {@link Factory} registered with the {@link ServiceLoader}; when one is found it takes precedence over
 * the default.
 */
public interface HttpTransport {

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
	 * Creates {@link HttpTransport} instances.
	 */
	interface Factory {

		/**
		 * @param connectTimeout the connect timeout in milliseconds
		 * @param socketTimeout the socket (read) timeout in milliseconds
		 * @return a new {@link HttpTransport} instance
		 */
		HttpTransport create(int connectTimeout, int socketTimeout);
	}

	/**
	 * Returns a {@link HttpTransport} instance, based on the first {@link Factory} found via the
	 * {@link ServiceLoader}, or the default {@link java.net.http.HttpClient} based implementation
	 * if none is available.
	 * @param connectTimeout the connect timeout in milliseconds
	 * @param socketTimeout the socket (read) timeout in milliseconds
	 * @return a new {@link HttpTransport} instance
	 */
	static HttpTransport instance(int connectTimeout, int socketTimeout) {
		return ServiceLoader.load(Factory.class).findFirst()
						// orElseGet, not orElse: the default factory references java.net.http, so it must
						// only be touched when no override is present, keeping it unresolved on Android.
						.orElseGet(JdkHttpTransport.Factory::new)
						.create(connectTimeout, socketTimeout);
	}
}
