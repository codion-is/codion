/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.state.State;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.server.monitor.ServerMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.util.List;

import static is.codion.swing.common.ui.control.Control.control;
import static java.util.Arrays.asList;

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
          null, true, null, null);
  private final ChartPanel gcEventsChartPanel = new ChartPanel(gcEventsChart);

  private final State synchronizedZoomState = State.state(true);

  /**
   * Instantiates a new ServerMonitorPanel
   * @param model the ServerMonitor to base this panel on
   * @throws RemoteException in case of an exception
   */
  public ServerMonitorPanel(ServerMonitor model) throws RemoteException {
    this.model = model;
    requestsPerSecondChart.getXYPlot().setDataset(model.connectionRequestsDataset());
    memoryUsageChart.getXYPlot().setDataset(model.memoryUsageDataset());
    connectionCountChart.getXYPlot().setDataset(model.connectionCountDataset());
    systemLoadChart.getXYPlot().setDataset(model.systemLoadDataset());
    systemLoadChart.getXYPlot().getRangeAxis().setRange(0, 100);
    gcEventsChart.getXYPlot().setDataset(model.gcEventsDataset());
    threadCountChart.getXYPlot().setDataset(model.threadCountDataset());
    setColors(requestsPerSecondChart);
    setColors(memoryUsageChart);
    setColors(connectionCountChart);
    setColors(systemLoadChart);
    setColors(gcEventsChart);
    setColors(threadCountChart);
    initializeUI();
    bindEvents();
  }

  public ServerMonitor model() {
    return model;
  }

  public void shutdownServer() {
    if (JOptionPane.showConfirmDialog(this, "Are you sure you want to shut down this server?", "Confirm shutdown",
            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      model.shutdownServer();
    }
  }

  private void initializeUI() throws RemoteException {
    JPanel serverPanel = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    serverPanel.add(new JLabel("Connections", SwingConstants.RIGHT));
    serverPanel.add(initializeConnectionCountField());
    serverPanel.add(new JLabel("limit", SwingConstants.RIGHT));
    serverPanel.add(Components.integerSpinner(model.connectionLimitValue())
            .columns(SPINNER_COLUMNS)
            .build());
    serverPanel.add(new JLabel("Mem. usage", SwingConstants.RIGHT));
    serverPanel.add(initializeMemoryField());
    serverPanel.add(new JLabel("Logging", SwingConstants.RIGHT));
    serverPanel.add(initializeLogLevelField());

    JPanel northPanel = new JPanel(Layouts.borderLayout());
    northPanel.add(serverPanel, BorderLayout.CENTER);
    JPanel shutdownBasePanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    shutdownBasePanel.add(Components.button(control(this::shutdownServer))
            .caption("Shutdown")
            .build(), BorderLayout.EAST);
    northPanel.add(shutdownBasePanel, BorderLayout.EAST);
    northPanel.setBorder(BorderFactory.createTitledBorder("Server"));

    setLayout(new BorderLayout());
    add(northPanel, BorderLayout.NORTH);
    JTabbedPane pane = new JTabbedPane();
    pane.addTab("Performance", initializePerformancePanel());
    pane.addTab("Database", new DatabaseMonitorPanel(model.databaseMonitor()));
    pane.addTab("Clients/Users", new ClientUserMonitorPanel(model.clientMonitor()));
    pane.addTab("Environment", initializeEnvironmentPanel());

    add(pane, BorderLayout.CENTER);
  }

  private JPanel initializePerformancePanel() {
    JPanel controlPanel = new JPanel(Layouts.flexibleGridLayout(1, 2));
    controlPanel.setBorder(BorderFactory.createTitledBorder("Charts"));

    JPanel intervalPanel = new JPanel(Layouts.borderLayout());
    intervalPanel.add(new JLabel("Update interval (s)"), BorderLayout.WEST);
    intervalPanel.add(Components.integerSpinner(model.updateIntervalValue())
            .minimum(1)
            .columns(SPINNER_COLUMNS)
            .editable(false)
            .build(), BorderLayout.CENTER);

    JPanel chartsPanel = new JPanel(Layouts.borderLayout());
    chartsPanel.add(intervalPanel, BorderLayout.CENTER);
    chartsPanel.add(Components.button(control(model::clearStatistics))
            .caption("Clear")
            .build(), BorderLayout.EAST);

    controlPanel.add(chartsPanel);

    JPanel zoomPanel = new JPanel(Layouts.borderLayout());
    zoomPanel.add(Components.checkBox(synchronizedZoomState)
            .caption("Synchronize zoom")
            .build(), BorderLayout.CENTER);
    zoomPanel.add(Components.button(control(this::resetZoom))
            .caption("Reset zoom")
            .build(), BorderLayout.EAST);
    controlPanel.add(zoomPanel);

    JPanel controlPanelBase = new JPanel(Layouts.borderLayout());
    controlPanelBase.add(controlPanel, BorderLayout.WEST);

    JPanel chartPanelLeft = new JPanel(Layouts.gridLayout(3, 1));
    JPanel chartPanelRight = new JPanel(Layouts.gridLayout(3, 1));
    chartPanelLeft.add(requestsPerSecondChartPanel);
    chartPanelLeft.add(connectionCountChartPanel);
    chartPanelLeft.add(systemLoadChartPanel);
    chartPanelRight.add(threadCountChartPanel);
    chartPanelRight.add(memoryUsageChartPanel);
    chartPanelRight.add(gcEventsChartPanel);
    JPanel chartPanel = new JPanel(Layouts.gridLayout(1, 2));
    chartPanel.add(chartPanelLeft);
    chartPanel.add(chartPanelRight);
    chartPanel.setBorder(BorderFactory.createEtchedBorder());

    JPanel overviewPanel = new JPanel(Layouts.borderLayout());
    overviewPanel.add(controlPanelBase, BorderLayout.SOUTH);
    overviewPanel.add(chartPanel, BorderLayout.CENTER);

    JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(overviewPanel, BorderLayout.CENTER);

    return panel;
  }

  private JTabbedPane initializeEnvironmentPanel() throws RemoteException {
    JTabbedPane panel = new JTabbedPane();
    panel.addTab("System", initializeEnvironmentInfoPanel());
    panel.addTab("Domain", initializeDomainModelPanel());

    return panel;
  }

  private JPanel initializeDomainModelPanel() {
    JPanel panel = new JPanel(Layouts.borderLayout());
    JTable table = new JTable(model.domainTableModel());
    table.setRowSorter(new TableRowSorter<>(model.domainTableModel()));
    JScrollPane scroller = new JScrollPane(table);

    JPanel refreshPanel = new JPanel(Layouts.flowLayout(FlowLayout.RIGHT));
    refreshPanel.add(Components.button(control(model::refreshDomainList))
            .caption("Refresh")
            .build());
    panel.add(refreshPanel, BorderLayout.NORTH);
    panel.add(scroller, BorderLayout.CENTER);

    return panel;
  }

  private JScrollPane initializeEnvironmentInfoPanel() throws RemoteException {
    return Components.textArea()
            .autoscrolls(false)
            .editable(false)
            .lineWrap(true)
            .wrapStyleWord(true)
            .initialValue(model.environmentInfo())
            .scrollPane()
            .build();
  }

  private JTextField initializeConnectionCountField() {
    return Components.integerField()
            .columns(4)
            .editable(false)
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValueObserver(model.connectionCountObserver())
            .build();
  }

  private JTextField initializeMemoryField() {
    return Components.textField()
            .columns(8)
            .editable(false)
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValueObserver(model.memoryUsageObserver())
            .build();
  }

  private JComboBox<Object> initializeLogLevelField() {
    DefaultComboBoxModel<Object> comboModel = new DefaultComboBoxModel<>(model.logLevels().toArray());

    return Components.comboBox(comboModel, model.logLevelValue())
            .build();
  }

  private void setColors(JFreeChart chart) {
    ChartUtil.linkColors(this, chart);
  }

  private void resetZoom() {
    boolean isSync = synchronizedZoomState.get();
    synchronizedZoomState.set(false);
    requestsPerSecondChartPanel.restoreAutoBounds();
    memoryUsageChartPanel.restoreAutoBounds();
    connectionCountChartPanel.restoreAutoBounds();
    systemLoadChartPanel.restoreAutoBounds();
    gcEventsChartPanel.restoreAutoBounds();
    threadCountChartPanel.restoreAutoBounds();
    synchronizedZoomState.set(isSync);
  }

  private void bindEvents() {
    ZoomSyncListener zoomSyncListener = new ZoomSyncListener();
    requestsPerSecondChart.getXYPlot().getDomainAxis().addChangeListener(zoomSyncListener);
    memoryUsageChart.getXYPlot().getDomainAxis().addChangeListener(zoomSyncListener);
    connectionCountChart.getXYPlot().getDomainAxis().addChangeListener(zoomSyncListener);
    systemLoadChart.getXYPlot().getDomainAxis().addChangeListener(zoomSyncListener);
    gcEventsChart.getXYPlot().getDomainAxis().addChangeListener(zoomSyncListener);
    threadCountChart.getXYPlot().getDomainAxis().addChangeListener(zoomSyncListener);
  }

  private final class ZoomSyncListener implements AxisChangeListener {

    private final List<JFreeChart> performanceCharts;

    private ZoomSyncListener() {
      performanceCharts = asList(requestsPerSecondChart, memoryUsageChart, connectionCountChart,
              systemLoadChart, gcEventsChart, threadCountChart);
    }

    @Override
    public void axisChanged(AxisChangeEvent event) {
      if (synchronizedZoomState.get()) {
        DateAxis dateAxis = (DateAxis) event.getAxis();
        performanceCharts.forEach(chart -> {
          if (!chart.equals(event.getChart())) {
            ValueAxis domainAxis = chart.getXYPlot().getDomainAxis();
            if (!domainAxis.getRange().equals(dateAxis.getRange())) {
              domainAxis.setRange(dateAxis.getRange());
            }
          }
        });
      }
    }
  }
}
