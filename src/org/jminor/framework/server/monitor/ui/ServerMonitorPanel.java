/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.Util;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.control.IntBeanSpinnerValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.TextBeanValueLink;
import org.jminor.framework.server.EntityDbServerAdmin;
import org.jminor.framework.server.monitor.ServerMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.net.URISyntaxException;
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
    requestsPerSecondChart.getXYPlot().setDataset(model.getConnectionRequestsDataSet());
    requestsPerSecondChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    memoryUsageChart.getXYPlot().setDataset(model.getMemoryUsageDataSet());
    memoryUsageChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    connectionCountChart.getXYPlot().setDataset(model.getConnectionCountDataSet());
    connectionCountChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    initUI();
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

  public void loadDomainModel() throws ClassNotFoundException, RemoteException,
          IllegalAccessException, InstantiationException, URISyntaxException {
    final String domainModelClass = JOptionPane.showInputDialog("Domain class name");
    final String locationURL = JOptionPane.showInputDialog("Location URL");
    if (!domainModelClass.isEmpty() && !locationURL.isEmpty()) {
      model.loadDomainModel(Util.getURI(locationURL), domainModelClass);
    }
  }

  private void initUI() throws RemoteException {
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    infoPanel.add(new JLabel("Remote connections", JLabel.RIGHT));
    infoPanel.add(initConnectionCountField());
    infoPanel.add(new JLabel("Memory usage", JLabel.RIGHT));
    infoPanel.add(initMemoryField());
    infoPanel.add(ControlProvider.createButton(Controls.methodControl(this, "loadDomainModel", "Load domain model...")));
    infoPanel.add(ControlProvider.createButton(Controls.methodControl(model, "performGC", "Run garbage collector")));
    infoPanel.add(ControlProvider.createButton(Controls.methodControl(this, "shutdownServer", "Shut down server")));

    final JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    controlPanel.add(new JLabel("Warning threshold (ms)"));
    final JSpinner spnWarningThreshold = new JSpinner(
            new IntBeanSpinnerValueLink(model, "warningThreshold", model.getWarningThresholdObserver()).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setColumns(3);
    controlPanel.add(spnWarningThreshold);

    final JPanel controlPanelBase = new JPanel(new BorderLayout(5, 5));
    controlPanelBase.add(controlPanel, BorderLayout.WEST);
    controlPanelBase.add(ControlProvider.createButton(Controls.methodControl(model, "resetStats", "Reset")), BorderLayout.EAST);

    final JPanel chartPanel = new JPanel(new GridLayout(3, 1, 5, 5));
    chartPanel.add(requestsPerSecondChartPanel);
    chartPanel.add(connectionCountChartPanel);
    chartPanel.add(memoryUsageChartPanel);
    chartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JPanel performancePanel = new JPanel(new BorderLayout());
    performancePanel.add(controlPanelBase, BorderLayout.NORTH);
    performancePanel.add(chartPanel, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(infoPanel, BorderLayout.NORTH);
    final JTabbedPane pane = new JTabbedPane();
    pane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    pane.addTab("Performance", performancePanel);
    pane.addTab("Environment", initEnvironmentPanel());
    pane.addTab("Database", new DatabaseMonitorPanel(model.getDatabaseMonitor()));
    pane.addTab("Clients/Users", new ClientUserMonitorPanel(model.getClientMonitor()));

    add(pane, BorderLayout.CENTER);
  }

  private JTabbedPane initEnvironmentPanel() throws RemoteException {
    final JTabbedPane panel = new JTabbedPane();
    panel.addTab("System", initEnvironmentInfoPanel());
    panel.addTab("Entities", initDomainModelPanel());

    return panel;
  }

  private JPanel initDomainModelPanel() {
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

  private JScrollPane initEnvironmentInfoPanel() throws RemoteException {
    final JTextArea infoArea = new JTextArea();
    infoArea.setAutoscrolls(false);
    infoArea.setEditable(false);

    final StringBuilder contents = new StringBuilder();
    final EntityDbServerAdmin server = model.getServer();
    contents.append("Server info:").append("\n");
    contents.append(server.getServerName()).append(" (").append(
            DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(new Date(server.getStartDate()))).append(")").append(
            " server/db port: ").append(server.getServerPort()).append("/").append(
            server.getServerDbPort()).append("\n").append("\n");
    contents.append("Database URL:").append("\n");
    contents.append(server.getDatabaseURL()).append("\n").append("\n");
    contents.append("System properties:").append("\n");
    contents.append(server.getSystemProperties());

    infoArea.setText(contents.toString());
    infoArea.setCaretPosition(0);

    return new JScrollPane(infoArea);
  }

  private JTextField initConnectionCountField() {
    final JTextField txtConnectionCount = new JTextField(6);
    txtConnectionCount.setEditable(false);
    txtConnectionCount.setHorizontalAlignment(JLabel.CENTER);
    new TextBeanValueLink(txtConnectionCount, model, "connectionCount", Integer.class, model.getStatsUpdatedObserver(),
            LinkType.READ_ONLY);

    return txtConnectionCount;
  }

  private JTextField initMemoryField() {
    final JTextField txtMemory = new JTextField(8);
    txtMemory.setEditable(false);
    txtMemory.setHorizontalAlignment(JLabel.CENTER);
    new TextBeanValueLink(txtMemory, model, "memoryUsage", String.class, model.getStatsUpdatedObserver(), LinkType.READ_ONLY);

    return txtMemory;
  }
}
