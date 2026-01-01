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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.listbox;

import is.codion.common.reactive.value.ValueSet;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.util.Set;
import java.util.function.Function;

/**
 * Creates a {@link JComboBox} based {@link ComponentValue} instance, represented by the items
 * in the combo box (as opposed to the selected item)
 * @param <T> the value type
 */
public interface ListBoxBuilder<T> extends ComponentValueBuilder<JComboBox<T>, Set<T>, ListBoxBuilder<T>> {

	/**
	 * @param formatter formats an item for display in the list
	 * @return this builder instance
	 */
	ListBoxBuilder<T> formatter(Function<Object, String> formatter);

	/**
	 * Provides a {@link LinkedValueBuilder}
	 */
	interface ItemValueStep {

		/**
		 * @param itemValue supplies new items to add to the list box.
		 * @param <T> the item type
		 * @return a {@link LinkedValueBuilder}
		 */
		<T> ListBoxBuilder.LinkedValueBuilder<T> itemValue(ComponentValue<? extends JComponent, T> itemValue);
	}

	/**
	 * Provides a {@link ListBoxBuilder}
	 * @param <T> the item type
	 */
	interface LinkedValueBuilder<T> {

		/**
		 * @param linkedValue the linked value
		 * @return a {@link ListBoxBuilder}
		 */
		ListBoxBuilder<T> linkedValue(ValueSet<T> linkedValue);
	}

	/**
	 * Creates a {@link JComboBox} based {@link ComponentValue} instance, represented by the items
	 * in the combo box (as opposed to the selected item). The provided {@code itemValue} supplies
	 * new items to add to the combo box.
	 * <ul>
	 * <li>{@link java.awt.event.KeyEvent#VK_INSERT} adds the current value whereas
	 * <li>{@link java.awt.event.KeyEvent#VK_DELETE} deletes the selected item from the list.
	 * </ul>
	 * @return a new {@link ComponentValue}
	 */
	static ItemValueStep builder() {
		return DefaultListBoxBuilder.ITEM;
	}
}
