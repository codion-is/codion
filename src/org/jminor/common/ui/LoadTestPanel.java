/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.ItemRandomizer;
import org.jminor.common.model.LoadTest;
import org.jminor.common.model.LoadTestModel;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.control.IntBeanSpinnerValueLink;
import org.jminor.common.ui.control.IntBeanValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.common.ui.textfield.IntField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
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

/**
 * A default UI component for the LoadTestModel class.
 * @see org.jminor.common.model.LoadTestModel
 */
public final class LoadTestPanel extends JPanel {

  private static final int DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
  private static final double DEFAULT_SCREEN_SIZE_RATIO = 0.75;
  private final LoadTest loadTestModel;

  private final JPanel durationBase = new JPanel(new GridLayout(0, 1, 5, 5));
  private ItemRandomizerPanel randomizerPanel;

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   */
  public LoadTestPanel(final LoadTest loadTestModel) {
    Util.rejectNullValue(loadTestModel, "loadTestModel");
    this.loadTestModel = loadTestModel;
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
    final JFrame frame = UiUtil.createFrame(Images.loadImage("jminor_logo32.gif").getImage());
    final String title = "JMinor - " + loadTestModel.getClass().getSimpleName();
    loadTestModel.addExitListener(new ExitListener(frame));
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
    randomizerPanel = initializeScenarioPanel();
    final JPanel chartControlPanel = initializeChartControlPanel();

    final JPanel controlPanel = new JPanel(new FlexibleGridLayout(5, 1, 5, 5, false, true));
    controlPanel.add(applicationPanel);
    controlPanel.add(activityPanel);
    controlPanel.add(randomizerPanel);
    controlPanel.add(userBase);
    controlPanel.add(chartControlPanel);

    final JPanel controlBase = new JPanel(new BorderLayout());
    controlBase.add(controlPanel, BorderLayout.NORTH);

    setLayout(new BorderLayout());
    add(controlBase, BorderLayout.WEST);
    add(chartBase, BorderLayout.CENTER);
    add(initializeSouthPanel(), BorderLayout.SOUTH);
  }

  private JPanel initializeSouthPanel() {
    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(UiUtil.createMemoryUsageField(DEFAULT_MEMORY_USAGE_UPDATE_INTERVAL_MS));

    return southPanel;
  }

