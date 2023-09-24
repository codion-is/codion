/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
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
