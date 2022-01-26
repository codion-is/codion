/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;

import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import static java.util.Objects.requireNonNull;

/**
 * A default ToggleControl implementation.
 */
final class DefaultToggleControl extends AbstractControl implements ToggleControl {

  private final Value<Boolean> value;

  /**
   * @param name the name
   * @param value the value to toggle
   * @param enabledObserver an observer indicating when this control should be enabled
   */
  DefaultToggleControl(final String name, final Value<Boolean> value, final StateObserver enabledObserver) {
    super(name, enabledObserver);
    this.value = requireNonNull(value, "value");
  }

  @Override
  public Value<Boolean> getValue() {
    return value;
  }

  @Override
  public JCheckBoxMenuItem createCheckBoxMenuItem() {
    if (getValue().isNullable()) {
      throw new IllegalArgumentException("A check box menu item does not support a nullable value");
    }
    final ButtonModel buttonModel = createButtonModel();
    final JCheckBoxMenuItem item = new JCheckBoxMenuItem(this) {
      @Override
      protected void processMouseEvent(final MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED && e.isControlDown()) {
          buttonModel.setSelected(!buttonModel.isSelected());
        }
        else {
          super.processMouseEvent(e);
        }
      }
    };
    item.setModel(buttonModel);
    addPropertyChangeListener(new ButtonPropertyChangeListener(item));

    return item;
  }

  @Override
  public JRadioButtonMenuItem createRadioButtonMenuItem() {
    if (getValue().isNullable()) {
      throw new IllegalArgumentException("A check box menu item does not support a nullable value");
    }
    final ButtonModel buttonModel = createButtonModel();
    final JRadioButtonMenuItem item = new JRadioButtonMenuItem(this) {
      @Override
      protected void processMouseEvent(final MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED && e.isControlDown()) {
          buttonModel.setSelected(!buttonModel.isSelected());
        }
        else {
          super.processMouseEvent(e);
        }
      }
    };
    item.setModel(buttonModel);
    addPropertyChangeListener(new ButtonPropertyChangeListener(item));

    return item;
  }

  @Override
  public ButtonModel createButtonModel() {
    final ButtonModel buttonModel;
    if (value.isNullable()) {
      buttonModel = new NullableToggleButtonModel(value.get());
    }
    else {
      buttonModel = new JToggleButton.ToggleButtonModel();
    }
    new BooleanButtonModelValue(buttonModel).link(value);
    buttonModel.setEnabled(getEnabledObserver().get());
    getEnabledObserver().addDataListener(buttonModel::setEnabled);
    addPropertyChangeListener(changeEvent -> {
      if (Action.MNEMONIC_KEY.equals(changeEvent.getPropertyName())) {
        buttonModel.setMnemonic((Integer) changeEvent.getNewValue());
      }
    });

    return buttonModel;
  }

  @Override
  public JCheckBox createCheckBox() {
    final JCheckBox checkBox;
    final ButtonModel buttonModel = createButtonModel();
    if (buttonModel instanceof NullableToggleButtonModel) {
      checkBox = new NullableCheckBox((NullableToggleButtonModel) buttonModel, getCaption());
    }
    else {
      checkBox = new JCheckBox(this);
      checkBox.setModel(buttonModel);
    }
    checkBox.setMnemonic(getMnemonic());
    addPropertyChangeListener(new ButtonPropertyChangeListener(checkBox));

    return checkBox;
  }

  @Override
  public JToggleButton createToggleButton() {
    final JToggleButton toggleButton = new JToggleButton(this);
    toggleButton.setModel(createButtonModel());
    toggleButton.setText(getCaption());
    toggleButton.setMnemonic(getMnemonic());
    addPropertyChangeListener(new ButtonPropertyChangeListener(toggleButton));

    return toggleButton;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {/*Not required*/}

  private static final class BooleanButtonModelValue extends AbstractValue<Boolean> {

    private final ButtonModel buttonModel;

    BooleanButtonModelValue(final ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(itemEvent -> notifyValueChange());
    }

    @Override
    protected void setValue(final Boolean value) {
      if (SwingUtilities.isEventDispatchThread()) {
        setModelValue(value);
      }
      else {
        try {
          SwingUtilities.invokeAndWait(() -> setModelValue(value));
        }
        catch (final InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        }
        catch (final InvocationTargetException e) {
          final Throwable cause = e.getCause();
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          }

          throw new RuntimeException(cause);
        }
        catch (final RuntimeException e) {
          throw e;
        }
        catch (final Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public Boolean get() {
      if (buttonModel instanceof NullableToggleButtonModel) {
        return ((NullableToggleButtonModel) buttonModel).getState();
      }

      return buttonModel.isSelected();
    }

    private void setModelValue(final Boolean value) {
      if (buttonModel instanceof NullableToggleButtonModel) {
        ((NullableToggleButtonModel) buttonModel).setState(value);
      }
      else {
        buttonModel.setSelected(value != null && value);
      }
    }
  }
}
