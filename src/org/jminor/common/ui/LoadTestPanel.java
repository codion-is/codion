/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.LoadTestModel;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.IntBeanPropertyLink;
import org.jminor.common.ui.control.IntBeanSpinnerPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.common.ui.textfield.IntField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A default UI component for the LoadTestModel class.
 * @see org.jminor.common.model.LoadTestModel
 */
public class LoadTestPanel extends JPanel {

  private final LoadTestModel loadTestModel;

  /**
   * Constructs a new LoadTestPanel.
   * @param loadTestModel the LoadTestModel to base this panel on
   */
  public LoadTestPanel(final LoadTestModel loadTestModel) {
    if (loadTestModel == null)
      throw new IllegalArgumentException("LoadTestPanel requires a LoadTestModel instance");
    this.loadTestModel = loadTestModel;
    initializeUI();
  }

  public LoadTestModel getModel() {
    return loadTestModel;
  }

  public void showFrame() {
    final JFrame frame = UiUtil.createFrame(Images.loadImage("jminor_logo32.gif").getImage());
    final String title = "JMinor - " + loadTestModel.getClass().getSimpleName();
    getModel().eventDoneExiting().addListener(new ActionListener() {
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
        getModel().exit();
      }
    });
    frame.setTitle(title);
    frame.getContentPane().add(this);
    UiUtil.resizeWindow(frame, 0.75);
    UiUtil.centerWindow(frame);
    frame.setVisible(true);
  }

  protected void initializeUI() {
    final JPanel chartBase = initializeChartPanel();
    final JPanel activityPanel = initializeActivityPanel();
    final JPanel applicationPanel = initializeApplicationPanel();
    final JPanel userBase = initializeUserPanel();
    final JPanel scenarioBase = initializeScenarioPanel();

    final JPanel controlBase = new JPanel(new FlexibleGridLayout(4, 1, 5, 5, false, true));
    controlBase.add(applicationPanel);
    controlBase.add(activityPanel);
    controlBase.add(scenarioBase);
    controlBase.add(userBase);

    setLayout(new BorderLayout());
    add(controlBase, BorderLayout.WEST);
    add(chartBase, BorderLayout.CENTER);
  }

  private JPanel initializeScenarioPanel() {
    final JPanel scenarioBase = new JPanel(new BorderLayout(5, 5));
    scenarioBase.add(new RandomItemPanel(getModel().getRandomModel()), BorderLayout.NORTH);
    scenarioBase.setBorder(BorderFactory.createTitledBorder("Scenarios"));

    return scenarioBase;
  }

  private JPanel initializeUserPanel() {
    final LoginPanel userPanel = new LoginPanel(getModel().getUser(), true, "Username", "Password");
    final ActionListener userInfoListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        getModel().setUser(userPanel.getUser());
      }
    };
    userPanel.getPasswordField().addActionListener(userInfoListener);
    userPanel.getUsernameField().addActionListener(userInfoListener);
    final JPanel userBase = new JPanel(new BorderLayout(5,5));
    userBase.setBorder(BorderFactory.createTitledBorder("User"));
    userBase.add(userPanel, BorderLayout.NORTH);

    return userBase;
  }

  private JPanel initializeApplicationPanel() {
    final JSpinner spnBatchSize = new JSpinner(new IntBeanSpinnerPropertyLink(getModel(), "applicationBatchSize",
            getModel().eventApplicationBatchSizeChanged(), null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnBatchSize.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnBatchSize.getEditor()).getTextField().setColumns(3);

    final IntField applicationCountField = new IntField();
    applicationCountField.setHorizontalAlignment(JTextField.CENTER);
    new IntBeanPropertyLink(applicationCountField, getModel(), "applicationCount", getModel().eventApplicationCountChanged(), LinkType.READ_ONLY);

    final Control addApplicationsControl = ControlFactory.methodControl(getModel(), "addApplications", "Add");
    addApplicationsControl.setMnemonic('A');
    final Control removeApplicationsControl = ControlFactory.methodControl(getModel(), "removeApplications", "Remove");
    removeApplicationsControl.setMnemonic('R');

    final JPanel applicationPanel = new JPanel(new BorderLayout(5, 5));
    applicationPanel.setBorder(BorderFactory.createTitledBorder("Applications"));

    final JPanel applicationBatchPanel = new JPanel(new GridLayout(2, 2, 5, 5));
    applicationBatchPanel.add(new JLabel("Application count", JLabel.CENTER));
    applicationBatchPanel.add(applicationCountField);
    applicationBatchPanel.add(new JLabel("Application batch size", JLabel.CENTER));
    applicationBatchPanel.add(spnBatchSize);

    final JPanel addRemovePanel = new JPanel(new GridLayout(1, 2, 5, 5));
    addRemovePanel.add(ControlProvider.createButton(addApplicationsControl));
    addRemovePanel.add(ControlProvider.createButton(removeApplicationsControl));

    applicationPanel.add(applicationBatchPanel, BorderLayout.NORTH);
    applicationPanel.add(addRemovePanel, BorderLayout.SOUTH);

    return applicationPanel;
  }

  private JPanel initializeChartPanel() {
    final JFreeChart workRequestsChart = ChartFactory.createXYStepChart(null,
            null, null, getModel().getWorkRequestsDataset(), PlotOrientation.VERTICAL, true, true, false);
    workRequestsChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel workRequestsChartPanel = new ChartPanel(workRequestsChart);
    workRequestsChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart thinkTimeChart = ChartFactory.createXYStepChart(null,
            null, null, getModel().getThinkTimeDataset(), PlotOrientation.VERTICAL, true, true, false);
    thinkTimeChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel thinkTimeChartPanel = new ChartPanel(thinkTimeChart);
    thinkTimeChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart numberOfApplicationsChart = ChartFactory.createXYStepChart(null,
            null, null, getModel().getNumberOfApplicationsDataset(), PlotOrientation.VERTICAL, true, true, false);
    numberOfApplicationsChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel numberOfApplicationsChartPanel = new ChartPanel(numberOfApplicationsChart);
    numberOfApplicationsChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JFreeChart usageScenarioChart = ChartFactory.createXYStepChart(null,
            null, null, getModel().getUsageScenarioDataset(), PlotOrientation.VERTICAL, true, true, false);
    usageScenarioChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel usageScenarioChartPanel = new ChartPanel(usageScenarioChart);
    usageScenarioChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final JPanel chartBase = new JPanel(new GridLayout(4, 1, 5, 5));
    chartBase.setBorder(BorderFactory.createTitledBorder("Status"));
    chartBase.add(workRequestsChartPanel);
    chartBase.add(usageScenarioChartPanel);
    chartBase.add(numberOfApplicationsChartPanel);
    chartBase.add(thinkTimeChartPanel);

    return chartBase;
  }

  private JPanel initializeActivityPanel() {
    final JSpinner spnMaxThinkTime = new JSpinner(new IntBeanSpinnerPropertyLink(getModel(), "maximumThinkTime",
            getModel().eventMaximumThinkTimeChanged(), null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnMaxThinkTime.getEditor()).getTextField().setColumns(3);

    final JSpinner spnMinThinkTimeField = new JSpinner(new IntBeanSpinnerPropertyLink(getModel(), "minimumThinkTime",
            getModel().eventMinimumThinkTimeChanged(), null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnMinThinkTimeField.getEditor()).getTextField().setColumns(3);

    final JSpinner spnWarningTime = new JSpinner(new IntBeanSpinnerPropertyLink(getModel(), "warningTime",
            getModel().eventWarningTimeChanged(), null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnWarningTime.getEditor()).getTextField().setColumns(3);
    spnWarningTime.setToolTipText("A work request is considered 'delayed' if the time it takes to process it exceeds this value (ms)");

    final ToggleBeanPropertyLink pauseControl = ControlFactory.toggleControl(getModel(), "paused", "Pause", getModel().eventPausedChanged());
    pauseControl.setMnemonic('P');

    final JPanel activityPanel = new JPanel(new BorderLayout(5, 5));
    activityPanel.setBorder(BorderFactory.createTitledBorder("Activity"));

    final JPanel thinkTimePanel = new JPanel(new GridLayout(3, 4, 5, 5));
    thinkTimePanel.add(new JLabel("Max. think time", JLabel.CENTER));
    thinkTimePanel.add(spnMaxThinkTime);
    thinkTimePanel.add(new JLabel("Min. think time", JLabel.CENTER));
    thinkTimePanel.add(spnMinThinkTimeField);
    thinkTimePanel.add(new JLabel("Warning time", JLabel.CENTER));
    thinkTimePanel.add(spnWarningTime);

    final JPanel pausePanel = new JPanel(new GridLayout(1, 2, 5, 5));
    pausePanel.add(new JLabel());
    pausePanel.add(ControlProvider.createToggleButton(pauseControl));

    activityPanel.add(thinkTimePanel, BorderLayout.NORTH);
    activityPanel.add(pausePanel, BorderLayout.SOUTH);

    return activityPanel;
  }
}