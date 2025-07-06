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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.user.User;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.frame.Frames;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.LoadTest.Scenario;
import is.codion.tools.loadtest.model.LoadTestModel;
import is.codion.tools.loadtest.model.LoadTestModel.ApplicationRow;
import is.codion.tools.loadtest.model.LoadTestModel.ApplicationRow.ColumnId;
import is.codion.tools.loadtest.randomizer.ItemRandomizer.RandomItem;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.icon.Logos.logoTransparent;
import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.enableLookAndFeel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.window.Windows.screenSizeRatio;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.*;
import static org.jfree.chart.ChartFactory.createXYStepChart;

/**
 * A default UI component for the LoadTestModel class.
 * @param <T> the load test application type
 * @see #loadTestPanel(LoadTestModel)
 * @see LoadTestModel
 */
public final class LoadTestPanel<T> extends JPanel {

	private static final int DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
	private static final double DEFAULT_SCREEN_SIZE_RATIO = 0.75;
	private static final int USER_COLUMNS = 6;
	private static final int SMALL_TEXT_FIELD_COLUMNS = 3;
	private static final int SPINNER_STEP_SIZE = 10;
	private static final double RESIZE_WEIGHT = 0.8;
	private static final String LOOK_AND_FEEL_PROPERTY = ".lookAndFeel";
	private static final NumberFormat DURATION_FORMAT = NumberFormat.getIntegerInstance();
	private static final String DEFAULT_TITLE = "Codion LoadTest";
	private static final NumberFormat MEMORY_USAGE_FORMAT = NumberFormat.getIntegerInstance();
	private static final Runtime RUNTIME = Runtime.getRuntime();

	private final LoadTestModel<T> loadTestModel;
	private final LoadTest<T> loadTest;
	private final JPanel scenarioBase = gridLayoutPanel(0, 1).build();

	static {
		FilterTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
		enableLookAndFeel(LoadTestPanel.class.getName() + LOOK_AND_FEEL_PROPERTY, FlatDarculaLaf.class);
	}

	private boolean exiting;

	private LoadTestPanel(LoadTestModel<T> loadTestModel) {
		this.loadTestModel = requireNonNull(loadTestModel);
		this.loadTest = loadTestModel.loadTest();
		this.loadTestModel.applicationTableModel().items().refresher().exception().addConsumer(this::displayException);
		initializeUI();
	}

	/**
	 * @return the load test model this panel is based on
	 */
	public LoadTestModel<T> model() {
		return loadTestModel;
	}

	/**
	 * Displays this LoadTestPanel in a frame on the Event Dispatch Thread.
	 */
	public void run() {
		SwingUtilities.invokeLater(this::showFrame);
	}

	/**
	 * Instantiates a new {@link LoadTestPanel} instance.
	 * @param loadTestModel the LoadTestModel to base this panel on
	 * @param <T> the load test application type
	 * @return a new {@link LoadTestPanel} instance.
	 */
	public static <T> LoadTestPanel<T> loadTestPanel(LoadTestModel<T> loadTestModel) {
		return new LoadTestPanel<>(loadTestModel);
	}

	/**
	 * Shows a frame containing this load test panel
	 * @return the frame
	 */
	private JFrame showFrame() {
		return Frames.builder()
						.component(this)
						.icon(logoTransparent())
						.menuBar(menu(createMainMenuControls()).buildMenuBar())
						.title(loadTest.name().orElse(DEFAULT_TITLE))
						.defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
						.onClosing(windowEvent -> exit())
						.size(screenSizeRatio(DEFAULT_SCREEN_SIZE_RATIO))
						.centerFrame(true)
						.show();
	}

