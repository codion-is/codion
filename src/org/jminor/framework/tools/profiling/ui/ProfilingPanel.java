/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.profiling.ui;

import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
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
import org.jminor.framework.tools.profiling.ProfilingModel;

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

  private final ProfilingModel profilingModel;

  /** Constructs a new ProfilingPanel.
   * @param profilingModel the profiling model
   */
  public ProfilingPanel(final ProfilingModel profilingModel) {
    if (profilingModel == null)
      throw new IllegalArgumentException("ProfilingPanel requires a ProfilingModel instance");
    this.profilingModel = profilingModel;
    initUI();
    showFrame();
  }

  public ProfilingModel getModel() {
    return profilingModel;
  }

  public void showFrame() {
    final JFrame frame = UiUtil.createFrame(Images.loadImage("jminor_logo32.gif").getImage());
    final String title = "JMinor - " + profilingModel.getClass().getSimpleName();
    getModel().evtDoneExiting.addListener(new ActionListener() {
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
    UiUtil.resizeWindow(frame, 0.6);
    UiUtil.centerWindow(frame);
    frame.setVisible(true);
  }

  protected void initUI() {
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

    final JFreeChart numberOfClientsChart = ChartFactory.createXYStepChart(null,
            null, null, getModel().getNumberOfClientsDataset(), PlotOrientation.VERTICAL, true, true, false);
    numberOfClientsChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    final ChartPanel numberOfClientsChartPanel = new ChartPanel(numberOfClientsChart);
    numberOfClientsChartPanel.setBorder(BorderFactory.createEtchedBorder());

    final ToggleBeanPropertyLink pauseControl =
            ControlFactory.toggleControl(getModel(), "pause", "Pause activity", getModel().evtPauseChanged);
    pauseControl.setMnemonic('P');
    final ToggleBeanPropertyLink relentlessControl =
            ControlFactory.toggleControl(getModel(), "relentless", "Relentless", getModel().evtRelentlessChanged);
    relentlessControl.setMnemonic('E');
    final Control addClientsControl = ControlFactory.methodControl(getModel(), "addClients", "Add client batch");
    addClientsControl.setMnemonic('A');
    final Control removeClientsControl = ControlFactory.methodControl(getModel(), "removeClients", "Remove client batch");
    removeClientsControl.setMnemonic('R');

    final IntField clientCountField = new IntField();
    clientCountField.setHorizontalAlignment(JTextField.CENTER);
    new IntBeanPropertyLink(clientCountField, getModel(), "clientCount", getModel().evtClientCountChanged, LinkType.READ_ONLY);

    final JSpinner spnMaxThinkTime = new JSpinner(new IntBeanSpinnerPropertyLink(getModel(), "maximumThinkTime",
            getModel().evtMaximumThinkTimeChanged, null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnMaxThinkTime.getEditor()).getTextField().setColumns(3);

    final JSpinner spnMinThinkTimeField = new JSpinner(new IntBeanSpinnerPropertyLink(getModel(), "minimumThinkTime",
            getModel().evtMinimumThinkTimeChanged, null).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnMinThinkTimeField.getEditor()).getTextField().setColumns(3);

    final JSpinner spnWarningTime = new JSpinner(new IntBeanSpinnerPropertyLink(getModel(), "warningTime",
            getModel().evtWarningTimeChanged, null).getSpinnerModel());
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

    final JSpinner spnBatchSize = new JSpinner(new IntBeanSpinnerPropertyLink(getModel(), "batchSize",
            getModel().evtMinimumThinkTimeChanged, null).getSpinnerModel());
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