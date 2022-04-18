/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.KeyEvents;

import javax.swing.AbstractAction;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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
   */
  public NullableCheckBox() {
    this(new NullableToggleButtonModel());
  }

  /**
   * Instantiates a new NullableCheckBox with no caption.
   * @param model the model
   */
  public NullableCheckBox(NullableToggleButtonModel model) {
    this(model, null);
  }

  /**
   * Instantiates a new NullableCheckBox.
   * @param model the model
   * @param caption the caption, if any
   */
  public NullableCheckBox(NullableToggleButtonModel model, String caption) {
    this(model, caption, null);
  }

  /**
   * Instantiates a new NullableCheckBox.
   * @param model the model
   * @param caption the caption, if any
   * @param icon the icon, if any
   */
  public NullableCheckBox(NullableToggleButtonModel model, String caption, Icon icon) {
    super(caption, icon);
    super.setModel(requireNonNull(model, "model"));
    addMouseListener(new NullableMouseListener());
    KeyEvents.builder(KeyEvent.VK_SPACE)
            .action(new NextStateAction(model))
            .enable(this);
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
  public final void setModel(ButtonModel model) {
    if (getModel() instanceof NullableToggleButtonModel) {
      throw new UnsupportedOperationException("Setting the model of a NullableCheckBox after construction is not supported");
    }
    super.setModel(model);
  }

  /**
   * Finalize this one since we call it in the constructor
   * @param listener the listener
   */
  @Override
  public final synchronized void addMouseListener(MouseListener listener) {
    super.addMouseListener(listener);
  }

  private final class NullableMouseListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      if (e == null || notModified(e)) {
        getNullableModel().nextState();
      }
    }

    private boolean notModified(MouseEvent e) {
      return !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() &&
              !e.isAltGraphDown() && !e.isMetaDown() && !e.isPopupTrigger();
    }
  }

  private static final class NextStateAction extends AbstractAction {

    private final NullableToggleButtonModel model;

    private NextStateAction(NullableToggleButtonModel model) {
      this.model = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      model.nextState();
    }
  }
}
