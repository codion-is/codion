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
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

class DefaultMenuItemBuilder<C extends JMenuItem, B extends MenuItemBuilder<C, B>> extends AbstractButtonBuilder<C, Void, B>
				implements MenuItemBuilder<C, B> {

	DefaultMenuItemBuilder() {
		horizontalAlignment(SwingConstants.LEADING);
	}

	@Override
	protected C createButton() {
		return (C) new JMenuItem();
	}

	@Override
	protected final ComponentValue<C, Void> createValue(C component) {
		return new MenuItemComponentValue<>(component);
	}

	private static final class MenuItemComponentValue<C extends JMenuItem> extends AbstractComponentValue<C, Void> {

		private MenuItemComponentValue(C component) {
			super(component);
		}

		@Override
		protected Void getComponentValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setComponentValue(Void value) {
			throw new UnsupportedOperationException();
		}
	}
}
