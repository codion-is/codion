/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.server.monitor.HostMonitor;
import org.jminor.framework.server.monitor.MonitorModel;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri<br>
 * Date: 4.12.2007<br>
 * Time: 18:11:06<br>
 */
public class MonitorPanel extends JPanel {

  private static final String JDK_PREFERENCE_KEY = MonitorPanel.class.getName() + ".jdkPathPreferenceKey";
  private static String jdkDir;

  private final Event evtAlwaysOnTopChanged = new Event();
  private static final int MEMORY_USAGE_UPDATE_INTERVAL = 2000;
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

  public void runJConsole() throws IOException {
    if (jdkDir == null) {
      throw new RuntimeException("No JDK home directory has been set");
    }
    final String separator = System.getProperty("file.separator");
    ProcessBuilder builder = new ProcessBuilder(jdkDir + separator + "bin"  + separator + "jconsole");
    builder.start();
  }

  public void setJDKDir() {
    setJDKDir(this);
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

  public static void setJDKDir(final JComponent dialogParent) {
    try {
      jdkDir = UiUtil.selectDirectory(dialogParent, jdkDir).getAbsolutePath();
      Util.putUserPreference(JDK_PREFERENCE_KEY, jdkDir);
    }
    catch (CancelException e) {/**/}
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout());
    final JTabbedPane hostPane = new JTabbedPane();
    hostPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final HostMonitor hostMonitor : model.getHostMonitors()) {
      hostPane.addTab(hostMonitor.getHostName(), new HostMonitorPanel(hostMonitor));
    }
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
    final ControlSet tools = new ControlSet("Tools", 'T');
    tools.add(initSetJDKDirControl());
    tools.add(initJConsoleControl());
    controlSet.add(tools);

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

  private Control initSetJDKDirControl() {
    final Control control =
            ControlFactory.methodControl(this, "setJDKDir", "Set JDK path...");
    control.setMnemonic('S');

    return control;
  }

  private Control initJConsoleControl() {
    final Control control =
            ControlFactory.methodControl(this, "runJConsole", "Run JConsole");
    control.setMnemonic('J');

    return control;
  }

  private Control initExitControl() {
    return ControlFactory.methodControl(this, "exit", "Exit", null, null, 'X');
  }

  private JPanel initializeSouthPanel() {
    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(UiUtil.createMemoryUsageField(MEMORY_USAGE_UPDATE_INTERVAL));

    return southPanel;
  }

  public static void main(final String[] arguments) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          new MonitorPanel().showFrame();
        }
        catch (Exception e) {
          System.exit(1);
        }
      }
    });
  }
}
