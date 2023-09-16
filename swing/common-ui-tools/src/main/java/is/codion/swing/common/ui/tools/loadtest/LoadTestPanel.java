/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.tools.loadtest;

import is.codion.common.Separators;
import is.codion.common.user.User;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel.Application;
import is.codion.swing.common.model.tools.loadtest.UsageScenario;
import is.codion.swing.common.model.tools.randomizer.ItemRandomizer.RandomItem;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.common.ui.component.text.MemoryUsageField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.tools.randomizer.ItemRandomizerPanel;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
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
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.ui.Windows.frame;
import static is.codion.swing.common.ui.Windows.screenSizeRatio;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.lookAndFeelSelectionDialog;
import static is.codion.swing.common.ui.icon.Logos.logoTransparent;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.defaultLookAndFeelName;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeelProvider;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEtchedBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static org.jfree.chart.ChartFactory.createXYStepChart;

/**
 * A default UI component for the LoadTestModel class.
 * @param <T> the load test application type
 * @see LoadTestModel
 */
public final class LoadTestPanel<T> extends JPanel {

  private static final int DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
  private static final double DEFAULT_SCREEN_SIZE_RATIO = 0.75;
  private static final int USERNAME_PASSWORD_COLUMNS = 6;
  private static final int SMALL_TEXT_FIELD_COLUMNS = 3;
  private static final int SPINNER_STEP_SIZE = 10;
  private static final double RESIZE_WEIGHT = 0.8;

  private final LoadTestModel<T> loadTestModel;
  private final JPanel scenarioBase = gridLayoutPanel(0, 1).build();

  static {
    FilteredTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
    Arrays.stream(FlatAllIJThemes.INFOS)
            .forEach(LookAndFeelProvider::addLookAndFeelProvider);
    findLookAndFeelProvider(defaultLookAndFeelName(LoadTestPanel.class.getName()))
            .ifPresent(LookAndFeelProvider::enable);
  }

  private boolean exiting;

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   */
  public LoadTestPanel(LoadTestModel<T> loadTestModel) {
    this.loadTestModel = requireNonNull(loadTestModel, "loadTestModel");
    initializeUI();
  }

  /**
   * @return the load test model this panel is based on
   */
  public LoadTestModel<T> model() {
    return loadTestModel;
  }

  /**
   * Displays this LoadTestPanel in a frame on the EDT.
   */
  public void run() {
    SwingUtilities.invokeLater(this::showFrame);
  }

  /**
   * Shows a frame containing this load test panel
   * @return the frame
   */
  private JFrame showFrame() {
    return frame(this)
            .icon(logoTransparent())
            .menuBar(menu(createMainMenuControls()).createMenuBar())
            .title("Codion - " + loadTestModel.title())
            .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
            .onClosing(windowEvent -> exit())
            .size(screenSizeRatio(DEFAULT_SCREEN_SIZE_RATIO))
            .centerFrame(true)
            .show();
  }

  private Controls createMainMenuControls() {
    return Controls.builder()
            .controls(Controls.builder()
                    .name("File")
                    .mnemonic('F')
                    .control(Control.builder(this::exit)
                            .name("Exit")
                            .mnemonic('X')))
            .controls(Controls.builder()
                    .name("View")
                    .mnemonic('V')
                    .control(lookAndFeelSelectionDialog()
                            .owner(this)
                            .userPreferencePropertyName(LoadTestPanel.class.getName())
                            .createControl())
                    .control(ToggleControl.builder(loadTestModel.collectChartData())
                            .name("Collect chart data"))
                    .control(Control.builder(loadTestModel::clearChartData)
                            .name("Clear charts")))
            .build();
  }

  private void initializeUI() {
    setLayout(borderLayout());
    add(createCenterPanel(), BorderLayout.CENTER);
    add(createSouthPanel(), BorderLayout.SOUTH);
  }

  private ItemRandomizerPanel<UsageScenario<T>> createScenarioPanel() {
    ItemRandomizerPanel<UsageScenario<T>> panel = ItemRandomizerPanel.itemRandomizerPanel(loadTestModel.scenarioChooser());
    panel.setBorder(createTitledBorder("Usage scenarios"));
    panel.addSelectedItemListener(this::onScenarioSelectionChanged);

    return panel;
  }

  private JPanel createAddRemoveApplicationPanel() {
    return borderLayoutPanel()
            .westComponent(button(Control.builder(loadTestModel::removeApplicationBatch)
                    .name("-")
                    .description("Remove application batch"))
                    .build())
            .centerComponent(integerField()
                    .editable(false)
                    .focusable(false)
                    .horizontalAlignment(SwingConstants.CENTER)
                    .columns(5)
                    .linkedValueObserver(loadTestModel.applicationCount())
                    .build())
            .eastComponent(button(Control.builder(loadTestModel::addApplicationBatch)
                    .name("+")
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
                            .add(integerSpinner(loadTestModel.applicationBatchSize())
                                    .editable(false)
                                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                                    .toolTipText("Application batch size")
                                    .build())
                            .add(createAddRemoveApplicationPanel())
                            .add(createUserPanel())
                            .add(new JLabel("Min. think time", SwingConstants.CENTER))
                            .add(integerSpinner(loadTestModel.minimumThinkTime())
                                    .stepSize(SPINNER_STEP_SIZE)
                                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                                    .build())
                            .add(new JLabel("Max. think time", SwingConstants.CENTER))
                            .add(integerSpinner(loadTestModel.maximumThinkTime())
                                    .stepSize(SPINNER_STEP_SIZE)
                                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                                    .build())
                            .add(toggleButton(loadTestModel.paused())
                                    .text("Pause")
                                    .mnemonic('P')
                                    .build())
                            .build())
                    .eastComponent(checkBox(loadTestModel.autoRefreshApplications())
                            .text("Automatic refresh")
                            .build())
                    .build())
            .centerComponent(scrollPane(createApplicationsTable()).build())
            .build();
  }

