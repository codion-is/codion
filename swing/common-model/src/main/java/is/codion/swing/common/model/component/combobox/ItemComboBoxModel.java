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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;

import java.util.List;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * @see #booleanItems()
 * @see #booleanItems(String)
 * @see #booleanItems(String, String, String)
 */
public final class ItemComboBoxModel {

	private ItemComboBoxModel() {}

	/**
	 * @return items for null, true and false, using the default captions
	 * @see FilterComboBoxModel#NULL_CAPTION
	 * @see Messages#yes()
	 * @see Messages#no()
	 */
	public static List<Item<Boolean>> booleanItems() {
		return booleanItems(FilterComboBoxModel.NULL_CAPTION.get());
	}

	/**
	 * @param nullCaption the caption for the null value
	 * @return items for null, true and false, using the given null caption and the default true/false captions
	 * @see Messages#yes()
	 * @see Messages#no()
	 */
	public static List<Item<Boolean>> booleanItems(String nullCaption) {
		return booleanItems(nullCaption, Messages.yes(), Messages.no());
	}

	/**
	 * @param nullCaption the caption for null
	 * @param trueCaption the caption for true
	 * @param falseCaption the caption for false
	 * @return items for null, true and false
	 */
	public static List<Item<Boolean>> booleanItems(String nullCaption, String trueCaption, String falseCaption) {
		return asList(item(null, requireNonNull(nullCaption)), item(true, requireNonNull(trueCaption)), item(false, requireNonNull(falseCaption)));
	}
}
