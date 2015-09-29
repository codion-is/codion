/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.swing.common.ui.DefaultExceptionHandler;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.framework.server.monitor.EntityServerMonitor;
import org.jminor.swing.framework.server.monitor.HostMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

/**
 * A UI based on the EntityServerMonitor model
 */
public final class EntityServerMonitorPanel extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityServerMonitorPanel.class);

  private static final String JDK_PREFERENCE_KEY = EntityServerMonitorPanel.class.getSimpleName() + ".jdkPathPreferenceKey";
  private static final double SCREEN_SIZE_RATIO = 0.75;
  private static final int MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
  private static String jdkDir = Util.getUserPreference(JDK_PREFERENCE_KEY, null);

  private final Event<Boolean> alwaysOnTopChangedEvent = Events.event();
  private final EntityServerMonitor model;
  private JFrame monitorFrame;

  /**
   * Instantiates a new EntityServerMonitorPanel
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitorPanel() throws RemoteException {
    this(new EntityServerMonitor(Configuration.getStringValue(Configuration.SERVER_HOST_NAME),
            Configuration.getIntValue(Configuration.REGISTRY_PORT)));
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final Thread t, final Throwable e) {
        DefaultExceptionHandler.getInstance().handleException(e, UiUtil.getParentWindow(EntityServerMonitorPanel.this));
      }
    });
  }

  /**
   * Instantiates a new EntityServerMonitorPanel
   * @param model the EntityServerMonitor to base this panel on
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitorPanel(final EntityServerMonitor model) throws RemoteException {
    this.model = model;
    initializeUI();
  }

  public EntityServerMonitor getModel() {
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
    alwaysOnTopChangedEvent.fire(value);
  }

  public void runJConsole() throws IOException {
    if (jdkDir == null) {
      setJDKDir();
      if (jdkDir == null) {
        throw new IllegalStateException("No JDK home directory has been specified");
      }
    }
    new ProcessBuilder(jdkDir + File.separator + "bin"  + File.separator + "jconsole").start();
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
    monitorFrame = new JFrame();
    monitorFrame.setIconImage(Images.loadImage("jminor_logo_red24.png").getImage());
    monitorFrame.setJMenuBar(ControlProvider.createMenuBar(initializeMainMenuControlSets()));
    monitorFrame.setTitle("JMinor Server Monitor");
    monitorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    monitorFrame.getContentPane().add(this);
    UiUtil.resizeWindow(monitorFrame, SCREEN_SIZE_RATIO);
    UiUtil.centerWindow(monitorFrame);
    monitorFrame.setVisible(true);
  }

  public static synchronized void setJDKDir(final JComponent dialogParent) {
    try {
      jdkDir = UiUtil.selectDirectory(dialogParent, jdkDir, "Set JDK home").getAbsolutePath();
      Util.putUserPreference(JDK_PREFERENCE_KEY, jdkDir);
    }
    catch (final CancelException ignored) {/*ignored*/}
  }

  private void initializeUI() throws RemoteException {
    setLayout(new BorderLayout());
    final JTabbedPane hostPane = new JTabbedPane();
    hostPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final HostMonitor hostMonitor : model.getHostMonitors()) {
      hostPane.addTab(hostMonitor.getHostName() + ":" + hostMonitor.getRegistryPort(), new HostMonitorPanel(hostMonitor));
    }
    add(hostPane, BorderLayout.CENTER);
    add(initializeSouthPanel(), BorderLayout.SOUTH);
  }

  private ControlSet initializeMainMenuControlSets() {
    final ControlSet controlSet = new ControlSet();
    final ControlSet file = new ControlSet("File", 'F');
    file.add(initializeExitControl());
    controlSet.add(file);
    final ControlSet view = new ControlSet("View", 'V');
    view.add(initializeRefreshControl());
    view.addSeparator();
    view.add(initializeAlwaysOnTopControl());
    controlSet.add(view);
    final ControlSet tools = new ControlSet("Tools", 'T');
    tools.add(initializeSetJDKDirControl());
    tools.add(initializeJConsoleControl());
    controlSet.add(tools);

    return controlSet;
  }

  private Control initializeRefreshControl() {
    final Control control = Controls.methodControl(model, "refresh", "Refresh");
    control.setMnemonic('R');

    return control;
  }

  private Control initializeAlwaysOnTopControl() {
    final Control control = Controls.toggleControl(this, "alwaysOnTop", "Always on Top", alwaysOnTopChangedEvent);
    control.setMnemonic('A');

    return control;
  }

  private Control initializeSetJDKDirControl() {
    final Control control =
            Controls.methodControl(this, "setJDKDir", "Set JDK home...");
    control.setMnemonic('S');

    return control;
  }

  private Control initializeJConsoleControl() {
    final Control control =
            Controls.methodControl(this, "runJConsole", "Run JConsole");
    control.setMnemonic('J');

    return control;
  }

  private Control initializeExitControl() {
    return Controls.methodControl(this, "exit", "Exit", null, null, 'X');
  }

  private JPanel initializeSouthPanel() {
    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    southPanel.setBorder(BorderFactory.createEtchedBorder());
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(UiUtil.createMemoryUsageField(MEMORY_USAGE_UPDATE_INTERVAL_MS));

    return southPanel;
  }

  public static void main(final String[] arguments) {
    Util.resolveTrustStoreFromClasspath(EntityServerMonitorPanel.class.getSimpleName());
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          new EntityServerMonitorPanel().showFrame();
        }
        catch (final Exception e) {
          LOG.error(e.getMessage(), e);
          UiUtil.showExceptionDialog(null, "Error during startup", e);
          System.exit(1);
        }
      }
    });
  }
}
