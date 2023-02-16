/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.NullOrEmpty;

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
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Configuration values for a {@link AbstractServer}.
 */
final class DefaultServerConfiguration implements ServerConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ServerConfiguration.class);

  private final int serverPort;
  private final int registryPort;
  private final Collection<String> auxiliaryServerFactoryClassNames = new HashSet<>();
  private int serverAdminPort;
  private boolean sslEnabled;
  private String serverName;
  private Supplier<String> serverNameProvider = () -> serverName;
  private RMIClientSocketFactory rmiClientSocketFactory;
  private RMIServerSocketFactory rmiServerSocketFactory;
  private String serializationFilterWhitelist;
  private boolean serializationFilterDryRun;
  private int connectionMaintenanceIntervalMs;

  DefaultServerConfiguration(int serverPort, int registryPort) {
    this.serverPort = serverPort;
    this.registryPort = registryPort;
  }

  @Override
  public String serverName() {
    if (serverName == null) {
      serverName = serverNameProvider.get();
    }

    return serverName;
  }

  @Override
  public int serverPort() {
    return serverPort;
  }

  @Override
  public int registryPort() {
    return registryPort;
  }

  @Override
  public int serverAdminPort() {
    return serverAdminPort;
  }

  @Override
  public Collection<String> auxiliaryServerFactoryClassNames() {
    return auxiliaryServerFactoryClassNames;
  }

  @Override
  public boolean isSslEnabled() {
    return sslEnabled;
  }

  @Override
  public RMIClientSocketFactory rmiClientSocketFactory() {
    return rmiClientSocketFactory;
  }

  @Override
  public RMIServerSocketFactory rmiServerSocketFactory() {
    return rmiServerSocketFactory;
  }

  @Override
  public String serializationFilterWhitelist() {
    return serializationFilterWhitelist;
  }

  @Override
  public boolean isSerializationFilterDryRun() {
    return serializationFilterDryRun;
  }

  @Override
  public int connectionMaintenanceInterval() {
    return connectionMaintenanceIntervalMs;
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
    private String serverName;
    private Supplier<String> serverNameProvider;
    private RMIClientSocketFactory rmiClientSocketFactory = new SslRMIClientSocketFactory();
    private RMIServerSocketFactory rmiServerSocketFactory = new SslRMIServerSocketFactory();
    private String serializationFilterWhitelist;
    private Boolean serializationFilterDryRun = false;
    private Integer connectionMaintenanceIntervalMs = DEFAULT_CONNECTION_MAINTENANCE_INTERVAL;

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
    public DefaultBuilder serverNameProvider(Supplier<String> serverNameProvider) {
      this.serverNameProvider = requireNonNull(serverNameProvider);
      return this;
    }

    @Override
    public DefaultBuilder serverName(String serverName) {
      this.serverName = requireNonNull(serverName);
      return this;
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
        rmiClientSocketFactory(new SslRMIClientSocketFactory());
        rmiServerSocketFactory(new SslRMIServerSocketFactory());
      }
      else {
        rmiClientSocketFactory(null);
        rmiServerSocketFactory(null);
      }
      return this;
    }

    @Override
    public DefaultBuilder rmiClientSocketFactory(RMIClientSocketFactory rmiClientSocketFactory) {
      this.rmiClientSocketFactory = rmiClientSocketFactory;
      return this;
    }

    @Override
    public DefaultBuilder rmiServerSocketFactory(RMIServerSocketFactory rmiServerSocketFactory) {
      this.rmiServerSocketFactory = rmiServerSocketFactory;
      return this;
    }

    @Override
    public DefaultBuilder serializationFilterWhitelist(String serializationFilterWhitelist) {
      this.serializationFilterWhitelist = serializationFilterWhitelist;
      return this;
    }

    @Override
    public DefaultBuilder serializationFilterDryRun(boolean serializationFilterDryRun) {
      this.serializationFilterDryRun = serializationFilterDryRun;
      return this;
    }

    @Override
    public DefaultBuilder connectionMaintenanceIntervalMs(int connectionMaintenanceIntervalMs) {
      this.connectionMaintenanceIntervalMs = connectionMaintenanceIntervalMs;
      return this;
    }

    @Override
    public ServerConfiguration build() {
      DefaultServerConfiguration configuration = new DefaultServerConfiguration(serverPort, registryPort);
      configuration.auxiliaryServerFactoryClassNames.addAll(auxiliaryServerFactoryClassNames);
      configuration.serverAdminPort = serverAdminPort;
      configuration.sslEnabled = sslEnabled;
      configuration.serverName = serverName;
      configuration.serverNameProvider = serverNameProvider;
      configuration.rmiClientSocketFactory = rmiClientSocketFactory;
      configuration.rmiServerSocketFactory = rmiServerSocketFactory;
      configuration.serializationFilterWhitelist = serializationFilterWhitelist;
      configuration.serializationFilterDryRun = serializationFilterDryRun;
      configuration.connectionMaintenanceIntervalMs = connectionMaintenanceIntervalMs;

      return configuration;
    }

    private static synchronized void resolveClasspathKeyStore() {
      String keystore = CLASSPATH_KEYSTORE.get();
      if (nullOrEmpty(keystore)) {
        LOG.debug("No classpath key store specified via {}", CLASSPATH_KEYSTORE.propertyName());
        return;
      }
      if (KEYSTORE.isNotNull()) {
        throw new IllegalStateException("Classpath keystore (" + keystore + ") can not be specified when "
                + JAVAX_NET_KEYSTORE + " is already set to " + KEYSTORE.get());
      }
      try (InputStream inputStream = NullOrEmpty.class.getClassLoader().getResourceAsStream(keystore)) {
        if (inputStream == null) {
          LOG.debug("Specified key store not found on classpath: {}", keystore);
          return;
        }
        File file = File.createTempFile("serverKeyStore", "tmp");
        Files.write(file.toPath(), readBytes(inputStream));
        file.deleteOnExit();

        KEYSTORE.set(file.getPath());
        LOG.debug("Classpath key store {} written to file {} and set as {}",
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
}
