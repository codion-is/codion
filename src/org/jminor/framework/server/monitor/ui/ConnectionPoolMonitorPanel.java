/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.IntBeanSpinnerValueLink;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.text.NumberFormat;

/**
 * User: Bjorn Darri<br>
 * Date: 4.12.2007<br>
 * Time: 22:49:27<br>
 */
public final class ConnectionPoolMonitorPanel extends JPanel {

  private final ConnectionPoolMonitor model;

  private final NumberFormat format = NumberFormat.getInstance();

  private final JFreeChart inPoolChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart inPoolMacroChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart requestsPerSecondChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart checkOutTimeChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);

  private final ChartPanel inPoolChartPanel = new ChartPanel(inPoolChart);
  private final ChartPanel inPoolChartPanelMacro = new ChartPanel(inPoolMacroChart);
  private final ChartPanel requestsPerSecondChartPanel = new ChartPanel(requestsPerSecondChart);
  private final ChartPanel checkOutTimePanel = new ChartPanel(checkOutTimeChart);

  private final JTextField txtPoolSize = new JTextField();
  private final JTextField txtCreated = new JTextField();
  private final JTextField txtDestroyed = new JTextField();
  private final JTextField txtCreatedDestroyedResetTime = new JTextField(14);
  private final JTextField txtRequested = new JTextField();
  private final JTextField txtDelayed = new JTextField();

  public ConnectionPoolMonitorPanel(final ConnectionPoolMonitor model) throws RemoteException {
    this.model = model;
    format.setMaximumFractionDigits(2);
    initUI();
    updateView();
    bindEvents();
  }

  public void updateView() {
    final ConnectionPoolStatistics stats = model.getConnectionPoolStats();
    txtPoolSize.setText(format.format(stats.getLiveConnectionCount()));
    txtCreated.setText(format.format(stats.getConnectionsCreated()));
    txtDestroyed.setText(format.format(stats.getConnectionsDestroyed()));
    txtCreatedDestroyedResetTime.setText(DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(stats.getResetDate()));
    txtRequested.setText(format.format(stats.getConnectionRequests()));
    final double prc = (double) stats.getConnectionRequestsDelayed() / (double) stats.getConnectionRequests() * 100;
    txtDelayed.setText(format.format(stats.getConnectionRequestsDelayed())
            + (prc > 0 ? " (" + format.format(prc)+"%)" : ""));
    if (model.datasetContainsData()) {
      inPoolChart.getXYPlot().setDataset(model.getInPoolDataSet());
    }
  }

  private void initUI() {
    initializeCharts(model);
    setLayout(new BorderLayout(5,5));

    final JPanel statusBase = new JPanel(new BorderLayout(5,5));
    statusBase.add(getStatsPanel(), BorderLayout.NORTH);
    statusBase.add(getChartPanel(), BorderLayout.CENTER);

    add(getPoolConfigPanel(), BorderLayout.NORTH);
    add(statusBase, BorderLayout.CENTER);
  }

  private void initializeCharts(final ConnectionPoolMonitor model) {
    inPoolMacroChart.getXYPlot().setDataset(model.getInPoolDataSetMacro());
    inPoolMacroChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) inPoolMacroChart.getXYPlot().getRenderer();
    renderer.setSeriesPaint(0, Color.RED);
    renderer.setSeriesPaint(1, Color.BLUE);
    renderer.setSeriesPaint(2, Color.PINK);
    renderer.setSeriesPaint(3, Color.GREEN);
    renderer.setSeriesPaint(4, Color.MAGENTA);
    requestsPerSecondChart.getXYPlot().setDataset(model.getRequestsPerSecondDataSet());
    requestsPerSecondChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    inPoolChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    checkOutTimeChart.getXYPlot().setDataset(model.getCheckOutTimeCollection());
    checkOutTimeChart.getXYPlot().setBackgroundPaint(Color.BLACK);
  }

  private void bindEvents() {
    model.eventStatsUpdated().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateView();
      }
    });
  }

  private JPanel getPoolConfigPanel() {
    final JPanel configBase = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));

    final JSpinner spnTimeout = new JSpinner(new IntBeanSpinnerValueLink(model, "pooledConnectionTimeout", null).getSpinnerModel());
    final JSpinner spnCleanupInterval = new JSpinner(new IntBeanSpinnerValueLink(model, "poolCleanupInterval", null).getSpinnerModel());
    final JSpinner spnMaximumSize = new JSpinner(new IntBeanSpinnerValueLink(model, "maximumPoolSize", null).getSpinnerModel());
    final JSpinner spnMinimumSize = new JSpinner(new IntBeanSpinnerValueLink(model, "minimumPoolSize", null).getSpinnerModel());

    ((JSpinner.DefaultEditor) spnTimeout.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnCleanupInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnMinimumSize.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnMaximumSize.getEditor()).getTextField().setEditable(false);

    ((JSpinner.DefaultEditor) spnTimeout.getEditor()).getTextField().setColumns(3);
    ((JSpinner.DefaultEditor) spnCleanupInterval.getEditor()).getTextField().setColumns(3);
    ((JSpinner.DefaultEditor) spnMinimumSize.getEditor()).getTextField().setColumns(3);
    ((JSpinner.DefaultEditor) spnMaximumSize.getEditor()).getTextField().setColumns(3);

    txtPoolSize.setEditable(false);
    txtPoolSize.setColumns(3);
    txtPoolSize.setHorizontalAlignment(JLabel.CENTER);

    configBase.add(new JLabel("Pool size"));
    configBase.add(txtPoolSize);
    configBase.add(new JLabel("Minimum size"));
    configBase.add(spnMinimumSize);
    configBase.add(new JLabel("Maximum size"));
    configBase.add(spnMaximumSize);
    configBase.add(new JLabel("Idle timeout (s)"));
    configBase.add(spnTimeout);
    configBase.add(new JLabel("Cleanup interval (s)"));
    configBase.add(spnCleanupInterval);

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
    panel.add(configBase, BorderLayout.CENTER);

    return panel;
  }

  private JPanel getStatsPanel() {
    final JPanel statsBase = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    txtCreated.setEditable(false);
    txtCreated.setHorizontalAlignment(JLabel.CENTER);
    txtDestroyed.setEditable(false);
    txtDestroyed.setHorizontalAlignment(JLabel.CENTER);
    txtRequested.setEditable(false);
    txtRequested.setHorizontalAlignment(JLabel.CENTER);
    txtDelayed.setEditable(false);
    txtDelayed.setHorizontalAlignment(JLabel.CENTER);
    txtCreatedDestroyedResetTime.setEditable(false);
    txtCreatedDestroyedResetTime.setHorizontalAlignment(JLabel.CENTER);

    final JCheckBox chkCollectStats = new JCheckBox();
    chkCollectStats.setModel(new ToggleBeanValueLink(model, "collectFineGrainedStats", model.eventCollectFineGrainedStatsChanged(), null).getButtonModel());

    statsBase.add(new JLabel("Fine grained statistics"));
    statsBase.add(chkCollectStats);
    statsBase.add(new JLabel("Connections requested"));
    statsBase.add(txtRequested);
    statsBase.add(new JLabel("delayed"));
    statsBase.add(txtDelayed);
    statsBase.add(new JLabel("created"));
    statsBase.add(txtCreated);
    statsBase.add(new JLabel("destroyed"));
    statsBase.add(txtDestroyed);
    statsBase.add(new JLabel(" since"));
    statsBase.add(txtCreatedDestroyedResetTime);

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
    panel.add(statsBase, BorderLayout.CENTER);
    panel.add(ControlProvider.createButton(
            ControlFactory.methodControl(model, "resetStats", "Reset")), BorderLayout.EAST);

    return panel;
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
            ControlFactory.methodControl(model, "resetInPoolStats", "Reset")), BorderLayout.EAST);

    final JPanel chartBase = new JPanel(new GridLayout(2,2));
    chartBase.add(requestsPerSecondChartPanel);
    chartBase.add(inPoolChartPanelMacro);
    chartBase.add(inPoolChartPanel);
    chartBase.add(checkOutTimePanel);
    chartBase.setBorder(BorderFactory.createEtchedBorder());

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    panel.setBorder(BorderFactory.createTitledBorder("Status"));
    panel.add(chartBase, BorderLayout.CENTER);
    panel.add(configBase, BorderLayout.NORTH);

    return panel;
  }
}
