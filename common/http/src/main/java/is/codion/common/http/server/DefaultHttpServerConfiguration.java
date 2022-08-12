/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.http.server;

import is.codion.common.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static is.codion.common.Util.nullOrEmpty;

final class DefaultHttpServerConfiguration implements HttpServerConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(HttpServerConfiguration.class);

  private final int serverPort;
  private final boolean secure;
  private final String documentRoot;
  private final String keystorePath;
  private final String keystorePassword;

  DefaultHttpServerConfiguration(DefaultBuilder builder) {
    this.serverPort = builder.serverPort;
    this.secure = builder.secure;
    this.documentRoot = builder.documentRoot;
    this.keystorePath = builder.keystorePath;
    this.keystorePassword = builder.keystorePassword;
  }

  @Override
  public int serverPort() {
    return serverPort;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public String documentRoot() {
    return documentRoot;
  }

  @Override
  public String keystorePath() {
    return keystorePath;
  }

  @Override
  public String keystorePassword() {
    return keystorePassword;
  }

  static final class DefaultBuilder implements Builder {

    static {
      resolveClasspathKeyStore();
    }

    private final int serverPort;

    private boolean secure = true;
    private String documentRoot;
    private String keystorePath;
    private String keystorePassword;

    DefaultBuilder(int serverPort) {
      this.serverPort = serverPort;
    }

    @Override
    public Builder secure(boolean secure) {
      this.secure = secure;
      return this;
    }

    @Override
    public Builder documentRoot(String documentRoot) {
      this.documentRoot = documentRoot;
      return this;
    }

    @Override
    public Builder keystorePath(String keystorePath) {
      this.keystorePath = keystorePath;
      return this;
    }

    @Override
    public Builder keystorePassword(String keystorePassword) {
      this.keystorePassword = keystorePassword;
      return this;
    }

    @Override
    public HttpServerConfiguration build() {
      return new DefaultHttpServerConfiguration(this);
    }

    private static synchronized void resolveClasspathKeyStore() {
      String keystore = HTTP_SERVER_CLASSPATH_KEYSTORE.get();
      if (nullOrEmpty(keystore)) {
        LOG.debug("No classpath key store specified via {}", HTTP_SERVER_CLASSPATH_KEYSTORE.propertyName());
        return;
      }
      if (HTTP_SERVER_KEYSTORE_PATH.isNotNull()) {
        throw new IllegalStateException("Classpath keystore (" + keystore + ") can not be specified when "
                + HTTP_SERVER_KEYSTORE_PATH.propertyName() + " is already set to " + HTTP_SERVER_KEYSTORE_PATH.get());
      }
      try (InputStream inputStream = Util.class.getClassLoader().getResourceAsStream(keystore)) {
        if (inputStream == null) {
          LOG.debug("Specified key store not found on classpath: {}", keystore);
          return;
        }
        File file = File.createTempFile("serverKeyStore", "tmp");
        Files.write(file.toPath(), readBytes(inputStream));
        file.deleteOnExit();

        HTTP_SERVER_KEYSTORE_PATH.set(file.getPath());
        LOG.debug("Classpath key store {} written to file {} and set as {}",
                HTTP_SERVER_CLASSPATH_KEYSTORE.propertyName(), file, HTTP_SERVER_KEYSTORE_PATH.propertyName());
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
