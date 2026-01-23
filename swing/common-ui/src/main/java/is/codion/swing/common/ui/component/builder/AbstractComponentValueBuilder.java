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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.builder;

import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueChange;
import is.codion.swing.common.ui.component.indicator.ModifiedIndicator;
import is.codion.swing.common.ui.component.indicator.ValidIndicator;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public abstract class AbstractComponentValueBuilder<C extends JComponent, T, B extends ComponentValueBuilder<C, T, B>>
				extends AbstractComponentBuilder<C, B> implements ComponentValueBuilder<C, T, B> {

	private final List<Consumer<ComponentValue<C, T>>> buildConsumers = new ArrayList<>(1);
	private final List<Value<T>> linkedValues = new ArrayList<>(1);
	private final List<Observable<T>> linkedObservables = new ArrayList<>(1);
	private final List<Value.Validator<T>> validators = new ArrayList<>();
	private final ValueListeners<T> listeners = new ValueListeners<>();

	private @Nullable ValidIndicator validIndicator = ValidIndicator.instance().orElse(null);
	private @Nullable ModifiedIndicator modifiedIndicator = ModifiedIndicator.instance().orElse(null);
	private @Nullable ObservableState modifiedObservable;
	private @Nullable ObservableState validObservable;
	private @Nullable Predicate<T> validPredicate;
	private @Nullable T value;
	private boolean valueSet = false;

	protected AbstractComponentValueBuilder() {}

	@Override
	public final B validIndicator(@Nullable ValidIndicator validIndicator) {
		this.validIndicator = validIndicator;
		return self();
	}

	@Override
	public final B valid(@Nullable ObservableState valid) {
		this.validObservable = valid;
		return self();
	}

	@Override
	public final B valid(@Nullable Predicate<T> valid) {
		this.validPredicate = valid;
		return self();
	}

	@Override
	public final B modifiedIndicator(@Nullable ModifiedIndicator modifiedIndicator) {
		this.modifiedIndicator = modifiedIndicator;
		return self();
	}

	@Override
	public final B modified(@Nullable ObservableState modified) {
		this.modifiedObservable = modified;
		return self();
	}

	@Override
	public final B validator(Value.Validator<T> validator) {
		this.validators.add(requireNonNull(validator));
		return self();
	}

	@Override
	public final B link(Value<T> linkedValue) {
		if (requireNonNull(linkedValue).isNullable() && !supportsNull()) {
			throw new IllegalArgumentException("Component does not support a nullable value");
		}
		this.linkedValues.add(linkedValue);
		return self();
	}

	@Override
	public final B link(Observable<T> linkedObservable) {
		if (requireNonNull(linkedObservable).isNullable() && !supportsNull()) {
			throw new IllegalArgumentException("Component does not support a nullable value");
		}
		this.linkedObservables.add(linkedObservable);
		return self();
	}

	@Override
	public final B listener(Runnable listener) {
		this.listeners.listener(listener);
		return self();
	}

	@Override
	public final B consumer(Consumer<? super T> consumer) {
		this.listeners.consumer(consumer);
		return self();
	}

	@Override
	public final B weakListener(Runnable weakListener) {
		this.listeners.weakListener(weakListener);
		return self();
	}

	@Override
	public final B weakConsumer(Consumer<? super T> weakConsumer) {
		this.listeners.weakConsumer(weakConsumer);
		return self();
	}

	@Override
	public final B changeListener(Runnable listener) {
		this.listeners.changeListener(listener);
		return self();
	}

	@Override
	public final B changeConsumer(Consumer<ValueChange<? super T>> consumer) {
		this.listeners.changeConsumer(consumer);
		return self();
	}

	@Override
	public final B weakChangeListener(Runnable weakListener) {
		this.listeners.weakChangeListener(weakListener);
		return self();
	}

	@Override
	public final B weakChangeConsumer(Consumer<ValueChange<? super T>> weakConsumer) {
		this.listeners.weakChangeConsumer(weakConsumer);
		return self();
	}

	@Override
	public final B when(T value, Runnable listener) {
		listeners.when(value, listener);
		return self();
	}

	@Override
	public final B when(T value, Consumer<? super T> consumer) {
		listeners.when(value, consumer);
		return self();
	}

	@Override
	public final B when(Predicate<T> predicate, Runnable listener) {
		listeners.when(predicate, listener);
		return self();
	}

	@Override
	public final B when(Predicate<T> predicate, Consumer<? super T> consumer) {
		listeners.when(predicate, consumer);
		return self();
	}

	@Override
	public final B value(@Nullable T value) {
		this.valueSet = true;
		this.value = value;
		return self();
	}

	@Override
	public final B onBuildValue(Consumer<ComponentValue<C, T>> onBuildValue) {
		buildConsumers.add(requireNonNull(onBuildValue));
		return self();
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
		configureValue(createValue(component));

		return super.configureComponent(component);
	}

	/**
	 * Creates the component value
	 * @param component the component
	 * @return a component value based on the component
	 */
	protected abstract ComponentValue<C, T> createValue(C component);

	private void configureValue(ComponentValue<C, T> componentValue) {
		C component = componentValue.component();
		component.putClientProperty(COMPONENT_VALUE, componentValue);
		validators.forEach(componentValue::addValidator);
		if (valueSet && linkedValues.isEmpty() && linkedObservables.isEmpty()) {
			componentValue.set(value);
		}
		linkedValues.forEach(componentValue::link);
		linkedObservables.forEach(componentValue::link);
		listeners.addListeners(componentValue);
		configureValidIndicator(componentValue);
		configureModifiedIndicator(component);
	}

	private void configureValidIndicator(ComponentValue<C, T> componentValue) {
		if (validIndicator == null) {
			return;
		}
		if (validObservable != null) {
			enableValidIndicator(validIndicator, componentValue.component(), validObservable);
		}
		else if (validPredicate != null) {
			enableValidIndicator(validIndicator, componentValue.component(), createValidState(componentValue, validPredicate));
		}
	}

	private void configureModifiedIndicator(C component) {
		if (modifiedIndicator != null && modifiedObservable != null) {
			enableModifiedIndicator(modifiedIndicator, component, modifiedObservable);
		}
	}

	private static <C extends JComponent, T> ObservableState createValidState(ComponentValue<C, T> componentValue,
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
