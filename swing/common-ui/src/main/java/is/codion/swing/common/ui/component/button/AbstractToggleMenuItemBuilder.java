/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.ButtonModel;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

abstract class AbstractToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends AbstractButtonBuilder<Boolean, C, B>
        implements ToggleMenuItemBuilder<C, B> {

  private final ToggleControl toggleControl;

  AbstractToggleMenuItemBuilder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    super(linkedValue);
    this.toggleControl = requireNonNull(toggleControl);
    caption(toggleControl.getCaption());
    mnemonic(toggleControl.getMnemonic());
    horizontalAlignment(SwingConstants.LEADING);
  }

  protected abstract JMenuItem createMenuItem();

  @Override
  protected final C createButton() {
    JMenuItem menuItem = createMenuItem();
    menuItem.setModel(createButtonModel());
    toggleControl().addPropertyChangeListener(new ButtonPropertyChangeListener(menuItem));

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

  protected final ToggleControl toggleControl() {
    return toggleControl;
  }

  private ButtonModel createButtonModel() {
    return DefaultToggleButtonBuilder.createButtonModel(toggleControl);
  }
}
