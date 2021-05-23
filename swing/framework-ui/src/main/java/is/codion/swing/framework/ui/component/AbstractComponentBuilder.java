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

abstract class AbstractComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>> implements ComponentBuilder<T, C, B> {

  protected final Property<T> property;
  protected final Value<T> value;

  private final Event<C> buildEvent = Event.event();

  private boolean focusable = true;
  private int preferredHeight;
  private int preferredWidth;
  private boolean transferFocusOnEnter;
  protected StateObserver enabledState;

  protected AbstractComponentBuilder(final Property<T> attribute, final Value<T> value) {
    this.property = requireNonNull(attribute);
    this.value = requireNonNull(value);
  }

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
  public final B addBuildListener(final EventDataListener<C> listener) {
    buildEvent.addDataListener(listener);
    return (B) this;
  }

  /**
   * Builds the component.
   * @return a new component instance
   */
  public final C build() {
    final C component = buildComponent();
    if (component.isFocusable() && !focusable) {
      component.setFocusable(false);
    }
    setPreferredSize(component);
    setDescriptionAndEnabledState(component);
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
  protected abstract C buildComponent();

  /**
   * Enables focus transfer on Enter, override for special handling
   * @param component the component
   */
  protected void setTransferFocusOnEnter(final C component) {
    Components.transferFocusOnEnter(component);
  }

  /**
   * @param component the component
   * @return a description for the component
   */
  protected String getDescription(final C component) {
    return property.getDescription();
  }

  private void setPreferredSize(final C component) {
    if (preferredHeight > 0) {
      Components.setPreferredHeight(component, preferredHeight);
    }
    if (preferredWidth > 0) {
      Components.setPreferredWidth(component, preferredWidth);
    }
  }

  private C setDescriptionAndEnabledState(final C component) {
    if (property.getDescription() != null) {
      component.setToolTipText(getDescription(component));
    }
    if (enabledState != null) {
      Components.linkToEnabledState(enabledState, component);
    }

    return component;
  }
}
