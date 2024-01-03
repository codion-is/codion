/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.logging.MethodLogger;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultClientLog implements ClientLog, Serializable {

  private static final long serialVersionUID = 1;

  private final UUID clientId;
  private final List<MethodLogger.Entry> entries;

  DefaultClientLog(UUID clientId, List<MethodLogger.Entry> entries) {
    this.clientId = requireNonNull(clientId);
    this.entries = requireNonNull(entries);
  }

  @Override
  public List<MethodLogger.Entry> entries() {
    return entries;
  }

  @Override
  public UUID clientId() {
    return clientId;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultClientLog)) {
      return false;
    }
    DefaultClientLog that = (DefaultClientLog) object;

    return clientId.equals(that.clientId);
  }

  @Override
  public int hashCode() {
    return clientId.hashCode();
  }
}
