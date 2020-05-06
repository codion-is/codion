/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.ui.loadtest;

import org.jminor.common.Util;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.common.value.Nullable;
import org.jminor.common.value.Values;
import org.jminor.swing.common.tools.loadtest.LoadTest;
import org.jminor.swing.common.tools.loadtest.LoadTestModel;
import org.jminor.swing.common.tools.loadtest.ScenarioException;
import org.jminor.swing.common.tools.loadtest.UsageScenario;
import org.jminor.swing.common.tools.randomizer.ItemRandomizer;
import org.jminor.swing.common.tools.ui.randomizer.ItemRandomizerPanel;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.control.ToggleControl;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.TextFields;
import org.jminor.swing.common.ui.value.NumericalValues;

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
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.jminor.swing.common.ui.icons.Icons.icons;

/**
 * A default UI component for the LoadTestModel class.
 * @see LoadTestModel
 */
public final class LoadTestPanel extends JPanel {

  private static final int DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
  private static final double DEFAULT_SCREEN_SIZE_RATIO = 0.75;
  private static final int LARGE_TEXT_FIELD_COLUMNS = 8;
  private static final int SMALL_TEXT_FIELD_COLUMNS = 3;
  private static final int SPINNER_STEP_SIZE = 10;
  private static final double RESIZE_WEIGHT = 0.8;

  private final LoadTest loadTestModel;

  private final JPanel scenarioBase = new JPanel(Layouts.gridLayout(0, 1));
  private final JPanel pluginPanel;
  private final ItemRandomizerPanel scenarioPanel;

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   */
  public LoadTestPanel(final LoadTest loadTestModel) {
    this(loadTestModel, null);
  }

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   * @param pluginPanel a panel to add as a plugin panel
   */
  public LoadTestPanel(final LoadTest loadTestModel, final JPanel pluginPanel) {
    requireNonNull(loadTestModel, "loadTestModel");
    this.loadTestModel = loadTestModel;
    this.pluginPanel = pluginPanel;
    this.scenarioPanel = initializeScenarioPanel();
    initializeUI();
  }

  /**
   * @return the load test model this panel is based on
   */
  public LoadTest getModel() {
    return loadTestModel;
  }

