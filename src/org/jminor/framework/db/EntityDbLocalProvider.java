/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.model.EntityRepository;

import org.apache.log4j.Logger;

/**
 * A class responsible for managing local db connections
 */
public class EntityDbLocalProvider implements IEntityDbProvider {

  private static final Logger log = Util.getLogger(EntityDbLocalProvider.class);

  /**
   * Fired when a successful connection has been made
   */
  public final Event evtConnected = new Event("EntityDbLocalProvider.evtConnected");

  protected final User user;
  protected IEntityDb entityDb;

  public EntityDbLocalProvider(final User user) {
    this.user = user;
    final String sid = System.getProperty(Database.DATABASE_SID_PROPERTY);
    if (sid == null || sid.length() == 0)
      throw new RuntimeException("Required property value not found: " + Database.DATABASE_SID_PROPERTY);
    user.setProperty(Database.DATABASE_SID_PROPERTY, sid);
  }

  public synchronized final IEntityDb getEntityDb() throws UserException {
    initializeEntityDb();

    return entityDb;
  }

  /** {@inheritDoc} */
  public Event getConnectEvent() {
    return evtConnected;
  }

  protected void connectServer() throws Exception {
    log.debug("Initializing connection for " + user);
    entityDb = new EntityDbConnection(user, EntityRepository.get(), FrameworkSettings.get());
    evtConnected.fire();
  }

  protected void initializeEntityDb() throws UserException {
    try {
      validateDbConnection();
    }
    catch (Exception e) {
      log.error(this, e);
      throw new UserException(e);
    }
  }

  private void validateDbConnection() throws Exception {
    if (entityDb == null)
      connectServer();

    if (!entityDb.isConnectionValid()) {
      //db unreachable
      //try to reconnect once in case db has become reachable
      entityDb = null;
      connectServer();
    }
  }
}
