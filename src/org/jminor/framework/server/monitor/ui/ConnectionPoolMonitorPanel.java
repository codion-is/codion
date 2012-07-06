/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.control.IntBeanSpinnerValueLink;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;

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

  private final ConnectionPoolMonitor model;

  private static final int RESET_FIELD_COLUMNS = 14;

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
    initUI();
    updateView();
    bindEvents();
  }

  public void updateView() {
    final ConnectionPoolStatistics statistics = model.getConnectionPoolStatistics();
    txtPoolSize.setText(format.format(statistics.getSize()));
    txtCreated.setText(format.format(statistics.getCreated()));
    txtDestroyed.setText(format.format(statistics.getDestroyed()));
    txtResetTime.setText(DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(statistics.getResetTime()));
    txtRequested.setText(format.format(statistics.getRequests()));
    double prc = (double) statistics.getDelayedRequests() / (double) statistics.getRequests() * 100;
    txtDelayed.setText(format.format(statistics.getDelayedRequests())
            + (prc > 0 ? " (" + format.format(prc)+"%)" : ""));
    prc = (double) statistics.getFailedRequests() / (double) statistics.getRequests() * 100;
    txtFailed.setText(format.format(statistics.getFailedRequests())
            + (prc > 0 ? " (" + format.format(prc)+"%)" : ""));
    if (model.datasetContainsData()) {
      inPoolFineGrainedChart.getXYPlot().setDataset(model.getFineGrainedInPoolDataset());
    }
  }

  private void initUI() {
    initializeCharts(model);
    setLayout(new FlexibleGridLayout(1, 3, 5, 5, true, false));

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
    model.addStatisticsListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        updateView();
      }
    });
  }

  private JPanel getConfigurationPanel() {
    final JPanel configBase = new JPanel(new GridLayout(0, 1, 5, 5));

    final JSpinner spnTimeout = new JSpinner(new IntBeanSpinnerValueLink(model, "pooledConnectionTimeout", null).getSpinnerModel());
    final JSpinner spnCleanupInterval = new JSpinner(new IntBeanSpinnerValueLink(model, "poolCleanupInterval", null).getSpinnerModel());
    final JSpinner spnMaximumSize = new JSpinner(new IntBeanSpinnerValueLink(model, "maximumPoolSize", null).getSpinnerModel());
    final JSpinner spnMinimumSize = new JSpinner(new IntBeanSpinnerValueLink(model, "minimumPoolSize", null).getSpinnerModel());
    final JSpinner spnMaximumRetryWait = new JSpinner(new IntBeanSpinnerValueLink(model, "maximumRetryWaitPeriod", null).getSpinnerModel());
    final JSpinner spnMaximumCheckOutTime = new JSpinner(new IntBeanSpinnerValueLink(model, "maximumCheckOutTime", null).getSpinnerModel());
    final JSpinner spnNewConnectionThreshold = new JSpinner(new IntBeanSpinnerValueLink(model, "newConnectionThreshold", null).getSpinnerModel());

    ((JSpinner.DefaultEditor) spnTimeout.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnCleanupInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnMinimumSize.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnMaximumSize.getEditor()).getTextField().setEditable(false);

    ((JSpinner.DefaultEditor) spnTimeout.getEditor()).getTextField().setColumns(30);

    configBase.add(UiUtil.northCenterPanel(new JLabel("Min size"), spnMinimumSize));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max size"), spnMaximumSize));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max retry wait (ms)"), spnMaximumRetryWait));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Max check out time (ms)"), spnMaximumCheckOutTime));
    configBase.add(UiUtil.northCenterPanel(new JLabel("New conn. threshold (ms)"), spnNewConnectionThreshold));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Idle timeout (s)"), spnTimeout));
    configBase.add(UiUtil.northCenterPanel(new JLabel("Cleanup interval (s)"), spnCleanupInterval));

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
    panel.add(configBase, BorderLayout.NORTH);

    return panel;
  }

  private JPanel getStatisticsPanel() {
    final JPanel statisticsBase = new JPanel(new GridLayout(0, 1, 5, 5));
    txtPoolSize.setEditable(false);
    txtPoolSize.setColumns(30);
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

    final JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
    panel.add(statisticsBase, BorderLayout.NORTH);
    panel.add(btnReset, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel getChartPanel() {
    final JPanel chartConfig = new JPanel(new GridLayout(1, 2, 5, 5));
    final JSpinner spnUpdateInterval = new JSpinner(new IntBeanSpinnerValueLink(model, "statisticsUpdateInterval",
            model.getStatisticsUpdateIntervalObserver()).getSpinnerModel());

    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setColumns(3);

    chartConfig.add(new JLabel("Update interval (s)"));
    chartConfig.add(spnUpdateInterval);

    final JPanel configBase = new JPanel(new BorderLayout(5,5));
    configBase.add(chartConfig, BorderLayout.WEST);
    final JButton btnReset = ControlProvider.createButton(
            Controls.methodControl(model, "resetInPoolStatistics", "Reset"));
    btnReset.setMaximumSize(UiUtil.getPreferredTextFieldSize());
    configBase.add(btnReset, BorderLayout.EAST);

    final JCheckBox chkCollectStatistics = new JCheckBox("Fine grained statistics");
    chkCollectStatistics.setModel(new ToggleBeanValueLink(model, "collectFineGrainedStatistics", model.getCollectFineGrainedStatisticsObserver(), null).getButtonModel());
    chkCollectStatistics.setMaximumSize(UiUtil.getPreferredTextFieldSize());

    final JPanel inPoolBase = new JPanel(new BorderLayout(5, 5));
    final JPanel checkBoxBase = new JPanel(new BorderLayout(5, 5));
    checkBoxBase.add(chkCollectStatistics, BorderLayout.EAST);
    inPoolBase.add(inPoolFineGrainedChartPanel, BorderLayout.CENTER);
    inPoolBase.add(checkBoxBase, BorderLayout.NORTH);

    final JPanel chartBase = new JPanel(new GridLayout(2,2));
    chartBase.add(requestsPerSecondChartPanel);
    chartBase.add(inPoolChartPanel);
    chartBase.add(checkOutTimePanel);
    chartBase.add(inPoolBase);
    chartBase.setBorder(BorderFactory.createEtchedBorder());

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    panel.setBorder(BorderFactory.createTitledBorder("Status"));
    panel.add(chartBase, BorderLayout.CENTER);
    panel.add(configBase, BorderLayout.SOUTH);

    return panel;
  }
}
