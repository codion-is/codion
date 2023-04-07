/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.swing.framework.server.monitor.ConnectionPoolMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.panel.Panels.createWestCenterPanel;
import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldSize;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.layout.Layouts.*;
import static javax.swing.BorderFactory.createEtchedBorder;
import static javax.swing.BorderFactory.createTitledBorder;
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
  private final DateTimeFormatter dateTimeFormatter = LocaleDateTimePattern.builder()
          .delimiterDash()
          .yearFourDigits()
          .hoursMinutesSeconds()
          .build()
          .createFormatter();
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

  private final JTextField resetTimeField = textField()
          .columns(RESET_FIELD_COLUMNS)
          .build();
  private final JTextField poolSizeField = textField()
          .editable(false)
          .horizontalAlignment(CENTER)
          .build();
  private final JTextField createdField = textField()
          .editable(false)
          .horizontalAlignment(CENTER)
          .build();
  private final JTextField destroyedField = textField()
          .editable(false)
          .horizontalAlignment(CENTER)
          .build();
  private final JTextField requestedField = textField()
          .editable(false)
          .horizontalAlignment(CENTER)
          .build();
  private final JTextField failedField = textField()
          .editable(false)
          .horizontalAlignment(CENTER)
          .build();

  /**
   * Instantiates a new ConnectionPoolMonitorPanel
   * @param model the ConnectionPoolMonitor to base this panel on
   */
  public ConnectionPoolMonitorPanel(ConnectionPoolMonitor model) {
    this.model = model;
    this.format.setMaximumFractionDigits(2);
    initializeUI();
    updateView();
    bindEvents();
  }

  private void updateView() {
    ConnectionPoolStatistics statistics = model.connectionPoolStatistics();
    poolSizeField.setText(format.format(statistics.size()));
    createdField.setText(format.format(statistics.created()));
    destroyedField.setText(format.format(statistics.destroyed()));
    resetTimeField.setText(dateTimeFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(statistics.resetTime()), ZoneId.systemDefault())));
    requestedField.setText(format.format(statistics.requests()));
    double prc = statistics.failedRequests() / (double) statistics.requests() * HUNDRED;
    failedField.setText(format.format(statistics.failedRequests()) + (prc > 0 ? " (" + format.format(prc) + "%)" : ""));
    if (model.datasetContainsData()) {
      inPoolSnapshotChart.getXYPlot().setDataset(model.snapshotDataset());
    }
  }

  private void initializeUI() {
    initializeCharts(model);
    setLayout(borderLayout());

    add(configurationPanel(), BorderLayout.NORTH);
    add(chartPanel(), BorderLayout.CENTER);
    add(southPanel(), BorderLayout.SOUTH);
  }

  private void initializeCharts(ConnectionPoolMonitor model) {
    JFreeChart checkOutTimeChart = ChartFactory.createXYStepChart(null,
            null, null, model.checkOutTimeCollection(), PlotOrientation.VERTICAL, true, true, false);
    setColors(checkOutTimeChart);
    checkOutTimePanel = new ChartPanel(checkOutTimeChart);
    checkOutTimePanel.setBorder(createEtchedBorder());

    DeviationRenderer devRenderer = new DeviationRenderer();
    devRenderer.setDefaultShapesVisible(false);
    checkOutTimeChart.getXYPlot().setRenderer(devRenderer);

    inPoolChart.getXYPlot().setDataset(model.inPoolDataset());
    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) inPoolChart.getXYPlot().getRenderer();
    renderer.setSeriesPaint(0, Color.RED);
    renderer.setSeriesPaint(1, Color.BLUE);
    renderer.setSeriesPaint(2, Color.PINK);
    renderer.setSeriesPaint(3, Color.GREEN);
    renderer.setSeriesPaint(4, Color.MAGENTA);
    requestsPerSecondChart.getXYPlot().setDataset(model.requestsPerSecondDataset());
    checkOutTimeChart.getXYPlot().setDataset(model.checkOutTimeCollection());
    setColors(inPoolSnapshotChart);
    setColors(inPoolChart);
    setColors(requestsPerSecondChart);
    setColors(checkOutTimeChart);
  }

  private void setColors(JFreeChart chart) {
    ChartUtil.linkColors(this, chart);
  }

  private void bindEvents() {
    model.statisticsObserver().addListener(this::updateView);
  }

  private JPanel configurationPanel() {
    JPanel configBase = panel(flexibleGridLayout(1, 0))
            .border(createTitledBorder("Configuration"))
            .add(createWestCenterPanel(new JLabel("Mininum size"), integerSpinner(model.minimumPoolSizeValue())
                    .columns(3)
                    .editable(false)
                    .build()))
            .add(createWestCenterPanel(new JLabel("Maximum size"), integerSpinner(model.maximumPoolSizeValue())
                    .columns(3)
                    .editable(false)
                    .build()))
            .add(createWestCenterPanel(new JLabel("Checkout timeout (ms)"), integerSpinner(model.maximumCheckOutTimeValue())
                    .stepSize(1000)
                    .columns(6)
                    .editable(false)
                    .build()))
            .add(createWestCenterPanel(new JLabel("Idle timeout (ms)"), integerSpinner(model.pooledConnectionTimeoutValue())
                    .stepSize(1000)
                    .columns(6)
                    .groupingUsed(true)
                    .editable(false)
                    .build()))
            .add(createWestCenterPanel(new JLabel("Cleanup interval (s)"), integerSpinner(model.poolCleanupIntervalValue())
                    .columns(3)
                    .editable(false)
                    .build()))
            .build();

    return panel(flowLayout(RIGHT))
            .add(configBase)
            .build();
  }

  private JPanel chartPanel() {
    return panel(new GridLayout(2, 2))
            .border(createEtchedBorder())
            .add(requestsPerSecondChartPanel)
            .add(inPoolChartPanel)
            .add(checkOutTimePanel)
            .add(inPoolSnapshotChartPanel)
            .build();
  }

  private JPanel southPanel() {
    JPanel chartConfig = panel(flexibleGridLayout(1, 4))
            .border(createTitledBorder("Charts"))
            .add(new JLabel("Update interval (s)"))
            .add(integerSpinner(model.updateIntervalValue())
                    .minimum(1)
                    .columns(SPINNER_COLUMNS)
                    .editable(false)
                    .build())
            .add(checkBox(model.collectSnapshotStatisticsState())
                    .caption("Snapshot")
                    .maximumSize(preferredTextFieldSize())
                    .build())
            .add(checkBox(model.collectCheckOutTimesState())
                    .caption("Check out times")
                    .maximumSize(preferredTextFieldSize())
                    .build())
            .add(button(control(model::clearStatistics))
                    .caption("Clear")
                    .maximumSize(preferredTextFieldSize())
                    .build())
            .build();

    return panel(borderLayout())
            .add(chartConfig, BorderLayout.WEST)
            .add(statisticsPanel(), BorderLayout.CENTER)
            .build();
  }

  private JPanel statisticsPanel() {
    return panel(borderLayout())
            .border(createTitledBorder("Statistics"))
            .add(panel(flexibleGridLayout(1, 0))
                    .add(createWestCenterPanel(new JLabel("Connections"), poolSizeField))
                    .add(createWestCenterPanel(new JLabel("Requested"), requestedField))
                    .add(createWestCenterPanel(new JLabel("Failed"), failedField))
                    .add(createWestCenterPanel(new JLabel("Created"), createdField))
                    .add(createWestCenterPanel(new JLabel("Destroyed"), destroyedField))
                    .add(createWestCenterPanel(new JLabel("Since"), resetTimeField))
                    .build(), BorderLayout.CENTER)
            .add(button(control(model::resetStatistics))
                    .caption("Reset")
                    .build(), BorderLayout.EAST)
            .build();
  }
}