  /**
   * Shows a frame containing this load test panel
   * @return the frame
   */
  public JFrame showFrame() {
    final JFrame frame = new JFrame();
    frame.setIconImage(icons().logo().getImage());
    final String title = "JMinor - " + loadTestModel.getTitle();
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        frame.setTitle(title + " - Closing...");
        loadTestModel.shutdown();
      }
    });
    frame.setTitle(title);
    frame.getContentPane().add(this);
    Windows.resizeWindow(frame, DEFAULT_SCREEN_SIZE_RATIO);
    Windows.centerWindow(frame);
    frame.setVisible(true);

    return frame;
  }

  private void initializeUI() {
    final JPanel chartBase = initializeChartPanel();
    final JPanel activityPanel = initializeActivityPanel();
    final JPanel applicationPanel = initializeApplicationPanel();
    final JPanel userBase = initializeUserPanel();
    final JPanel chartControlPanel = initializeChartControlPanel();

    final JPanel controlPanel = new JPanel(Layouts.flexibleGridLayout(5, 1, false, true));
    controlPanel.add(applicationPanel);
    controlPanel.add(activityPanel);
    controlPanel.add(scenarioPanel);
    controlPanel.add(userBase);
    controlPanel.add(chartControlPanel);

    final JPanel controlBase = new JPanel(new BorderLayout());
    controlBase.add(controlPanel, BorderLayout.NORTH);

    setLayout(new BorderLayout());
    add(controlBase, BorderLayout.WEST);
    if (pluginPanel != null) {
      final JTabbedPane tabPanel = new JTabbedPane();
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
    final JPanel southPanel = new JPanel(Layouts.flowLayout(FlowLayout.TRAILING));
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(Components.createMemoryUsageField(DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS));

    return southPanel;
  }

  private ItemRandomizerPanel<UsageScenario> initializeScenarioPanel() {
    final ItemRandomizerPanel<UsageScenario> panel = new ItemRandomizerPanel<>(loadTestModel.getScenarioChooser());
    panel.setBorder(BorderFactory.createTitledBorder("Usage scenarios"));
    panel.addSelectedItemListener(this::onScenarioSelectionChanged);

    return panel;
  }

  private JPanel initializeUserPanel() {
    final User user = loadTestModel.getUser();
    final JTextField usernameField = new JTextField(user.getUsername());
    usernameField.setColumns(LARGE_TEXT_FIELD_COLUMNS);
    final JPasswordField passwordField = new JPasswordField(String.valueOf(user.getPassword()));
    passwordField.setColumns(LARGE_TEXT_FIELD_COLUMNS);
    final ActionListener userInfoListener = e -> loadTestModel.setUser(Users.user(usernameField.getText(), passwordField.getPassword()));
    usernameField.addActionListener(userInfoListener);
    passwordField.addActionListener(userInfoListener);
    final FlexibleGridLayout layout = Layouts.flexibleGridLayout(2, 2, true, false);
    layout.setFixedRowHeight(TextFields.getPreferredTextFieldHeight());
    final JPanel userBase = new JPanel(layout);
    userBase.setBorder(BorderFactory.createTitledBorder("User"));

    userBase.add(new JLabel("Username"));
    userBase.add(usernameField);
    userBase.add(new JLabel("Password"));
    userBase.add(passwordField);

    return userBase;
  }

  private JPanel initializeApplicationPanel() {
    final IntegerField applicationCountField = new IntegerField();
    applicationCountField.setHorizontalAlignment(JTextField.CENTER);
    Values.propertyValue(loadTestModel, "applicationCount", int.class,
            loadTestModel.applicationCountObserver()).link(NumericalValues.integerValue(applicationCountField, Nullable.NO));
    final JPanel applicationPanel = new JPanel(Layouts.borderLayout());
    applicationPanel.setBorder(BorderFactory.createTitledBorder("Applications"));

    final JSpinner batchSizeSpinner = new JSpinner(NumericalValues.integerValueSpinnerModel(loadTestModel, "applicationBatchSize",
            loadTestModel.applicationBatchSizeObserver()));
    batchSizeSpinner.setToolTipText("Application batch size");
    ((JSpinner.DefaultEditor) batchSizeSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) batchSizeSpinner.getEditor()).getTextField().setColumns(SMALL_TEXT_FIELD_COLUMNS);

    final JPanel applicationCountPanel = new JPanel(Layouts.borderLayout());
    applicationCountPanel.add(initializeApplicationCountButtonPanel(), BorderLayout.WEST);
    applicationCountPanel.add(applicationCountField, BorderLayout.CENTER);
    applicationCountPanel.add(batchSizeSpinner, BorderLayout.EAST);

    applicationPanel.add(applicationCountPanel, BorderLayout.NORTH);

    return applicationPanel;
  }

  private JPanel initializeApplicationCountButtonPanel() {
    final JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 0, 0));
    buttonPanel.add(initializeAddRemoveApplicationButton(false));
    buttonPanel.add(initializeAddRemoveApplicationButton(true));

    return buttonPanel;
  }

  private JButton initializeAddRemoveApplicationButton(final boolean add) {
    final JButton button = new JButton(Controls.control(() -> {
      if (add) {
        loadTestModel.addApplicationBatch();
      }
      else {
        loadTestModel.removeApplicationBatch();
      }
    }, add ? "+" : "-"));
    button.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setToolTipText(add ? "Add application batch" : "Remove application batch");

    return button;
  }

  private JPanel initializeChartControlPanel() {
    final JPanel controlPanel = new JPanel(Layouts.flexibleGridLayout(1, 2, true, false));
    controlPanel.setBorder(BorderFactory.createTitledBorder("Charts"));
    controlPanel.add(ControlProvider.createCheckBox(Controls.toggleControl(loadTestModel, "collectChartData",
            "Collect chart data", loadTestModel.collectChartDataObserver())));
    controlPanel.add(new JButton(Controls.control(loadTestModel::resetChartData, "Reset")));

    return controlPanel;
  }

  private JPanel initializeChartPanel() {
    final JFreeChart thinkTimeChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getThinkTimeDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(thinkTimeChart);
    final ChartPanel thinkTimeChartPanel = new ChartPanel(thinkTimeChart);
    thinkTimeChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart numberOfApplicationsChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getNumberOfApplicationsDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(numberOfApplicationsChart);
    final ChartPanel numberOfApplicationsChartPanel = new ChartPanel(numberOfApplicationsChart);
    numberOfApplicationsChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart usageScenarioChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getUsageScenarioDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(usageScenarioChart);
    final ChartPanel usageScenarioChartPanel = new ChartPanel(usageScenarioChart);
    usageScenarioChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart failureChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getUsageScenarioFailureDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(failureChart);
    final ChartPanel failureChartPanel = new ChartPanel(failureChart);
    failureChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart memoryUsageChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getMemoryUsageDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(memoryUsageChart);
    final ChartPanel memoryUsageChartPanel = new ChartPanel(memoryUsageChart);
    memoryUsageChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart systemLoadChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getSystemLoadDataset(), PlotOrientation.VERTICAL, true, true, false);
    setColors(systemLoadChart);
    systemLoadChart.getXYPlot().getRangeAxis().setRange(0, 100);
    final ChartPanel systemLoadChartPanel = new ChartPanel(systemLoadChart);
    systemLoadChartPanel.setBorder(BorderFactory.createEtchedBorder());

    usageScenarioChartPanel.setBorder(BorderFactory.createTitledBorder("Scenarios run per second"));
    thinkTimeChartPanel.setBorder(BorderFactory.createTitledBorder("Think time (ms)"));
    numberOfApplicationsChartPanel.setBorder(BorderFactory.createTitledBorder("Application count"));
    memoryUsageChartPanel.setBorder(BorderFactory.createTitledBorder("Memory usage (MB)"));
    systemLoadChartPanel.setBorder(BorderFactory.createTitledBorder("System load"));
    failureChartPanel.setBorder(BorderFactory.createTitledBorder("Scenario failure rate (%)"));

    final JTabbedPane twoTab = new JTabbedPane();
    twoTab.addTab("Scenarios run", usageScenarioChartPanel);
    twoTab.addTab("Failed runs", failureChartPanel);

    final JPanel bottomPanel = new JPanel(new GridLayout(1, 4, 0, 0));
    bottomPanel.add(memoryUsageChartPanel);
    bottomPanel.add(systemLoadChartPanel);
    bottomPanel.add(thinkTimeChartPanel);
    bottomPanel.add(numberOfApplicationsChartPanel);

    final JSplitPane two = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    two.setOneTouchExpandable(true);
    two.setLeftComponent(twoTab);
    two.setRightComponent(bottomPanel);
    two.setResizeWeight(RESIZE_WEIGHT);

    final JPanel chartBase = new JPanel(new BorderLayout(0, 0));

    final JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab("Overview", two);
    tabPane.addTab("Scenarios", scenarioBase);

    chartBase.add(tabPane);

    return chartBase;
  }

  private JPanel initializeActivityPanel() {
    final SpinnerNumberModel maxSpinnerModel = NumericalValues.integerValueSpinnerModel(loadTestModel, "maximumThinkTime",
            loadTestModel.maximumThinkTimeObserver());
    maxSpinnerModel.setStepSize(SPINNER_STEP_SIZE);
    final JSpinner maxThinkTimeSpinner = new JSpinner(maxSpinnerModel);
    ((JSpinner.DefaultEditor) maxThinkTimeSpinner.getEditor()).getTextField().setColumns(SMALL_TEXT_FIELD_COLUMNS);

    final SpinnerNumberModel minSpinnerModel = NumericalValues.integerValueSpinnerModel(loadTestModel, "minimumThinkTime",
            loadTestModel.getMinimumThinkTimeObserver());
    minSpinnerModel.setStepSize(SPINNER_STEP_SIZE);
    final JSpinner minThinkTimeSpinner = new JSpinner(minSpinnerModel);
    ((JSpinner.DefaultEditor) minThinkTimeSpinner.getEditor()).getTextField().setColumns(SMALL_TEXT_FIELD_COLUMNS);

    final ToggleControl pauseControl = Controls.toggleControl(loadTestModel, "paused", "Pause", loadTestModel.getPauseObserver());
    pauseControl.setMnemonic('P');

    final FlexibleGridLayout layout = Layouts.flexibleGridLayout(4, 2, true, false);
    layout.setFixedRowHeight(TextFields.getPreferredTextFieldHeight());
    final JPanel thinkTimePanel = new JPanel(layout);
    thinkTimePanel.add(new JLabel("Max. think time", JLabel.CENTER));
    thinkTimePanel.add(maxThinkTimeSpinner);
    thinkTimePanel.add(new JLabel("Min. think time", JLabel.CENTER));
    thinkTimePanel.add(minThinkTimeSpinner);
    thinkTimePanel.add(ControlProvider.createToggleButton(pauseControl));

    thinkTimePanel.setBorder(BorderFactory.createTitledBorder("Activity"));

    return thinkTimePanel;
  }

  private void onScenarioSelectionChanged(final List<ItemRandomizer.RandomItem<UsageScenario>> selectedScenarios) {
    scenarioBase.removeAll();
    for (final ItemRandomizer.RandomItem<UsageScenario> selectedItem : selectedScenarios) {
      scenarioBase.add(createScenarioPanel(selectedItem.getItem()));
    }
    validate();
    repaint();
  }

  private JPanel createScenarioPanel(final UsageScenario item) {
    final JFreeChart scenarioDurationChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getScenarioDurationDataset(item.getName()),
            PlotOrientation.VERTICAL, true, true, false);
    setColors(scenarioDurationChart);
    final ChartPanel scenarioDurationChartPanel = new ChartPanel(scenarioDurationChart);
    scenarioDurationChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final DeviationRenderer renderer = new DeviationRenderer();
    renderer.setDefaultShapesVisible(false);
    scenarioDurationChart.getXYPlot().setRenderer(renderer);

    final JPanel basePanel = new JPanel(Layouts.borderLayout());
    final JTabbedPane tabPanel = new JTabbedPane();
    tabPanel.addTab("Duration", scenarioDurationChartPanel);

    final JTextArea exceptionsArea = new JTextArea();
    final JPanel scenarioExceptionPanel = new JPanel(Layouts.borderLayout());
    scenarioExceptionPanel.add(exceptionsArea, BorderLayout.CENTER);
    final JButton refreshButton = new JButton(new RefreshExceptionsAction(exceptionsArea, item));
    refreshButton.doClick();
    final JButton clearButton = new JButton(new ClearExceptionsAction(exceptionsArea, item));

    final JScrollPane exceptionScroller = new JScrollPane(exceptionsArea);

    scenarioExceptionPanel.add(exceptionScroller, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(Layouts.borderLayout());
    buttonPanel.add(refreshButton, BorderLayout.NORTH);
    buttonPanel.add(clearButton, BorderLayout.SOUTH);

    scenarioExceptionPanel.add(buttonPanel, BorderLayout.EAST);

    tabPanel.addTab("Exceptions", scenarioExceptionPanel);

    basePanel.add(tabPanel, BorderLayout.CENTER);

    return basePanel;
  }

  private void setColors(final JFreeChart chart) {
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    chart.setBackgroundPaint(this.getBackground());
  }

  private abstract static class ExceptionsAction extends AbstractAction {
    private final JTextArea exceptionsTextArea;
    private final UsageScenario scenario;

    private ExceptionsAction(final String name, final JTextArea exceptionsTextArea, final UsageScenario scenario) {
      super(name);
      this.exceptionsTextArea = exceptionsTextArea;
      this.scenario = scenario;
    }

    JTextArea getExceptionsTextArea() {
      return exceptionsTextArea;
    }

    UsageScenario getScenario() {
      return scenario;
    }
  }

  private static final class ClearExceptionsAction extends ExceptionsAction {

    private ClearExceptionsAction(final JTextArea exceptionsArea, final UsageScenario scenario) {
      super("Clear", exceptionsArea, scenario);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      getScenario().clearExceptions();
      getExceptionsTextArea().replaceRange("", 0, getExceptionsTextArea().getDocument().getLength());
    }
  }

  private static final class RefreshExceptionsAction extends ExceptionsAction {

    private RefreshExceptionsAction(final JTextArea exceptionsArea, final UsageScenario scenario) {
      super("Refresh", exceptionsArea, scenario);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      getExceptionsTextArea().replaceRange("", 0, getExceptionsTextArea().getDocument().getLength());
      final List<ScenarioException> exceptions = getScenario().getExceptions();
      for (final ScenarioException exception : exceptions) {
        getExceptionsTextArea().append(exception.getMessage());
        getExceptionsTextArea().append(Util.LINE_SEPARATOR);
        getExceptionsTextArea().append(Util.LINE_SEPARATOR);
      }
    }
  }
}