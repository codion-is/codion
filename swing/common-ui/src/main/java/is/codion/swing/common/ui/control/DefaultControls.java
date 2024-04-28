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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static is.codion.common.Text.nullOrEmpty;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultControls extends AbstractControl implements Controls {

	private static final String CONTROLS_PARAMETER = "controls";

	private final List<Action> actions = new ArrayList<>();

	DefaultControls(String name, StateObserver enabledState, List<Action> controls) {
		super(name, enabledState);
		controls.forEach(this::add);
	}

	@Override
	public List<Action> actions() {
		return unmodifiableList(actions);
	}

	@Override
	public Controls add(Action action) {
		actions.add(requireNonNull(action, "action"));
		return this;
	}

	@Override
	public Controls addAt(int index, Action action) {
		actions.add(index, requireNonNull(action, "action"));
		return this;
	}

	@Override
	public Controls remove(Action action) {
		actions.remove(action);
		return this;
	}

	@Override
	public Controls removeAll() {
		actions.clear();
		return this;
	}

	@Override
	public int size() {
		return actions.size();
	}

	@Override
	public boolean empty() {
		return actions.stream().noneMatch(action -> action != SEPARATOR);
	}

	@Override
	public boolean notEmpty() {
		return !empty();
	}

	@Override
	public Action get(int index) {
		return actions.get(index);
	}

	@Override
	public Controls add(Controls controls) {
		actions.add(requireNonNull(controls, CONTROLS_PARAMETER));
		return this;
	}

	@Override
	public Controls addAt(int index, Controls controls) {
		actions.add(index, requireNonNull(controls, CONTROLS_PARAMETER));
		return this;
	}

	@Override
	public Controls addSeparator() {
		actions.add(SEPARATOR);
		return this;
	}

	@Override
	public Controls addSeparatorAt(int index) {
		actions.add(index, SEPARATOR);
		return this;
	}

	@Override
	public Controls addAll(Controls controls) {
		actions.addAll(requireNonNull(controls, CONTROLS_PARAMETER).actions());
		return this;
	}

	@Override
	public void actionPerformed(ActionEvent e) {/*Not required*/}

	static final class DefaultSeparator implements Action {

		private static final String CONTROLS_SEPARATOR = "Separator";

		DefaultSeparator() {}

		@Override
		public Object getValue(String key) {
			throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
		}

		@Override
		public void putValue(String key, Object value) {
			throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
		}

		@Override
		public void setEnabled(boolean b) {
			throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
		}

		@Override
		public boolean isEnabled() {
			throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
		}

		@Override
		public void addPropertyChangeListener(PropertyChangeListener listener) {
			throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
		}

		@Override
		public void removePropertyChangeListener(PropertyChangeListener listener) {
			throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
		}
	}

	static final class DefaultConfig<T extends Enum<T>> implements Config<T> {

		private static final ControlItem SEPARATOR = new Separator();

		private final List<T> defaults;
		private final Function<T, Optional<Control>> standardControls;
		private final List<ControlItem> items = new ArrayList<>();

		DefaultConfig(Function<T, Optional<Control>> standardControls, List<T> defaults) {
			this.defaults = requireNonNull(defaults);
			this.standardControls = requireNonNull(standardControls);
			defaults();
		}

		@Override
		public Config<T> separator() {
			if (items.isEmpty() || items.get(items.size() - 1) != SEPARATOR) {
				items.add(SEPARATOR);
			}
			return this;
		}

		@Override
		public Config<T> standard(T identifier) {
			add(requireNonNull(identifier));
			return this;
		}

		@Override
		public Config<T> control(Control control) {
			items.add(new CustomAction(requireNonNull(control)));
			return this;
		}

		@Override
		public Config<T> control(Control.Builder<?, ?> controlBuilder) {
			return control(requireNonNull(controlBuilder).build());
		}

		@Override
		public Config<T> action(Action action) {
			items.add(new CustomAction(requireNonNull(action)));
			return this;
		}

		@Override
		public Config<T> clear() {
			items.clear();
			return this;
		}

		@Override
		public Config<T> defaults() {
			return defaults(null);
		}

		@Override
		public Config<T> defaults(T stopBefore) {
			for (T control : defaults) {
				if (stopBefore != null && control == stopBefore) {
					return this;
				}
				if (control == null) {
					separator();
				}
				else {
					add(control);
				}
			}

			return this;
		}

		@Override
		public Controls create() {
			Controls created = Controls.controls();
			items.forEach(item -> item.addTo(created));

			return created;
		}

		private void add(T controlIdentifier) {
			StandardControl<T> standardControl = new StandardControl<>(controlIdentifier, standardControls);
			if (!items.contains(standardControl)) {
				items.add(standardControl);
			}
		}

		private interface ControlItem {
			void addTo(Controls controlsToAddTo);
		}

		private static final class StandardControl<T> implements ControlItem {

			private final T tableControl;
			private final Function<T, Optional<Control>> controlProvider;

			private StandardControl(T tableControl, Function<T, Optional<Control>> controlProvider) {
				this.tableControl = tableControl;
				this.controlProvider = controlProvider;
			}

			@Override
			public void addTo(Controls controlsToAddTo) {
				controlProvider.apply(tableControl).ifPresent(control -> {
					if (control instanceof Controls) {
						Controls controls = (Controls) control;
						if (controls.notEmpty()) {
							if (nullOrEmpty(controls.getName())) {
								controls.actions().stream()
												.filter(action -> action != SEPARATOR)
												.forEach(action -> new CustomAction(action).addTo(controlsToAddTo));
							}
							else {
								controlsToAddTo.add(controls);
							}
						}
					}
					else {
						controlsToAddTo.add(control);
					}
				});
			}

			@Override
			public boolean equals(Object object) {
				if (this == object) {
					return true;
				}
				if (!(object instanceof DefaultConfig.StandardControl)) {
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

		private static final class CustomAction implements ControlItem {

			private final Action action;

			private CustomAction(Action action) {
				this.action = action;
			}

			@Override
			public void addTo(Controls controlsToAddTo) {
				controlsToAddTo.add(action);
			}
		}

		private static final class Separator implements ControlItem {

			@Override
			public void addTo(Controls controlsToAddTo) {
				List<Action> actions = controlsToAddTo.actions();
				if (actions.isEmpty() || actions.get(actions.size() - 1) != Controls.SEPARATOR) {
					controlsToAddTo.addSeparator();
				}
			}
		}
	}
}
