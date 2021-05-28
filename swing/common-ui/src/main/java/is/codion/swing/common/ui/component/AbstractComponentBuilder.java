/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.value.ComponentValue;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.Components.*;
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
  private Border border;
  private boolean transferFocusOnEnter = TRANSFER_FOCUS_ON_ENTER.get();
  private String description;
  private StateObserver enabledState;
  private Controls popupMenuControls;
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
  public final B maximumSize(final Dimension maximumSize) {
    this.maximumSize = maximumSize;
    return (B) this;
  }

  @Override
  public final B minimumSize(final Dimension minimumSize) {
    this.minimumSize = minimumSize;
    return (B) this;
  }

  @Override
  public final B border(final Border border) {
    this.border = border;
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
  public B popupMenuControl(final Control popupMenuControl) {
    return popupMenuControls(Controls.controls(popupMenuControl));
  }

  @Override
  public B popupMenuControls(final Controls popupMenuControls) {
    this.popupMenuControls = popupMenuControls;
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
  public final B linkedValueObserver(final ValueObserver<T> linkedValueObserver) {
    if (linkedValue != null) {
      throw new IllegalStateException("linkedValue has already been set");
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
  public final B onBuild(final Consumer<C> onBuild) {
    buildEvent.addDataListener(onBuild::accept);
    return (B) this;
  }

  @Override
  public final C build() {
    return build(null);
  }

  @Override
  public final C build(final Consumer<C> onBuild) {
    if (component != null) {
      return component;
    }
    component = buildComponent();
    if (component.isFocusable() && !focusable) {
      component.setFocusable(false);
    }
    setSizes(component);
    if (border != null) {
      component.setBorder(border);
    }
    if (enabledState != null) {
      linkToEnabledState(enabledState, component);
    }
    if (popupMenuControls != null) {
      component.setComponentPopupMenu(popupMenuControls.createPopupMenu());
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
    if (onBuild != null) {
      onBuild.accept(component);
    }

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

  /**
   * @return the enabled state
   */
  protected final StateObserver getEnabledState() {
    return enabledState;
  }

  private void setSizes(final C component) {
    if (minimumSize != null) {
      component.setMinimumSize(minimumSize);
    }
    if (maximumSize != null) {
      component.setMaximumSize(maximumSize);
    }
    if (preferredHeight > 0) {
      setPreferredHeight(component, preferredHeight);
    }
    if (preferredWidth > 0) {
      setPreferredWidth(component, preferredWidth);
    }
  }
}
