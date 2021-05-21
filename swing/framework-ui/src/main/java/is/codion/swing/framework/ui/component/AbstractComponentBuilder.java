/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.Components;

import javax.swing.JComponent;
import java.awt.Dimension;

import static java.util.Objects.requireNonNull;

abstract class AbstractComponentBuilder<V, T extends JComponent, B extends ComponentBuilder<V, T, B>> implements ComponentBuilder<V, T, B> {

  protected final Property<V> property;
  protected final Value<V> value;

  private final Event<T> buildEvent = Event.event();

  private int preferredHeight;
  private int preferredWidth;
  private boolean transferFocusOnEnter;
  protected StateObserver enabledState;

  protected AbstractComponentBuilder(final Property<V> attribute, final Value<V> value) {
    this.property = requireNonNull(attribute);
    this.value = requireNonNull(value);
  }

  @Override
  public final B preferredHeight(final int preferredHeight) {
    this.preferredHeight = preferredHeight;
    return (B) this;
  }

  @Override
  public final B preferredWidth(final int preferredWidth) {
    this.preferredWidth = preferredWidth;
    return (B) this;
  }

  @Override
  public final B preferredSize(final Dimension preferredSize) {
    requireNonNull(preferredSize);
    this.preferredHeight = preferredSize.height;
    this.preferredWidth = preferredSize.width;
    return (B) this;
  }

  @Override
  public final B transferFocusOnEnter(final boolean transferFocusOnEnter) {
    this.transferFocusOnEnter = transferFocusOnEnter;
    return (B) this;
  }

  @Override
  public final B enabledState(final StateObserver enabledState) {
    this.enabledState = enabledState;
    return (B) this;
  }

  @Override
  public final B addBuildListener(final EventDataListener<T> listener) {
    buildEvent.addDataListener(listener);
    return (B) this;
  }

  /**
   * Builds the component.
   * @return a new component instance
   */
  public final T build() {
    final T component = buildComponent();
    setPreferredSize(component);
    if (transferFocusOnEnter) {
      setTransferFocusOnEnter(component);
    }
    buildEvent.onEvent(component);

    return component;
  }

  /**
   * Builds the component.
   * @return a new component instance
   */
  protected abstract T buildComponent();

  /**
   * Enables focus transfer on Enter, override for special handling
   * @param component the component
   */
  protected void setTransferFocusOnEnter(final T component) {
    Components.transferFocusOnEnter(component);
  }

  private void setPreferredSize(final T component) {
    if (preferredHeight > 0) {
      Components.setPreferredHeight(component, preferredHeight);
    }
    if (preferredWidth > 0) {
      Components.setPreferredWidth(component, preferredWidth);
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
