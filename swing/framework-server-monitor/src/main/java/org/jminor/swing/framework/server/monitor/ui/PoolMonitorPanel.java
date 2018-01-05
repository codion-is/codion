/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.swing.framework.server.monitor.ConnectionPoolMonitor;
import org.jminor.swing.framework.server.monitor.PoolMonitor;

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

  private void initializeUI() {
    setLayout(new BorderLayout());
    final JTabbedPane connectionPoolPane = new JTabbedPane();
    for (final ConnectionPoolMonitor monitor : model.getConnectionPoolInstanceMonitors()) {
      connectionPoolPane.addTab(monitor.getUser().getUsername(), new ConnectionPoolMonitorPanel(monitor));
    }
    add(connectionPoolPane, BorderLayout.CENTER);
  }
}
