/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.checkbox;

import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.ui.control.Controls;

import javax.swing.ActionMap;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ActionMapUIResource;
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
public final class NullableCheckBox extends JCheckBox {

  /**
   * Instantiates a new NullableCheckBox.
   * @param caption the caption, if any
   * @param model the model
   */
  public NullableCheckBox(final String caption, final NullableToggleButtonModel model) {
    super(caption);
    requireNonNull(model, "model");
    setModel(model);
    super.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        model.nextState();
      }
    });
    final ActionMap actions = new ActionMapUIResource();
    actions.put("pressed", Controls.control(model::nextState));
    actions.put("released", null);
    SwingUtilities.replaceUIActionMap(this, actions);
  }

  /**
   * Returns the current state, null, false or true
   * @return the current state
   */
  public Boolean get() {
    return ((NullableToggleButtonModel) getModel()).get();
  }

  /**
   * @return the underlying button model
   */
  public NullableToggleButtonModel getNullableModel() {
    return (NullableToggleButtonModel) getModel();
  }

  /**
   * Does nothing.
   * @param listener the listener
   */
  @Override
  public void addMouseListener(final MouseListener listener) {/*Disabled*/}
}
