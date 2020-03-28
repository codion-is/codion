/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.TaskScheduler;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.common.ui.value.NumericalValues;
import org.jminor.swing.framework.server.monitor.DatabaseMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

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
  public DatabaseMonitorPanel(final DatabaseMonitor model) {
    this.model = model;
    queriesPerSecondChart.getXYPlot().setDataset(model.getQueriesPerSecondCollection());
    queriesPerSecondChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    queriesPerSecondChart.setBackgroundPaint(this.getBackground());
    initializeUI();
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab("Connection Pools", new PoolMonitorPanel(model.getConnectionPoolMonitor()));
    tabPane.addTab("Performance", getChartPanel());
    add(tabPane, BorderLayout.CENTER);
  }

  private JPanel getChartPanel() {
    final JPanel chartConfig = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    final JSpinner updateIntervalSpinner = new JSpinner(NumericalValues.integerValueSpinnerModel(model.getUpdateScheduler(),
            TaskScheduler.INTERVAL_PROPERTY, model.getUpdateScheduler().getIntervalObserver()));

    ((JSpinner.DefaultEditor) updateIntervalSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) updateIntervalSpinner.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);

    chartConfig.add(new JLabel("Update interval (s)"));
    chartConfig.add(updateIntervalSpinner);

    final JPanel configBase = new JPanel(Layouts.borderLayout());
    configBase.add(chartConfig, BorderLayout.CENTER);
    configBase.add(new JButton(Controls.control(model::resetStatistics, "Reset")), BorderLayout.EAST);

    final JPanel panel = new JPanel(Layouts.borderLayout());
    queriesPerSecondChartPanel.setBorder(BorderFactory.createEtchedBorder());
    panel.add(queriesPerSecondChartPanel, BorderLayout.CENTER);
    panel.add(configBase, BorderLayout.SOUTH);

    return panel;
  }
}
