/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.control.IntBeanSpinnerValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.SelectedItemBeanValueLink;
import org.jminor.common.ui.control.TextBeanValueLink;
import org.jminor.framework.server.EntityConnectionServerAdmin;
import org.jminor.framework.server.monitor.ServerMonitor;

import ch.qos.logback.classic.Level;
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
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * A ServerMonitorPanel
 */
public final class ServerMonitorPanel extends JPanel {

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
    setColors(requestsPerSecondChart);
    setColors(memoryUsageChart);
    setColors(connectionCountChart);
    initializeUI();
  }

  public ServerMonitor getModel() {
    return model;
  }

  public void shutdownServer() throws RemoteException {
    if (JOptionPane.showConfirmDialog(this, "Are you sure you want to shut down this server?", "Confirm shutdown",
            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      model.shutdownServer();
    }
  }

  private void initializeUI() throws RemoteException {
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    infoPanel.add(new JLabel("Connections", JLabel.RIGHT));
    infoPanel.add(initializeConnectionCountField());
    infoPanel.add(new JLabel("limit", JLabel.RIGHT));
    final JSpinner spnConnectionLimit = new JSpinner(
            new IntBeanSpinnerValueLink(model, "connectionLimit", model.getConnectionLimitObserver()).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnConnectionLimit.getEditor()).getTextField().setColumns(3);
    infoPanel.add(spnConnectionLimit);
    infoPanel.add(new JLabel("Mem. usage", JLabel.RIGHT));
    infoPanel.add(initializeMemoryField());
    infoPanel.add(new JLabel("Logging", JLabel.RIGHT));
    infoPanel.add(initializeLoggingLevelField());
    infoPanel.add(ControlProvider.createButton(Controls.methodControl(this, "shutdownServer", "Shutdown")));
    infoPanel.add(ControlProvider.createButton(Controls.methodControl(model, "performGC", "GC")));

    final JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

    final JSpinner spnUpdateInterval = new JSpinner(new IntBeanSpinnerValueLink(model, "statisticsUpdateInterval",
            model.getStatisticsUpdateIntervalObserver()).getSpinnerModel());

    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setColumns(3);

    controlPanel.add(new JLabel("Update interval (s)"));
    controlPanel.add(spnUpdateInterval);

    controlPanel.add(new JLabel("Warning threshold (ms)"));
    final JSpinner spnWarningThreshold = new JSpinner(
            new IntBeanSpinnerValueLink(model, "warningThreshold", model.getWarningThresholdObserver()).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setColumns(3);
    controlPanel.add(spnWarningThreshold);

    final JPanel controlPanelBase = new JPanel(new BorderLayout(5, 5));
    controlPanelBase.add(controlPanel, BorderLayout.WEST);
    controlPanelBase.add(ControlProvider.createButton(Controls.methodControl(model, "resetStatistics", "Reset")), BorderLayout.EAST);

    final JPanel chartPanel = new JPanel(new GridLayout(3, 1, 5, 5));
    chartPanel.add(requestsPerSecondChartPanel);
    chartPanel.add(connectionCountChartPanel);
    chartPanel.add(memoryUsageChartPanel);
    chartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JPanel performancePanel = new JPanel(new BorderLayout());
    performancePanel.add(controlPanelBase, BorderLayout.SOUTH);
    performancePanel.add(chartPanel, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(infoPanel, BorderLayout.NORTH);
    final JTabbedPane pane = new JTabbedPane();
    pane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    pane.addTab("Performance", performancePanel);
    pane.addTab("Database", new DatabaseMonitorPanel(model.getDatabaseMonitor()));
    pane.addTab("Clients/Users", new ClientUserMonitorPanel(model.getClientMonitor()));
    pane.addTab("Environment", initializeEnvironmentPanel());

    add(pane, BorderLayout.CENTER);
  }

  private JTabbedPane initializeEnvironmentPanel() throws RemoteException {
    final JTabbedPane panel = new JTabbedPane();
    panel.addTab("System", initializeEnvironmentInfoPanel());
    panel.addTab("Entities", initializeDomainModelPanel());

    return panel;
  }

  private JPanel initializeDomainModelPanel() {
    final JPanel panel = new JPanel(new BorderLayout(5,5));
    final JTable table = new JTable(model.getDomainTableModel());
    table.setRowSorter(new TableRowSorter<TableModel>(model.getDomainTableModel()));
    final JScrollPane scroller = new JScrollPane(table);

    final JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    refreshPanel.add(ControlProvider.createButton(Controls.methodControl(model, "refreshDomainList", "Refresh")));
    panel.add(refreshPanel, BorderLayout.NORTH);
    panel.add(scroller, BorderLayout.CENTER);

    return panel;
  }

  private JScrollPane initializeEnvironmentInfoPanel() throws RemoteException {
    final JTextArea infoArea = new JTextArea();
    infoArea.setAutoscrolls(false);
    infoArea.setEditable(false);

    final StringBuilder contents = new StringBuilder();
    final EntityConnectionServerAdmin server = model.getServer();
    final String startDate = DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(new Date(server.getStartDate()));
    contents.append("Server info:").append("\n");
    contents.append(server.getServerName()).append(" (").append(startDate).append(")").append(
            " port: ").append(server.getServerPort()).append("\n").append("\n");
    contents.append("Server version:").append("\n");
    contents.append(server.getServerVersion()).append("\n");
    contents.append("Database URL:").append("\n");
    contents.append(server.getDatabaseURL()).append("\n").append("\n");
    contents.append("System properties:").append("\n");
    contents.append(server.getSystemProperties());

    infoArea.setText(contents.toString());
    infoArea.setCaretPosition(0);

    return new JScrollPane(infoArea);
  }

  private JTextField initializeConnectionCountField() {
    final JTextField txtConnectionCount = new JTextField(6);
    txtConnectionCount.setEditable(false);
    txtConnectionCount.setHorizontalAlignment(JLabel.CENTER);
    new TextBeanValueLink(txtConnectionCount, model, "connectionCount", Integer.class, model.getStatisticsUpdatedObserver(),
            LinkType.READ_ONLY);

    return txtConnectionCount;
  }

  private JTextField initializeMemoryField() {
    final JTextField txtMemory = new JTextField(8);
    txtMemory.setEditable(false);
    txtMemory.setHorizontalAlignment(JLabel.CENTER);
    new TextBeanValueLink(txtMemory, model, "memoryUsage", String.class, model.getStatisticsUpdatedObserver(), LinkType.READ_ONLY);

    return txtMemory;
  }

  private JComboBox initializeLoggingLevelField() {
    final DefaultComboBoxModel comboModel = new DefaultComboBoxModel();
    comboModel.addElement(Level.TRACE);
    comboModel.addElement(Level.DEBUG);
    comboModel.addElement(Level.INFO);
    comboModel.addElement(Level.WARN);
    comboModel.addElement(Level.ERROR);

    final JComboBox box = new JComboBox(comboModel);
    new SelectedItemBeanValueLink(box, model, "loggingLevel", Level.class, model.getLoggingLevelObserver());

    return box;
  }

  private void setColors(final JFreeChart chart) {
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    chart.setBackgroundPaint(this.getBackground());
  }
}
