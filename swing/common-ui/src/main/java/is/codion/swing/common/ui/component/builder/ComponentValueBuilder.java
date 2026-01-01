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
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueChange;
import is.codion.swing.common.ui.component.indicator.ModifiedIndicatorFactory;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Use {@link #build} to build a JComponent instance or {@link #buildValue()} to build a {@link ComponentValue} instance.<br>
 * The component is available via {@link ComponentValue#component()} and the associated {@link ComponentValue} instance
 * is available via the {@link #COMPONENT_VALUE} client property.
 * @param <C> the component type
 * @param <T> the type of the value the component represents
 * @param <B> the builder type
 */
public interface ComponentValueBuilder<C extends JComponent, T, B extends ComponentValueBuilder<C, T, B>>
				extends ComponentBuilder<C, B>, Supplier<C> {

	/**
	 * The client property key for the associated {@link ComponentValue}
	 * {@snippet :
	 *   JTextField textField =
	 *            Components.stringField()
	 *                    .build();
	 *
	 *   ComponentValue<JTextField, String > componentValue =
	 *            (ComponentValue<JTextField, String>)
	 *                    textField.getClientProperty(COMPONENT_VALUE);
	 *}
	 * @see JComponent#getClientProperty(Object)
	 */
	String COMPONENT_VALUE = "componentValue";

	/**
	 * Sets the initial value for the component, unless value(s) have been linked via {@link #link(Value)}
	 * or {@link #link(Observable)}, which then control the inital value.
	 * The initial value is set before any listeners are added, so no events are triggered.
	 * @param value the initial value
	 * @return this builder instance
	 */
	B value(@Nullable T value);

	/**
	 * Enables a modified indicator based on the given modified state.
	 * @param modified the modified state
	 * @return this builder instance
	 * @see #modifiedIndicator(ModifiedIndicatorFactory)
	 */
	B modifiedIndicator(@Nullable ObservableState modified);

	/**
	 * @param validIndicator the {@link ValidIndicatorFactory} to use, null for none
	 * @return this builder instance
	 * @see ValidIndicatorFactory#instance()
	 */
	B validIndicator(@Nullable ValidIndicatorFactory validIndicator);

	/**
	 * Enables a valid indicator based on the given valid state.
	 * @param valid the valid state
	 * @return this builder instance
	 * @see #validIndicator(ValidIndicatorFactory)
	 * @see is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory
	 */
	B validIndicator(@Nullable ObservableState valid);

	/**
	 * <p>Enables a valid indicator based on the given validator. Note that this
	 * is overridden by {@link #validIndicator(ObservableState)}.
	 * <p>The validator gets called each time the value changes and
	 * should return true as long as the value is valid.
	 * @param validator called each time the component value changes
	 * @return this builder instance
	 * @see #validIndicator(ValidIndicatorFactory)
	 * @see is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory
	 */
	B validIndicator(@Nullable Predicate<T> validator);

	/**
	 * By default {@link is.codion.swing.common.ui.component.indicator.UnderlineModifiedIndicatorFactory}.
	 * @param modifiedIndicator the {@link ModifiedIndicatorFactory} to use, null for none
	 * @return this builder instance
	 */
	B modifiedIndicator(@Nullable ModifiedIndicatorFactory modifiedIndicator);

	/**
	 * @param validator the validator to use
	 * @return this builder instance
	 */
	B validator(Value.Validator<T> validator);

	/**
	 * Creates a bidirectional link to the given value. Overrides any initial value set.
	 * @param linkedValue a value to link to the component value
	 * @return this builder instance
	 */
	B link(Value<T> linkedValue);

	/**
	 * Creates a read-only link to the given {@link Observable}.
	 * @param linkedValue a value to link to the component value
	 * @return this builder instance
	 */
	B link(Observable<T> linkedValue);

	/**
	 * @param listener a listener to add to the resulting component value
	 * @return this builder instance
	 */
	B listener(Runnable listener);

	/**
	 * @param consumer a consumer to add to the resulting component value
	 * @return this builder instance
	 */
	B consumer(Consumer<? super T> consumer);

	/**
	 * @param weakListener a weak listener to add
	 * @return this builder instance
	 */
	B weakListener(Runnable weakListener);

	/**
	 * @param weakConsumer a weak consumer to add
	 * @return this builder instance
	 */
	B weakConsumer(Consumer<? super T> weakConsumer);

	/**
	 * @param listener a change listener to add
	 * @return this builder instance
	 * @see Value#changed()
	 */
	B changeListener(Runnable listener);

	/**
	 * @param consumer a change consumer to add
	 * @return this builder instance
	 * @see Value#changed()
	 */
	B changeConsumer(Consumer<ValueChange<? super T>> consumer);

	/**
	 * @param weakListener a weak change listener to add
	 * @return this builder instance
	 * @see Value#changed()
	 */
	B weakChangeListener(Runnable weakListener);

	/**
	 * @param weakConsumer a weak change consumer to add
	 * @return this builder instance
	 * @see Value#changed()
	 */
	B weakChangeConsumer(Consumer<ValueChange<? super T>> weakConsumer);

	/**
	 * Adds a conditional listener
	 * @param value the value on which to run
	 * @param listener the listener
	 * @return this builder instance
	 */
	B when(T value, Runnable listener);

	/**
	 * Adds a conditional consumer
	 * @param value the value to consume
	 * @param consumer the consumer
	 * @return this builder instance
	 */
	B when(T value, Consumer<? super T> consumer);

	/**
	 * Adds a conditional listener
	 * @param predicate the predicate on which to run
	 * @param listener the runnable
	 * @return this builder instance
	 */
	B when(Predicate<T> predicate, Runnable listener);

	/**
	 * Adds a conditional consumer
	 * @param predicate the predicate on which to consume the value
	 * @param consumer the consumer to use
	 * @return this builder instance
	 */
	B when(Predicate<T> predicate, Consumer<? super T> consumer);

	/**
	 * @param onBuildValue called when the component value has been built.
	 * @return this builder instance
	 */
	B onBuildValue(Consumer<ComponentValue<C, T>> onBuildValue);

	/**
	 * Builds and returns the component value.
	 * @return the component value
	 */
	ComponentValue<C, T> buildValue();

	/**
	 * Builds and returns the component value.
	 * @param onBuild called after the component value is built.
	 * @return the component value
	 */
	ComponentValue<C, T> buildValue(@Nullable Consumer<ComponentValue<C, T>> onBuild);
}
