/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.version.Version;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A ClientUserMonitor for monitoring connected clients and users connected to a server
 */
public final class ClientUserMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(ClientUserMonitor.class);

  private static final int THOUSAND = 1000;

  private static final int USERNAME_COLUMN = 0;
  private static final int CLIENT_TYPE_COLUMN = 1;
  private static final int CLIENT_VERSION_COLUMN = 2;
  private static final int FRAMEWORK_VERSION_COLUMN = 3;
  private static final int CLIENT_HOST_COLUMN = 4;
  private static final int LAST_SEEN_COLUMN = 5;
  private static final int CONNECTION_COUNT_COLUMN = 6;

  private final EntityServerAdmin server;
  private final Value<Integer> idleConnectionTimeoutValue;
  private final ClientMonitor clientMonitor;
  private final FilteredTableModel<UserInfo, Integer> userHistoryTableModel =
          FilteredTableModel.builder(ClientUserMonitor::createUserHistoryColumns, new UserHistoryColumnValueProvider())
                  .itemSupplier(new UserHistoryItemSupplier())
                  .mergeOnRefresh(true)
                  .build();

  private final TaskScheduler updateScheduler;
  private final Value<Integer> updateIntervalValue;

  /**
   * Instantiates a new {@link ClientUserMonitor}
   * @param server the server
   * @param updateRate the initial statistics update rate in seconds
   */
  public ClientUserMonitor(EntityServerAdmin server, int updateRate) {
    this.server = requireNonNull(server);
    this.clientMonitor = new ClientMonitor(server);
    this.idleConnectionTimeoutValue = Value.value(this::getIdleConnectionTimeout, this::setIdleConnectionTimeout, 0);
    this.updateScheduler = TaskScheduler.builder(this::refreshUserHistoryTableModel)
            .interval(updateRate, TimeUnit.SECONDS)
            .start();
    this.updateIntervalValue = Value.value(updateScheduler::getInterval, updateScheduler::setInterval, 0);
  }

  /**
   * Shuts down this monitor
   */
  public void shutdown() {
    updateScheduler.stop();
  }

  public ClientMonitor clientMonitor() {
    return clientMonitor;
  }

  /**
   * @return a TableModel for displaying the user connection history
   */
  public FilteredTableModel<?, Integer> userHistoryTableModel() {
    return userHistoryTableModel;
  }

  /**
   * Disconnects all users from the server
   * @throws RemoteException in case of an exception
   */
  public void disconnectAll() throws RemoteException {
    server.disconnectAllClients();
    clientMonitor.refresh();
  }

  /**
   * Disconnects all timed out users from the server
   * @throws RemoteException in case of an exception
   */
  public void disconnectTimedOut() throws RemoteException {
    server.disconnectTimedOutClients();
    clientMonitor.refresh();
  }

  /**
   * Sets the server's connection maintenance interval
   * @param interval the maintenance interval in seconds
   * @throws RemoteException in case of an exception
   */
  public void setMaintenanceInterval(int interval) throws RemoteException {
    server.setMaintenanceInterval(interval * THOUSAND);
  }

  /**
   * @return the server's connection maintenance interval in seconds
   * @throws RemoteException in case of an exception
   */
  public int getMaintenanceInterval() throws RemoteException {
    return server.getMaintenanceInterval() / THOUSAND;
  }

  /**
   * Resets the user connection history
   */
  public void resetHistory() {
    userHistoryTableModel.clear();
  }

  /**
   * @return a Value linked to the idle connection timeout
   */
  public Value<Integer> idleConnectionTimeout() {
    return idleConnectionTimeoutValue;
  }

  /**
   * @return the value controlling the update interval
   */
  public Value<Integer> updateInterval() {
    return updateIntervalValue;
  }

  private int getIdleConnectionTimeout() {
    try {
      return server.getIdleConnectionTimeout() / THOUSAND;
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the idle client connection timeout
   * @param idleConnectionTimeout the timeout in seconds
   */
  private void setIdleConnectionTimeout(int idleConnectionTimeout) {
    if (idleConnectionTimeout < 0) {
      throw new IllegalArgumentException("Idle connection timeout must be a positive integer");
    }
    try {
      server.setIdleConnectionTimeout(idleConnectionTimeout * THOUSAND);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  private void refreshUserHistoryTableModel() {
    try {
      userHistoryTableModel.refresh();
    }
    catch (Exception e) {
      LOG.error("Error while refreshing user history table model", e);
    }
  }

  private static List<FilteredTableColumn<Integer>> createUserHistoryColumns() {
    return asList(
            createColumn(USERNAME_COLUMN, "Username", String.class),
            createColumn(CLIENT_TYPE_COLUMN, "Client type", String.class),
            createColumn(CLIENT_VERSION_COLUMN, "Client version", Version.class),
            createColumn(FRAMEWORK_VERSION_COLUMN, "Framework version", Version.class),
            createColumn(CLIENT_HOST_COLUMN, "Host", String.class),
            createColumn(LAST_SEEN_COLUMN, "Last seen", LocalDateTime.class, new LastSeenRenderer()),
            createColumn(CONNECTION_COUNT_COLUMN, "Connections", Integer.class));
  }

  private static FilteredTableColumn<Integer> createColumn(Integer identifier, String headerValue, Class<?> columnClass) {
    return createColumn(identifier, headerValue, columnClass, null);
  }

  private static FilteredTableColumn<Integer> createColumn(Integer identifier, String headerValue,
                                                           Class<?> columnClass, TableCellRenderer cellRenderer) {
    return FilteredTableColumn.builder(identifier)
            .headerValue(headerValue)
            .columnClass(columnClass)
            .cellRenderer(cellRenderer)
            .build();
  }

  private final class UserHistoryItemSupplier implements Supplier<Collection<UserInfo>> {

    @Override
    public Collection<UserInfo> get() {
      try {
        List<UserInfo> items = new ArrayList<>(userHistoryTableModel.items());
        for (RemoteClient remoteClient : server.clients()) {
          UserInfo newUserInfo = new UserInfo(remoteClient.user(), remoteClient.clientTypeId(),
                  remoteClient.clientHost(), LocalDateTime.now(), remoteClient.clientId(),
                  remoteClient.clientVersion(), remoteClient.frameworkVersion());
          int index = items.indexOf(newUserInfo);
          if (index == -1) {
            items.add(newUserInfo);
          }
          else {
            UserInfo currentUserInfo = items.get(index);
            currentUserInfo.setLastSeen(newUserInfo.getLastSeen());
            if (currentUserInfo.isNewConnection(newUserInfo.getClientId())) {
              currentUserInfo.incrementConnectionCount();
              currentUserInfo.setClientID(newUserInfo.getClientId());
            }
          }
        }

        return items;
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class UserHistoryColumnValueProvider implements ColumnValueProvider<UserInfo, Integer> {

    @Override
    public Object value(UserInfo row, Integer columnIdentifier) {
      switch (columnIdentifier) {
        case USERNAME_COLUMN:
          return row.user().username();
        case CLIENT_TYPE_COLUMN:
          return row.clientTypeId();
        case CLIENT_VERSION_COLUMN:
          return row.clientVersion();
        case FRAMEWORK_VERSION_COLUMN:
          return row.frameworkVersion();
        case CLIENT_HOST_COLUMN:
          return row.clientHost();
        case LAST_SEEN_COLUMN:
          return row.getLastSeen();
        case CONNECTION_COUNT_COLUMN:
          return row.connectionCount();
        default:
          throw new IllegalArgumentException(columnIdentifier.toString());
      }
    }
  }

  private static final class UserInfo {

    private final User user;
    private final String clientTypeId;
    private final String clientHost;
    private final Version clientVersion;
    private final Version frameworkVersion;
    private LocalDateTime lastSeen;
    private UUID clientId;
    private int connectionCount = 1;

    private UserInfo(User user, String clientTypeId, String clientHost, LocalDateTime lastSeen,
                     UUID clientId, Version clientVersion, Version frameworkVersion) {
      this.user = user;
      this.clientTypeId = clientTypeId;
      this.clientHost = clientHost;
      this.lastSeen = lastSeen;
      this.clientId = clientId;
      this.clientVersion = clientVersion;
      this.frameworkVersion = frameworkVersion;
    }

    public User user() {
      return user;
    }

    public String clientTypeId() {
      return clientTypeId;
    }

    public String clientHost() {
      return clientHost;
    }

    public LocalDateTime getLastSeen() {
      return lastSeen;
    }

    public UUID getClientId() {
      return clientId;
    }

    public Version clientVersion() {
      return clientVersion;
    }

    public Version frameworkVersion() {
      return frameworkVersion;
    }

    public int connectionCount() {
      return connectionCount;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
      this.lastSeen = lastSeen;
    }

    public void setClientID(UUID clientId) {
      this.clientId = clientId;
    }

    public void incrementConnectionCount() {
      connectionCount++;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof UserInfo)) {
        return false;
      }

      UserInfo that = (UserInfo) obj;

      return this.user.username().equalsIgnoreCase(that.user.username()) &&
              this.clientTypeId.equals(that.clientTypeId) && this.clientHost.equals(that.clientHost);
    }

    @Override
    public int hashCode() {
      int result = user.username().toLowerCase().hashCode();
      result = 31 * result + clientTypeId.hashCode();
      result = 31 * result + clientHost.hashCode();

      return result;
    }

    public boolean isNewConnection(UUID clientId) {
      return !this.clientId.equals(clientId);
    }
  }

  private static final class LastSeenRenderer extends DefaultTableCellRenderer {

    private final DateTimeFormatter formatter = LocaleDateTimePattern.builder()
            .delimiterDash().yearFourDigits().hoursMinutesSeconds()
            .build().createFormatter();

    @Override
    protected void setValue(Object value) {
      if (value instanceof Temporal) {
        super.setValue(formatter.format((Temporal) value));
      }
      else {
        super.setValue(value);
      }
    }
  }
}
