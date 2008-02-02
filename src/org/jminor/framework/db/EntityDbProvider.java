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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A class responsible for managing local db connections
 */
public class EntityDbProvider implements IEntityDbProvider {

  private static final Logger log = Util.getLogger(EntityDbProvider.class);

  /**
   * Fired when a successful connection has been made
   */
  public final Event evtConnected = new Event("EntityDbProvider.evtConnected");

  protected final User user;
  protected IEntityDb entityDb;
  protected IEntityDb entityDbProxy;

  public EntityDbProvider(final User user) {
    this.user = user;
    final String sid = System.getProperty(Database.DATABASE_SID_PROPERTY);
    if (sid == null || sid.length() == 0)
      throw new RuntimeException("Required property value not found: " + Database.DATABASE_SID_PROPERTY);
    user.setProperty(Database.DATABASE_SID_PROPERTY, sid);
  }

  public synchronized final IEntityDb getEntityDb() throws UserException {
    initializeEntityDb();
    if (entityDbProxy == null)
      entityDbProxy = initializeDbProxy();

    return entityDbProxy;
  }

  /** {@inheritDoc} */
  public Event getConnectEvent() {
    return evtConnected;
  }

  protected String getClientID() {
    return user.toString();
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

  private IEntityDb initializeDbProxy() {
    return (IEntityDb) Proxy.newProxyInstance(EntityDbProxy.class.getClassLoader(),
            new Class[] {IEntityDb.class}, new EntityDbProxy());
  }

  private class EntityDbProxy implements InvocationHandler {
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
//      final String methodName = method.getName();
//      final Date time = new Date(System.currentTimeMillis());
      try {
        return method.invoke(entityDb, args);
      }
      catch (InvocationTargetException ie) {
        throw ie.getTargetException();
      }
//      finally {
//        final long delta = System.currentTimeMillis()-time.getTime();
//        if (delta > 200)
//          System.out.println(time + " " + delta + " ms: " + methodName + "::" + getClientID());
//      }
    }
  }
}
