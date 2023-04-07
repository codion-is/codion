/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.loadtest;

import is.codion.common.Separators;
import is.codion.common.user.User;
import is.codion.swing.common.tools.loadtest.LoadTest;
import is.codion.swing.common.tools.loadtest.LoadTestModel;
import is.codion.swing.common.tools.loadtest.UsageScenario;
import is.codion.swing.common.tools.randomizer.ItemRandomizer.RandomItem;
import is.codion.swing.common.tools.ui.randomizer.ItemRandomizerPanel;
import is.codion.swing.common.ui.component.text.MemoryUsageField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.laf.LookAndFeelSelectionPanel;

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
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.tools.ui.randomizer.ItemRandomizerPanel.itemRandomizerPanel;
import static is.codion.swing.common.ui.Windows.frame;
import static is.codion.swing.common.ui.Windows.screenSizeRatio;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.lookAndFeelSelectionDialog;
import static is.codion.swing.common.ui.icon.Logos.logoTransparent;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.*;
import static is.codion.swing.common.ui.layout.Layouts.*;
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
  private static final int LARGE_TEXT_FIELD_COLUMNS = 8;
  private static final int SMALL_TEXT_FIELD_COLUMNS = 3;
  private static final int SPINNER_STEP_SIZE = 10;
  private static final double RESIZE_WEIGHT = 0.8;

  private final LoadTest<T> loadTestModel;
  private final JPanel scenarioBase = new JPanel(gridLayout(0, 1));
  private final ItemRandomizerPanel<UsageScenario<T>> scenarioPanel;

  static {
    LookAndFeelSelectionPanel.CHANGE_ON_SELECTION.set(true);
    Arrays.stream(FlatAllIJThemes.INFOS).forEach(themeInfo ->
            addLookAndFeelProvider(lookAndFeelProvider(themeInfo.getClassName())));
    findLookAndFeelProvider(defaultLookAndFeelName(LoadTestPanel.class.getName()))
            .ifPresent(LookAndFeelProvider::enable);
  }

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   */
  public LoadTestPanel(LoadTest<T> loadTestModel) {
    this.loadTestModel = requireNonNull(loadTestModel, "loadTestModel");
    this.scenarioPanel = createScenarioPanel();
    initializeUI();
  }

  /**
   * @return the load test model this panel is based on
   */
  public LoadTest<T> model() {
    return loadTestModel;
  }

  /**
   * Shows a frame containing this load test panel
   * @return the frame
   */
  public JFrame showFrame() {
    return frame(this)
            .icon(logoTransparent())
            .menuBar(createMainMenuControls().createMenuBar())
            .title("Codion - " + loadTestModel.title())
            .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
            .onClosing(windowEvent -> {
              JFrame frame = (JFrame) windowEvent.getWindow();
              frame.setTitle(frame.getTitle() + " - Closing...");
              loadTestModel.shutdown();
            })
            .size(screenSizeRatio(DEFAULT_SCREEN_SIZE_RATIO))
            .centerFrame(true)
            .show();
  }

  private Controls createMainMenuControls() {
    return Controls.builder()
            .control(Controls.builder()
                    .caption("View")
                    .mnemonic('V')
                    .control(lookAndFeelSelectionDialog()
                            .owner(this)
                            .userPreferencePropertyName(LoadTestPanel.class.getName())
                            .createControl()))
            .build();
  }

  private void initializeUI() {
    setLayout(borderLayout());
    add(panel(new BorderLayout())
            .add(panel(flexibleGridLayout(5, 1))
                    .add(createApplicationPanel())
                    .add(createActivityPanel())
                    .add(scenarioPanel)
                    .add(createUserPanel())
                    .add(createChartControlPanel())
                    .build(), BorderLayout.NORTH)
            .build(), BorderLayout.WEST);
    add(createChartPanel(), BorderLayout.CENTER);
    add(createSouthPanel(), BorderLayout.SOUTH);
  }

  private static JPanel createSouthPanel() {
    return panel(flowLayout(FlowLayout.TRAILING))
            .add(new JLabel("Memory usage:"))
            .add(new MemoryUsageField(DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS))
            .build();
  }

  private ItemRandomizerPanel<UsageScenario<T>> createScenarioPanel() {
    ItemRandomizerPanel<UsageScenario<T>> panel = itemRandomizerPanel(loadTestModel.scenarioChooser());
    panel.setBorder(createTitledBorder("Usage scenarios"));
    panel.addSelectedItemListener(this::onScenarioSelectionChanged);

    return panel;
  }

  private JPanel createUserPanel() {
    User user = loadTestModel.getUser();
    JTextField usernameField = textField()
            .initialValue(user.username())
            .columns(LARGE_TEXT_FIELD_COLUMNS)
            .build();
    JPasswordField passwordField = passwordField()
            .initialValue(String.valueOf(user.getPassword()))
            .columns(LARGE_TEXT_FIELD_COLUMNS)
            .build();
    ActionListener userInfoListener = e -> loadTestModel.setUser(User.user(usernameField.getText(), passwordField.getPassword()));
    usernameField.addActionListener(userInfoListener);
    passwordField.addActionListener(userInfoListener);

    return panel(flexibleGridLayout(2, 2))
            .border(createTitledBorder("User"))
            .add(new JLabel("Username"))
            .add(usernameField)
            .add(new JLabel("Password"))
            .add(passwordField)
            .build();
  }

  private JPanel createApplicationPanel() {
    return panel(borderLayout())
            .border(createTitledBorder("Applications"))
            .add(panel(borderLayout())
                    .add(panel(flexibleGridLayout(1, 2))
                            .add(new JLabel("Batch size"))
                            .add(integerSpinner(loadTestModel.applicationBatchSizeValue())
                                    .editable(false)
                                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                                    .toolTipText("Application batch size")
                                    .build())
                            .build(), BorderLayout.WEST)
                    .add(createAddRemoveApplicationPanel(), BorderLayout.CENTER)
                    .build(), BorderLayout.NORTH)
            .build();
  }

  private JPanel createAddRemoveApplicationPanel() {
    return panel(new BorderLayout())
            .add(Control.builder(loadTestModel::removeApplicationBatch)
                    .caption("-")
                    .description("Remove application batch")
                    .build().createButton(), BorderLayout.WEST)
            .add(integerField()
                    .editable(false)
                    .horizontalAlignment(SwingConstants.CENTER)
                    .columns(5)
                    .linkedValueObserver(loadTestModel.applicationCountObserver())
                    .build(), BorderLayout.CENTER)
            .add(Control.builder(loadTestModel::addApplicationBatch)
                    .caption("+")
                    .description("Add application batch")
                    .build().createButton(), BorderLayout.EAST)
            .build();
  }

  private JPanel createChartControlPanel() {
    return panel(flexibleGridLayout(1, 2))
            .border(createTitledBorder("Charts"))
            .add(checkBox(loadTestModel.collectChartDataState())
                    .caption("Collect chart data")
                    .build())
            .add(Control.builder(loadTestModel::clearChartData)
                    .caption("Clear")
                    .build().createButton())
            .build();
  }

  private JPanel createChartPanel() {
    JFreeChart thinkTimeChart = createXYStepChart(null,
            null, null, loadTestModel.thinkTimeDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(thinkTimeChart);
    ChartPanel thinkTimeChartPanel = new ChartPanel(thinkTimeChart);
    thinkTimeChartPanel.setBorder(createEtchedBorder());

    JFreeChart numberOfApplicationsChart = createXYStepChart(null,
            null, null, loadTestModel.numberOfApplicationsDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(numberOfApplicationsChart);
    ChartPanel numberOfApplicationsChartPanel = new ChartPanel(numberOfApplicationsChart);
    numberOfApplicationsChartPanel.setBorder(createEtchedBorder());

    JFreeChart usageScenarioChart = createXYStepChart(null,
            null, null, loadTestModel.usageScenarioDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(usageScenarioChart);
    ChartPanel usageScenarioChartPanel = new ChartPanel(usageScenarioChart);
    usageScenarioChartPanel.setBorder(createEtchedBorder());

    JFreeChart failureChart = createXYStepChart(null,
            null, null, loadTestModel.usageScenarioFailureDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(failureChart);
    ChartPanel failureChartPanel = new ChartPanel(failureChart);
    failureChartPanel.setBorder(createEtchedBorder());

    JFreeChart memoryUsageChart = createXYStepChart(null,
            null, null, loadTestModel.memoryUsageDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(memoryUsageChart);
    ChartPanel memoryUsageChartPanel = new ChartPanel(memoryUsageChart);
    memoryUsageChartPanel.setBorder(createEtchedBorder());

    JFreeChart systemLoadChart = createXYStepChart(null,
            null, null, loadTestModel.systemLoadDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(systemLoadChart);
    systemLoadChart.getXYPlot().getRangeAxis().setRange(0, 100);
    ChartPanel systemLoadChartPanel = new ChartPanel(systemLoadChart);
    systemLoadChartPanel.setBorder(createEtchedBorder());

    usageScenarioChartPanel.setBorder(createTitledBorder("Scenarios run per second"));
    thinkTimeChartPanel.setBorder(createTitledBorder("Think time (ms)"));
    numberOfApplicationsChartPanel.setBorder(createTitledBorder("Application count"));
    memoryUsageChartPanel.setBorder(createTitledBorder("Memory usage (MB)"));
    systemLoadChartPanel.setBorder(createTitledBorder("System load"));
    failureChartPanel.setBorder(createTitledBorder("Scenario run failures per second"));

    return panel(new BorderLayout())
            .add(tabbedPane()
                    .tab("Overview", splitPane()
                            .orientation(JSplitPane.VERTICAL_SPLIT)
                            .oneTouchExpandable(true)
                            .leftComponent(tabbedPane()
                                    .tab("Scenarios run", usageScenarioChartPanel)
                                    .tab("Failed runs", failureChartPanel)
                                    .build())
                            .rightComponent(panel(new GridLayout(1, 4))
                                    .add(memoryUsageChartPanel)
                                    .add(systemLoadChartPanel)
                                    .add(thinkTimeChartPanel)
                                    .add(numberOfApplicationsChartPanel)
                                    .build())
                            .resizeWeight(RESIZE_WEIGHT)
                            .build())
                    .tab("Scenarios", scenarioBase)
                    .build())
            .build();
  }

  private JPanel createActivityPanel() {
    return panel(flexibleGridLayout(4, 2))
            .add(new JLabel("Max. think time", SwingConstants.CENTER))
            .add(integerSpinner(loadTestModel.maximumThinkTimeValue())
                    .stepSize(SPINNER_STEP_SIZE)
                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                    .build())
            .add(new JLabel("Min. think time", SwingConstants.CENTER))
            .add(integerSpinner(loadTestModel.minimumThinkTimeValue())
                    .stepSize(SPINNER_STEP_SIZE)
                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                    .build())
            .add(toggleButton(loadTestModel.pausedState())
                    .caption("Pause")
                    .mnemonic('P')
                    .build())
            .border(createTitledBorder("Activity"))
            .build();
  }

  private void onScenarioSelectionChanged(List<RandomItem<UsageScenario<T>>> selectedScenarios) {
    scenarioBase.removeAll();
    selectedScenarios.forEach(scenario -> scenarioBase.add(createScenarioPanel(scenario.item())));
    validate();
    repaint();
  }

  private JPanel createScenarioPanel(UsageScenario<T> item) {
    return panel(borderLayout())
            .add(tabbedPane()
                    .tab("Duration", createScenarioDurationChartPanel(item))
                    .tab("Exceptions", createScenarioExceptionsPanel(item))
                    .build(), BorderLayout.CENTER)
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
    JButton refreshButton = Control.builder(new RefreshExceptionsCommand(exceptionsArea, scenario))
            .caption("Refresh")
            .build()
            .createButton();
    refreshButton.doClick();

    JButton clearButton = Control.builder(new ClearExceptionsCommand(exceptionsArea, scenario))
            .caption("Clear")
            .build()
            .createButton();

    return panel(borderLayout())
            .add(new JScrollPane(exceptionsArea), BorderLayout.CENTER)
            .add(panel(borderLayout())
                    .add(refreshButton, BorderLayout.NORTH)
                    .add(clearButton, BorderLayout.SOUTH)
                    .build(), BorderLayout.EAST)
            .build();
  }

  private void setColors(JFreeChart chart) {
    ChartUtil.linkColors(this, chart);
  }

  private static final class ClearExceptionsCommand implements Control.Command {

    private final JTextArea exceptionsTextArea;
    private final UsageScenario<?> scenario;

    private ClearExceptionsCommand(JTextArea exceptionsTextArea, UsageScenario<?> scenario) {
      this.exceptionsTextArea = exceptionsTextArea;
      this.scenario = scenario;
    }

    @Override
    public void perform() throws Exception {
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
    public void perform() throws Exception {
      exceptionsTextArea.replaceRange("", 0, exceptionsTextArea.getDocument().getLength());
      for (Throwable exception : scenario.exceptions()) {
        exceptionsTextArea.append(exception.getMessage());
        exceptionsTextArea.append(Separators.LINE_SEPARATOR);
        exceptionsTextArea.append(Separators.LINE_SEPARATOR);
      }
    }
  }
}