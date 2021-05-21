/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a JComponent
 * @param <V> the type of the value the component represents
 * @param <T> the component type
 */
public interface ComponentBuilder<V, T extends JComponent> {

  /**
   * @param preferredHeight the preferred component height
   * @return this builder instance
   */
  ComponentBuilder<V, T> preferredHeight(int preferredHeight);

  /**
   * @param preferredWidth the preferred component width
   * @return this builder instance
   */
  ComponentBuilder<V, T> preferredWidth(int preferredWidth);

  /**
   * @param preferredSize the preferred component size
   * @return this builder instance
   */
  ComponentBuilder<V, T> preferredSize(Dimension preferredSize);

  /**
   * @param transferFocusOnEnter true if the component should transfer focus on Enter
   * @return this builder instance
   */
  ComponentBuilder<V, T> transferFocusOnEnter(boolean transferFocusOnEnter);

  /**
   * @param enabledState the state controlling the component enabled status
   * @return this builder instance
   */
  ComponentBuilder<V, T> enabledState(StateObserver enabledState);

  /**
   * @param onBuild called after the component is built
   * @return this builder instance
   */
  ComponentBuilder<V, T> onBuild(Consumer<T> onBuild);

  /**
   * Builds the component.
   * @return the component
   */
  T build();
}