	private Controls createMainMenuControls() {
		return Controls.builder()
						.control(Controls.builder()
										.caption("File")
										.mnemonic('F')
										.control(Control.builder()
														.command(this::exit)
														.caption("Exit")
														.mnemonic('X')))
						.control(Controls.builder()
										.caption("View")
										.mnemonic('V')
										.control(Dialogs.select()
														.lookAndFeel()
														.owner(this)
														.createControl(LoadTestPanel::lookAndFeelSelected))
										.control(Control.builder()
														.toggle(loadTestModel.collectChartData())
														.caption("Collect chart data"))
										.control(Control.builder()
														.command(loadTestModel::clearCharts)
														.caption("Clear charts")))
						.build();
	}

	private void initializeUI() {
		setLayout(borderLayout());
		int gap = Layouts.GAP.getOrThrow();
		setBorder(createEmptyBorder(gap, gap, 0, gap));
		add(createCenterPanel(), BorderLayout.CENTER);
		add(createSouthPanel(), BorderLayout.SOUTH);
	}

	private ItemRandomizerPanel<Scenario<T>> createScenarioPanel() {
		ItemRandomizerPanel<Scenario<T>> panel = new ItemRandomizerPanel<>(loadTest.scenarioChooser());
		panel.setBorder(createTitledBorder("Usage scenarios"));
		panel.selectedItems().addConsumer(this::onScenarioSelectionChanged);

		return panel;
	}

	private JPanel createAddRemoveApplicationPanel() {
		return borderLayoutPanel()
						.westComponent(button()
										.control(Control.builder()
														.command(loadTest::removeApplicationBatch)
														.caption("-")
														.description("Remove application batch"))
										.build())
						.centerComponent(integerField()
										.editable(false)
										.focusable(false)
										.horizontalAlignment(SwingConstants.CENTER)
										.columns(5)
										.link(loadTest.applicationCount())
										.build())
						.eastComponent(button()
										.control(Control.builder()
														.command(loadTest::addApplicationBatch)
														.caption("+")
														.description("Add application batch"))
										.build())
						.build();
	}

	private JPanel createCenterPanel() {
		return borderLayoutPanel()
						.centerComponent(tabbedPane()
										.tab("Applications", createApplicationsPanel())
										.tab("Scenarios", borderLayoutPanel()
														.westComponent(createScenarioPanel())
														.centerComponent(scenarioBase)
														.build())
										.tab("Overview", borderLayoutPanel()
														.centerComponent(splitPane()
																		.orientation(JSplitPane.VERTICAL_SPLIT)
																		.oneTouchExpandable(true)
																		.topComponent(createScenarioOverviewChartPanel())
																		.bottomComponent(createSouthChartPanel())
																		.resizeWeight(RESIZE_WEIGHT)
																		.build())
														.build())
										.build())
						.build();
	}

	private JPanel createApplicationsPanel() {
		return borderLayoutPanel()
						.northComponent(borderLayoutPanel()
										.centerComponent(flowLayoutPanel(FlowLayout.LEADING)
														.add(new JLabel("Batch size"))
														.add(integerSpinner()
																		.link(loadTest.applicationBatchSize())
																		.editable(false)
																		.columns(SMALL_TEXT_FIELD_COLUMNS)
																		.toolTipText("Application batch size")
																		.build())
														.add(createAddRemoveApplicationPanel())
														.add(createUserPanel())
														.add(new JLabel("Min. think time", SwingConstants.CENTER))
														.add(integerSpinner()
																		.link(loadTest.minimumThinkTime())
																		.stepSize(SPINNER_STEP_SIZE)
																		.columns(SMALL_TEXT_FIELD_COLUMNS)
																		.build())
														.add(new JLabel("Max. think time", SwingConstants.CENTER))
														.add(integerSpinner()
																		.link(loadTest.maximumThinkTime())
																		.stepSize(SPINNER_STEP_SIZE)
																		.columns(SMALL_TEXT_FIELD_COLUMNS)
																		.build())
														.add(toggleButton()
																		.link(loadTest.paused())
																		.text("Pause")
																		.mnemonic('P')
																		.build())
														.build())
										.eastComponent(checkBox()
														.link(loadTestModel.autoRefreshApplications())
														.text("Automatic refresh")
														.build())
										.build())
						.centerComponent(scrollPane(createApplicationsTable()).build())
						.build();
	}

