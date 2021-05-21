/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.Configuration;
import is.codion.common.item.Item;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.textfield.TextFields.ValueContainsLiterals;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.textfield.TextInputPanel.ButtonFocusable;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntitySearchField;
import is.codion.swing.framework.ui.EntitySearchField.SelectionProvider;

import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;
import static is.codion.swing.common.ui.textfield.StringLengthValidator.stringLengthValidator;
import static is.codion.swing.common.ui.textfield.TextFields.createFormattedField;
import static is.codion.swing.common.ui.textfield.TextFields.selectAllOnFocusGained;
import static java.util.Objects.requireNonNull;

/**
 * Provides input components for editing entities.
 */
public final class EntityInputComponents {

  /**
   * Specifies whether maximum match or autocomplete is used for comboboxes,
   * {@link Completion#COMPLETION_MODE_MAXIMUM_MATCH} for maximum match
   * and {@link Completion#COMPLETION_MODE_AUTOCOMPLETE} for auto completion.<br>
   * Value type:String<br>
   * Default value: {@link Completion#COMPLETION_MODE_MAXIMUM_MATCH}
   */
  public static final PropertyValue<String> COMBO_BOX_COMPLETION_MODE = Configuration.stringValue("codion.swing.comboBoxCompletionMode", Completion.COMPLETION_MODE_MAXIMUM_MATCH);

  /**
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (JLabel.LEFT, JLabel.RIGHT, JLabel.CENTER)<br>
   * Default value: JLabel.LEFT
   */
  public static final PropertyValue<Integer> LABEL_TEXT_ALIGNMENT = Configuration.integerValue("codion.swing.labelTextAlignment", JLabel.LEFT);

  private static final String ATTRIBUTE_PARAM_NAME = "attribute";
  private static final int BOOLEAN_COMBO_BOX_POPUP_WIDTH = 40;

  /**
   * The underlying entity definition
   */
  private final EntityDefinition entityDefinition;

  /**
   * Instantiates a new EntityInputComponents, for creating input
   * components for a single entity type.
   * @param entityDefinition the definition of the entity
   */
  public EntityInputComponents(final EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  /**
   * @param attribute the attribute for which to create the input component
   * @param value the value to bind to the field
   * @param <T> the attribute type
   * @return the component handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   */
  public <T> JComponent createInputComponent(final Attribute<T> attribute, final Value<T> value) {
    return createInputComponent(attribute, value, null);
  }

  /**
   * @param attribute the attribute for which to create the input component
   * @param value the value to bind to the field
   * @param enabledState the enabled state
   * @param <T> the attribute type
   * @return the component handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   */
  public <T> JComponent createInputComponent(final Attribute<T> attribute, final Value<T> value,
                                             final StateObserver enabledState) {
    if (attribute instanceof ForeignKey) {
      throw new IllegalArgumentException("Use createForeignKeyComboBox() or createForeignKeySearchField() for ForeignKeys");
    }
    final Property<T> property = entityDefinition.getProperty(attribute);
    if (property instanceof ValueListProperty) {
      return valueListComboBoxBuilder(attribute, value)
              .enabledState(enabledState)
              .build();
    }
    if (attribute.isBoolean()) {
      return checkBoxBuilder((Attribute<Boolean>) attribute, (Value<Boolean>) value)
              .enabledState(enabledState)
              .nullable(property.isNullable())
              .includeCaption(false)
              .build();
    }
    if (attribute.isTemporal() || attribute.isNumerical() || attribute.isString() || attribute.isCharacter()) {
      return textFieldBuilder(attribute, value)
              .enabledState(enabledState)
              .updateOn(UpdateOn.KEYSTROKE)
              .build();
    }

    throw new IllegalArgumentException("No input component available for attribute: " + attribute + " (type: " + attribute.getTypeClass() + ")");
  }

  /**
   * Creates a JLabel with a caption from the given attribute, using the default label text alignment
   * @param attribute the attribute for which to create the label
   * @return a JLabel for the given attribute
   * @see EntityInputComponents#LABEL_TEXT_ALIGNMENT
   */
  public JLabel createLabel(final Attribute<?> attribute) {
    return createLabel(attribute, LABEL_TEXT_ALIGNMENT.get());
  }

  /**
   * Creates a JLabel with a caption from the given attribute
   * @param attribute the attribute for which to create the label
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given attribute
   */
  public JLabel createLabel(final Attribute<?> attribute, final int horizontalAlignment) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    final Property<?> property = entityDefinition.getProperty(attribute);
    final JLabel label = new JLabel(property.getCaption(), horizontalAlignment);
    if (property.getMnemonic() != null) {
      label.setDisplayedMnemonic(property.getMnemonic());
    }

    return label;
  }

  public CheckBoxBuilder checkBoxBuilder(final Attribute<Boolean> attribute, final Value<Boolean> value) {
    return new DefaultCheckBoxBuilder(entityDefinition.getProperty(attribute), value);
  }

  public BooleanComboBoxBuilder booleanComboBoxBuilder(final Attribute<Boolean> attribute, final Value<Boolean> value) {
    return new DefaultBooleanComboBoxBuilder(entityDefinition.getProperty(attribute), value);
  }

  public ForeignKeyComboBoxBuilder foreignKeyComboBoxBuilder(final ForeignKey foreignKey, final Value<Entity> value,
                                                             final SwingEntityComboBoxModel comboBoxModel) {
    return new DefaultForeignKeyComboBoxBuilder(entityDefinition.getForeignKeyProperty(foreignKey), value, comboBoxModel);
  }

  public ForeignKeySearchFieldBuilder foreignKeySearchFieldBuilder(final ForeignKey foreignKey, final Value<Entity> value,
                                                                   final EntitySearchModel searchModel) {
    return new DefaultForeignKeySearchFieldBuilder(entityDefinition.getForeignKeyProperty(foreignKey), value, searchModel);
  }

  public ForeignKeyFieldBuilder foreignKeyFieldBuilder(final ForeignKey foreignKey, final Value<Entity> value) {
    return new DefaultForeignKeyFieldBuilder(entityDefinition.getForeignKeyProperty(foreignKey), value);
  }

