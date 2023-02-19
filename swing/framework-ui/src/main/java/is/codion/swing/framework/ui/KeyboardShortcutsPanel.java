/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.i18n.FrameworkMessages;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.panel;
import static is.codion.swing.common.ui.layout.Layouts.*;
import static javax.swing.BorderFactory.createTitledBorder;

final class KeyboardShortcutsPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(KeyboardShortcutsPanel.class.getName());

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
    panel(flexibleGridLayout(0, 1))
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
            .add(textInput())
            .scrollPane()
            .verticalUnitIncrement(VERTICAL_UNIT_INCREMENT)
            .build(this::add);
  }

  private static JPanel navigation() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("navigate_up_down")), new JLabel(CTRL + ALT + UP_DOWN))
            .addAll(new JLabel(MESSAGES.getString("navigate_left_right")), new JLabel(CTRL + ALT + LEFT_RIGHT))
            .border(createTitledBorder(MESSAGES.getString("navigation")))
            .build();
  }

  private static JPanel resizing() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("resize_left_right")), new JLabel(SHIFT + ALT + LEFT_RIGHT))
            .addAll(new JLabel(MESSAGES.getString("toggle_edit_panel")), new JLabel(SHIFT + ALT + UP_DOWN))
            .border(createTitledBorder(MESSAGES.getString("resizing")))
            .build();
  }

  private static JPanel focusTransferral() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("transfer_focus_edit_panel")), new JLabel(CTRL + "E"))
            .addAll(new JLabel(MESSAGES.getString("transfer_focus_table")), new JLabel(CTRL + "T"))
            .addAll(new JLabel(MESSAGES.getString("transfer_focus_input_field")), new JLabel(CTRL + "I"))
            .addAll(new JLabel(MESSAGES.getString("transfer_focus_search_field")), new JLabel(CTRL + "S"))
            .addAll(new JLabel(MESSAGES.getString("transfer_focus_find_in_table")), new JLabel(CTRL + "F"))
            .border(createTitledBorder(MESSAGES.getString("transfer_focus")))
            .build();
  }

  private static JPanel editPanel() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("transfer_focus_to_next_input_field")), new JLabel(ENTER))
            .addAll(new JLabel(MESSAGES.getString("transfer_focus_to_previous_input_field")), new JLabel(SHIFT + ENTER))
            .addAll(new JLabel(MESSAGES.getString("save")), new JLabel(ALT + FrameworkMessages.saveMnemonic()))
            .addAll(new JLabel(MESSAGES.getString("add")), new JLabel(ALT + FrameworkMessages.addMnemonic()))
            .addAll(new JLabel(MESSAGES.getString("update")), new JLabel(ALT + FrameworkMessages.updateMnemonic()))
            .addAll(new JLabel(MESSAGES.getString("delete")), new JLabel(ALT + FrameworkMessages.deleteMnemonic()))
            .addAll(new JLabel(MESSAGES.getString("clear")), new JLabel(ALT + FrameworkMessages.clearMnemonic()))
            .addAll(new JLabel(MESSAGES.getString("refresh")), new JLabel(ALT + FrameworkMessages.refreshMnemonic()))
            .border(createTitledBorder(MESSAGES.getString("edit_panel")))
            .build();
  }

  private static JPanel tablePanel() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("delete_selected")), new JLabel("Delete"))
            .addAll(new JLabel(MESSAGES.getString("copy_selected_rows")), new JLabel(CTRL + "C"))
            .addAll(new JLabel(MESSAGES.getString("copy_selected_cell")), new JLabel(CTRL + ALT + "C"))
            .addAll(new JLabel(MESSAGES.getString("move_selected_column")), new JLabel(CTRL + SHIFT + LEFT_RIGHT))
            .addAll(new JLabel(MESSAGES.getString("resize_selected_column")), new JLabel(CTRL + "+/-"))
            .addAll(new JLabel(MESSAGES.getString("show_popup_menu")), new JLabel(CTRL + "G"))
            .addAll(new JLabel(MESSAGES.getString("refresh_button")), new JLabel("F5"))
            .border(createTitledBorder(MESSAGES.getString("table_panel")))
            .build();
  }

  private static JPanel conditionPanel() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("refresh_table_data")), new JLabel(ENTER))
            .border(createTitledBorder(MESSAGES.getString("condition_panel")))
            .build();
  }

  private static JPanel searchField() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("find_next")), new JLabel(ENTER + OR + UP))
            .addAll(new JLabel(MESSAGES.getString("find_and_select_next")), new JLabel(SHIFT + ENTER + OR + UP))
            .addAll(new JLabel(MESSAGES.getString("find_previous")), new JLabel(DOWN))
            .addAll(new JLabel(MESSAGES.getString("find_and_select_previous")), new JLabel(SHIFT + DOWN))
            .addAll(new JLabel(MESSAGES.getString("move_focus_to_table")), new JLabel("Esc"))
            .border(createTitledBorder(MESSAGES.getString("table_search_field")))
            .build();
  }

  private static JPanel dateTimeField() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("display_calendar")), new JLabel(INSERT))
            .border(createTitledBorder(MESSAGES.getString("date_time_field")))
            .build();
  }

  private static JPanel calendar() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("previous_next_year")), new JLabel(CTRL + LEFT_RIGHT + OR + DOWN_UP))
            .addAll(new JLabel(MESSAGES.getString("previous_next_month")), new JLabel(SHIFT + LEFT_RIGHT + OR + DOWN_UP))
            .addAll(new JLabel(MESSAGES.getString("previous_next_day")), new JLabel(ALT + LEFT_RIGHT))
            .addAll(new JLabel(MESSAGES.getString("previous_next_week")), new JLabel(ALT + UP_DOWN))
            .addAll(new JLabel(MESSAGES.getString("previous_next_hour")), new JLabel(SHIFT + ALT + LEFT_RIGHT + OR + DOWN_UP))
            .addAll(new JLabel(MESSAGES.getString("previous_next_minute")), new JLabel(CTRL + ALT + LEFT_RIGHT + OR + DOWN_UP))
            .border(createTitledBorder(MESSAGES.getString("calendar")))
            .build();
  }

  private static JPanel entityField() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("add_new_item")), new JLabel(INSERT))
            .border(createTitledBorder(MESSAGES.getString("entity_field")))
            .build();
  }

  private static JPanel textInput() {
    return panel(gridLayout(0, 2))
            .addAll(new JLabel(MESSAGES.getString("display_input_dialog")), new JLabel(INSERT))
            .border(createTitledBorder(MESSAGES.getString("text_input_panel")))
            .build();
  }
}
