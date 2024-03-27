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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;
import java.awt.event.ItemListener;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Builds a item combo box.
 * @param <T> the value type
 */
public interface ItemComboBoxBuilder<T> extends ComponentBuilder<T, JComboBox<Item<T>>, ItemComboBoxBuilder<T>> {

	/**
	 * @param nullable true if a null value should be added to the model if missing
	 * @return this builder instance
	 */
	ItemComboBoxBuilder<T> nullable(boolean nullable);

	/**
	 * Sorts the items by caption
	 * @param sorted if true then the items will be sorted by caption
	 * @return this builder instance
	 */
	ItemComboBoxBuilder<T> sorted(boolean sorted);

	/**
	 * @param comparator if specified the combo box items are sorted using this comparator
	 * @return this builder instance
	 */
	ItemComboBoxBuilder<T> comparator(Comparator<Item<T>> comparator);

	/**
	 * @param completionMode the completion mode
	 * @return this builder instance
	 */
	ItemComboBoxBuilder<T> completionMode(Completion.Mode completionMode);

	/**
	 * Enable mouse wheel scrolling on the combo box
	 * @param mouseWheelScrolling true if mouse wheel scrolling should be enabled
	 * @return this builder instance
	 */
	ItemComboBoxBuilder<T> mouseWheelScrolling(boolean mouseWheelScrolling);

	/**
	 * Enable mouse wheel scrolling on the combo box, with wrap around
	 * @param mouseWheelScrollingWithWrapAround true if mouse wheel scrolling with wrap around should be enabled
	 * @return this builder instance
	 */
	ItemComboBoxBuilder<T> mouseWheelScrollingWithWrapAround(boolean mouseWheelScrollingWithWrapAround);

	/**
	 * @param maximumRowCount the maximum row count
	 * @return this builder instance
	 */
	ItemComboBoxBuilder<T> maximumRowCount(int maximumRowCount);

	/**
	 * Only used for the system and cross-platform Look and Feels.
	 * @param popupWidth a fixed popup width
	 * @return this builder instance
	 */
	ItemComboBoxBuilder<T> popupWidth(int popupWidth);

	/**
	 * @param renderer the renderer for the combo box
	 * @return this builder instance
	 * @see JComboBox#setRenderer(ListCellRenderer)
	 */
	ItemComboBoxBuilder<T> renderer(ListCellRenderer<Item<T>> renderer);

	/**
	 * @param editor the editor for the combo box
	 * @return this builder instance
	 * @see JComboBox#setEditor(ComboBoxEditor)
	 */
	ItemComboBoxBuilder<T> editor(ComboBoxEditor editor);

	/**
	 * @param itemListener the item listener
	 * @return this builder instance
	 * @see JComboBox#addItemListener(ItemListener)
	 */
	ItemComboBoxBuilder<T> itemListener(ItemListener itemListener);

	/**
	 * @param comboBoxModel the combo box model
	 * @param <T> the value type
	 * @return a builder for a component
	 */
	static <T> ItemComboBoxBuilder<T> builder(ItemComboBoxModel<T> comboBoxModel) {
		return new DefaultItemComboBoxBuilder<>(comboBoxModel, null);
	}

	/**
	 * @param comboBoxModel the combo box model
	 * @param linkedValue the value to link to the component
	 * @param <T> the value type
	 * @return a builder for a component
	 */
	static <T> ItemComboBoxBuilder<T> builder(ItemComboBoxModel<T> comboBoxModel,
																						Value<T> linkedValue) {
		return new DefaultItemComboBoxBuilder<>(comboBoxModel, requireNonNull(linkedValue));
	}

	/**
	 * @param values the values
	 * @param <T> the value type
	 * @return a builder for a component
	 */
	static <T> ItemComboBoxBuilder<T> builder(List<Item<T>> values) {
		return new DefaultItemComboBoxBuilder<>(values, null);
	}

	/**
	 * @param values the values
	 * @param linkedValue the value to link to the component
	 * @param <T> the value type
	 * @return a builder for a component
	 */
	static <T> ItemComboBoxBuilder<T> builder(List<Item<T>> values, Value<T> linkedValue) {
		return new DefaultItemComboBoxBuilder<>(values, requireNonNull(linkedValue));
	}
}
