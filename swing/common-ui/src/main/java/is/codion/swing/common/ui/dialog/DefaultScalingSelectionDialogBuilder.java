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
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.scaler.Scaler;

import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

final class DefaultScalingSelectionDialogBuilder implements ScalingSelectionDialogBuilder {

	private @Nullable JComponent owner;

	@Override
	public ScalingSelectionDialogBuilder owner(@Nullable JComponent owner) {
		this.owner = owner;
		return this;
	}

	@Override
	public int show(String title) {
		List<Item<Integer>> values = initializeValues();
		ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue = ItemComboBoxBuilder.builder()
						.items(values)
						.value(Scaler.SCALING.getOrThrow())
						.renderer(new FontSizeCellRenderer(values, Scaler.SCALING.getOrThrow()))
						.buildValue();

		return new DefaultInputDialogBuilder<Integer>(componentValue)
						.owner(owner)
						.title(title)
						.show();
	}

		private static List<Item<Integer>> initializeValues() {
			List<Item<Integer>> values = new ArrayList<>();
			for (int i = 50; i <= 300; i += 5) {
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
					int newSize = Math.round(font.getSize() * (values.get(index).getOrThrow() / (float) currentFontSize.doubleValue()));
					component.setFont(new Font(font.getName(), font.getStyle(), newSize));
				}

				return component;
			}
		}
}
