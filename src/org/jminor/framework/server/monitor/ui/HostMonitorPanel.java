/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.EventListener;
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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

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
    setLayout(UiUtil.createBorderLayout());
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
    model.getRefreshObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        try {
          refreshServerTabs();
        }
        catch (final RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    model.getServerMonitorRemovedObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        try {
          refreshServerTabs();
        }
        catch (final RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
  }

  private void refreshServerTabs() throws RemoteException {
    final Collection<ServerMonitorPanel> serverTabs = new ArrayList<>();
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
    serverPane.addTab(serverMonitor.getServerName(), serverMonitorPanel);
  }

  private void removeServerTab(final ServerMonitorPanel panel) {
    serverPane.remove(panel);
  }
}