	private JPanel createUserPanel() {
		User user = loadTest.user().get();
		JTextField usernameField = stringField()
						.value(user == null ? null : user.username())
						.columns(USER_COLUMNS)
						.editable(false)
						.build();
		loadTest.user().addConsumer(u -> usernameField.setText(u.username()));

		return flexibleGridLayoutPanel(1, 3)
						.add(new JLabel("User"))
						.add(usernameField)
						.add(new JButton(Control.builder()
										.command(this::setUser)
										.caption("...")
										.description("Set the application user")
										.build()))
						.build();
	}

	private void setUser() {
		User user = loadTest.user().get();
		try {
			loadTest.user().set(Dialogs.login()
							.owner(LoadTestPanel.this)
							.title("User")
							.defaultUser(user == null ? null : User.user(user.username()))
							.show());
		}
		catch (CancelException ignored) {/**/}
	}

	private JTabbedPane createScenarioOverviewChartPanel() {
		return tabbedPane()
						.tab("Scenarios run", createScenarioChartPanel())
						.tab("Failed runs", createFailureChartPanel())
						.build();
	}

	private ChartPanel createScenarioChartPanel() {
		JFreeChart scenarioChart = createXYStepChart(null, null, null,
						loadTestModel.scenarioDataset(), PlotOrientation.VERTICAL, true, true, false);
		setColors(scenarioChart);
		ChartPanel scenarioChartPanel = new ChartPanel(scenarioChart);
		scenarioChartPanel.setBorder(createTitledBorder("Scenarios run per second"));

		return scenarioChartPanel;
	}

	private ChartPanel createFailureChartPanel() {
		JFreeChart failureChart = createXYStepChart(null, null, null,
						loadTestModel.scenarioFailureDataset(), PlotOrientation.VERTICAL, true, true, false);
		setColors(failureChart);
		ChartPanel failureChartPanel = new ChartPanel(failureChart);
		failureChartPanel.setBorder(createTitledBorder("Scenario run failures per second"));

		return failureChartPanel;
	}

	private JPanel createSouthChartPanel() {
		return gridLayoutPanel(1, 4)
						.add(createMemoryUsageChartPanel())
						.add(createSystemLoadChartPanel())
						.add(createThinkTimeChartPanel())
						.add(createNumberOfApplicationsChartPanel())
						.build();
	}

	private ChartPanel createMemoryUsageChartPanel() {
		JFreeChart memoryUsageChart = createXYStepChart(null, null, null,
						loadTestModel.memoryUsageDataset(), PlotOrientation.VERTICAL, true, true, false);
		setColors(memoryUsageChart);
		ChartPanel memoryUsageChartPanel = new ChartPanel(memoryUsageChart);
		memoryUsageChartPanel.setBorder(createTitledBorder("Memory usage (MB)"));

		return memoryUsageChartPanel;
	}

	private ChartPanel createSystemLoadChartPanel() {
		JFreeChart systemLoadChart = createXYStepChart(null, null, null,
						loadTestModel.systemLoadDataset(), PlotOrientation.VERTICAL, true, true, false);
		setColors(systemLoadChart);
		systemLoadChart.getXYPlot().getRangeAxis().setRange(0, 100);
		ChartPanel systemLoadChartPanel = new ChartPanel(systemLoadChart);
		systemLoadChartPanel.setBorder(createTitledBorder("System load"));

		return systemLoadChartPanel;
	}

	private ChartPanel createThinkTimeChartPanel() {
		JFreeChart thinkTimeChart = createXYStepChart(null, null, null,
						loadTestModel.thinkTimeDataset(), PlotOrientation.VERTICAL, true, true, false);
		setColors(thinkTimeChart);
		ChartPanel thinkTimeChartPanel = new ChartPanel(thinkTimeChart);
		thinkTimeChartPanel.setBorder(createTitledBorder("Think time (ms)"));

		return thinkTimeChartPanel;
	}

