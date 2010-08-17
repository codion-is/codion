/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.framework.server.monitor.HostMonitor;
import org.jminor.framework.server.monitor.ServerMonitor;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A HostMonitorPanel 
 */
public final class HostMonitorPanel extends JPanel {

  private final HostMonitor model;

  private JTabbedPane serverPane;

  public HostMonitorPanel(final HostMonitor model) throws RemoteException {
    this.model = model;
    initUI();
    bindEvents();
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout(5,5));
    add(ControlProvider.createToolbar(getControls(), JToolBar.HORIZONTAL), BorderLayout.NORTH);
    serverPane = new JTabbedPane();
    serverPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    add(serverPane, BorderLayout.CENTER);
    refreshServerTabs();
  }

  private ControlSet getControls() {
    final ControlSet controlSet = new ControlSet();
    controlSet.add(Controls.methodControl(model, "refresh", "Refresh"));

    return controlSet;
  }

  private void bindEvents() {
    model.addRefreshListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          refreshServerTabs();
        }
        catch (RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    model.addServerMonitorRemovedListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          refreshServerTabs();
        }
        catch (RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
  }

  private void refreshServerTabs() throws RemoteException {
    final Collection<ServerMonitorPanel> serverTabs = new ArrayList<ServerMonitorPanel>();
    for (int i = 0; i < serverPane.getTabCount(); i++) {
      serverTabs.add((ServerMonitorPanel) serverPane.getComponentAt(i));
    }

    final Collection<ServerMonitor> serverMonitors = model.getServerMonitors();
    //remove disconnected server tabs and remove server names that already have tabs
    for (final ServerMonitorPanel panel : serverTabs) {
      final ServerMonitor serverMonitor = panel.getModel();
      if (!serverMonitors.contains(serverMonitor)) {
        removeServerTab(panel);
      }
      serverMonitors.remove(serverMonitor);
    }

    //add the remaining servers
    for (final ServerMonitor serverMonitor : serverMonitors) {
      addServerTab(serverMonitor);
    }
  }

  private void addServerTab(final ServerMonitor serverMonitor) throws RemoteException {
    final ServerMonitorPanel serverMonitorPanel = new ServerMonitorPanel(serverMonitor);
    serverPane.add(serverMonitor.getServerName(), serverMonitorPanel);
  }

  private void removeServerTab(final ServerMonitorPanel panel) {
    serverPane.remove(panel);
  }
}
