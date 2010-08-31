/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.loadtest;

import org.jminor.common.server.AbstractRemoteServer;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;

public class LoadTestServer extends AbstractRemoteServer<RemoteLoadTest> {

  public static final String SERVER_NAME = "LoadTestServer";
  private static final Logger LOG = LoggerFactory.getLogger(LoadTestServer.class);
  private final int loadTestPort;

  static {
    System.setSecurityManager(new RMISecurityManager());
    try {
      Util.initializeRegistry();
    }
    catch (RemoteException re) {
      throw new RuntimeException(re);
    }
  }

  public LoadTestServer(final int serverPort, final int loadTestPort, final String serverName) throws RemoteException {
    super(serverPort, serverName, RMISocketFactory.getSocketFactory(), RMISocketFactory.getSocketFactory());
    this.loadTestPort = loadTestPort;
    getRegistry().rebind(serverName, this);
  }

  protected RemoteLoadTest doConnect(final ClientInfo clientInfo) throws RemoteException {
    final RemoteLoadTest remoteAdapter = new RemoteLoadTestAdapter(clientInfo, loadTestPort);

    LOG.debug(clientInfo + " connected");

    return remoteAdapter;
  }

  protected void doDisconnect(final RemoteLoadTest connection) throws RemoteException {
    LOG.debug(((RemoteLoadTestAdapter) connection).getClientInfo() + " disconnected");
  }

  public int getServerLoad() throws RemoteException {
    return 0;
  }

  /**
   * Runs a new EntityDbRemote server with a server admin interface exported.
   * @param arguments no arguments required
   * @throws Exception in case of an exception
   */
  public static void main(final String[] arguments) throws Exception {
    new LoadTestServer(6666, 6666, SERVER_NAME);
    System.out.println(SERVER_NAME);
    LOG.debug(SERVER_NAME + " bound to registry");
  }
}
