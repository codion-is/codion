/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.control.Control;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

abstract class AbstractButtonBuilder<T, C extends AbstractButton, B extends ButtonBuilder<T, C, B>>
        extends AbstractComponentBuilder<T, C, B> implements ButtonBuilder<T, C, B> {

  private final List<ActionListener> actionListeners = new ArrayList<>();

  private String text;
  private int mnemonic;
  private boolean includeText = true;
  private int horizontalAlignment = SwingConstants.CENTER;
  private Icon icon;
  private Insets insets;
  private ButtonGroup buttonGroup;
  private boolean selected = false;
  private Action action;

  protected AbstractButtonBuilder(Value<T> linkedValue) {
    super(linkedValue);
  }

  @Override
  public final B text(String text) {
    this.text = text;
    return (B) this;
  }

  @Override
  public final B mnemonic(int mnemonic) {
    this.mnemonic = mnemonic;
    return (B) this;
  }

  @Override
  public final B includeText(boolean includeText) {
    this.includeText = includeText;
    return (B) this;
  }

  @Override
  public final B horizontalAlignment(int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return (B) this;
  }

  @Override
  public final B icon(Icon icon) {
    this.icon = icon;
    return (B) this;
  }

  @Override
  public final B margin(Insets insets) {
    this.insets = requireNonNull(insets);
    return (B) this;
  }

  @Override
  public final B buttonGroup(ButtonGroup buttonGroup) {
    this.buttonGroup = requireNonNull(buttonGroup);
    return (B) this;
  }

  @Override
  public final B selected(boolean selected) {
    this.selected = selected;
    return (B) this;
  }

  @Override
  public final B action(Action action) {
    this.action = action;
    return (B) this;
  }

  @Override
  public final B actionListener(ActionListener actionListener) {
    this.actionListeners.add(requireNonNull(actionListener));
    return (B) this;
  }

  @Override
  protected final C createComponent() {
    C button = createButton();
    if (action != null) {
      button.setAction(action);
      action.addPropertyChangeListener(new ButtonPropertyChangeListener(button));
    }
    actionListeners.forEach(button::addActionListener);
    if (!includeText) {
      button.setText(null);
    }
    else if (text != null) {
      button.setText(text);
    }
    button.setHorizontalAlignment(horizontalAlignment);
    if (mnemonic != 0) {
      button.setMnemonic(mnemonic);
    }
    if (icon != null) {
      button.setIcon(icon);
    }
    if (insets != null) {
      button.setMargin(insets);
    }
    if (buttonGroup != null) {
      buttonGroup.add(button);
    }
    if (selected) {
      button.setSelected(true);
    }

    return button;
  }

  protected abstract C createButton();

  private static final class ButtonPropertyChangeListener implements PropertyChangeListener {

    private final AbstractButton button;

    private ButtonPropertyChangeListener(AbstractButton button) {
      this.button = requireNonNull(button);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      switch (evt.getPropertyName()) {
        case Control.BACKGROUND: {
          button.setBackground((Color) evt.getNewValue());
          break;
        }
        case Control.FOREGROUND: {
          button.setForeground((Color) evt.getNewValue());
          break;
        }
        case Control.FONT: {
          button.setFont((Font) evt.getNewValue());
          break;
        }
        case Control.MNEMONIC_KEY: {
          button.setMnemonic((Integer) evt.getNewValue());
        }
        default:
          break;
      }
    }
  }
}