	private ChartPanel createNumberOfApplicationsChartPanel() {
		JFreeChart numberOfApplicationsChart = createXYStepChart(null, null, null,
						loadTestModel.numberOfApplicationsDataset(), PlotOrientation.VERTICAL, true, true, false);
		setColors(numberOfApplicationsChart);
		ChartPanel numberOfApplicationsChartPanel = new ChartPanel(numberOfApplicationsChart);
		numberOfApplicationsChartPanel.setBorder(createTitledBorder("Application count"));

		return numberOfApplicationsChartPanel;
	}

	private FilterTable<ApplicationRow, ColumnId> createApplicationsTable() {
		FilterTableModel<ApplicationRow, ColumnId> tableModel = model().applicationTableModel();

		return FilterTable.builder()
						.model(tableModel)
						.columns(createApplicationTableModelColumns())
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.doubleClick(command(this::viewException))
						.scrollToSelectedItem(false)
						.cellRenderer(ColumnId.DURATION, FilterTableCellRenderer.builder(Integer.class)
										.string(duration -> duration == null ? null : DURATION_FORMAT.format(duration))
										.build())
						.popupMenuControls(table -> Controls.builder()
										.control(Control.builder()
														.command(table.model().items()::refresh)
														.caption("Refresh")
														.enabled(model().autoRefreshApplications().not()))
										.separator()
										.control(Control.builder()
														.command(model()::removeSelectedApplications)
														.caption("Remove selected"))
										.separator()
										.control(Controls.builder()
														.caption("Columns")
														.control(table.createToggleColumnsControls())
														.control(table.createResetColumnsControl())
														.control(table.createSelectAutoResizeModeControl()))
										.build())
						.build();
	}

	private static List<FilterTableColumn<ColumnId>> createApplicationTableModelColumns() {
		return Arrays.asList(
						FilterTableColumn.builder(ColumnId.NAME)
										.headerValue("Name")
										.build(),
						FilterTableColumn.builder(ColumnId.USERNAME)
										.headerValue("User")
										.build(),
						FilterTableColumn.builder(ColumnId.SCENARIO)
										.headerValue("Scenario")
										.build(),
						FilterTableColumn.builder(ColumnId.SUCCESSFUL)
										.headerValue("Success")
										.build(),
						FilterTableColumn.builder(ColumnId.DURATION)
										.headerValue("Duration (μs)")
										.build(),
						FilterTableColumn.builder(ColumnId.EXCEPTION)
										.headerValue("Exception")
										.build(),
						FilterTableColumn.builder(ColumnId.MESSAGE)
										.headerValue("Message")
										.build(),
						FilterTableColumn.builder(ColumnId.CREATED)
										.headerValue("Created")
										.build()
		);
	}

	private void viewException() {
		model().applicationTableModel().selection().item().optional()
						.map(LoadTestPanel::exception)
						.ifPresent(this::displayException);
	}

	private void onScenarioSelectionChanged(List<RandomItem<Scenario<T>>> selectedScenarios) {
		scenarioBase.removeAll();
		selectedScenarios.forEach(scenario -> scenarioBase.add(createScenarioPanel(scenario.item())));
		validate();
		repaint();
	}

	private JPanel createScenarioPanel(Scenario<T> item) {
		return borderLayoutPanel()
						.centerComponent(tabbedPane()
										.tab("Duration", createScenarioDurationChartPanel(item))
										.tab("Exceptions", createScenarioExceptionsPanel(item))
										.build())
						.build();
	}

	private ChartPanel createScenarioDurationChartPanel(Scenario<T> scenario) {
		JFreeChart scenarioDurationChart = createXYStepChart(null,
						null, null, loadTestModel.scenarioDurationDataset(scenario.name()),
						PlotOrientation.VERTICAL, true, true, false);
		setColors(scenarioDurationChart);
		ChartPanel scenarioDurationChartPanel = new ChartPanel(scenarioDurationChart);
		scenarioDurationChartPanel.setBorder(createEtchedBorder());
		DeviationRenderer renderer = new DeviationRenderer();
		renderer.setDefaultShapesVisible(false);
		scenarioDurationChart.getXYPlot().setRenderer(renderer);

		return scenarioDurationChartPanel;
	}

