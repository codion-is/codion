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

import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.common.utilities.logging.MethodTrace;
import is.codion.common.utilities.user.User;
import is.codion.swing.common.ui.component.logging.LogViewer;
import is.codion.tools.monitor.model.ClientInstanceMonitor;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
					.build().formatter();
	private static final DateTimeFormatter DATE_TIME_FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
	private static final NumberFormat MICROSECOND_FORMAT = NumberFormat.getIntegerInstance();

	private final ClientInstanceMonitor model;
	private final LogViewer logViewer;

	private final JTextField creationDateField = stringField()
					.editable(false)
					.build();

	/**
	 * Instantiates a new ClientInstanceMonitorPanel
	 * @param model the model
	 */
	public ClientInstanceMonitorPanel(ClientInstanceMonitor model) {
		this.model = requireNonNull(model);
		this.logViewer = logViewer(new FilenameSupplier());
		initializeUI();
		updateView();
	}

	public void updateView() {
		creationDateField.setText(DATE_TIME_FORMATTER.format(model.remoteClient().creationTime()));
		refreshLog();
	}

	private void initializeUI() {
		JPanel settingsPanel = flowLayoutPanel(FlowLayout.LEFT)
						.add(checkBox()
										.link(model.tracingEnabled())
										.text("Tracing enabled"))
						.add(checkBox()
										.link(model.traceToFileEnabled())
										.text("Trace to file"))
						.add(button()
										.control(command(this::updateView))
										.text("Refresh"))
						.build();

		JPanel centerPane = borderLayoutPanel()
						.border(createTitledBorder("Trace log"))
						.north(borderLayoutPanel()
										.east(settingsPanel))
						.center(logViewer)
						.build();

		setLayout(borderLayout());
		add(centerPane, BorderLayout.CENTER);
	}

	private void refreshLog() {
		List<MethodTrace> methodTraces = model.methodTraces();
		StringBuilder logBuilder = new StringBuilder();
		for (MethodTrace trace : methodTraces) {
			trace.appendTo(logBuilder);
			DefaultMutableTreeNode traceNode = new DefaultMutableTreeNode(traceString(trace));
			addChildTraces(traceNode, trace.children());
		}
		logViewer.clear();
		logViewer.append(logBuilder.toString());
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

			return user.username() + "@" + DATE_TIME_FILENAME_FORMATTER.format(creationDate);
		}
	}
}
