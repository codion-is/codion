/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.loadtest;

import org.jminor.common.model.Util;
import org.jminor.common.server.AbstractRemoteServer;
import org.jminor.common.server.ClientInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;

/**
 * A server for running load tests remotely.
 * @see org.jminor.common.model.LoadTestModel
 */
public final class LoadTestServer extends AbstractRemoteServer<RemoteLoadTest> {

  private static final long serialVersionUID = 1;

  private static final int DEFAULT_PORT = 6666;

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

  /**
   * Instantiates and exports a new LoadTestServer.
   * @param serverPort the port on which to serve clients
   * @param loadTestPort the port on which to export the load tests
   * @param serverName the name of this server
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  public LoadTestServer(final int serverPort, final int loadTestPort, final String serverName) throws RemoteException {
    super(serverPort, serverName, RMISocketFactory.getSocketFactory(), RMISocketFactory.getSocketFactory());
    this.loadTestPort = loadTestPort;
    Util.getRegistry().rebind(serverName, this);
  }

  /** {@inheritDoc} */
  @Override
  protected RemoteLoadTest doConnect(final ClientInfo clientInfo) throws RemoteException {
    final RemoteLoadTest remoteAdapter = new RemoteLoadTestAdapter(clientInfo, loadTestPort);

    LOG.debug(clientInfo + " connected");

    return remoteAdapter;
  }

  /** {@inheritDoc} */
  @Override
  protected void doDisconnect(final RemoteLoadTest connection) throws RemoteException {
    LOG.debug(((RemoteLoadTestAdapter) connection).getClientInfo() + " disconnected");
  }

  /**
   * Not implemented!
   * @return 0
   * @throws UnsupportedOperationException consistently
   * @throws RemoteException in case of a remote exception
   */
  public int getServerLoad() throws RemoteException {
    throw new UnsupportedOperationException();
  }

  /**
   * Runs a new EntityDbRemote server with a server admin interface exported.
   * @param arguments no arguments required
   * @throws Exception in case of an exception
   */
  public static void main(final String[] arguments) throws Exception {
    new LoadTestServer(DEFAULT_PORT, DEFAULT_PORT, SERVER_NAME);
    LOG.info(SERVER_NAME + " bound to registry");
  }
}
