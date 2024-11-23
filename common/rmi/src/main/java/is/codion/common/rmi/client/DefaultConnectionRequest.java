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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultConnectionRequest implements ConnectionRequest, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final User user;
	private final UUID clientId;
	private final String clientTypeId;
	private final Locale clientLocale = Locale.getDefault();
	private final ZoneId clientTimeZone = ZoneId.systemDefault();
	private final Version clientVersion;
	private final Version frameworkVersion = Version.version();
	private final Map<String, Object> parameters;

	private DefaultConnectionRequest(DefaultBuilder builder) {
		this.user = requireNonNull(builder.user, "user must be specified");
		this.clientId = builder.clientId == null ? UUID.randomUUID() : builder.clientId;
		this.clientTypeId = requireNonNull(builder.clientTypeId, "clientTypeId must be specified");
		this.clientVersion = builder.clientVersion;
		this.parameters = builder.parameters == null ? null : unmodifiableMap(builder.parameters);
	}

	@Override
	public User user() {
		return user;
	}

	@Override
	public UUID clientId() {
		return clientId;
	}

	@Override
	public String clientTypeId() {
		return clientTypeId;
	}

	@Override
	public Locale clientLocale() {
		return clientLocale;
	}

	@Override
	public ZoneId clientTimeZone() {
		return clientTimeZone;
	}

	@Override
	public Optional<Version> clientVersion() {
		return Optional.ofNullable(clientVersion);
	}

	@Override
	public Version frameworkVersion() {
		return frameworkVersion;
	}

	@Override
	public Map<String, Object> parameters() {
		return parameters == null ? emptyMap() : parameters;
	}

	@Override
	public ConnectionRequest copy() {
		Builder builder = new DefaultBuilder()
						.user(user.copy())
						.clientId(clientId)
						.clientTypeId(clientTypeId)
						.clientVersion(clientVersion);
		if (parameters != null) {
			parameters.forEach(builder::parameter);
		}

		return builder.build();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof ConnectionRequest && clientId.equals(((ConnectionRequest) obj).clientId());
	}

	@Override
	public int hashCode() {
		return clientId.hashCode();
	}

	@Override
	public String toString() {
		return user + " [" + clientTypeId + "] - " + clientId;
	}

	static final class DefaultBuilder implements Builder {

		private User user;
		private UUID clientId;
		private String clientTypeId;
		private Version clientVersion;
		private Map<String, Object> parameters;

		DefaultBuilder() {}

		@Override
		public Builder user(User user) {
			this.user = requireNonNull(user);
			return this;
		}

		@Override
		public Builder clientId(UUID clientId) {
			this.clientId = requireNonNull(clientId);
			return this;
		}

		@Override
		public Builder clientTypeId(String clientTypeId) {
			this.clientTypeId = requireNonNull(clientTypeId);
			return this;
		}

		@Override
		public Builder clientVersion(Version clientVersion) {
			this.clientVersion = clientVersion;
			return this;
		}

		@Override
		public Builder parameter(String key, Object value) {
			if (parameters == null) {
				parameters = new HashMap<>();
			}
			parameters.put(key, value);
			return this;
		}

		@Override
		public ConnectionRequest build() {
			return new DefaultConnectionRequest(this);
		}
	}
}
