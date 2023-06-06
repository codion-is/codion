/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

class DefaultToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>>
        extends AbstractButtonBuilder<Boolean, C, B> implements ToggleButtonBuilder<C, B> {

  private final ToggleControl toggleControl;

  DefaultToggleButtonBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
    this.toggleControl = null;
  }

  DefaultToggleButtonBuilder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    super(linkedValue);
    this.toggleControl = toggleControl;
    text(toggleControl.getName());
    mnemonic(toggleControl.getMnemonic());
  }

  protected JToggleButton createToggleButton() {
    return new JToggleButton();
  }

  @Override
  protected final C createButton() {
    JToggleButton toggleButton = createToggleButton();
    if (toggleControl != null) {
      toggleButton.setModel(createButtonModel(toggleControl));
      toggleControl.addPropertyChangeListener(new ButtonPropertyChangeListener(toggleButton));
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
    ButtonModel buttonModel;
    if (toggleControl.value().isNullable()) {
      buttonModel = new NullableToggleButtonModel(toggleControl.value().get());
    }
    else {
      buttonModel = new JToggleButton.ToggleButtonModel();
    }
    buttonModel.setEnabled(toggleControl.enabledObserver().get());
    toggleControl.enabledObserver().addDataListener(buttonModel::setEnabled);
    new BooleanButtonModelValue(buttonModel).link(toggleControl.value());
    toggleControl.addPropertyChangeListener(changeEvent -> {
      if (Action.MNEMONIC_KEY.equals(changeEvent.getPropertyName())) {
        buttonModel.setMnemonic((Integer) changeEvent.getNewValue());
      }
    });

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
