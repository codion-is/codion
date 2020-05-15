/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.Util;
import is.codion.common.rmi.server.ServerConfiguration;

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

  private static final int INPUT_BUFFER_SIZE = 8192;

  private Clients() {}

  /**
   * Reads the trust store specified by "javax.net.ssl.trustStore" from the classpath, copies it
   * to a temporary file and sets the trust store property so that it points to that temporary file.
   * If the trust store file specified is not found on the classpath this method has no effect.
   * @param temporaryFileNamePrefix the prefix to use for the temporary filename
   * @see ServerConfiguration#TRUSTSTORE
   */
  public static void resolveTrustStoreFromClasspath(final String temporaryFileNamePrefix) {
    final String value = ServerConfiguration.TRUSTSTORE.get();
    if (nullOrEmpty(value)) {
      LOG.debug("No trust store specified via {}", ServerConfiguration.JAVAX_NET_TRUSTSTORE);
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
      LOG.debug("Classpath trust store written to file: {} -> {}", ServerConfiguration.JAVAX_NET_TRUSTSTORE, file);

      ServerConfiguration.TRUSTSTORE.set(file.getPath());
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
