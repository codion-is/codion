/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.User;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.swing.model.table.AbstractFilteredTableModel;
import org.jminor.common.swing.model.table.AbstractTableSortModel;
import org.jminor.common.swing.model.table.FilteredTableModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.server.EntityConnectionServerAdmin;

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
 * A ClientUserMonitor
 */
public final class ClientUserMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(ClientUserMonitor.class);

  private static final int THOUSAND = 1000;

  private static final int USERNAME_COLUMN = 0;
  private static final int CLIENT_TYPE_COLUMN = 1;
  private static final int CLIENT_HOST_COLUMN = 2;
  private static final int LAST_SEEN_COLUMN = 3;
  private static final int CONNECTION_COUNT_COLUMN = 4;
  private static final Comparator<User> USER_COMPARATOR = new Comparator<User>() {
    @Override
    public int compare(final User u1, final User u2) {
      return u1.getUsername().compareToIgnoreCase(u2.getUsername());
    }
  };

  private final EntityConnectionServerAdmin server;
  private final Event<Integer> connectionTimeoutChangedEvent = Events.event();
  private final DefaultListModel<ClientMonitor> clientTypeListModel = new DefaultListModel<>();
  private final DefaultListModel<ClientMonitor> userListModel = new DefaultListModel<>();
  private final FilteredTableModel<UserInfo, Integer> userHistoryTableModel = new UserHistoryTableModel();

  private final TaskScheduler updateScheduler = new TaskScheduler(new Runnable() {
    @Override
    public void run() {
      try {
        userHistoryTableModel.refresh();
      }
      catch (final Exception e) {
        LOG.error("Error while refreshing user history table model", e);
      }
    }
  }, Configuration.getIntValue(Configuration.SERVER_MONITOR_UPDATE_RATE), 2, TimeUnit.SECONDS).start();

  public ClientUserMonitor(final EntityConnectionServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void shutdown() {
    updateScheduler.stop();
  }

  public DefaultListModel<ClientMonitor> getClientTypeListModel() {
    return clientTypeListModel;
  }

  public DefaultListModel<ClientMonitor> getUserListModel() {
    return userListModel;
  }

  public FilteredTableModel getUserHistoryTableModel() {
    return userHistoryTableModel;
  }

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

  public void disconnectAll() throws RemoteException {
    server.removeConnections(false);
    refresh();
  }

  public void disconnectTimedOut() throws RemoteException {
    server.removeConnections(true);
    refresh();
  }

  public void setMaintenanceInterval(final int interval) throws RemoteException {
    server.setMaintenanceInterval(interval * THOUSAND);
  }

  public int getMaintenanceInterval() throws RemoteException {
    return server.getMaintenanceInterval() / THOUSAND;
  }

  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout() / THOUSAND;
  }

  public void setConnectionTimeout(final int timeout) throws RemoteException {
    server.setConnectionTimeout(timeout * THOUSAND);
    connectionTimeoutChangedEvent.fire(timeout);
  }

  public void resetHistory() {
    userHistoryTableModel.clear();
  }

  public EventObserver<Integer> getConnectionTimeoutObserver() {
    return connectionTimeoutChangedEvent.getObserver();
  }

  private List<String> getSortedClientTypes() throws RemoteException {
    final List<String> users = new ArrayList<>(server.getClientTypes());
    Collections.sort(users);

    return users;
  }

  private List<User> getSortedUsers() throws RemoteException {
    final List<User> users = new ArrayList<>(server.getUsers());
    Collections.sort(users, USER_COMPARATOR);

    return users;
  }

  public TaskScheduler getUpdateScheduler() {
    return updateScheduler;
  }

  private final class UserHistoryTableModel extends AbstractFilteredTableModel<UserInfo, Integer> {

    private UserHistoryTableModel() {
      super(new UserHistoryTableSortModel(), null);
    }

    @Override
    protected void doRefresh() {
      try {
        for (final ClientInfo clientInfo : server.getClients()) {
          final UserInfo newUserInfo = new UserInfo(clientInfo.getUser(), clientInfo.getClientTypeID(),
                  clientInfo.getClientHost(), new Date(), clientInfo.getClientID());
          if (contains(newUserInfo, true)) {
            final UserInfo currentUserInfo = getItemAt(indexOf(newUserInfo));
            currentUserInfo.setLastSeen(newUserInfo.getLastSeen());
            if (currentUserInfo.isNewConnection(newUserInfo.getClientID())) {
              currentUserInfo.incrementConnectionCount();
              currentUserInfo.setClientID(newUserInfo.getClientID());
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
        case CLIENT_TYPE_COLUMN: return userInfo.getClientTypeID();
        case CLIENT_HOST_COLUMN: return userInfo.getClientHost();
        case LAST_SEEN_COLUMN: return userInfo.getLastSeen();
        case CONNECTION_COUNT_COLUMN: return userInfo.getConnectionCount();
        default: throw new IllegalArgumentException(Integer.toString(column));
      }
    }
  }

  private static final class UserInfo {

    private final User user;
    private final String clientTypeID;
    private final String clientHost;
    private Date lastSeen;
    private UUID clientID;
    private int connectionCount = 1;

    private UserInfo(final User user, final String clientTypeID, final String clientHost, final Date lastSeen,
                     final UUID clientID) {
      this.user = user;
      this.clientTypeID = clientTypeID;
      this.clientHost = clientHost;
      this.lastSeen = lastSeen;
      this.clientID = clientID;
    }

    public User getUser() {
      return user;
    }

    public String getClientTypeID() {
      return clientTypeID;
    }

    public String getClientHost() {
      return clientHost;
    }

    public Date getLastSeen() {
      return lastSeen;
    }

    public UUID getClientID() {
      return clientID;
    }

    public int getConnectionCount() {
      return connectionCount;
    }

    public void setLastSeen(final Date lastSeen) {
      this.lastSeen = lastSeen;
    }

    public void setClientID(final UUID clientID) {
      this.clientID = clientID;
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

      return this.user.equals(that.user) && this.clientTypeID.equals(that.clientTypeID) && this.clientHost.equals(that.clientHost);
    }

    @Override
    public int hashCode() {
      int result = user.hashCode();
      result = 31 * result + clientTypeID.hashCode();
      result = 31 * result + clientHost.hashCode();

      return result;
    }

    public boolean isNewConnection(final UUID clientID) {
      return !this.clientID.equals(clientID);
    }
  }

  private static final class UserHistoryTableSortModel extends AbstractTableSortModel<UserInfo, Integer> {

    private UserHistoryTableSortModel() {
      super(createUserHistoryColumns());
    }

    @Override
    public Class getColumnClass(final Integer columnIdentifier) {
      switch (columnIdentifier) {
        case USERNAME_COLUMN:
        case CLIENT_TYPE_COLUMN:
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
        case CLIENT_TYPE_COLUMN: return rowObject.getClientTypeID();
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

      return Arrays.asList(username, clientType, host, lastSeen, connectionCount);
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
