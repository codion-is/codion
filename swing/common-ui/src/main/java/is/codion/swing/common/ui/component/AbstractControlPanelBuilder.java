/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.button.ToggleButtonType;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

abstract class AbstractControlPanelBuilder<C extends JComponent, B extends ControlPanelBuilder<C, B>>
        extends AbstractComponentBuilder<Void, C, B> implements ControlPanelBuilder<C, B> {

  private final Controls controls = Controls.controls();

  private int orientation = SwingConstants.HORIZONTAL;
  private ToggleButtonType toggleButtonType = ToggleButtonType.BUTTON;

  protected AbstractControlPanelBuilder(Controls controls) {
    if (controls != null) {
      this.controls.addAll(controls);
    }
  }

  @Override
  public final B orientation(int orientation) {
    if (orientation != SwingConstants.VERTICAL && orientation != SwingConstants.HORIZONTAL) {
      throw new IllegalArgumentException("Unknown orientation value: " + orientation);
    }
    this.orientation = orientation;
    return (B) this;
  }

  @Override
  public final B action(Action action) {
    this.controls.add(requireNonNull(action));
    return (B) this;
  }

  @Override
  public final B controls(Controls controls) {
    this.controls.addAll(requireNonNull(controls));
    return (B) this;
  }

  @Override
  public final B separator() {
    this.controls.addSeparator();
    return (B) this;
  }

  @Override
  public final B toggleButtonType(ToggleButtonType toggleButtonType) {
    this.toggleButtonType = requireNonNull(toggleButtonType);
    return (B) this;
  }

  @Override
  protected final ComponentValue<Void, C> createComponentValue(C component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on this component type");
  }

  @Override
  protected final void setInitialValue(C component, Void initialValue) {}

  protected final Controls controls() {
    return controls;
  }

  protected final int orientation() {
    return orientation;
  }

  protected final ToggleButtonType toggleButtonType() {
    return toggleButtonType;
  }
}
