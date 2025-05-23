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
package is.codion.swing.common.ui.dialog;

import is.codion.common.item.Item;
import is.codion.common.resource.MessageBundle;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEmptyBorder;

final class DefaultFontSizeSelectionDialogBuilder implements FontSizeSelectionDialogBuilder {

	private JComponent owner;
	private int initialSelection = 100;

	@Override
	public FontSizeSelectionDialogBuilder owner(JComponent owner) {
		this.owner = requireNonNull(owner);
		return this;
	}

	@Override
	public Control createControl(Consumer<Integer> selectedFontSize) {
		requireNonNull(selectedFontSize);
		MessageBundle resourceBundle =
						messageBundle(DefaultFontSizeSelectionDialogBuilder.class,
										getBundle(DefaultFontSizeSelectionDialogBuilder.class.getName()));
		String caption = resourceBundle.getString("select_font_size");

		return Control.builder()
						.command(() -> selectFontSize(selectedFontSize))
						.caption(caption)
						.build();
	}

	@Override
	public FontSizeSelectionDialogBuilder initialSelection(int initialSelection) {
		this.initialSelection = initialSelection;
		return this;
	}

	@Override
	public void selectFontSize(Consumer<Integer> selectedFontSize) {
		requireNonNull(selectedFontSize);
		MessageBundle resourceBundle =
						messageBundle(DefaultFileSelectionDialogBuilder.class,
										getBundle(DefaultFontSizeSelectionDialogBuilder.class.getName()));
		FontSizeSelectionPanel fontSizeSelectionPanel = new FontSizeSelectionPanel(initialSelection);
		new DefaultOkCancelDialogBuilder(fontSizeSelectionPanel)
						.owner(owner)
						.title(resourceBundle.getString("select_font_size"))
						.onOk(() -> selectedFontSize.accept(fontSizeSelectionPanel.selectedFontSize()))
						.show();
	}

	private static final class FontSizeSelectionPanel extends JPanel {

		private final ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue;

		private FontSizeSelectionPanel(int initialFontSize) {
			super(Layouts.borderLayout());
			List<Item<Integer>> values = initializeValues();
			componentValue = ItemComboBoxBuilder.builder(values)
							.value(initialFontSize)
							.renderer(new FontSizeCellRenderer(values, initialFontSize))
							.buildValue();
			add(componentValue.component(), BorderLayout.CENTER);
			setBorder(createEmptyBorder(10, 10, 0, 10));
		}

		private int selectedFontSize() {
			return componentValue.getOrThrow();
		}

		private static List<Item<Integer>> initializeValues() {
			List<Item<Integer>> values = new ArrayList<>();
			for (int i = 50; i <= 200; i += 5) {
				values.add(Item.item(i, i + "%"));
			}

			return values;
		}

		private static final class FontSizeCellRenderer implements ListCellRenderer<Item<Integer>> {

			private final DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
			private final List<Item<Integer>> values;
			private final Integer currentFontSize;

			private FontSizeCellRenderer(List<Item<Integer>> values, Integer currentFontSize) {
				this.values = values;
				this.currentFontSize = currentFontSize;
			}

			@Override
			public Component getListCellRendererComponent(JList<? extends Item<Integer>> list, Item<Integer> value, int index,
																										boolean isSelected, boolean cellHasFocus) {
				Component component = defaultListCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (index >= 0) {
					Font font = component.getFont();
					int newSize = Math.round(font.getSize() * (values.get(index).value() / (float) currentFontSize.doubleValue()));
					component.setFont(new Font(font.getName(), font.getStyle(), newSize));
				}

				return component;
			}
		}
	}
}
