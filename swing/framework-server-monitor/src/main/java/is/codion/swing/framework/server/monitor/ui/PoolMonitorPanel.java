/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.framework.server.monitor.ConnectionPoolMonitor;
import is.codion.swing.framework.server.monitor.PoolMonitor;

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
    JTabbedPane connectionPoolPane = new JTabbedPane();
    for (final ConnectionPoolMonitor monitor : model.getConnectionPoolInstanceMonitors()) {
      connectionPoolPane.addTab(monitor.getUsername(), new ConnectionPoolMonitorPanel(monitor));
    }
    add(connectionPoolPane, BorderLayout.CENTER);
  }
}
