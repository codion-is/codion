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
package is.codion.swing.framework.ui;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

abstract class DefaultControlConfig<T extends Enum<T>, C extends ControlConfig<T, C>>
				implements ControlConfig<T, C> {

	private static final MenuItem SEPARATOR = new Separator();

	private final List<T> defaults;
	private final T additionalControls;
	private final List<MenuItem> items = new ArrayList<>();
	private final Additional additional = new Additional();

	protected DefaultControlConfig(T additionalControls, List<T> defaults) {
		this.defaults = defaults;
		this.additionalControls = additionalControls;
		defaults();
	}

	@Override
	public final C separator() {
		if (items.isEmpty() || items.get(items.size() - 1) != SEPARATOR) {
			items.add(SEPARATOR);
		}
		return (C) this;
	}

	@Override
	public final C standard(T control) {
		add(requireNonNull(control));
		return (C) this;
	}

	@Override
	public final C control(Control control) {
		items.add(new CustomControl(requireNonNull(control)));
		return (C) this;
	}

	@Override
	public final C clear() {
		items.clear();
		return (C) this;
	}

	@Override
	public final C defaults() {
		return defaults(null);
	}

	@Override
	public final C defaults(T stopBefore) {
		for (T control : defaults) {
			if (stopBefore != null && control == stopBefore) {
				return (C) this;
			}
			if (control == null) {
				separator();
			}
			else if (control == additionalControls && !items.contains(additional)) {
				items.add(additional);
			}
			else {
				add(control);
			}
		}

		return (C) this;
	}

	@Override
	public final Controls createControls() {
		Controls created = Controls.controls();
		items.forEach(item -> item.addTo(created));

		return created;
	}

	protected abstract Optional<Control> control(T control);

	private void add(T tableControl) {
		StandardControl<T> standardControl = new StandardControl<>(tableControl, this::control);
		if (!items.contains(standardControl)) {
			items.add(standardControl);
		}
	}

	private interface MenuItem {
		void addTo(Controls popupControls);
	}

	private final class Additional implements MenuItem {

		@Override
		public void addTo(Controls popupControls) {
			control(additionalControls)
							.map(Controls.class::cast)
							.ifPresent(controlsToAdd -> add(controlsToAdd, popupControls));
		}

		private void add(Controls controls, Controls popupControls) {
			controls.actions().forEach(action -> {
				if (action == null) {
					popupControls.addSeparator();
				}
				else {
					popupControls.add(action);
				}
			});
			if (controls.notEmpty()) {
				popupControls.addSeparator();
			}
		}
	}

	private static final class StandardControl<T> implements MenuItem {

		private final T tableControl;
		private final Function<T, Optional<Control>> controls;

		private StandardControl(T tableControl, Function<T, Optional<Control>> controls) {
			this.tableControl = tableControl;
			this.controls = controls;
		}

		@Override
		public void addTo(Controls popupControls) {
			controls.apply(tableControl).ifPresent(control -> {
				if (control instanceof Controls) {
					if (((Controls) control).notEmpty()) {
						popupControls.add(control);
					}
				}
				else if (control != null) {
					popupControls.add(control);
				}
			});
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof StandardControl)) {
				return false;
			}
			StandardControl<?> that = (StandardControl<?>) object;
			return tableControl == that.tableControl;
		}

		@Override
		public int hashCode() {
			return Objects.hash(tableControl);
		}
	}

	private static final class CustomControl implements MenuItem {

		private final Control control;

		private CustomControl(Control control) {
			this.control = control;
		}

		@Override
		public void addTo(Controls popupControls) {
			popupControls.add(control);
		}
	}

	private static final class Separator implements MenuItem {

		@Override
		public void addTo(Controls popupControls) {
			List<Action> actions = popupControls.actions();
			if (actions.isEmpty() || actions.get(actions.size() - 1) != Controls.SEPARATOR) {
				popupControls.addSeparator();
			}
		}
	}
}
