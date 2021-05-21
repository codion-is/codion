/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.Components;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

abstract class AbstractComponentBuilder<V, T extends JComponent> implements ComponentBuilder<V, T> {

  protected final Property<V> property;
  protected final Value<V> value;

  private int preferredHeight;
  private int preferredWidth;
  protected boolean transferFocusOnEnter;
  protected StateObserver enabledState;
  protected Consumer<T> onBuild;

  protected AbstractComponentBuilder(final Property<V> attribute, final Value<V> value) {
    this.property = attribute;
    this.value = value;
  }

  @Override
  public ComponentBuilder<V, T> preferredHeight(final int preferredHeight) {//todo return type
    this.preferredHeight = preferredHeight;
    return this;
  }

  @Override
  public ComponentBuilder<V, T> preferredWidth(final int preferredWidth) {
    this.preferredWidth = preferredWidth;
    return this;
  }

  @Override
  public ComponentBuilder<V, T> preferredSize(final Dimension preferredSize) {
    requireNonNull(preferredSize);
    this.preferredHeight = preferredSize.height;
    this.preferredWidth = preferredSize.width;
    return this;
  }

  @Override
  public ComponentBuilder<V, T> transferFocusOnEnter(final boolean transferFocusOnEnter) {
    this.transferFocusOnEnter = transferFocusOnEnter;
    return this;
  }

  @Override
  public ComponentBuilder<V, T> enabledState(final StateObserver enabledState) {
    this.enabledState = enabledState;
    return this;
  }

  @Override
  public ComponentBuilder<V, T> onBuild(final Consumer<T> onBuild) {
    this.onBuild = onBuild;
    return this;
  }

  protected final void setPreferredSize(final T component) {
    if (preferredHeight > 0) {
      Components.setPreferredHeight(component, preferredHeight);
    }
    if (preferredWidth > 0) {
      Components.setPreferredWidth(component, preferredWidth);
    }
  }

  protected final void onBuild(final T component) {
    if (onBuild != null) {
      onBuild.accept(component);
    }
  }

  static <T extends JComponent> T setDescriptionAndEnabledState(final T component, final String description,
                                                                final StateObserver enabledState) {
    if (description != null) {
      component.setToolTipText(description);
    }
    if (enabledState != null) {
      Components.linkToEnabledState(enabledState, component);
    }

    return component;
  }
}