  private JPanel createUserPanel() {
    User user = loadTestModel.user().get();
    JTextField usernameField = textField()
            .initialValue(user.username())
            .columns(USERNAME_PASSWORD_COLUMNS)
            .build();
    JPasswordField passwordField = passwordField()
            .initialValue(String.valueOf(user.password()))
            .columns(USERNAME_PASSWORD_COLUMNS)
            .build();
    ActionListener userInfoListener = e -> loadTestModel.user().set(User.user(usernameField.getText(), passwordField.getPassword()));
    usernameField.addActionListener(userInfoListener);
    passwordField.addActionListener(userInfoListener);

    return flexibleGridLayoutPanel(1, 4)
            .add(new JLabel("Username"))
            .add(usernameField)
            .add(new JLabel("Password"))
            .add(passwordField)
            .build();
  }

  private JTabbedPane createScenarioOverviewChartPanel() {
    return tabbedPane()
            .tab("Scenarios run", createUsageScenarioChartPanel())
            .tab("Failed runs", createFailureChartPanel())
            .build();
  }

  private ChartPanel createUsageScenarioChartPanel() {
    JFreeChart usageScenarioChart = createXYStepChart(null, null, null,
            loadTestModel.usageScenarioDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(usageScenarioChart);
    ChartPanel usageScenarioChartPanel = new ChartPanel(usageScenarioChart);
    usageScenarioChartPanel.setBorder(createTitledBorder("Scenarios run per second"));

    return usageScenarioChartPanel;
  }

  private ChartPanel createFailureChartPanel() {
    JFreeChart failureChart = createXYStepChart(null, null, null,
            loadTestModel.usageScenarioFailureDataset(), PlotOrientation.VERTICAL, true, true, false);
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

  private FilteredTable<Application, Integer> createApplicationsTable() {
    return FilteredTable.builder(model().applicationTableModel())
            .autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
            .popupMenuControl(Control.builder(model().applicationTableModel()::refresh)
                    .name("Refresh")
                    .enabled(model().autoRefreshApplications().not())
                    .build())
            .build();
  }

  private void onScenarioSelectionChanged(List<RandomItem<UsageScenario<T>>> selectedScenarios) {
    scenarioBase.removeAll();
    selectedScenarios.forEach(scenario -> scenarioBase.add(createScenarioPanel(scenario.item())));
    validate();
    repaint();
  }

  private JPanel createScenarioPanel(UsageScenario<T> item) {
    return borderLayoutPanel()
            .centerComponent(tabbedPane()
                    .tab("Duration", createScenarioDurationChartPanel(item))
                    .tab("Exceptions", createScenarioExceptionsPanel(item))
                    .build())
            .build();
  }

  private ChartPanel createScenarioDurationChartPanel(UsageScenario<T> scenario) {
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

  private JPanel createScenarioExceptionsPanel(UsageScenario<T> scenario) {
    JTextArea exceptionsArea = textArea()
            .editable(false)
            .build();
    JButton refreshButton = button(Control.builder(new RefreshExceptionsCommand(exceptionsArea, scenario))
            .name("Refresh"))
            .build();
    refreshButton.doClick();

    JButton clearButton = button(Control.builder(new ClearExceptionsCommand(exceptionsArea, scenario))
            .name("Clear"))
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
    if (exiting) {
      return;
    }
    exiting = true;
    JFrame frame = Utilities.parentFrame(this);
    if (frame != null) {
      frame.setTitle(frame.getTitle() + " - Closing...");
    }
    loadTestModel.shutdown();
    System.exit(0);
  }

  private static JPanel createSouthPanel() {
    return flowLayoutPanel(FlowLayout.TRAILING)
            .add(new JLabel("Memory usage:"))
            .add(new MemoryUsageField(DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS))
            .build();
  }

  private static final class ClearExceptionsCommand implements Control.Command {

    private final JTextArea exceptionsTextArea;
    private final UsageScenario<?> scenario;

    private ClearExceptionsCommand(JTextArea exceptionsTextArea, UsageScenario<?> scenario) {
      this.exceptionsTextArea = exceptionsTextArea;
      this.scenario = scenario;
    }

    @Override
    public void perform() {
      scenario.clearExceptions();
      exceptionsTextArea.replaceRange("", 0, exceptionsTextArea.getDocument().getLength());
    }
  }

  private static final class RefreshExceptionsCommand implements Control.Command {

    private final JTextArea exceptionsTextArea;
    private final UsageScenario<?> scenario;

    private RefreshExceptionsCommand(JTextArea exceptionsTextArea, UsageScenario<?> scenario) {
      this.exceptionsTextArea = exceptionsTextArea;
      this.scenario = scenario;
    }

    @Override
    public void perform() {
      exceptionsTextArea.replaceRange("", 0, exceptionsTextArea.getDocument().getLength());
      for (Throwable exception : scenario.exceptions()) {
        exceptionsTextArea.append(exception.getMessage());
        exceptionsTextArea.append(Separators.LINE_SEPARATOR);
        exceptionsTextArea.append(Separators.LINE_SEPARATOR);
      }
    }
  }
}