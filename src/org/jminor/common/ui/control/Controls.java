/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * A factory class for Control objects.
 */
public final class Controls {

  private Controls() {}

  public static MethodControl methodControl(final Object owner, final String methodName, final Icon icon) {
    return methodControl(owner, methodName, null, null, null, -1, null, icon);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String methodName) {
    return methodControl(owner, method, methodName, null);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String methodName,
                                            final StateObserver state) {
    return new MethodControl(methodName, owner, method, state);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String methodName,
                                            final StateObserver state, final String description) {
    return (MethodControl) methodControl(owner, method, methodName, state).setDescription(description);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String methodName,
                                            final StateObserver state, final String description, final int mnemonic) {
    return (MethodControl) methodControl(owner, method, methodName, state, description).setMnemonic(mnemonic);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String methodName,
                                            final StateObserver state, final String description, final int mnemonic, final KeyStroke ks) {
    return (MethodControl) methodControl(owner, method, methodName, state, description, mnemonic).setKeyStroke(ks);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String methodName,
                                            final StateObserver state, final String description, final int mnemonic,
                                            final KeyStroke ks, final Icon icon) {
    return (MethodControl) methodControl(owner, method, methodName, state, description, mnemonic, ks).setIcon(icon);
  }

  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption) {
    return toggleControl(owner, beanPropertyName, caption, null);
  }

  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver changeEvent) {
    return toggleControl(owner, beanPropertyName, caption, changeEvent, null);
  }

  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver changeEvent, final StateObserver enabledObserver) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    Values.link(Values.<Boolean>beanValue(owner, beanPropertyName, boolean.class, changeEvent,
            false), new BooleanValue(buttonModel), false);

    return new ToggleControl(caption, buttonModel, enabledObserver);
  }

  /**
   * So as to not introduce a dependency to common.ui
   */
  private static final class BooleanValue implements Value<Boolean> {
    private final ButtonModel buttonModel;
    private final Event changeEvent = Events.event();

    private BooleanValue(final ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(new ItemListener() {
        /** {@inheritDoc} */
        @Override
        public void itemStateChanged(final ItemEvent e) {
          changeEvent.fire();
        }
      });
    }

    /** {@inheritDoc} */
    @Override
    public Boolean get() {
      return buttonModel.isSelected();
    }

    @Override
    public final void set(final Boolean value) {
      if (SwingUtilities.isEventDispatchThread()) {
        buttonModel.setSelected(value != null && value);
      }
      else {
        try {
          SwingUtilities.invokeAndWait(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
              buttonModel.setSelected(value != null && value);
            }
          });
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public final EventObserver getChangeEvent() {
      return changeEvent.getObserver();
    }
  }
}
