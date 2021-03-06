/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.DateFormats;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.BooleanValues;
import is.codion.swing.framework.server.monitor.ConnectionPoolMonitor;

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
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.Components.createWestCenterPanel;
import static is.codion.swing.common.ui.value.NumericalValues.integerValueSpinnerModel;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.RIGHT;

/**
 * A ConnectionPoolMonitorPanel
 */
public final class ConnectionPoolMonitorPanel extends JPanel {

  private static final int RESET_FIELD_COLUMNS = 14;
  private static final int HUNDRED = 100;
  private static final int SPINNER_COLUMNS = 3;

  private final ConnectionPoolMonitor model;

  private final NumberFormat format = NumberFormat.getInstance();
  private final JFreeChart inPoolSnapshotChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart inPoolChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final JFreeChart requestsPerSecondChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel inPoolSnapshotChartPanel = new ChartPanel(inPoolSnapshotChart);
  private final ChartPanel inPoolChartPanel = new ChartPanel(inPoolChart);
  private final ChartPanel requestsPerSecondChartPanel = new ChartPanel(requestsPerSecondChart);

  private ChartPanel checkOutTimePanel;

  private final JTextField resetTimeField = new JTextField(RESET_FIELD_COLUMNS);
  private final JTextField poolSizeField = new JTextField();
  private final JTextField createdField = new JTextField();
  private final JTextField destroyedField = new JTextField();
  private final JTextField requestedField = new JTextField();
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
    resetTimeField.setText(DateTimeFormatter.ofPattern(DateFormats.TIMESTAMP_FULL)
            .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(statistics.getResetTime()), ZoneId.systemDefault())));
    requestedField.setText(format.format(statistics.getRequests()));
    final double prc = (double) statistics.getFailedRequests() / (double) statistics.getRequests() * HUNDRED;
    failedField.setText(format.format(statistics.getFailedRequests()) + (prc > 0 ? " (" + format.format(prc) + "%)" : ""));
    if (model.datasetContainsData()) {
      inPoolSnapshotChart.getXYPlot().setDataset(model.getSnapshotDataset());
    }
  }

  private void initializeUI() {
    initializeCharts(model);
    setLayout(Layouts.borderLayout());

    add(getConfigurationPanel(), BorderLayout.NORTH);
    add(getChartPanel(), BorderLayout.CENTER);
    add(getSouthPanel(), BorderLayout.SOUTH);
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
    setColors(inPoolSnapshotChart);
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
    final JPanel configBase = new JPanel(Layouts.flexibleGridLayout(1, 0));

    final JSpinner timeoutSpinner = new JSpinner(integerValueSpinnerModel(model.getPooledConnectionTimeoutValue()));
    final JSpinner cleanupIntervalSpinner = new JSpinner(integerValueSpinnerModel(model.getPoolCleanupIntervalValue()));
    final JSpinner maximumSizeSpinner = new JSpinner(integerValueSpinnerModel(model.getMaximumPoolSizeValue()));
    final JSpinner minimumSizeSpinner = new JSpinner(integerValueSpinnerModel(model.getMinimumPoolSizeValue()));
    final SpinnerNumberModel maximumCheckOutTimeModel = integerValueSpinnerModel(this.model.getMaximumCheckOutTimeValue());
    maximumCheckOutTimeModel.setStepSize(100);
    final JSpinner maximumCheckOutTimeSpinner = new JSpinner(maximumCheckOutTimeModel);

    ((JSpinner.DefaultEditor) timeoutSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) timeoutSpinner.getEditor()).getTextField().setColumns(3);
    ((JSpinner.DefaultEditor) cleanupIntervalSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) cleanupIntervalSpinner.getEditor()).getTextField().setColumns(3);
    ((JSpinner.DefaultEditor) minimumSizeSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) minimumSizeSpinner.getEditor()).getTextField().setColumns(3);
    ((JSpinner.DefaultEditor) maximumSizeSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) maximumSizeSpinner.getEditor()).getTextField().setColumns(3);
    ((JSpinner.DefaultEditor) maximumCheckOutTimeSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) maximumCheckOutTimeSpinner.getEditor()).getTextField().setColumns(6);

    configBase.add(createWestCenterPanel(new JLabel("Mininum size"), minimumSizeSpinner));
    configBase.add(createWestCenterPanel(new JLabel("Maximum size"), maximumSizeSpinner));
    configBase.add(createWestCenterPanel(new JLabel("Checkout timeout (ms)"), maximumCheckOutTimeSpinner));
    configBase.add(createWestCenterPanel(new JLabel("Idle timeout (s)"), timeoutSpinner));
    configBase.add(createWestCenterPanel(new JLabel("Cleanup interval (s)"), cleanupIntervalSpinner));

    configBase.setBorder(BorderFactory.createTitledBorder("Configuration"));

    final JPanel configPanel = new JPanel(Layouts.flowLayout(RIGHT));
    configPanel.add(configBase);

    return configPanel;
  }

  private JPanel getChartPanel() {
    final JPanel chartBase = new JPanel(new GridLayout(2, 2));
    chartBase.add(requestsPerSecondChartPanel);
    chartBase.add(inPoolChartPanel);
    chartBase.add(checkOutTimePanel);
    chartBase.add(inPoolSnapshotChartPanel);
    chartBase.setBorder(BorderFactory.createEtchedBorder());

    return chartBase;
  }

  private JPanel getSouthPanel() {
    final JPanel chartConfig = new JPanel(Layouts.flexibleGridLayout(1, 4));
    chartConfig.setBorder(BorderFactory.createTitledBorder("Charts"));
    final JSpinner updateIntervalSpinner = new JSpinner(integerValueSpinnerModel(model.getUpdateIntervalValue()));
    ((SpinnerNumberModel) updateIntervalSpinner.getModel()).setMinimum(1);

    ((JSpinner.DefaultEditor) updateIntervalSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) updateIntervalSpinner.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);

    chartConfig.add(new JLabel("Update interval (s)"));
    chartConfig.add(updateIntervalSpinner);

    final JCheckBox collectSnapshotCheckBox = new JCheckBox("Snapshot");
    BooleanValues.booleanButtonModelValue(collectSnapshotCheckBox.getModel())
            .link(model.getCollectSnapshotStatisticsState());
    collectSnapshotCheckBox.setMaximumSize(TextFields.getPreferredTextFieldSize());
    chartConfig.add(collectSnapshotCheckBox);

    final JButton clearButton = new JButton(Control.builder()
            .command(model::clearInPoolStatistics)
            .name("Clear")
            .build());
    clearButton.setMaximumSize(TextFields.getPreferredTextFieldSize());
    chartConfig.add(clearButton);

    final JPanel southPanel = new JPanel(Layouts.borderLayout());
    southPanel.add(chartConfig, BorderLayout.WEST);

    southPanel.add(getStatisticsPanel(), BorderLayout.CENTER);

    return southPanel;
  }

  private JPanel getStatisticsPanel() {
    final JPanel statisticsBase = new JPanel(Layouts.flexibleGridLayout(1, 0));
    poolSizeField.setEditable(false);
    poolSizeField.setHorizontalAlignment(CENTER);
    createdField.setEditable(false);
    createdField.setHorizontalAlignment(CENTER);
    destroyedField.setEditable(false);
    destroyedField.setHorizontalAlignment(CENTER);
    requestedField.setEditable(false);
    requestedField.setHorizontalAlignment(CENTER);
    failedField.setEditable(false);
    failedField.setHorizontalAlignment(CENTER);
    resetTimeField.setEditable(false);
    resetTimeField.setHorizontalAlignment(CENTER);

    statisticsBase.add(createWestCenterPanel(new JLabel("Connections"), poolSizeField));
    statisticsBase.add(createWestCenterPanel(new JLabel("Requested"), requestedField));
    statisticsBase.add(createWestCenterPanel(new JLabel("Failed"), failedField));
    statisticsBase.add(createWestCenterPanel(new JLabel("Created"), createdField));
    statisticsBase.add(createWestCenterPanel(new JLabel("Destroyed"), destroyedField));
    statisticsBase.add(createWestCenterPanel(new JLabel("Since"), resetTimeField));
    statisticsBase.add(new JButton(Control.builder()
            .command(model::clearStatistics)
            .name("Clear")
            .build()), BorderLayout.SOUTH);

    statisticsBase.setBorder(BorderFactory.createTitledBorder("Statistics"));

    return statisticsBase;
  }
}
