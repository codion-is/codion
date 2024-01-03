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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.framework.server.monitor.DatabaseMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.control;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEtchedBorder;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A DatabaseMonitorPanel
 */
public final class DatabaseMonitorPanel extends JPanel {

  private static final int SPINNER_COLUMNS = 3;

  private final DatabaseMonitor model;

  private final JFreeChart queriesPerSecondChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);

  private final ChartPanel queriesPerSecondChartPanel = new ChartPanel(queriesPerSecondChart);

  /**
   * Instantiates a new DatabaseMonitorPanel
   * @param model the DatabaseMonitor to base this panel on
   */
  public DatabaseMonitorPanel(DatabaseMonitor model) {
    this.model = requireNonNull(model);
    this.queriesPerSecondChart.getXYPlot().setDataset(model.queriesPerSecondCollection());
    ChartUtil.linkColors(this, queriesPerSecondChart);
    initializeUI();
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    add(tabbedPane()
            .tab("Connection Pools", new PoolMonitorPanel(model.connectionPoolMonitor()))
            .tab("Performance", chartPanel())
            .build(), BorderLayout.CENTER);
  }

  private JPanel chartPanel() {
    JPanel chartConfig = flexibleGridLayoutPanel(1, 3)
            .border(createTitledBorder("Charts"))
            .add(new JLabel("Update interval (s)"))
            .add(integerSpinner(model.updateInterval())
                    .minimum(1)
                    .columns(SPINNER_COLUMNS)
                    .editable(false)
                    .build())
            .add(button(control(model::clearStatistics))
                    .text("Clear")
                    .build())
            .build();

    queriesPerSecondChartPanel.setBorder(createEtchedBorder());

    return borderLayoutPanel()
            .centerComponent(queriesPerSecondChartPanel)
            .southComponent(borderLayoutPanel()
                    .westComponent(chartConfig)
                    .build())
            .build();
  }
}
