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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.model.EntityEditor.EditorValue;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditor;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static javax.accessibility.AccessibleRelation.LABELED_BY;

/**
 * A default {@link EditorComponents} implementation
 */
final class DefaultEditorComponents implements EditorComponents {

	private final Map<Attribute<?>, EditorComponent<?>> components = new HashMap<>();
	private final Map<Attribute<?>, ComponentValueBuilder<?, ?, ?>> componentBuilders = new HashMap<>();

	private final ComponentSettings settings = new DefaultComponentSettings();
	private final CreateComponents create;
	private final SwingEntityEditor editor;

	DefaultEditorComponents(SwingEntityEditor editor) {
		this.editor = requireNonNull(editor);
		this.create = new CreateComponents(this);
	}

	@Override
	public SwingEntityEditor editor() {
		return editor;
	}

	@Override
	public ComponentSettings settings() {
		return settings;
	}

	@Override
	public CreateComponents create() {
		return create;
	}

	@Override
	public <T> EditorComponent<T> component(Attribute<T> attribute) {
		ComponentValueBuilder<?, ?, ?> componentBuilder = componentBuilders.get(requireNonNull(attribute));
		if (componentBuilder != null) {
			componentBuilder.build();
		}

		return (EditorComponent<T>) components.computeIfAbsent(attribute, k -> new DefaultEditorComponent<>(editor.value(attribute)));
	}

	Map<Attribute<?>, EditorComponent<?>> components() {
		return unmodifiableMap(components);
	}

	private static final class DefaultComponentSettings implements ComponentSettings {

		private final Value<Integer> textFieldColumns = Value.nonNull(12);
		private final State modifiedIndicator = State.state(true);
		private final State validIndicator = State.state(true);
		private final State transferFocusOnEnter = State.state(true);

		@Override
		public Value<Integer> textFieldColumns() {
			return textFieldColumns;
		}

		@Override
		public State modifiedIndicator() {
			return modifiedIndicator;
		}

		@Override
		public State validIndicator() {
			return validIndicator;
		}

		@Override
		public State transferFocusOnEnter() {
			return transferFocusOnEnter;
		}
	}

	private final class DefaultEditorComponent<T> implements EditorComponent<T> {

		private final Value<JComponent> component = Value.builder()
						.<JComponent>nullable()
						.notify(Value.Notify.CHANGED)
						.build();
		private final EditorValue<T> value;

		private DefaultEditorComponent(EditorValue<T> value) {
			this.value = value;
		}

		@Override
		public JComponent get() {
			if (component.isNull()) {
				throw new IllegalStateException("Component has not been set for: " + value.attribute());
			}

			return component.getOrThrow();
		}

		@Override
		public void set(JComponent component) {
			requireNonNull(component);
			if (!this.component.isNull()) {
				throw new IllegalStateException("Component has already been set for: " + value.attribute());
			}
			this.component.set(component);
		}

		@Override
		public void set(ComponentValue<? extends JComponent, T> componentValue) {
			set(requireNonNull(componentValue).component());
			componentValue.link(value);
		}

		@Override
		public <C extends JComponent, B extends ComponentValueBuilder<C, T, B>> B set(B componentBuilder) {
			requireNonNull(componentBuilder);
			if (componentBuilders.containsKey(value.attribute()) || !component.isNull()) {
				throw new IllegalStateException("Component has already been set for attribute: " + value.attribute());
			}
			AttributeDefinition<T> attributeDefinition = editor.entities()
							.definition(value.attribute().entityType()).attributes().definition(value.attribute());
			componentBuilders.put(value.attribute(), componentBuilder
							.link(value)
							.name(value.attribute().toString())
							.toolTipText(value.message())
							.enabled(!attributeDefinition.derived())
							.label(label -> label
											.text(attributeDefinition.caption())
											.displayedMnemonic(attributeDefinition.mnemonic()))
							.transferFocusOnEnter(settings.transferFocusOnEnter().is())
							.valid(settings.validIndicator().is() ? value.valid() : null)
							.modified(settings.modifiedIndicator().is() ? value.modified() : null)
							.onBuild(this::setComponent));
			if (componentBuilder instanceof TextFieldBuilder<?,?,?>) {
				((TextFieldBuilder<?, ?, ?>) componentBuilder).columns(settings.textFieldColumns().getOrThrow());
			}

			return componentBuilder;
		}

		@Override
		public void replace(JComponent component) {
			requireNonNull(component);
			if (this.component.isNull()) {
				throw new IllegalStateException("No component has been set for: " + value.attribute());
			}
			this.component.set(component);
		}

		@Override
		public Optional<JComponent> optional() {
			return component.optional();
		}

		@Override
		public JLabel label() {
			JLabel label = (JLabel) get().getClientProperty(LABELED_BY);
			if (label == null) {
				throw new IllegalStateException("No label associated with component: " + value.attribute());
			}

			return label;
		}

		private void setComponent(JComponent comp) {
			componentBuilders.remove(value.attribute());
			component.set(comp);
		}
	}
}
