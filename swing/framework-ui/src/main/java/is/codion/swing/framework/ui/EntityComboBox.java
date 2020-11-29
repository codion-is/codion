/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.ui.combobox.MaximumMatch;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.AbstractComponentValue;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.util.Collection;
import java.util.ResourceBundle;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;

/**
 * A UI component based on the SwingEntityComboBoxModel.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends SteppedComboBox<Entity> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityComboBox.class.getName());

  /**
   * Instantiates a new EntityComboBox
   * @param model the SwingEntityComboBoxModel
   */
  public EntityComboBox(final SwingEntityComboBoxModel model) {
    super(model);
    setComponentPopupMenu(initializePopupMenu());
  }

  @Override
  public SwingEntityComboBoxModel getModel() {
    return (SwingEntityComboBoxModel) super.getModel();
  }

  /**
   * Creates an Action which displays a dialog for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @return a Control for filtering this combo box
   */
  public Control createForeignKeyFilterControl(final ForeignKey foreignKey) {
    return Controls.control(() -> {
      final Collection<Entity> current = getModel().getForeignKeyFilterEntities(foreignKey);
      final int result = JOptionPane.showOptionDialog(EntityComboBox.this, createForeignKeyFilterComboBox(foreignKey),
              MESSAGES.getString("filter_by"), JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE, null, null, null);
      if (result != JOptionPane.OK_OPTION) {
        getModel().setForeignKeyFilterEntities(foreignKey, current);
      }
    }, null, null, null, 0, null, frameworkIcons().filter());
  }

  /**
   * Creates a EntityComboBox for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @return an EntityComboBox for filtering this combo box
   */
  public EntityComboBox createForeignKeyFilterComboBox(final ForeignKey foreignKey) {
    final EntityComboBox comboBox = new EntityComboBox(getModel().createForeignKeyFilterComboBoxModel(foreignKey));
    MaximumMatch.enable(comboBox);

    return comboBox;
  }

  /**
   * Creates a {@link IntegerField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @return a {@link IntegerField} bound to the selected value
   */
  public IntegerField integerFieldSelector(final Attribute<Integer> attribute) {
    final IntegerField integerField = new IntegerField(2);
    TextFields.selectAllOnFocusGained(integerField);
    NumericalValues.integerValue(integerField).link(getModel().integerValueSelector(attribute));

    return integerField;
  }

  /**
   * Creates a {@link IntegerField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @param finder responsible for finding the item to select by value
   * @return a {@link IntegerField} bound to the selected value
   */
  public IntegerField integerFieldSelector(final Attribute<Integer> attribute, final EntityComboBoxModel.Finder<Integer> finder) {
    final IntegerField integerField = new IntegerField(2);
    TextFields.selectAllOnFocusGained(integerField);
    NumericalValues.integerValue(integerField).link(getModel().integerValueSelector(attribute, finder));

    return integerField;
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(Controls.control(((EntityComboBoxModel) getModel())::forceRefresh, FrameworkMessages.get(FrameworkMessages.REFRESH)));

    return popupMenu;
  }

  /**
   * A {@link is.codion.swing.common.ui.value.ComponentValue} implementation for Entity values based on a {@link EntityComboBox}.
   * @see SwingEntityComboBoxModel
   */
  public static final class ComponentValue extends AbstractComponentValue<Entity, EntityComboBox> {

    /**
     * Instantiates a new component value based on the EntityComboBoxModel class
     * @param comboBoxModel the combo box model
     * @param initialValue the initial value to display
     */
    public ComponentValue(final SwingEntityComboBoxModel comboBoxModel, final Entity initialValue) {
      super(createComboBox(comboBoxModel, initialValue));
    }

    @Override
    protected Entity getComponentValue(final EntityComboBox component) {
      return component.getModel().getSelectedValue();
    }

    @Override
    protected void setComponentValue(final EntityComboBox component, final Entity value) {
      component.setSelectedItem(value);
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
