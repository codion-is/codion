/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
    JPanel serverPanel = Components.panel(Layouts.flowLayout(FlowLayout.LEFT))
            .add(new JLabel("Connections", SwingConstants.RIGHT))
            .add(createConnectionCountField())
            .add(new JLabel("limit", SwingConstants.RIGHT))
            .add(Components.integerSpinner(model.connectionLimitValue())
                    .columns(SPINNER_COLUMNS)
                    .build())
            .add(new JLabel("Mem. usage", SwingConstants.RIGHT))
            .add(createMemoryField())
            .add(new JLabel("Logging", SwingConstants.RIGHT))
            .add(createLogLevelField())
            .build();

    JPanel shutdownBasePanel = Components.panel(Layouts.flowLayout(FlowLayout.CENTER))
            .add(Components.button(control(this::shutdownServer))
                    .caption("Shutdown")
                    .build(), BorderLayout.EAST)
            .build();

    JPanel northPanel = Components.panel(Layouts.borderLayout())
            .border(BorderFactory.createTitledBorder("Server"))
            .add(serverPanel, BorderLayout.CENTER)
            .add(shutdownBasePanel, BorderLayout.EAST)
            .build();

    JTabbedPane tabbedPane = Components.tabbedPane()
            .tab("Performance", createPerformancePanel())
            .tab("Database", new DatabaseMonitorPanel(model.databaseMonitor()))
            .tab("Clients/Users", new ClientUserMonitorPanel(model.clientMonitor()))
            .tab("Server", createServerPanel())
            .build();

    setLayout(new BorderLayout());
    add(northPanel, BorderLayout.NORTH);
    add(tabbedPane, BorderLayout.CENTER);
  }

  private JPanel createPerformancePanel() {
    JPanel intervalPanel = Components.panel(Layouts.borderLayout())
            .add(new JLabel("Update interval (s)"), BorderLayout.WEST)
            .add(Components.integerSpinner(model.updateIntervalValue())
                    .minimum(1)
                    .columns(SPINNER_COLUMNS)
                    .editable(false)
                    .build(), BorderLayout.CENTER)
            .build();

    JPanel chartsPanel = Components.panel(Layouts.borderLayout())
            .add(intervalPanel, BorderLayout.CENTER)
            .add(Components.button(control(model::clearStatistics))
                    .caption("Clear")
                    .build(), BorderLayout.EAST)
            .build();

    JPanel zoomPanel = Components.panel(Layouts.borderLayout())
            .add(Components.checkBox(synchronizedZoomState)
                    .caption("Synchronize zoom")
                    .build(), BorderLayout.CENTER)
            .add(Components.button(control(this::resetZoom))
                    .caption("Reset zoom")
                    .build(), BorderLayout.EAST)
            .build();

    JPanel controlPanel = Components.panel(Layouts.flexibleGridLayout(1, 2))
            .border(BorderFactory.createTitledBorder("Charts"))
            .add(chartsPanel)
            .add(zoomPanel)
            .build();

    JPanel chartPanelLeft = Components.panel(Layouts.gridLayout(3, 1))
            .add(requestsPerSecondChartPanel)
            .add(connectionCountChartPanel)
            .add(systemLoadChartPanel)
            .build();
    JPanel chartPanelRight = Components.panel(Layouts.gridLayout(3, 1))
            .add(threadCountChartPanel)
            .add(memoryUsageChartPanel)
            .add(gcEventsChartPanel)
            .build();
    JPanel chartPanel = Components.panel(Layouts.gridLayout(1, 2))
            .border(BorderFactory.createEtchedBorder())
            .add(chartPanelLeft)
            .add(chartPanelRight)
            .build();

    JPanel controlPanelBase = Components.panel(Layouts.borderLayout())
            .add(controlPanel, BorderLayout.WEST)
            .build();

    JPanel overviewPanel = Components.panel(Layouts.borderLayout())
            .add(controlPanelBase, BorderLayout.SOUTH)
            .add(chartPanel, BorderLayout.CENTER)
            .build();

    return Components.panel(Layouts.borderLayout())
            .add(overviewPanel, BorderLayout.CENTER)
            .build();
  }

  private JTabbedPane createServerPanel() throws RemoteException {
    return Components.tabbedPane()
            .tab("System", createEnvironmentInfoPanel())
            .tab("Entities", createEntityPanel())
            .tab("Operations", createOperationPanel())
            .tab("Reports", createReportPanel())
            .build();
  }

  private JPanel createOperationPanel() {
    JTable table = new JTable(model.operationTableModel());
    table.setRowSorter(new TableRowSorter<>(model.operationTableModel()));

    JPanel refreshPanel = Components.panel(Layouts.flowLayout(FlowLayout.RIGHT))
            .add(Components.button(control(model::refreshOperationList))
                    .caption("Refresh")
                    .build())
            .build();

    return Components.panel(Layouts.borderLayout())
            .add(refreshPanel, BorderLayout.NORTH)
            .add(new JScrollPane(table), BorderLayout.CENTER)
            .build();
  }

  private JPanel createReportPanel() {
    JTable table = new JTable(model.reportTableModel());
    table.setRowSorter(new TableRowSorter<>(model.reportTableModel()));

    JPanel clearCacheAndRefreshPanel = Components.panel(Layouts.flowLayout(FlowLayout.RIGHT))
            .add(Components.button(control(model::clearReportCache))
                    .caption("Clear Cache")
                    .build())
            .add(Components.button(control(model::refreshReportList))
                    .caption("Refresh")
                    .build())
            .build();

    return Components.panel(Layouts.borderLayout())
            .add(clearCacheAndRefreshPanel, BorderLayout.NORTH)
            .add(new JScrollPane(table), BorderLayout.CENTER)
            .build();
  }

  private JPanel createEntityPanel() {
    JTable table = new JTable(model.domainTableModel());
    table.setRowSorter(new TableRowSorter<>(model.domainTableModel()));

    JPanel refreshPanel = Components.panel(Layouts.flowLayout(FlowLayout.RIGHT))
            .add(Components.button(control(model::refreshDomainList))
                    .caption("Refresh")
                    .build())
            .build();

    return Components.panel(Layouts.borderLayout())
            .add(refreshPanel, BorderLayout.NORTH)
            .add(new JScrollPane(table), BorderLayout.CENTER)
            .build();
  }

  private JScrollPane createEnvironmentInfoPanel() throws RemoteException {
    return Components.textArea()
            .autoscrolls(false)
            .editable(false)
            .lineWrap(true)
            .wrapStyleWord(true)
            .initialValue(model.environmentInfo())
            .scrollPane()
            .build();
  }

  private JTextField createConnectionCountField() {
    return Components.integerField()
            .columns(4)
            .editable(false)
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValueObserver(model.connectionCountObserver())
            .build();
  }

  private JTextField createMemoryField() {
    return Components.textField()
            .columns(8)
            .editable(false)
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValueObserver(model.memoryUsageObserver())
            .build();
  }

  private JComboBox<Object> createLogLevelField() {
    return Components.comboBox(new DefaultComboBoxModel<Object>(model.logLevels().toArray()), model.logLevelValue()).build();
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
