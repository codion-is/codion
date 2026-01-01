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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.logging;

import is.codion.common.i18n.Messages;
import is.codion.common.utilities.logging.LoggerProxy;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.Components.comboBox;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS;

/**
 * A UI panel for configuring logging levels for a set of loggers.
 * <p>
 * Displays a table with two columns:
 * <ul>
 *   <li><b>Logger</b> - The logger name (e.g., "is.codion.framework.db", "is.codion.swing.framework.ui")</li>
 *   <li><b>Level</b> - The effective log level, editable via combo box</li>
 * </ul>
 * <p>
 * The table shows all loggers provided by the {@link LoggerProxy}, plus any parent loggers in the hierarchy.
 * For example, if the supplier provides "is.codion.framework.db.local", the table will also include
 * parent loggers "is.codion.framework.db", "is.codion.framework", "is.codion", and "is", along with "ROOT".
 * This allows configuration of logging at any level of the package hierarchy.
 * <p>
 * The Level column shows the <i>effective</i> level for each logger, which may be:
 * <ul>
 *   <li>An explicitly configured level for that specific logger</li>
 *   <li>A level inherited from the nearest parent logger with an explicit configuration</li>
 *   <li>The ROOT logger level if no parent has an explicit configuration</li>
 * </ul>
 * <p>
 * Changes to log levels are applied immediately via {@link LoggerProxy#setLogLevel(String, Object)}.
 * When a logger's level is changed, child loggers that don't have explicit levels will automatically
 * inherit the new level. The table refreshes after each change to reflect the current state of the
 * logging system.
 * <p>
 */
