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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

final class DefaultMenuItemBuilder<B extends MenuItemBuilder<B>> extends AbstractButtonBuilder<JMenuItem, Void, B>
				implements MenuItemBuilder<B> {

	DefaultMenuItemBuilder() {
		horizontalAlignment(SwingConstants.LEADING);
	}

	@Override
	protected JMenuItem createButton() {
		return new JMenuItem();
	}

	@Override
	protected ComponentValue<JMenuItem, Void> createValue(JMenuItem component) {
		return new MenuItemComponentValue(component);
	}

	private static final class MenuItemComponentValue extends AbstractComponentValue<JMenuItem, Void> {

		private MenuItemComponentValue(JMenuItem component) {
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
