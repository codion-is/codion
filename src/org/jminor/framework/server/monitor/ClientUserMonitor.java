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
import org.jminor.common.model.table.TableSortModel;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.Configuration;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A ClientUserMonitor
 */
public final class ClientUserMonitor {

  private static final int THOUSAND = 1000;

  private final EntityConnectionServerAdmin server;
  private final Event<Integer> connectionTimeoutChangedEvent = Events.event();
  private final DefaultListModel<ClientMonitor> clientTypeListModel = new DefaultListModel<>();
  private final DefaultListModel<ClientMonitor> userListModel = new DefaultListModel<>();
  private final FilteredTableModel<UserInfo, Integer> userHistoryTableModel = new UserHistoryTableModel();

  private final TaskScheduler updateScheduler = new TaskScheduler(new Runnable() {
    @Override
    public void run() {
      userHistoryTableModel.refresh();
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
    private Date lastSeen;

    private UserInfo(User user, String clientTypeID, String clientHost, Date lastSeen) {
      this.user = user;
      this.clientTypeID = clientTypeID;
      this.clientHost = clientHost;
      this.lastSeen = lastSeen;
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

    public void setClientTypeID(String clientTypeID) {
      this.clientTypeID = clientTypeID;
    }

    public void setClientHost(String clientHost) {
      this.clientHost = clientHost;
    }

    public void setLastSeen(Date lastSeen) {
      this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof UserInfo && ((UserInfo) obj).user.equals(user);
    }

    @Override
    public int hashCode() {
      return user.hashCode();
    }
  }

  private final class UserHistoryTableModel extends AbstractFilteredTableModel<UserInfo, Integer> {
    private UserHistoryTableModel() {
      super(createSortModel(), null);
    }

    @Override
    protected void doRefresh() {
      try {
        for (final ClientInfo clientInfo : server.getClients()) {
          final UserInfo userInfo = new UserInfo(clientInfo.getUser(), clientInfo.getClientTypeID(), clientInfo.getClientHost(), new Date());
          if (contains(userInfo, true)) {
            final int index = indexOf(userInfo);
            final UserInfo info = getItemAt(index);
            info.setClientHost(userInfo.getClientHost());
            info.setClientTypeID(userInfo.getClientTypeID());
            info.setLastSeen(userInfo.getLastSeen());
            fireTableRowsUpdated(index, index);
          }
          else {
            addItems(Arrays.asList(userInfo), true);
          }
        }
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Object getValueAt(final int row, final int column) {
      final UserInfo rowObject = getItemAt(row);
      switch (column) {
        case 0: return rowObject.getUser().getUsername();
        case 1: return rowObject.getClientTypeID();
        case 2: return rowObject.getClientHost();
        case 3: return rowObject.getLastSeen();
      }
      throw new IllegalArgumentException(Integer.toString(column));
    }
  }

  private static TableSortModel<UserInfo, Integer> createSortModel() {
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

    return new AbstractTableSortModel<UserInfo, Integer>(Arrays.asList(username, clientType, host, lastSeen)) {
      @Override
      protected Comparable getComparable(final UserInfo rowObject, final Integer columnIdentifier) {
        switch (columnIdentifier) {
          case 0: return rowObject.getUser().getUsername();
          case 1: return rowObject.getClientTypeID();
          case 2: return rowObject.getClientHost();
          case 3: return rowObject.getLastSeen();
        }
        throw new IllegalArgumentException(columnIdentifier.toString());
      }

      @Override
      protected Class getColumnClass(final Integer columnIdentifier) {
        switch (columnIdentifier) {
          case 0: return String.class;
          case 1: return String.class;
          case 2: return String.class;
          case 3: return Date.class;
        }
        throw new IllegalArgumentException(columnIdentifier.toString());
      }
    };
  }
}
