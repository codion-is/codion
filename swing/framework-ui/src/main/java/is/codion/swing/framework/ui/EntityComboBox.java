/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel.ItemFinder;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.combobox.DefaultComboBoxBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.EntityComboBoxModel;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A UI component based on the {@link EntityComboBoxModel}.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends JComboBox<Entity> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityComboBox.class.getName());

  /**
   * Instantiates a new EntityComboBox
   * @param model the {@link EntityComboBoxModel}
   */
  EntityComboBox(EntityComboBoxModel model) {
    super(model);
  }

  @Override
  public EntityComboBoxModel getModel() {
    return (EntityComboBoxModel) super.getModel();
  }

  /**
   * Overridden as a workaround for editable combo boxes as initial focus components on
   * detail panels stealing the focus from the parent panel on initialization
   */
  @Override
  public void requestFocus() {
    if (isEditable()) {
      getEditor().getEditorComponent().requestFocus();
    }
    else {
      super.requestFocus();
    }
  }

  @Override
  public synchronized void addFocusListener(FocusListener listener) {
    super.addFocusListener(listener);
    if (isEditable()) {
      getEditor().getEditorComponent().addFocusListener(listener);
    }
  }

  @Override
  public synchronized void removeFocusListener(FocusListener listener) {
    super.removeFocusListener(listener);
    if (isEditable()) {
      getEditor().getEditorComponent().removeFocusListener(listener);
    }
  }

  /**
   * Creates an Action which displays a dialog for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @return a Control for filtering this combo box
   */
  public Control createForeignKeyFilterControl(ForeignKey foreignKey) {
    return Control.builder(createForeignKeyFilterCommand(requireNonNull(foreignKey)))
            .smallIcon(FrameworkIcons.instance().filter())
            .build();
  }

  /**
   * Creates a {@link ComboBoxBuilder} returning a combo box for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @param <B> the builder type
   * @return a {@link ComboBoxBuilder} for a foreign key filter combo box
   */
  public <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> createForeignKeyFilterComboBox(
          ForeignKey foreignKey) {
    return (B) builder(getModel().createForeignKeyFilterComboBoxModel(requireNonNull(foreignKey)))
            .completionMode(Completion.Mode.MAXIMUM_MATCH);
  }

  /**
   * Creates a {@link TextFieldBuilder} returning a {@link NumberField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a {@link NumberField} builder bound to the selected value
   */
  public <B extends TextFieldBuilder<Integer, NumberField<Integer>, B>> TextFieldBuilder<Integer, NumberField<Integer>, B> integerSelectorField(
          Attribute<Integer> attribute) {
    requireNonNull(attribute);
    return (B) Components.integerField(getModel().createSelectorValue(attribute))
            .columns(2)
            .selectAllOnFocusGained(true);
  }

  /**
   * Creates a {@link TextFieldBuilder} returning a {@link NumberField} which value is bound to the selected value in this combo box
   * @param itemFinder responsible for finding the item to select by value
   * @param <B> the builder type
   * @return a {@link NumberField} builder bound to the selected value
   */
  public <B extends TextFieldBuilder<Integer, NumberField<Integer>, B>> TextFieldBuilder<Integer, NumberField<Integer>, B> integerSelectorField(
          ItemFinder<Entity, Integer> itemFinder) {
    requireNonNull(itemFinder);
    return (B) Components.integerField(getModel().createSelectorValue(itemFinder))
            .columns(2)
            .selectAllOnFocusGained(true);
  }

  /**
   * Creates a {@link TextFieldBuilder} returning a text field which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a {@link JTextField} builder bound to the selected value
   */
  public <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> stringSelectorField(
          Attribute<String> attribute) {
    requireNonNull(attribute);
    return (B) Components.textField(getModel().createSelectorValue(attribute))
            .columns(2)
            .selectAllOnFocusGained(true);
  }

  /**
   * Creates a {@link TextFieldBuilder} returning a text field which value is bound to the selected value in this combo box
   * @param itemFinder responsible for finding the item to select by value
   * @param <B> the builder type
   * @return a {@link JTextField} builder bound to the selected value
   */
  public <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> stringSelectorField(
          ItemFinder<Entity, String> itemFinder) {
    requireNonNull(itemFinder);
    return (B) Components.textField(getModel().createSelectorValue(itemFinder))
            .columns(2)
            .selectAllOnFocusGained(true);
  }

  /**
   * Instantiates a new {@link EntityComboBox} builder
   * @param comboBoxModel the combo box model
   * @param <B> the builder type
   * @return a builder for a {@link EntityComboBox}
   */
  public static <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> builder(EntityComboBoxModel comboBoxModel) {
    return builder(comboBoxModel, null);
  }

  /**
   * Instantiates a new {@link EntityComboBox} builder
   * @param comboBoxModel the combo box model
   * @param linkedValue the linked value
   * @param <B> the builder type
   * @return a builder for a {@link EntityComboBox}
   */
  public static <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> builder(EntityComboBoxModel comboBoxModel,
                                                                                                                          Value<Entity> linkedValue) {
    return new DefaultBuilder<>(comboBoxModel, linkedValue);
  }

  private Control.Command createForeignKeyFilterCommand(ForeignKey foreignKey) {
    return () -> {
      Collection<Key> currentFilterKeys = getModel().getForeignKeyFilterKeys(foreignKey);
      Dialogs.okCancelDialog(createForeignKeyFilterComboBox(foreignKey).build())
              .owner(this)
              .title(MESSAGES.getString("filter_by"))
              .onCancel(() -> getModel().setForeignKeyFilterKeys(foreignKey, currentFilterKeys))
              .show();
    };
  }

  private static final class DefaultBuilder<B extends ComboBoxBuilder<Entity, EntityComboBox, B>> extends DefaultComboBoxBuilder<Entity, EntityComboBox, B> {

    private DefaultBuilder(EntityComboBoxModel comboBoxModel, Value<Entity> linkedValue) {
      super(comboBoxModel, linkedValue);
      popupMenuControl(Control.builder(new ForceRefreshCommand(comboBoxModel))
              .caption(FrameworkMessages.refresh())
              .build());
    }

    @Override
    protected EntityComboBox createComboBox() {
      return new EntityComboBox((EntityComboBoxModel) comboBoxModel);
    }
  }

  private static final class ForceRefreshCommand implements Control.Command {

    private final EntityComboBoxModel comboBoxModel;

    private ForceRefreshCommand(EntityComboBoxModel comboBoxModel) {
      this.comboBoxModel = comboBoxModel;
    }

    @Override
    public void perform() throws Exception {
      comboBoxModel.forceRefresh();
    }
  }
}
