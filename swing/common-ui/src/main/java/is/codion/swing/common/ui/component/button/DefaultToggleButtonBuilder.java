/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

import static java.util.Objects.requireNonNull;

class DefaultToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>>
        extends AbstractButtonBuilder<Boolean, C, B> implements ToggleButtonBuilder<C, B> {

  private final ToggleControl toggleControl;

  DefaultToggleButtonBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
    this.toggleControl = null;
  }

  DefaultToggleButtonBuilder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    super(linkedValue);
    this.toggleControl = requireNonNull(toggleControl);
    action(toggleControl);
  }

  protected JToggleButton createToggleButton() {
    return new JToggleButton();
  }

  @Override
  protected final C createButton() {
    JToggleButton toggleButton = createToggleButton();
    if (toggleControl != null) {
      toggleButton.setModel(createButtonModel(toggleControl));
    }

    return (C) toggleButton;
  }

  @Override
  protected final ComponentValue<Boolean, C> createComponentValue(JToggleButton component) {
    if (component instanceof NullableCheckBox) {
      return (ComponentValue<Boolean, C>) new BooleanNullableCheckBoxValue((NullableCheckBox) component);
    }

    return (ComponentValue<Boolean, C>) new BooleanToggleButtonValue<>(component);
  }

  @Override
  protected final void setInitialValue(C component, Boolean initialValue) {
    component.setSelected(initialValue);
  }

  static ButtonModel createButtonModel(ToggleControl toggleControl) {
    ButtonModel buttonModel = toggleControl.value().isNullable() ?
            new NullableToggleButtonModel(toggleControl.value().get()) : createToggleButtonModel(toggleControl.value().get());
    buttonModel.setEnabled(toggleControl.enabledObserver().get());
    toggleControl.enabledObserver().addDataListener(buttonModel::setEnabled);
    new BooleanButtonModelValue(buttonModel).link(toggleControl.value());

    return buttonModel;
  }

  private static ToggleButtonModel createToggleButtonModel(boolean selected) {
    ToggleButtonModel buttonModel = new ToggleButtonModel();
    buttonModel.setSelected(selected);

    return buttonModel;
  }

  private static final class BooleanButtonModelValue extends AbstractValue<Boolean> {

    private final ButtonModel buttonModel;

    private BooleanButtonModelValue(ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(itemEvent -> notifyValueChange());
    }

    @Override
    public Boolean get() {
      if (buttonModel instanceof NullableToggleButtonModel) {
        return ((NullableToggleButtonModel) buttonModel).getState();
      }

      return buttonModel.isSelected();
    }

    @Override
    protected void setValue(Boolean value) {
      if (SwingUtilities.isEventDispatchThread()) {
        setModelValue(value);
      }
      else {
        try {
          SwingUtilities.invokeAndWait(() -> setModelValue(value));
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        }
        catch (InvocationTargetException e) {
          Throwable cause = e.getCause();
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          }

          throw new RuntimeException(cause);
        }
        catch (RuntimeException e) {
          throw e;
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    private void setModelValue(Boolean value) {
      if (buttonModel instanceof NullableToggleButtonModel) {
        ((NullableToggleButtonModel) buttonModel).setState(value);
      }
      else {
        buttonModel.setSelected(value != null && value);
      }
    }
  }
}
