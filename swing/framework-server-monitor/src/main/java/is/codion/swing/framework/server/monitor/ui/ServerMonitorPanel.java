/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.state.State;
import is.codion.framework.server.EntityServerAdmin.DomainEntityDefinition;
import is.codion.framework.server.EntityServerAdmin.DomainOperation;
import is.codion.framework.server.EntityServerAdmin.DomainReport;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.framework.server.monitor.ServerMonitor;
import is.codion.swing.framework.server.monitor.ServerMonitor.DomainColumns;
import is.codion.swing.framework.server.monitor.ServerMonitor.OperationColumns;
import is.codion.swing.framework.server.monitor.ServerMonitor.ReportColumns;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;

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
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.commandControl;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEtchedBorder;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A ServerMonitorPanel
 */
public final class ServerMonitorPanel extends JPanel {

	private static final int SPINNER_COLUMNS = 3;
	private static final String DOMAIN = "Domain";

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
		this.model = requireNonNull(model);
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
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			model.shutdownServer();
		}
	}

	private void initializeUI() throws RemoteException {
		JPanel serverPanel = flowLayoutPanel(FlowLayout.LEFT)
						.add(new JLabel("Connections", SwingConstants.RIGHT))
						.add(createConnectionCountField())
						.add(new JLabel("limit", SwingConstants.RIGHT))
						.add(integerSpinner(model.connectionLimit())
										.columns(SPINNER_COLUMNS)
										.build())
						.add(new JLabel("Mem. usage", SwingConstants.RIGHT))
						.add(createMemoryField())
						.add(new JLabel("Logging", SwingConstants.RIGHT))
						.add(createLogLevelField())
						.build();

		JPanel northPanel = borderLayoutPanel()
						.border(createTitledBorder("Server"))
						.centerComponent(serverPanel)
						.eastComponent(button(commandControl(this::shutdownServer))
										.text("Shutdown")
										.build())
						.build();

		JTabbedPane tabbedPane = tabbedPane()
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
		JPanel intervalPanel = borderLayoutPanel()
						.westComponent(new JLabel("Update interval (s)"))
						.centerComponent(integerSpinner(model.updateInterval())
										.minimum(1)
										.columns(SPINNER_COLUMNS)
										.editable(false)
										.build())
						.build();

		JPanel chartsPanel = borderLayoutPanel()
						.centerComponent(intervalPanel)
						.eastComponent(button(commandControl(model::clearStatistics))
										.text("Clear")
										.build())
						.build();

		JPanel zoomPanel = borderLayoutPanel()
						.centerComponent(checkBox(synchronizedZoomState)
										.text("Synchronize zoom")
										.build())
						.eastComponent(button(commandControl(this::resetZoom))
										.text("Reset zoom")
										.build())
						.build();

		JPanel controlPanel = flexibleGridLayoutPanel(1, 2)
						.border(createTitledBorder("Charts"))
						.add(chartsPanel)
						.add(zoomPanel)
						.build();

		JPanel chartPanelLeft = gridLayoutPanel(3, 1)
						.add(requestsPerSecondChartPanel)
						.add(connectionCountChartPanel)
						.add(systemLoadChartPanel)
						.build();
		JPanel chartPanelRight = gridLayoutPanel(3, 1)
						.add(threadCountChartPanel)
						.add(memoryUsageChartPanel)
						.add(gcEventsChartPanel)
						.build();
		JPanel chartPanel = gridLayoutPanel(1, 2)
						.border(createEtchedBorder())
						.add(chartPanelLeft)
						.add(chartPanelRight)
						.build();

		JPanel controlPanelBase = borderLayoutPanel()
						.westComponent(controlPanel)
						.build();

		JPanel overviewPanel = borderLayoutPanel()
						.southComponent(controlPanelBase)
						.centerComponent(chartPanel)
						.build();

		return borderLayoutPanel()
						.centerComponent(overviewPanel)
						.build();
	}

	private JTabbedPane createServerPanel() throws RemoteException {
		return tabbedPane()
						.tab("System", createEnvironmentInfoPanel())
						.tab("Entities", createEntityPanel())
						.tab("Operations", createOperationPanel())
						.tab("Reports", createReportPanel())
						.build();
	}

	private JPanel createOperationPanel() {
		FilterTable<DomainOperation, OperationColumns.Id> table =
						FilterTable.builder(model.operationTableModel(), createOperationColumns())
										.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
										.build();

		JPanel refreshPanel = flowLayoutPanel(FlowLayout.RIGHT)
						.add(button(commandControl(model::refreshOperationList))
										.text("Refresh")
										.build())
						.build();

		return borderLayoutPanel()
						.northComponent(refreshPanel)
						.centerComponent(new JScrollPane(table))
						.build();
	}

	private JPanel createReportPanel() {
		FilterTable<DomainReport, ReportColumns.Id> table =
						FilterTable.builder(model.reportTableModel(), createReportColumns())
										.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
										.build();

		JPanel clearCacheAndRefreshPanel = flowLayoutPanel(FlowLayout.RIGHT)
						.add(button(commandControl(model::clearReportCache))
										.text("Clear Cache")
										.build())
						.add(button(commandControl(model::refreshReportList))
										.text("Refresh")
										.build())
						.build();

		return borderLayoutPanel()
						.northComponent(clearCacheAndRefreshPanel)
						.centerComponent(new JScrollPane(table))
						.build();
	}

	private JPanel createEntityPanel() {
		FilterTable<DomainEntityDefinition, DomainColumns.Id> table =
						FilterTable.builder(model.domainTableModel(), createDomainColumns())
										.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
										.build();

		JPanel refreshPanel = flowLayoutPanel(FlowLayout.RIGHT)
						.add(button(commandControl(model::refreshDomainList))
										.text("Refresh")
										.build())
						.build();

		return borderLayoutPanel()
						.northComponent(refreshPanel)
						.centerComponent(new JScrollPane(table))
						.build();
	}

	private JScrollPane createEnvironmentInfoPanel() throws RemoteException {
		return textArea()
						.autoscrolls(false)
						.editable(false)
						.lineWrap(true)
						.wrapStyleWord(true)
						.initialValue(model.environmentInfo())
						.scrollPane()
						.build();
	}

	private JTextField createConnectionCountField() {
		return integerField()
						.columns(4)
						.editable(false)
						.horizontalAlignment(SwingConstants.CENTER)
						.link(model.connectionCount())
						.build();
	}

	private JTextField createMemoryField() {
		return stringField()
						.columns(8)
						.editable(false)
						.horizontalAlignment(SwingConstants.CENTER)
						.link(model.memoryUsage())
						.build();
	}

	private JComboBox<Object> createLogLevelField() {
		return comboBox(new DefaultComboBoxModel<>(model.logLevels().toArray()), model.logLevel()).build();
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

	private static List<FilterTableColumn<ReportColumns.Id>> createReportColumns() {
		return Arrays.asList(
						FilterTableColumn.builder(ReportColumns.Id.DOMAIN)
										.headerValue(DOMAIN)
										.build(),
						FilterTableColumn.builder(ReportColumns.Id.REPORT)
										.headerValue("Report")
										.build(),
						FilterTableColumn.builder(ReportColumns.Id.TYPE)
										.headerValue("Type")
										.build(),
						FilterTableColumn.builder(ReportColumns.Id.PATH)
										.headerValue("Path")
										.build(),
						FilterTableColumn.builder(ReportColumns.Id.CACHED)
										.headerValue("Cached")
										.build());
	}

	private static List<FilterTableColumn<DomainColumns.Id>> createDomainColumns() {
		return Arrays.asList(
						FilterTableColumn.builder(DomainColumns.Id.DOMAIN)
										.headerValue(DOMAIN)
										.build(),
						FilterTableColumn.builder(DomainColumns.Id.ENTITY)
										.headerValue("Entity")
										.build(),
						FilterTableColumn.builder(DomainColumns.Id.TABLE)
										.headerValue("Table")
										.build());
	}

	private static List<FilterTableColumn<OperationColumns.Id>> createOperationColumns() {
		return Arrays.asList(
						FilterTableColumn.builder(OperationColumns.Id.DOMAIN)
										.headerValue(DOMAIN)
										.build(),
						FilterTableColumn.builder(OperationColumns.Id.TYPE)
										.headerValue("Type")
										.build(),
						FilterTableColumn.builder(OperationColumns.Id.OPERATION)
										.headerValue("Operation")
										.build(),
						FilterTableColumn.builder(OperationColumns.Id.CLASS)
										.headerValue("Class")
										.build());
	}
}
