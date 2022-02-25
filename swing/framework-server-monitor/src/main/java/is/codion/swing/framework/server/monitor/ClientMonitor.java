/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.user.User;
import is.codion.framework.server.EntityServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A ClientMonitor
 */
public final class ClientMonitor {

  private static final Comparator<RemoteClient> CLIENT_INFO_COMPARATOR = (c1, c2) -> c1.getUser().getUsername().compareToIgnoreCase(c2.getUser().getUsername());
  private final EntityServerAdmin server;
  private final String clientTypeId;
  private final User user;

  private final DefaultListModel<RemoteClient> clientInstanceListModel = new DefaultListModel<>();

  /**
   * Instantiates a new {@link ClientMonitor}
   * @param server the server being monitored
   * @param clientTypeId the clientTypeId of the clients to monitor
   * @param user the user to monitor
   * @throws RemoteException in case of an exception
   */
  public ClientMonitor(final EntityServerAdmin server, final String clientTypeId, final User user) throws RemoteException {
    this.server = server;
    this.clientTypeId = clientTypeId;
    this.user = user;
    refresh();
  }

  /**
   * Refreshes the client info from the server
   * @throws RemoteException in case of an exception
   */
  public void refresh() throws RemoteException {
    clientInstanceListModel.clear();
    List<RemoteClient> clients = new ArrayList<>(clientTypeId == null ? server.getClients(user) : server.getClients(clientTypeId));
    clients.sort(CLIENT_INFO_COMPARATOR);
    for (final RemoteClient client : clients) {
      clientInstanceListModel.addElement(client);
    }
  }

  /**
   * @return the ListModel for displaying the client instances
   */
  public DefaultListModel<RemoteClient> getRemoteClientListModel() {
    return clientInstanceListModel;
  }

  public EntityServerAdmin getServer() {
    return server;
  }

  /**
   * @return the clientTypeId being monitored
   */
  public String getClientTypeId() {
    return clientTypeId;
  }

  @Override
  public String toString() {
    return clientTypeId == null ? user.toString() : clientTypeId;
  }
}