/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.TaskScheduler;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.framework.server.monitor.ServerMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.rmi.RemoteException;

/**
 * A ServerMonitorPanel
 */
public final class ServerMonitorPanel extends JPanel {

  private static final int SPINNER_COLUMNS = 3;

  private final ServerMonitor model;

  private final JFreeChart requestsPerSecondChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel requestsPerSecondChartPanel = new ChartPanel(requestsPerSecondChart);

  private final JFreeChart memoryUsageChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel memoryUsageChartPanel = new ChartPanel(memoryUsageChart);

  private final JFreeChart connectionCountChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel connectionCountChartPanel = new ChartPanel(connectionCountChart);

  private final JFreeChart systemLoadChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel systemLoadChartPanel = new ChartPanel(systemLoadChart);

  private final JFreeChart threadCountChart = ChartFactory.createXYStepChart(null,
          null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel threadCountChartPanel = new ChartPanel(threadCountChart);

  private final JFreeChart gcEventsChart = ChartFactory.createXYBarChart(null,
          null, true, "Duration (ms)", null);
  private final ChartPanel gcEventsChartPanel = new ChartPanel(gcEventsChart);

  /**
   * Instantiates a new ServerMonitorPanel
   * @param model the ServerMonitor to base this panel on
   * @throws RemoteException in case of an exception
   */
  public ServerMonitorPanel(final ServerMonitor model) throws RemoteException {
    this.model = model;
    requestsPerSecondChart.getXYPlot().setDataset(model.getConnectionRequestsDataset());
    memoryUsageChart.getXYPlot().setDataset(model.getMemoryUsageDataset());
    connectionCountChart.getXYPlot().setDataset(model.getConnectionCountDataset());
    systemLoadChart.getXYPlot().setDataset(model.getSystemLoadDataset());
    systemLoadChart.getXYPlot().getRangeAxis().setRange(0, 100);
    gcEventsChart.getXYPlot().setDataset(model.getGcEventsDataset());
    threadCountChart.getXYPlot().setDataset(model.getThreadCountDataset());
    setColors(requestsPerSecondChart);
    setColors(memoryUsageChart);
    setColors(connectionCountChart);
    setColors(systemLoadChart);
    setColors(gcEventsChart);
    setColors(threadCountChart);
    initializeUI();
  }

  public ServerMonitor getModel() {
    return model;
  }

  public void shutdownServer() {
    if (JOptionPane.showConfirmDialog(this, "Are you sure you want to shut down this server?", "Confirm shutdown",
            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      model.shutdownServer();
    }
  }

  private void initializeUI() throws RemoteException {
    final JPanel infoPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));
    infoPanel.add(new JLabel("Connections", JLabel.RIGHT));
    infoPanel.add(initializeConnectionCountField());
    infoPanel.add(new JLabel("limit", JLabel.RIGHT));
    final JSpinner spnConnectionLimit = new JSpinner(
            ValueLinks.intSpinnerValueLink(model, "connectionLimit", model.getConnectionLimitObserver()));
    ((JSpinner.DefaultEditor) spnConnectionLimit.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);
    infoPanel.add(spnConnectionLimit);
    infoPanel.add(new JLabel("Mem. usage", JLabel.RIGHT));
    infoPanel.add(initializeMemoryField());
    infoPanel.add(new JLabel("Logging", JLabel.RIGHT));
    infoPanel.add(initializeLoggingLevelField());
    infoPanel.add(ControlProvider.createButton(Controls.control(this::shutdownServer, "Shutdown")));

    setLayout(new BorderLayout());
    add(infoPanel, BorderLayout.NORTH);
    final JTabbedPane pane = new JTabbedPane();
    pane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    pane.addTab("Performance", initializePerformancePanel());
    pane.addTab("Database", new DatabaseMonitorPanel(model.getDatabaseMonitor()));
    pane.addTab("Clients/Users", new ClientUserMonitorPanel(model.getClientMonitor()));
    pane.addTab("Environment", initializeEnvironmentPanel());

    add(pane, BorderLayout.CENTER);
  }

