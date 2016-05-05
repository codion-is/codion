/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.images.Images;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
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
    super((ComboBoxModel<Entity>) model);
    setComponentPopupMenu(initializePopupMenu());
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
        final Collection<Entity> current = ((EntityComboBoxModel) getModel()).getForeignKeyFilterEntities(foreignKeyPropertyID);
        final int result = JOptionPane.showOptionDialog(EntityComboBox.this, createForeignKeyFilterComboBox(foreignKeyPropertyID),
                FrameworkMessages.get(FrameworkMessages.FILTER_BY), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (result != JOptionPane.OK_OPTION) {
          ((EntityComboBoxModel) getModel()).setForeignKeyFilterEntities(foreignKeyPropertyID, current);
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
    final EntityComboBox comboBox = new EntityComboBox(((EntityComboBoxModel) getModel()).createForeignKeyFilterComboBoxModel(foreignKeyPropertyID));
    MaximumMatch.enable(comboBox);
    return comboBox;
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.REFRESH)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        ((EntityComboBoxModel) getModel()).forceRefresh();
      }
    });

    return popupMenu;
  }
}
