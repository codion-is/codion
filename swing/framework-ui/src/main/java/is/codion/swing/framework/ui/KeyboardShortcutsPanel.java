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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.resource.MessageBundle;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_UP;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createTitledBorder;

final class KeyboardShortcutsPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(KeyboardShortcutsPanel.class, getBundle(KeyboardShortcutsPanel.class.getName()));

	private static final int VERTICAL_UNIT_INCREMENT = 16;

	private static final String ALT = "Alt + ";
	private static final String CTRL = "Ctrl + ";
	private static final String SHIFT = "Shift + ";
	private static final String INSERT = "Insert";
	private static final String ENTER = "Enter";
	private static final String LEFT_RIGHT = "←/→";
	private static final String UP_DOWN = "↑/↓";
	private static final String DOWN_UP = "↓/↑";
	private static final String UP = "↑";
	private static final String DOWN = "↓";
	private static final String OR = " or ";

	KeyboardShortcutsPanel() {
		super(borderLayout());
		flexibleGridLayoutPanel(0, 1)
						.add(navigation())
						.add(resizing())
						.add(focusTransferral())
						.add(editPanel())
						.add(tablePanel())
						.add(conditionPanel())
						.add(searchField())
						.add(dateTimeField())
						.add(calendar())
						.add(entityField())
						.add(textFieldPanel())
						.add(dependencies())
						.scrollPane()
						.verticalUnitIncrement(VERTICAL_UNIT_INCREMENT)
						.onBuild(KeyboardShortcutsPanel::addScrollKeyEvents)
						.build(this::add);
	}

	private static JPanel navigation() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("navigate_up_down")), new JLabel(CTRL + ALT + UP_DOWN))
						.addAll(new JLabel(MESSAGES.getString("navigate_left_right")), new JLabel(CTRL + ALT + LEFT_RIGHT))
						.border(createTitledBorder(MESSAGES.getString("navigation")))
						.build();
	}

	private static JPanel resizing() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("resize_left_right")), new JLabel(SHIFT + ALT + LEFT_RIGHT))
						.addAll(new JLabel(MESSAGES.getString("expand_collapse")), new JLabel(CTRL + SHIFT + ALT + LEFT_RIGHT))
						.addAll(new JLabel(MESSAGES.getString("toggle_edit_panel")), new JLabel(CTRL + ALT + "E"))
						.border(createTitledBorder(MESSAGES.getString("resizing")))
						.build();
	}

	private static JPanel focusTransferral() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("transfer_focus_edit_panel")), new JLabel(CTRL + "E"))
						.addAll(new JLabel(MESSAGES.getString("transfer_focus_table")), new JLabel(CTRL + "T"))
						.addAll(new JLabel(MESSAGES.getString("transfer_focus_input_field")), new JLabel(CTRL + "I"))
						.addAll(new JLabel(MESSAGES.getString("transfer_focus_search_field")), new JLabel(CTRL + "S"))
						.addAll(new JLabel(MESSAGES.getString("transfer_focus_find_in_table")), new JLabel(CTRL + "F"))
						.border(createTitledBorder(MESSAGES.getString("transfer_focus")))
						.build();
	}

	private static JPanel editPanel() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("transfer_focus_to_next_input_field")), new JLabel(ENTER))
						.addAll(new JLabel(MESSAGES.getString("transfer_focus_to_previous_input_field")), new JLabel(SHIFT + ENTER))
						.addAll(new JLabel(MESSAGES.getString("add")), new JLabel(ALT + FrameworkMessages.insertMnemonic()))
						.addAll(new JLabel(MESSAGES.getString("save")), new JLabel(ALT + FrameworkMessages.saveMnemonic()))
						.addAll(new JLabel(MESSAGES.getString("update")), new JLabel(ALT + FrameworkMessages.updateMnemonic()))
						.addAll(new JLabel(MESSAGES.getString("delete")), new JLabel(ALT + FrameworkMessages.deleteMnemonic()))
						.addAll(new JLabel(Messages.clear()), new JLabel(ALT + Messages.clearMnemonic()))
						.addAll(new JLabel(MESSAGES.getString("refresh")), new JLabel(ALT + Messages.refreshMnemonic()))
						.border(createTitledBorder(MESSAGES.getString("edit_panel")))
						.build();
	}

	private static JPanel tablePanel() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("add_new_item")), new JLabel(INSERT))
						.addAll(new JLabel(MESSAGES.getString("edit_selected_item")), new JLabel(CTRL + INSERT))
						.addAll(new JLabel(MESSAGES.getString("edit_value")), new JLabel(SHIFT + INSERT))
						.addAll(new JLabel(MESSAGES.getString("delete_selected")), new JLabel("Delete"))
						.addAll(new JLabel(MESSAGES.getString("copy_selected_rows")), new JLabel(CTRL + "C"))
						.addAll(new JLabel(MESSAGES.getString("copy_selected_cell")), new JLabel(CTRL + ALT + "C"))
						.addAll(new JLabel(MESSAGES.getString("move_selected_column")), new JLabel(CTRL + SHIFT + LEFT_RIGHT))
						.addAll(new JLabel(MESSAGES.getString("resize_selected_column")), new JLabel(CTRL + "+/-"))
						.addAll(new JLabel(MESSAGES.getString("move_selection")), new JLabel(CTRL + SHIFT + UP + "/" + DOWN))
						.addAll(new JLabel(MESSAGES.getString("show_popup_menu")), new JLabel(CTRL + "G"))
						.addAll(new JLabel(MESSAGES.getString("print")), new JLabel(CTRL + "P"))
						.addAll(new JLabel(MESSAGES.getString("refresh")), new JLabel(ALT + "R"))
						.addAll(new JLabel(MESSAGES.getString("refresh_button")), new JLabel("F5"))
						.addAll(new JLabel(MESSAGES.getString("toggle_condition_panel")), new JLabel(CTRL + ALT + "S"))
						.addAll(new JLabel(MESSAGES.getString("select_condition_panel")), new JLabel(CTRL + "S"))
						.addAll(new JLabel(MESSAGES.getString("toggle_filter_panel")), new JLabel(CTRL + ALT + "F"))
						.addAll(new JLabel(MESSAGES.getString("select_filter_panel")), new JLabel(CTRL + SHIFT + "F"))
						.addAll(new JLabel(MESSAGES.getString("toggle_column_sort")), new JLabel(ALT + UP + "/" + DOWN))
						.addAll(new JLabel(MESSAGES.getString("toggle_column_sort_add")), new JLabel(SHIFT + ALT + UP + "/" + DOWN))
						.border(createTitledBorder(MESSAGES.getString("table_panel")))
						.build();
	}

	private static JPanel conditionPanel() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("previous_next_operator")), new JLabel(CTRL + UP_DOWN))
						.addAll(new JLabel(MESSAGES.getString("enable_disable_condition")), new JLabel(CTRL + ENTER))
						.addAll(new JLabel(MESSAGES.getString("clear_condition")), new JLabel(CTRL + SHIFT + ENTER))
						.addAll(new JLabel(MESSAGES.getString("refresh_table_data")), new JLabel(ENTER))
						.border(createTitledBorder(MESSAGES.getString("condition_panel")))
						.build();
	}

	private static JPanel searchField() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("find_next")), new JLabel(ENTER + OR + DOWN))
						.addAll(new JLabel(MESSAGES.getString("find_and_select_next")), new JLabel(SHIFT + ENTER + OR + DOWN))
						.addAll(new JLabel(MESSAGES.getString("find_previous")), new JLabel(UP))
						.addAll(new JLabel(MESSAGES.getString("find_and_select_previous")), new JLabel(SHIFT + UP))
						.addAll(new JLabel(MESSAGES.getString("move_focus_to_table")), new JLabel("Esc"))
						.border(createTitledBorder(MESSAGES.getString("table_search_field")))
						.build();
	}

	private static JPanel dateTimeField() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("display_calendar")), new JLabel(INSERT))
						.addAll(new JLabel(MESSAGES.getString("increment_decrement")), new JLabel(UP_DOWN))
						.border(createTitledBorder(MESSAGES.getString("date_time_field")))
						.build();
	}

	private static JPanel calendar() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("previous_next_year")), new JLabel(CTRL + DOWN_UP))
						.addAll(new JLabel(MESSAGES.getString("previous_next_month")), new JLabel(SHIFT + DOWN_UP))
						.addAll(new JLabel(MESSAGES.getString("previous_next_day")), new JLabel(LEFT_RIGHT))
						.addAll(new JLabel(MESSAGES.getString("previous_next_week")), new JLabel(UP_DOWN))
						.addAll(new JLabel(MESSAGES.getString("previous_next_hour")), new JLabel(SHIFT + ALT + DOWN_UP))
						.addAll(new JLabel(MESSAGES.getString("previous_next_minute")), new JLabel(CTRL + ALT + DOWN_UP))
						.border(createTitledBorder(MESSAGES.getString("calendar")))
						.build();
	}

	private static JPanel entityField() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("add_new_item")), new JLabel(INSERT))
						.addAll(new JLabel(MESSAGES.getString("edit_selected_item")), new JLabel(CTRL + INSERT))
						.border(createTitledBorder(MESSAGES.getString("entity_field")))
						.build();
	}

	private static JPanel textFieldPanel() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("display_input_dialog")), new JLabel(INSERT))
						.border(createTitledBorder(MESSAGES.getString("text_field_panel")))
						.build();
	}

	private static JPanel dependencies() {
		return gridLayoutPanel(0, 2)
						.addAll(new JLabel(MESSAGES.getString("navigate_left_right")), new JLabel(CTRL + ALT + LEFT_RIGHT))
						.border(createTitledBorder(FrameworkMessages.dependencies()))
						.build();
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
}
