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

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.tools.monitor.model.ClientUserMonitor;
import is.codion.tools.monitor.model.ClientUserMonitor.UserHistoryColumns;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.JOptionPane.showConfirmDialog;

/**
 * A ClientUserMonitorPanel
 */
public final class ClientUserMonitorPanel extends JPanel {

	private static final int SPINNER_COLUMNS = 3;
	private static final Integer[] MAINTENANCE_INTERVAL_VALUES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60, 120, 180, 340, 6000, 10000};
	private static final DateTimeFormatter LAST_SEEN_FORMATTER = LocaleDateTimePattern.builder()
					.delimiterDash().yearFourDigits().hoursMinutesSeconds()
					.build()
					.createFormatter();

	private final ClientUserMonitor model;

	private final ClientMonitorPanel clientTypeMonitorPanel;

	/**
	 * Instantiates a new ClientUserMonitorPanel
	 * @param model the ClientUserMonitor to base this panel on
	 * @throws RemoteException in case of an exception
	 */
	public ClientUserMonitorPanel(ClientUserMonitor model) throws RemoteException {
		this.model = requireNonNull(model);
		this.clientTypeMonitorPanel = new ClientMonitorPanel(model.clientMonitor());
		initializeUI();
	}

	public void disconnectAll() throws RemoteException {
		if (showConfirmDialog(this, "Are you sure you want to disconnect all clients?", "Disconnect all",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			model.disconnectAll();
		}
	}

	private void initializeUI() throws RemoteException {
		setLayout(new BorderLayout());
		add(tabbedPane()
						.tab("Current", createCurrentConnectionsPanel())
						.tab("History", createConnectionHistoryPanel())
						.build(), BorderLayout.CENTER);
	}

	private JPanel createCurrentConnectionsPanel() throws RemoteException {
		JPanel actionBase = flowLayoutPanel(FlowLayout.LEFT)
						.border(createTitledBorder("Remote connection controls"))
						.add(new JLabel("Reaper interval (s)", SwingConstants.RIGHT))
						.add(createMaintenanceIntervalComponent())
						.add(new JLabel("Idle connection timeout (s)"))
						.add(integerSpinner()
										.model(new SpinnerNumberModel())
										.link(model.idleConnectionTimeout())
										.columns(4))
						.add(button()
										.control(command(model::disconnectTimedOut))
										.text("Disconnect idle")
										.toolTipText("Disconnect those that have exceeded the allowed idle time"))
						.add(button()
										.control(command(this::disconnectAll))
										.text("Disconnect all")
										.toolTipText("Disconnect all clients"))
						.build();

		return borderLayoutPanel()
						.north(actionBase)
						.center(clientTypeMonitorPanel)
						.build();
	}

	private JPanel createConnectionHistoryPanel() {
		JPanel configPanel = flowLayoutPanel(FlowLayout.LEFT)
						.add(new JLabel("Update interval (s)"))
						.add(integerSpinner()
										.link(model.updateInterval())
										.minimum(1)
										.columns(SPINNER_COLUMNS)
										.editable(false))
						.build();

		JPanel configBase = borderLayoutPanel()
						.center(configPanel)
						.east(button()
										.control(command(model::resetHistory))
										.text("Reset"))
						.build();

		FilterTable<?, ?> userHistoryTable = FilterTable.builder()
						.model(model.userHistoryTableModel())
						.cellRenderer(UserHistoryColumns.LAST_SEEN, FilterTableCellRenderer.builder()
										.columnClass(LocalDateTime.class)
										.formatter(lastSeen -> lastSeen == null ? null : LAST_SEEN_FORMATTER.format(lastSeen))
										.build())
						.popupMenuControls(table -> Controls.builder()
										.control(Controls.builder()
														.caption("Columns")
														.control(table.createToggleColumnsControls())
														.control(table.createResetColumnsControl())
														.control(table.createSelectAutoResizeModeControl()))
										.build())
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.build();

		return borderLayoutPanel()
						.center(new JScrollPane(userHistoryTable))
						.south(configBase)
						.build();
	}

	private JComponent createMaintenanceIntervalComponent() throws RemoteException {
		return comboBox()
						.model(new DefaultComboBoxModel<>(MAINTENANCE_INTERVAL_VALUES))
						.value(model.getMaintenanceInterval())
						.itemListener(e -> {
							try {
								model.setMaintenanceInterval((Integer) e.getItem());
							}
							catch (RemoteException ex) {
								onException(ex);
							}
						}).build();
	}

	private void onException(Exception exception) {
		Dialogs.exception()
						.owner(this)
						.show(exception);
	}
}
