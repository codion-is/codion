/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.logging.MethodLogger;

import java.util.List;
import java.util.UUID;

/**
 * Encapsulates a collection of server access log entries and basic connection access info.
 */
public interface ClientLog {

  /**
   * @return the log entry list
   */
  List<MethodLogger.Entry> entries();

  /**
   * @return the UUID identifying this logs client
   */
  UUID clientId();

  /**
   * Instantiates a new ClientLog instance.
   * @param clientId the id of the client this log represents
   * @param entries the log entries
   * @return a new ClientLog instance
   */
  static ClientLog clientLog(UUID clientId, List<MethodLogger.Entry> entries) {
    return new DefaultClientLog(clientId, entries);
  }
}
