/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.ui;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Values;
import org.jminor.swing.common.tools.ItemRandomizer;
import org.jminor.swing.common.tools.LoadTest;
import org.jminor.swing.common.tools.LoadTestModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.UpdateTrigger;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.textfield.IntegerField;

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
import java.util.Objects;

/**
 * A default UI component for the LoadTestModel class.
 * @see org.jminor.swing.common.tools.LoadTestModel
 */
public final class LoadTestPanel extends JPanel {

  private static final int DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
  private static final double DEFAULT_SCREEN_SIZE_RATIO = 0.75;
  private static final int LARGE_TEXT_FIELD_COLUMNS = 8;
  private static final int SMALL_TEXT_FIELD_COLUMNS = 3;
  private static final int SPINNER_STEP_SIZE = 10;
  private static final int COMPONENT_GAP = 0;
  private static final double RESIZE_WEIGHT = 0.8;

  private final LoadTest loadTestModel;

  private final JPanel scenarioBase = new JPanel(UiUtil.createGridLayout(COMPONENT_GAP, 1));
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
    Objects.requireNonNull(loadTestModel, "loadTestModel");
    this.loadTestModel = loadTestModel;
    this.pluginPanel = pluginPanel;
    this.scenarioPanel = initializeScenarioPanel();
    initializeUI();
    handleScenarioSelected();
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
    frame.setIconImage(Images.loadImage("jminor_logo32.gif").getImage());
    final String title = "JMinor - " + loadTestModel.getTitle();
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        frame.setTitle(title + " - Closing...");
        loadTestModel.exit();
      }
    });
    frame.setTitle(title);
    frame.getContentPane().add(this);
    UiUtil.resizeWindow(frame, DEFAULT_SCREEN_SIZE_RATIO);
    UiUtil.centerWindow(frame);
    frame.setVisible(true);

    return frame;
  }

  private void initializeUI() {
    final JPanel chartBase = initializeChartPanel();
    final JPanel activityPanel = initializeActivityPanel();
    final JPanel applicationPanel = initializeApplicationPanel();
    final JPanel userBase = initializeUserPanel();
    final JPanel chartControlPanel = initializeChartControlPanel();

    final JPanel controlPanel = new JPanel(UiUtil.createFlexibleGridLayout(5, 1, false, true));
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
    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(UiUtil.createMemoryUsageField(DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS));

    return southPanel;
  }

  private ItemRandomizerPanel initializeScenarioPanel() {
    final ItemRandomizerPanel<LoadTestModel.UsageScenario> panel = new ItemRandomizerPanel<>(loadTestModel.getScenarioChooser());
    panel.setBorder(BorderFactory.createTitledBorder("Usage scenarios"));
    panel.addSelectedItemListener(this::handleScenarioSelected);

    return panel;
  }

  private JPanel initializeUserPanel() {
    final User user = loadTestModel.getUser();
    final JTextField usernameField = new JTextField(user.getUsername());
    usernameField.setColumns(LARGE_TEXT_FIELD_COLUMNS);
    final JPasswordField passwordField = new JPasswordField(String.valueOf(user.getPassword()));
    passwordField.setColumns(LARGE_TEXT_FIELD_COLUMNS);
    final ActionListener userInfoListener = e -> loadTestModel.setUser(new User(usernameField.getText(), passwordField.getPassword()));
    usernameField.addActionListener(userInfoListener);
    passwordField.addActionListener(userInfoListener);
    final FlexibleGridLayout layout = UiUtil.createFlexibleGridLayout(2, 2, true, false);
    layout.setFixedRowHeight(UiUtil.getPreferredTextFieldHeight());
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
    ValueLinks.integerValueLink(applicationCountField, Values.propertyValue(loadTestModel, "applicationCount",
            int.class, loadTestModel.applicationCountObserver()), false, UpdateTrigger.KEYSTROKE);
    final JPanel applicationPanel = new JPanel(UiUtil.createBorderLayout());
    applicationPanel.setBorder(BorderFactory.createTitledBorder("Applications"));

    final JSpinner batchSizeSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(loadTestModel, "applicationBatchSize",
            loadTestModel.applicationBatchSizeObserver()));
    batchSizeSpinner.setToolTipText("Application batch size");
    ((JSpinner.DefaultEditor) batchSizeSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) batchSizeSpinner.getEditor()).getTextField().setColumns(SMALL_TEXT_FIELD_COLUMNS);

    final JPanel applicationCountPanel = new JPanel(UiUtil.createBorderLayout());
    applicationCountPanel.add(initializeApplicationCountButtonPanel(), BorderLayout.WEST);
    applicationCountPanel.add(applicationCountField, BorderLayout.CENTER);
    applicationCountPanel.add(batchSizeSpinner, BorderLayout.EAST);

    applicationPanel.add(applicationCountPanel, BorderLayout.NORTH);

    return applicationPanel;
  }

  private JPanel initializeApplicationCountButtonPanel() {
    final JPanel buttonPanel = new JPanel(new GridLayout(1, 2, COMPONENT_GAP, COMPONENT_GAP));
    buttonPanel.add(initializeAddRemoveApplicationButton(false));
    buttonPanel.add(initializeAddRemoveApplicationButton(true));

    return buttonPanel;
  }

  private JButton initializeAddRemoveApplicationButton(final boolean add) {
    final JButton button = new JButton(new Control(add ? "+" : "-") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (add) {
          loadTestModel.addApplicationBatch();
        }
        else {
          loadTestModel.removeApplicationBatch();
        }
      }
    });
    button.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
    button.setMargin(new Insets(COMPONENT_GAP, COMPONENT_GAP, COMPONENT_GAP, COMPONENT_GAP));
    button.setToolTipText(add ? "Add application batch" : "Remove application batch");

    return button;
  }

  private JPanel initializeChartControlPanel() {
    final JPanel controlPanel = new JPanel(UiUtil.createFlexibleGridLayout(1, 2, true, false));
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

    usageScenarioChartPanel.setBorder(BorderFactory.createTitledBorder("Scenarios run per second"));
    thinkTimeChartPanel.setBorder(BorderFactory.createTitledBorder("Think time (ms)"));
    numberOfApplicationsChartPanel.setBorder(BorderFactory.createTitledBorder("Application count"));
    memoryUsageChartPanel.setBorder(BorderFactory.createTitledBorder("Memory usage (MB)"));
    failureChartPanel.setBorder(BorderFactory.createTitledBorder("Scenario failure rate (%)"));

    final JTabbedPane twoTab = new JTabbedPane();
    twoTab.addTab("Scenarios run", usageScenarioChartPanel);
    twoTab.addTab("Failed runs", failureChartPanel);

    final JPanel bottomPanel = new JPanel(new GridLayout(1, 3, COMPONENT_GAP, COMPONENT_GAP));
    bottomPanel.add(memoryUsageChartPanel);
    bottomPanel.add(thinkTimeChartPanel);
    bottomPanel.add(numberOfApplicationsChartPanel);

    final JSplitPane two = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    two.setOneTouchExpandable(true);
    two.setLeftComponent(twoTab);
    two.setRightComponent(bottomPanel);
    two.setResizeWeight(RESIZE_WEIGHT);

    final JPanel chartBase = new JPanel(new BorderLayout(COMPONENT_GAP, COMPONENT_GAP));

    final JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab("Overview", two);
    tabPane.addTab("Scenarios", scenarioBase);

    chartBase.add(tabPane);

    return chartBase;
  }

  private JPanel initializeActivityPanel() {
    final SpinnerNumberModel maxSpinnerModel = ValueLinks.intSpinnerValueLink(loadTestModel, "maximumThinkTime",
            loadTestModel.maximumThinkTimeObserver());
    maxSpinnerModel.setStepSize(SPINNER_STEP_SIZE);
    final JSpinner maxThinkTimeSpinner = new JSpinner(maxSpinnerModel);
    ((JSpinner.DefaultEditor) maxThinkTimeSpinner.getEditor()).getTextField().setColumns(SMALL_TEXT_FIELD_COLUMNS);

    final SpinnerNumberModel minSpinnerModel = ValueLinks.intSpinnerValueLink(loadTestModel, "minimumThinkTime",
            loadTestModel.getMinimumThinkTimeObserver());
    minSpinnerModel.setStepSize(SPINNER_STEP_SIZE);
    final JSpinner minThinkTimeSpinner = new JSpinner(minSpinnerModel);
    ((JSpinner.DefaultEditor) minThinkTimeSpinner.getEditor()).getTextField().setColumns(SMALL_TEXT_FIELD_COLUMNS);

    final SpinnerNumberModel warningSpinnerModel = ValueLinks.intSpinnerValueLink(loadTestModel, "warningTime",
            loadTestModel.getWarningTimeObserver());
    warningSpinnerModel.setStepSize(SPINNER_STEP_SIZE);
    final JSpinner warningTimeSpinner = new JSpinner(warningSpinnerModel);
    ((JSpinner.DefaultEditor) warningTimeSpinner.getEditor()).getTextField().setColumns(SMALL_TEXT_FIELD_COLUMNS);
    warningTimeSpinner.setToolTipText("A work request is considered 'delayed' if the time it takes to process it exceeds this value (ms)");

    final Controls.ToggleControl pauseControl = Controls.toggleControl(loadTestModel, "paused", "Pause", loadTestModel.getPauseObserver());
    pauseControl.setMnemonic('P');

    final FlexibleGridLayout layout = UiUtil.createFlexibleGridLayout(4, 2, true, false);
    layout.setFixedRowHeight(UiUtil.getPreferredTextFieldHeight());
    final JPanel thinkTimePanel = new JPanel(layout);
    thinkTimePanel.add(new JLabel("Max. think time", JLabel.CENTER));
    thinkTimePanel.add(maxThinkTimeSpinner);
    thinkTimePanel.add(new JLabel("Min. think time", JLabel.CENTER));
    thinkTimePanel.add(minThinkTimeSpinner);
    thinkTimePanel.add(new JLabel("Warning time", JLabel.CENTER));
    thinkTimePanel.add(warningTimeSpinner);
    thinkTimePanel.add(ControlProvider.createToggleButton(pauseControl));

    thinkTimePanel.setBorder(BorderFactory.createTitledBorder("Activity"));

    return thinkTimePanel;
  }

  @SuppressWarnings({"unchecked"})
  private void handleScenarioSelected() {
    scenarioBase.removeAll();
    for (final Object selectedItem : scenarioPanel.getSelectedItems()) {
      final ItemRandomizer.RandomItem<LoadTest.UsageScenario> item = (ItemRandomizer.RandomItem<LoadTest.UsageScenario>) selectedItem;
      final JPanel panel = createScenarioPanel(item.getItem());
      scenarioBase.add(panel);
    }
    validate();
    repaint();
  }

  private JPanel createScenarioPanel(final LoadTest.UsageScenario item) {
    final JFreeChart scenarioDurationChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getScenarioDurationDataset(item.getName()),
            PlotOrientation.VERTICAL, true, true, false);
    setColors(scenarioDurationChart);
    final ChartPanel scenarioDurationChartPanel = new ChartPanel(scenarioDurationChart);
    scenarioDurationChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final DeviationRenderer renderer = new DeviationRenderer();
    renderer.setDefaultShapesVisible(false);
    scenarioDurationChart.getXYPlot().setRenderer(renderer);

    final JPanel basePanel = new JPanel(UiUtil.createBorderLayout());
    final JTabbedPane tabPanel = new JTabbedPane();
    tabPanel.addTab("Duration", scenarioDurationChartPanel);

    final JTextArea exceptionsArea = new JTextArea();
    final JPanel scenarioExceptionPanel = new JPanel(UiUtil.createBorderLayout());
    scenarioExceptionPanel.add(exceptionsArea, BorderLayout.CENTER);
    final JButton refreshButton = new JButton(new RefreshExceptionsAction(exceptionsArea, item));
    refreshButton.doClick();
    final JButton clearButton = new JButton(new ClearExceptionsAction(exceptionsArea, item));

    final JScrollPane exceptionScroller = new JScrollPane(exceptionsArea);

    scenarioExceptionPanel.add(exceptionScroller, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(UiUtil.createBorderLayout());
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
    private final LoadTest.UsageScenario scenario;

    private ExceptionsAction(final String name, final JTextArea exceptionsTextArea, final LoadTest.UsageScenario scenario) {
      super(name);
      this.exceptionsTextArea = exceptionsTextArea;
      this.scenario = scenario;
    }

    JTextArea getExceptionsTextArea() {
      return exceptionsTextArea;
    }

    LoadTest.UsageScenario getScenario() {
      return scenario;
    }
  }

  private static final class ClearExceptionsAction extends ExceptionsAction {

    private ClearExceptionsAction(final JTextArea exceptionsArea, final LoadTest.UsageScenario scenario) {
      super("Clear", exceptionsArea, scenario);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      getScenario().clearExceptions();
      getExceptionsTextArea().replaceRange("", 0, getExceptionsTextArea().getDocument().getLength());
    }
  }

  private static final class RefreshExceptionsAction extends ExceptionsAction {

    private RefreshExceptionsAction(final JTextArea exceptionsArea, final LoadTest.UsageScenario scenario) {
      super("Refresh", exceptionsArea, scenario);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      getExceptionsTextArea().replaceRange("", 0, getExceptionsTextArea().getDocument().getLength());
      final List<LoadTest.ScenarioException> exceptions = getScenario().getExceptions();
      for (final LoadTest.ScenarioException exception : exceptions) {
        getExceptionsTextArea().append(exception.getMessage());
        getExceptionsTextArea().append(Util.LINE_SEPARATOR);
        getExceptionsTextArea().append(Util.LINE_SEPARATOR);
      }
    }
  }
}