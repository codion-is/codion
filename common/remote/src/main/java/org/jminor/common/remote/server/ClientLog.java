/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import org.jminor.common.MethodLogger;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Encapsulates a simple collection of server access log entries and basic connection access info.
 */
public interface ClientLog extends Serializable {

  /**
   * @return the log entry list
   */
  List<MethodLogger.Entry> getEntries();

  /**
   * @return the UUID identifying this logs client
   */
  UUID getClientId();

  /**
   * @return the log creation date
   */
  LocalDateTime getConnectionCreationDate();
}
