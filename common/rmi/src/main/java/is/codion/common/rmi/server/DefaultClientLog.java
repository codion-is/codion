/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.MethodLogger;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

final class DefaultClientLog implements ClientLog, Serializable {

  private static final long serialVersionUID = 1;

  private final UUID clientId;
  private final LocalDateTime connectionCreationDate;
  private final List<MethodLogger.Entry> entries;

  DefaultClientLog(final UUID clientId, final LocalDateTime connectionCreationDate,
                   final List<MethodLogger.Entry> entries) {
    this.clientId = clientId;
    this.connectionCreationDate = connectionCreationDate;
    this.entries = entries;
  }

  @Override
  public List<MethodLogger.Entry> getEntries() {
    return entries;
  }

  @Override
  public UUID getClientId() {
    return clientId;
  }

  @Override
  public LocalDateTime getConnectionCreationDate() {
    return connectionCreationDate;
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || !((obj == null) || (obj.getClass() != this.getClass()))
            && clientId.equals(((ClientLog) obj).getClientId());
  }

  @Override
  public int hashCode() {
    return clientId.hashCode();
  }
}
