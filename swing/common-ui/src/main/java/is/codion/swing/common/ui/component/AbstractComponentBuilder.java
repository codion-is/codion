/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.value.ComponentValue;

import javax.swing.JComponent;
import java.awt.Dimension;

import static java.util.Objects.requireNonNull;

public abstract class AbstractComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>> implements ComponentBuilder<T, C, B> {

  private final Event<C> buildEvent = Event.event();

  private C component;
  private ComponentValue<T, C> componentValue;

  private boolean focusable = true;
  private int preferredHeight;
  private int preferredWidth;
  private Dimension maximumSize;
  private Dimension minimumSize;
  private boolean transferFocusOnEnter;
  protected String description;
  protected StateObserver enabledState;
  private Value<T> linkedValue;
  private ValueObserver<T> linkedValueObserver;
  private T initialValue;

  @Override
  public final B focusable(final boolean focusable) {
    this.focusable = focusable;
    return (B) this;
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
  public B maximumSize(final Dimension maximumSize) {
    this.maximumSize = maximumSize;
    return (B) this;
  }

  @Override
  public B minimumSize(final Dimension minimumSize) {
    this.minimumSize = minimumSize;
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
  public final B description(final String description) {
    this.description = description;
    return (B) this;
  }

  @Override
  public final B linkedValue(final Value<T> value) {
    if (linkedValueObserver != null) {
      throw new IllegalStateException("linkeValueObserver has already been set");
    }
    this.linkedValue = value;
    return (B) this;
  }

  @Override
  public B linkedValueObserver(final ValueObserver<T> linkedValueObserver) {
    if (linkedValueObserver != null) {
      throw new IllegalStateException("linkeValue has already been set");
    }
    this.linkedValueObserver = linkedValueObserver;
    return (B) this;
  }

  @Override
  public final B initialValue(final T initialValue) {
    this.initialValue = requireNonNull(initialValue);
    return (B) this;
  }

  @Override
  public final B addBuildListener(final EventDataListener<C> listener) {
    buildEvent.addDataListener(listener);
    return (B) this;
  }

  @Override
  public final C build() {
    if (component != null) {
      return component;
    }
    component = buildComponent();
    if (component.isFocusable() && !focusable) {
      component.setFocusable(false);
    }
    setSizes(component);
    if (enabledState != null) {
      Components.linkToEnabledState(enabledState, component);
    }
    if (description != null) {
      component.setToolTipText(description);
    }
    if (transferFocusOnEnter) {
      setTransferFocusOnEnter(component);
    }
    if (initialValue != null) {
      setInitialValue(component, initialValue);
    }
    if (linkedValue != null) {
      buildComponentValue().link(linkedValue);
    }
    if (linkedValueObserver != null) {
      buildComponentValue().link(linkedValueObserver);
    }
    buildEvent.onEvent(component);

    return component;
  }

  @Override
  public final ComponentValue<T, C> buildComponentValue() {
    if (componentValue != null) {
      return componentValue;
    }
    componentValue = buildComponentValue(build());

    return componentValue;
  }

  /**
   * Builds the component.
   * @return a new component instance
   */
  protected abstract C buildComponent();

  /**
   * @param component the component
   * @return a component value based on the component
   */
  protected abstract ComponentValue<T, C> buildComponentValue(final C component);

  /**
   * Sets the initial value in the component, only called for non-null values.
   * @param component the component
   * @param initialValue the initial value, not null
   */
  protected abstract void setInitialValue(final C component, final T initialValue);

  /**
   * Enables focus transfer on Enter, override for special handling
   * @param component the component
   */
  protected void setTransferFocusOnEnter(final C component) {
    Components.transferFocusOnEnter(component);
  }

  private void setSizes(final C component) {
    if (minimumSize != null) {
      component.setMinimumSize(minimumSize);
    }
    if (maximumSize != null) {
      component.setMaximumSize(maximumSize);
    }
    if (preferredHeight > 0) {
      Components.setPreferredHeight(component, preferredHeight);
    }
    if (preferredWidth > 0) {
      Components.setPreferredWidth(component, preferredWidth);
    }
  }
}
