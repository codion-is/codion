/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.server.monitor.DatabaseMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.control.Control.control;

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
    this.queriesPerSecondChart.getXYPlot().setDataset(model.getQueriesPerSecondCollection());
    ChartUtil.linkColors(this, queriesPerSecondChart);
    initializeUI();
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab("Connection Pools", new PoolMonitorPanel(model.getConnectionPoolMonitor()));
    tabPane.addTab("Performance", getChartPanel());
    add(tabPane, BorderLayout.CENTER);
  }

  private JPanel getChartPanel() {
    JPanel chartConfig = new JPanel(Layouts.flexibleGridLayout(1, 3));
    chartConfig.setBorder(BorderFactory.createTitledBorder("Charts"));
    chartConfig.add(new JLabel("Update interval (s)"));
    chartConfig.add(Components.integerSpinner(model.getUpdateIntervalValue())
            .minimum(1)
            .columns(SPINNER_COLUMNS)
            .editable(false)
            .build());
    chartConfig.add(Components.button(control(model::clearStatistics))
            .caption("Clear")
            .build());

    JPanel configBase = new JPanel(Layouts.borderLayout());
    configBase.add(chartConfig, BorderLayout.WEST);

    JPanel panel = new JPanel(Layouts.borderLayout());
    queriesPerSecondChartPanel.setBorder(BorderFactory.createEtchedBorder());
    panel.add(queriesPerSecondChartPanel, BorderLayout.CENTER);
    panel.add(configBase, BorderLayout.SOUTH);

    return panel;
  }
}
