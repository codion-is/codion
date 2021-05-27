/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.value.ComponentValue;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a JComponent.<br>
 * Note that this builder is a single-user builder, once {@link #build} or {@link #buildComponentValue()}
 * have been called they will return the same instance on subsequent calls.
 * @param <T> the type of the value the component represents
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>> {

  /**
   * @param focusable false if the component should not be focusable
   * @return this builder instance
   */
  B focusable(boolean focusable);

  /**
   * @param preferredHeight the preferred component height
   * @return this builder instance
   */
  B preferredHeight(int preferredHeight);

  /**
   * @param preferredWidth the preferred component width
   * @return this builder instance
   */
  B preferredWidth(int preferredWidth);

  /**
   * @param preferredSize the preferred component size
   * @return this builder instance
   */
  B preferredSize(Dimension preferredSize);

  /**
   * @param maximumSize the maximum component size
   * @return this builder instance
   */
  B maximumSize(Dimension maximumSize);

  /**
   * @param minimumSize the minimum component size
   * @return this builder instance
   */
  B minimumSize(Dimension minimumSize);

  /**
   * @param border the component border
   * @return this builder instance
   */
  B border(Border border);

  /**
   * @param transferFocusOnEnter if true then the text field transfer focus on enter (shift-enter for backwards)
   * @return this builder instance
   */
  B transferFocusOnEnter(boolean transferFocusOnEnter);

  /**
   * @param description the description
   * @return this builder instance
   */
  B description(String description);

  /**
   * @param enabledState the state controlling the component enabled status
   * @return this builder instance
   */
  B enabledState(StateObserver enabledState);

  /**
   * @param linkedValue a value to link to the component value
   * @return this builder instance
   */
  B linkedValue(Value<T> linkedValue);

  /**
   * @param linkedValueObserver a value to link to the component value
   * @return this builder instance
   */
  B linkedValueObserver(ValueObserver<T> linkedValueObserver);

  /**
   * Sets the initial value for the component, overridden by {@link #linkedValue(Value)}.
   * @param initialValue the initial value
   * @return this builder instance
   */
  B initialValue(T initialValue);

  /**
   * @param onBuild called when the component has been built
   * @return this builder instance
   */
  B onBuild(Consumer<C> onBuild);

  /**
   * Builds and returns the component, note that subsequent calls return the same component.
   * @return the component
   */
  C build();

  /**
   * Builds and returns the component value, note that subsequent calls return the same component value.
   * @return the component value
   */
  ComponentValue<T, C> buildComponentValue();
}
