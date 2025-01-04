/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.tools.monitor.model.EntityServerMonitor;
import is.codion.tools.monitor.model.HostMonitor;

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
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import static is.codion.common.model.UserPreferences.setUserPreference;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.*;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.defaultLookAndFeelName;
import static is.codion.swing.common.ui.laf.LookAndFeelProviders.findLookAndFeelProvider;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * A UI based on the EntityServerMonitor model
 */
public final class EntityServerMonitorPanel extends JPanel {

	private static final Logger LOG = LoggerFactory.getLogger(EntityServerMonitorPanel.class);

	private static final String JDK_PREFERENCE_KEY = EntityServerMonitorPanel.class.getSimpleName() + ".jdkPathPreferenceKey";
	private static final double SCREEN_SIZE_RATIO = 0.75;
	private static final int MEMORY_USAGE_UPDATE_INTERVAL_MS = 2000;
	private static final NumberFormat MEMORY_USAGE_FORMAT = NumberFormat.getIntegerInstance();
	private static final Runtime RUNTIME = Runtime.getRuntime();
	private static String jdkDir = UserPreferences.getUserPreference(JDK_PREFERENCE_KEY);

	private final State alwaysOnTopState = State.state();
	private final EntityServerMonitor model;
	private JFrame monitorFrame;

	/**
	 * Instantiates a new EntityServerMonitorPanel
	 * @throws RemoteException in case of an exception
	 */
	public EntityServerMonitorPanel() throws RemoteException {
		this(new EntityServerMonitor(Clients.SERVER_HOSTNAME.get(),
						ServerConfiguration.REGISTRY_PORT.getOrThrow(), adminUser()));
		Thread.setDefaultUncaughtExceptionHandler((t, e) ->
						displayExceptionDialog(e, Utilities.parentWindow(EntityServerMonitorPanel.this)));
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
						.menuBar(menu(createMainMenuControls()).buildMenuBar())
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
		int gap = Layouts.GAP.getOrThrow();
		setBorder(createEmptyBorder(gap, gap, 0, gap));
		add(tabbedPaneBuilder.build(), BorderLayout.CENTER);
		add(createSouthPanel(), BorderLayout.SOUTH);
	}

	private Controls createMainMenuControls() {
		return Controls.builder()
						.control(Controls.builder()
										.name("File")
										.mnemonic('F')
										.control(createExitControl()))
						.control(Controls.builder()
										.name("View")
										.mnemonic('V')
										.control(createRefreshControl())
										.control(createUpateIntervalControl())
										.control(createClearChartsControl())
										.separator()
										.control(lookAndFeelSelectionDialog()
														.owner(this)
														.createControl(EntityServerMonitorPanel::lookAndFeelSelected))
										.control(createAlwaysOnTopControl()))
						.control(Controls.builder()
										.name("Tools")
										.mnemonic('T')
										.control(createSetJDKDirControl())
										.control(createJConsoleControl()))
						.build();
	}

	private Control createRefreshControl() {
		return Control.builder()
						.command(this::refresh)
						.name("Refresh")
						.mnemonic('R')
						.build();
	}

	private Control createAlwaysOnTopControl() {
		return Control.builder()
						.toggle(alwaysOnTopState)
						.name("Always on Top")
						.mnemonic('A')
						.build();
	}

	private Control createUpateIntervalControl() {
		return Control.builder()
						.command(this::setUpdateInterval)
						.name("Chart update interval...")
						.build();
	}

	private Control createClearChartsControl() {
		return Control.builder()
						.command(model::clearCharts)
						.name("Clear charts")
						.build();
	}

	private Control createSetJDKDirControl() {
		return Control.builder()
						.command(this::setJDKDir)
						.name("Set JDK home...")
						.mnemonic('S')
						.build();
	}

	private Control createJConsoleControl() {
		return Control.builder()
						.command(this::runJConsole)
						.name("Run JConsole")
						.mnemonic('J')
						.build();
	}

	private void setUpdateInterval() {
		NumberField<Integer> field = integerField()
						.value(5)
						.columns(6)
						.minimumValue(1d)
						.horizontalAlignment(SwingConstants.CENTER)
						.selectAllOnFocusGained(true)
						.build();

		okCancelDialog(flowLayoutPanel(FlowLayout.CENTER)
						.add(field)
						.build())
						.owner(this)
						.title("Update interval (s)")
						.onOk(() -> model().setUpdateInterval(field.get()))
						.show();
	}

	private void bindEvents() {
		alwaysOnTopState.addConsumer(alwaysOnTop -> {
			if (monitorFrame != null) {
				monitorFrame.setAlwaysOnTop(alwaysOnTop);
			}
		});
	}

	private static Control createExitControl() {
		return Control.builder()
						.command(() -> System.exit(0))
						.name("Exit")
						.mnemonic('X')
						.build();
	}

	private static JPanel createSouthPanel() {
		return flowLayoutPanel(FlowLayout.TRAILING)
						.border(createEtchedBorder())
						.add(new JLabel("Memory usage:"))
						.add(Components.stringField()
										.columns(8)
										.editable(false)
										.horizontalAlignment(SwingConstants.CENTER)
										.onBuild(memoryUsageField -> TaskScheduler.builder(() -> SwingUtilities.invokeLater(() ->
																		memoryUsageField.setText(memoryUsage())))
														.interval(MEMORY_USAGE_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
														.start())
										.build())
						.build();
	}

	private static String memoryUsage() {
		return MEMORY_USAGE_FORMAT.format((RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 1024) + " KB";
	}

	private static User adminUser() {
		return User.parse(ServerConfiguration.ADMIN_USER.getOrThrow());
	}

	private static void lookAndFeelSelected(LookAndFeelProvider selectedLookAndFeel) {
		setUserPreference(EntityServerMonitorPanel.class.getName(),
						selectedLookAndFeel.lookAndFeelInfo().getClassName());
	}

	public static void main(String[] arguments) {
		UiManagerDefaults.initialize();
		Clients.resolveTrustStore();
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
