/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.swing.ui;

import org.jminor.common.swing.ui.combobox.MaximumMatch;
import org.jminor.common.swing.ui.combobox.SteppedComboBox;
import org.jminor.common.swing.ui.images.Images;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.swing.model.EntityComboBoxModel;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * A UI component based on the EntityComboBoxModel.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends SteppedComboBox<Entity> {

  /**
   * Instantiates a new EntityComboBox
   * @param model the EntityComboBoxModel
   */
  public EntityComboBox(final EntityComboBoxModel model) {
    super(model);
    setComponentPopupMenu(initializePopupMenu());
  }

  /** {@inheritDoc} */
  @Override
  public EntityComboBoxModel getModel() {
    return (EntityComboBoxModel) super.getModel();
  }

  /**
   * Creates an Action which displays a dialog for filtering this combo box via a foreign key
   * @param foreignKeyPropertyID the ID of the foreign key property on which to filter
   * @return an Action for filtering this combo box
   */
  public Action createForeignKeyFilterAction(final String foreignKeyPropertyID) {
    return new AbstractAction(null, Images.loadImage(Images.IMG_FILTER_16)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final Collection<Entity> current = getModel().getForeignKeyFilterEntities(foreignKeyPropertyID);
        final int result = JOptionPane.showOptionDialog(EntityComboBox.this, createForeignKeyFilterComboBox(foreignKeyPropertyID),
                FrameworkMessages.get(FrameworkMessages.FILTER_BY), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (result != JOptionPane.OK_OPTION) {
          getModel().setForeignKeyFilterEntities(foreignKeyPropertyID, current);
        }
      }
    };
  }

  /**
   * Creates a EntityComboBox for filtering this combo box via a foreign key
   * @param foreignKeyPropertyID the ID of the foreign key property on which to filter
   * @return an EntityComboBox for filtering this combo box
   */
  public EntityComboBox createForeignKeyFilterComboBox(final String foreignKeyPropertyID) {
    final EntityComboBox comboBox = new EntityComboBox(getModel().createForeignKeyFilterComboBoxModel(foreignKeyPropertyID));
    MaximumMatch.enable(comboBox);
    return comboBox;
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.REFRESH)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getModel().forceRefresh();
      }
    });

    return popupMenu;
  }
}
