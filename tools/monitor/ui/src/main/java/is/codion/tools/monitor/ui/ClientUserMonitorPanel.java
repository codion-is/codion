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
import is.codion.swing.common.ui.component.table.FilterTableColumn;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.JOptionPane.showConfirmDialog;

/**
 * A ClientUserMonitorPanel
 */
public final class ClientUserMonitorPanel extends JPanel {

	private static final int SPINNER_COLUMNS = 3;
	private static final Integer[] MAINTENANCE_INTERVAL_VALUES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60, 120, 180, 340, 6000, 10000};

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
						.add(integerSpinner(new SpinnerNumberModel(), model.idleConnectionTimeout())
										.columns(4)
										.build())
						.add(button(command(model::disconnectTimedOut))
										.text("Disconnect idle")
										.toolTipText("Disconnect those that have exceeded the allowed idle time")
										.build())
						.add(button(command(this::disconnectAll))
										.text("Disconnect all")
										.toolTipText("Disconnect all clients")
										.build())
						.build();

		return borderLayoutPanel()
						.northComponent(actionBase)
						.centerComponent(clientTypeMonitorPanel)
						.build();
	}

	private JPanel createConnectionHistoryPanel() {
		JPanel configPanel = flowLayoutPanel(FlowLayout.LEFT)
						.add(new JLabel("Update interval (s)"))
						.add(integerSpinner(model.updateInterval())
										.minimum(1)
										.columns(SPINNER_COLUMNS)
										.editable(false)
										.build())
						.build();

		JPanel configBase = borderLayoutPanel()
						.centerComponent(configPanel)
						.eastComponent(button(command(model::resetHistory))
										.text("Reset")
										.build())
						.build();

		FilterTable<?, ?> userHistoryTable = FilterTable.builder()
						.model(model.userHistoryTableModel())
						.columns(createUserHistoryColumns())
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
						.centerComponent(new JScrollPane(userHistoryTable))
						.southComponent(configBase)
						.build();
	}

	private static List<FilterTableColumn<UserHistoryColumns.Id>> createUserHistoryColumns() {
		return asList(
						createColumn(UserHistoryColumns.Id.USERNAME_COLUMN, "Username"),
						createColumn(UserHistoryColumns.Id.CLIENT_TYPE_COLUMN, "Client type"),
						createColumn(UserHistoryColumns.Id.CLIENT_VERSION_COLUMN, "Client version"),
						createColumn(UserHistoryColumns.Id.FRAMEWORK_VERSION_COLUMN, "Framework version"),
						createColumn(UserHistoryColumns.Id.CLIENT_HOST_COLUMN, "Host"),
						createColumn(UserHistoryColumns.Id.LAST_SEEN_COLUMN, "Last seen", new LastSeenRenderer()),
						createColumn(UserHistoryColumns.Id.CONNECTION_COUNT_COLUMN, "Connections"));
	}

	private static FilterTableColumn<UserHistoryColumns.Id> createColumn(UserHistoryColumns.Id identifier,
																																			 String headerValue) {
		return createColumn(identifier, headerValue, null);
	}

	private static FilterTableColumn<UserHistoryColumns.Id> createColumn(UserHistoryColumns.Id identifier,
																																			 String headerValue,
																																			 TableCellRenderer cellRenderer) {
		return FilterTableColumn.builder(identifier)
						.headerValue(headerValue)
						.cellRenderer(cellRenderer)
						.build();
	}

	private JComponent createMaintenanceIntervalComponent() throws RemoteException {
		return comboBox(new DefaultComboBoxModel<>(MAINTENANCE_INTERVAL_VALUES))
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

	private static final class LastSeenRenderer extends DefaultTableCellRenderer {

		private final DateTimeFormatter formatter = LocaleDateTimePattern.builder()
						.delimiterDash().yearFourDigits().hoursMinutesSeconds()
						.build().createFormatter();

		@Override
		protected void setValue(Object value) {
			if (value instanceof Temporal) {
				super.setValue(formatter.format((Temporal) value));
			}
			else {
				super.setValue(value);
			}
		}
	}
}
