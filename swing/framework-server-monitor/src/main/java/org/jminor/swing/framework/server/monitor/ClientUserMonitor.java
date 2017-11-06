/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.DateFormats;
import org.jminor.common.Event;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.TaskScheduler;
import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.server.RemoteClient;
import org.jminor.framework.server.EntityConnectionServerAdmin;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.model.table.AbstractTableSortModel;
import org.jminor.swing.common.model.table.FilteredTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.rmi.RemoteException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
  private static final Comparator<User> USER_COMPARATOR = (u1, u2) -> u1.getUsername().compareToIgnoreCase(u2.getUsername());

  private final EntityConnectionServerAdmin server;
  private final Event<Integer> connectionTimeoutChangedEvent = Events.event();
  private final DefaultListModel<ClientMonitor> clientTypeListModel = new DefaultListModel<>();
  private final DefaultListModel<ClientMonitor> userListModel = new DefaultListModel<>();
  private final FilteredTableModel<UserInfo, Integer> userHistoryTableModel = new UserHistoryTableModel();

  private final TaskScheduler updateScheduler = new TaskScheduler(() -> {
    try {
      userHistoryTableModel.refresh();
    }
    catch (final Exception e) {
      LOG.error("Error while refreshing user history table model", e);
    }
  }, EntityServerMonitor.SERVER_MONITOR_UPDATE_RATE.get(), 2, TimeUnit.SECONDS).start();

  /**
   * Instantiates a new {@link ClientUserMonitor}
   * @param server the server
   * @throws RemoteException in case of a communication error
   */
  public ClientUserMonitor(final EntityConnectionServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  /**
   * Shuts down this monitor
   */
  public void shutdown() {
    updateScheduler.stop();
  }

  /**
   * @return a ListModel containing the connected client types
   */
  public DefaultListModel<ClientMonitor> getClientTypeListModel() {
    return clientTypeListModel;
  }

  /**
   * @return a ListModel containing the connected users
   */
  public DefaultListModel<ClientMonitor> getUserListModel() {
    return userListModel;
  }

  /**
   * @return a TableModel for displaying the user connection history
   */
  public FilteredTableModel getUserHistoryTableModel() {
    return userHistoryTableModel;
  }

  /**
   * Refreshes the user and client data from the server
   * @throws RemoteException in case of a communication error
   */
  public void refresh() throws RemoteException {
    clientTypeListModel.clear();
    for (final String clientType : getSortedClientTypes()) {
      clientTypeListModel.addElement(new ClientMonitor(server, clientType, null));
    }
    userListModel.clear();
    for (final User user : getSortedUsers()) {
      userListModel.addElement(new ClientMonitor(server, null, user));
    }
  }

  /**
   * Disconnects all users from the server
   * @throws RemoteException in case of an exception
   */
  public void disconnectAll() throws RemoteException {
    server.removeConnections(false);
    refresh();
  }

  /**
   * Disconnects all timed out users from the server
   * @throws RemoteException in case of an exception
   */
  public void disconnectTimedOut() throws RemoteException {
    server.removeConnections(true);
    refresh();
  }

  /**
   * Sets the servers connection maintenance interval
   * @param interval the maintenance interval in seconds
   * @throws RemoteException in case of an exception
   */
  public void setMaintenanceInterval(final int interval) throws RemoteException {
    server.setMaintenanceInterval(interval * THOUSAND);
  }

  /**
   * @return the servers connection maintenance interval in seconds
   * @throws RemoteException in case of an exception
   */
  public int getMaintenanceInterval() throws RemoteException {
    return server.getMaintenanceInterval() / THOUSAND;
  }

  /**
   * @return the server timeout for connections in seconds
   * @throws RemoteException in case of an exception
   */
  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout() / THOUSAND;
  }

  /**
   * Sets the server connection timeout
   * @param timeout the timeout in seconds
   * @throws RemoteException in case of an exception
   */
  public void setConnectionTimeout(final int timeout) throws RemoteException {
    server.setConnectionTimeout(timeout * THOUSAND);
    connectionTimeoutChangedEvent.fire(timeout);
  }

  /**
   * Resets the user connection history
   */
  public void resetHistory() {
    userHistoryTableModel.clear();
  }

  /**
   * @return an EventObserver notified when the connection timeout is changed
   */
  public EventObserver<Integer> getConnectionTimeoutObserver() {
    return connectionTimeoutChangedEvent.getObserver();
  }

  /**
   * @return the data update scheduler
   */
  public TaskScheduler getUpdateScheduler() {
    return updateScheduler;
  }

  private List<String> getSortedClientTypes() throws RemoteException {
    final List<String> users = new ArrayList<>(server.getClientTypes());
    Collections.sort(users);

    return users;
  }

  private List<User> getSortedUsers() throws RemoteException {
    final List<User> users = new ArrayList<>(server.getUsers());
    users.sort(USER_COMPARATOR);

    return users;
  }

  private final class UserHistoryTableModel extends AbstractFilteredTableModel<UserInfo, Integer> {

    private UserHistoryTableModel() {
      super(new UserHistoryTableSortModel(), null);
    }

    @Override
    protected void doRefresh() {
      try {
        for (final RemoteClient remoteClient : server.getClients()) {
          final UserInfo newUserInfo = new UserInfo(remoteClient.getUser(), remoteClient.getClientTypeId(),
                  remoteClient.getClientHost(), new Date(), remoteClient.getClientId(), remoteClient.getClientVersion(),
                  remoteClient.getFrameworkVersion());
          if (contains(newUserInfo, true)) {
            final UserInfo currentUserInfo = getItemAt(indexOf(newUserInfo));
            currentUserInfo.setLastSeen(newUserInfo.getLastSeen());
            if (currentUserInfo.isNewConnection(newUserInfo.getClientId())) {
              currentUserInfo.incrementConnectionCount();
              currentUserInfo.setClientID(newUserInfo.getClientId());
            }
          }
          else {
            addItems(Collections.singletonList(newUserInfo), true);
          }
        }
        sortContents();
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Object getValueAt(final int row, final int column) {
      final UserInfo userInfo = getItemAt(row);
      switch (column) {
        case USERNAME_COLUMN: return userInfo.getUser().getUsername();
        case CLIENT_TYPE_COLUMN: return userInfo.getClientTypeId();
        case CLIENT_VERSION_COLUMN: return userInfo.getClientVersion();
        case FRAMEWORK_VERSION_COLUMN: return userInfo.getFrameworkVersion();
        case CLIENT_HOST_COLUMN: return userInfo.getClientHost();
        case LAST_SEEN_COLUMN: return userInfo.getLastSeen();
        case CONNECTION_COUNT_COLUMN: return userInfo.getConnectionCount();
        default: throw new IllegalArgumentException(Integer.toString(column));
      }
    }
  }

  private static final class UserInfo {

    private final User user;
    private final String clientTypeId;
    private final String clientHost;
    private final Version clientVersion;
    private final Version frameworkVersion;
    private Date lastSeen;
    private UUID clientId;
    private int connectionCount = 1;

    private UserInfo(final User user, final String clientTypeId, final String clientHost, final Date lastSeen,
                     final UUID clientId, final Version clientVersion, final Version frameworkVersion) {
      this.user = user;
      this.clientTypeId = clientTypeId;
      this.clientHost = clientHost;
      this.lastSeen = lastSeen;
      this.clientId = clientId;
      this.clientVersion = clientVersion;
      this.frameworkVersion = frameworkVersion;
    }

    public User getUser() {
      return user;
    }

    public String getClientTypeId() {
      return clientTypeId;
    }

    public String getClientHost() {
      return clientHost;
    }

    public Date getLastSeen() {
      return lastSeen;
    }

    public UUID getClientId() {
      return clientId;
    }

    public Version getClientVersion() {
      return clientVersion;
    }

    public Version getFrameworkVersion() {
      return frameworkVersion;
    }

    public int getConnectionCount() {
      return connectionCount;
    }

    public void setLastSeen(final Date lastSeen) {
      this.lastSeen = lastSeen;
    }

    public void setClientID(final UUID clientId) {
      this.clientId = clientId;
    }

    public void incrementConnectionCount() {
      connectionCount++;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof UserInfo)) {
        return false;
      }

      final UserInfo that = (UserInfo) obj;

      return this.user.getUsername().equalsIgnoreCase(that.user.getUsername()) &&
              this.clientTypeId.equals(that.clientTypeId) && this.clientHost.equals(that.clientHost);
    }

    @Override
    public int hashCode() {
      int result = user.getUsername().toLowerCase().hashCode();
      result = 31 * result + clientTypeId.hashCode();
      result = 31 * result + clientHost.hashCode();

      return result;
    }

    public boolean isNewConnection(final UUID clientId) {
      return !this.clientId.equals(clientId);
    }
  }

  private static final class UserHistoryTableSortModel extends AbstractTableSortModel<UserInfo, Integer> {

    private UserHistoryTableSortModel() {
      super(createUserHistoryColumns());
    }

    @Override
    public Class getColumnClass(final Integer columnIdentifier) {
      switch (columnIdentifier) {
        case USERNAME_COLUMN: return String.class;
        case CLIENT_TYPE_COLUMN: return String.class;
        case CLIENT_VERSION_COLUMN: return Version.class;
        case FRAMEWORK_VERSION_COLUMN: return Version.class;
        case CLIENT_HOST_COLUMN: return String.class;
        case LAST_SEEN_COLUMN: return Date.class;
        case CONNECTION_COUNT_COLUMN: return Integer.class;
        default: throw new IllegalArgumentException(columnIdentifier.toString());
      }
    }

    @Override
    protected Comparable getComparable(final UserInfo rowObject, final Integer columnIdentifier) {
      switch (columnIdentifier) {
        case USERNAME_COLUMN: return rowObject.getUser().getUsername();
        case CLIENT_TYPE_COLUMN: return rowObject.getClientTypeId();
        case CLIENT_VERSION_COLUMN: return rowObject.getClientVersion();
        case FRAMEWORK_VERSION_COLUMN: return rowObject.getFrameworkVersion();
        case CLIENT_HOST_COLUMN: return rowObject.getClientHost();
        case LAST_SEEN_COLUMN: return rowObject.getLastSeen();
        case CONNECTION_COUNT_COLUMN: return rowObject.getConnectionCount();
        default: throw new IllegalArgumentException(columnIdentifier.toString());
      }
    }

    private static List<TableColumn> createUserHistoryColumns() {
      final TableColumn username = new TableColumn(USERNAME_COLUMN);
      username.setIdentifier(USERNAME_COLUMN);
      username.setHeaderValue("Username");
      final TableColumn clientType = new TableColumn(CLIENT_TYPE_COLUMN);
      clientType.setIdentifier(CLIENT_TYPE_COLUMN);
      clientType.setHeaderValue("Client type");
      final TableColumn clientVersion = new TableColumn(CLIENT_VERSION_COLUMN);
      clientVersion.setIdentifier(CLIENT_VERSION_COLUMN);
      clientVersion.setHeaderValue("Client version");
      final TableColumn frameworkVersion = new TableColumn(FRAMEWORK_VERSION_COLUMN);
      frameworkVersion.setIdentifier(FRAMEWORK_VERSION_COLUMN);
      frameworkVersion.setHeaderValue("Framework version");
      final TableColumn host = new TableColumn(CLIENT_HOST_COLUMN);
      host.setIdentifier(CLIENT_HOST_COLUMN);
      host.setHeaderValue("Host");
      final TableColumn lastSeen = new TableColumn(LAST_SEEN_COLUMN);
      lastSeen.setIdentifier(LAST_SEEN_COLUMN);
      lastSeen.setHeaderValue("Last seen");
      lastSeen.setCellRenderer(new LastSeenRenderer());
      final TableColumn connectionCount = new TableColumn(CONNECTION_COUNT_COLUMN);
      connectionCount.setIdentifier(CONNECTION_COUNT_COLUMN);
      connectionCount.setHeaderValue("Connections");

      return Arrays.asList(username, clientType, clientVersion, frameworkVersion, host, lastSeen, connectionCount);
    }
  }

  private static final class LastSeenRenderer extends DefaultTableCellRenderer {

    private final Format formatter = DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP);

    @Override
    protected void setValue(final Object value) {
      if (value instanceof Date) {
        super.setValue(formatter.format(value));
      }
      else {
        super.setValue(value);
      }
    }
  }
}
