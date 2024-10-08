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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor.ui;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.state.State;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.tools.monitor.model.ClientInstanceMonitor;
import is.codion.tools.monitor.model.ClientMonitor;
import is.codion.tools.monitor.model.ClientMonitor.RemoteClientColumns;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.rmi.RemoteException;
import java.util.List;

import static is.codion.swing.common.ui.Utilities.linkBoundedRangeModels;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Arrays.asList;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

/**
 * A ClientMonitorPanel
 */
public final class ClientMonitorPanel extends JPanel {

	private final ClientMonitor model;
	private final FilterTable<RemoteClient, RemoteClientColumns.Id> clientInstanceTable;
	private final JScrollPane filterScrollPane;
	private final JScrollPane clientInstanceScroller;
	private final State advancedFilterState = State.builder()
					.consumer(this::toggleAdvancedFilters)
					.build();

	/**
	 * Instantiates a new ClientMonitorPanel
	 * @param model the model
	 */
	public ClientMonitorPanel(ClientMonitor model) {
		this.model = model;
		clientInstanceTable = FilterTable.builder(model.clientInstanceTableModel(), createColumns())
						.popupMenu(this::createPopupMenu)
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.conditionState(ConditionState.SIMPLE)
						.build();
		clientInstanceScroller = scrollPane(clientInstanceTable)
						.border(BorderFactory.createTitledBorder("Clients"))
						.build();
		filterScrollPane = createLinkedScrollPane(clientInstanceScroller, clientInstanceTable.filters());
		initializeUI();
	}

	public ClientMonitor model() {
		return model;
	}

	public void refresh() {
		model.refresh();
	}

	private void initializeUI() {
		JPanel clientInstanceBase = borderLayoutPanel()
						.northComponent(filterScrollPane)
						.centerComponent(clientInstanceScroller)
						.southComponent(borderLayoutPanel()
										.southComponent(borderLayoutPanel()
														.centerComponent(clientInstanceTable.searchField())
														.eastComponent(flexibleGridLayoutPanel(1, 2)
																		.add(checkBox(advancedFilterState)
																						.text("Advanced filters")
																						.build())
																		.add(button(command(this::refresh))
																						.text("Refresh")
																						.build())
																		.build())
														.build())
										.build())
						.build();

		JPanel clientInstancePanel = borderLayoutPanel().build();
		JSplitPane splitPane = splitPane()
						.orientation(JSplitPane.HORIZONTAL_SPLIT)
						.oneTouchExpandable(true)
						.continuousLayout(true)
						.leftComponent(clientInstanceBase)
						.rightComponent(clientInstancePanel)
						.build();

		model.clientInstanceTableModel().selection().item().addConsumer(remoteClient -> {
			clientInstancePanel.removeAll();
			try {
				if (model != null && remoteClient != null) {
					ClientInstanceMonitorPanel clientMonitor = new ClientInstanceMonitorPanel(new ClientInstanceMonitor(model.server(), remoteClient));
					clientInstancePanel.add(clientMonitor, BorderLayout.CENTER);
				}
				revalidate();
				repaint();
			}
			catch (RemoteException ex) {
				throw new RuntimeException(ex);
			}
		});
		setLayout(borderLayout());
		add(splitPane, BorderLayout.CENTER);
	}

	private JPopupMenu createPopupMenu(FilterTable<RemoteClient, RemoteClientColumns.Id> table) {
		return menu(Controls.builder()
						.control(Control.builder()
										.command(this::disconnect)
										.name("Disconnect")
										.enabled(model.clientInstanceTableModel().selection().empty().not()))
						.separator()
						.control(Controls.builder()
										.name("Columns")
										.control(table.createToggleColumnsControls())
										.control(table.createResetColumnsControl())
										.control(table.createSelectAutoResizeModeControl())))
						.buildPopupMenu();
	}

	private void disconnect() throws RemoteException {
		for (RemoteClient remoteClient : model.clientInstanceTableModel().selection().items().get()) {
			model.server().disconnect(remoteClient.clientId());
			model.clientInstanceTableModel().items().removeItem(remoteClient);
		}
	}

	private void toggleAdvancedFilters(boolean advanced) {
		clientInstanceTable.filters().state().set(advanced ?
						ConditionState.ADVANCED : ConditionState.SIMPLE);
		revalidate();
	}

	private static JScrollPane createLinkedScrollPane(JScrollPane parentScrollPane, JComponent componentToScroll) {
		return Components.scrollPane(componentToScroll)
						.horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
						.verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
						.onBuild(scrollPane -> linkBoundedRangeModels(
										parentScrollPane.getHorizontalScrollBar().getModel(),
										scrollPane.getHorizontalScrollBar().getModel()))
						.build();
	}

	private static List<FilterTableColumn<RemoteClientColumns.Id>> createColumns() {
		return asList(
						FilterTableColumn.builder(RemoteClientColumns.Id.USER)
										.headerValue("User")
										.build(),
						FilterTableColumn.builder(RemoteClientColumns.Id.CLIENT_HOST)
										.headerValue("Host")
										.build(),
						FilterTableColumn.builder(RemoteClientColumns.Id.CLIENT_TYPE)
										.headerValue("Type")
										.build(),
						FilterTableColumn.builder(RemoteClientColumns.Id.CLIENT_VERSION)
										.headerValue("Version")
										.build(),
						FilterTableColumn.builder(RemoteClientColumns.Id.CODION_VERSION)
										.headerValue("Framework version")
										.build(),
						FilterTableColumn.builder(RemoteClientColumns.Id.CLIENT_ID)
										.headerValue("Id")
										.build(),
						FilterTableColumn.builder(RemoteClientColumns.Id.LOCALE)
										.headerValue("Locale")
										.build(),
						FilterTableColumn.builder(RemoteClientColumns.Id.TIMEZONE)
										.headerValue("Timezone")
										.build(),
						FilterTableColumn.builder(RemoteClientColumns.Id.CREATION_TIME)
										.headerValue("Created")
										.build()
		);
	}
}
