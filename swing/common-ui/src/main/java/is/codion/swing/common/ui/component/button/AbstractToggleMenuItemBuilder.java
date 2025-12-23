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

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.component.button.DefaultToggleButtonBuilder.SetComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

abstract class AbstractToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends AbstractButtonBuilder<C, Boolean, B>
				implements ToggleMenuItemBuilder<C, B> {

	private final List<ObservableState> linkedObservableStates = new ArrayList<>(1);
	private PersistMenu persistMenu = PERSIST_MENU.getOrThrow();

	AbstractToggleMenuItemBuilder() {
		horizontalAlignment(SwingConstants.LEADING);
	}

	@Override
	public final B toggle(ToggleControl toggleControl) {
		if (requireNonNull(toggleControl).value().isNullable()) {
			throw new IllegalArgumentException("A toggle menu item does not support a nullable value");
		}
		link(toggleControl.value());
		action(toggleControl);
		return self();
	}

	@Override
	public final B toggle(Supplier<ToggleControl> toggleControl) {
		return toggle(requireNonNull(toggleControl).get());
	}

	@Override
	public final B persistMenu(PersistMenu persistMenu) {
		this.persistMenu = requireNonNull(persistMenu);
		return self();
	}

	@Override
	public final B link(State linkedState) {
		link(requireNonNull(linkedState).value());
		return (B) this;
	}

	@Override
	public final B link(ObservableState linkedState) {
		linkedObservableStates.add(requireNonNull(linkedState));
		return (B) this;
	}

	protected abstract JMenuItem createMenuItem(PersistMenu persistMenu);

	@Override
	protected final C createButton() {
		return (C) createMenuItem(persistMenu);
	}

	@Override
	protected final ComponentValue<C, Boolean> createValue(C component) {
		ToggleButtonValue<C> componentValue = new ToggleButtonValue<>(component);
		linkedObservableStates.forEach(state -> state.addConsumer(new SetComponentValue(componentValue)));

		return componentValue;
	}

	@Override
	protected final boolean supportsNull() {
		return false;
	}
}
