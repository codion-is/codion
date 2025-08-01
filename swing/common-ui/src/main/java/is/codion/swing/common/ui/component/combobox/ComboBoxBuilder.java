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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.property.PropertyValue;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;
import java.awt.event.ItemListener;

import static is.codion.common.Configuration.booleanValue;
import static java.util.Objects.requireNonNull;

/**
 * Builds a {@link JComboBox}.
 * @param <T> the value type
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ComboBoxBuilder<T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> extends ComponentBuilder<T, C, B> {

	/**
	 * Specifies whether mouse wheel scrolling is enabled in combo boxes by default.
	 * <ul>
	 * <li>Value type:Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> MOUSE_WHEEL_SCROLLING =
					booleanValue(ComboBoxBuilder.class.getName() + ".mouseWheelScrolling", true);

	/**
	 * @param editable specifies whether the combo box should be editable
	 * @return this builder instance
	 * @see JComboBox#setEditable(boolean)
	 */
	B editable(boolean editable);

	/**
	 * @param completionMode the completion mode
	 * @return this builder instance
	 */
	B completionMode(Completion.Mode completionMode);

	/**
	 * Specifies whether to normalize strings during auto-completion
	 * @param normalize true if strings should be normalized during autocomplete
	 * @return this builder instance
	 * @see #completionMode(Completion.Mode)
	 */
	B normalize(boolean normalize);

	/**
	 * @param renderer the renderer for the combo box
	 * @return this builder instance
	 * @see JComboBox#setRenderer(ListCellRenderer)
	 */
	B renderer(@Nullable ListCellRenderer<T> renderer);

	/**
	 * @param editor the editor for the combo box
	 * @return this builder instance
	 * @see JComboBox#setEditor(ComboBoxEditor)
	 */
	B editor(@Nullable ComboBoxEditor editor);

	/**
	 * Enable mouse wheel scrolling on the combo box
	 * @param mouseWheelScrolling true if mouse wheel scrolling should be enabled
	 * @return this builder instance
	 * @see #MOUSE_WHEEL_SCROLLING
	 */
	B mouseWheelScrolling(boolean mouseWheelScrolling);

	/**
	 * Enable mouse wheel scrolling on the combo box, with wrap around
	 * @param mouseWheelScrollingWithWrapAround true if mouse wheel scrolling with wrap around should be enabled
	 * @return this builder instance
	 */
	B mouseWheelScrollingWithWrapAround(boolean mouseWheelScrollingWithWrapAround);

	/**
	 * @param maximumRowCount the maximum row count before a scroll bar is shown
	 * @return this builder instance
	 * @see JComboBox#setMaximumRowCount(int)
	 */
	B maximumRowCount(int maximumRowCount);

	/**
	 * When a JComboBox is editable, and a long item is selected, the caret is moved to the end, hiding
	 * the start of the selected item. Enabling this moves the caret to the front on selection, thereby
	 * showing the start of the selected item.
	 * Note that this only works in conjunction with a {@link FilterComboBoxModel}.
	 * This is enabled by default.
	 * @param moveCaretToFrontOnSelection if true the caret is moved to the front of the editor component on selection,
	 * displaying the start of the selected item, instead of the end
	 * @return this builder instance
	 */
	B moveCaretToFrontOnSelection(boolean moveCaretToFrontOnSelection);

	/**
	 * Only applicable for the system and cross-platform Look and Feels.
	 * @param popupWidth a fixed popup width
	 * @return this builder instance
	 */
	B popupWidth(int popupWidth);

	/**
	 * @param itemListener the item listener
	 * @return this builder instance
	 * @see JComboBox#addItemListener(ItemListener)
	 */
	B itemListener(ItemListener itemListener);

	/**
	 * Provides a {@link ComboBoxBuilder}
	 */
	interface ModelStep {

		/**
		 * @param <T> the value type
		 * @param <C> the component type
		 * @param <B> the builder type
		 * @param comboBoxModel the combo box model
		 * @return a builder for a component
		 */
		<T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> model(ComboBoxModel<T> comboBoxModel);
	}

	/**
	 * @return a {@link ModelStep}
	 */
	static ModelStep builder() {
		return DefaultComboBoxBuilder.MODEL;
	}

	/**
	 * Enables mouse wheel selection for the given combo box
	 * @param comboBox the combo box
	 */
	static void enableMouseWheelSelection(JComboBox<?> comboBox) {
		requireNonNull(comboBox).addMouseWheelListener(new ComboBoxMouseWheelListener(comboBox, false));
	}

	/**
	 * Enables mouse wheel selection for the given combo box with wrap around
	 * @param comboBox the combo box
	 */
	static void enableMouseWheelSelectionWithWrapAround(JComboBox<?> comboBox) {
		comboBox.addMouseWheelListener(new ComboBoxMouseWheelListener(comboBox, true));
	}
}
