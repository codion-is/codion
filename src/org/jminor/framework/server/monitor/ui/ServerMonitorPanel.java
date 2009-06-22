/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.formats.FullDateFormat;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.IntBeanSpinnerPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.TextBeanPropertyLink;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;
import org.jminor.framework.server.monitor.ServerMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
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

  private void initUI() throws RemoteException {
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    infoPanel.add(new JLabel("Remote connections", JLabel.RIGHT));
    infoPanel.add(initConnectionCountField());
    infoPanel.add(new JLabel("Memory usage", JLabel.RIGHT));
    infoPanel.add(initMemoryField());
    infoPanel.add(ControlProvider.createButton(ControlFactory.methodControl(model, "performGC", "Run garbage collector")));
    infoPanel.add(ControlProvider.createButton(ControlFactory.methodControl(model, "shutdownServer", "Shut down server")));

    final JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    controlPanel.add(new JLabel("Warning threshold (ms)"));
    final JSpinner spnWarningThreshold = new JSpinner(
            new IntBeanSpinnerPropertyLink(model, "warningThreshold", model.evtWarningThresholdChanged, null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setColumns(3);
    controlPanel.add(spnWarningThreshold);

    final JPanel performancePanel = new JPanel(new BorderLayout());
    performancePanel.add(controlPanel, BorderLayout.NORTH);
    performancePanel.add(requestsPerSecondChartPanel, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(infoPanel, BorderLayout.NORTH);
    final JTabbedPane pane = new JTabbedPane();
    pane.setUI(new BasicTabbedPaneUI() {
      @Override
      protected Insets getContentBorderInsets(final int tabPlacement) {
        return new Insets(1,0,1,0);
      }

      @Override
      protected Insets getSelectedTabPadInsets(int tabPlacement) {
        return new Insets(2,2,2,1);
      }

      @Override
      protected Insets getTabAreaInsets(int tabPlacement) {
        return new Insets(3,2,0,2);
      }

      @Override
      protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return new Insets(0,4,1,4);
      }
    });
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

    final StringBuffer contents = new StringBuffer();
    final IEntityDbRemoteServerAdmin server = model.getServer();
    contents.append("Server info:").append("\n");
    contents.append(server.getServerName()).append(" (").append(
            FullDateFormat.get().format(server.getStartDate())).append(")").append(
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
    new TextBeanPropertyLink(txtConnectionCount, model, "connectionCount", Integer.class, model.evtStatsUpdated,
            null, LinkType.READ_ONLY, null);

    return txtConnectionCount;
  }

  private JTextField initMemoryField() {
    final JTextField txtMemory = new JTextField(8);
    txtMemory.setEditable(false);
    txtMemory.setHorizontalAlignment(JLabel.CENTER);
    new TextBeanPropertyLink(txtMemory, model, "memoryUsage", String.class, model.evtStatsUpdated,
            null, LinkType.READ_ONLY, null);

    return txtMemory;
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return ret;
  }
}
