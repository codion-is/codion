/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.loadtest;

import is.codion.common.Util;
import is.codion.common.user.User;
import is.codion.swing.common.tools.loadtest.LoadTest;
import is.codion.swing.common.tools.loadtest.LoadTestModel;
import is.codion.swing.common.tools.loadtest.UsageScenario;
import is.codion.swing.common.tools.randomizer.ItemRandomizer;
import is.codion.swing.common.tools.ui.randomizer.ItemRandomizerPanel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.textfield.MemoryUsageField;
import is.codion.swing.common.ui.component.textfield.TextComponents;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.LookAndFeelSelectionDialogBuilder;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.layout.Layouts;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.ui.laf.LookAndFeelProvider.addLookAndFeelProvider;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.getDefaultLookAndFeelName;
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
  private final JPanel pluginPanel;
  private final ItemRandomizerPanel<UsageScenario<T>> scenarioPanel;

  static {
    LookAndFeelSelectionDialogBuilder.CHANGE_LOOK_AND_FEEL_DURING_SELECTION.set(true);
    Arrays.stream(FlatAllIJThemes.INFOS).forEach(themeInfo ->
            addLookAndFeelProvider(LookAndFeelProvider.create(themeInfo.getClassName())));
    LookAndFeelProvider.getLookAndFeelProvider(getDefaultLookAndFeelName(LoadTestPanel.class.getName()))
            .ifPresent(LookAndFeelProvider::enable);
  }

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   */
  public LoadTestPanel(LoadTest<T> loadTestModel) {
    this(loadTestModel, null);
  }

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   * @param pluginPanel a panel to add as a plugin panel
   */
  public LoadTestPanel(LoadTest<T> loadTestModel, JPanel pluginPanel) {
    requireNonNull(loadTestModel, "loadTestModel");
    this.loadTestModel = loadTestModel;
    this.pluginPanel = pluginPanel;
    this.scenarioPanel = initializeScenarioPanel();
    initializeUI();
  }

  /**
   * @return the load test model this panel is based on
   */
  public LoadTest<T> getModel() {
    return loadTestModel;
  }

  /**
   * Shows a frame containing this load test panel
   * @return the frame
   */
  public JFrame showFrame() {
    return Windows.frameBuilder(this)
            .icon(Logos.logoTransparent())
            .menuBar(initializeMainMenuControls().createMenuBar())
            .title("Codion - " + loadTestModel.getTitle())
            .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
            .onClosing(windowEvent -> {
              JFrame frame = (JFrame) windowEvent.getWindow();
              frame.setTitle(frame.getTitle() + " - Closing...");
              loadTestModel.shutdown();
            })
            .size(Windows.getScreenSizeRatio(DEFAULT_SCREEN_SIZE_RATIO))
            .centerFrame(true)
            .show();
  }

  private Controls initializeMainMenuControls() {
    return Controls.builder()
            .control(Controls.builder()
                    .caption("View")
                    .mnemonic('V')
                    .control(Dialogs.lookAndFeelSelectionDialog()
                            .dialogOwner(this)
                            .userPreferencePropertyName(LoadTestPanel.class.getName())
                            .createControl()))
            .build();
  }

  private void initializeUI() {
    JPanel chartBase = initializeChartPanel();
    JPanel activityPanel = initializeActivityPanel();
    JPanel applicationPanel = initializeApplicationPanel();
    JPanel userBase = initializeUserPanel();
    JPanel chartControlPanel = initializeChartControlPanel();

    JPanel controlPanel = new JPanel(Layouts.flexibleGridLayout()
            .rowsColumns(5, 1)
            .fixColumnWidths(true)
            .build());
    controlPanel.add(applicationPanel);
    controlPanel.add(activityPanel);
    controlPanel.add(scenarioPanel);
    controlPanel.add(userBase);
    controlPanel.add(chartControlPanel);

    JPanel controlBase = new JPanel(new BorderLayout());
    controlBase.add(controlPanel, BorderLayout.NORTH);

    setLayout(new BorderLayout());
    add(controlBase, BorderLayout.WEST);
    if (pluginPanel != null) {
      JTabbedPane tabPanel = new JTabbedPane();
      tabPanel.addTab("Load test", chartBase);
      tabPanel.addTab("Plugins", pluginPanel);
      add(tabPanel, BorderLayout.CENTER);
    }
    else {
      add(chartBase, BorderLayout.CENTER);
    }
    add(initializeSouthPanel(), BorderLayout.SOUTH);
  }

  private static JPanel initializeSouthPanel() {
    JPanel southPanel = new JPanel(Layouts.flowLayout(FlowLayout.TRAILING));
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(new MemoryUsageField(DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS));

    return southPanel;
  }

  private ItemRandomizerPanel<UsageScenario<T>> initializeScenarioPanel() {
    ItemRandomizerPanel<UsageScenario<T>> panel = new ItemRandomizerPanel<>(loadTestModel.getScenarioChooser());
    panel.setBorder(BorderFactory.createTitledBorder("Usage scenarios"));
    panel.addSelectedItemListener(this::onScenarioSelectionChanged);

    return panel;
  }

  private JPanel initializeUserPanel() {
    User user = loadTestModel.getUser();
    JTextField usernameField = Components.textField()
            .columns(LARGE_TEXT_FIELD_COLUMNS)
            .initialValue(user.getUsername())
            .build();
    JPasswordField passwordField = new JPasswordField(String.valueOf(user.getPassword()));
    passwordField.setColumns(LARGE_TEXT_FIELD_COLUMNS);
    ActionListener userInfoListener = e -> loadTestModel.setUser(User.user(usernameField.getText(), passwordField.getPassword()));
    usernameField.addActionListener(userInfoListener);
    passwordField.addActionListener(userInfoListener);
    JPanel userBase = new JPanel(Layouts.flexibleGridLayout()
            .rowsColumns(2, 2)
            .fixRowHeights(true)
            .fixedRowHeight(TextComponents.getPreferredTextFieldHeight())
            .build());
    userBase.setBorder(BorderFactory.createTitledBorder("User"));

    userBase.add(new JLabel("Username"));
    userBase.add(usernameField);
    userBase.add(new JLabel("Password"));
    userBase.add(passwordField);

    return userBase;
  }

  private JPanel initializeApplicationPanel() {
    JPanel applicationCountPanel = new JPanel(Layouts.borderLayout());
    applicationCountPanel.add(initializeApplicationCountButtonPanel(), BorderLayout.WEST);
    applicationCountPanel.add(Components.integerField()
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValueObserver(loadTestModel.applicationCountObserver())
            .build(), BorderLayout.CENTER);
    applicationCountPanel.add(Components.integerSpinner(loadTestModel.getApplicationBatchSizeValue())
            .editable(false)
            .columns(SMALL_TEXT_FIELD_COLUMNS)
            .toolTipText("Application batch size")
            .build(), BorderLayout.EAST);

    JPanel applicationPanel = new JPanel(Layouts.borderLayout());
    applicationPanel.setBorder(BorderFactory.createTitledBorder("Applications"));
    applicationPanel.add(applicationCountPanel, BorderLayout.NORTH);

    return applicationPanel;
  }

  private JPanel initializeApplicationCountButtonPanel() {
    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 0, 0));
    buttonPanel.add(initializeAddRemoveApplicationButton(false));
    buttonPanel.add(initializeAddRemoveApplicationButton(true));

    return buttonPanel;
  }

  private JButton initializeAddRemoveApplicationButton(boolean add) {
    JButton button = Control.builder(add ? loadTestModel::addApplicationBatch : loadTestModel::removeApplicationBatch)
            .caption(add ? "+" : "-")
            .description(add ? "Add application batch" : "Remove application batch")
            .build()
            .createButton();
    button.setPreferredSize(TextComponents.DIMENSION_TEXT_FIELD_SQUARE);
    button.setMargin(new Insets(0, 0, 0, 0));

    return button;
  }

  private JPanel initializeChartControlPanel() {
    JPanel controlPanel = new JPanel(Layouts.flexibleGridLayout()
            .rowsColumns(1, 2)
            .fixRowHeights(true)
            .build());
    controlPanel.setBorder(BorderFactory.createTitledBorder("Charts"));
    controlPanel.add(Components.checkBox(loadTestModel.getCollectChartDataState())
            .caption("Collect chart data")
            .build());
    controlPanel.add(new JButton(Control.builder(loadTestModel::resetChartData)
            .caption("Reset")
            .build()));

    return controlPanel;
  }

  private JPanel initializeChartPanel() {
    JFreeChart thinkTimeChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getThinkTimeDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(thinkTimeChart);
    ChartPanel thinkTimeChartPanel = new ChartPanel(thinkTimeChart);
    thinkTimeChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart numberOfApplicationsChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getNumberOfApplicationsDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(numberOfApplicationsChart);
    ChartPanel numberOfApplicationsChartPanel = new ChartPanel(numberOfApplicationsChart);
    numberOfApplicationsChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart usageScenarioChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getUsageScenarioDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(usageScenarioChart);
    ChartPanel usageScenarioChartPanel = new ChartPanel(usageScenarioChart);
    usageScenarioChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart failureChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getUsageScenarioFailureDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(failureChart);
    ChartPanel failureChartPanel = new ChartPanel(failureChart);
    failureChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart memoryUsageChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getMemoryUsageDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(memoryUsageChart);
    ChartPanel memoryUsageChartPanel = new ChartPanel(memoryUsageChart);
    memoryUsageChartPanel.setBorder(BorderFactory.createEtchedBorder());

    JFreeChart systemLoadChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getSystemLoadDataset(), PlotOrientation.VERTICAL, true, true, false);
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

  private JPanel initializeActivityPanel() {
    return Components.panel(Layouts.flexibleGridLayout()
                    .rowsColumns(4, 2)
                    .fixRowHeights(true)
                    .fixedRowHeight(TextComponents.getPreferredTextFieldHeight())
                    .build())
            .add(new JLabel("Max. think time", SwingConstants.CENTER))
            .add(Components.integerSpinner(loadTestModel.getMaximumThinkTimeValue())
                    .stepSize(SPINNER_STEP_SIZE)
                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                    .build())
            .add(new JLabel("Min. think time", SwingConstants.CENTER))
            .add(Components.integerSpinner(loadTestModel.getMinimumThinkTimeValue())
                    .stepSize(SPINNER_STEP_SIZE)
                    .columns(SMALL_TEXT_FIELD_COLUMNS)
                    .build())
            .add(Components.toggleButton(loadTestModel.getPausedState())
                    .caption("Pause")
                    .mnemonic('P')
                    .build())
            .border(BorderFactory.createTitledBorder("Activity"))
            .build();
  }

  private void onScenarioSelectionChanged(List<ItemRandomizer.RandomItem<UsageScenario<T>>> selectedScenarios) {
    scenarioBase.removeAll();
    for (ItemRandomizer.RandomItem<UsageScenario<T>> selectedItem : selectedScenarios) {
      scenarioBase.add(createScenarioPanel(selectedItem.getItem()));
    }
    validate();
    repaint();
  }

  private JPanel createScenarioPanel(UsageScenario<T> item) {
    JFreeChart scenarioDurationChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getScenarioDurationDataset(item.getName()),
            PlotOrientation.VERTICAL, true, true, false);
    setColors(scenarioDurationChart);
    ChartPanel scenarioDurationChartPanel = new ChartPanel(scenarioDurationChart);
    scenarioDurationChartPanel.setBorder(BorderFactory.createEtchedBorder());

    DeviationRenderer renderer = new DeviationRenderer();
    renderer.setDefaultShapesVisible(false);
    scenarioDurationChart.getXYPlot().setRenderer(renderer);

    JPanel basePanel = new JPanel(Layouts.borderLayout());
    JTabbedPane tabPanel = new JTabbedPane();
    tabPanel.addTab("Duration", scenarioDurationChartPanel);

    JTextArea exceptionsArea = new JTextArea();
    JPanel scenarioExceptionPanel = new JPanel(Layouts.borderLayout());
    scenarioExceptionPanel.add(exceptionsArea, BorderLayout.CENTER);
    JButton refreshButton = new JButton(new RefreshExceptionsAction(exceptionsArea, item));
    refreshButton.doClick();
    JButton clearButton = new JButton(new ClearExceptionsAction(exceptionsArea, item));

    JScrollPane exceptionScroller = new JScrollPane(exceptionsArea);

    scenarioExceptionPanel.add(exceptionScroller, BorderLayout.CENTER);
    JPanel buttonPanel = new JPanel(Layouts.borderLayout());
    buttonPanel.add(refreshButton, BorderLayout.NORTH);
    buttonPanel.add(clearButton, BorderLayout.SOUTH);

    scenarioExceptionPanel.add(buttonPanel, BorderLayout.EAST);

    tabPanel.addTab("Exceptions", scenarioExceptionPanel);

    basePanel.add(tabPanel, BorderLayout.CENTER);

    return basePanel;
  }

  private void setColors(JFreeChart chart) {
    ChartUtil.linkColors(this, chart);
  }

  private abstract static class ExceptionsAction extends AbstractAction {
    private final JTextArea exceptionsTextArea;
    private final UsageScenario<?> scenario;

    private ExceptionsAction(String name, JTextArea exceptionsTextArea, UsageScenario<?> scenario) {
      super(name);
      this.exceptionsTextArea = exceptionsTextArea;
      this.scenario = scenario;
    }

    JTextArea getExceptionsTextArea() {
      return exceptionsTextArea;
    }

    UsageScenario<?> getScenario() {
      return scenario;
    }
  }

  private static final class ClearExceptionsAction extends ExceptionsAction {

    private ClearExceptionsAction(JTextArea exceptionsArea, UsageScenario<?> scenario) {
      super("Clear", exceptionsArea, scenario);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      getScenario().clearExceptions();
      getExceptionsTextArea().replaceRange("", 0, getExceptionsTextArea().getDocument().getLength());
    }
  }

  private static final class RefreshExceptionsAction extends ExceptionsAction {

    private RefreshExceptionsAction(JTextArea exceptionsArea, UsageScenario<?> scenario) {
      super("Refresh", exceptionsArea, scenario);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      getExceptionsTextArea().replaceRange("", 0, getExceptionsTextArea().getDocument().getLength());
      List<Exception> exceptions = getScenario().getExceptions();
      for (Exception exception : exceptions) {
        getExceptionsTextArea().append(exception.getMessage());
        getExceptionsTextArea().append(Util.LINE_SEPARATOR);
        getExceptionsTextArea().append(Util.LINE_SEPARATOR);
      }
    }
  }
}