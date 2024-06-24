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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.keybinding;

import is.codion.framework.demos.manual.keybinding.KeyBindingModel.KeyBinding;
import is.codion.framework.demos.manual.keybinding.KeyBindingModel.KeyBindingColumns.Id;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;

import static is.codion.framework.demos.manual.keybinding.KeyBindingModel.KeyBindingColumns.Id.*;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldHeight;
import static is.codion.swing.common.ui.laf.LookAndFeelComboBox.lookAndFeelComboBox;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Arrays.asList;

/**
 * A utility for displaying component action/input maps for installed look and feels.<br>
 * Based on <a href="https://tips4java.wordpress.com/2008/10/10/key-bindings/">KeyBindings.java by Rob Comick</a>
 * @author Rob Camick
 * @author bjorndarri
 */
public final class KeyBindingPanel extends JPanel {

	private final LookAndFeelComboBox lookAndFeelComboBox = lookAndFeelComboBox(true);
	private final KeyBindingModel keyBindingModel;
	private final FilterTable<KeyBinding, Id> table;
	private final JComboBox<String> componentComboBox;

	public KeyBindingPanel() {
		super(borderLayout());
		this.keyBindingModel = new KeyBindingModel(lookAndFeelComboBox.getModel());
		this.table = FilterTable.builder(keyBindingModel.tableModel(), createColumns()).build();
		this.componentComboBox = comboBox(keyBindingModel.componentComboBoxModel())
						.preferredHeight(preferredTextFieldHeight())
						.preferredWidth(200)
						.build();
		setBorder(emptyBorder());
		add(flexibleGridLayoutPanel(1, 4)
						.add(label("Look & Feel")
										.horizontalAlignment(SwingConstants.RIGHT)
										.preferredWidth(100)
										.build())
						.add(lookAndFeelComboBox)
						.add(label("Component")
										.horizontalAlignment(SwingConstants.RIGHT)
										.preferredWidth(100)
										.build())
						.add(componentComboBox)
						.build(), BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(table.searchField(), BorderLayout.SOUTH);
	}

	private static List<FilterTableColumn<Id>> createColumns() {
		FilterTableColumn<Id> action = FilterTableColumn.builder(ACTION_COLUMN)
						.headerValue("Action")
						.build();
		FilterTableColumn<Id> whenFocused = FilterTableColumn.builder(WHEN_FOCUSED_COLUMN)
						.headerValue("When Focused")
						.build();
		FilterTableColumn<Id> whenInFocusedWindow = FilterTableColumn.builder(WHEN_IN_FOCUSED_WINDOW_COLUMN)
						.headerValue("When in Focused Window")
						.build();
		FilterTableColumn<Id> whenAncestor = FilterTableColumn.builder(WHEN_ANCESTOR_COLUMN)
						.headerValue("When Ancestor")
						.build();

		return asList(action, whenFocused, whenInFocusedWindow, whenAncestor);
	}

	public static void main(String[] args) {
		System.setProperty("sun.awt.disablegrab", "true");
		Arrays.stream(FlatAllIJThemes.INFOS)
						.forEach(LookAndFeelProvider::addLookAndFeel);
		SwingUtilities.invokeLater(() -> Windows.frame(new KeyBindingPanel())
						.title("Key Bindings")
						.defaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
						.centerFrame(true)
						.show());
	}
}
