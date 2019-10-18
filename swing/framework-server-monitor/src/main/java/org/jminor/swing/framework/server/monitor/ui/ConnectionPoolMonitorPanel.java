/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.DateFormats;
import org.jminor.common.TaskScheduler;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.server.monitor.ConnectionPoolMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A ConnectionPoolMonitorPanel
 */
public final class ConnectionPoolMonitorPanel extends JPanel {

  private static final int RESET_FIELD_COLUMNS = 14;
  private static final int HUNDRED = 100;
  private static final int SPINNER_COLUMNS = 3;
  private static final int MAIN_LAYOUT_COLUMNS = 3;

  private final ConnectionPoolMonitor model;

  private final NumberFormat format = NumberFormat.getInstance();
  private final JFreeChart inPoolFineGrainedChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart inPoolChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart requestsPerSecondChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel inPoolFineGrainedChartPanel = new ChartPanel(inPoolFineGrainedChart);
  private final ChartPanel inPoolChartPanel = new ChartPanel(inPoolChart);
  private final ChartPanel requestsPerSecondChartPanel = new ChartPanel(requestsPerSecondChart);

  private ChartPanel checkOutTimePanel;

  private final JTextField resetTimeField = new JTextField(RESET_FIELD_COLUMNS);
  private final JTextField poolSizeField = new JTextField();
  private final JTextField createdField = new JTextField();
  private final JTextField destroyedField = new JTextField();
  private final JTextField requestedField = new JTextField();
  private final JTextField delayedField = new JTextField();
  private final JTextField failedField = new JTextField();

  /**
   * Instantiates a new ConnectionPoolMonitorPanel
   * @param model the ConnectionPoolMonitor to base this panel on
   */
  public ConnectionPoolMonitorPanel(final ConnectionPoolMonitor model) {
    this.model = model;
    this.format.setMaximumFractionDigits(2);
    initializeUI();
    updateView();
    bindEvents();
  }

