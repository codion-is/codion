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

import is.codion.tools.monitor.model.HostMonitor;
import is.codion.tools.monitor.model.ServerMonitor;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;

/**
 * A HostMonitorPanel
 */
public final class HostMonitorPanel extends JPanel {

	private final HostMonitor model;

	private JTabbedPane serverPane;

	/**
	 * Instantiates a new HostMonitorPanel
	 * @param model the HostMonitor to base this panel on
	 * @throws RemoteException in case of an exception
	 */
	public HostMonitorPanel(HostMonitor model) throws RemoteException {
		this.model = requireNonNull(model);
		initializeUI();
		bindEvents();
	}

	private void initializeUI() throws RemoteException {
		setLayout(borderLayout());
		serverPane = new JTabbedPane();
		add(serverPane, BorderLayout.CENTER);
		addServerTabs();
	}

	private void bindEvents() {
		model.serverAdded().addConsumer(serverMonitor -> {
			try {
				addServerTab(serverMonitor);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		});
		model.serverRemoved().addConsumer(serverMonitor -> {
			for (int i = 0; i < serverPane.getTabCount(); i++) {
				ServerMonitorPanel panel = (ServerMonitorPanel) serverPane.getComponentAt(i);
				if (panel.model() == serverMonitor) {
					removeServerTab(panel);
				}
			}
		});
	}

	private void addServerTabs() throws RemoteException {
		for (ServerMonitor serverMonitor : model.serverMonitors()) {
			addServerTab(serverMonitor);
		}
	}

	private void addServerTab(ServerMonitor serverMonitor) throws RemoteException {
		ServerMonitorPanel serverMonitorPanel = new ServerMonitorPanel(serverMonitor);
		serverPane.addTab(serverMonitor.serverInformation().name(), serverMonitorPanel);
	}

	private void removeServerTab(ServerMonitorPanel panel) {
		serverPane.remove(panel);
	}
}
