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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.keybinding;

import is.codion.manual.keybinding.KeyBindingModel.KeyBindingColumns.ColumnId;
import is.codion.manual.keybinding.KeyBindingModel.KeyBindingRow;
import is.codion.plugin.flatlaf.intellij.themes.monokaipro.MonokaiPro;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.frame.Frames;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.util.List;

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

/**
 * A utility for displaying component action/input maps for installed look and feels.<br>
 * Based on <a href="https://tips4java.wordpress.com/2008/10/10/key-bindings/">KeyBindings.java by Rob Comick</a>
 * @author Rob Camick
 * @author bjorndarri
 */
public final class KeyBindingPanel extends JPanel {

	private final LookAndFeelComboBox lookAndFeelComboBox = LookAndFeelComboBox.builder().build();
	private final KeyBindingModel keyBindingModel;
	private final FilterTable<KeyBindingRow, ColumnId> table;
	private final JComboBox<String> componentComboBox;

	public KeyBindingPanel() {
		super(borderLayout());
		this.keyBindingModel = new KeyBindingModel(lookAndFeelComboBox.getModel());
		this.table = FilterTable.builder(keyBindingModel.tableModel(), createColumns())
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.build();
		this.componentComboBox = comboBox(keyBindingModel.componentModel())
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

	private static List<FilterTableColumn<ColumnId>> createColumns() {
		return List.of(FilterTableColumn.builder(ColumnId.ACTION)
										.headerValue("Action")
										.build(),
						FilterTableColumn.builder(ColumnId.WHEN_FOCUSED)
										.headerValue("When Focused")
										.build(),
						FilterTableColumn.builder(ColumnId.WHEN_IN_FOCUSED_WINDOW)
										.headerValue("When in Focused Window")
										.build(),
						FilterTableColumn.builder(ColumnId.WHEN_ANCESTOR)
										.headerValue("When Ancestor")
										.build());
	}

	public static void main(String[] args) {
		System.setProperty("sun.awt.disablegrab", "true");
		findLookAndFeel(MonokaiPro.class)
						.ifPresent(LookAndFeelEnabler::enable);
		SwingUtilities.invokeLater(() -> Frames.builder(new KeyBindingPanel())
						.title("Key Bindings")
						.defaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
						.centerFrame(true)
						.show());
	}
}
