/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.HostMonitor;
import org.jminor.framework.server.monitor.ServerMonitor;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:12:28
 */
public class HostMonitorPanel extends JPanel {

  private final HostMonitor model;

  public HostMonitorPanel(final HostMonitor model) throws RemoteException {
    this.model = model;
    initUI();
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout(5,5));
    final JTabbedPane serverPane = new JTabbedPane();
    serverPane.setUI(new BasicTabbedPaneUI() {
      @Override
      protected Insets getContentBorderInsets(final int tabPlacement) {
        return new Insets(1,0,1,0);
      }

      @Override
      protected Insets getSelectedTabPadInsets(int tabPlacement) {
        return new Insets(2,2,2,1);
      }

      @Override
      protected Insets getTabAreaInsets(int tabPlacement) {
        return new Insets(3,2,0,2);
      }

      @Override
      protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return new Insets(0,4,1,4);
      }
    });
    for (final String serverName : model.getServerNames())
      serverPane.add(serverName, new ServerMonitorPanel(new ServerMonitor(model.getHostName(), serverName)));
    add(serverPane, BorderLayout.CENTER);
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return ret;
  }
}
