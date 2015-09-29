/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.control.ControlProvider;
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

  private final JTextField txtResetTime = new JTextField(RESET_FIELD_COLUMNS);
  private final JTextField txtPoolSize = new JTextField();
  private final JTextField txtCreated = new JTextField();
  private final JTextField txtDestroyed = new JTextField();
  private final JTextField txtRequested = new JTextField();
  private final JTextField txtDelayed = new JTextField();
  private final JTextField txtFailed = new JTextField();

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
    txtPoolSize.setText(format.format(statistics.getSize()));
    txtCreated.setText(format.format(statistics.getCreated()));
    txtDestroyed.setText(format.format(statistics.getDestroyed()));
    txtResetTime.setText(DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(statistics.getResetTime()));
    txtRequested.setText(format.format(statistics.getRequests()));
    double prc = (double) statistics.getDelayedRequests() / (double) statistics.getRequests() * HUNDRED;
    txtDelayed.setText(format.format(statistics.getDelayedRequests())
            + (prc > 0 ? " (" + format.format(prc)+"%)" : ""));
    prc = (double) statistics.getFailedRequests() / (double) statistics.getRequests() * HUNDRED;
    txtFailed.setText(format.format(statistics.getFailedRequests())
            + (prc > 0 ? " (" + format.format(prc)+"%)" : ""));
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
    devRenderer.setBaseShapesVisible(false);
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
    model.getStatisticsObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        updateView();
      }
    });
  }

  private JPanel getConfigurationPanel() {
    final JPanel configBase = new JPanel(UiUtil.createGridLayout(0, 1));

    final JSpinner spnTimeout = new JSpinner(ValueLinks.intSpinnerValueLink(model, "pooledConnectionTimeout", null));
    final JSpinner spnCleanupInterval = new JSpinner(ValueLinks.intSpinnerValueLink(model, "poolCleanupInterval", null));
    final JSpinner spnMaximumSize = new JSpinner(ValueLinks.intSpinnerValueLink(model, "maximumPoolSize", null));
    final JSpinner spnMinimumSize = new JSpinner(ValueLinks.intSpinnerValueLink(model, "minimumPoolSize", null));
    final JSpinner spnMaximumRetryWait = new JSpinner(ValueLinks.intSpinnerValueLink(model, "maximumRetryWaitPeriod", null));
    final JSpinner spnMaximumCheckOutTime = new JSpinner(ValueLinks.intSpinnerValueLink(model, "maximumCheckOutTime", null));
    final JSpinner spnNewConnectionThreshold = new JSpinner(ValueLinks.intSpinnerValueLink(model, "newConnectionThreshold", null));

    ((JSpinner.DefaultEditor) spnTimeout.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnCleanupInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnMinimumSize.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnMaximumSize.getEditor()).getTextField().setEditable(false);

    configBase.add(UiUtil.northCenterPanel(new JLabel("Min size"), spnMinimumSize));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max size"), spnMaximumSize));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max retry wait (ms)"), spnMaximumRetryWait));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max check out time (ms)"), spnMaximumCheckOutTime));
    configBase.add(UiUtil.northCenterPanel(new JLabel("New conn. threshold (ms)"), spnNewConnectionThreshold));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Idle timeout (s)"), spnTimeout));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Cleanup interval (s)"), spnCleanupInterval));

    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
    panel.add(configBase, BorderLayout.NORTH);

    return panel;
  }

  private JPanel getStatisticsPanel() {
    final JPanel statisticsBase = new JPanel(UiUtil.createGridLayout(0, 1));
    txtPoolSize.setEditable(false);
    txtPoolSize.setHorizontalAlignment(JLabel.CENTER);
    txtCreated.setEditable(false);
    txtCreated.setHorizontalAlignment(JLabel.CENTER);
    txtDestroyed.setEditable(false);
    txtDestroyed.setHorizontalAlignment(JLabel.CENTER);
    txtRequested.setEditable(false);
    txtRequested.setHorizontalAlignment(JLabel.CENTER);
    txtDelayed.setEditable(false);
    txtDelayed.setHorizontalAlignment(JLabel.CENTER);
    txtFailed.setEditable(false);
    txtFailed.setHorizontalAlignment(JLabel.CENTER);
    txtResetTime.setEditable(false);
    txtResetTime.setHorizontalAlignment(JLabel.CENTER);

    final JButton btnReset = ControlProvider.createButton(Controls.methodControl(model, "resetStatistics", "Reset"));
    btnReset.setMaximumSize(UiUtil.getPreferredTextFieldSize());

    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Pool size"), txtPoolSize));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Connections requested"), txtRequested));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Delayed requests"), txtDelayed));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Failed requests"), txtFailed));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Connections created"), txtCreated));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("Connections destroyed"), txtDestroyed));
    statisticsBase.add(UiUtil.northCenterPanel(new JLabel("since"), txtResetTime));

    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
    panel.add(statisticsBase, BorderLayout.NORTH);
    panel.add(btnReset, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel getChartPanel() {
    final JPanel chartConfig = new JPanel(UiUtil.createFlexibleGridLayout(1, 3, true, false));
    final JSpinner spnUpdateInterval = new JSpinner(ValueLinks.intSpinnerValueLink(model.getUpdateScheduler(),
            TaskScheduler.INTERVAL_PROPERTY, model.getUpdateScheduler().getIntervalObserver()));

    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);

    chartConfig.add(new JLabel("Update interval (s)"));
    chartConfig.add(spnUpdateInterval);

    final JCheckBox chkCollectStatistics = new JCheckBox("Fine grained statistics");
    chkCollectStatistics.setModel(ValueLinks.toggleValueLink(model, "collectFineGrainedStatistics",
            model.getCollectFineGrainedStatisticsObserver()));
    chkCollectStatistics.setMaximumSize(UiUtil.getPreferredTextFieldSize());

    chartConfig.add(chkCollectStatistics);

    final JPanel configBase = new JPanel(UiUtil.createBorderLayout());
    configBase.add(chartConfig, BorderLayout.WEST);
    final JButton btnReset = ControlProvider.createButton(
            Controls.methodControl(model, "resetInPoolStatistics", "Reset"));
    btnReset.setMaximumSize(UiUtil.getPreferredTextFieldSize());
    configBase.add(btnReset, BorderLayout.EAST);

    final JPanel chartBase = new JPanel(new GridLayout(2,2));
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
