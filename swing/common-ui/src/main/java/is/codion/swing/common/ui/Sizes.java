/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.util.Objects;

/**
 * Utility class for setting component sizes.
 */
public final class Sizes {

  private static final String COMPONENT = "component";

  private Sizes() {}

  /**
   * Sets the preferred size of the given component to its current height and the given {@code preferredWidth}
   * @param component the component
   * @param preferredWidth the preferred width
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T setPreferredWidth(final T component, final int preferredWidth) {
    Objects.requireNonNull(component, COMPONENT);
    component.setPreferredSize(new Dimension(preferredWidth, component.getPreferredSize().height));

    return component;
  }

  /**
   * Sets the preferred size of the given component to its current width and the given {@code preferredHeight}
   * @param component the component
   * @param preferredHeight the preferred height
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T setPreferredHeight(final T component, final int preferredHeight) {
    Objects.requireNonNull(component, COMPONENT);
    component.setPreferredSize(new Dimension(component.getPreferredSize().width, preferredHeight));

    return component;
  }

  /**
   * Sets the minimum size of the given component to its current height and the given {@code minimumWidth}
   * @param component the component
   * @param minimumWidth the minimum width
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T setMinimumWidth(final T component, final int minimumWidth) {
    Objects.requireNonNull(component, COMPONENT);
    component.setMinimumSize(new Dimension(minimumWidth, component.getMinimumSize().height));

    return component;
  }

  /**
   * Sets the minimum size of the given component to its current width and the given {@code minimumHeight}
   * @param component the component
   * @param minimumHeight the minimum height
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T setMinimumHeight(final T component, final int minimumHeight) {
    Objects.requireNonNull(component, COMPONENT);
    component.setMinimumSize(new Dimension(component.getMinimumSize().width, minimumHeight));

    return component;
  }

  /**
   * Sets the maximum size of the given component to its current height and the given {@code maximumWidth}
   * @param component the component
   * @param maximumWidth the maximum width
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T setMaximumWidth(final T component, final int maximumWidth) {
    Objects.requireNonNull(component, COMPONENT);
    component.setMaximumSize(new Dimension(maximumWidth, component.getMaximumSize().height));

    return component;
  }

  /**
   * Sets the maximum size of the given component to its current width and the given {@code maximumHeight}
   * @param component the component
   * @param maximumHeight the maximum height
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T setMaximumHeight(final T component, final int maximumHeight) {
    Objects.requireNonNull(component, COMPONENT);
    component.setMaximumSize(new Dimension(component.getMaximumSize().width, maximumHeight));

    return component;
  }
}
