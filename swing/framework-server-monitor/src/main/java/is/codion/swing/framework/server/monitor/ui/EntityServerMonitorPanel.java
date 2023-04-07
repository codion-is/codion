/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.TabbedPaneBuilder;
import is.codion.swing.common.ui.component.text.MemoryUsageField;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.laf.LookAndFeelSelectionPanel;
import is.codion.swing.framework.server.monitor.EntityServerMonitor;
import is.codion.swing.framework.server.monitor.HostMonitor;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;

import static is.codion.common.model.UserPreferences.setUserPreference;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.*;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.*;
import static is.codion.swing.common.ui.layout.Layouts.flowLayout;
import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * A UI based on the EntityServerMonitor model
 */
public final class EntityServerMonitorPanel extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityServerMonitorPanel.class);

  private static final String JDK_PREFERENCE_KEY = EntityServerMonitorPanel.class.getSimpleName() + ".jdkPathPreferenceKey";
  private static final double SCREEN_SIZE_RATIO = 0.75;
  private static final int MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
  private static String jdkDir = UserPreferences.getUserPreference(JDK_PREFERENCE_KEY, null);

  private final State alwaysOnTopState = State.state();
  private final EntityServerMonitor model;
  private JFrame monitorFrame;

  /**
   * Instantiates a new EntityServerMonitorPanel
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitorPanel() throws RemoteException {
    this(new EntityServerMonitor(Clients.SERVER_HOST_NAME.get(),
            ServerConfiguration.REGISTRY_PORT.get(), adminUser()));
    Thread.setDefaultUncaughtExceptionHandler((t, e) ->
            displayExceptionDialog(e, Utilities.getParentWindow(EntityServerMonitorPanel.this)));
  }

  /**
   * Instantiates a new EntityServerMonitorPanel
   * @param model the EntityServerMonitor to base this panel on
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitorPanel(EntityServerMonitor model) throws RemoteException {
    this.model = model;
    initializeUI();
    bindEvents();
  }

  public EntityServerMonitor model() {
    return model;
  }

  /**
   * @return a State controlling the alwaysOnTop state of this panels parent window
   */
  public State alwaysOnTopState() {
    return alwaysOnTopState;
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

  public void refresh() throws RemoteException {
    model.refresh();
  }

  public void showFrame() {
    monitorFrame = Windows.frame(this)
            .icon(Logos.logoRed())
            .menuBar(createMainMenuControls().createMenuBar())
            .title("Codion Server Monitor")
            .defaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
            .size(Windows.screenSizeRatio(SCREEN_SIZE_RATIO))
            .centerFrame(true)
            .show();
  }

  public static synchronized void setJDKDir(JComponent dialogParent) {
    try {
      jdkDir = fileSelectionDialog()
              .owner(dialogParent)
              .startDirectory(jdkDir)
              .title("Set JDK home")
              .selectDirectory()
              .getAbsolutePath();
      setUserPreference(JDK_PREFERENCE_KEY, jdkDir);
    }
    catch (CancelException ignored) {/*ignored*/}
  }

  private void initializeUI() throws RemoteException {
    TabbedPaneBuilder tabbedPaneBuilder = tabbedPane();
    for (HostMonitor hostMonitor : model.hostMonitors()) {
      tabbedPaneBuilder.tab(hostMonitor.hostName() + ":" + hostMonitor.registryPort(), new HostMonitorPanel(hostMonitor));
    }
    setLayout(new BorderLayout());
    add(tabbedPaneBuilder.build(), BorderLayout.CENTER);
    add(createSouthPanel(), BorderLayout.SOUTH);
  }

  private Controls createMainMenuControls() {
    return Controls.builder()
            .control(Controls.builder()
                    .caption("File")
                    .mnemonic('F')
                    .control(createExitControl()))
            .control(Controls.builder()
                    .caption("View")
                    .mnemonic('V')
                    .control(createRefreshControl())
                    .control(createUpateIntervalControl())
                    .control(createClearChartsControl())
                    .separator()
                    .control(lookAndFeelSelectionDialog()
                            .owner(this)
                            .userPreferencePropertyName(EntityServerMonitorPanel.class.getName())
                            .createControl())
                    .control(createAlwaysOnTopControl()))
            .control(Controls.builder()
                    .caption("Tools")
                    .mnemonic('T')
                    .control(createSetJDKDirControl())
                    .control(createJConsoleControl()))
            .build();
  }

  private Control createRefreshControl() {
    return Control.builder(model::refresh)
            .caption("Refresh")
            .mnemonic('R')
            .build();
  }

  private Control createAlwaysOnTopControl() {
    return ToggleControl.builder(alwaysOnTopState)
            .caption("Always on Top")
            .mnemonic('A')
            .build();
  }

  private Control createUpateIntervalControl() {
    return Control.builder(this::setUpdateInterval)
            .caption("Chart update interval...")
            .build();
  }

  private Control createClearChartsControl() {
    return Control.builder(model::clearCharts)
            .caption("Clear charts")
            .build();
  }

  private Control createSetJDKDirControl() {
    return Control.builder(this::setJDKDir)
            .caption("Set JDK home...")
            .mnemonic('S')
            .build();
  }

  private Control createJConsoleControl() {
    return Control.builder(this::runJConsole)
            .caption("Run JConsole")
            .mnemonic('J')
            .build();
  }

  private void setUpdateInterval() {
    NumberField<Integer> field = integerField()
            .initialValue(5)
            .columns(6)
            .minimumValue(1d)
            .horizontalAlignment(SwingConstants.CENTER)
            .selectAllOnFocusGained(true)
            .build();

    okCancelDialog(panel(new FlowLayout(FlowLayout.CENTER))
                    .add(field)
                    .build())
            .owner(this)
            .title("Update interval (s)")
            .onOk(() -> model().setUpdateInterval(field.getNumber()))
            .show();
  }

  private void bindEvents() {
    alwaysOnTopState.addDataListener(alwaysOnTop -> {
      if (monitorFrame != null) {
        monitorFrame.setAlwaysOnTop(alwaysOnTop);
      }
    });
  }

  private static Control createExitControl() {
    return Control.builder(() -> System.exit(0))
            .caption("Exit")
            .mnemonic('X')
            .build();
  }

  private static JPanel createSouthPanel() {
    return panel(flowLayout(FlowLayout.TRAILING))
            .border(createEtchedBorder())
            .add(new JLabel("Memory usage:"))
            .add(new MemoryUsageField(MEMORY_USAGE_UPDATE_INTERVAL_MS))
            .build();
  }

  private static User adminUser() {
    return User.parse(ServerConfiguration.SERVER_ADMIN_USER.getOrThrow());
  }

  public static void main(String[] arguments) {
    UiManagerDefaults.initialize();
    Clients.resolveTrustStore();
    LookAndFeelSelectionPanel.CHANGE_ON_SELECTION.set(true);
    Arrays.stream(FlatAllIJThemes.INFOS).forEach(themeInfo ->
            addLookAndFeelProvider(lookAndFeelProvider(themeInfo.getClassName())));
    SwingUtilities.invokeLater(() -> {
      try {
        findLookAndFeelProvider(defaultLookAndFeelName(EntityServerMonitorPanel.class.getName()))
                .ifPresent(LookAndFeelProvider::enable);
        new EntityServerMonitorPanel().showFrame();
      }
      catch (Exception exception) {
        LOG.error(exception.getMessage(), exception);
        exceptionDialog()
                .title("Error during startup")
                .show(exception);
        System.exit(1);
      }
    });
  }
}
