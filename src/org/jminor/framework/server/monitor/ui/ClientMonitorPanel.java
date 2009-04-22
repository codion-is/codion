/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.framework.server.monitor.ClientMonitor;

import javax.swing.JPanel;
import java.rmi.RemoteException;

/**
 * User: Bj�rn Darri
 * Date: 10.12.2007
 * Time: 17:32:53
 */
public class ClientMonitorPanel extends JPanel {

  private final ClientMonitor model;

  public ClientMonitorPanel(final ClientMonitor model) throws RemoteException {
    this.model = model;
  }
}
