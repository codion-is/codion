/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.KeyStoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import static is.codion.common.Util.nullOrEmpty;

/**
 * Utility methods for remote clients
 */
public final class Clients {

  private static final Logger LOG = LoggerFactory.getLogger(Clients.class);

  /**
   * The system property key for specifying a ssl truststore
   */
  public static final String JAVAX_NET_TRUSTSTORE = "javax.net.ssl.trustStore";

  /**
   * The system property key for specifying a ssl truststore password
   */
  public static final String JAVAX_NET_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

  /**
   * The rmi ssl truststore to use<br>
   * Value type: String
   * Default value: null
   */
  public static final PropertyValue<String> TRUSTSTORE =
          Configuration.stringValue("codion.client.trustStore", null);

  /**
   * The rmi ssl truststore password to use<br>
   * Value type: String
   * Default value: null
   */
  public static final PropertyValue<String> TRUSTSTORE_PASSWORD =
          Configuration.stringValue("codion.client.trustStorePassword", null);

  /**
   * The host on which to locate the server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final PropertyValue<String> SERVER_HOST_NAME =
          Configuration.stringValue("codion.server.hostname", "localhost");

  private Clients() {}

  /**
   * Reads the trust store and password specified by the 'codion.client.trustStore' and 'codion.client.trustStorePassword'
   * system properties and if one exists, either in the filesystem or on the classpath, combines it with the default
   * system truststore, writes the combined truststore to a temporary file and sets 'javax.net.ssl.trustStore'
   * so that it points to that file and 'javax.net.ssl.trustStorePassword' to the given password.
   * If no truststore is specified or the file is not found, this method has no effect.
   * @throws IllegalArgumentException in case a truststore is specified but no password
   * @see Clients#TRUSTSTORE
   * @see Clients#TRUSTSTORE_PASSWORD
   */
  public static void resolveTrustStore() {
    final String trustStorePath = TRUSTSTORE.get();
    if (nullOrEmpty(trustStorePath)) {
      LOG.debug("No client truststore specified via {}", TRUSTSTORE.getPropertyName());
      return;
    }
    final String password = TRUSTSTORE_PASSWORD.getOrThrow();
    final SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder()
            .withDefaultTrustMaterial();
    final File trustStore = new File(trustStorePath);
    if (trustStore.exists()) {
      sslFactoryBuilder.withTrustMaterial(trustStore.toPath(), password.toCharArray());
    }
    else {
      sslFactoryBuilder.withTrustMaterial(trustStorePath, password.toCharArray());
    }

    final X509TrustManager trustManager = sslFactoryBuilder.build()
            .getTrustManager()
            .orElseThrow(() -> new RuntimeException("No TrustManager available after combining truststores"));
    final KeyStore store = KeyStoreUtils.createTrustStore(trustManager);
    try {
      final File file = File.createTempFile("combinedTrustStore", "tmp");
      file.deleteOnExit();
      try (final OutputStream outputStream = new FileOutputStream(file)) {
        store.store(outputStream, password.toCharArray());
      }
      LOG.debug("Classpath trust store written to file: {} -> {}", JAVAX_NET_TRUSTSTORE, file);

      System.setProperty(JAVAX_NET_TRUSTSTORE, file.getPath());
      System.setProperty(JAVAX_NET_TRUSTSTORE_PASSWORD, password);
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
