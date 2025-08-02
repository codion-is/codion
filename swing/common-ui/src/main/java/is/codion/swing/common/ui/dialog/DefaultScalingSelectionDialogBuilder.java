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

import org.jspecify.annotations.Nullable;

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

final class DefaultScalingSelectionDialogBuilder implements ScalingSelectionDialogBuilder {

	private @Nullable JComponent owner;
	private int initialSelection = 100;

	@Override
	public ScalingSelectionDialogBuilder owner(@Nullable JComponent owner) {
		this.owner = owner;
		return this;
	}

	@Override
	public Control createControl(Consumer<Integer> scalingSelected) {
		requireNonNull(scalingSelected);
		MessageBundle resourceBundle =
						messageBundle(DefaultScalingSelectionDialogBuilder.class,
										getBundle(DefaultScalingSelectionDialogBuilder.class.getName()));
		String caption = resourceBundle.getString("scaling");

		return Control.builder()
						.command(() -> selectScaling(scalingSelected))
						.caption(caption)
						.build();
	}

	@Override
	public ScalingSelectionDialogBuilder initialSelection(int initialSelection) {
		this.initialSelection = initialSelection;
		return this;
	}

	@Override
	public void selectScaling(Consumer<Integer> scalingSelected) {
		requireNonNull(scalingSelected);
		MessageBundle resourceBundle =
						messageBundle(DefaultFileSelectionDialogBuilder.class,
										getBundle(DefaultScalingSelectionDialogBuilder.class.getName()));
		ScalingSelectionPanel scalingSelectionPanel = new ScalingSelectionPanel(initialSelection);
		new DefaultOkCancelDialogBuilder()
						.component(scalingSelectionPanel)
						.owner(owner)
						.title(resourceBundle.getString("scaling"))
						.onOk(() -> scalingSelected.accept(scalingSelectionPanel.selectedScaling()))
						.show();
	}

	private static final class ScalingSelectionPanel extends JPanel {

		private final ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue;

		private ScalingSelectionPanel(int initialScaling) {
			super(Layouts.borderLayout());
			List<Item<Integer>> values = initializeValues();
			componentValue = ItemComboBoxBuilder.builder()
							.items(values)
							.value(initialScaling)
							.renderer(new FontSizeCellRenderer(values, initialScaling))
							.buildValue();
			add(componentValue.component(), BorderLayout.CENTER);
			setBorder(createEmptyBorder(10, 10, 0, 10));
		}

		private int selectedScaling() {
			return componentValue.getOrThrow();
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
}
