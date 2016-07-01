/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.MethodLogger;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * A class encapsulating a simple collection of server access log entries and basic connection access info.
 */
public final class ClientLog implements Serializable {

  private static final long serialVersionUID = 1;

  private final UUID clientID;
  private final long connectionCreationDate;
  private final List<MethodLogger.Entry> entries;

  /**
   * Instantiates a new ClientLog instance.
   * @param clientID the ID of the client this log represents
   * @param connectionCreationDate the date this client connection was created
   * @param entries the log entries
   */
  public ClientLog(final UUID clientID, final long connectionCreationDate, final List<MethodLogger.Entry> entries) {
    this.clientID = clientID;
    this.connectionCreationDate = connectionCreationDate;
    this.entries = entries;
  }

  /**
   * @return the log entry list
   */
  public List<MethodLogger.Entry> getEntries() {
    return entries;
  }

  /**
   * @return the UUID identifying this logs client
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

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || !((obj == null) || (obj.getClass() != this.getClass()))
            && clientID.equals(((ClientLog) obj).clientID);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return clientID.hashCode();
  }
}
