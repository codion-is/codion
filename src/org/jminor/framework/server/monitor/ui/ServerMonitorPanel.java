/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.BorderlessTabbedPaneUI;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.IntBeanSpinnerPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.TextBeanPropertyLink;
import org.jminor.framework.server.EntityDbServerAdmin;
import org.jminor.framework.server.monitor.ServerMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri
 * Date: 4.12.2007
 * Time: 18:14:35
 */
public class ServerMonitorPanel extends JPanel {

  private ServerMonitor model;

  private final JFreeChart requestsPerSecondChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel requestsPerSecondChartPanel = new ChartPanel(requestsPerSecondChart);

  public ServerMonitorPanel(final ServerMonitor model) throws RemoteException {
    this.model = model;
    requestsPerSecondChart.getXYPlot().setDataset(model.getConnectionRequestsDataSet());
    requestsPerSecondChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    initUI();
  }

  public ServerMonitor getModel() {
    return model;
  }

  public void loadDomainModel() throws ClassNotFoundException, RemoteException, MalformedURLException,
          IllegalAccessException, InstantiationException {
    final String domainModelClass = JOptionPane.showInputDialog("Domain class name");
    final String locationURL = JOptionPane.showInputDialog("Location URL");
    getModel().loadDomainModel(new URL(locationURL), domainModelClass);
  }

  private void initUI() throws RemoteException {
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    infoPanel.add(new JLabel("Remote connections", JLabel.RIGHT));
    infoPanel.add(initConnectionCountField());
    infoPanel.add(new JLabel("Memory usage", JLabel.RIGHT));
    infoPanel.add(initMemoryField());
    infoPanel.add(ControlProvider.createButton(ControlFactory.methodControl(this, "loadDomainModel", "Load domain model...")));
    infoPanel.add(ControlProvider.createButton(ControlFactory.methodControl(model, "performGC", "Run garbage collector")));
    infoPanel.add(ControlProvider.createButton(ControlFactory.methodControl(model, "shutdownServer", "Shut down server")));

    final JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    controlPanel.add(new JLabel("Warning threshold (ms)"));
    final JSpinner spnWarningThreshold = new JSpinner(
            new IntBeanSpinnerPropertyLink(model, "warningThreshold", model.eventWarningThresholdChanged(), null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setColumns(3);
    controlPanel.add(spnWarningThreshold);
    controlPanel.add(new JLabel("Connection timeout (ms)"));
    final JSpinner spnConnectionTimeout = new JSpinner(
            new IntBeanSpinnerPropertyLink(model, "connectionTimeout", model.eventConnectionTimeoutChanged(), null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnConnectionTimeout.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnConnectionTimeout.getEditor()).getTextField().setColumns(7);
    controlPanel.add(spnConnectionTimeout);

    final JPanel performancePanel = new JPanel(new BorderLayout());
    requestsPerSecondChartPanel.setBorder(BorderFactory.createEtchedBorder());
    performancePanel.add(controlPanel, BorderLayout.NORTH);
    performancePanel.add(requestsPerSecondChartPanel, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(infoPanel, BorderLayout.NORTH);
    final JTabbedPane pane = new JTabbedPane();
    pane.setUI(new BorderlessTabbedPaneUI());
    pane.addTab("Performance", performancePanel);
    pane.addTab("Environment", initEnvironmentInfoPanel());
    pane.addTab("Database", new DatabaseMonitorPanel(model.getDatabaseMonitor()));
    pane.addTab("Clients", new ClientMonitorPanel(model.getClientMonitor()));
    pane.addTab("Users", new UserMonitorPanel(model.getUserMonitor()));

    add(pane, BorderLayout.CENTER);
  }

  private JScrollPane initEnvironmentInfoPanel() throws RemoteException {
    final JTextArea infoArea = new JTextArea();
    infoArea.setAutoscrolls(false);
    infoArea.setEditable(false);

    final StringBuilder contents = new StringBuilder();
    final EntityDbServerAdmin server = model.getServer();
    contents.append("Server info:").append("\n");
    contents.append(server.getServerName()).append(" (").append(
            DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(server.getStartDate())).append(")").append(
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
    new TextBeanPropertyLink(txtConnectionCount, model, "connectionCount", Integer.class, model.eventStatsUpdated(),
            LinkType.READ_ONLY);

    return txtConnectionCount;
  }

  private JTextField initMemoryField() {
    final JTextField txtMemory = new JTextField(8);
    txtMemory.setEditable(false);
    txtMemory.setHorizontalAlignment(JLabel.CENTER);
    new TextBeanPropertyLink(txtMemory, model, "memoryUsage", String.class, model.eventStatsUpdated(),
            LinkType.READ_ONLY);

    return txtMemory;
  }
}