	private JPanel createScenarioExceptionsPanel(Scenario<T> scenario) {
		JTextArea exceptionsArea = textArea()
						.editable(false)
						.build();
		JButton refreshButton = button()
						.control(Control.builder()
										.command(new RefreshExceptionsCommand(exceptionsArea, scenario))
										.caption("Refresh"))
						.build();
		refreshButton.doClick();

		JButton clearButton = button()
						.control(Control.builder()
										.command(new ClearExceptionsCommand(exceptionsArea, scenario))
										.caption("Clear"))
						.build();

		return borderLayoutPanel()
						.northComponent(flowLayoutPanel(FlowLayout.LEADING)
										.add(refreshButton)
										.add(clearButton)
										.build())
						.centerComponent(scrollPane(exceptionsArea).build())
						.build();
	}

	private void setColors(JFreeChart chart) {
		ChartUtil.linkColors(this, chart);
	}

	private synchronized void exit() {
		if (!exiting) {
			exiting = true;
			Dialogs.progressWorker()
							.task(loadTest::shutdown)
							.owner(Utilities.parentFrame(this))
							.title("Shutting down...")
							.onResult(() -> System.exit(0))
							.execute();
		}
	}

	private void displayException(Exception exception) {
		Dialogs.displayException(exception, parentWindow(this));
	}

	private static Exception exception(ApplicationRow application) {
		List<Scenario.Result> results = application.results();

		return results.isEmpty() ? null : results.get(results.size() - 1).exception().orElse(null);
	}

	private static JPanel createSouthPanel() {
		return flowLayoutPanel(FlowLayout.TRAILING)
						.add(new JLabel("Memory usage:"))
						.add(Components.stringField()
										.columns(8)
										.editable(false)
										.horizontalAlignment(SwingConstants.CENTER)
										.onBuild(memoryUsageField -> TaskScheduler.builder()
														.task(() -> SwingUtilities.invokeLater(() -> memoryUsageField.setText(memoryUsage())))
														.interval(DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
														.start())
										.build())
						.build();
	}

	private static String memoryUsage() {
		return MEMORY_USAGE_FORMAT.format((RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 1024) + " KB";
	}

	private static void lookAndFeelSelected(LookAndFeelEnabler selectedLookAndFeel) {
		UserPreferences.set(LoadTestPanel.class.getName() + LOOK_AND_FEEL_PROPERTY,
						selectedLookAndFeel.lookAndFeelInfo().getClassName());
	}

	private final class ClearExceptionsCommand implements Control.Command {

		private final JTextArea exceptionsTextArea;
		private final Scenario<?> scenario;

		private ClearExceptionsCommand(JTextArea exceptionsTextArea, Scenario<?> scenario) {
			this.exceptionsTextArea = exceptionsTextArea;
			this.scenario = scenario;
		}

		@Override
		public void execute() {
			loadTestModel.clearExceptions(scenario.name());
			exceptionsTextArea.replaceRange("", 0, exceptionsTextArea.getDocument().getLength());
		}
	}

	private final class RefreshExceptionsCommand implements Control.Command {

		private final JTextArea exceptionsTextArea;
		private final Scenario<?> scenario;

		private RefreshExceptionsCommand(JTextArea exceptionsTextArea, Scenario<?> scenario) {
			this.exceptionsTextArea = exceptionsTextArea;
			this.scenario = scenario;
		}

		@Override
		public void execute() {
			exceptionsTextArea.replaceRange("", 0, exceptionsTextArea.getDocument().getLength());
			for (Exception exception : loadTestModel.exceptions(scenario.name())) {
				exceptionsTextArea.append(exception.getMessage());
				exceptionsTextArea.append(System.lineSeparator());
				exceptionsTextArea.append(System.lineSeparator());
			}
		}
	}
}