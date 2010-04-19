/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.Event;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.server.monitor.HostMonitor;
import org.jminor.framework.server.monitor.MonitorModel;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri
 * Date: 4.12.2007
 * Time: 18:11:06
 */
public class MonitorPanel extends JPanel {

  private final Event evtAlwaysOnTopChanged = new Event();

  private final MonitorModel model;
  private JFrame monitorFrame;

  public MonitorPanel() throws RemoteException {
    this(new MonitorModel(System.getProperty(Configuration.SERVER_HOST_NAME)));
  }

  public MonitorPanel(final MonitorModel model) throws RemoteException {
    this.model = model;
    initUI();
  }

  public MonitorModel getModel() {
    return model;
  }

  /**
   * @return true if the parent frame is always on top
   */
  public boolean getAlwaysOnTop() {
    return monitorFrame != null && monitorFrame.isAlwaysOnTop();
  }

  /**
   * @param value true if the parent frame should be always on top
   */
  public void setAlwaysOnTop(final boolean value) {
    monitorFrame.setAlwaysOnTop(value);
    evtAlwaysOnTopChanged.fire();
  }

  public void exit() {
    System.exit(0);
  }

  public void refresh() throws RemoteException {
    model.refresh();
  }

  public void showFrame() {
    monitorFrame = UiUtil.createFrame(Images.loadImage("jminor_logo_red24.png").getImage());
    monitorFrame.setJMenuBar(ControlProvider.createMenuBar(initMainMenuControlSets()));
    monitorFrame.setTitle("JMinor - EntityDb Server Monitor");
    monitorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    monitorFrame.getContentPane().add(this);
    UiUtil.resizeWindow(monitorFrame, 0.75);
    UiUtil.centerWindow(monitorFrame);
    monitorFrame.setVisible(true);
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout());
    final JTabbedPane hostPane = new JTabbedPane();
    hostPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final HostMonitor hostMonitor : model.getHostMonitors())
      hostPane.addTab(hostMonitor.getHostName(), new HostMonitorPanel(hostMonitor));
    add(hostPane, BorderLayout.CENTER);
    add(initializeSouthPanel(), BorderLayout.SOUTH);
  }

  private ControlSet initMainMenuControlSets() {
    final ControlSet controlSet = new ControlSet();
    final ControlSet file = new ControlSet("File", 'F');
    file.add(initExitControl());
    controlSet.add(file);
    final ControlSet view = new ControlSet("View", 'V');
    view.add(initRefreshControl());
    view.addSeparator();
    view.add(initAlwaysOnTopControl());
    controlSet.add(view);

    return controlSet;
  }

  private Control initRefreshControl() {
    final Control control = ControlFactory.methodControl(model, "refresh", "Refresh");
    control.setMnemonic('R');

    return control;
  }

  private Control initAlwaysOnTopControl() {
    final Control control =
            ControlFactory.toggleControl(this, "alwaysOnTop", "Always on Top", evtAlwaysOnTopChanged);
    control.setMnemonic('A');

    return control;
  }

  private Control initExitControl() {
    return ControlFactory.methodControl(this, "exit", "Exit", null, null, 'X');
  }

  private JPanel initializeSouthPanel() {
    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(UiUtil.createMemoryUsageField(2000));

    return southPanel;
  }

  public static void main(final String[] arguments) {
    try {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      new MonitorPanel().showFrame();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
