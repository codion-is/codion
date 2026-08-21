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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor.ui;

import is.codion.common.reactive.state.State;
import is.codion.common.rmi.server.RemoteSession;
import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.tools.monitor.model.SessionInstanceMonitor;
import is.codion.tools.monitor.model.SessionMonitor;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.tools.monitor.model.SessionMonitor.RemoteSessionColumns.CREATION_TIME;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

/**
 * A SessionMonitorPanel
 */
public final class SessionMonitorPanel extends JPanel {

	private static final DateTimeFormatter CREATED_FORMATTER = LocaleDateTimePattern.builder()
					.delimiterDash()
					.yearFourDigits()
					.hoursMinutesSeconds()
					.build()
					.formatter();

	private final SessionMonitor model;
	private final FilterTable<RemoteSession, String> sessionTable;
	private final JScrollPane filterScrollPane;
	private final JScrollPane sessionScroller;
	private final State advancedFilterState = State.builder()
					.consumer(this::toggleAdvancedFilters)
					.build();

	/**
	 * Instantiates a new SessionMonitorPanel
	 * @param model the model
	 */
	public SessionMonitorPanel(SessionMonitor model) {
		this.model = model;
		sessionTable = FilterTable.builder()
						.model(model.sessionTableModel())
						.cellRenderer(CREATION_TIME, LocalDateTime.class, renderer -> renderer
										.formatter(CREATED_FORMATTER::format))
						.popupMenu(this::createPopupMenu)
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.filterView(ConditionView.SIMPLE)
						.build();
		sessionScroller = scrollPane()
						.view(sessionTable)
						.border(BorderFactory.createTitledBorder("Sessions"))
						.build();
		filterScrollPane = createLinkedScrollPane(sessionScroller, sessionTable.filters());
		initializeUI();
	}

	public SessionMonitor model() {
		return model;
	}

	public void refresh() {
		model.refresh();
	}

	private void initializeUI() {
		JPanel clientInstanceBase = borderLayoutPanel()
						.north(filterScrollPane)
						.center(sessionScroller)
						.south(borderLayoutPanel()
										.south(borderLayoutPanel()
														.center(sessionTable.searchField())
														.east(flexibleGridLayoutPanel(1, 2)
																		.add(checkBox()
																						.link(advancedFilterState)
																						.text("Advanced filters"))
																		.add(button()
																						.control(command(this::refresh))
																						.text("Refresh")))))
						.build();

		JPanel sessionInstancePanel = borderLayoutPanel().build();
		JSplitPane splitPane = splitPane()
						.orientation(JSplitPane.HORIZONTAL_SPLIT)
						.oneTouchExpandable(true)
						.continuousLayout(true)
						.leftComponent(clientInstanceBase)
						.rightComponent(sessionInstancePanel)
						.build();

		model.sessionTableModel().selection().item().addConsumer(session -> {
			sessionInstancePanel.removeAll();
			try {
				if (session != null) {
					SessionInstanceMonitorPanel sessionMonitor = new SessionInstanceMonitorPanel(new SessionInstanceMonitor(model.server(), session));
					sessionInstancePanel.add(sessionMonitor, BorderLayout.CENTER);
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

	private JPopupMenu createPopupMenu(FilterTable<RemoteSession, String> table) {
		return menu()
						.controls(Controls.builder()
										.control(Control.builder()
														.command(this::disconnect)
														.caption("Disconnect")
														.enabled(model.sessionTableModel().selection().empty().not()))
										.separator()
										.control(Controls.builder()
														.caption("Columns")
														.control(table.createToggleColumnsControls())
														.control(table.createResetColumnsControl())
														.control(table.createSelectAutoResizeModeControl())))
						.buildPopupMenu();
	}

	private void disconnect() throws RemoteException {
		for (RemoteSession session : model.sessionTableModel().selection().items().get()) {
			model.server().disconnect(session.id());
			model.sessionTableModel().items().remove(session);
		}
	}

	private void toggleAdvancedFilters(boolean advanced) {
		sessionTable.filters().view().set(advanced ?
						ConditionView.ADVANCED : ConditionView.SIMPLE);
		revalidate();
	}

	private static JScrollPane createLinkedScrollPane(JScrollPane parentScrollPane, JComponent componentToScroll) {
		return Components.scrollPane()
						.view(componentToScroll)
						.horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
						.verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
						.onBuild(scrollPane -> new ScrollPaneSynchronizer(parentScrollPane, scrollPane))
						.build();
	}

	private static final class ScrollPaneSynchronizer {

		private final BoundedRangeModel mainModel;
		private final BoundedRangeModel linkedModel;

		private ScrollPaneSynchronizer(JScrollPane main, JScrollPane linked) {
			mainModel = main.getHorizontalScrollBar().getModel();
			linkedModel = linked.getHorizontalScrollBar().getModel();
			mainModel.addChangeListener(e -> synchronize());
			linked.addHierarchyListener(e -> synchronize());
		}

		private void synchronize() {
			linkedModel.setRangeProperties(mainModel.getValue(), mainModel.getExtent(),
							mainModel.getMinimum(), mainModel.getMaximum(), mainModel.getValueIsAdjusting());
		}
	}
}
