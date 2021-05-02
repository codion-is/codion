/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.AbstractComponentValue;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.common.ui.value.TextValues;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.util.Arrays;
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
   * Calling this method results in the underlying combo box model being refreshed
   * the next time this combo box is made visible.
   * Calling this method when this combo box is already visible has no effect.
   * @return this combo box
   * @see SwingEntityComboBoxModel#refresh()
   */
  public EntityComboBox refreshOnSetVisible() {
    new RefreshOnVisible(this);
    return this;
  }

  /**
   * Creates an Action which displays a dialog for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @return a Control for filtering this combo box
   */
  public Control createForeignKeyFilterControl(final ForeignKey foreignKey) {
    return Control.builder().command(() -> {
      final Collection<Entity> current = getModel().getForeignKeyFilterEntities(foreignKey);
      final int result = JOptionPane.showOptionDialog(EntityComboBox.this, createForeignKeyFilterComboBox(foreignKey),
              MESSAGES.getString("filter_by"), JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE, null, null, null);
      if (result != JOptionPane.OK_OPTION) {
        getModel().setForeignKeyFilterEntities(foreignKey, current);
      }
    }).icon(frameworkIcons().filter()).build();
  }

  /**
   * Creates a EntityComboBox for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @return an EntityComboBox for filtering this combo box
   */
  public EntityComboBox createForeignKeyFilterComboBox(final ForeignKey foreignKey) {
    return Completion.maximumMatch(new EntityComboBox(getModel().createForeignKeyFilterComboBoxModel(foreignKey)));
  }

  /**
   * Creates a {@link IntegerField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @return a {@link IntegerField} bound to the selected value
   */
  public IntegerField integerFieldSelector(final Attribute<Integer> attribute) {
    final IntegerField integerField = new IntegerField(2);
    TextFields.selectAllOnFocusGained(integerField);
    NumericalValues.integerFieldValue(integerField).link(getModel().selectorValue(attribute));

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
    NumericalValues.integerFieldValue(integerField).link(getModel().selectorValue(attribute, finder));

    return integerField;
  }

  /**
   * Creates a {@link JTextField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @return a {@link JTextField} bound to the selected value
   */
  public JTextField textFieldSelector(final Attribute<String> attribute) {
    final JTextField textField = new JTextField(2);
    TextFields.selectAllOnFocusGained(textField);
    TextValues.textValue(textField).link(getModel().selectorValue(attribute));

    return textField;
  }

  /**
   * Creates a {@link JTextField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @param finder responsible for finding the item to select by value
   * @return a {@link JTextField} bound to the selected value
   */
  public JTextField textFieldSelector(final Attribute<String> attribute, final EntityComboBoxModel.Finder<String> finder) {
    final JTextField textField = new IntegerField(2);
    TextFields.selectAllOnFocusGained(textField);
    TextValues.textValue(textField).link(getModel().selectorValue(attribute, finder));

    return textField;
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(Control.builder()
            .command(((EntityComboBoxModel) getModel())::forceRefresh)
            .name(FrameworkMessages.get(FrameworkMessages.REFRESH))
            .build());

    return popupMenu;
  }

  /**
   * A {@link is.codion.swing.common.ui.value.ComponentValue} implementation for Entity values based on a {@link EntityComboBox}.
   * @see SwingEntityComboBoxModel
   */
  public static final class ComboBoxValue extends AbstractComponentValue<Entity, EntityComboBox> {

    /**
     * Instantiates a new component value based on the EntityComboBoxModel class
     * @param comboBoxModel the combo box model
     * @param initialValue the initial value to display
     */
    public ComboBoxValue(final SwingEntityComboBoxModel comboBoxModel, final Entity initialValue) {
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

  private static final class RefreshOnVisible implements AncestorListener {

    private final EntityComboBox comboBox;

    private RefreshOnVisible(final EntityComboBox comboBox) {
      this.comboBox = comboBox;
      if (!comboBox.isShowing() && Arrays.stream(comboBox.getAncestorListeners())
              .noneMatch(listener -> listener instanceof RefreshOnVisible)) {
        this.comboBox.addAncestorListener(this);
      }
    }

    @Override
    public void ancestorAdded(final AncestorEvent event) {
      comboBox.getModel().refresh();
      comboBox.removeAncestorListener(this);
    }

    @Override
    public void ancestorRemoved(final AncestorEvent event) {/*Not necessary*/}

    @Override
    public void ancestorMoved(final AncestorEvent event) {/*Not necessary*/}
  }
}
