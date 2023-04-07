/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
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
    this.model = model;
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
    JPanel chartConfig = panel(flexibleGridLayout(1, 3))
            .border(createTitledBorder("Charts"))
            .add(new JLabel("Update interval (s)"))
            .add(integerSpinner(model.updateIntervalValue())
                    .minimum(1)
                    .columns(SPINNER_COLUMNS)
                    .editable(false)
                    .build())
            .add(button(control(model::clearStatistics))
                    .caption("Clear")
                    .build())
            .build();

    queriesPerSecondChartPanel.setBorder(createEtchedBorder());

    return panel(borderLayout())
            .add(queriesPerSecondChartPanel, BorderLayout.CENTER)
            .add(panel(borderLayout())
                    .add(chartConfig, BorderLayout.WEST)
                    .build(), BorderLayout.SOUTH)
            .build();
  }
}
