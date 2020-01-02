/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.checkbox;

import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;

import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ActionMapUIResource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static java.util.Objects.requireNonNull;
import static org.jminor.swing.common.ui.control.Controls.control;

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
    super.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        requestFocusInWindow();
        model.nextState();
      }
    });
    final ActionMap actions = new ActionMapUIResource();
    actions.put("pressed", control(model::nextState));
    actions.put("released", null);
    SwingUtilities.replaceUIActionMap(this, actions);
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
      throw new UnsupportedOperationException("Setting the model of a NullableCheckBox is not supported");
    }
    super.setModel(model);
  }

  /**
   * Does nothing.
   * @param listener the listener
   */
  @Override
  public final void addMouseListener(final MouseListener listener) {/*Disabled*/}
}
