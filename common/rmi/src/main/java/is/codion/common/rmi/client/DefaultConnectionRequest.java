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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import org.jspecify.annotations.Nullable;

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
	private final String clientType;
	private final Locale locale = Locale.getDefault();
	private final ZoneId timeZone = ZoneId.systemDefault();
	private final @Nullable Version version;
	private final Version frameworkVersion = Version.version();
	private final @Nullable Map<String, Object> parameters;

	private DefaultConnectionRequest(DefaultBuilder builder) {
		this.user = requireNonNull(builder.user, "user must be specified");
		this.clientId = builder.clientId == null ? UUID.randomUUID() : builder.clientId;
		this.clientType = requireNonNull(builder.clientType, "client type must be specified");
		this.version = builder.version;
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
	public String clientType() {
		return clientType;
	}

	@Override
	public Locale locale() {
		return locale;
	}

	@Override
	public ZoneId timeZone() {
		return timeZone;
	}

	@Override
	public Optional<Version> version() {
		return Optional.ofNullable(version);
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
						.clientType(clientType)
						.version(version);
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
		return user + " [" + clientType + "] - " + clientId;
	}

	static final class DefaultBuilder implements Builder {

		private @Nullable User user;
		private @Nullable UUID clientId;
		private @Nullable String clientType;
		private @Nullable Version version;
		private @Nullable Map<String, Object> parameters;

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
		public Builder clientType(String clientType) {
			this.clientType = requireNonNull(clientType);
			return this;
		}

		@Override
		public Builder version(@Nullable Version version) {
			this.version = version;
			return this;
		}

		@Override
		public Builder parameter(String key, Object value) {
			requireNonNull(key);
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
