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

import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;

import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

abstract class AbstractToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends AbstractButtonBuilder<Boolean, C, B>
				implements ToggleMenuItemBuilder<C, B> {

	private @Nullable ToggleControl toggleControl;
	private PersistMenu persistMenu = PERSIST_MENU.getOrThrow();

	AbstractToggleMenuItemBuilder() {
		value(false);
		horizontalAlignment(SwingConstants.LEADING);
	}

	@Override
	public final B toggleControl(ToggleControl toggleControl) {
		if (requireNonNull(toggleControl).value().isNullable()) {
			throw new IllegalArgumentException("A toggle menu item does not support a nullable value");
		}
		this.toggleControl = toggleControl;
		value(toggleControl.value().get());
		action(toggleControl);
		return self();
	}

	@Override
	public final B toggleControl(Control.Builder<ToggleControl, ?> toggleControlBuilder) {
		return toggleControl(requireNonNull(toggleControlBuilder).build());
	}

	@Override
	public final B persistMenu(PersistMenu persistMenu) {
		this.persistMenu = requireNonNull(persistMenu);
		return self();
	}

	protected abstract JMenuItem createMenuItem(PersistMenu persistMenu);

	@Override
	protected final C createButton() {
		JMenuItem menuItem = createMenuItem(persistMenu);
		if (toggleControl != null) {
			menuItem.setModel(createButtonModel(toggleControl));
		}

		return (C) menuItem;
	}

	@Override
	protected final ComponentValue<Boolean, C> createComponentValue(C component) {
		return new BooleanToggleButtonValue<>(component);
	}

	@Override
	protected final boolean supportsNull() {
		return false;
	}
}
