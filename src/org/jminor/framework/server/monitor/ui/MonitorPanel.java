/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.common.ui.DefaultExceptionHandler;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.server.monitor.HostMonitor;
import org.jminor.framework.server.monitor.MonitorModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * A MonitorPanel
 */
public final class MonitorPanel extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(MonitorPanel.class);

  private static final String JDK_PREFERENCE_KEY = MonitorPanel.class.getName() + ".jdkPathPreferenceKey";
  private static String jdkDir = Util.getUserPreference(JDK_PREFERENCE_KEY, null);

  private final Event evtAlwaysOnTopChanged = Events.event();
  private static final int MEMORY_USAGE_UPDATE_INTERVAL = 2000;
  private final MonitorModel model;
  private JFrame monitorFrame;

  /**
   * Instantiates a new MonitorPanel
   * @throws RemoteException in case of an exception
   */
  public MonitorPanel() throws RemoteException {
    this(new MonitorModel(Configuration.getStringValue(Configuration.SERVER_HOST_NAME)));
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      public void uncaughtException(final Thread t, final Throwable e) {
        DefaultExceptionHandler.getInstance().handleException(e, MonitorPanel.this);
      }
    });
  }

  /**
   * Instantiates a new MonitorPanel
   * @param model the MonitorModel to base this panel on
   * @throws RemoteException in case of an exception
   */
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
  public boolean isAlwaysOnTop() {
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
      setJDKDir();
      if (jdkDir == null) {
        throw new IllegalStateException("No JDK home directory has been specified");
      }
    }
    final String separator = System.getProperty("file.separator");
    new ProcessBuilder(jdkDir + separator + "bin"  + separator + "jconsole").start();
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
    monitorFrame.setTitle("JMinor Server Monitor");
    monitorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    monitorFrame.getContentPane().add(this);
    UiUtil.resizeWindow(monitorFrame, 0.75);
    UiUtil.centerWindow(monitorFrame);
    monitorFrame.setVisible(true);
  }

  public static void setJDKDir(final JComponent dialogParent) {
    try {
      jdkDir = UiUtil.selectDirectory(dialogParent, jdkDir, "Set JDK home").getAbsolutePath();
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
    final Control control = Controls.methodControl(model, "refresh", "Refresh");
    control.setMnemonic('R');

    return control;
  }

  private Control initAlwaysOnTopControl() {
    final Control control =
            Controls.toggleControl(this, "alwaysOnTop", "Always on Top", evtAlwaysOnTopChanged);
    control.setMnemonic('A');

    return control;
  }

  private Control initSetJDKDirControl() {
    final Control control =
            Controls.methodControl(this, "setJDKDir", "Set JDK home...");
    control.setMnemonic('S');

    return control;
  }

  private Control initJConsoleControl() {
    final Control control =
            Controls.methodControl(this, "runJConsole", "Run JConsole");
    control.setMnemonic('J');

    return control;
  }

  private Control initExitControl() {
    return Controls.methodControl(this, "exit", "Exit", null, null, 'X');
  }

  private JPanel initializeSouthPanel() {
    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(UiUtil.createMemoryUsageField(MEMORY_USAGE_UPDATE_INTERVAL));

    return southPanel;
  }

  public static void main(final String[] arguments) {
    Configuration.resolveTruststoreProperty(MonitorPanel.class.getSimpleName());
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          new MonitorPanel().showFrame();
        }
        catch (Exception e) {
          LOG.error(e.getMessage(), e);
          System.exit(1);
        }
      }
    });
  }
}
