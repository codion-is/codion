/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.framework.server.monitor.EntityServerMonitor;
import is.codion.swing.framework.server.monitor.HostMonitor;

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

import static is.codion.swing.common.ui.icons.Icons.icons;

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
    bindEvents();
  }

  public EntityServerMonitor getModel() {
    return model;
  }

  /**
   * @return a State controlling the alwaysOnTop state of this panels parent window
   */
  public State getAlwaysOnTopState() {
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
    monitorFrame = new JFrame();
    monitorFrame.setIconImage(icons().logoRed().getImage());
    monitorFrame.setJMenuBar(initializeMainMenuControls().createMenuBar());
    monitorFrame.setTitle("Codion Server Monitor");
    monitorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    monitorFrame.getContentPane().add(this);
    monitorFrame.setAlwaysOnTop(alwaysOnTopState.get());
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

  private Controls initializeMainMenuControls() {
    return Controls.builder()
            .control(Controls.builder()
                    .name("File")
                    .mnemonic('F')
                    .control(initializeExitControl()).build())
            .control(Controls.builder()
                    .name("View")
                    .mnemonic('V')
                    .control(initializeRefreshControl())
                    .control(initializeUpateIntervalControl())
                    .separator()
                    .control(initializeAlwaysOnTopControl()).build())
            .control(Controls.builder()
                    .name("Tools")
                    .mnemonic('T')
                    .control(initializeSetJDKDirControl())
                    .control(initializeJConsoleControl()).build())
            .build();
  }

  private Control initializeRefreshControl() {
    return Control.builder()
            .command(model::refresh)
            .name("Refresh")
            .mnemonic('R')
            .build();
  }

  private Control initializeAlwaysOnTopControl() {
    return ToggleControl.builder()
            .state(alwaysOnTopState)
            .name("Always on Top")
            .mnemonic('A')
            .build();
  }

  private Control initializeUpateIntervalControl() {
    return Control.builder()
            .command(this::setUpdateInterval)
            .name("Chart update interval...")
            .build();
  }

  private Control initializeSetJDKDirControl() {
    return Control.builder()
            .command(this::setJDKDir)
            .name("Set JDK home...")
            .mnemonic('S')
            .build();
  }

  private Control initializeJConsoleControl() {
    return Control.builder()
            .command(this::runJConsole)
            .name("Run JConsole")
            .mnemonic('J')
            .build();
  }

  private Control initializeExitControl() {
    return Control.builder()
            .command(() -> System.exit(0))
            .name("Exit")
            .mnemonic('X')
            .build();
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
    Dialogs.prepareOkCancelDialog(dialog, panel, Control.control(() -> {
      getModel().setUpdateInterval(componentValue.get());
      dialog.dispose();
    }), Control.control(dialog::dispose));
    dialog.setVisible(true);
  }

  private void bindEvents() {
    alwaysOnTopState.addDataListener(alwaysOnTop -> {
      if (monitorFrame != null) {
        monitorFrame.setAlwaysOnTop(alwaysOnTop);
      }
    });
  }

  private static JPanel initializeSouthPanel() {
    final JPanel southPanel = new JPanel(Layouts.flowLayout(FlowLayout.TRAILING));
    southPanel.setBorder(BorderFactory.createEtchedBorder());
    southPanel.add(new JLabel("Memory usage:"));
    southPanel.add(Components.createMemoryUsageField(MEMORY_USAGE_UPDATE_INTERVAL_MS));

    return southPanel;
  }

  private static User getAdminUser() {
    return User.parseUser(ServerConfiguration.SERVER_ADMIN_USER.getOrThrow());
  }

  public static void main(final String[] arguments) {
    UiManagerDefaults.initialize();
    Clients.resolveTrustStoreFromClasspath(EntityServerMonitorPanel.class.getSimpleName());
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(Components.getSystemLookAndFeelClassName());
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
