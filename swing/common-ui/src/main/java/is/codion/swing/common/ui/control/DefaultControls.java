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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.value.Value;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultControls extends AbstractControl implements Controls {

	private final List<Action> actions;

	DefaultControls(DefaultControlsBuilder builder) {
		super(builder);
		actions = initializeActions(builder);
	}

	@Override
	public List<Action> actions() {
		return actions;
	}

	@Override
	public int size() {
		return (int) actions.stream().filter(action -> action != SEPARATOR).count();
	}

	@Override
	public void actionPerformed(ActionEvent e) {/*Not required*/}

	@Override
	public ControlsBuilder copy() {
		return new DefaultControlsBuilder(this);
	}

	private static List<Action> initializeActions(DefaultControlsBuilder builder) {
		List<Action> actionsToAdd = new ArrayList<>();
		builder.actions.forEach(action -> addAction(action, actionsToAdd));
		removeTrailingSeparators(actionsToAdd);

		return unmodifiableList(new ArrayList<>(actionsToAdd));
	}

	private static void addAction(Action action, List<Action> actionsToAdd) {
		if (action == SEPARATOR) {
			if (!actionsToAdd.isEmpty() && actionsToAdd.get(actionsToAdd.size() - 1) != SEPARATOR) {
				actionsToAdd.add(SEPARATOR);
			}
		}
		else if (action instanceof Controls) {
			if (((Controls) action).size() > 0) {
				actionsToAdd.add(action);
			}
		}
		else {
			actionsToAdd.add(action);
		}
	}

	private static void removeTrailingSeparators(List<Action> actionsToAdd) {
		while (!actionsToAdd.isEmpty() && actionsToAdd.get(actionsToAdd.size() - 1) == SEPARATOR) {
			actionsToAdd.remove(actionsToAdd.size() - 1);
		}
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

	static final class DefaultLayout implements Layout {

		private static final ControlItem SEPARATOR = new Separator();

		private final Value<KeyStroke> keyStroke = Value.nullable();
		private final List<ControlKey<?>> defaults;
		private final List<ControlItem> items = new ArrayList<>();

		DefaultLayout(List<ControlKey<?>> defaults) {
			this.defaults = requireNonNull(defaults);
			defaults();
		}

		private DefaultLayout(DefaultLayout layout) {
			this.keyStroke.set(layout.keyStroke.get());
			this.defaults = new ArrayList<>(layout.defaults);
			defaults();
		}

		@Override
		public Layout separator() {
			if (items.isEmpty() || items.get(items.size() - 1) != SEPARATOR) {
				items.add(SEPARATOR);
			}
			return this;
		}

		@Override
		public Layout control(ControlKey<?> controlKey) {
			add(requireNonNull(controlKey));
			return this;
		}

		@Override
		public Layout control(Control control) {
			items.add(new CustomAction(requireNonNull(control)));
			return this;
		}

		@Override
		public Layout control(Supplier<? extends Control> control) {
			return control(requireNonNull(control).get());
		}

		@Override
		public Layout action(Action action) {
			items.add(new CustomAction(requireNonNull(action)));
			return this;
		}

		@Override
		public Layout controls(ControlKey<Controls> controlsKey) {
			return control(controlsKey);
		}

		@Override
		public Layout controls(ControlsKey controlsKey, Consumer<Layout> layout) {
			Layout defaultLayout = controlsKey.defaultLayout()
							.map(Layout::copy)
							.orElseThrow(() -> new IllegalArgumentException("No default controls layout available"));
			layout.accept(defaultLayout);
			items.add(new StandardControls(controlsKey, defaultLayout));
			return this;
		}

		@Override
		public Layout clear() {
			items.clear();
			return this;
		}

		@Override
		public Layout copy() {
			return new DefaultLayout(this);
		}

		@Override
		public Layout defaults() {
			return doDefaults(null);
		}

		@Override
		public Layout defaults(ControlKey<?> stopAt) {
			return doDefaults(stopAt);
		}

		@Override
		public Controls create(ControlMap controlMap) {
			ControlsBuilder builder = Controls.builder();
			items.forEach(item -> item.addTo(builder, controlMap));

			return builder.build();
		}

		private Layout doDefaults(@Nullable ControlKey<?> stopAt) {
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

		private void add(ControlKey<?> controlKey) {
			if (!contains(controlKey)) {
				items.add(new StandardControl(controlKey));
			}
		}

		private boolean contains(ControlKey<?> controlKey) {
			return items.stream()
							.map(ControlItem::controlKey)
							.flatMap(Optional::stream)
							.anyMatch(controlKey::equals);
		}

		private interface ControlItem {
			void addTo(ControlsBuilder builder, ControlMap controlMap);

			default Optional<ControlKey<?>> controlKey() {
				return Optional.empty();
			}
		}

		private static final class StandardControl implements ControlItem {

			private final ControlKey<?> controlKey;

			private StandardControl(ControlKey<?> controlKey) {
				this.controlKey = controlKey;
			}

			@Override
			public void addTo(ControlsBuilder builder, ControlMap controlMap) {
				controlMap.control(controlKey).optional().ifPresent(control -> {
					if (control instanceof Controls) {
						addControls(builder, (Controls) control, controlMap);
					}
					else {
						builder.control(control);
					}
				});
			}

			@Override
			public Optional<ControlKey<?>> controlKey() {
				return Optional.of(controlKey);
			}

			private static void addControls(ControlsBuilder builder, Controls controlsToAdd, ControlMap controlMap) {
				if (controlsToAdd.size() > 0) {
					if (!controlsToAdd.caption().isPresent()) {
						controlsToAdd.actions().stream()
										.filter(action -> action != SEPARATOR)
										.forEach(action -> new CustomAction(action).addTo(builder, controlMap));
					}
					else {
						builder.control(controlsToAdd);
					}
				}
			}
		}

		private static final class StandardControls implements ControlItem {

			private final ControlsKey controlsKey;
			private final Layout layout;

			private StandardControls(ControlsKey controlsKey, Layout layout) {
				this.controlsKey = controlsKey;
				this.layout = layout;
			}

			@Override
			public void addTo(ControlsBuilder builder, ControlMap controlMap) {
				Controls defaultControls = controlMap.control(controlsKey).getOrThrow();
				Controls configuredControls = layout.create(controlMap);
				builder.control(defaultControls.copy()
								.removeAll()
								.actions(configuredControls.actions()));
			}

			@Override
			public Optional<ControlKey<?>> controlKey() {
				return Optional.of(controlsKey);
			}
		}

		private static final class CustomAction implements ControlItem {

			private final Action action;

			private CustomAction(Action action) {
				this.action = action;
			}

			@Override
			public void addTo(ControlsBuilder builder, ControlMap controlMap) {
				builder.action(action);
			}
		}

		private static final class Separator implements ControlItem {

			@Override
			public void addTo(ControlsBuilder builder, ControlMap controlMap) {
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
		public ControlsBuilder control(Supplier<? extends Control> control) {
			return controlAt(actions.size(), control);
		}

		@Override
		public ControlsBuilder controlAt(int index, Control control) {
			return actionAt(index, requireNonNull(control));
		}

		@Override
		public ControlsBuilder controlAt(int index, Supplier<? extends Control> control) {
			return actionAt(index, requireNonNull(control).get());
		}

		@Override
		public ControlsBuilder controls(Control... controls) {
			return controls(asList(requireNonNull(controls)));
		}

		@Override
		public ControlsBuilder controls(Collection<? extends Control> controls) {
			actions.addAll(requireNonNull(controls));
			return this;
		}

		@Override
		public ControlsBuilder controls(Supplier<? extends Control>... controls) {
			actions.addAll(Arrays.stream(controls)
							.map(Supplier::get)
							.collect(Collectors.toList()));
			return this;
		}

		@Override
		public ControlsBuilder action(Action action) {
			return actionAt(actions.size(), action);
		}

		@Override
		public ControlsBuilder actionAt(int index, Action action) {
			if (requireNonNull(action) == SEPARATOR) {
				return separatorAt(index);
			}
			actions.add(index, action);
			return this;
		}

		@Override
		public ControlsBuilder actions(Action... actions) {
			return actions(asList(requireNonNull(actions)));
		}

		@Override
		public ControlsBuilder actions(Collection<Action> actions) {
			requireNonNull(actions).forEach(this::action);
			return this;
		}

		@Override
		public ControlsBuilder separator() {
			return separatorAt(actions.size());
		}

		@Override
		public ControlsBuilder separatorAt(int index) {
			actions.add(index, SEPARATOR);
			return this;
		}

		@Override
		public ControlsBuilder remove(Action action) {
			actions.remove(requireNonNull(action));
			return this;
		}

		@Override
		public ControlsBuilder removeAll() {
			actions.clear();
			return this;
		}

		@Override
		public Controls build() {
			return new DefaultControls(this);
		}
	}
}
