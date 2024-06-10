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

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultControls extends AbstractControl implements Controls {

	private final List<Action> actions = new ArrayList<>();

	DefaultControls(DefaultControlsBuilder builder) {
		super(builder);
		actions.addAll(builder.actions);
	}

	@Override
	public List<Action> actions() {
		return unmodifiableList(actions);
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
	public void actionPerformed(ActionEvent e) {/*Not required*/}

	@Override
	public ControlsBuilder copy() {
		return new DefaultControlsBuilder(this);
	}

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

	static final class DefaultConfig implements Config {

		private static final ControlItem SEPARATOR = new Separator();

		private final List<ControlKey<?>> defaults;
		private final ControlMap controls;
		private final List<ControlItem> items = new ArrayList<>();

		DefaultConfig(ControlMap controls, List<ControlKey<?>> defaults) {
			this.defaults = requireNonNull(defaults);
			this.controls = requireNonNull(controls);
			defaults();
		}

		@Override
		public Config separator() {
			if (items.isEmpty() || items.get(items.size() - 1) != SEPARATOR) {
				items.add(SEPARATOR);
			}
			return this;
		}

		@Override
		public Config standard(ControlKey<?> controlKey) {
			add(requireNonNull(controlKey));
			return this;
		}

		@Override
		public Config control(Control control) {
			items.add(new CustomAction(requireNonNull(control)));
			return this;
		}

		@Override
		public Config control(Control.Builder<?, ?> controlBuilder) {
			return control(requireNonNull(controlBuilder).build());
		}

		@Override
		public Config action(Action action) {
			items.add(new CustomAction(requireNonNull(action)));
			return this;
		}

		@Override
		public Config clear() {
			items.clear();
			return this;
		}

		@Override
		public Config defaults() {
			return defaults(null);
		}

		@Override
		public Config defaults(ControlKey<?> stopAt) {
			for (ControlKey<?> control : defaults) {
				if (control == null) {
					separator();
				}
				else {
					add(control);
				}
				if (stopAt != null && control == stopAt) {
					break;
				}
			}

			return this;
		}

		@Override
		public Controls create() {
			ControlsBuilder builder = Controls.builder();
			items.forEach(item -> item.addTo(builder));

			return builder.build();
		}

		private void add(ControlKey<?> controlIdentifier) {
			StandardControl standardControl = new StandardControl(controlIdentifier, controls);
			if (!items.contains(standardControl)) {
				items.add(standardControl);
			}
		}

		private interface ControlItem {
			void addTo(ControlsBuilder builder);
		}

		private static final class StandardControl implements ControlItem {

			private final ControlKey<?> tableControl;
			private final ControlMap controls;

			private StandardControl(ControlKey<?> tableControl, ControlMap controls) {
				this.tableControl = tableControl;
				this.controls = controls;
			}

			@Override
			public void addTo(ControlsBuilder builder) {
				controls.control(tableControl).optional().ifPresent(control -> {
					if (control instanceof Controls) {
						Controls controlsToAdd = (Controls) control;
						if (controlsToAdd.notEmpty()) {
							if (!controlsToAdd.name().isPresent()) {
								controlsToAdd.actions().stream()
												.filter(action -> action != SEPARATOR)
												.forEach(action -> new CustomAction(action).addTo(builder));
							}
							else {
								builder.control(controlsToAdd);
							}
						}
					}
					else {
						builder.control(control);
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
				StandardControl that = (StandardControl) object;
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
			public void addTo(ControlsBuilder builder) {
				builder.action(action);
			}
		}

		private static final class Separator implements ControlItem {

			@Override
			public void addTo(ControlsBuilder builder) {
				builder.separator();
			}
		}
	}

	static final class DefaultControlsBuilder extends AbstractControlBuilder<Controls, ControlsBuilder> implements ControlsBuilder {

		private final List<Action> actions = new ArrayList<>();

		DefaultControlsBuilder() {}

		private DefaultControlsBuilder(DefaultControls controls) {
			enabled(controls.enabled());
			controls.keys().forEach(key -> value(key, controls.getValue(key)));
			actions.addAll(controls.actions);
		}

		@Override
		public ControlsBuilder control(Control control) {
			return controlAt(actions.size(), control);
		}

		@Override
		public ControlsBuilder control(Control.Builder<?, ?> controlBuilder) {
			return controlAt(actions.size(), controlBuilder);
		}

		@Override
		public ControlsBuilder controlAt(int index, Control control) {
			actions.add(index, requireNonNull(control));
			return this;
		}

		@Override
		public ControlsBuilder controlAt(int index, Control.Builder<?, ?> controlBuilder) {
			actions.add(index, requireNonNull(controlBuilder).build());
			return this;
		}

		@Override
		public ControlsBuilder controls(Control... controls) {
			this.actions.addAll(Arrays.asList(requireNonNull(controls)));
			return this;
		}

		@Override
		public ControlsBuilder controls(Control.Builder<?, ?>... controlBuilders) {
			this.actions.addAll(Arrays.stream(controlBuilders)
							.map(new BuildControl())
							.collect(Collectors.toList()));
			return this;
		}

		@Override
		public ControlsBuilder action(Action action) {
			return actionAt(actions.size(), action);
		}

		@Override
		public ControlsBuilder actionAt(int index, Action action) {
			this.actions.add(index, requireNonNull(action));
			return this;
		}

		@Override
		public ControlsBuilder actions(Action... actions) {
			return actions(Arrays.asList(requireNonNull(actions)));
		}

		@Override
		public ControlsBuilder actions(Collection<Action> actions) {
			this.actions.addAll(requireNonNull(actions));
			return this;
		}

		@Override
		public ControlsBuilder separator() {
			return separatorAt(actions.size());
		}

		@Override
		public ControlsBuilder separatorAt(int index) {
			if (actions.isEmpty() || actions.get(index - 1) != Controls.SEPARATOR) {
				this.actions.add(SEPARATOR);
			}
			return this;
		}

		@Override
		public ControlsBuilder remove(Action action) {
			this.actions.remove(requireNonNull(action));
			return this;
		}

		@Override
		public ControlsBuilder removeAll() {
			this.actions.clear();
			return this;
		}

		@Override
		public Controls build() {
			return new DefaultControls(this);
		}

		private static final class BuildControl implements Function<Control.Builder<?, ?>, Control> {

			@Override
			public Control apply(Control.Builder<?, ?> builder) {
				return builder.build();
			}
		}
	}
}
