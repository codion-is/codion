/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  public PoolMonitorPanel(PoolMonitor model) {
    this.model = model;
    initializeUI();
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    JTabbedPane connectionPoolPane = new JTabbedPane();
    for (ConnectionPoolMonitor monitor : model.connectionPoolInstanceMonitors()) {
      connectionPoolPane.addTab(monitor.username(), new ConnectionPoolMonitorPanel(monitor));
    }
    add(connectionPoolPane, BorderLayout.CENTER);
  }
}