  public <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final Attribute<T> attribute, final Value<T> value) {
    return new DefaultValueListComboBoxBuilder<>(entityDefinition.getProperty(attribute), value);
  }

  public <T> ComboBoxBuilder<T> comboBoxBuilder(final Attribute<T> attribute, final Value<T> value,
                                                final ComboBoxModel<T> comboBoxModel) {
    return new DefaultComboBoxBuilder<>(entityDefinition.getProperty(attribute), value, comboBoxModel);
  }

  public <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuilder(final Attribute<T> attribute, final Value<T> value) {
    return new DefaultTemporalInputPanelBuiler<>(entityDefinition.getProperty(attribute), value);
  }

  public TextInputPanelBuilder textInputPanelBuilder(final Attribute<String> attribute, final Value<String> value) {
    return new DefaultTextInputPanelBuilder(entityDefinition.getProperty(attribute), value);
  }

  public TextAreaBuilder textAreaBuilder(final Attribute<String> attribute, final Value<String> value) {
    return new DefaultTextAreaBuilder(entityDefinition.getProperty(attribute), value);
  }

  public <T> TextFieldBuilder<T> textFieldBuilder(final Attribute<T> attribute, final Value<T> value) {
    return new DefaultTextFieldBuilder<>(entityDefinition.getProperty(attribute), value);
  }

  public FormattedTextFieldBuilder formattedTextFieldBuilder(final Attribute<String> attribute, final Value<String> value) {
    return new DefaultFormattedTextFieldBuilder(entityDefinition.getProperty(attribute), value);
  }

  /**
   * Builds a JComponent
   * @param <V> the type of the value the component represents
   * @param <T> the component type
   */
  public interface ComponentBuilder<V, T extends JComponent> {

    /**
     * @param preferredHeight the preferred component height
     * @return this builder instance
     */
    ComponentBuilder<V, T> preferredHeight(int preferredHeight);

    /**
     * @param preferredWidth the preferred component width
     * @return this builder instance
     */
    ComponentBuilder<V, T> preferredWidth(int preferredWidth);

    /**
     * @param preferredSize the preferred component size
     * @return this builder instance
     */
    ComponentBuilder<V, T> preferredSize(Dimension preferredSize);

    /**
     * @param transferFocusOnEnter true if the component should transfer focus on Enter
     * @return this builder instance
     */
    ComponentBuilder<V, T> transferFocusOnEnter(boolean transferFocusOnEnter);

    /**
     * @param enabledState the state controlling the component enabled status
     * @return this builder instance
     */
    ComponentBuilder<V, T> enabledState(StateObserver enabledState);

    /**
     * @param onBuild called after the component is built
     * @return this builder instance
     */
    ComponentBuilder<V, T> onBuild(Consumer<T> onBuild);

    /**
     * Builds the component.
     * @return the component
     */
    T build();
  }

  /**
   * Builds a JCheckBox.
   */
  public interface CheckBoxBuilder extends ComponentBuilder<Boolean, JCheckBox> {

    @Override
    CheckBoxBuilder preferredHeight(int preferredHeight);

    @Override
    CheckBoxBuilder preferredWidth(int preferredWidth);

    @Override
    CheckBoxBuilder preferredSize(Dimension preferredSize);

    @Override
    CheckBoxBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    CheckBoxBuilder enabledState(StateObserver enabledState);

    @Override
    CheckBoxBuilder onBuild(Consumer<JCheckBox> onBuild);

    /**
     * @return this builder instance
     */
    CheckBoxBuilder includeCaption(boolean includeCaption);

    /**
     * @param nullable if true then a {@link NullableCheckBox} is built.
     * @return this builder instance
     */
    CheckBoxBuilder nullable(boolean nullable);
  }

  public interface ComboBoxBuilder<T> extends ComponentBuilder<T, SteppedComboBox<T>> {

    @Override
    ComboBoxBuilder<T> preferredHeight(int preferredHeight);

    @Override
    ComboBoxBuilder<T> preferredWidth(int preferredWidth);

    @Override
    ComboBoxBuilder<T> preferredSize(Dimension preferredSize);

    @Override
    ComboBoxBuilder<T> transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    ComboBoxBuilder<T> enabledState(StateObserver enabledState);

    @Override
    ComboBoxBuilder<T> onBuild(Consumer<SteppedComboBox<T>> onBuild);

    /**
     * @return this builder instance
     */
    ComboBoxBuilder<T> editable(boolean editable);
  }

  /**
   * Builds a value list combo box.
   * @param <T> the value type
   */
  public interface ValueListComboBoxBuilder<T> extends ComponentBuilder<T, SteppedComboBox<Item<T>>> {

    @Override
    ValueListComboBoxBuilder<T> preferredHeight(int preferredHeight);

    @Override
    ValueListComboBoxBuilder<T> preferredWidth(int preferredWidth);

    @Override
    ValueListComboBoxBuilder<T> preferredSize(Dimension preferredSize);

    @Override
    ValueListComboBoxBuilder<T> transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    ValueListComboBoxBuilder<T> enabledState(StateObserver enabledState);

    @Override
    ValueListComboBoxBuilder<T> onBuild(Consumer<SteppedComboBox<Item<T>>> onBuild);

    /**
     * @return this builder instance
     */
    ValueListComboBoxBuilder<T> sorted(boolean sorted);
  }

  /**
   * Builds a boolean combo box.
   */
  public interface BooleanComboBoxBuilder extends ComponentBuilder<Boolean, SteppedComboBox<Item<Boolean>>> {

    @Override
    BooleanComboBoxBuilder preferredHeight(int preferredHeight);

    @Override
    BooleanComboBoxBuilder preferredWidth(int preferredWidth);

    @Override
    BooleanComboBoxBuilder preferredSize(Dimension preferredSize);

    @Override
    BooleanComboBoxBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    BooleanComboBoxBuilder enabledState(StateObserver enabledState);

    @Override
    BooleanComboBoxBuilder onBuild(Consumer<SteppedComboBox<Item<Boolean>>> onBuild);
  }

  /**
   * Builds a foreign key combo box.
   */
  public interface ForeignKeyComboBoxBuilder extends ComponentBuilder<Entity, EntityComboBox> {

    @Override
    ForeignKeyComboBoxBuilder preferredHeight(int preferredHeight);

    @Override
    ForeignKeyComboBoxBuilder preferredWidth(int preferredWidth);

    @Override
    ForeignKeyComboBoxBuilder preferredSize(Dimension preferredSize);

    @Override
    ForeignKeyComboBoxBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    ForeignKeyComboBoxBuilder enabledState(StateObserver enabledState);

    @Override
    ForeignKeyComboBoxBuilder onBuild(Consumer<EntityComboBox> onBuild);

    /**
     * @return this builder instance
     */
    ForeignKeyComboBoxBuilder popupWidth(int popupWidth);
  }

  /**
   * Builds a JTextField.
   * @param <T> the type the text field represents
   */
  public interface TextFieldBuilder<T> extends ComponentBuilder<T, JTextField> {

    @Override
    TextFieldBuilder<T> preferredHeight(int preferredHeight);

    @Override
    TextFieldBuilder<T> preferredWidth(int preferredWidth);

    @Override
    TextFieldBuilder<T> preferredSize(Dimension preferredSize);

    @Override
    TextFieldBuilder<T> transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    TextFieldBuilder<T> enabledState(StateObserver enabledState);

    @Override
    TextFieldBuilder<T> onBuild(Consumer<JTextField> onBuild);

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instance
     */
    TextFieldBuilder<T> updateOn(UpdateOn updateOn);

    /**
     * @param columns the number of colums in the text field
     * @return this builder instance
     */
    TextFieldBuilder<T> columns(int columns);

    /**
     * Note that this disables {@link #transferFocusOnEnter(boolean)}.
     * @param action the action to associate with the text field
     * @return this builder instance
     */
    TextFieldBuilder<T> action(Action action);

    /**
     * Makes the text field select all when it gains focus
     * @return this builder instance
     */
    TextFieldBuilder<T> selectAllOnFocusGained(boolean selectAllOnFocusGained);

    /**
     * Makes the text field convert all lower case input to upper case
     * @return this builder instance
     */
    TextFieldBuilder<T> upperCase();

    /**
     * Makes the text field convert all upper case input to lower case
     * @return this builder instance
     */
    TextFieldBuilder<T> lowerCase();
  }

  /**
   * Builds a formatted text field.
   */
  public interface FormattedTextFieldBuilder extends ComponentBuilder<String, JFormattedTextField> {

    @Override
    FormattedTextFieldBuilder preferredHeight(int preferredHeight);

    @Override
    FormattedTextFieldBuilder preferredWidth(int preferredWidth);

    @Override
    FormattedTextFieldBuilder preferredSize(Dimension preferredSize);

    @Override
    FormattedTextFieldBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    FormattedTextFieldBuilder enabledState(StateObserver enabledState);

    @Override
    FormattedTextFieldBuilder onBuild(Consumer<JFormattedTextField> onBuild);

    /**
     * @return this builder instance
     */
    FormattedTextFieldBuilder formatMaskString(String formatMaskString);

    /**
     * @return this builder instance
     */
    FormattedTextFieldBuilder valueContainsLiterals(boolean valueContainsLiterals);

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instance
     */
    FormattedTextFieldBuilder updateOn(UpdateOn updateOn);

    /**
     * @return this builder instance
     */
    FormattedTextFieldBuilder columns(int columns);
  }

  /**
   * Builds a JTextArea.
   */
  public interface TextAreaBuilder extends ComponentBuilder<String, JTextArea> {

    @Override
    TextAreaBuilder preferredHeight(int preferredHeight);

    @Override
    TextAreaBuilder preferredWidth(int preferredWidth);

    @Override
    TextAreaBuilder preferredSize(Dimension preferredSize);

    @Override
    TextAreaBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    TextAreaBuilder enabledState(StateObserver enabledState);

    @Override
    TextAreaBuilder onBuild(Consumer<JTextArea> onBuild);

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instance
     */
    TextAreaBuilder updateOn(UpdateOn updateOn);

    /**
     * @param rows the number of rows in the text area
     * @return this builder instance
     */
    TextAreaBuilder rows(int rows);

    /**
     * @param columns the number of colums in the text area
     * @return this builder instance
     */
    TextAreaBuilder columns(int columns);
  }

  /**
   * Builds a TextInputPanel.
   */
  public interface TextInputPanelBuilder extends ComponentBuilder<String, TextInputPanel> {

    @Override
    TextInputPanelBuilder preferredHeight(int preferredHeight);

    @Override
    TextInputPanelBuilder preferredWidth(int preferredWidth);

    @Override
    TextInputPanelBuilder preferredSize(Dimension preferredSize);

    @Override
    TextInputPanelBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    TextInputPanelBuilder enabledState(StateObserver enabledState);

    @Override
    TextInputPanelBuilder onBuild(Consumer<TextInputPanel> onBuild);

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instance
     */
    TextInputPanelBuilder updateOn(UpdateOn updateOn);

    /**
     * @param columns the number of colums in the text field
     * @return this builder instance
     */
    TextInputPanelBuilder columns(int columns);

    /**
     * @return this builder instance
     */
    TextInputPanelBuilder buttonFocusable(boolean buttonFocusable);

    /**
     * @param textAreaSize the input text area size
     * @return this builder instance
     */
    TextInputPanelBuilder textAreaSize(Dimension textAreaSize);
  }

  /**
   * Builds a TemporalInputPanel.
   * @param <T> the temporal type
   */
  public interface TemporalInputPanelBuilder<T extends Temporal> extends ComponentBuilder<T, TemporalInputPanel<T>> {

    @Override
    TemporalInputPanelBuilder<T> preferredHeight(int preferredHeight);

    @Override
    TemporalInputPanelBuilder<T> preferredWidth(int preferredWidth);

    @Override
    TemporalInputPanelBuilder<T> preferredSize(Dimension preferredSize);

    @Override
    TemporalInputPanelBuilder<T> transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    TemporalInputPanelBuilder<T> enabledState(StateObserver enabledState);

    @Override
    TemporalInputPanelBuilder<T> onBuild(Consumer<TemporalInputPanel<T>> onBuild);

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instance
     */
    TemporalInputPanelBuilder<T> updateOn(UpdateOn updateOn);

    /**
     * @param columns the number of colums in the text field
     * @return this builder instance
     */
    TemporalInputPanelBuilder<T> columns(int columns);

    /**
     * @return this builder instance
     */
    TemporalInputPanelBuilder<T> calendarButton(boolean calendarButton);
  }

  /**
   * Builds a foreign key search field.
   */
  public interface ForeignKeySearchFieldBuilder extends ComponentBuilder<Entity, EntitySearchField> {

    @Override
    ForeignKeySearchFieldBuilder preferredHeight(int preferredHeight);

    @Override
    ForeignKeySearchFieldBuilder preferredWidth(int preferredWidth);

    @Override
    ForeignKeySearchFieldBuilder preferredSize(Dimension preferredSize);

    @Override
    ForeignKeySearchFieldBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    ForeignKeySearchFieldBuilder enabledState(StateObserver enabledState);

    @Override
    ForeignKeySearchFieldBuilder onBuild(Consumer<EntitySearchField> onBuild);

    /**
     * @param columns the number of colums in the text field
     * @return this builder instance
     */
    ForeignKeySearchFieldBuilder columns(int columns);

    /**
     * @return this builder instance
     */
    ForeignKeySearchFieldBuilder selectionProviderFactory(Function<EntitySearchModel, SelectionProvider> selectionProviderFactory);
  }

  /**
   * Builds a read-only JTextField displaying a Entity instance.
   */
  public interface ForeignKeyFieldBuilder extends ComponentBuilder<Entity, JTextField> {

    @Override
    ForeignKeyFieldBuilder preferredHeight(int preferredHeight);

    @Override
    ForeignKeyFieldBuilder preferredWidth(int preferredWidth);

    @Override
    ForeignKeyFieldBuilder preferredSize(Dimension preferredSize);

    @Override
    ForeignKeyFieldBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

    @Override
    ForeignKeyFieldBuilder enabledState(StateObserver enabledState);

    @Override
    ForeignKeyFieldBuilder onBuild(Consumer<JTextField> onBuild);

    ForeignKeyFieldBuilder columns(int columns);
  }

  private static abstract class AbstractComponentBuilder<V, T extends JComponent> implements ComponentBuilder<V, T> {

    protected final Property<V> property;
    protected final Value<V> value;

    private int preferredHeight;
    private int preferredWidth;
    protected boolean transferFocusOnEnter;
    protected StateObserver enabledState;
    protected Consumer<T> onBuild;

    private AbstractComponentBuilder(final Property<V> attribute, final Value<V> value) {
      this.property = attribute;
      this.value = value;
    }

    @Override
    public ComponentBuilder<V, T> preferredHeight(final int preferredHeight) {//todo return type
      this.preferredHeight = preferredHeight;
      return this;
    }

    @Override
    public ComponentBuilder<V, T> preferredWidth(final int preferredWidth) {
      this.preferredWidth = preferredWidth;
      return this;
    }

    @Override
    public ComponentBuilder<V, T> preferredSize(final Dimension preferredSize) {
      requireNonNull(preferredSize);
      this.preferredHeight = preferredSize.height;
      this.preferredWidth = preferredSize.width;
      return this;
    }

    @Override
    public ComponentBuilder<V, T> transferFocusOnEnter(final boolean transferFocusOnEnter) {
      this.transferFocusOnEnter = transferFocusOnEnter;
      return this;
    }

    @Override
    public ComponentBuilder<V, T> enabledState(final StateObserver enabledState) {
      this.enabledState = enabledState;
      return this;
    }

    @Override
    public ComponentBuilder<V, T> onBuild(final Consumer<T> onBuild) {
      this.onBuild = onBuild;
      return this;
    }

    protected final void setPreferredSize(final T component) {
      if (preferredHeight > 0) {
        Components.setPreferredHeight(component, preferredHeight);
      }
      if (preferredWidth > 0) {
        Components.setPreferredWidth(component, preferredWidth);
      }
    }

    protected void onBuild(final T component) {
      if (onBuild != null) {
        onBuild.accept(component);
      }
    }

    static <T extends JComponent> T setDescriptionAndEnabledState(final T component, final String description,
                                                                  final StateObserver enabledState) {
      if (description != null) {
        component.setToolTipText(description);
      }
      if (enabledState != null) {
        Components.linkToEnabledState(enabledState, component);
      }

      return component;
    }
  }

  private static final class DefaultCheckBoxBuilder extends AbstractComponentBuilder<Boolean, JCheckBox> implements CheckBoxBuilder {

    private boolean includeCaption;
    private boolean nullable = false;

    private DefaultCheckBoxBuilder(final Property<Boolean> attribute, final Value<Boolean> value) {
      super(attribute, value);
    }

    @Override
    public CheckBoxBuilder preferredHeight(final int preferredHeight) {
      return (CheckBoxBuilder) super.preferredHeight(preferredHeight);
    }

    @Override
    public CheckBoxBuilder preferredWidth(final int preferredWidth) {
      return (CheckBoxBuilder) super.preferredWidth(preferredWidth);
    }

    @Override
    public CheckBoxBuilder preferredSize(final Dimension preferredSize) {
      return (CheckBoxBuilder) super.preferredSize(preferredSize);
    }

    @Override
    public CheckBoxBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (CheckBoxBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public CheckBoxBuilder enabledState(final StateObserver enabledState) {
      return (CheckBoxBuilder) super.enabledState(enabledState);
    }

    @Override
    public CheckBoxBuilder onBuild(final Consumer<JCheckBox> onBuild) {
      return (CheckBoxBuilder) super.onBuild(onBuild);
    }

    @Override
    public CheckBoxBuilder includeCaption(final boolean includeCaption) {
      this.includeCaption = includeCaption;
      return this;
    }

    @Override
    public CheckBoxBuilder nullable(final boolean nullable) {
      this.nullable = nullable;
      return this;
    }

    @Override
    public JCheckBox build() {
      final JCheckBox checkBox;
      if (nullable) {
        checkBox = createNullableCheckBox();
      }
      else {
        checkBox = createCheckBox();
      }
      setPreferredSize(checkBox);
      onBuild(checkBox);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter(checkBox);
      }

      return checkBox;
    }

    private NullableCheckBox createNullableCheckBox() {
      if (!property.isNullable()) {
        throw new IllegalArgumentException("Nullable boolean attribute required for createNullableCheckBox()");
      }
      final NullableCheckBox checkBox = new NullableCheckBox(new NullableToggleButtonModel(),
              includeCaption ? property.getCaption() : null);
      ComponentValues.toggleButton(checkBox).link(value);

      return setDescriptionAndEnabledState(checkBox, property.getDescription(), enabledState);
    }

    private JCheckBox createCheckBox() {
      final JCheckBox checkBox = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
      ComponentValues.toggleButton(checkBox).link(value);

      return setDescriptionAndEnabledState(checkBox, property.getDescription(), enabledState);
    }
  }

  private static final class DefaultComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<T>> implements ComboBoxBuilder<T> {

    private final ComboBoxModel<T> comboBoxModel;
    private boolean editable = false;

    private DefaultComboBoxBuilder(final Property<T> attribute, final Value<T> value,
                                   final ComboBoxModel<T> comboBoxModel) {
      super(attribute, value);
      this.comboBoxModel = comboBoxModel;
    }

    @Override
    public ComboBoxBuilder<T> preferredHeight(final int preferredHeight) {
      return (ComboBoxBuilder<T>) super.preferredHeight(preferredHeight);
    }

    @Override
    public ComboBoxBuilder<T> preferredWidth(final int preferredWidth) {
      return (ComboBoxBuilder<T>) super.preferredWidth(preferredWidth);
    }

    @Override
    public ComboBoxBuilder<T> preferredSize(final Dimension preferredSize) {
      return (ComboBoxBuilder<T>) super.preferredSize(preferredSize);
    }

    @Override
    public ComboBoxBuilder<T> transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (ComboBoxBuilder<T>) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public ComboBoxBuilder<T> enabledState(final StateObserver enabledState) {
      return (ComboBoxBuilder<T>) super.enabledState(enabledState);
    }

    @Override
    public ComboBoxBuilder<T> onBuild(final Consumer<SteppedComboBox<T>> onBuild) {
      return (ComboBoxBuilder<T>) super.onBuild(onBuild);
    }

    @Override
    public ComboBoxBuilder<T> editable(final boolean editable) {
      this.editable = editable;
      return this;
    }

    @Override
    public SteppedComboBox<T> build() {
      final SteppedComboBox<T> comboBox = createComboBox();
      setPreferredSize(comboBox);
      onBuild(comboBox);
      comboBox.setTransferFocusOnEnter(transferFocusOnEnter);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      }

      return comboBox;
    }

    private SteppedComboBox<T> createComboBox() {
      final SteppedComboBox<T> comboBox = new SteppedComboBox<>(comboBoxModel);
      if (editable && !property.getAttribute().isString()) {
        throw new IllegalArgumentException("Editable attribute ComboBox is only implemented for String properties");
      }
      comboBox.setEditable(editable);
      ComponentValues.comboBox(comboBox).link(value);

      return setDescriptionAndEnabledState(comboBox, property.getDescription(), enabledState);
    }

    static void addComboBoxCompletion(final JComboBox<?> comboBox) {
      final String completionMode = COMBO_BOX_COMPLETION_MODE.get();
      switch (completionMode) {
        case Completion.COMPLETION_MODE_NONE:
          break;
        case Completion.COMPLETION_MODE_AUTOCOMPLETE:
          Completion.autoComplete(comboBox);
          break;
        case Completion.COMPLETION_MODE_MAXIMUM_MATCH:
          Completion.maximumMatch(comboBox);
          break;
        default:
          throw new IllegalArgumentException("Unknown completion mode: " + completionMode);
      }
    }
  }

  private static final class DefaultValueListComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<Item<T>>> implements ValueListComboBoxBuilder<T> {

    private boolean sorted = true;

    private DefaultValueListComboBoxBuilder(final Property<T> attribute, final Value<T> value) {
      super(attribute, value);
    }

    @Override
    public ValueListComboBoxBuilder<T> preferredHeight(final int preferredHeight) {
      return (ValueListComboBoxBuilder<T>) super.preferredHeight(preferredHeight);
    }

    @Override
    public ValueListComboBoxBuilder<T> preferredWidth(final int preferredWidth) {
      return (ValueListComboBoxBuilder<T>) super.preferredWidth(preferredWidth);
    }

    @Override
    public ValueListComboBoxBuilder<T> preferredSize(final Dimension preferredSize) {
      return (ValueListComboBoxBuilder<T>) super.preferredSize(preferredSize);
    }

    @Override
    public ValueListComboBoxBuilder<T> transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (ValueListComboBoxBuilder<T>) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public ValueListComboBoxBuilder<T> enabledState(final StateObserver enabledState) {
      return (ValueListComboBoxBuilder<T>) super.enabledState(enabledState);
    }

    @Override
    public ValueListComboBoxBuilder<T> onBuild(final Consumer<SteppedComboBox<Item<T>>> onBuild) {
      return (ValueListComboBoxBuilder<T>) super.onBuild(onBuild);
    }

    @Override
    public ValueListComboBoxBuilder<T> sorted(final boolean sorted) {
      this.sorted = sorted;
      return this;
    }

    @Override
    public SteppedComboBox<Item<T>> build() {
      final SteppedComboBox<Item<T>> comboBox = createValueListComboBox();
      setPreferredSize(comboBox);
      onBuild(comboBox);
      comboBox.setTransferFocusOnEnter(transferFocusOnEnter);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      }

      return comboBox;
    }

    private SteppedComboBox<Item<T>> createValueListComboBox() {
      if (!(property instanceof ValueListProperty)) {
        throw new IllegalArgumentException("Property based on '" + property.getAttribute() + "' is not a ValueListProperty");
      }
      final ItemComboBoxModel<T> valueListComboBoxModel = createValueListComboBoxModel();
      final SteppedComboBox<Item<T>> comboBox = new SteppedComboBox<>(valueListComboBoxModel);
      ComponentValues.itemComboBox(comboBox).link(value);
      DefaultComboBoxBuilder.addComboBoxCompletion(comboBox);

      return setDescriptionAndEnabledState(comboBox, property.getDescription(), enabledState);
    }

    private ItemComboBoxModel<T> createValueListComboBoxModel() {
      final List<Item<T>> values = ((ValueListProperty<T>) property).getValues();
      final ItemComboBoxModel<T> model = sorted ?
              new ItemComboBoxModel<>(values) : new ItemComboBoxModel<>(null, values);
      final Item<T> nullItem = Item.item(null, FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
      if (property.isNullable() && !model.containsItem(nullItem)) {
        model.addItem(nullItem);
        model.setSelectedItem(nullItem);
      }

      return model;
    }
  }

  private static final class DefaultBooleanComboBoxBuilder extends AbstractComponentBuilder<Boolean, SteppedComboBox<Item<Boolean>>> implements BooleanComboBoxBuilder {

    private DefaultBooleanComboBoxBuilder(final Property<Boolean> attribute, final Value<Boolean> value) {
      super(attribute, value);
    }

    @Override
    public BooleanComboBoxBuilder preferredHeight(final int preferredHeight) {
      return (BooleanComboBoxBuilder) super.preferredHeight(preferredHeight);
    }

    @Override
    public BooleanComboBoxBuilder preferredWidth(final int preferredWidth) {
      return (BooleanComboBoxBuilder) super.preferredWidth(preferredWidth);
    }

    @Override
    public BooleanComboBoxBuilder preferredSize(final Dimension preferredSize) {
      return (BooleanComboBoxBuilder) super.preferredSize(preferredSize);
    }

    @Override
    public BooleanComboBoxBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (BooleanComboBoxBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public BooleanComboBoxBuilder enabledState(final StateObserver enabledState) {
      return (BooleanComboBoxBuilder) super.enabledState(enabledState);
    }

    @Override
    public BooleanComboBoxBuilder onBuild(final Consumer<SteppedComboBox<Item<Boolean>>> onBuild) {
      return (BooleanComboBoxBuilder) super.onBuild(onBuild);
    }

    @Override
    public SteppedComboBox<Item<Boolean>> build() {
      final SteppedComboBox<Item<Boolean>> comboBox = createBooleanComboBox();
      setPreferredSize(comboBox);
      onBuild(comboBox);
      comboBox.setTransferFocusOnEnter(transferFocusOnEnter);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      }

      return comboBox;
    }

    private SteppedComboBox<Item<Boolean>> createBooleanComboBox() {
      final BooleanComboBoxModel comboBoxModel = new BooleanComboBoxModel();
      final SteppedComboBox<Item<Boolean>> comboBox = new SteppedComboBox<>(comboBoxModel);
      ComponentValues.itemComboBox(comboBox).link(value);
      DefaultComboBoxBuilder.addComboBoxCompletion(comboBox);
      comboBox.setPopupWidth(BOOLEAN_COMBO_BOX_POPUP_WIDTH);

      return setDescriptionAndEnabledState(comboBox, property.getDescription(), enabledState);
    }
  }

  private static final class DefaultForeignKeyComboBoxBuilder extends AbstractComponentBuilder<Entity, EntityComboBox> implements ForeignKeyComboBoxBuilder {

    private final SwingEntityComboBoxModel comboBoxModel;
    private int popupWidth;

    private DefaultForeignKeyComboBoxBuilder(final ForeignKeyProperty foreignKey, final Value<Entity> value,
                                             final SwingEntityComboBoxModel comboBoxModel) {
      super(foreignKey, value);
      this.comboBoxModel = comboBoxModel;
    }

    @Override
    public ForeignKeyComboBoxBuilder preferredHeight(final int preferredHeight) {
      return (ForeignKeyComboBoxBuilder) super.preferredHeight(preferredHeight);
    }

    @Override
    public ForeignKeyComboBoxBuilder preferredWidth(final int preferredWidth) {
      return (ForeignKeyComboBoxBuilder) super.preferredWidth(preferredWidth);
    }

    @Override
    public ForeignKeyComboBoxBuilder preferredSize(final Dimension preferredSize) {
      return (ForeignKeyComboBoxBuilder) super.preferredSize(preferredSize);
    }

    @Override
    public ForeignKeyComboBoxBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (ForeignKeyComboBoxBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public ForeignKeyComboBoxBuilder enabledState(final StateObserver enabledState) {
      return (ForeignKeyComboBoxBuilder) super.enabledState(enabledState);
    }

    @Override
    public ForeignKeyComboBoxBuilder onBuild(final Consumer<EntityComboBox> onBuild) {
      return (ForeignKeyComboBoxBuilder) super.onBuild(onBuild);
    }

    @Override
    public ForeignKeyComboBoxBuilder popupWidth(final int popupWidth) {
      this.popupWidth = popupWidth;
      return this;
    }

    @Override
    public EntityComboBox build() {
      final EntityComboBox comboBox = createForeignKeyComboBox();
      setPreferredSize(comboBox);
      onBuild(comboBox);
      comboBox.setTransferFocusOnEnter(transferFocusOnEnter);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      }
      setPreferredSize(comboBox);
      if (popupWidth > 0) {
        comboBox.setPopupWidth(popupWidth);
      }

      return comboBox;
    }

    /**
     * Creates EntityComboBox based on the given foreign key
     * @param foreignKey the foreign key on which entity to base the combobox
     * @param value the value to bind to the field
     * @param comboBoxModel the combo box model
     * @param enabledState the state controlling the enabled state of the combobox
     * @return a EntityComboBox based on the given foreign key
     */
    private EntityComboBox createForeignKeyComboBox() {
      final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
      ComponentValues.comboBox(comboBox).link(value);
      DefaultComboBoxBuilder.addComboBoxCompletion(comboBox);

      return setDescriptionAndEnabledState(comboBox, property.getDescription(), enabledState);
    }
  }

  private static final class DefaultTextFieldBuilder<T> extends AbstractComponentBuilder<T, JTextField> implements TextFieldBuilder<T> {

    private UpdateOn updateOn = UpdateOn.KEYSTROKE;
    private int columns;
    private Action action;
    private boolean selectAllOnFocusGained;
    private boolean upperCase;
    private boolean lowerCase;

    private DefaultTextFieldBuilder(final Property<T> attribute, final Value<T> value) {
      super(attribute, value);
    }

    @Override
    public TextFieldBuilder<T> preferredHeight(final int preferredHeight) {
      return (TextFieldBuilder<T>) super.preferredHeight(preferredHeight);
    }

    @Override
    public TextFieldBuilder<T> preferredWidth(final int preferredWidth) {
      return (TextFieldBuilder<T>) super.preferredWidth(preferredWidth);
    }

    @Override
    public TextFieldBuilder<T> preferredSize(final Dimension preferredSize) {
      return (TextFieldBuilder<T>) super.preferredSize(preferredSize);
    }

    @Override
    public TextFieldBuilder<T> transferFocusOnEnter(final boolean transferFocusOnEnter) {
      if (action != null) {
        throw new IllegalStateException("Action has already been set");
      }
      return (TextFieldBuilder<T>) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public TextFieldBuilder<T> enabledState(final StateObserver enabledState) {
      return (TextFieldBuilder<T>) super.enabledState(enabledState);
    }

    @Override
    public TextFieldBuilder<T> onBuild(final Consumer<JTextField> onBuild) {
      return (TextFieldBuilder<T>) super.onBuild(onBuild);
    }

    @Override
    public TextFieldBuilder<T> updateOn(final UpdateOn updateOn) {
      this.updateOn = updateOn;
      return this;
    }

    @Override
    public TextFieldBuilder<T> columns(final int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public TextFieldBuilder<T> action(final Action action) {
      this.action = action;
      this.transferFocusOnEnter = false;
      return this;
    }

    @Override
    public TextFieldBuilder<T> selectAllOnFocusGained(final boolean selectAllOnFocusGained) {
      this.selectAllOnFocusGained = selectAllOnFocusGained;
      return this;
    }

    @Override
    public TextFieldBuilder<T> upperCase() {
      this.upperCase = true;
      this.lowerCase = false;
      return this;
    }

    @Override
    public TextFieldBuilder<T> lowerCase() {
      this.lowerCase = true;
      this.upperCase = false;
      return this;
    }

    @Override
    public JTextField build() {
      final JTextField textField = createTextField();
      setPreferredSize(textField);
      onBuild(textField);
      textField.setColumns(columns);
      if (action != null) {
        textField.setAction(action);
      }
      else if (transferFocusOnEnter) {
        Components.transferFocusOnEnter(textField);
      }
      if (selectAllOnFocusGained) {
        TextFields.selectAllOnFocusGained(textField);
      }
      if (upperCase) {
        TextFields.upperCase(textField);
      }
      if (lowerCase) {
        TextFields.lowerCase(textField);
      }

      return textField;
    }

    private JTextField createTextField() {
      final JTextField textField = createTextField(property, enabledState);
      final Attribute<T> attribute = property.getAttribute();
      if (attribute.isString()) {
        ComponentValues.textComponent(textField, property.getFormat(), updateOn).link((Value<String>) value);
      }
      else if (attribute.isCharacter()) {
        ComponentValues.characterTextField(textField, updateOn).link((Value<Character>) value);
      }
      else if (attribute.isInteger()) {
        ComponentValues.integerFieldBuilder()
                .component((IntegerField) textField)
                .updateOn(updateOn)
                .build()
                .link((Value<Integer>) value);
      }
      else if (attribute.isDouble()) {
        ComponentValues.doubleFieldBuilder()
                .component((DoubleField) textField)
                .updateOn(updateOn)
                .build()
                .link((Value<Double>) value);
      }
      else if (attribute.isBigDecimal()) {
        ComponentValues.bigDecimalFieldBuilder()
                .component((BigDecimalField) textField)
                .updateOn(updateOn)
                .build()
                .link((Value<BigDecimal>) value);
      }
      else if (attribute.isLong()) {
        ComponentValues.longFieldBuilder()
                .component((LongField) textField)
                .updateOn(updateOn)
                .build()
                .link((Value<Long>) value);
      }
      else if (attribute.isTemporal()) {
        ComponentValues.temporalField((TemporalField<Temporal>) textField, updateOn).link((Value<Temporal>) value);
      }
      else {
        throw new IllegalArgumentException("Text fields not implemented for attribute type: " + attribute);
      }

      return textField;
    }

    static JTextField createTextField(final Property<?> property, final StateObserver enabledState) {
      return setDescriptionAndEnabledState(createTextField(property), property.getDescription(), enabledState);
    }

    private static JTextField createTextField(final Property<?> property) {
      final Attribute<?> attribute = property.getAttribute();
      if (attribute.isInteger()) {
        return initializeIntegerField((Property<Integer>) property);
      }
      else if (attribute.isDouble()) {
        return initializeDoubleField((Property<Double>) property);
      }
      else if (attribute.isBigDecimal()) {
        return initializeBigDecimalField((Property<BigDecimal>) property);
      }
      else if (attribute.isLong()) {
        return initializeLongField((Property<Long>) property);
      }
      else if (attribute.isTemporal()) {
        return new TemporalField<>((Class<Temporal>) attribute.getTypeClass(), property.getDateTimePattern());
      }
      else if (attribute.isString()) {
        return initializeStringField(property.getMaximumLength());
      }
      else if (attribute.isCharacter()) {
        return new JTextField(new SizedDocument(1), "", 1);
      }

      throw new IllegalArgumentException("Creating text fields for type: " + attribute.getTypeClass() + " is not implemented (" + property + ")");
    }

    private static JTextField initializeStringField(final int maximumLength) {
      final SizedDocument sizedDocument = new SizedDocument();
      if (maximumLength > 0) {
        sizedDocument.setMaximumLength(maximumLength);
      }

      return new JTextField(sizedDocument, "", 0);
    }

    private static DoubleField initializeDoubleField(final Property<Double> property) {
      final DoubleField field = new DoubleField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
      if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
        field.setRange(Math.min(property.getMinimumValue(), 0), property.getMaximumValue());
      }

      return field;
    }

    private static BigDecimalField initializeBigDecimalField(final Property<BigDecimal> property) {
      final BigDecimalField field = new BigDecimalField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
      if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
        field.setRange(Math.min(property.getMinimumValue(), 0), property.getMaximumValue());
      }

      return field;
    }

    private static IntegerField initializeIntegerField(final Property<Integer> property) {
      final IntegerField field = new IntegerField(cloneFormat((NumberFormat) property.getFormat()));
      if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
        field.setRange(property.getMinimumValue(), property.getMaximumValue());
      }

      return field;
    }

    private static LongField initializeLongField(final Property<Long> property) {
      final LongField field = new LongField(cloneFormat((NumberFormat) property.getFormat()));
      if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
        field.setRange(property.getMinimumValue(), property.getMaximumValue());
      }

      return field;
    }

    private static NumberFormat cloneFormat(final NumberFormat format) {
      final NumberFormat cloned = (NumberFormat) format.clone();
      cloned.setGroupingUsed(format.isGroupingUsed());
      cloned.setMaximumIntegerDigits(format.getMaximumIntegerDigits());
      cloned.setMaximumFractionDigits(format.getMaximumFractionDigits());
      cloned.setMinimumFractionDigits(format.getMinimumFractionDigits());
      cloned.setRoundingMode(format.getRoundingMode());
      cloned.setCurrency(format.getCurrency());
      cloned.setParseIntegerOnly(format.isParseIntegerOnly());

      return cloned;
    }
  }

  private static final class DefaultFormattedTextFieldBuilder extends AbstractComponentBuilder<String, JFormattedTextField> implements FormattedTextFieldBuilder {

    private String formatMaskString;
    private boolean valueContainsLiterals = true;
    private UpdateOn updateOn = UpdateOn.KEYSTROKE;
    private int columns;

    private DefaultFormattedTextFieldBuilder(final Property<String> attribute, final Value<String> value) {
      super(attribute, value);
    }

    @Override
    public FormattedTextFieldBuilder preferredHeight(final int preferredHeight) {
      return (FormattedTextFieldBuilder) super.preferredHeight(preferredHeight);
    }

    @Override
    public FormattedTextFieldBuilder preferredWidth(final int preferredWidth) {
      return (FormattedTextFieldBuilder) super.preferredWidth(preferredWidth);
    }

    @Override
    public FormattedTextFieldBuilder preferredSize(final Dimension preferredSize) {
      return (FormattedTextFieldBuilder) super.preferredSize(preferredSize);
    }

    @Override
    public FormattedTextFieldBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (FormattedTextFieldBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public FormattedTextFieldBuilder enabledState(final StateObserver enabledState) {
      return (FormattedTextFieldBuilder) super.enabledState(enabledState);
    }

    @Override
    public FormattedTextFieldBuilder onBuild(final Consumer<JFormattedTextField> onBuild) {
      return (FormattedTextFieldBuilder) super.onBuild(onBuild);
    }

    @Override
    public FormattedTextFieldBuilder formatMaskString(final String formatMaskString) {
      this.formatMaskString = requireNonNull(formatMaskString);
      return this;
    }

    @Override
    public FormattedTextFieldBuilder valueContainsLiterals(final boolean valueContainsLiterals) {
      this.valueContainsLiterals = valueContainsLiterals;
      return this;
    }

    @Override
    public FormattedTextFieldBuilder updateOn(final UpdateOn updateOn) {
      this.updateOn = requireNonNull(updateOn);
      return this;
    }

    @Override
    public FormattedTextFieldBuilder columns(final int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public JFormattedTextField build() {
      final JFormattedTextField textField = setDescriptionAndEnabledState(createFormattedField(formatMaskString,
              valueContainsLiterals ? ValueContainsLiterals.YES : ValueContainsLiterals.NO),
              property.getDescription(), enabledState);
      ComponentValues.textComponent(textField, null, updateOn).link(value);
      setPreferredSize(textField);
      onBuild(textField);
      textField.setColumns(columns);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter(textField);
      }

      return textField;
    }
  }

  private static final class DefaultTextAreaBuilder extends AbstractComponentBuilder<String, JTextArea> implements TextAreaBuilder {

    private UpdateOn updateOn = UpdateOn.KEYSTROKE;
    private int rows;
    private int columns;

    private DefaultTextAreaBuilder(final Property<String> attribute, final Value<String> value) {
      super(attribute, value);
    }

    @Override
    public TextAreaBuilder preferredHeight(final int preferredHeight) {
      return (TextAreaBuilder) super.preferredHeight(preferredHeight);
    }

    @Override
    public TextAreaBuilder preferredWidth(final int preferredWidth) {
      return (TextAreaBuilder) super.preferredWidth(preferredWidth);
    }

    @Override
    public TextAreaBuilder preferredSize(final Dimension preferredSize) {
      return (TextAreaBuilder) super.preferredSize(preferredSize);
    }

    @Override
    public TextAreaBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (TextAreaBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public TextAreaBuilder enabledState(final StateObserver enabledState) {
      return (TextAreaBuilder) super.enabledState(enabledState);
    }

    @Override
    public TextAreaBuilder onBuild(final Consumer<JTextArea> onBuild) {
      return (TextAreaBuilder) super.onBuild(onBuild);
    }

    @Override
    public TextAreaBuilder updateOn(final UpdateOn updateOn) {
      this.updateOn = requireNonNull(updateOn);
      return this;
    }

    @Override
    public TextAreaBuilder rows(final int rows) {
      this.rows = rows;
      return this;
    }

    @Override
    public TextAreaBuilder columns(final int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public JTextArea build() {
      if (!property.getAttribute().isString()) {
        throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
      }

      final JTextArea textArea = setDescriptionAndEnabledState(rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea(), property.getDescription(), enabledState);
      setPreferredSize(textArea);
      onBuild(textArea);
      textArea.setLineWrap(true);//todo
      textArea.setWrapStyleWord(true);//todo
      if (property.getMaximumLength() > 0) {
        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(
                parsingDocumentFilter(stringLengthValidator(property.getMaximumLength())));
      }
      ComponentValues.textComponent(textArea, null, updateOn).link(value);

      return textArea;
    }
  }

  private static final class DefaultTextInputPanelBuilder extends AbstractComponentBuilder<String, TextInputPanel> implements TextInputPanelBuilder {

    private UpdateOn updateOn = UpdateOn.KEYSTROKE;
    private boolean buttonFocusable;
    private int columns;
    private Dimension textAreaSize;

    private DefaultTextInputPanelBuilder(final Property<String> attribute, final Value<String> value) {
      super(attribute, value);
    }

    @Override
    public TextInputPanelBuilder preferredHeight(final int preferredHeight) {
      return (TextInputPanelBuilder) super.preferredHeight(preferredHeight);
    }

    @Override
    public TextInputPanelBuilder preferredWidth(final int preferredWidth) {
      return (TextInputPanelBuilder) super.preferredWidth(preferredWidth);
    }

    @Override
    public TextInputPanelBuilder preferredSize(final Dimension preferredSize) {
      return (TextInputPanelBuilder) super.preferredSize(preferredSize);
    }

    @Override
    public TextInputPanelBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (TextInputPanelBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public TextInputPanelBuilder enabledState(final StateObserver enabledState) {
      return (TextInputPanelBuilder) super.enabledState(enabledState);
    }

    @Override
    public TextInputPanelBuilder onBuild(final Consumer<TextInputPanel> onBuild) {
      return (TextInputPanelBuilder) super.onBuild(onBuild);
    }

    @Override
    public TextInputPanelBuilder updateOn(final UpdateOn updateOn) {
      this.updateOn = requireNonNull(updateOn);
      return this;
    }

    @Override
    public TextInputPanelBuilder columns(final int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public TextInputPanelBuilder buttonFocusable(final boolean buttonFocusable) {
      this.buttonFocusable = buttonFocusable;
      return this;
    }

    @Override
    public TextInputPanelBuilder textAreaSize(final Dimension textAreaSize) {
      this.textAreaSize = requireNonNull(textAreaSize);
      return this;
    }

    @Override
    public TextInputPanel build() {
      final TextInputPanel inputPanel = createTextInputPanel();
      setPreferredSize(inputPanel);
      onBuild(inputPanel);
      inputPanel.getTextField().setColumns(columns);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter(inputPanel.getTextField());
        if (inputPanel.getButton() != null) {
          Components.transferFocusOnEnter(inputPanel.getButton());
        }
      }

      return inputPanel;
    }

    private TextInputPanel createTextInputPanel() {
      final JTextField field = new DefaultTextFieldBuilder<>(property, value)
              .updateOn(updateOn)
              .build();
      final TextInputPanel panel = new TextInputPanel(field, property.getCaption(), textAreaSize,
              buttonFocusable ? ButtonFocusable.YES : ButtonFocusable.NO);
      panel.setMaximumLength(property.getMaximumLength());

      return panel;
    }
  }

  private static final class DefaultTemporalInputPanelBuiler<T extends Temporal> extends AbstractComponentBuilder<T, TemporalInputPanel<T>>
          implements TemporalInputPanelBuilder<T> {

    private UpdateOn updateOn = UpdateOn.KEYSTROKE;
    private boolean calendarButton;
    private int columns;

    private DefaultTemporalInputPanelBuiler(final Property<T> attribute, final Value<T> value) {
      super(attribute, value);
    }

    @Override
    public TemporalInputPanelBuilder<T> preferredHeight(final int preferredHeight) {
      return (TemporalInputPanelBuilder<T>) super.preferredHeight(preferredHeight);
    }

    @Override
    public TemporalInputPanelBuilder<T> preferredWidth(final int preferredWidth) {
      return (TemporalInputPanelBuilder<T>) super.preferredWidth(preferredWidth);
    }

    @Override
    public TemporalInputPanelBuilder<T> preferredSize(final Dimension preferredSize) {
      return (TemporalInputPanelBuilder<T>) super.preferredSize(preferredSize);
    }

    @Override
    public TemporalInputPanelBuilder<T> transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (TemporalInputPanelBuilder<T>) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public TemporalInputPanelBuilder<T> enabledState(final StateObserver enabledState) {
      return (TemporalInputPanelBuilder<T>) super.enabledState(enabledState);
    }

    @Override
    public TemporalInputPanelBuilder<T> onBuild(final Consumer<TemporalInputPanel<T>> onBuild) {
      return (TemporalInputPanelBuilder<T>) super.onBuild(onBuild);
    }

    @Override
    public TemporalInputPanelBuilder<T> updateOn(final UpdateOn updateOn) {
      this.updateOn = requireNonNull(updateOn);
      return this;
    }

    @Override
    public TemporalInputPanelBuilder<T> columns(final int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public TemporalInputPanelBuilder<T> calendarButton(final boolean calendarButton) {
      this.calendarButton = calendarButton;
      return this;
    }

    @Override
    public TemporalInputPanel<T> build() {
      final TemporalInputPanel<T> inputPanel = createTemporalInputPanel();
      setPreferredSize(inputPanel);
      onBuild(inputPanel);
      inputPanel.getInputField().setColumns(columns);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter(inputPanel.getInputField());
        if (inputPanel.getCalendarButton() != null) {
          Components.transferFocusOnEnter(inputPanel.getCalendarButton());
        }
      }

      return inputPanel;
    }

    private <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel() {
      if (!property.getAttribute().isTemporal()) {
        throw new IllegalArgumentException("Property " + property.getAttribute() + " is not a date or time attribute");
      }

      final TemporalField<Temporal> temporalField = (TemporalField<Temporal>) DefaultTextFieldBuilder.createTextField(property, enabledState);

      ComponentValues.temporalField(temporalField, updateOn).link((Value<Temporal>) value);

      return (TemporalInputPanel<T>) TemporalInputPanel.builder()
              .temporalField(temporalField)
              .calendarButton(calendarButton)
              .enabledState(enabledState)
              .build();
    }
  }

  private static final class DefaultForeignKeySearchFieldBuilder extends AbstractComponentBuilder<Entity, EntitySearchField> implements ForeignKeySearchFieldBuilder {

    private final EntitySearchModel searchModel;
    private int columns;
    private Function<EntitySearchModel, SelectionProvider> selectionProviderFactory;

    private DefaultForeignKeySearchFieldBuilder(final ForeignKeyProperty attribute, final Value<Entity> value,
                                                final EntitySearchModel searchModel) {
      super(attribute, value);
      this.searchModel = searchModel;
    }

    @Override
    public ForeignKeySearchFieldBuilder preferredHeight(final int preferredHeight) {
      return (ForeignKeySearchFieldBuilder) super.preferredHeight(preferredHeight);
    }

    @Override
    public ForeignKeySearchFieldBuilder preferredWidth(final int preferredWidth) {
      return (ForeignKeySearchFieldBuilder) super.preferredWidth(preferredWidth);
    }

    @Override
    public ForeignKeySearchFieldBuilder preferredSize(final Dimension preferredSize) {
      return (ForeignKeySearchFieldBuilder) super.preferredSize(preferredSize);
    }

    @Override
    public ForeignKeySearchFieldBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (ForeignKeySearchFieldBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public ForeignKeySearchFieldBuilder enabledState(final StateObserver enabledState) {
      return (ForeignKeySearchFieldBuilder) super.enabledState(enabledState);
    }

    @Override
    public ForeignKeySearchFieldBuilder onBuild(final Consumer<EntitySearchField> onBuild) {
      return (ForeignKeySearchFieldBuilder) super.onBuild(onBuild);
    }

    @Override
    public ForeignKeySearchFieldBuilder columns(final int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public ForeignKeySearchFieldBuilder selectionProviderFactory(final Function<EntitySearchModel, SelectionProvider> selectionProviderFactory) {
      this.selectionProviderFactory = requireNonNull(selectionProviderFactory);
      return this;
    }

    @Override
    public EntitySearchField build() {
      final EntitySearchField searchField = createForeignKeySearchField();
      setPreferredSize(searchField);
      onBuild(searchField);
      searchField.setColumns(columns);
      if (transferFocusOnEnter) {
        searchField.setTransferFocusOnEnter(true);
      }
      if (selectionProviderFactory != null) {
        searchField.setSelectionProvider(selectionProviderFactory.apply(searchField.getModel()));
      }

      return searchField;
    }

    private EntitySearchField createForeignKeySearchField() {
      final EntitySearchField searchField = new EntitySearchField(searchModel);
      new SearchUIValue(searchField.getModel()).link(value);
      selectAllOnFocusGained(searchField);

      final String propertyDescription = property.getDescription();

      return setDescriptionAndEnabledState(searchField, propertyDescription == null ? searchModel.getDescription() : propertyDescription, enabledState);
    }

    private static final class SearchUIValue extends AbstractValue<Entity> {
      private final EntitySearchModel searchModel;

      private SearchUIValue(final EntitySearchModel searchModel) {
        this.searchModel = searchModel;
        this.searchModel.addSelectedEntitiesListener(selected -> notifyValueChange());
      }

      @Override
      public Entity get() {
        final List<Entity> selectedEntities = searchModel.getSelectedEntities();
        return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
      }

      @Override
      protected void setValue(final Entity value) {
        searchModel.setSelectedEntity(value);
      }
    }
  }

  private static final class DefaultForeignKeyFieldBuilder extends AbstractComponentBuilder<Entity, JTextField> implements ForeignKeyFieldBuilder {

    private int columns;

    private DefaultForeignKeyFieldBuilder(final ForeignKeyProperty attribute, final Value<Entity> value) {
      super(attribute, value);
    }

    @Override
    public ForeignKeyFieldBuilder preferredHeight(final int preferredHeight) {
      return (ForeignKeyFieldBuilder) super.preferredHeight(preferredHeight);
    }

    @Override
    public ForeignKeyFieldBuilder preferredWidth(final int preferredWidth) {
      return (ForeignKeyFieldBuilder) super.preferredWidth(preferredWidth);
    }

    @Override
    public ForeignKeyFieldBuilder preferredSize(final Dimension preferredSize) {
      return (ForeignKeyFieldBuilder) super.preferredSize(preferredSize);
    }

    @Override
    public ForeignKeyFieldBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
      return (ForeignKeyFieldBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
    }

    @Override
    public ForeignKeyFieldBuilder enabledState(final StateObserver enabledState) {
      throw new UnsupportedOperationException("Foreign key fields are read-only and disabled by default");
    }

    @Override
    public ForeignKeyFieldBuilder onBuild(final Consumer<JTextField> onBuild) {
      return (ForeignKeyFieldBuilder) super.onBuild(onBuild);
    }

    @Override
    public ForeignKeyFieldBuilder columns(final int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public JTextField build() {
      final JTextField textField = new JTextField(columns);
      setPreferredSize(textField);
      onBuild(textField);
      textField.setEditable(false);
      textField.setFocusable(false);
      textField.setToolTipText(property.getDescription());
      final Value<String> entityStringValue = Value.value();
      value.addDataListener(entity -> entityStringValue.set(entity == null ? "" : entity.toString()));
      ComponentValues.textComponent(textField).link(entityStringValue);
      if (transferFocusOnEnter) {
        Components.transferFocusOnEnter(textField);
      }

      return textField;
    }
  }
}