  private ItemRandomizerPanel initializeScenarioPanel() {
    durationBase.setBorder(BorderFactory.createTitledBorder("Scenario duration (ms)"));
    final ItemRandomizerPanel<LoadTestModel.UsageScenario> scenarioBase = new ItemRandomizerPanel<LoadTestModel.UsageScenario>(loadTestModel.getScenarioChooser());
    scenarioBase.setBorder(BorderFactory.createTitledBorder("Usage scenarios"));
    scenarioBase.addSelectedItemListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        handleScenarioSelected();
      }
    });

    return scenarioBase;
  }

  private JPanel initializeUserPanel() {
    final User user = loadTestModel.getUser();
    final JTextField txtUsername = new JTextField(user.getUsername());
    txtUsername.setColumns(8);
    final JTextField txtPassword = new JPasswordField(user.getPassword());
    txtPassword.setColumns(8);
    final ActionListener userInfoListener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        loadTestModel.setUser(new User(txtUsername.getText(), txtPassword.getText()));
      }
    };
    txtUsername.addActionListener(userInfoListener);
    txtPassword.addActionListener(userInfoListener);
    final FlexibleGridLayout layout = new FlexibleGridLayout(2, 2, 5, 5, true, false);
    layout.setFixedRowHeight(UiUtil.getPreferredTextFieldHeight());
    final JPanel userBase = new JPanel(layout);
    userBase.setBorder(BorderFactory.createTitledBorder("User"));

    userBase.add(new JLabel("Username"));
    userBase.add(txtUsername);
    userBase.add(new JLabel("Password"));
    userBase.add(txtPassword);

    return userBase;
  }

  private JPanel initializeApplicationPanel() {
    final IntField applicationCountField = new IntField();
    applicationCountField.setHorizontalAlignment(JTextField.CENTER);
    new IntBeanValueLink(applicationCountField, loadTestModel, "applicationCount", loadTestModel.applicationCountObserver(), LinkType.READ_ONLY);

    final JPanel applicationPanel = new JPanel(new BorderLayout(5, 5));
    applicationPanel.setBorder(BorderFactory.createTitledBorder("Applications"));

    final JSpinner spnBatchSize = new JSpinner(new IntBeanSpinnerValueLink(loadTestModel, "applicationBatchSize",
            loadTestModel.applicationBatchSizeObserver()).getSpinnerModel());
    spnBatchSize.setToolTipText("Application batch size");
    ((JSpinner.DefaultEditor) spnBatchSize.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnBatchSize.getEditor()).getTextField().setColumns(3);

    final JPanel applicationCountPanel = new JPanel(new BorderLayout(5, 5));
    applicationCountPanel.add(initializeApplicationCountButtonPanel(), BorderLayout.WEST);
    applicationCountPanel.add(applicationCountField, BorderLayout.CENTER);
    applicationCountPanel.add(spnBatchSize, BorderLayout.EAST);

    applicationPanel.add(applicationCountPanel, BorderLayout.NORTH);

    return applicationPanel;
  }

  private JPanel initializeApplicationCountButtonPanel() {
    final JPanel btnPanel = new JPanel(new GridLayout(1, 2, 0, 0));
    btnPanel.add(initializeAddRemoveApplicationButton(false));
    btnPanel.add(initializeAddRemoveApplicationButton(true));

    return btnPanel;
  }

  private JButton initializeAddRemoveApplicationButton(final boolean add) {
    final JButton btn = new JButton(new Control(add ? "+" : "-") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        try {
          if (add) {
            loadTestModel.addApplicationBatch();
          }
          else {
            loadTestModel.removeApplicationBatch();
          }
        }
        catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
    btn.setMargin(new Insets(0, 0, 0, 0));
    btn.setToolTipText(add ? "Add application batch" : "Remove application batch");

    return btn;
  }

  private JPanel initializeChartControlPanel() {
    final JPanel controlPanel = new JPanel(new FlexibleGridLayout(1, 2, 5, 5, true, false));
    controlPanel.setBorder(BorderFactory.createTitledBorder("Charts"));
    controlPanel.add(ControlProvider.createCheckBox(Controls.toggleControl(loadTestModel, "collectChartData",
            "Collect chart data", loadTestModel.collectChartDataObserver())));
    controlPanel.add(ControlProvider.createButton(Controls.methodControl(loadTestModel, "resetChartData", "Reset")));

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
    failureChartPanel.setBorder(BorderFactory.createTitledBorder("Scenario failure rate"));

    final JPanel two = new JPanel(new GridLayout(5, 1, 0, 0));

    two.add(usageScenarioChartPanel);
    two.add(failureChartPanel);
    two.add(memoryUsageChartPanel);
    two.add(thinkTimeChartPanel);
    two.add(numberOfApplicationsChartPanel);

    final JPanel chartBase = new JPanel(new BorderLayout(0, 0));

    final JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab("Scenario durations", durationBase);
    tabPane.addTab("Overview", two);

    chartBase.add(tabPane);

    return chartBase;
  }

  private JPanel initializeActivityPanel() {
    final SpinnerNumberModel maxSpinnerModel = new IntBeanSpinnerValueLink(loadTestModel, "maximumThinkTime",
            loadTestModel.maximumThinkTimeObserver()).getSpinnerModel();
    maxSpinnerModel.setStepSize(10);
    final JSpinner spnMaxThinkTime = new JSpinner(maxSpinnerModel);
    ((JSpinner.DefaultEditor) spnMaxThinkTime.getEditor()).getTextField().setColumns(3);

    final SpinnerNumberModel minSpinnerModel = new IntBeanSpinnerValueLink(loadTestModel, "minimumThinkTime",
            loadTestModel.getMinimumThinkTimeObserver()).getSpinnerModel();
    minSpinnerModel.setStepSize(10);
    final JSpinner spnMinThinkTimeField = new JSpinner(minSpinnerModel);
    ((JSpinner.DefaultEditor) spnMinThinkTimeField.getEditor()).getTextField().setColumns(3);

    final SpinnerNumberModel warningSpinnerModel = new IntBeanSpinnerValueLink(loadTestModel, "warningTime",
            loadTestModel.getWarningTimeObserver()).getSpinnerModel();
    warningSpinnerModel.setStepSize(10);
    final JSpinner spnWarningTime = new JSpinner(warningSpinnerModel);
    ((JSpinner.DefaultEditor) spnWarningTime.getEditor()).getTextField().setColumns(3);
    spnWarningTime.setToolTipText("A work request is considered 'delayed' if the time it takes to process it exceeds this value (ms)");

    final ToggleBeanValueLink pauseControl = Controls.toggleControl(loadTestModel, "paused", "Pause", loadTestModel.getPauseObserver());
    pauseControl.setMnemonic('P');

    final FlexibleGridLayout layout = new FlexibleGridLayout(4, 2, 5, 5, true, false);
    layout.setFixedRowHeight(UiUtil.getPreferredTextFieldHeight());
    final JPanel thinkTimePanel = new JPanel(layout);
    thinkTimePanel.add(new JLabel("Max. think time", JLabel.CENTER));
    thinkTimePanel.add(spnMaxThinkTime);
    thinkTimePanel.add(new JLabel("Min. think time", JLabel.CENTER));
    thinkTimePanel.add(spnMinThinkTimeField);
    thinkTimePanel.add(new JLabel("Warning time", JLabel.CENTER));
    thinkTimePanel.add(spnWarningTime);
    thinkTimePanel.add(ControlProvider.createToggleButton(pauseControl));

    thinkTimePanel.setBorder(BorderFactory.createTitledBorder("Activity"));

    return thinkTimePanel;
  }

  @SuppressWarnings({"unchecked"})
  private void handleScenarioSelected() {
    durationBase.removeAll();

    for (final Object selectedItem : randomizerPanel.getSelectedItems()) {
      final ItemRandomizer.RandomItem<LoadTest.UsageScenario> item = (ItemRandomizer.RandomItem<LoadTest.UsageScenario>) selectedItem;
      final JFreeChart scenarioDurationChart = ChartFactory.createXYStepChart(null,
              null, null, loadTestModel.getScenarioDurationDataset(item.getItem().getName()),
              PlotOrientation.VERTICAL, true, true, false);
      setColors(scenarioDurationChart);
      final ChartPanel scenarioDurationChartPanel = new ChartPanel(scenarioDurationChart);
      scenarioDurationChartPanel.setBorder(BorderFactory.createEtchedBorder());

      final DeviationRenderer renderer = new DeviationRenderer();
      renderer.setBaseShapesVisible(false);
      scenarioDurationChart.getXYPlot().setRenderer(renderer);

      durationBase.add(scenarioDurationChartPanel);
    }
    revalidate();
  }

  private void setColors(final JFreeChart chart) {
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    chart.setBackgroundPaint(this.getBackground());
    final XYSplineRenderer renderer = new XYSplineRenderer();
    renderer.setBaseShapesVisible(false);
    chart.getXYPlot().setRenderer(renderer);
  }

  private static final class ExitListener implements ActionListener {
    private final JFrame frame;

    private ExitListener(final JFrame frame) {
      this.frame = frame;
    }

    public void actionPerformed(final ActionEvent e) {
      if (frame != null) {
        frame.setVisible(false);
        frame.dispose();
      }
    }
  }
}