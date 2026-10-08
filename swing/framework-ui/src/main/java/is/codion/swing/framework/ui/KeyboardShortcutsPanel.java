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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TextInput;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntitySearchField;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * Displays the keyboard shortcuts, those of controls based on their default key strokes.
 * @see ControlKey#defaultKeystroke()
 */
final class KeyboardShortcutsPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(KeyboardShortcutsPanel.class, getBundle(KeyboardShortcutsPanel.class.getName()));

	private static final int VERTICAL_UNIT_INCREMENT = 16;

	private static final String PLUS = " + ";
	private static final String OR = " or ";

	KeyboardShortcutsPanel() {
		super(borderLayout());
		flexibleGridLayoutPanel(0, 1)
						.addAll(Stream.of(navigation(), resizing(), focusTransferral(), editPanel(), tablePanel(), conditionPanel(),
														searchField(), tableExport(), dateTimeField(), calendar(), entityField(), textFieldPanel(), dependencies())
										.filter(Section::hasRows)
										.map(Section::panel)
										.collect(toList()))
						.scrollPane()
						.verticalUnitIncrement(VERTICAL_UNIT_INCREMENT)
						.onBuild(KeyboardShortcutsPanel::addScrollKeyEvents)
						.build(this::add);
	}

	private static Section navigation() {
		return new Section(message("navigation"))
						.keys(message("navigate_up_down"), EntityPanel.ControlKeys.NAVIGATE_UP, EntityPanel.ControlKeys.NAVIGATE_DOWN)
						.keys(message("navigate_left_right"), EntityPanel.ControlKeys.NAVIGATE_LEFT, EntityPanel.ControlKeys.NAVIGATE_RIGHT);
	}

	private static Section resizing() {
		return new Section(message("resizing"))
						.keys(message("resize_left_right"), TabbedDetailLayout.ControlKeys.RESIZE_LEFT, TabbedDetailLayout.ControlKeys.RESIZE_RIGHT)
						.keys(message("expand_collapse"), TabbedDetailLayout.ControlKeys.EXPAND, TabbedDetailLayout.ControlKeys.COLLAPSE)
						.keys(message("toggle_edit_panel"), EntityPanel.ControlKeys.TOGGLE_EDIT_PANEL);
	}

	private static Section focusTransferral() {
		return new Section(message("transfer_focus"))
						.keys(message("transfer_focus_edit_panel"), EntityPanel.ControlKeys.FOCUS_EDIT_PANEL)
						.keys(message("transfer_focus_table"), EntityTablePanel.ControlKeys.FOCUS_TABLE)
						.keys(message("transfer_focus_input_field"), EntityEditPanel.ControlKeys.SELECT_INPUT_FIELD)
						.keys(message("transfer_focus_search_field"), EntityTablePanel.ControlKeys.SELECT_CONDITION)
						.keys(message("transfer_focus_find_in_table"), EntityTablePanel.ControlKeys.FOCUS_SEARCH_FIELD);
	}

	private static Section editPanel() {
		return new Section(message("edit_panel"))
						.row(message("transfer_focus_to_next_input_field"), text(keyStroke(VK_ENTER)))
						.row(message("transfer_focus_to_previous_input_field"), text(keyStroke(VK_ENTER, SHIFT_DOWN_MASK)))
						.row(message("add"), mnemonic(FrameworkMessages.insertMnemonic(), EntityEditPanel.ControlKeys.INSERT))
						.row(message("update"), mnemonic(FrameworkMessages.updateMnemonic(), EntityEditPanel.ControlKeys.UPDATE))
						.row(message("delete"), mnemonic(FrameworkMessages.deleteMnemonic(), EntityEditPanel.ControlKeys.DELETE))
						.row(Messages.clear(), mnemonic(Messages.clearMnemonic(), EntityEditPanel.ControlKeys.CLEAR))
						.row(message("refresh"), mnemonic(Messages.refreshMnemonic(), EntityPanel.ControlKeys.REFRESH));
	}

	private static Section tablePanel() {
		return new Section(message("table_panel"))
						.keys(message("add_new_item"), EntityTablePanel.ControlKeys.ADD)
						.keys(message("edit_selected_item"), EntityTablePanel.ControlKeys.EDIT)
						.keys(message("edit_value"), EntityTablePanel.ControlKeys.EDIT_ATTRIBUTE)
						.keys(message("view_selected_item"), EntityTablePanel.ControlKeys.VIEW_ENTITY)
						.keys(message("delete_selected"), EntityTablePanel.ControlKeys.DELETE)
						// the look and feel copy action of the table, rows only
						.row(message("copy_selected_rows"), alternatives(keyStroke(VK_C, MENU_SHORTCUT_MASK),
										EntityTablePanel.ControlKeys.COPY_ROWS.defaultKeystroke().get()))
						.keys(message("copy_selected_cell"), EntityTablePanel.ControlKeys.COPY_CELL)
						.keys(message("copy_selected_column"), EntityTablePanel.ControlKeys.COPY_COLUMN)
						.keys(message("move_selected_column"), FilterTable.ControlKeys.MOVE_COLUMN_LEFT, FilterTable.ControlKeys.MOVE_COLUMN_RIGHT)
						.keys(message("resize_selected_column"), FilterTable.ControlKeys.INCREASE_COLUMN_SIZE, FilterTable.ControlKeys.DECREASE_COLUMN_SIZE)
						.keys(message("move_selection"), EntityTablePanel.ControlKeys.DECREMENT_SELECTION, EntityTablePanel.ControlKeys.INCREMENT_SELECTION)
						.keys(message("show_popup_menu"), EntityTablePanel.ControlKeys.POPUP_MENU)
						.keys(message("print"), EntityTablePanel.ControlKeys.PRINT)
						.keys(message("refresh"), EntityTablePanel.ControlKeys.REFRESH)
						.keys(message("toggle_condition_panel"), EntityTablePanel.ControlKeys.TOGGLE_CONDITION_VIEW)
						.keys(message("select_condition_panel"), EntityTablePanel.ControlKeys.SELECT_CONDITION)
						.keys(message("toggle_filter_panel"), EntityTablePanel.ControlKeys.TOGGLE_FILTER_VIEW)
						.keys(message("select_filter_panel"), EntityTablePanel.ControlKeys.SELECT_FILTER)
						.keys(message("toggle_column_sort"), FilterTable.ControlKeys.TOGGLE_PREVIOUS_SORT_ORDER, FilterTable.ControlKeys.TOGGLE_NEXT_SORT_ORDER)
						.keys(message("toggle_column_sort_add"), FilterTable.ControlKeys.TOGGLE_PREVIOUS_SORT_ORDER_ADD, FilterTable.ControlKeys.TOGGLE_NEXT_SORT_ORDER_ADD);
	}

	private static Section conditionPanel() {
		return new Section(message("condition_panel"))
						.keys(message("previous_next_operator"), ColumnConditionPanel.ControlKeys.PREVIOUS_OPERATOR, ColumnConditionPanel.ControlKeys.NEXT_OPERATOR)
						.keys(message("enable_disable_condition"), ColumnConditionPanel.ControlKeys.TOGGLE_ENABLED)
						.keys(message("clear_condition"), ColumnConditionPanel.ControlKeys.CLEAR)
						.row(message("refresh_table_data"), text(keyStroke(VK_ENTER)));
	}

	private static Section searchField() {
		return new Section(message("table_search_field"))
						.row(message("find_next"), alternatives(keyStroke(VK_ENTER), keyStroke(VK_DOWN)))
						.row(message("select_next"), alternatives(keyStroke(VK_ENTER, MENU_SHORTCUT_MASK), keyStroke(VK_DOWN, MENU_SHORTCUT_MASK)))
						.row(message("add_next_to_selection"), alternatives(keyStroke(VK_ENTER, SHIFT_DOWN_MASK), keyStroke(VK_DOWN, SHIFT_DOWN_MASK)))
						.row(message("find_previous"), text(keyStroke(VK_UP)))
						.row(message("select_previous"), text(keyStroke(VK_UP, MENU_SHORTCUT_MASK)))
						.row(message("add_previous_to_selection"), text(keyStroke(VK_UP, SHIFT_DOWN_MASK)))
						.row(message("toggle_search_result_selection"), text(keyStroke(VK_SPACE, MENU_SHORTCUT_MASK)))
						.row(message("move_focus_to_table"), text(keyStroke(VK_ESCAPE)));
	}

	private static Section tableExport() {
		return new Section(message("table_export"))
						.row(message("toggle_include_attribute"), text(keyStroke(VK_SPACE)));
	}

	private static Section dateTimeField() {
		return new Section(message("date_time_field"))
						.keys(message("display_calendar"), TemporalField.ControlKeys.DISPLAY_CALENDAR)
						.keys(message("increment_decrement"), TemporalField.ControlKeys.INCREMENT, TemporalField.ControlKeys.DECREMENT);
	}

	private static Section calendar() {
		return new Section(message("calendar"))
						.keys(message("previous_next_year"), CalendarPanel.ControlKeys.PREVIOUS_YEAR, CalendarPanel.ControlKeys.NEXT_YEAR)
						.keys(message("previous_next_month"), CalendarPanel.ControlKeys.PREVIOUS_MONTH, CalendarPanel.ControlKeys.NEXT_MONTH)
						.keys(message("previous_next_day"), CalendarPanel.ControlKeys.PREVIOUS_DAY, CalendarPanel.ControlKeys.NEXT_DAY)
						.keys(message("previous_next_week"), CalendarPanel.ControlKeys.PREVIOUS_WEEK, CalendarPanel.ControlKeys.NEXT_WEEK)
						.keys(message("previous_next_hour"), CalendarPanel.ControlKeys.PREVIOUS_HOUR, CalendarPanel.ControlKeys.NEXT_HOUR)
						.keys(message("previous_next_minute"), CalendarPanel.ControlKeys.PREVIOUS_MINUTE, CalendarPanel.ControlKeys.NEXT_MINUTE);
	}

	private static Section entityField() {
		return new Section(message("entity_field"))
						.row(message("add_new_item"), alternatives(EntityComboBox.ControlKeys.ADD.defaultKeystroke().get(),
										EntitySearchField.ControlKeys.ADD.defaultKeystroke().get()))
						.row(message("edit_selected_item"), alternatives(EntityComboBox.ControlKeys.EDIT.defaultKeystroke().get(),
										EntitySearchField.ControlKeys.EDIT.defaultKeystroke().get()));
	}

	private static Section textFieldPanel() {
		return new Section(message("text_field_panel"))
						.keys(message("display_input_dialog"), TextInput.ControlKeys.DISPLAY_TEXT_AREA);
	}

	private static Section dependencies() {
		return new Section(FrameworkMessages.dependencies())
						.keys(message("navigate_left_right"), EntityDependenciesPanel.ControlKeys.NAVIGATE_LEFT, EntityDependenciesPanel.ControlKeys.NAVIGATE_RIGHT);
	}

	/**
	 * @param keys the control keys
	 * @return the default key strokes of the given keys, sharing modifiers shown once, such as Ctrl + ↑/↓
	 */
	static String keys(ControlKey<?>... keys) {
		List<KeyStroke> keyStrokes = Stream.of(keys)
						.map(key -> key.defaultKeystroke().get())
						.filter(Objects::nonNull)
						.collect(toList());
		if (keyStrokes.isEmpty()) {
			return "";
		}
		int modifiers = keyStrokes.get(0).getModifiers();
		if (keyStrokes.stream().allMatch(keyStroke -> keyStroke.getModifiers() == modifiers)) {
			return modifiers(modifiers) + keyStrokes.stream()
							.map(keyStroke -> key(keyStroke.getKeyCode()))
							.collect(joining("/"));
		}

		return keyStrokes.stream()
						.map(KeyboardShortcutsPanel::text)
						.collect(joining(" / "));
	}

	static String alternatives(KeyStroke... keyStrokes) {
		return Stream.of(keyStrokes)
						.filter(Objects::nonNull)
						.map(KeyboardShortcutsPanel::text)
						.distinct()
						.collect(joining(OR));
	}

	static String text(KeyStroke keyStroke) {
		return modifiers(keyStroke.getModifiers()) + key(keyStroke.getKeyCode());
	}

	private static String mnemonic(char mnemonic, ControlKey<?> controlKey) {
		String text = modifiers(ALT_DOWN_MASK) + Character.toUpperCase(mnemonic);
		KeyStroke keyStroke = controlKey.defaultKeystroke().get();

		return keyStroke == null ? text : text + OR + text(keyStroke);
	}

	private static KeyStroke keyStroke(int keyCode) {
		return keyStroke(keyCode, 0);
	}

	private static KeyStroke keyStroke(int keyCode, int modifiers) {
		return KeyEvents.keyStroke(keyCode, modifiers);
	}

	private static String modifiers(int modifiers) {
		StringBuilder builder = new StringBuilder();
		if ((modifiers & META_DOWN_MASK) != 0) {
			builder.append(MENU_SHORTCUT_MASK == META_DOWN_MASK ? "⌘" : "Meta").append(PLUS);
		}
		if ((modifiers & CTRL_DOWN_MASK) != 0) {
			builder.append("Ctrl").append(PLUS);
		}
		if ((modifiers & ALT_DOWN_MASK) != 0) {
			builder.append("Alt").append(PLUS);
		}
		if ((modifiers & ALT_GRAPH_DOWN_MASK) != 0) {
			builder.append("AltGr").append(PLUS);
		}
		if ((modifiers & SHIFT_DOWN_MASK) != 0) {
			builder.append("Shift").append(PLUS);
		}

		return builder.toString();
	}

	private static String key(int keyCode) {
		switch (keyCode) {
			case VK_UP:
				return "↑";
			case VK_DOWN:
				return "↓";
			case VK_LEFT:
				return "←";
			case VK_RIGHT:
				return "→";
			case VK_ADD:
				return "+";
			case VK_SUBTRACT:
				return "-";
			default:
				return KeyEvent.getKeyText(keyCode);
		}
	}

	private static String message(String key) {
		return MESSAGES.getString(key);
	}

	private static void addScrollKeyEvents(JScrollPane scrollPane) {
		JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
		KeyEvents.builder()
						.keyCode(VK_UP)
						.action(command(() -> verticalScrollBar.setValue(verticalScrollBar.getValue() - VERTICAL_UNIT_INCREMENT)))
						.enable(scrollPane);
		KeyEvents.builder()
						.keyCode(VK_DOWN)
						.action(command(() -> verticalScrollBar.setValue(verticalScrollBar.getValue() + VERTICAL_UNIT_INCREMENT)))
						.enable(scrollPane);
	}

	private static final class Section {

		private final String title;
		private final List<JComponent> labels = new ArrayList<>();

		private Section(String title) {
			this.title = title;
		}

		private Section keys(String label, ControlKey<?>... keys) {
			return row(label, KeyboardShortcutsPanel.keys(keys));
		}

		/**
		 * @param label the label
		 * @param text the key stroke text, an empty one omitting the row
		 * @return this section
		 */
		private Section row(String label, String text) {
			if (!text.isEmpty()) {
				labels.add(new JLabel(label));
				labels.add(new JLabel(text));
			}

			return this;
		}

		private boolean hasRows() {
			return !labels.isEmpty();
		}

		private JPanel panel() {
			return gridLayoutPanel(0, 2)
							.addAll(labels)
							.border(createTitledBorder(title))
							.build();
		}
	}
}
