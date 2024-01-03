/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.framework.server.monitor.ConnectionPoolMonitor;
import is.codion.swing.framework.server.monitor.PoolMonitor;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.tabbedPane;
import static java.util.Objects.requireNonNull;

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
    this.model = requireNonNull(model);
    initializeUI();
  }

  private void initializeUI() {
    TabbedPaneBuilder tabbedPaneBuilder = tabbedPane();
    for (ConnectionPoolMonitor monitor : model.connectionPoolInstanceMonitors()) {
      tabbedPaneBuilder.tab(monitor.username(), new ConnectionPoolMonitorPanel(monitor));
    }
    setLayout(new BorderLayout());
    add(tabbedPaneBuilder.build(), BorderLayout.CENTER);
  }
}
