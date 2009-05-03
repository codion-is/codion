/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.Util;
import org.jminor.common.model.formats.FullDateFormat;
import org.jminor.common.ui.IPopupProvider;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.IntBeanSpinnerPropertyLink;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;
import org.jminor.framework.server.monitor.ServerMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:14:35
 */
public class ServerMonitorPanel extends JPanel implements IPopupProvider {

  private ServerMonitor model;
  private JTextField txtConnectionCount;
  private JTextField txtMemory;
  private JTextArea infoArea;
  private JPopupMenu popupMenu;

  private final JFreeChart requestsPerSecondChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel requestsPerSecondChartPanel = new ChartPanel(requestsPerSecondChart);

  public ServerMonitorPanel(final ServerMonitor model) throws RemoteException {
    this.model = model;
    requestsPerSecondChart.getXYPlot().setDataset(model.getConnectionRequestsDataSet());
    requestsPerSecondChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    initUI();
    updateView();
    model.evtRefresh.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          updateView();
        }
        catch (RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
  }

  public JPopupMenu getPopupMenu() {
    if (popupMenu == null)
      popupMenu = ControlProvider.createPopupMenu(getPopupCommands());

    return popupMenu;
  }

  private void initUI() throws RemoteException {
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    infoPanel.add(new JLabel("Remote connections", JLabel.RIGHT));
    infoPanel.add(initConnectionCountField());
    infoPanel.add(new JLabel("Memory usage", JLabel.RIGHT));
    infoPanel.add(initMemoryField());
    infoPanel.setBorder(BorderFactory.createTitledBorder("Server"));
    infoPanel.add(ControlProvider.createButton(ControlFactory.methodControl(model, "performGC", "Run garbage collector")));
    infoPanel.add(ControlProvider.createButton(ControlFactory.methodControl(model, "shutdownServer", "Shut down server")));

    setLayout(new BorderLayout());
    add(infoPanel, BorderLayout.NORTH);
    final JTabbedPane pane = new JTabbedPane();
    pane.addTab("Performance", requestsPerSecondChartPanel);
    pane.addTab("Environment", initEnvironmentInfoPanel());

    final JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    controlPanel.add(new JLabel("Warning threshold (ms)"));
    final JSpinner spnWarningThreshold = new JSpinner(
            new IntBeanSpinnerPropertyLink(model, "warningThreshold", model.evtWarningThresholdChanged, null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnWarningThreshold.getEditor()).getTextField().setColumns(3);
    controlPanel.add(spnWarningThreshold);

    final JPanel tabBase = new JPanel(new BorderLayout(5,5));
    tabBase.setBorder(BorderFactory.createTitledBorder("Server information"));
    tabBase.add(controlPanel, BorderLayout.NORTH);
    tabBase.add(pane, BorderLayout.CENTER);

    add(tabBase, BorderLayout.CENTER);
  }

  private JScrollPane initEnvironmentInfoPanel() {
    infoArea = new JTextArea();
    infoArea.setAutoscrolls(false);
    infoArea.setEditable(false);

    return new JScrollPane(infoArea);
  }

  private JTextField initConnectionCountField() {
    txtConnectionCount = new JTextField(6);
    txtConnectionCount.setEditable(false);
    txtConnectionCount.setHorizontalAlignment(JLabel.CENTER);

    return txtConnectionCount;
  }

  private JTextField initMemoryField() {
    txtMemory = new JTextField(8);
    txtMemory.setEditable(false);
    txtMemory.setHorizontalAlignment(JLabel.CENTER);

    return txtMemory;
  }

  private void updateView() throws RemoteException {
    txtConnectionCount.setText(String.valueOf(model.getServer().getConnectionCount()));
    txtMemory.setText(String.valueOf(model.getServer().getMemoryUsage()));

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
    contents.append(Util.getSystemProperties());

    infoArea.setText(contents.toString());
    infoArea.setCaretPosition(0);
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return ret;
  }
}
