/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.client;

import org.jminor.common.Util;
import org.jminor.common.remote.server.ServerConfiguration;
import org.jminor.common.user.User;
import org.jminor.common.version.Version;
import org.jminor.common.version.Versions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import static org.jminor.common.Util.nullOrEmpty;

/**
 * Utility methods for remote clients
 */
public final class Clients {

  private static final Logger LOG = LoggerFactory.getLogger(Clients.class);

  private static final int INPUT_BUFFER_SIZE = 8192;

  private Clients() {}

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientId the client id
   * @param clientTypeId the client type id
   * @return a ConnectionRequest
   */
  public static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId) {
    return connectionRequest(user, clientId, clientTypeId, null);
  }

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientId the client id
   * @param clientTypeId the client type id
   * @param parameters misc. parameters, values must implement {@link java.io.Serializable}
   * @return a ConnectionRequest
   */
  public static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId,
                                                    final Map<String, Object> parameters) {
    return connectionRequest(user, clientId, clientTypeId, null, parameters);
  }

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientId the client id
   * @param clientTypeId the client type id
   * @param clientVersion the client application version
   * @param parameters misc. parameters, values must implement {@link java.io.Serializable}
   * @return a ConnectionRequest
   */
  public static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId,
                                                    final Version clientVersion, final Map<String, Object> parameters) {
    return new DefaultConnectionRequest(user, clientId, clientTypeId, clientVersion, Versions.getVersion(), parameters);
  }

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
