/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;
import org.jminor.framework.server.monitor.PoolMonitor;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/**
 * A PoolMonitorPanel
 */
public final class PoolMonitorPanel extends JPanel {

  private final PoolMonitor model;

  /**
   * Instantiates a new PoolMonitorPanel
   * @param model the PoolMonitor to base this panel on
   */
  public PoolMonitorPanel(final PoolMonitor model) {
    this.model = model;
    initializeUI();
  }

  public void addConnectionPool() {
    final String usernames = JOptionPane.showInputDialog("User name(s) (comma separated)");
    if (!Util.nullOrEmpty(usernames)) {
      model.addConnectionPools(usernames.split(","));
    }
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    final JTabbedPane connectionPoolPane = new JTabbedPane();
    connectionPoolPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final ConnectionPoolMonitor monitor : model.getConnectionPoolInstanceMonitors()) {
      connectionPoolPane.addTab(monitor.getUser().getUsername(), new ConnectionPoolMonitorPanel(monitor));
    }
    add(connectionPoolPane, BorderLayout.CENTER);
  }
}
