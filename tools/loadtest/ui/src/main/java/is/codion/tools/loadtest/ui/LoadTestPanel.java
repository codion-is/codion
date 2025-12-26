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

import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.scheduler.TaskScheduler;
import is.codion.common.utilities.user.User;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.frame.Frames;
import is.codion.swing.common.ui.icon.SVGIcon;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.Scenario;
import is.codion.tools.loadtest.model.LoadTestModel;
import is.codion.tools.loadtest.model.LoadTestModel.ApplicationRow;
import is.codion.tools.loadtest.model.LoadTestModel.ExceptionTimestamp;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.enableLookAndFeel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.window.Windows.screenSizeRatio;
import static java.time.ZoneId.systemDefault;
import static java.time.format.DateTimeFormatter.ofPattern;
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

	private static final SVGIcon LOGO = SVGIcon.svgIcon(LoadTestPanel.class.getResource("logo.svg"), 68, Color.BLACK);
	private static final int DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
	private static final double DEFAULT_SCREEN_SIZE_RATIO = 0.75;
	private static final int SMALL_TEXT_FIELD_COLUMNS = 3;
	private static final int SPINNER_STEP_SIZE = 10;
	private static final double RESIZE_WEIGHT = 0.8;
	private static final String LOOK_AND_FEEL_PROPERTY = ".lookAndFeel";
	private static final NumberFormat DURATION_FORMAT = NumberFormat.getIntegerInstance();
	private static final String DEFAULT_TITLE = "Codion LoadTest";
	private static final NumberFormat MEMORY_USAGE_FORMAT = NumberFormat.getIntegerInstance();
	private static final Runtime RUNTIME = Runtime.getRuntime();

	private final LoadTestModel<T> loadTestModel;
	private final FilterComboBoxModel<Item<User>> userComboBoxModel = FilterComboBoxModel.builder()
					.<Item<User>>items(Collections::emptyList)
					.nullItem(item(null, "-"))
					.build();
	private final JComboBox<Item<User>> userComboBox = comboBox()
					.model(userComboBoxModel)
					.consumer(this::setUser)
					.preferredWidth(120)
					.build();
	private final LoadTest<T> loadTest;
	private final Map<Scenario<T>, JPanel> exceptionPanels = new HashMap<>();
	private final Map<Scenario<T>, JPanel> durationPanelsPanels = new HashMap<>();
	private final JPanel exceptionPanel = borderLayoutPanel().build();
	private final JPanel durationPanel = gridLayoutPanel(0, 1).build();

	static {
		FilterTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
		enableLookAndFeel(LoadTestPanel.class.getName() + LOOK_AND_FEEL_PROPERTY, FlatDarculaLaf.class);
	}

	private boolean exiting;

	private LoadTestPanel(LoadTestModel<T> loadTestModel) {
		this.loadTestModel = requireNonNull(loadTestModel);
		this.loadTest = loadTestModel.loadTest();
		loadTest.applications().user().optional().ifPresent(user -> {
			Item<User> item = item(user, user.username());
			userComboBoxModel.items().add(item);
			userComboBoxModel.selection().item().set(item);
		});
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
		Thread.setDefaultUncaughtExceptionHandler((t, e) ->
						Dialogs.displayException(e, Ancestor.window().of(LoadTestPanel.this).get()));
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

	private JFrame showFrame() {
		return Frames.builder()
						.component(this)
						.icon(LOGO)
						.menuBar(menu()
										.controls(createMainMenuControls())
										.buildMenuBar())
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
														.toggle(loadTestModel.chartStatistics())
														.caption("Chart statistics"))
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

	private ScenarioPanel<T> createScenarioPanel() {
		ScenarioPanel<T> panel = new ScenarioPanel<>(loadTest.randomizer());
		panel.setBorder(createTitledBorder("Usage scenarios"));
		panel.selectedScenarios().addConsumer(this::onScenarioSelectionChanged);

		return panel;
	}

	private JPanel createAddRemoveApplicationPanel() {
		return borderLayoutPanel()
						.layout(new BorderLayout(0, 0))
						.center(integerField()
										.editable(false)
										.focusable(false)
										.horizontalAlignment(SwingConstants.CENTER)
										.columns(5)
										.link(loadTest.applications().count()))
						.east(gridLayoutPanel(1, 2)
										.layout(new GridLayout(1, 2, 0, 0))
										.add(button()
														.control(Control.builder()
																		.command(loadTest.applications()::removeBatch)
																		.caption("-")
																		.description("Remove application batch")))
										.add(button()
														.control(Control.builder()
																		.command(loadTest.applications()::addBatch)
																		.caption("+")
																		.enabled(userComboBoxModel.selection().empty().not())
																		.description("Add application batch"))))
						.build();
	}

	private JPanel createCenterPanel() {
		return borderLayoutPanel()
						.center(tabbedPane()
										.tab("Applications")
										.component(createApplicationsPanel())
										.mnemonic(KeyEvent.VK_1)
										.add()
										.tab("Scenarios")
										.component(borderLayoutPanel()
														.west(createScenarioPanel())
														.center(tabbedPane()
																		.tab("Exceptions", exceptionPanel)
																		.tab("Duration", durationPanel)
																		.build()))
										.mnemonic(KeyEvent.VK_2)
										.add()
										.tab("Overview")
										.component(borderLayoutPanel()
														.center(splitPane()
																		.orientation(JSplitPane.VERTICAL_SPLIT)
																		.oneTouchExpandable(true)
																		.topComponent(createScenarioOverviewChartPanel())
																		.bottomComponent(createSouthChartPanel())
																		.resizeWeight(RESIZE_WEIGHT)))
										.mnemonic(KeyEvent.VK_3)
										.add())
						.build();
	}

	private JPanel createApplicationsPanel() {
		return borderLayoutPanel()
						.north(borderLayoutPanel()
										.center(flowLayoutPanel(FlowLayout.LEADING)
														.add(new JLabel("Batch size"))
														.add(integerSpinner()
																		.link(loadTest.applications().batchSize())
																		.minimum(1)
																		.editable(false)
																		.columns(SMALL_TEXT_FIELD_COLUMNS)
																		.toolTipText("Application batch size"))
														.add(createAddRemoveApplicationPanel())
														.add(createUserPanel())
														.add(new JLabel("Min. think time", SwingConstants.CENTER))
														.add(integerSpinner()
																		.link(loadTest.thinkTime().minimum())
																		.stepSize(SPINNER_STEP_SIZE)
																		.columns(SMALL_TEXT_FIELD_COLUMNS))
														.add(new JLabel("Max. think time", SwingConstants.CENTER))
														.add(integerSpinner()
																		.link(loadTest.thinkTime().maximum())
																		.stepSize(SPINNER_STEP_SIZE)
																		.columns(SMALL_TEXT_FIELD_COLUMNS))
														.add(checkBox()
																		.link(loadTest.paused())
																		.text("Pause")
																		.mnemonic('P'))
														.add(checkBox()
																		.link(loadTest.pauseOnException())
																		.text("Pause on exception")
																		.mnemonic('E')
																		.toolTipText("Automatically pause on error")))
										.east(checkBox()
														.link(loadTestModel.autoRefreshApplications())
														.text("Automatic refresh")
														.mnemonic('A')))
						.center(scrollPane()
										.view(createApplicationsTable()))
						.build();
	}

	private JPanel createUserPanel() {
		return borderLayoutPanel()
						.layout(new BorderLayout(0, 0))
						.west(new JLabel("User"))
						.center(userComboBox)
						.east(gridLayoutPanel(1, 2)
										.layout(new GridLayout(1, 2, 0, 0))
										.add(button()
														.control(Control.builder()
																		.command(this::removeUser)
																		.caption("-")
																		.description("Remove the selected application user")
																		.enabled(userComboBoxModel.selection().empty().not())))
										.add(button()
														.control(Control.builder()
																		.command(this::addUser)
																		.caption("+")
																		.description("Add an application user"))))
						.build();
	}

	private void addUser() {
		User user = Dialogs.login()
						.owner(LoadTestPanel.this)
						.title("Add user")
						.show();
		Item<User> item = item(user, user.username());
		userComboBoxModel.items().add(item);
		userComboBoxModel.selection().item().set(item);
	}

	private void removeUser() {
		userComboBoxModel.selection().item().optional().ifPresent(userComboBoxModel.items()::remove);
	}

	private void setUser(Item<User> item) {
		loadTest.applications().user().set(item == null ? null : item.get());
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

	private FilterTable<ApplicationRow, String> createApplicationsTable() {
		FilterTableModel<ApplicationRow, String> tableModel = model().applicationTableModel();

		return FilterTable.builder()
						.model(tableModel)
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.doubleClick(command(this::viewException))
						.scrollToSelectedItem(false)
						.cellRenderer(ApplicationRow.DURATION, Long.class, renderer -> renderer
										.formatter(duration -> duration == null ? "" : DURATION_FORMAT.format(duration)))
						.popupControls(table -> Controls.builder()
										.control(Control.builder()
														.command(table.model().items()::refresh)
														.caption("Refresh")
														.enabled(model().autoRefreshApplications().not()))
										.separator()
										.control(Control.builder()
														.command(model()::removeSelectedApplications)
														.enabled(tableModel.selection().empty().not())
														.caption("Remove"))
										.separator()
										.control(Controls.builder()
														.caption("Columns")
														.control(table.createToggleColumnsControls())
														.control(table.createResetColumnsControl())
														.control(table.createSelectAutoResizeModeControl()))
										.build())
						.build();
	}

	private void viewException() {
		model().applicationTableModel().selection().item().optional()
						.map(LoadTestPanel::exception)
						.ifPresent(this::displayException);
	}

	private void onScenarioSelectionChanged(List<Scenario<T>> scenarios) {
		exceptionPanel.removeAll();
		if (!scenarios.isEmpty()) {
			TabbedPaneBuilder builder = tabbedPane();
			scenarios.forEach(scenario -> builder.tab(scenario.name(),
							exceptionPanels.computeIfAbsent(scenario, k -> createScenarioExceptionsPanel(scenario))));
			exceptionPanel.add(builder.build(), BorderLayout.CENTER);
		}
		durationPanel.removeAll();
		scenarios.forEach(scenario -> durationPanel.add(durationPanelsPanels.computeIfAbsent(scenario, k -> createScenarioDurationChartPanel(scenario))));
		validate();
		repaint();
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
										.caption("Refresh")
										.mnemonic('R'))
						.build();
		refreshButton.doClick();

		JButton clearButton = button()
						.control(Control.builder()
										.command(new ClearExceptionsCommand(exceptionsArea, scenario))
										.caption("Clear")
										.mnemonic('C'))
						.build();

		return borderLayoutPanel()
						.north(flowLayoutPanel(FlowLayout.LEADING)
										.add(refreshButton)
										.add(clearButton))
						.center(scrollPane()
										.view(exceptionsArea))
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
							.owner(Ancestor.window().of(this).get())
							.title("Shutting down...")
							.onResult(() -> System.exit(0))
							.execute();
		}
	}

	private void displayException(Exception exception) {
		Dialogs.displayException(exception, Ancestor.window().of(this).get());
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
														.start()))
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

		private static final DateTimeFormatter TIMESTAMP_FORMATTER = ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

		private final JTextArea exceptionsTextArea;
		private final Scenario<?> scenario;

		private RefreshExceptionsCommand(JTextArea exceptionsTextArea, Scenario<?> scenario) {
			this.exceptionsTextArea = exceptionsTextArea;
			this.scenario = scenario;
		}

		@Override
		public void execute() {
			exceptionsTextArea.replaceRange("", 0, exceptionsTextArea.getDocument().getLength());
			for (ExceptionTimestamp exceptionTimestamp : loadTestModel.exceptions(scenario.name())) {
				StringWriter stringWriter = new StringWriter();
				PrintWriter printWriter = new PrintWriter(stringWriter);
				exceptionTimestamp.exception().printStackTrace(printWriter);
				LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(exceptionTimestamp.timestamp()), systemDefault());
				exceptionsTextArea.insert("@" + timestamp.format(TIMESTAMP_FORMATTER) + "\n" + stringWriter + "\n", 0);
			}
			exceptionsTextArea.setCaretPosition(0);
		}
	}
}