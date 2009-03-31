/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.model.formats.FullDateFormat;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.IntBeanSpinnerPropertyLink;
import org.jminor.framework.server.monitor.ConnectionPoolInstanceMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import javax.swing.BorderFactory;
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
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 22:49:27
 */
public class ConnectionPoolInstanceMonitorPanel extends JPanel {

  private final ConnectionPoolInstanceMonitor model;

  private final NumberFormat format = NumberFormat.getInstance();

  private final JFreeChart inPoolChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart inPoolMacroChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart requestsPerSecondChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);

  private final ChartPanel inPoolChartPanel = new ChartPanel(inPoolChart);
  private final ChartPanel inPoolChartPanelMacro = new ChartPanel(inPoolMacroChart);
  private final ChartPanel requestsPerSecondChartPanel = new ChartPanel(requestsPerSecondChart);

  private final JTextField txtPoolSize = new JTextField();
  private final JTextField txtCreated = new JTextField();
  private final JTextField txtDestroyed = new JTextField();
  private final JTextField txtCreatedDestroyedResetTime = new JTextField(14);
  private final JTextField txtRequested = new JTextField();
  private final JTextField txtDelayed = new JTextField();

  public ConnectionPoolInstanceMonitorPanel(final ConnectionPoolInstanceMonitor model) throws RemoteException {
    this.model = model;
    format.setMaximumFractionDigits(2);
    initUI();
    updateView();
    bindEvents();
  }

  public void updateView() {
    final ConnectionPoolSettings settings = model.getConnectionPoolStats();
    txtPoolSize.setText(format.format(settings.getLiveConnectionCount()));
    txtCreated.setText(format.format(settings.getConnectionsCreated()));
    txtDestroyed.setText(format.format(settings.getConnectionsDestroyed()));
    txtCreatedDestroyedResetTime.setText(FullDateFormat.get().format(settings.getResetDate()));
    txtRequested.setText(format.format(settings.getConnectionRequests()));
    final double prc = (double) settings.getConnectionRequestsDelayed()/(double) settings.getConnectionRequests()*100;
    txtDelayed.setText(format.format(settings.getConnectionRequestsDelayed())
            + (prc > 0 ? " (" + format.format(prc)+"%)" : ""));
    if (model.datasetContainsData())
      inPoolChart.getXYPlot().setDataset(model.getInPoolDataSet());
  }

  private void initUI() {
    initializeCharts(model);
    setLayout(new BorderLayout(5,5));
    add(getPoolConfigPanel(), BorderLayout.NORTH);
    final JPanel statusBase = new JPanel(new BorderLayout(5,5));
    statusBase.add(getStatsPanel(), BorderLayout.NORTH);
    statusBase.add(getChartPanel(), BorderLayout.CENTER);
    add(statusBase, BorderLayout.CENTER);
  }

  private void initializeCharts(ConnectionPoolInstanceMonitor model) {
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
  }

  private void bindEvents() {
    model.evtStatsUpdated.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateView();
      }
    });
  }

  private JPanel getPoolConfigPanel() {
    final JPanel configBase = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));

    final JSpinner spnTimeout = new JSpinner(new IntBeanSpinnerPropertyLink(model, "pooledConnectionTimout", null, null).getSpinnerModel());
    final JSpinner spnMaximumSize = new JSpinner(new IntBeanSpinnerPropertyLink(model, "maximumPoolSize", null, null).getSpinnerModel());
    final JSpinner spnMinimumSize = new JSpinner(new IntBeanSpinnerPropertyLink(model, "minimumPoolSize", null, null).getSpinnerModel());

    ((JSpinner.DefaultEditor) spnTimeout.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnMinimumSize.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnMaximumSize.getEditor()).getTextField().setEditable(false);

    ((JSpinner.DefaultEditor) spnTimeout.getEditor()).getTextField().setColumns(3);
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
    configBase.add(new JLabel("Idle timout (s)"));
    configBase.add(spnTimeout);

    final JPanel ret = new JPanel(new BorderLayout(5,5));
    ret.setBorder(BorderFactory.createTitledBorder("Connection pool configuration"));
    ret.add(configBase, BorderLayout.CENTER);

    return ret;
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

    final JPanel ret = new JPanel(new BorderLayout(5,5));
    ret.setBorder(BorderFactory.createTitledBorder("Connection pool statistics"));
    ret.add(statsBase, BorderLayout.CENTER);
    ret.add(ControlProvider.createButton(
            ControlFactory.methodControl(model, "resetStats", "Reset")), BorderLayout.EAST);

    return ret;
  }

  private JPanel getChartPanel() {
    final JPanel chartConfig = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    final JSpinner spnUpdateInterval = new JSpinner(new IntBeanSpinnerPropertyLink(model, "statsUpdateInterval",
            model.evtStatsUpdateIntervalChanged, null).getSpinnerModel());

    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setColumns(3);

    chartConfig.add(new JLabel("Update interval (s)"));
    chartConfig.add(spnUpdateInterval);

    final JPanel configBase = new JPanel(new BorderLayout(5,5));
    configBase.add(chartConfig, BorderLayout.CENTER);
    configBase.add(ControlProvider.createButton(
            ControlFactory.methodControl(model, "resetInPoolStats", "Reset")), BorderLayout.EAST);

    final JPanel chartBase = new JPanel(new GridLayout(3,1));
    chartBase.add(requestsPerSecondChartPanel);
    chartBase.add(inPoolChartPanelMacro);
    chartBase.add(inPoolChartPanel);

    final JPanel ret = new JPanel(new BorderLayout(5,5));
    ret.setBorder(BorderFactory.createTitledBorder("Connection pool status"));
    ret.add(chartBase, BorderLayout.CENTER);
    ret.add(configBase, BorderLayout.NORTH);

    return ret;
  }
}
