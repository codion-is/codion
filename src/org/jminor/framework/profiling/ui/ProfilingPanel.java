/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.profiling.ui;

import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.IntBeanPropertyLink;
import org.jminor.common.ui.control.IntBeanSpinnerPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.profiling.ProfilingModel;

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

public class ProfilingPanel extends JPanel {

  private final ProfilingModel model;
  private final LoginPanel userPanel;

  private final JFreeChart workRequestsChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel workRequestsChartPanel = new ChartPanel(workRequestsChart);

  private final JFreeChart thinkTimeChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel thinkTimeChartPanel = new ChartPanel(thinkTimeChart);

  private final JFreeChart numberOfClientsChart = ChartFactory.createXYStepChart(null,
        null, null, null, PlotOrientation.VERTICAL, true, true, false);
  private final ChartPanel numberOfClientsChartPanel = new ChartPanel(numberOfClientsChart);

  JFrame frame;

  /** Constructs a new ProfilingPanel.
   * @param profilingModel the profiling model
   */
  public ProfilingPanel(final ProfilingModel profilingModel) {
    this.model = profilingModel;
    this.userPanel = new LoginPanel(model.getUser(), true, "Username", "Password");
    this.workRequestsChart.getXYPlot().setDataset(model.getWorkRequestsDataset());
    this.workRequestsChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    this.thinkTimeChart.getXYPlot().setDataset(model.getThinkTimeDataset());
    this.thinkTimeChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    this.numberOfClientsChart.getXYPlot().setDataset(model.getNumberOfClientsDataset());
    this.numberOfClientsChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    initUI();
    this.model.evtDoneExiting.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (frame != null) {
          frame.setVisible(false);
          frame.dispose();
        }
      }
    });
    showFrame();
  }

  public void showFrame() {
    final String title = "JMinor - " + model.getClass().getSimpleName();
    frame = UiUtil.createFrame(Images.loadImage("jminor_logo32.gif").getImage());
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        frame.setTitle(title + " - Closing...");
        model.exit();
      }
    });
    frame.setTitle(title);
    frame.getContentPane().add(this);
    UiUtil.resizeWindow(frame, 0.6);
    UiUtil.centerWindow(frame);
    frame.setVisible(true);
  }

  protected void initUI() {
    final ToggleBeanPropertyLink pauseControl = ControlFactory.toggleControl(model, "pause", "Pause activity", model.evtPauseChanged);
    pauseControl.setMnemonic('P');
    final ToggleBeanPropertyLink relentlessControl = ControlFactory.toggleControl(model, "relentless", "Relentless", model.evtRelentlessChanged);
    relentlessControl.setMnemonic('E');
    final Control addClientsControl = ControlFactory.methodControl(model, "addClients", "Add client batch");
    addClientsControl.setMnemonic('A');
    final Control removeClientsControl = ControlFactory.methodControl(model, "removeClients", "Remove client batch");
    removeClientsControl.setMnemonic('R');

    final IntField clientCountField = new IntField();
    clientCountField.setHorizontalAlignment(JTextField.CENTER);
    new IntBeanPropertyLink(clientCountField, model, "clientCount", model.evtClientCountChanged, null,
            LinkType.READ_ONLY, null);

    final JSpinner spnMaxThinkTime = new JSpinner(new IntBeanSpinnerPropertyLink(model, "maximumThinkTime",
            model.evtMaximumThinkTimeChanged, null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnMaxThinkTime.getEditor()).getTextField().setColumns(3);

    final JSpinner spnMinThinkTimeField = new JSpinner(new IntBeanSpinnerPropertyLink(model, "minimumThinkTime",
            model.evtMinimumThinkTimeChanged, null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnMinThinkTimeField.getEditor()).getTextField().setColumns(3);

    final JSpinner spnWarningTime = new JSpinner(new IntBeanSpinnerPropertyLink(model, "warningTime",
            model.evtWarningTimeChanged, null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnWarningTime.getEditor()).getTextField().setColumns(3);

    FlexibleGridLayout layout = new FlexibleGridLayout(8,1,5,5,true,false);
    layout.setFixedRowHeight(UiUtil.getPreferredTextFieldHeight());
    final JPanel activityPanel = new JPanel(layout);
    activityPanel.setBorder(BorderFactory.createTitledBorder("Activity"));
    activityPanel.add(new JLabel("Max. think time (ms)", JLabel.CENTER));
    activityPanel.add(spnMaxThinkTime);
    activityPanel.add(new JLabel("Min. think time (ms)", JLabel.CENTER));
    activityPanel.add(spnMinThinkTimeField);
    activityPanel.add(new JLabel("Warning time (ms)", JLabel.CENTER));
    activityPanel.add(spnWarningTime);
    activityPanel.add(ControlProvider.createToggleButton(pauseControl));
    activityPanel.add(ControlProvider.createToggleButton(relentlessControl));

    final JSpinner spnBatchSize =
            new JSpinner(new IntBeanSpinnerPropertyLink(model, "batchSize", model.evtMinimumThinkTimeChanged, null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnBatchSize.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnBatchSize.getEditor()).getTextField().setColumns(3);

    layout = new FlexibleGridLayout(6,1,5,5,true,false);
    layout.setFixedRowHeight(UiUtil.getPreferredTextFieldHeight());
    final JPanel clientPanel = new JPanel(layout);
    clientPanel.setBorder(BorderFactory.createTitledBorder("Clients"));
    clientPanel.add(new JLabel("Client count", JLabel.CENTER));
    clientPanel.add(clientCountField);
    clientPanel.add(new JLabel("Client batch size", JLabel.CENTER));
    clientPanel.add(spnBatchSize);
    clientPanel.add(ControlProvider.createButton(addClientsControl));
    clientPanel.add(ControlProvider.createButton(removeClientsControl));

    final ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        model.setUser(userPanel.getUser());
      }
    };
    userPanel.txtPassword.addActionListener(listener);
    userPanel.txtUsername.addActionListener(listener);
    final JPanel userBase = new JPanel(new BorderLayout(5,5));
    userBase.setBorder(BorderFactory.createTitledBorder("User"));
    userBase.add(userPanel, BorderLayout.NORTH);

    final JPanel chartBase = new JPanel(new GridLayout(3,1,5,5));
    chartBase.setBorder(BorderFactory.createTitledBorder("Status"));
    chartBase.add(workRequestsChartPanel);
    chartBase.add(numberOfClientsChartPanel);
    chartBase.add(thinkTimeChartPanel);

    final JPanel controlBase = new JPanel(new FlexibleGridLayout(3,1,5,5,false,true));
    controlBase.add(clientPanel);
    controlBase.add(activityPanel);
    controlBase.add(userBase);

    setLayout(new BorderLayout());
    add(controlBase, BorderLayout.WEST);
    add(chartBase, BorderLayout.CENTER);
  }
}
