/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.StateObserver;

import javax.swing.JComponent;
import java.awt.Dimension;

/**
 * Builds a JComponent
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
   * @param transferFocusOnEnter true if the component should transfer focus on Enter
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
   * @param listener called after a component is built
   * @return this builder instance
   */
  B addBuildListener(EventDataListener<C> listener);

  /**
   * Builds the component.
   * @return the component
   */
  C build();
}