  private void updateView() {
    final ConnectionPoolStatistics statistics = model.getConnectionPoolStatistics();
    poolSizeField.setText(format.format(statistics.getSize()));
    createdField.setText(format.format(statistics.getCreated()));
    destroyedField.setText(format.format(statistics.getDestroyed()));
    resetTimeField.setText(DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP)
            .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(statistics.getResetTime()), ZoneId.systemDefault())));
    requestedField.setText(format.format(statistics.getRequests()));
    double prc = (double) statistics.getDelayedRequests() / (double) statistics.getRequests() * HUNDRED;
    delayedField.setText(format.format(statistics.getDelayedRequests())
            + (prc > 0 ? " (" + format.format(prc) + "%)" : ""));
    prc = (double) statistics.getFailedRequests() / (double) statistics.getRequests() * HUNDRED;
    failedField.setText(format.format(statistics.getFailedRequests())
            + (prc > 0 ? " (" + format.format(prc) + "%)" : ""));
    if (model.datasetContainsData()) {
      inPoolFineGrainedChart.getXYPlot().setDataset(model.getFineGrainedInPoolDataset());
    }
  }

  private void initializeUI() {
    initializeCharts(model);
    setLayout(UiUtil.createFlexibleGridLayout(1, MAIN_LAYOUT_COLUMNS, true, false));

    add(getChartPanel());
    add(getStatisticsPanel());
    add(getConfigurationPanel());
  }

  private void initializeCharts(final ConnectionPoolMonitor model) {
    final JFreeChart checkOutTimeChart = ChartFactory.createXYStepChart(null,
            null, null, model.getCheckOutTimeCollection(), PlotOrientation.VERTICAL, true, true, false);
    setColors(checkOutTimeChart);
    checkOutTimePanel = new ChartPanel(checkOutTimeChart);
    checkOutTimePanel.setBorder(BorderFactory.createEtchedBorder());

    final DeviationRenderer devRenderer = new DeviationRenderer();
    devRenderer.setDefaultShapesVisible(false);
    checkOutTimeChart.getXYPlot().setRenderer(devRenderer);

    inPoolChart.getXYPlot().setDataset(model.getInPoolDataset());
    final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) inPoolChart.getXYPlot().getRenderer();
    renderer.setSeriesPaint(0, Color.RED);
    renderer.setSeriesPaint(1, Color.BLUE);
    renderer.setSeriesPaint(2, Color.PINK);
    renderer.setSeriesPaint(3, Color.GREEN);
    renderer.setSeriesPaint(4, Color.MAGENTA);
    requestsPerSecondChart.getXYPlot().setDataset(model.getRequestsPerSecondDataset());
    checkOutTimeChart.getXYPlot().setDataset(model.getCheckOutTimeCollection());
    setColors(inPoolFineGrainedChart);
    setColors(inPoolChart);
    setColors(requestsPerSecondChart);
    setColors(checkOutTimeChart);
  }

  private void setColors(final JFreeChart chart) {
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    chart.setBackgroundPaint(this.getBackground());
  }

  private void bindEvents() {
    model.getStatisticsObserver().addListener(this::updateView);
  }

  private JPanel getConfigurationPanel() {
    final JPanel configBase = new JPanel(UiUtil.createGridLayout(0, 1));

    final JSpinner timeoutSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model, "pooledConnectionTimeout",
            model.getStatisticsObserver()));
    final JSpinner cleanupIntervalSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model, "poolCleanupInterval",
            model.getStatisticsObserver()));
    final JSpinner maximumSizeSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model, "maximumPoolSize",
            model.getStatisticsObserver()));
    final JSpinner minimumSizeSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model, "minimumPoolSize",
            model.getStatisticsObserver()));
    final JSpinner maximumRetryWaitSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model, "maximumRetryWaitPeriod",
            model.getStatisticsObserver()));
    final JSpinner maximumCheckOutTimeSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model, "maximumCheckOutTime",
            model.getStatisticsObserver()));
    final JSpinner newConnectionThresholdSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model, "newConnectionThreshold",
            model.getStatisticsObserver()));

    ((JSpinner.DefaultEditor) timeoutSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) cleanupIntervalSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) minimumSizeSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) maximumSizeSpinner.getEditor()).getTextField().setEditable(false);

    configBase.add(UiUtil.northCenterPanel(new JLabel("Min size"), minimumSizeSpinner));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max size"), maximumSizeSpinner));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max retry wait (ms)"), maximumRetryWaitSpinner));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max check out time (ms)"), maximumCheckOutTimeSpinner));
    configBase.add(UiUtil.northCenterPanel(new JLabel("New conn. threshold (ms)"), newConnectionThresholdSpinner));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Idle timeout (s)"), timeoutSpinner));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Cleanup interval (s)"), cleanupIntervalSpinner));

    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
    panel.add(configBase, BorderLayout.NORTH);

    return panel;
  }

  private JPanel getStatisticsPanel() {
    final JPanel statisticsBase = new JPanel(UiUtil.createGridLayout(0, 1));
    poolSizeField.setEditable(false);
    poolSizeField.setHorizontalAlignment(JLabel.CENTER);
    createdField.setEditable(false);
    createdField.setHorizontalAlignment(JLabel.CENTER);
    destroyedField.setEditable(false);
    destroyedField.setHorizontalAlignment(JLabel.CENTER);
    requestedField.setEditable(false);
    requestedField.setHorizontalAlignment(JLabel.CENTER);
    delayedField.setEditable(false);
    delayedField.setHorizontalAlignment(JLabel.CENTER);
    failedField.setEditable(false);
    failedField.setHorizontalAlignment(JLabel.CENTER);
    resetTimeField.setEditable(false);
    resetTimeField.setHorizontalAlignment(JLabel.CENTER);

    final JButton resetButton = new JButton(Controls.control(model::resetStatistics, "Reset"));
    resetButton.setMaximumSize(UiUtil.getPreferredTextFieldSize());

    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Pool size"), poolSizeField));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Connections requested"), requestedField));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Delayed requests"), delayedField));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Failed requests"), failedField));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Connections created"), createdField));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Connections destroyed"), destroyedField));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("since"), resetTimeField));

    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
    panel.add(statisticsBase, BorderLayout.NORTH);
    panel.add(resetButton, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel getChartPanel() {
    final JPanel chartConfig = new JPanel(UiUtil.createFlexibleGridLayout(1, 3, true, false));
    final JSpinner updateIntervalSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model.getUpdateScheduler(),
            TaskScheduler.INTERVAL_PROPERTY, model.getUpdateScheduler().getIntervalObserver()));

    ((JSpinner.DefaultEditor) updateIntervalSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) updateIntervalSpinner.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);

    chartConfig.add(new JLabel("Update interval (s)"));
    chartConfig.add(updateIntervalSpinner);

    final JCheckBox collectStatisticsCheckBox = new JCheckBox("Fine grained statistics");
    collectStatisticsCheckBox.setModel(ValueLinks.toggleValueLink(model, "collectFineGrainedStatistics",
            model.getCollectFineGrainedStatisticsObserver()));
    collectStatisticsCheckBox.setMaximumSize(UiUtil.getPreferredTextFieldSize());

    chartConfig.add(collectStatisticsCheckBox);

    final JPanel configBase = new JPanel(UiUtil.createBorderLayout());
    configBase.add(chartConfig, BorderLayout.WEST);
    final JButton resetButton = new JButton(Controls.control(model::resetInPoolStatistics, "Reset"));
    resetButton.setMaximumSize(UiUtil.getPreferredTextFieldSize());
    configBase.add(resetButton, BorderLayout.EAST);

    final JPanel chartBase = new JPanel(new GridLayout(2, 2));
    chartBase.add(requestsPerSecondChartPanel);
    chartBase.add(inPoolChartPanel);
    chartBase.add(checkOutTimePanel);
    chartBase.add(inPoolFineGrainedChartPanel);
    chartBase.setBorder(BorderFactory.createEtchedBorder());

    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Status"));
    panel.add(chartBase, BorderLayout.CENTER);
    panel.add(configBase, BorderLayout.SOUTH);

    return panel;
  }
}
