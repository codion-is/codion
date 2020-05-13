/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.UserPreferences;
import org.jminor.common.rmi.client.Clients;
import org.jminor.common.rmi.server.ServerConfiguration;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.UiManagerDefaults;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlList;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.value.ComponentValue;
import org.jminor.swing.common.ui.value.NumericalValues;
import org.jminor.swing.framework.server.monitor.EntityServerMonitor;
import org.jminor.swing.framework.server.monitor.HostMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.swing.common.ui.icons.Icons.icons;

/**
 * A UI based on the EntityServerMonitor model
 */
public final class EntityServerMonitorPanel extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityServerMonitorPanel.class);

  private static final String JDK_PREFERENCE_KEY = EntityServerMonitorPanel.class.getSimpleName() + ".jdkPathPreferenceKey";
  private static final double SCREEN_SIZE_RATIO = 0.75;
  private static final int MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
  private static String jdkDir = UserPreferences.getUserPreference(JDK_PREFERENCE_KEY, null);

  private final Event<Boolean> alwaysOnTopChangedEvent = Events.event();
  private final EntityServerMonitor model;
  private JFrame monitorFrame;

  /**
   * Instantiates a new EntityServerMonitorPanel
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitorPanel() throws RemoteException {
    this(new EntityServerMonitor(ServerConfiguration.SERVER_HOST_NAME.get(),
            ServerConfiguration.REGISTRY_PORT.get(), getAdminUser()));
    Thread.setDefaultUncaughtExceptionHandler((t, e) ->
            DefaultDialogExceptionHandler.getInstance().displayException(e, Windows.getParentWindow(EntityServerMonitorPanel.this)));
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
   * @param alwaysOnTop true if the parent frame should be always on top
   */
  public void setAlwaysOnTop(final boolean alwaysOnTop) {
    monitorFrame.setAlwaysOnTop(alwaysOnTop);
    alwaysOnTopChangedEvent.onEvent(alwaysOnTop);
  }

  public void runJConsole() throws IOException {
    if (jdkDir == null) {
      setJDKDir();
      if (jdkDir == null) {
        throw new IllegalStateException("No JDK home directory has been specified");
      }
    }
    new ProcessBuilder(jdkDir + File.separator + "bin" + File.separator + "jconsole").start();
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
    monitorFrame.setIconImage(icons().logoRed().getImage());
    monitorFrame.setJMenuBar(ControlProvider.createMenuBar(initializeMainMenuControls()));
    monitorFrame.setTitle("JMinor Server Monitor");
    monitorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    monitorFrame.getContentPane().add(this);
    Windows.resizeWindow(monitorFrame, SCREEN_SIZE_RATIO);
    Windows.centerWindow(monitorFrame);
    monitorFrame.setVisible(true);
  }

  public static synchronized void setJDKDir(final JComponent dialogParent) {
    try {
      jdkDir = Dialogs.selectDirectory(dialogParent, jdkDir, "Set JDK home").getAbsolutePath();
      UserPreferences.putUserPreference(JDK_PREFERENCE_KEY, jdkDir);
    }
    catch (final CancelException ignored) {/*ignored*/}
  }

  private void initializeUI() throws RemoteException {
    setLayout(new BorderLayout());
    final JTabbedPane hostPane = new JTabbedPane();
    for (final HostMonitor hostMonitor : model.getHostMonitors()) {
      hostPane.addTab(hostMonitor.getHostName() + ":" + hostMonitor.getRegistryPort(), new HostMonitorPanel(hostMonitor));
    }
    add(hostPane, BorderLayout.CENTER);
    add(initializeSouthPanel(), BorderLayout.SOUTH);
  }

  private ControlList initializeMainMenuControls() {
    final ControlList controls = Controls.controlList();
    final ControlList file = Controls.controlList("File", 'F');
    file.add(initializeExitControl());
    controls.add(file);
    final ControlList view = Controls.controlList("View", 'V');
    view.add(initializeRefreshControl());
    view.addSeparator();
    view.add(initializeAlwaysOnTopControl());
    controls.add(view);
    final ControlList tools = Controls.controlList("Tools", 'T');
    tools.add(Controls.control(this::setUpdateInterval, "Chart update interval"));
    tools.add(initializeSetJDKDirControl());
    tools.add(initializeJConsoleControl());
    controls.add(tools);

    return controls;
  }

  private Control initializeRefreshControl() {
    final Control control = Controls.control(model::refresh, "Refresh");
    control.setMnemonic('R');

    return control;
  }

  private Control initializeAlwaysOnTopControl() {
    final Control control = Controls.toggleControl(this, "alwaysOnTop", "Always on Top", alwaysOnTopChangedEvent);
    control.setMnemonic('A');

    return control;
  }

  private Control initializeSetJDKDirControl() {
    final Control control = Controls.control(this::setJDKDir, "Set JDK home...");
    control.setMnemonic('S');

    return control;
  }

  private Control initializeJConsoleControl() {
    final Control control = Controls.control(this::runJConsole, "Run JConsole");
    control.setMnemonic('J');

    return control;
  }

  private Control initializeExitControl() {
    return Controls.control(this::exit, "Exit", null, null, 'X');
  }

  private void setUpdateInterval() {
    final ComponentValue<Integer, IntegerField> componentValue = NumericalValues.integerValue(5);
    final IntegerField field = componentValue.getComponent();
    field.setColumns(6);
    field.setHorizontalAlignment(SwingConstants.CENTER);
    field.selectAll();
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    panel.add(field);
    final JDialog dialog = new JDialog(Windows.getParentWindow(this), "Update interval (s)");
    Dialogs.prepareOkCancelDialog(dialog, this, panel, Controls.control(() -> {
      getModel().setUpdateInterval(componentValue.get());
      dialog.dispose();
    }), Controls.control(dialog::dispose));
    dialog.setVisible(true);
  }

  private static JPanel initializeSouthPanel() {
    final JPanel southPanel = new JPanel(Layouts.flowLayout(FlowLayout.TRAILING));
    southPanel.setBorder(BorderFactory.createEtchedBorder());
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(Components.createMemoryUsageField(MEMORY_USAGE_UPDATE_INTERVAL_MS));

    return southPanel;
  }

  private static User getAdminUser() {
    final String adminUser = ServerConfiguration.SERVER_ADMIN_USER.get();
    if (nullOrEmpty(adminUser)) {
      throw new IllegalStateException("Required configuration value missing: " + ServerConfiguration.SERVER_ADMIN_USER);
    }

    return Users.parseUser(adminUser);
  }

  public static void main(final String[] arguments) {
    UiManagerDefaults.initialize();
    Clients.resolveTrustStoreFromClasspath(EntityServerMonitorPanel.class.getSimpleName());
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(Components.getDefaultLookAndFeelClassName());
        new EntityServerMonitorPanel().showFrame();
      }
      catch (final Exception e) {
        LOG.error(e.getMessage(), e);
        Dialogs.showExceptionDialog(null, "Error during startup", e);
        System.exit(1);
      }
    });
  }
}
