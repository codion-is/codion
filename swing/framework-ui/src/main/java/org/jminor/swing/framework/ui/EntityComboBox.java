/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.input.AbstractInputProvider;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A UI component based on the SwingEntityComboBoxModel.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends SteppedComboBox<Entity> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityComboBox.class.getName(), Locale.getDefault());

  /**
   * Instantiates a new EntityComboBox
   * @param model the SwingEntityComboBoxModel
   */
  public EntityComboBox(final SwingEntityComboBoxModel model) {
    super(model);
    setComponentPopupMenu(initializePopupMenu());
  }

  /** {@inheritDoc} */
  @Override
  public SwingEntityComboBoxModel getModel() {
    return (SwingEntityComboBoxModel) super.getModel();
  }

  /**
   * Creates an Action which displays a dialog for filtering this combo box via a foreign key
   * @param foreignKeyPropertyId the ID of the foreign key property on which to filter
   * @return a Control for filtering this combo box
   */
  public Control createForeignKeyFilterControl(final String foreignKeyPropertyId) {
    return Controls.control(() -> {
      final Collection<Entity> current = getModel().getForeignKeyFilterEntities(foreignKeyPropertyId);
      final int result = JOptionPane.showOptionDialog(EntityComboBox.this, createForeignKeyFilterComboBox(foreignKeyPropertyId),
              MESSAGES.getString("filter_by"), JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE, null, null, null);
      if (result != JOptionPane.OK_OPTION) {
        getModel().setForeignKeyFilterEntities(foreignKeyPropertyId, current);
      }
    }, null, null, null, 0, null, Images.loadImage(Images.IMG_FILTER_16));
  }

  /**
   * Creates a EntityComboBox for filtering this combo box via a foreign key
   * @param foreignKeyPropertyId the ID of the foreign key property on which to filter
   * @return an EntityComboBox for filtering this combo box
   */
  public EntityComboBox createForeignKeyFilterComboBox(final String foreignKeyPropertyId) {
    final EntityComboBox comboBox = new EntityComboBox(getModel().createForeignKeyFilterComboBoxModel(foreignKeyPropertyId));
    MaximumMatch.enable(comboBox);

    return comboBox;
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(Controls.control(((EntityComboBoxModel) getModel())::forceRefresh, FrameworkMessages.get(FrameworkMessages.REFRESH)));

    return popupMenu;
  }

  /**
   * A InputProvider implementation for Entity values based on a {@link EntityComboBox}.
   * @see SwingEntityComboBoxModel
   */
  public static final class InputProvider extends AbstractInputProvider<Entity, EntityComboBox> {

    /**
     * Instantiates a new input provider based on the EntityComboBoxModel class
     * @param comboBoxModel the combo box model
     * @param initialValue the initial value to display
     */
    public InputProvider(final SwingEntityComboBoxModel comboBoxModel, final Entity initialValue) {
      super(createComboBox(comboBoxModel, initialValue));
    }

    /** {@inheritDoc} */
    @Override
    public Entity getValue() {
      return getInputComponent().getModel().getSelectedValue();
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Entity value) {
      getInputComponent().setSelectedItem(value);
    }

    private static EntityComboBox createComboBox(final SwingEntityComboBoxModel comboBoxModel, final Object currentValue) {
      if (comboBoxModel.isCleared()) {
        comboBoxModel.refresh();
      }
      if (currentValue != null) {
        comboBoxModel.setSelectedItem(currentValue);
      }

      return new EntityComboBox(comboBoxModel);
    }
  }
}
