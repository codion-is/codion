/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

abstract class AbstractToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends AbstractButtonBuilder<Boolean, C, B>
        implements ToggleMenuItemBuilder<C, B> {

  private ToggleControl toggleControl;

  AbstractToggleMenuItemBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  public final B toggleControl(ToggleControl toggleControl) {
    if (requireNonNull(toggleControl).value().nullable()) {
      throw new IllegalArgumentException("A toggle menu item does not support a nullable value");
    }
    this.toggleControl = toggleControl;
    action(toggleControl);
    return (B) this;
  }

  @Override
  public final B toggleControl(ToggleControl.Builder toggleControlBuilder) {
    return toggleControl(requireNonNull(toggleControlBuilder).build());
  }

  protected abstract JMenuItem createMenuItem();

  @Override
  protected final C createButton() {
    JMenuItem menuItem = createMenuItem();
    if (toggleControl != null) {
      menuItem.setModel(createButtonModel(toggleControl));
    }

    return (C) menuItem;
  }

  @Override
  protected final ComponentValue<Boolean, C> createComponentValue(C component) {
    return new BooleanToggleButtonValue<>(component);
  }

  @Override
  protected final void setInitialValue(C component, Boolean initialValue) {
    component.setSelected(initialValue);
  }

  @Override
  protected final boolean supportsNull() {
    return false;
  }
}
