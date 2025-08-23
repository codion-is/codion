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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.builder;

import is.codion.common.observer.Observable;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.indicator.ModifiedIndicatorFactory;
import is.codion.swing.common.ui.component.indicator.UnderlineModifiedIndicatorFactory;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public abstract class AbstractComponentValueBuilder<T, C extends JComponent, B extends ComponentValueBuilder<T, C, B>>
	extends AbstractComponentBuilder<C, B> implements ComponentValueBuilder<T, C, B> {

	private final List<Consumer<ComponentValue<C, T>>> buildConsumers = new ArrayList<>(1);
	private final List<Value<T>> linkedValues = new ArrayList<>(1);
	private final List<Observable<T>> linkedObservables = new ArrayList<>(1);
	private final List<Value.Validator<T>> validators = new ArrayList<>();
	private final List<Runnable> listeners = new ArrayList<>();
	private final List<Consumer<T>> consumers = new ArrayList<>();

	private @Nullable ValidIndicatorFactory validIndicatorFactory =
					ValidIndicatorFactory.instance().orElse(null);
	private @Nullable ModifiedIndicatorFactory modifiedIndicatorFactory =
					new UnderlineModifiedIndicatorFactory();
	private @Nullable ObservableState modifiedObservable;
	private @Nullable ObservableState validObservable;
	private @Nullable Predicate<T> validator;
	private @Nullable T value;
	private boolean valueSet = false;

	protected AbstractComponentValueBuilder() {}

	@Override
	public final B validIndicatorFactory(@Nullable ValidIndicatorFactory validIndicatorFactory) {
		this.validIndicatorFactory = validIndicatorFactory;
		return (B) this;
	}

	@Override
	public final B validIndicator(@Nullable ObservableState valid) {
		this.validObservable = valid;
		return (B) this;
	}

	@Override
	public final B validIndicator(@Nullable Predicate<T> validator) {
		this.validator = validator;
		return (B) this;
	}

	@Override
	public final B modifiedIndicatorFactory(@Nullable ModifiedIndicatorFactory modifiedIndicatorFactory) {
		this.modifiedIndicatorFactory = modifiedIndicatorFactory;
		return (B) this;
	}

	@Override
	public final B modifiedIndicator(@Nullable ObservableState modified) {
		this.modifiedObservable = modified;
		return (B) this;
	}

	@Override
	public final B validator(Value.Validator<T> validator) {
		this.validators.add(requireNonNull(validator));
		return (B) this;
	}

	@Override
	public final B link(Value<T> linkedValue) {
		if (requireNonNull(linkedValue).isNullable() && !supportsNull()) {
			throw new IllegalArgumentException("Component does not support a nullable value");
		}
		this.linkedValues.add(linkedValue);
		return (B) this;
	}

	@Override
	public final B link(Observable<T> linkedObservable) {
		if (requireNonNull(linkedObservable).isNullable() && !supportsNull()) {
			throw new IllegalArgumentException("Component does not support a nullable value");
		}
		this.linkedObservables.add(linkedObservable);
		return (B) this;
	}

	@Override
	public final B listener(Runnable listener) {
		this.listeners.add(requireNonNull(listener));
		return (B) this;
	}

	@Override
	public final B consumer(Consumer<T> consumer) {
		this.consumers.add(requireNonNull(consumer));
		return (B) this;
	}

	@Override
	public final B value(@Nullable T value) {
		this.valueSet = true;
		this.value = value;
		return (B) this;
	}

	@Override
	public final B onBuildValue(Consumer<ComponentValue<C, T>> onBuildValue) {
		buildConsumers.add(requireNonNull(onBuildValue));
		return (B) this;
	}

	@Override
	public final ComponentValue<C, T> buildValue() {
		return buildValue(null);
	}

	@Override
	public final ComponentValue<C, T> buildValue(@Nullable Consumer<ComponentValue<C, T>> onBuild) {
		C component = build();
		ComponentValue<C, T> componentValue = (ComponentValue<C, T>) component.getClientProperty(COMPONENT_VALUE);
		if (onBuild != null) {
			onBuild.accept(componentValue);
		}
		buildConsumers.forEach(consumer -> consumer.accept(componentValue));

		return componentValue;
	}

	@Override
	protected final C configureComponent(C component) {
		configureValue(createComponentValue(component));

		return super.configureComponent(component);
	}

	/**
	 * Creates the component value
	 * @param component the component
	 * @return a component value based on the component
	 */
	protected abstract ComponentValue<C, T> createComponentValue(C component);

	private void configureValue(ComponentValue<C, T> componentValue) {
		C component = componentValue.component();
		component.putClientProperty(COMPONENT_VALUE, componentValue);
		validators.forEach(componentValue::addValidator);
		if (valueSet && linkedValues.isEmpty() && linkedObservables.isEmpty()) {
			componentValue.set(value);
		}
		linkedValues.forEach(componentValue::link);
		linkedObservables.forEach(componentValue::link);
		listeners.forEach(componentValue::addListener);
		consumers.forEach(componentValue::addConsumer);
		configureValidIndicator(componentValue);
		configureModifiedIndicator(component);
	}

	private void configureValidIndicator(ComponentValue<C, T> componentValue) {
		if (validIndicatorFactory == null) {
			return;
		}
		if (validObservable != null) {
			enableValidIndicator(validIndicatorFactory, componentValue.component(), validObservable);
		}
		else if (validator != null) {
			enableValidIndicator(validIndicatorFactory, componentValue.component(), createValidState(componentValue, validator));
		}
	}

	private void configureModifiedIndicator(C component) {
		if (modifiedIndicatorFactory != null && modifiedObservable != null) {
			modifiedIndicatorFactory.enable(component, modifiedObservable);
		}
	}

	private static <T, C extends JComponent> ObservableState createValidState(ComponentValue<C, T> componentValue,
																																						Predicate<T> validator) {
		ValidationConsumer<T> validationConsumer = new ValidationConsumer<>(componentValue.get(), validator);
		componentValue.addConsumer(validationConsumer);

		return validationConsumer.valid.observable();
	}

	private static final class ValidationConsumer<T> implements Consumer<T> {

		private final Predicate<@Nullable T> validator;
		private final State valid;

		private ValidationConsumer(@Nullable T initialValue, Predicate<T> validator) {
			this.validator = validator;
			this.valid = State.state();
			accept(initialValue);
		}

		@Override
		public void accept(@Nullable T value) {
			valid.set(validator.test(value));
		}
	}
}
