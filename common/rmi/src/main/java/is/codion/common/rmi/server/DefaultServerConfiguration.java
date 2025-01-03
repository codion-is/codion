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
package is.codion.common.rmi.server;

import is.codion.common.version.Version;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Supplier;

import static is.codion.common.Text.nullOrEmpty;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link AbstractServer}.
 */
final class DefaultServerConfiguration implements ServerConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultServerConfiguration.class);

	private final int port;
	private final int registryPort;
	private final Collection<String> auxiliaryServerFactoryClassNames;
	private final int adminPort;
	private final boolean sslEnabled;
	private final Supplier<String> serverNameSupplier;
	private final @Nullable RMIClientSocketFactory rmiClientSocketFactory;
	private final @Nullable RMIServerSocketFactory rmiServerSocketFactory;
	private final @Nullable String objectInputFilterFactoryClassName;
	private final int connectionMaintenanceInterval;
	private final int connectionLimit;

	DefaultServerConfiguration(DefaultServerConfiguration.DefaultBuilder builder) {
		this.port = builder.serverPort;
		this.registryPort = builder.registryPort;
		this.auxiliaryServerFactoryClassNames = unmodifiableCollection(builder.auxiliaryServerFactoryClassNames);
		this.adminPort = builder.serverAdminPort;
		this.sslEnabled = builder.sslEnabled;
		this.serverNameSupplier = builder.serverName;
		this.rmiClientSocketFactory = builder.rmiClientSocketFactory;
		this.rmiServerSocketFactory = builder.rmiServerSocketFactory;
		this.objectInputFilterFactoryClassName = builder.objectInputFilterFactoryClassName;
		this.connectionMaintenanceInterval = builder.connectionMaintenanceInterval;
		this.connectionLimit = builder.connectionLimit;
	}

	@Override
	public String serverName() {
		String serverName = serverNameSupplier.get();
		if (serverName == null || serverName.isEmpty()) {
			throw new IllegalArgumentException("serverName must not be null or empty");
		}

		return serverName;
	}

	@Override
	public int port() {
		return port;
	}

	@Override
	public int registryPort() {
		return registryPort;
	}

	@Override
	public int adminPort() {
		return adminPort;
	}

	@Override
	public Collection<String> auxiliaryServerFactoryClassNames() {
		return auxiliaryServerFactoryClassNames;
	}

	@Override
	public boolean sslEnabled() {
		return sslEnabled;
	}

	@Override
	public Optional<RMIClientSocketFactory> rmiClientSocketFactory() {
		return Optional.ofNullable(rmiClientSocketFactory);
	}

	@Override
	public Optional<RMIServerSocketFactory> rmiServerSocketFactory() {
		return Optional.ofNullable(rmiServerSocketFactory);
	}

	@Override
	public Optional<String> objectInputFilterFactoryClassName() {
		return Optional.ofNullable(objectInputFilterFactoryClassName);
	}

	@Override
	public int connectionMaintenanceInterval() {
		return connectionMaintenanceInterval;
	}

	@Override
	public int connectionLimit() {
		return connectionLimit;
	}

	static final class DefaultBuilder implements Builder<DefaultBuilder> {

		static {
			resolveClasspathKeyStore();
		}

		private final int serverPort;
		private final int registryPort;
		private final Collection<String> auxiliaryServerFactoryClassNames = new HashSet<>();
		private int serverAdminPort;
		private boolean sslEnabled = true;
		private Supplier<String> serverName = new DefaultServerName();
		private @Nullable RMIClientSocketFactory rmiClientSocketFactory = new SslRMIClientSocketFactory();
		private @Nullable RMIServerSocketFactory rmiServerSocketFactory = new SslRMIServerSocketFactory();
		private @Nullable String objectInputFilterFactoryClassName = OBJECT_INPUT_FILTER_FACTORY_CLASS_NAME.get();
		private Integer connectionMaintenanceInterval = DEFAULT_CONNECTION_MAINTENANCE_INTERVAL;
		private int connectionLimit = -1;

		DefaultBuilder(int serverPort, int registryPort) {
			this.serverPort = serverPort;
			this.registryPort = registryPort;
		}

		@Override
		public DefaultBuilder adminPort(int adminPort) {
			this.serverAdminPort = adminPort;
			return this;
		}

		@Override
		public DefaultBuilder serverName(Supplier<String> serverName) {
			this.serverName = requireNonNull(serverName);
			return this;
		}

		@Override
		public DefaultBuilder serverName(String serverName) {
			if (serverName == null || serverName.isEmpty()) {
				throw new IllegalArgumentException("serverName must not be null or empty");
			}

			return serverName(() -> serverName);
		}

		@Override
		public DefaultBuilder auxiliaryServerFactoryClassNames(Collection<String> auxiliaryServerFactoryClassNames) {
			this.auxiliaryServerFactoryClassNames.addAll(requireNonNull(auxiliaryServerFactoryClassNames));
			return this;
		}

		@Override
		public DefaultBuilder sslEnabled(boolean sslEnabled) {
			this.sslEnabled = sslEnabled;
			if (sslEnabled) {
				rmiClientSocketFactory = new SslRMIClientSocketFactory();
				rmiServerSocketFactory = new SslRMIServerSocketFactory();
			}
			else {
				rmiClientSocketFactory = null;
				rmiServerSocketFactory = null;
			}
			return this;
		}

		@Override
		public DefaultBuilder objectInputFilterFactoryClassName(@Nullable String objectInputFilterFactoryClassName) {
			this.objectInputFilterFactoryClassName = objectInputFilterFactoryClassName;
			return this;
		}

		@Override
		public DefaultBuilder connectionMaintenanceInterval(int connectionMaintenanceInterval) {
			this.connectionMaintenanceInterval = connectionMaintenanceInterval;
			return this;
		}

		@Override
		public DefaultBuilder connectionLimit(int connectionLimit) {
			this.connectionLimit = connectionLimit;
			return this;
		}

		@Override
		public ServerConfiguration build() {
			return new DefaultServerConfiguration(this);
		}

		private static synchronized void resolveClasspathKeyStore() {
			String keystore = CLASSPATH_KEYSTORE.get();
			if (nullOrEmpty(keystore)) {
				LOG.warn("No classpath key store specified via {}", CLASSPATH_KEYSTORE.propertyName());
				return;
			}
			if (KEYSTORE.isNotNull()) {
				throw new IllegalStateException("Classpath keystore (" + keystore + ") can not be specified when "
								+ JAVAX_NET_KEYSTORE + " is already set to " + KEYSTORE.get());
			}
			try (InputStream inputStream = DefaultServerConfiguration.class.getClassLoader().getResourceAsStream(keystore)) {
				if (inputStream == null) {
					LOG.warn("Specified key store not found on classpath: {}", keystore);
					return;
				}
				File file = File.createTempFile("serverKeyStore", "tmp");
				Files.write(file.toPath(), readBytes(inputStream));
				file.deleteOnExit();

				KEYSTORE.set(file.getPath());
				LOG.info("Classpath key store {} written to file {} and set as {}",
								CLASSPATH_KEYSTORE.propertyName(), file, JAVAX_NET_KEYSTORE);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private static byte[] readBytes(InputStream stream) throws IOException {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int line;
			while ((line = stream.read(buffer)) != -1) {
				os.write(buffer, 0, line);
			}
			os.flush();

			return os.toByteArray();
		}
	}

	private static final class DefaultServerName implements Supplier<String> {

		@Override
		public String get() {
			String serverNamePrefix = SERVER_NAME_PREFIX.getOrThrow();
			if (serverNamePrefix.isEmpty()) {
				throw new IllegalArgumentException("serverNamePrefix must not be empty");
			}

			return serverNamePrefix + " " + Version.versionString();
		}
	}
}
