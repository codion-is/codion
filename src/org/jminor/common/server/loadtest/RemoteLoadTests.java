/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.loadtest;

import org.jminor.common.server.ClientInfo;

import java.rmi.RemoteException;

/**
 * A factory class for the RemoteLoadTest class
 */
public final class RemoteLoadTests {

  private RemoteLoadTests() {}

  /**
   * Instantiates a new RemoteLoadTestAdapter.
   * @param clientInfo the client info
   * @param loadTestPort the port on which to register the service
   * @return a remote load test instance
   * @throws RemoteException in case of an exception
   */
  public static RemoteLoadTest createInstance(final ClientInfo clientInfo, final int loadTestPort) throws RemoteException {
    return new RemoteLoadTestImpl(clientInfo, loadTestPort);
  }
}