  private JPanel initializePerformancePanel() {
    final JPanel controlPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));

    final JSpinner spnUpdateInterval = new JSpinner(ValueLinks.intSpinnerValueLink(model.getUpdateScheduler(),
            TaskScheduler.INTERVAL_PROPERTY, model.getUpdateScheduler().getIntervalObserver()));

    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);

    controlPanel.add(new JLabel("Update interval (s)"));
    controlPanel.add(spnUpdateInterval);

    final JPanel controlPanelBase = new JPanel(UiUtil.createBorderLayout());
    controlPanelBase.add(controlPanel, BorderLayout.WEST);
    controlPanelBase.add(ControlProvider.createButton(Controls.control(model::resetStatistics, "Reset")), BorderLayout.EAST);

    final JPanel chartPanelLeft = new JPanel(new GridLayout(3, 1, 5, 5));
    final JPanel chartPanelRight = new JPanel(new GridLayout(2, 1, 5, 5));
    chartPanelLeft.add(requestsPerSecondChartPanel);
    chartPanelLeft.add(connectionCountChartPanel);
    chartPanelLeft.add(systemLoadChartPanel);
    chartPanelRight.add(threadCountChartPanel);
    chartPanelRight.add(memoryUsageChartPanel);
    final JPanel chartPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    chartPanel.add(chartPanelLeft);
    chartPanel.add(chartPanelRight);
    chartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JPanel overviewPanel = new JPanel(UiUtil.createBorderLayout());
    overviewPanel.add(controlPanelBase, BorderLayout.SOUTH);
    overviewPanel.add(chartPanel, BorderLayout.CENTER);

    final JTabbedPane tabPane = new JTabbedPane();
    tabPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    tabPane.addTab("Overview", overviewPanel);
    tabPane.addTab("GC", initializeGCPanel());

    final JPanel ret = new JPanel(UiUtil.createBorderLayout());
    ret.add(tabPane, BorderLayout.CENTER);

    return ret;
  }

  private JPanel initializeGCPanel() {
    final JPanel chartPanel = new JPanel(UiUtil.createBorderLayout());
    chartPanel.add(gcEventsChartPanel);

    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    southPanel.add(ControlProvider.createButton(Controls.control(model::refreshGCInfo, "Refresh")));

    final JPanel panel = new JPanel(UiUtil.createBorderLayout());

    panel.add(chartPanel, BorderLayout.CENTER);
    panel.add(southPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JTabbedPane initializeEnvironmentPanel() throws RemoteException {
    final JTabbedPane panel = new JTabbedPane();
    panel.addTab("System", initializeEnvironmentInfoPanel());
    panel.addTab("Entities", initializeDomainModelPanel());

    return panel;
  }

  private JPanel initializeDomainModelPanel() {
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    final JTable table = new JTable(model.getDomainTableModel());
    table.setRowSorter(new TableRowSorter<>(model.getDomainTableModel()));
    final JScrollPane scroller = new JScrollPane(table);

    final JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    refreshPanel.add(ControlProvider.createButton(Controls.control(model::refreshDomainList, "Refresh")));
    panel.add(refreshPanel, BorderLayout.NORTH);
    panel.add(scroller, BorderLayout.CENTER);

    return panel;
  }

  private JScrollPane initializeEnvironmentInfoPanel() throws RemoteException {
    final JTextArea infoArea = new JTextArea();
    infoArea.setAutoscrolls(false);
    infoArea.setEditable(false);
    infoArea.setText(model.getEnvironmentInfo());
    infoArea.setCaretPosition(0);

    return new JScrollPane(infoArea);
  }

  private JTextField initializeConnectionCountField() {
    final IntegerField txtConnectionCount = new IntegerField(4);
    txtConnectionCount.setEditable(false);
    txtConnectionCount.setHorizontalAlignment(JLabel.CENTER);
    ValueLinks.integerValueLink(txtConnectionCount, model, "connectionCount", model.getStatisticsUpdatedObserver(), true, true, true);

    return txtConnectionCount;
  }

  private JTextField initializeMemoryField() {
    final JTextField txtMemory = new JTextField(8);
    txtMemory.setEditable(false);
    txtMemory.setHorizontalAlignment(JLabel.CENTER);
    ValueLinks.textValueLink(txtMemory, model, "memoryUsage", model.getStatisticsUpdatedObserver(), true);

    return txtMemory;
  }

  private JComboBox initializeLoggingLevelField() {
    final DefaultComboBoxModel comboModel = new DefaultComboBoxModel(model.getLoggingLevels().toArray());

    final JComboBox box = new JComboBox<>(comboModel);
    ValueLinks.selectedItemValueLink(box, model, "loggingLevel", Object.class, model.getLoggingLevelObserver());

    return box;
  }

  private void setColors(final JFreeChart chart) {
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    chart.setBackgroundPaint(this.getBackground());
  }
}
