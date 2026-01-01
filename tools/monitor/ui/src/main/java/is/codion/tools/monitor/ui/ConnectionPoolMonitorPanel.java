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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor.ui;

import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.tools.monitor.model.ConnectionPoolMonitor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldSize;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEtchedBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingConstants.CENTER;

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
					.formatter();
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

	private final JTextField resetTimeField = stringField()
					.columns(RESET_FIELD_COLUMNS)
					.build();
	private final JTextField poolSizeField = stringField()
					.editable(false)
					.horizontalAlignment(CENTER)
					.build();
	private final JTextField createdField = stringField()
					.editable(false)
					.horizontalAlignment(CENTER)
					.build();
	private final JTextField destroyedField = stringField()
					.editable(false)
					.horizontalAlignment(CENTER)
					.build();
	private final JTextField requestedField = stringField()
					.editable(false)
					.horizontalAlignment(CENTER)
					.build();
	private final JTextField failedField = stringField()
					.editable(false)
					.horizontalAlignment(CENTER)
					.build();

	/**
	 * Instantiates a new ConnectionPoolMonitorPanel
	 * @param model the ConnectionPoolMonitor to base this panel on
	 */
	public ConnectionPoolMonitorPanel(ConnectionPoolMonitor model) {
		this.model = requireNonNull(model);
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
		model.statisticsUpdated().addListener(this::updateView);
	}

	private JPanel configurationPanel() {
		JPanel configBase = flexibleGridLayoutPanel(1, 0)
						.border(createTitledBorder("Configuration"))
						.add(borderLayoutPanel()
										.west(new JLabel("Mininum size"))
										.center(integerSpinner()
														.link(model.minimumPoolSize())
														.columns(3)
														.editable(false)))
						.add(borderLayoutPanel()
										.west(new JLabel("Maximum size"))
										.center(integerSpinner()
														.link(model.maximumPoolSize())
														.columns(3)
														.editable(false)))
						.add(borderLayoutPanel()
										.west(new JLabel("Checkout timeout (ms)"))
										.center(integerSpinner()
														.link(model.maximumCheckOutTime())
														.stepSize(1000)
														.columns(6)
														.editable(false)))
						.add(borderLayoutPanel()
										.west(new JLabel("Idle timeout (ms)"))
										.center(integerSpinner()
														.link(model.pooledConnectionTimeout())
														.stepSize(1000)
														.columns(6)
														.groupingUsed(true)
														.editable(false)))
						.add(borderLayoutPanel()
										.west(new JLabel("Cleanup interval (ms)"))
										.center(integerSpinner()
														.link(model.poolCleanupInterval())
														.stepSize(1000)
														.columns(6)
														.groupingUsed(true)
														.editable(false)))
						.build();

		return flowLayoutPanel(SwingConstants.LEADING)
						.add(configBase)
						.build();
	}

	private JPanel chartPanel() {
		return panel()
						.layout(new GridLayout(2, 2))
						.border(createEtchedBorder())
						.add(requestsPerSecondChartPanel)
						.add(inPoolChartPanel)
						.add(checkOutTimePanel)
						.add(inPoolSnapshotChartPanel)
						.build();
	}

	private JPanel southPanel() {
		JPanel chartConfig = flexibleGridLayoutPanel(1, 4)
						.border(createTitledBorder("Charts"))
						.add(new JLabel("Update interval (s)"))
						.add(integerSpinner()
										.link(model.updateInterval())
										.minimum(1)
										.columns(SPINNER_COLUMNS)
										.editable(false))
						.add(checkBox()
										.link(model.collectSnapshotStatistics())
										.text("Snapshot")
										.maximumSize(preferredTextFieldSize()))
						.add(checkBox()
										.link(model.collectCheckOutTimes())
										.text("Check out times")
										.maximumSize(preferredTextFieldSize()))
						.add(button()
										.control(command(model::clearStatistics))
										.text("Clear")
										.maximumSize(preferredTextFieldSize()))
						.build();

		return borderLayoutPanel()
						.west(chartConfig)
						.center(statisticsPanel())
						.build();
	}

	private JPanel statisticsPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder("Statistics"))
						.center(flexibleGridLayoutPanel(1, 0)
										.add(borderLayoutPanel()
														.west(new JLabel("Connections"))
														.center(poolSizeField))
										.add(borderLayoutPanel()
														.west(new JLabel("Requested"))
														.center(requestedField))
										.add(borderLayoutPanel()
														.west(new JLabel("Failed"))
														.center(failedField))
										.add(borderLayoutPanel()
														.west(new JLabel("Created"))
														.center(createdField))
										.add(borderLayoutPanel()
														.west(new JLabel("Destroyed"))
														.center(destroyedField))
										.add(borderLayoutPanel()
														.west(new JLabel("Since"))
														.center(resetTimeField)))
						.east(button()
										.control(command(model::resetStatistics))
										.text("Reset"))
						.build();
	}
}
