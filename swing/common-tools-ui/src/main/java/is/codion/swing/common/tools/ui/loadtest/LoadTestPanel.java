/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.loadtest;

import is.codion.common.Separators;
import is.codion.common.user.User;
import is.codion.swing.common.tools.loadtest.LoadTest;
import is.codion.swing.common.tools.loadtest.LoadTestModel;
import is.codion.swing.common.tools.loadtest.UsageScenario;
import is.codion.swing.common.tools.randomizer.ItemRandomizer;
import is.codion.swing.common.tools.ui.randomizer.ItemRandomizerPanel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.MemoryUsageField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.laf.LookAndFeelSelectionPanel;
import is.codion.swing.common.ui.layout.Layouts;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;

import javax.swing.BorderFactory;
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

import static is.codion.swing.common.ui.laf.LookAndFeelProvider.*;
import static java.util.Objects.requireNonNull;

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
  private final JPanel scenarioBase = new JPanel(Layouts.gridLayout(0, 1));
  private final ItemRandomizerPanel<UsageScenario<T>> scenarioPanel;

  static {
    LookAndFeelSelectionPanel.CHANGE_ON_SELECTION.set(true);
    Arrays.stream(FlatAllIJThemes.INFOS).forEach(themeInfo ->
            addLookAndFeelProvider(lookAndFeelProvider(themeInfo.getClassName())));
    LookAndFeelProvider.findLookAndFeelProvider(defaultLookAndFeelName(LoadTestPanel.class.getName()))
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
    return Windows.frame(this)
            .icon(Logos.logoTransparent())
            .menuBar(createMainMenuControls().createMenuBar())
            .title("Codion - " + loadTestModel.title())
            .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
            .onClosing(windowEvent -> {
              JFrame frame = (JFrame) windowEvent.getWindow();
              frame.setTitle(frame.getTitle() + " - Closing...");
              loadTestModel.shutdown();
            })
            .size(Windows.screenSizeRatio(DEFAULT_SCREEN_SIZE_RATIO))
            .centerFrame(true)
            .show();
  }

  private Controls createMainMenuControls() {
    return Controls.builder()
            .control(Controls.builder()
                    .caption("View")
                    .mnemonic('V')
                    .control(Dialogs.lookAndFeelSelectionDialog()
                            .owner(this)
                            .userPreferencePropertyName(LoadTestPanel.class.getName())
                            .createControl()))
            .build();
  }

  private void initializeUI() {
    setLayout(Layouts.borderLayout());
    add(Components.panel(new BorderLayout())
            .add(Components.panel(Layouts.flexibleGridLayout(5, 1))
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
    return Components.panel(Layouts.flowLayout(FlowLayout.TRAILING))
            .add(new JLabel("Memory usage:"))
            .add(new MemoryUsageField(DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS))
            .build();
  }

  private ItemRandomizerPanel<UsageScenario<T>> createScenarioPanel() {
    ItemRandomizerPanel<UsageScenario<T>> panel = ItemRandomizerPanel.itemRandomizerPanel(loadTestModel.scenarioChooser());
    panel.setBorder(BorderFactory.createTitledBorder("Usage scenarios"));
    panel.addSelectedItemListener(this::onScenarioSelectionChanged);

    return panel;
  }

  private JPanel createUserPanel() {
    User user = loadTestModel.getUser();
    JTextField usernameField = Components.textField()
            .initialValue(user.username())
            .columns(LARGE_TEXT_FIELD_COLUMNS)
            .build();
    JPasswordField passwordField = Components.passwordField()
            .initialValue(String.valueOf(user.getPassword()))
            .columns(LARGE_TEXT_FIELD_COLUMNS)
            .build();
    ActionListener userInfoListener = e -> loadTestModel.setUser(User.user(usernameField.getText(), passwordField.getPassword()));
    usernameField.addActionListener(userInfoListener);
    passwordField.addActionListener(userInfoListener);

    return Components.panel(Layouts.flexibleGridLayout(2, 2))
            .border(BorderFactory.createTitledBorder("User"))
            .add(new JLabel("Username"))
            .add(usernameField)
            .add(new JLabel("Password"))
            .add(passwordField)
            .build();
  }

  private JPanel createApplicationPanel() {
    return Components.panel(Layouts.borderLayout())
            .border(BorderFactory.createTitledBorder("Applications"))
            .add(Components.panel(Layouts.borderLayout())
                    .add(Components.panel(Layouts.flexibleGridLayout(1, 2))
                            .add(new JLabel("Batch size"))
                            .add(Components.integerSpinner(loadTestModel.applicationBatchSizeValue())
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
    return Components.panel(new BorderLayout())
            .add(Control.builder(loadTestModel::removeApplicationBatch)
                    .caption("-")
                    .description("Remove application batch")
                    .build().createButton(), BorderLayout.WEST)
            .add(Components.integerField()
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
    return Components.panel(Layouts.flexibleGridLayout(1, 2))
            .border(BorderFactory.createTitledBorder("Charts"))
            .add(Components.checkBox(loadTestModel.collectChartDataState())
                    .caption("Collect chart data")
                    .build())
            .add(Control.builder(loadTestModel::resetChartData)
                    .caption("Reset")
                    .build().createButton())
            .build();
  }

  private JPanel createChartPanel() {
    JFreeChart thinkTimeChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.thinkTimeDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(thinkTimeChart);
    ChartPanel thinkTimeChartPanel = new ChartPanel(thinkTimeChart);
    thinkTimeChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart numberOfApplicationsChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.numberOfApplicationsDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(numberOfApplicationsChart);
    ChartPanel numberOfApplicationsChartPanel = new ChartPanel(numberOfApplicationsChart);
    numberOfApplicationsChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart usageScenarioChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.usageScenarioDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(usageScenarioChart);
    ChartPanel usageScenarioChartPanel = new ChartPanel(usageScenarioChart);
    usageScenarioChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart failureChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.usageScenarioFailureDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(failureChart);
    ChartPanel failureChartPanel = new ChartPanel(failureChart);
    failureChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart memoryUsageChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.memoryUsageDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(memoryUsageChart);
    ChartPanel memoryUsageChartPanel = new ChartPanel(memoryUsageChart);
    memoryUsageChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart systemLoadChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.systemLoadDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(systemLoadChart);
    systemLoadChart.getXYPlot().getRangeAxis().setRange(0, 100);
    ChartPanel systemLoadChartPanel = new ChartPanel(systemLoadChart);
    systemLoadChartPanel.setBorder(BorderFactory.createEtchedBorder());

    usageScenarioChartPanel.setBorder(BorderFactory.createTitledBorder("Scenarios run per second"));
    thinkTimeChartPanel.setBorder(BorderFactory.createTitledBorder("Think time (ms)"));
    numberOfApplicationsChartPanel.setBorder(BorderFactory.createTitledBorder("Application count"));
    memoryUsageChartPanel.setBorder(BorderFactory.createTitledBorder("Memory usage (MB)"));
    systemLoadChartPanel.setBorder(BorderFactory.createTitledBorder("System load"));
    failureChartPanel.setBorder(BorderFactory.createTitledBorder("Scenario run failures per second"));

    return Components.panel(new BorderLayout(0, 0))
            .add(Components.tabbedPane()
                    .tab("Overview", Components.splitPane()
                            .orientation(JSplitPane.VERTICAL_SPLIT)
                            .oneTouchExpandable(true)
                            .leftComponent(Components.tabbedPane()
                                    .tab("Scenarios run", usageScenarioChartPanel)
                                    .tab("Failed runs", failureChartPanel)
                                    .build())
                            .rightComponent(Components.panel(new GridLayout(1, 4, 0, 0))
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
    return Components.panel(Layouts.flexibleGridLayout(4, 2))
            .add(new JLabel("Max. think time", SwingConstants.CENTER))
            .add(Components.integerSpinner(loadTestModel.maximumThinkTimeValue())
                    .stepSize(SPINNER_STEP_SIZE)
                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                    .build())
            .add(new JLabel("Min. think time", SwingConstants.CENTER))
            .add(Components.integerSpinner(loadTestModel.minimumThinkTimeValue())
                    .stepSize(SPINNER_STEP_SIZE)
                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                    .build())
            .add(Components.toggleButton(loadTestModel.pausedState())
                    .caption("Pause")
                    .mnemonic('P')
                    .build())
            .border(BorderFactory.createTitledBorder("Activity"))
            .build();
  }

  private void onScenarioSelectionChanged(List<ItemRandomizer.RandomItem<UsageScenario<T>>> selectedScenarios) {
    scenarioBase.removeAll();
    selectedScenarios.forEach(scenario -> scenarioBase.add(createScenarioPanel(scenario.item())));
    validate();
    repaint();
  }

  private JPanel createScenarioPanel(UsageScenario<T> item) {
    return Components.panel(Layouts.borderLayout())
            .add(Components.tabbedPane()
                    .tab("Duration", createScenarioDurationChartPanel(item))
                    .tab("Exceptions", createScenarioExceptionsPanel(item))
                    .build(), BorderLayout.CENTER)
            .build();
  }

  private ChartPanel createScenarioDurationChartPanel(UsageScenario<T> scenario) {
    JFreeChart scenarioDurationChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.scenarioDurationDataset(scenario.name()),
            PlotOrientation.VERTICAL, true, true, false);
    setColors(scenarioDurationChart);
    ChartPanel scenarioDurationChartPanel = new ChartPanel(scenarioDurationChart);
    scenarioDurationChartPanel.setBorder(BorderFactory.createEtchedBorder());
    DeviationRenderer renderer = new DeviationRenderer();
    renderer.setDefaultShapesVisible(false);
    scenarioDurationChart.getXYPlot().setRenderer(renderer);

    return scenarioDurationChartPanel;
  }

  private JPanel createScenarioExceptionsPanel(UsageScenario<T> scenario) {
    JTextArea exceptionsArea = Components.textArea()
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

    return Components.panel(Layouts.borderLayout())
            .add(new JScrollPane(exceptionsArea), BorderLayout.CENTER)
            .add(Components.panel(Layouts.borderLayout())
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