/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.tools.MethodLogger;

import java.io.Serializable;
import java.util.UUID;

/**
 * A class encapsulating a simple collection of server access log entries and basic connection access info.
 */
public final class ServerLog implements Serializable {

  private static final long serialVersionUID = 1;

  private final long logCreationDate = System.currentTimeMillis();
  private final UUID clientID;
  private final long connectionCreationDate;
  private final MethodLogger logger;

  /**
   * Instantiates a new ServerLog instance.
   * @param clientID the ID of the client this log represents
   * @param connectionCreationDate the date this client connection was created
   */
  public ServerLog(final UUID clientID, final long connectionCreationDate, final MethodLogger logger) {
    this.clientID = clientID;
    this.connectionCreationDate = connectionCreationDate;
    this.logger = logger;
  }

  /**
   * @return the log entry list
   */
  public MethodLogger getLogger() {
    return logger;
  }

  /**
   * @return the date this log was created
   */
  public long getLogCreationDate() {
    return logCreationDate;
  }

  /**
   * @return the UUID identifying this log's client
   */
  public UUID getClientID() {
    return clientID;
  }

  /**
   * @return the log creation date
   */
  public long getConnectionCreationDate() {
    return connectionCreationDate;
  }
  /**
   * @return the duration of the last method call
   */
  public long getLastDelta() {
    return logger.getLastExitTime() - logger.getLastAccessTime();
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || !((obj == null) || (obj.getClass() != this.getClass()))
            && clientID.equals(((ServerLog) obj).clientID);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return clientID.hashCode();
  }
}
