/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.checkbox;

import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.control.Control;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static is.codion.swing.common.ui.KeyEvents.addKeyEvent;
import static java.util.Objects.requireNonNull;

/**
 * A JCheckBox implementation, which allows null values, via {@link NullableToggleButtonModel}.
 *
 * Heavily influenced by TristateCheckBox by Heinz M. Kabutz
 * http://www.javaspecialists.eu/archive/Issue145.html
 */
public class NullableCheckBox extends JCheckBox {

  /**
   * Instantiates a new NullableCheckBox with no caption.
   * @param model the model
   */
  public NullableCheckBox(final NullableToggleButtonModel model) {
    this(model, null);
  }

  /**
   * Instantiates a new NullableCheckBox.
   * @param model the model
   * @param caption the caption, if any
   */
  public NullableCheckBox(final NullableToggleButtonModel model, final String caption) {
    this(model, caption, null);
  }

  /**
   * Instantiates a new NullableCheckBox.
   * @param model the model
   * @param caption the caption, if any
   * @param icon the icon, if any
   */
  public NullableCheckBox(final NullableToggleButtonModel model, final String caption, final Icon icon) {
    super(caption, icon);
    super.setModel(requireNonNull(model, "model"));
    addMouseListener(new NullableMouseListener());
    addKeyEvent(this, KeyEvent.VK_SPACE, 0, Control.control(model::nextState));
  }

  /**
   * Returns the current state, null, false or true
   * @return the current state
   */
  public final Boolean getState() {
    return getNullableModel().getState();
  }

  /**
   * @return the underlying button model
   */
  public final NullableToggleButtonModel getNullableModel() {
    return (NullableToggleButtonModel) getModel();
  }

  /**
   * Disabled.
   * @param model the model
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setModel(final ButtonModel model) {
    if (getModel() instanceof NullableToggleButtonModel) {
      throw new UnsupportedOperationException("Setting the model of a NullableCheckBox after construction is not supported");
    }
    super.setModel(model);
  }

  private final class NullableMouseListener extends MouseAdapter {
    @Override
    public void mouseClicked(final MouseEvent e) {
      if (e == null || notModified(e)) {
        getNullableModel().nextState();
      }
    }

    private boolean notModified(final MouseEvent e) {
      return !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() &&
              !e.isAltGraphDown() && !e.isMetaDown() && !e.isPopupTrigger();
    }
  }
}
