/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.logging.LoggerProxy;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.DefaultServerAdmin;
import is.codion.framework.domain.entity.EntityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the EntityServerAdmin interface, providing admin access to a EntityServer instance.
 */
final class DefaultEntityServerAdmin extends DefaultServerAdmin implements EntityServerAdmin {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityServerAdmin.class);

  private static final long serialVersionUID = 1;

  /**
   * The server being administrated
   */
  private final EntityServer server;

  private final LoggerProxy loggerProxy = LoggerProxy.loggerProxy();

  /**
   * Instantiates a new DefaultEntityServerAdmin
   * @param server the server to administer
   * @param configuration the port on which to make the server admin available
   * @throws RemoteException in case of an exception
   * @throws NullPointerException in case {@code configuration} or {@code server} are not specified
   */
  DefaultEntityServerAdmin(final EntityServer server, final EntityServerConfiguration configuration) throws RemoteException {
    super(server, configuration);
    this.server = server;
  }

  @Override
  public String getDatabaseUrl() {
    return server.getDatabase().getUrl();
  }

  @Override
  public Object getLogLevel() {
    return loggerProxy.getLogLevel();
  }

  @Override
  public void setLogLevel(final Object level) {
    LOG.info("setLogLevel({})", level);
    loggerProxy.setLogLevel(level);
  }

  @Override
  public int getMaintenanceInterval() {
    return server.getMaintenanceInterval();
  }

  @Override
  public void setMaintenanceInterval(final int interval) {
    LOG.info("setMaintenanceInterval({})", interval);
    server.setMaintenanceInterval(interval);
  }

  @Override
  public void disconnectTimedOutClients() throws RemoteException {
    LOG.info("disconnectTimedOutClients()");
    server.disconnectClients(true);
  }

  @Override
  public void disconnectAllClients() throws RemoteException {
    LOG.info("disconnectAllClients()");
    server.disconnectClients(false);
  }

  @Override
  public void resetConnectionPoolStatistics(final String username) {
    LOG.info("resetConnectionPoolStatistics({})", username);
    server.getDatabase().getConnectionPool(username).resetStatistics();
  }

  @Override
  public boolean isCollectPoolSnapshotStatistics(final String username) {
    return server.getDatabase().getConnectionPool(username).isCollectSnapshotStatistics();
  }

  @Override
  public void setCollectPoolSnapshotStatistics(final String username, final boolean snapshotStatistics) {
    LOG.info("setCollectPoolSnapshotStatistics({}, {})", username, snapshotStatistics);
    server.getDatabase().getConnectionPool(username).setCollectSnapshotStatistics(snapshotStatistics);
  }

  @Override
  public boolean isCollectPoolCheckOutTimes(final String username) throws RemoteException {
    return server.getDatabase().getConnectionPool(username).isCollectCheckOutTimes();
  }

  @Override
  public void setCollectPoolCheckOutTimes(final String username, final boolean collectCheckOutTimes) throws RemoteException {
    LOG.info("setCollectPoolCheckOutTimes({}, {})", username, collectCheckOutTimes);
    server.getDatabase().getConnectionPool(username).setCollectCheckOutTimes(collectCheckOutTimes);
  }

  @Override
  public int getRequestsPerSecond() {
    return AbstractRemoteEntityConnection.getRequestsPerSecond();
  }

  @Override
  public ConnectionPoolStatistics getConnectionPoolStatistics(final String username, final long since) {
    return server.getDatabase().getConnectionPool(username).getStatistics(since);
  }

  @Override
  public Database.Statistics getDatabaseStatistics() {
    return server.getDatabaseStatistics();
  }

  @Override
  public Collection<String> getConnectionPoolUsernames() {
    return server.getDatabase().getConnectionPoolUsernames();
  }

  @Override
  public int getConnectionPoolCleanupInterval(final String username) {
    return server.getDatabase().getConnectionPool(username).getCleanupInterval();
  }

  @Override
  public void setConnectionPoolCleanupInterval(final String username, final int poolCleanupInterval) {
    LOG.info("setConnectionPoolCleanupInterval({}, {})", username, poolCleanupInterval);
    server.getDatabase().getConnectionPool(username).setCleanupInterval(poolCleanupInterval);
  }

  @Override
  public int getMaximumConnectionPoolSize(final String username) {
    return server.getDatabase().getConnectionPool(username).getMaximumPoolSize();
  }

  @Override
  public void setMaximumConnectionPoolSize(final String username, final int value) {
    LOG.info("setMaximumConnectionPoolSize({}, {})", username, value);
    server.getDatabase().getConnectionPool(username).setMaximumPoolSize(value);
  }

  @Override
  public int getMinimumConnectionPoolSize(final String username) {
    return server.getDatabase().getConnectionPool(username).getMinimumPoolSize();
  }

  @Override
  public void setMinimumConnectionPoolSize(final String username, final int value) {
    LOG.info("setMinimumConnectionPoolSize({}, {})", username, value);
    server.getDatabase().getConnectionPool(username).setMinimumPoolSize(value);
  }

  @Override
  public int getPooledConnectionTimeout(final String username) {
    return server.getDatabase().getConnectionPool(username).getConnectionTimeout();
  }

  @Override
  public void setPooledConnectionTimeout(final String username, final int timeout) {
    LOG.info("setPooledConnectionTimeout({}, {})", username, timeout);
    server.getDatabase().getConnectionPool(username).setConnectionTimeout(timeout);
  }

  @Override
  public int getMaximumPoolCheckOutTime(final String username) {
    return server.getDatabase().getConnectionPool(username).getMaximumCheckOutTime();
  }

  @Override
  public void setMaximumPoolCheckOutTime(final String username, final int value) {
    LOG.info("setMaximumPoolCheckOutTime({}, {})", username, value);
    server.getDatabase().getConnectionPool(username).setMaximumCheckOutTime(value);
  }

  @Override
  public ClientLog getClientLog(final UUID clientId) {
    return server.getClientLog(clientId);
  }

  @Override
  public boolean isLoggingEnabled(final UUID clientId) {
    return server.isLoggingEnabled(clientId);
  }

  @Override
  public void setLoggingEnabled(final UUID clientId, final boolean loggingEnabled) {
    LOG.info("setLoggingEnabled({}, {})", clientId, loggingEnabled);
    server.setLoggingEnabled(clientId, loggingEnabled);
  }

  @Override
  public int getConnectionTimeout() {
    return server.getConnectionTimeout();
  }

  @Override
  public void setConnectionTimeout(final int timeout) {
    LOG.info("setConnectionTimeout({})", timeout);
    server.setConnectionTimeout(timeout);
  }

  @Override
  public Map<String, String> getEntityDefinitions() {
    final Map<EntityType, String> entityDefinitions = server.getEntityDefinitions();
    final Map<String, String> definitions = new HashMap<>();
    entityDefinitions.forEach((key, value) -> definitions.put(key.getDomainName() + ":" + key.getName(), value));

    return definitions;
  }
}
