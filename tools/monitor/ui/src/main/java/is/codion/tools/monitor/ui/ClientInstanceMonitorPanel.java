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
import is.codion.common.logging.MethodTrace;
import is.codion.common.user.User;
import is.codion.swing.common.ui.component.logging.LogViewer;
import is.codion.tools.monitor.model.ClientInstanceMonitor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.logging.LogViewer.logViewer;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = LocaleDateTimePattern.builder()
					.delimiterDash().yearFourDigits().hoursMinutesSeconds()
					.build().createFormatter();
	private static final DateTimeFormatter DATE_TIME_FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
	private static final NumberFormat MICROSECOND_FORMAT = NumberFormat.getIntegerInstance();

	private final ClientInstanceMonitor model;
	private final LogViewer logViewer;
	private final DefaultMutableTreeNode logRootNode = new DefaultMutableTreeNode();
	private final DefaultTreeModel logTreeModel = new DefaultTreeModel(logRootNode);

	private final JTextField creationDateField = stringField()
					.editable(false)
					.build();

	/**
	 * Instantiates a new ClientInstanceMonitorPanel
	 * @param model the model
	 * @throws RemoteException in case of an exception
	 */
	public ClientInstanceMonitorPanel(ClientInstanceMonitor model) throws RemoteException {
		this.model = requireNonNull(model);
		this.logViewer = logViewer(new FilenameSupplier());
		initializeUI();
		updateView();
	}

	public void updateView() throws RemoteException {
		creationDateField.setText(DATE_TIME_FORMATTER.format(model.remoteClient().creationTime()));
		refreshLog();
	}

	private void initializeUI() {
		JPanel creationDatePanel = flowLayoutPanel(FlowLayout.LEFT)
						.add(new JLabel("Creation time"))
						.add(creationDateField)
						.build();

		JPanel settingsPanel = flowLayoutPanel(FlowLayout.LEFT)
						.add(checkBox()
										.link(model.loggingEnabled())
										.text("Logging enabled"))
						.add(button()
										.control(command(this::updateView))
										.text("Refresh log"))
						.build();

		JPanel northPanel = borderLayoutPanel()
						.border(createTitledBorder("Connection info"))
						.center(creationDatePanel)
						.east(settingsPanel)
						.build();

		JTabbedPane centerPane = tabbedPane()
						.tab("Text", logViewer)
						.tab("Tree", new JScrollPane(createLogTree()))
						.build();

		setLayout(borderLayout());
		add(northPanel, BorderLayout.NORTH);
		add(centerPane, BorderLayout.CENTER);
	}

	private JTree createLogTree() {
		JTree treeLog = new JTree(logTreeModel);
		treeLog.setRootVisible(false);

		return treeLog;
	}

	private void refreshLog() {
		List<MethodTrace> methodTraces = model.methodTraces();
		StringBuilder logBuilder = new StringBuilder();
		for (MethodTrace trace : methodTraces) {
			trace.appendTo(logBuilder);
			DefaultMutableTreeNode traceNode = new DefaultMutableTreeNode(traceString(trace));
			addChildTraces(traceNode, trace.children());
			logRootNode.add(traceNode);
		}
		logViewer.clear();
		logViewer.append(logBuilder.toString());
		logRootNode.removeAllChildren();
		logTreeModel.setRoot(logRootNode);
	}

	private static void addChildTraces(DefaultMutableTreeNode traceNode, List<MethodTrace> childTraces) {
		for (MethodTrace trace : childTraces) {
			DefaultMutableTreeNode subEntry = new DefaultMutableTreeNode(traceString(trace));
			addChildTraces(subEntry, trace.children());
			traceNode.add(subEntry);
		}
	}

	private static String traceString(MethodTrace trace) {
		StringBuilder builder = new StringBuilder(trace.method()).append(" [")
						.append(MICROSECOND_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(trace.duration())))
						.append(" μs").append("]");
		String enterMessage = trace.message();
		if (enterMessage != null) {
			builder.append(": ").append(enterMessage.replace('\n', ' '));
		}

		return builder.toString();
	}

	private final class FilenameSupplier implements Supplier<String> {

		@Override
		public String get() {
			if (creationDateField.getText().isEmpty()) {
				throw new IllegalStateException("No client selected");
			}

			User user = model.remoteClient().user();
			LocalDateTime creationDate = LocalDateTime.parse(creationDateField.getText(), DATE_TIME_FORMATTER);

			return user.username() + "@" + DATE_TIME_FILENAME_FORMATTER.format(creationDate) + ".log";
		}
	}
}
