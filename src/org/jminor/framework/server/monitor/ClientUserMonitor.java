/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.User;
import org.jminor.common.model.table.AbstractFilteredTableModel;
import org.jminor.common.model.table.AbstractTableSortModel;
import org.jminor.common.model.table.FilteredTableModel;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.Configuration;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.table.TableColumn;
import java.rmi.RemoteException;
import java.util.Arrays;
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

  public FilteredTableModel<UserInfo, Integer> getUserHistoryTableModel() {
    return userHistoryTableModel;
  }

  public void refresh() throws RemoteException {
    clientTypeListModel.clear();
    for (final String clientType : server.getClientTypes()) {
      clientTypeListModel.addElement(new ClientMonitor(server, clientType, null));
    }
    userListModel.clear();
    for (final User user : server.getUsers()) {
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

  public TaskScheduler getUpdateScheduler() {
    return updateScheduler;
  }

  public static final class UserInfo {
    private final User user;
    private String clientTypeID;
    private String clientHost;
    private long lastSeen;
    private UUID clientID;
    private int connectionCount = 1;

    private UserInfo(final User user, final String clientTypeID, final String clientHost, final long lastSeen,
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

    public long getLastSeen() {
      return lastSeen;
    }

    public void setClientTypeID(final String clientTypeID) {
      this.clientTypeID = clientTypeID;
    }

    public UUID getClientID() {
      return clientID;
    }

    public int getConnectionCount() {
      return connectionCount;
    }

    public void setClientHost(final String clientHost) {
      this.clientHost = clientHost;
    }

    public void setLastSeen(final long lastSeen) {
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
      return obj instanceof UserInfo && ((UserInfo) obj).user.equals(user);
    }

    @Override
    public int hashCode() {
      return user.hashCode();
    }

    public boolean isNewConnection(final UUID clientID) {
      return !this.clientID.equals(clientID);
    }
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
                  clientInfo.getClientHost(), System.currentTimeMillis(), clientInfo.getClientID());
          if (contains(newUserInfo, true)) {
            final int index = indexOf(newUserInfo);
            final UserInfo currentUserIinfo= getItemAt(index);
            currentUserIinfo.setClientHost(newUserInfo.getClientHost());
            currentUserIinfo.setClientTypeID(newUserInfo.getClientTypeID());
            currentUserIinfo.setLastSeen(newUserInfo.getLastSeen());
            if (currentUserIinfo.isNewConnection(newUserInfo.getClientID())) {
              currentUserIinfo.incrementConnectionCount();
              currentUserIinfo.setClientID(newUserInfo.getClientID());
            }
          }
          else {
            addItems(Arrays.asList(newUserInfo), true);
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
      final UserInfo rowObject = getItemAt(row);
      switch (column) {
        case USERNAME_COLUMN: return rowObject.getUser().getUsername();
        case CLIENT_TYPE_COLUMN: return rowObject.getClientTypeID();
        case CLIENT_HOST_COLUMN: return rowObject.getClientHost();
        case LAST_SEEN_COLUMN: return new Date(rowObject.getLastSeen());
        case CONNECTION_COUNT_COLUMN: return rowObject.getConnectionCount();
      }
      throw new IllegalArgumentException(Integer.toString(column));
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
        case CLIENT_HOST_COLUMN: return String.class;
        //simple way to get fully formatted date values
        case LAST_SEEN_COLUMN: return Object.class;
        case CONNECTION_COUNT_COLUMN: return Integer.class;
      }
      throw new IllegalArgumentException(columnIdentifier.toString());
    }

    @Override
    protected Comparable getComparable(final UserInfo rowObject, final Integer columnIdentifier) {
      switch (columnIdentifier) {
        case USERNAME_COLUMN: return rowObject.getUser().getUsername();
        case CLIENT_TYPE_COLUMN: return rowObject.getClientTypeID();
        case CLIENT_HOST_COLUMN: return rowObject.getClientHost();
        case LAST_SEEN_COLUMN: return rowObject.getLastSeen();
        case CONNECTION_COUNT_COLUMN: return rowObject.getConnectionCount();
      }
      throw new IllegalArgumentException(columnIdentifier.toString());
    }
  }

  private static List<TableColumn> createUserHistoryColumns() {
    final TableColumn username = new TableColumn(0);
    username.setIdentifier(0);
    username.setHeaderValue("Username");
    final TableColumn clientType = new TableColumn(1);
    clientType.setIdentifier(1);
    clientType.setHeaderValue("Client type");
    final TableColumn host = new TableColumn(2);
    host.setIdentifier(2);
    host.setHeaderValue("Host");
    final TableColumn lastSeen = new TableColumn(3);
    lastSeen.setIdentifier(3);
    lastSeen.setHeaderValue("Last seen");
    final TableColumn connectionCount = new TableColumn(4);
    connectionCount.setIdentifier(4);
    connectionCount.setHeaderValue("Connections");

    return Arrays.asList(username, clientType, host, lastSeen, connectionCount);
  }
}
