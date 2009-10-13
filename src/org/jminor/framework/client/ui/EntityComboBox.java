/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * An EntityComboBox provides a button for creating a new entity
 * Use <code>createPanel()</code> to create a panel which includes the EntityComboBox and the button
 */
public class EntityComboBox extends SteppedComboBox {

  /**
   * Instantiates a new EntityComboBox
   * @param model the EntityComboBoxModel
   */
  public EntityComboBox(final EntityComboBoxModel model) {
    super(model);
    setComponentPopupMenu(initializePopupMenu());
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension dimension = super.getPreferredSize();
    dimension.setSize(new Dimension(dimension.width, UiUtil.getPreferredTextFieldHeight()));

    return dimension;
  }

  @Override
  public EntityComboBoxModel getModel() {
    return (EntityComboBoxModel) super.getModel();
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.REFRESH)) {
      public void actionPerformed(ActionEvent e) {
        getModel().forceRefresh();
      }
    });

    return popupMenu;
  }
}
