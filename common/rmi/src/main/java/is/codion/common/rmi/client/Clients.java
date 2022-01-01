/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.Configuration;
import is.codion.common.Util;
import is.codion.common.value.PropertyValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
          Configuration.stringValue(JAVAX_NET_TRUSTSTORE, null);

  /**
   * The rmi ssl truststore password to use<br>
   * Value type: String
   * Default value: null
   */
  public static final PropertyValue<String> TRUSTSTORE_PASSWORD =
          Configuration.stringValue(JAVAX_NET_TRUSTSTORE_PASSWORD, null);

  /**
   * The host on which to locate the server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final PropertyValue<String> SERVER_HOST_NAME =
          Configuration.stringValue("codion.server.hostname", "localhost");

  private static final int INPUT_BUFFER_SIZE = 8192;

  private Clients() {}

  /**
   * Reads the trust store specified by "javax.net.ssl.trustStore" from the classpath, copies it
   * to a temporary file and sets the trust store property so that it points to that temporary file.
   * If the trust store file specified is not found on the classpath this method has no effect.
   * @param temporaryFileNamePrefix the prefix to use for the temporary filename
   * @see Clients#TRUSTSTORE
   */
  public static void resolveTrustStoreFromClasspath(final String temporaryFileNamePrefix) {
    final String value = TRUSTSTORE.get();
    if (nullOrEmpty(value)) {
      LOG.debug("No trust store specified via {}", JAVAX_NET_TRUSTSTORE);
      return;
    }
    try (final InputStream inputStream = Util.class.getClassLoader().getResourceAsStream(value)) {
      if (inputStream == null) {
        LOG.debug("Specified trust store not found on classpath: {}", value);
        return;
      }
      final File file = File.createTempFile(temporaryFileNamePrefix, "tmp");
      Files.write(file.toPath(), getBytes(inputStream));
      file.deleteOnExit();
      LOG.debug("Classpath trust store written to file: {} -> {}", JAVAX_NET_TRUSTSTORE, file);

      TRUSTSTORE.set(file.getPath());
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] getBytes(final InputStream stream) throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final byte[] buffer = new byte[INPUT_BUFFER_SIZE];
    int line;
    while ((line = stream.read(buffer)) != -1) {
      os.write(buffer, 0, line);
    }
    os.flush();

    return os.toByteArray();
  }
}
