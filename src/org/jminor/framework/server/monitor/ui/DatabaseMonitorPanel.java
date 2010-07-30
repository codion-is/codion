/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.control.IntBeanSpinnerValueLink;
import org.jminor.framework.server.monitor.DatabaseMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri<br>
 */
public final class DatabaseMonitorPanel extends JPanel {

  private final DatabaseMonitor model;

  private final JFreeChart queriesPerSecondChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);

  private final ChartPanel queriesPerSecondChartPanel = new ChartPanel(queriesPerSecondChart);

  public DatabaseMonitorPanel(final DatabaseMonitor model) throws RemoteException {
    this.model = model;
    queriesPerSecondChart.getXYPlot().setDataset(model.getQueriesPerSecondCollection());
    queriesPerSecondChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    initUI();
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane();
    tabPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    tabPane.addTab("Performance", getChartPanel());
    tabPane.addTab("Connection Pools", new PoolMonitorPanel(model.getConnectionPoolMonitor()));
    add(tabPane, BorderLayout.CENTER);
  }

  private JPanel getChartPanel() {
    final JPanel chartConfig = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    final JSpinner spnUpdateInterval = new JSpinner(new IntBeanSpinnerValueLink(model, "statsUpdateInterval",
            model.eventStatsUpdateIntervalChanged()).getSpinnerModel());

    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setColumns(3);

    chartConfig.add(new JLabel("Update interval (s)"));
    chartConfig.add(spnUpdateInterval);

    final JPanel configBase = new JPanel(new BorderLayout(5,5));
    configBase.add(chartConfig, BorderLayout.CENTER);
    configBase.add(ControlProvider.createButton(
            Controls.methodControl(model, "resetStats", "Reset")), BorderLayout.EAST);

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    queriesPerSecondChartPanel.setBorder(BorderFactory.createEtchedBorder());
    panel.add(queriesPerSecondChartPanel, BorderLayout.CENTER);
    panel.add(configBase, BorderLayout.NORTH);

    return panel;
  }
}
