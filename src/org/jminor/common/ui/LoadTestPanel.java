/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.LoadTestModel;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.IntBeanSpinnerValueLink;
import org.jminor.common.ui.control.IntBeanValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.MethodControl;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.common.ui.textfield.IntField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
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
public class LoadTestPanel extends JPanel {

  private static final int MEMORY_USAGE_UPDATE_INTERVAL = 2000;
  private final LoadTestModel loadTestModel;

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   */
  public LoadTestPanel(final LoadTestModel loadTestModel) {
    Util.rejectNullValue(loadTestModel);
    this.loadTestModel = loadTestModel;
    initializeUI();
  }

  public LoadTestModel getModel() {
    return loadTestModel;
  }

  public JFrame showFrame() {
    final JFrame frame = UiUtil.createFrame(Images.loadImage("jminor_logo32.gif").getImage());
    final String title = "JMinor - " + loadTestModel.getClass().getSimpleName();
    loadTestModel.eventDoneExiting().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (frame != null) {
          frame.setVisible(false);
          frame.dispose();
        }
      }
    });
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        frame.setTitle(title + " - Closing...");
        loadTestModel.exit();
      }
    });
    frame.setTitle(title);
    frame.getContentPane().add(this);
    UiUtil.resizeWindow(frame, 0.75);
    UiUtil.centerWindow(frame);
    frame.setVisible(true);

    return frame;
  }

  protected void initializeUI() {
    final JPanel chartBase = initializeChartPanel();
    final JPanel activityPanel = initializeActivityPanel();
    final JPanel applicationPanel = initializeApplicationPanel();
    final JPanel userBase = initializeUserPanel();
    final JPanel scenarioBase = initializeScenarioPanel();
    final JPanel chartControlPanel = initializeChartControlPanel();

    final JPanel controlPanel = new JPanel(new FlexibleGridLayout(5, 1, 5, 5, false, true));
    controlPanel.add(applicationPanel);
    controlPanel.add(activityPanel);
    controlPanel.add(scenarioBase);
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
    southPanel.add(UiUtil.createMemoryUsageField(MEMORY_USAGE_UPDATE_INTERVAL));

    return southPanel;
  }

  private JPanel initializeScenarioPanel() {
    final RandomItemPanel scenarioBase = new RandomItemPanel<LoadTestModel.UsageScenario>(loadTestModel.getScenarioChooser());
    scenarioBase.setBorder(BorderFactory.createTitledBorder("Usage scenarios"));

    return scenarioBase;
  }

  private JPanel initializeUserPanel() {
    final User user = loadTestModel.getUser();
    final JTextField txtUsername = new JTextField(user.getUsername());
    txtUsername.setColumns(8);
    final JTextField txtPassword = new JPasswordField(user.getPassword());
    txtPassword.setColumns(8);
    final ActionListener userInfoListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
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
    new IntBeanValueLink(applicationCountField, getModel(), "applicationCount", loadTestModel.eventApplicationCountChanged(), LinkType.READ_ONLY) {
      @Override
      protected synchronized void setUIValue(final Object value) {
        super.setUIValue(value);
      }
    };

    final JPanel applicationPanel = new JPanel(new BorderLayout(5, 5));
    applicationPanel.setBorder(BorderFactory.createTitledBorder("Applications"));

    final JSpinner spnBatchSize = new JSpinner(new IntBeanSpinnerValueLink(loadTestModel, "applicationBatchSize",
            loadTestModel.eventApplicationBatchSizeChanged()).getSpinnerModel());
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
            loadTestModel.addApplications();
          }
          else {
            loadTestModel.removeApplications();
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
    controlPanel.add(ControlProvider.createCheckBox(ControlFactory.toggleControl(loadTestModel, "collectChartData",
            "Collect chart data", loadTestModel.eventCollectChartDataChanged())));
    controlPanel.add(ControlProvider.createButton(ControlFactory.methodControl(loadTestModel, "resetChartData", "Reset")));

    return controlPanel;
  }

  private JPanel initializeChartPanel() {
    final JFreeChart workRequestsChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getWorkRequestsDataset(), PlotOrientation.VERTICAL, true, true, false);
    workRequestsChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel workRequestsChartPanel = new ChartPanel(workRequestsChart);
    workRequestsChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart thinkTimeChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getThinkTimeDataset(), PlotOrientation.VERTICAL, true, true, false);
    thinkTimeChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel thinkTimeChartPanel = new ChartPanel(thinkTimeChart);
    thinkTimeChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart numberOfApplicationsChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getNumberOfApplicationsDataset(), PlotOrientation.VERTICAL, true, true, false);
    numberOfApplicationsChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel numberOfApplicationsChartPanel = new ChartPanel(numberOfApplicationsChart);
    numberOfApplicationsChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart usageScenarioChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getUsageScenarioDataset(), PlotOrientation.VERTICAL, true, true, false);
    usageScenarioChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel usageScenarioChartPanel = new ChartPanel(usageScenarioChart);
    usageScenarioChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart memoryUsageChart = ChartFactory.createXYStepChart(null,
            null, null, loadTestModel.getMemoryUsageDataset(), PlotOrientation.VERTICAL, true, true, false);
    memoryUsageChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel memoryUsageChartPanel = new ChartPanel(memoryUsageChart);
    memoryUsageChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JPanel chartBase = new JPanel(new GridLayout(5, 1, 0, 0));
    chartBase.setBorder(BorderFactory.createTitledBorder("Status"));
    chartBase.add(workRequestsChartPanel);
    chartBase.add(usageScenarioChartPanel);
    chartBase.add(numberOfApplicationsChartPanel);
    chartBase.add(thinkTimeChartPanel);
    chartBase.add(memoryUsageChartPanel);

    return chartBase;
  }

  private JPanel initializeActivityPanel() {
    SpinnerNumberModel spinnerModel = new IntBeanSpinnerValueLink(loadTestModel, "maximumThinkTime",
            loadTestModel.eventMaximumThinkTimeChanged()).getSpinnerModel();
    spinnerModel.setStepSize(10);
    final JSpinner spnMaxThinkTime = new JSpinner(spinnerModel);
    ((JSpinner.DefaultEditor) spnMaxThinkTime.getEditor()).getTextField().setColumns(3);

    spinnerModel = new IntBeanSpinnerValueLink(loadTestModel, "minimumThinkTime",
            loadTestModel.eventMinimumThinkTimeChanged()).getSpinnerModel();
    spinnerModel.setStepSize(10);
    final JSpinner spnMinThinkTimeField = new JSpinner(spinnerModel);
    ((JSpinner.DefaultEditor) spnMinThinkTimeField.getEditor()).getTextField().setColumns(3);

    spinnerModel = new IntBeanSpinnerValueLink(loadTestModel, "warningTime",
            loadTestModel.eventWarningTimeChanged()).getSpinnerModel();
    spinnerModel.setStepSize(10);
    final JSpinner spnWarningTime = new JSpinner(spinnerModel);
    ((JSpinner.DefaultEditor) spnWarningTime.getEditor()).getTextField().setColumns(3);
    spnWarningTime.setToolTipText("A work request is considered 'delayed' if the time it takes to process it exceeds this value (ms)");

    final ToggleBeanValueLink pauseControl = ControlFactory.toggleControl(loadTestModel, "paused", "Pause", loadTestModel.eventPausedChanged());
    pauseControl.setMnemonic('P');
    final MethodControl gcControl = ControlFactory.methodControl(loadTestModel, "performGC", "Perform GC");

    final FlexibleGridLayout layout = new FlexibleGridLayout(4, 2, 5, 5, true, false);
    layout.setFixedRowHeight(UiUtil.getPreferredTextFieldHeight());
    final JPanel thinkTimePanel = new JPanel(layout);
    thinkTimePanel.add(new JLabel("Max. think time", JLabel.CENTER));
    thinkTimePanel.add(spnMaxThinkTime);
    thinkTimePanel.add(new JLabel("Min. think time", JLabel.CENTER));
    thinkTimePanel.add(spnMinThinkTimeField);
    thinkTimePanel.add(new JLabel("Warning time", JLabel.CENTER));
    thinkTimePanel.add(spnWarningTime);

    thinkTimePanel.add(ControlProvider.createButton(gcControl));
    thinkTimePanel.add(ControlProvider.createToggleButton(pauseControl));

    thinkTimePanel.setBorder(BorderFactory.createTitledBorder("Activity"));

    return thinkTimePanel;
  }
}