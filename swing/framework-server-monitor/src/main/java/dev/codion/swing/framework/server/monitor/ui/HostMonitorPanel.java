/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.server.monitor.ui;

import dev.codion.swing.common.ui.control.ControlList;
import dev.codion.swing.common.ui.control.ControlProvider;
import dev.codion.swing.common.ui.control.Controls;
import dev.codion.swing.common.ui.layout.Layouts;
import dev.codion.swing.framework.server.monitor.HostMonitor;
import dev.codion.swing.framework.server.monitor.ServerMonitor;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

/**
 * A HostMonitorPanel
 */
public final class HostMonitorPanel extends JPanel {

  private final HostMonitor model;

  private JTabbedPane serverPane;

  /**
   * Instantiates a new HostMonitorPanel
   * @param model the HostMonitor to base this panel on
   * @throws RemoteException in case of an exception
   */
  public HostMonitorPanel(final HostMonitor model) throws RemoteException {
    this.model = model;
    initializeUI();
    bindEvents();
  }

  private void initializeUI() throws RemoteException {
    setLayout(Layouts.borderLayout());
    add(ControlProvider.createToolBar(getControls(), JToolBar.HORIZONTAL), BorderLayout.NORTH);
    serverPane = new JTabbedPane();
    add(serverPane, BorderLayout.CENTER);
    initializeServerTabs();
  }

  private ControlList getControls() {
    return Controls.controlList(Controls.control(model::refresh, "Refresh"));
  }

  private void bindEvents() {
    model.addServerAddedListener(serverMonitor -> {
      try {
        addServerTab(serverMonitor);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    });
    model.addServerRemovedListener(serverMonitor -> {
      for (int i = 0; i < serverPane.getTabCount(); i++) {
        final ServerMonitorPanel panel = (ServerMonitorPanel) serverPane.getComponentAt(i);
        if (panel.getModel() == serverMonitor) {
          removeServerTab(panel);
        }
      }
    });
  }

  private void initializeServerTabs() throws RemoteException {
    for (final ServerMonitor serverMonitor : model.getServerMonitors()) {
      addServerTab(serverMonitor);
    }
  }

  private void addServerTab(final ServerMonitor serverMonitor) throws RemoteException {
    final ServerMonitorPanel serverMonitorPanel = new ServerMonitorPanel(serverMonitor);
    serverPane.addTab(serverMonitor.getServerInformation().getServerName(), serverMonitorPanel);
  }

  private void removeServerTab(final ServerMonitorPanel panel) {
    serverPane.remove(panel);
  }
}
