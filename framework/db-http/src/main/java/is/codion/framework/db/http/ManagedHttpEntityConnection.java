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
 * Copyright (c) 2017 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.framework.db.AbstractEntityConnection;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A self-managing {@link HttpEntityConnection}, connecting on demand and reconnecting
 * when the underlying connection has gone bad.
 * @see HttpEntityConnection#builder()
 */
final class ManagedHttpEntityConnection extends AbstractEntityConnection
				implements HttpEntityConnection {

	private static final Logger LOG = LoggerFactory.getLogger(ManagedHttpEntityConnection.class);

	private final String hostname;
	private final int port;
	private final int securePort;
	private final boolean https;
	private final boolean json;
	private final int socketTimeout;
	private final int connectTimeout;
	private final @Nullable Domain domain;

	ManagedHttpEntityConnection(DefaultHttpEntityConnectionBuilder builder) {
		super(builder);
		this.hostname = requireNonNull(builder.hostname, "hostname must be specified");
		this.port = builder.port;
		this.securePort = builder.securePort;
		this.https = builder.https;
		this.json = builder.json;
		this.socketTimeout = builder.socketTimeout;
		this.connectTimeout = builder.connectTimeout;
		this.domain = builder.domain;
	}

	@Override
	public Optional<String> description() {
		return Optional.of(DESCRIPTION.optional().orElse(hostname));
	}

	@Override
	protected EntityConnection connect() {
		try {
			LOG.debug("Initializing connection for {}", user());
			AbstractHttpEntityConnection.DefaultBuilder builder =
							new AbstractHttpEntityConnection.DefaultBuilder()
											.domain(domainType());
			Domain localDomain = domain != null ? domain : localDomain(domainType());
			if (localDomain != null) {
				builder.domain(localDomain);
			}
			return builder.hostname(hostname)
							.port(port)
							.securePort(securePort)
							.user(user())
							.clientType(clientType())
							.clientId(clientId())
							.clientVersion(clientVersion().orElse(null))
							.json(json)
							.https(https)
							.socketTimeout(socketTimeout)
							.connectTimeout(connectTimeout)
							.build();
		}
		catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}

	private static @Nullable Domain localDomain(DomainType domainType) {
		return Domain.domains().stream()
						.filter(domain -> domain.type().equals(domainType))
						.findAny()
						.orElse(null);
	}
}