public final class LogLevelPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(LogLevelPanel.class, getBundle(LogLevelPanel.class.getName()));

	private final LoggerProxy loggerProxy;
	private final FilterTableModel<LogLevelRow, LogLevelColumn> tableModel;
	private final FilterTable<LogLevelRow, LogLevelColumn> table;
	private final JTextField filterField;

	private LogLevelPanel(LoggerProxy loggerProxy) {
		super(borderLayout());
		this.loggerProxy = loggerProxy;
		tableModel = FilterTableModel.builder()
						.columns(new LogLevelColumns())
						.items(new LogLevelItems())
						.editor(LogLevelEditor::new)
						.refresh(true)
						.build();
		tableModel.sort().ascending(LogLevelColumn.LOGGER);
		table = FilterTable.builder()
						.model(tableModel)
						.sortable(false)
						.autoResizeMode(AUTO_RESIZE_ALL_COLUMNS)
						.columnReordering(false)
						.columns(LogLevelPanel::configureColumns)
						.cellEditor(LogLevelColumn.LEVEL, FilterTableCellEditor.builder()
										.component(() -> comboBox()
														.model(FilterComboBoxModel.builder()
																		.items(loggerProxy.levels())
																		.comparator(null)
																		.build())
														.buildValue())
										.build())
						.cellEditable(LogLevelPanel::cellEditable)
						.surrendersFocusOnKeystroke(true)
						.build();
		filterField = Components.stringField()
						.link(tableModel.filters().<String>get(LogLevelColumn.LOGGER).operands().equal())
						.hint(Messages.filterVerb())
						.selectAllOnFocusGained(true)
						.keyEvent(KeyEvents.builder()
										.keyCode(KeyEvent.VK_ESCAPE)
										.action(command(table::requestFocusInWindow)))
						.build();
		KeyEvents.builder()
						.keyCode(KeyEvent.VK_F)
						.modifiers(InputEvent.CTRL_DOWN_MASK)
						.action(command(filterField::requestFocusInWindow))
						.enable(table);
		add(scrollPane()
						.view(table)
						.build(), BorderLayout.CENTER);
		add(filterField, BorderLayout.SOUTH);
	}

	private static boolean cellEditable(LogLevelRow row, LogLevelColumn column) {
		return LogLevelColumn.LEVEL.equals(column);
	}

	private static void configureColumns(FilterTableColumn.Builder<LogLevelColumn> column) {
		switch (column.identifier()) {
			case LOGGER:
				column.headerValue(MESSAGES.getString("logger"));
				break;
			case LEVEL:
				column.headerValue(MESSAGES.getString("level"));
				column.fixedWidth(100);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	/**
	 * Creates a new {@link LogLevelPanel} configured to use the local {@link LoggerProxy} instance.
	 * <p>
	 * @return a new {@link LogLevelPanel}
	 * @see LoggerProxy#instance()
	 */
	public static LogLevelPanel logLevelPanel() {
		return logLevelPanel(LoggerProxy.instance());
	}

	/**
	 * Creates a new {@link LogLevelPanel} configured to use the given {@link LoggerProxy} instance.
	 * <p>
	 * @param loggerProxy the logger proxy
	 * @return a new {@link LogLevelPanel}
	 */
	public static LogLevelPanel logLevelPanel(LoggerProxy loggerProxy) {
		return new LogLevelPanel(requireNonNull(loggerProxy));
	}

	enum LogLevelColumn {
		LOGGER, LEVEL
	}

	interface LogLevelRow {

		String logger();

		Object level();
	}

	private final class LogLevelItems implements Supplier<Collection<LogLevelRow>> {

		@Override
		public Collection<LogLevelRow> get() {
			List<LogLevelRow> rows = new ArrayList<>();
			Map<String, Object> logLevels = new HashMap<>();
			Object rootLevel = logLevels.computeIfAbsent(loggerProxy.rootLogger(), loggerProxy::getLogLevel);
			loggerProxy.loggers().stream().sorted().forEach(logger -> {
				String loggerName = logger;
				Object logLevel = loggerProxy.getLogLevel(loggerName);
				while (logLevel == null && loggerName.contains(".")) {
					loggerName = loggerName.substring(0, loggerName.lastIndexOf('.'));
					logLevel = logLevels.computeIfAbsent(loggerName, loggerProxy::getLogLevel);
				}
				rows.add(new DefaultLogLevelRow(logger, logLevel == null ? rootLevel : logLevel));
			});

			return rows;
		}
	}

	private final class LogLevelEditor implements FilterTableModel.Editor<LogLevelRow, LogLevelColumn> {

		private final FilterTableModel<LogLevelRow, LogLevelColumn> tableModel;

		private LogLevelEditor(FilterTableModel<LogLevelRow, LogLevelColumn> tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public boolean editable(LogLevelRow row, LogLevelColumn identifier) {
			return LogLevelColumn.LEVEL.equals(identifier);
		}

		@Override
		public void set(Object value, int rowIndex, LogLevelRow row, LogLevelColumn identifier) {
			if (LogLevelColumn.LEVEL.equals(identifier)) {
				loggerProxy.setLogLevel(row.logger(), value);
				tableModel.items().refresh();
			}
		}
	}

	private static final class DefaultLogLevelRow implements LogLevelRow {

		private final String logger;
		private final Object level;

		private DefaultLogLevelRow(String logger, Object level) {
			this.logger = logger;
			this.level = level;
		}

		@Override
		public String logger() {
			return logger;
		}

		@Override
		public Object level() {
			return level;
		}

		@Override
		public boolean equals(Object object) {
			if (object == null || getClass() != object.getClass()) {
				return false;
			}
			DefaultLogLevelRow that = (DefaultLogLevelRow) object;

			return Objects.equals(logger, that.logger);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(logger);
		}
	}

	private static class LogLevelColumns implements FilterTableModel.TableColumns<LogLevelRow, LogLevelColumn> {

		private static final List<LogLevelColumn> IDENTIFIERS = asList(LogLevelColumn.values());

		@Override
		public List<LogLevelColumn> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(LogLevelColumn identifier) {
			switch (identifier) {
				case LOGGER:
					return String.class;
				case LEVEL:
					return Object.class;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public Object value(LogLevelRow row, LogLevelColumn identifier) {
			switch (identifier) {
				case LOGGER:
					return row.logger();
				case LEVEL:
					return row.level();
				default:
					throw new IllegalArgumentException();
			}
		}
	}
}
