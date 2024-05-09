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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.listbox;

import is.codion.common.value.ValueSet;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
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
public interface ListBoxBuilder<T> extends ComponentBuilder<Set<T>, JComboBox<T>, ListBoxBuilder<T>> {

	/**
	 * @param string provides a String to display in the list for a given value, formatted or otherwise
	 * @return this builder instance
	 */
	ListBoxBuilder<T> string(Function<Object, String> string);

	/**
	 * Creates a {@link JComboBox} based {@link ComponentValue} instance, represented by the items
	 * in the combo box (as opposed to the selected item). The provided {@code itemValue} supplies
	 * new items to add to the combo box.<br>
	 * {@link java.awt.event.KeyEvent#VK_INSERT} adds the current value whereas
	 * {@link java.awt.event.KeyEvent#VK_DELETE} deletes the selected item from the list.
	 * @param itemValue the component value providing the items to add
	 * @param linkedValue the value to link
	 * @param <T> the value type
	 * @return a new {@link ComponentValue}
	 */
	static <T> ListBoxBuilder<T> listBox(ComponentValue<T, ? extends JComponent> itemValue,
																			 ValueSet<T> linkedValue) {
		return new DefaultListBoxBuilder<>(itemValue, linkedValue);
	}
}
