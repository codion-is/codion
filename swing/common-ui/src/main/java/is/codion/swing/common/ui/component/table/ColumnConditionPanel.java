/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.item.Item;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.Utilities.getParentOfType;
import static is.codion.swing.common.ui.component.Components.*;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for {@link ColumnConditionModel}.
 * For instances use the {@link #columnConditionPanel(ColumnConditionModel)} or
 * {@link #columnConditionPanel(ColumnConditionModel, BoundFieldFactory)} factory methods.
 * @param <C> the type of objects used to identify columns
 * @param <T> the column value type
 * @see #columnConditionPanel(ColumnConditionModel)
 * @see #columnConditionPanel(ColumnConditionModel, BoundFieldFactory)
 */
public final class ColumnConditionPanel<C, T> extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ColumnConditionPanel.class.getName());

  private final ColumnConditionModel<? extends C, T> conditionModel;
  private final JToggleButton toggleEnabledButton;
  private final JComboBox<Item<Operator>> operatorCombo;
  private final JComponent equalField;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;
  private final JPanel controlPanel = new JPanel(new BorderLayout());
  private final JPanel inputPanel = new JPanel(new BorderLayout());
  private final JPanel rangePanel = new JPanel(new GridLayout(1, 2));

  private final Event<C> focusGainedEvent = Event.event();
  private final State advancedViewState = State.state();

  private ColumnConditionPanel(ColumnConditionModel<? extends C, T> conditionModel, BoundFieldFactory boundFieldFactory) {
    requireNonNull(conditionModel, "conditionModel");
    requireNonNull(boundFieldFactory, "boundFieldFactory");
    this.conditionModel = conditionModel;
    boolean modelLocked = conditionModel.isLocked();
    conditionModel.setLocked(false);//otherwise, the validator checking the locked state kicks in during value linking
    this.equalField = boundFieldFactory.createEqualField();
    this.upperBoundField = boundFieldFactory.createUpperBoundField().orElse(null);
    this.lowerBoundField = boundFieldFactory.createLowerBoundField().orElse(null);
    this.operatorCombo = createOperatorComboBox(conditionModel.operators());
    this.toggleEnabledButton = radioButton(conditionModel.enabledState())
            .horizontalAlignment(CENTER)
            .popupMenu(Controls.builder()
                    .control(ToggleControl.builder(conditionModel.autoEnableState())
                            .caption(MESSAGES.getString("auto_enable"))).build()
                    .createPopupMenu())
            .build();
    conditionModel.setLocked(modelLocked);
    initializeUI();
    bindEvents();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(toggleEnabledButton, operatorCombo, equalField,
            lowerBoundField, upperBoundField, controlPanel, inputPanel, rangePanel);
  }

  /**
   * @return the condition model this panel uses
   */
  public ColumnConditionModel<C, T> model() {
    return (ColumnConditionModel<C, T>) conditionModel;
  }

  /**
   * Requests keyboard focus for this panels input field
   */
  public void requestInputFocus() {
    switch (conditionModel.getOperator()) {
      case EQUAL:
      case NOT_EQUAL:
        equalField.requestFocusInWindow();
        break;
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL:
      case BETWEEN_EXCLUSIVE:
      case BETWEEN:
      case NOT_BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN:
        lowerBoundField.requestFocusInWindow();
        break;
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL:
        upperBoundField.requestFocusInWindow();
        break;
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.getOperator());
    }
  }

  /**
   * @param advanced true if advanced view should be enabled
   */
  public void setAdvancedView(boolean advanced) {
    advancedViewState.set(advanced);
  }

  /**
   * @return true if the advanced view is enabled
   */
  public boolean isAdvancedView() {
    return advancedViewState.get();
  }

  /**
   * @return the condition operator combo box
   */
  public JComboBox<Item<Operator>> operatorComboBox() {
    return operatorCombo;
  }

  /**
   * @return the JComponent used to specify the equal value
   */
  public JComponent equalField() {
    return equalField;
  }

  /**
   * @return the JComponent used to specify the upper bound
   */
  public JComponent upperBoundField() {
    return upperBoundField;
  }

  /**
   * @return the JComponent used to specify the lower bound
   */
  public JComponent lowerBoundField() {
    return lowerBoundField;
  }

  /**
   * @param listener a listener notified each time the advanced condition state changes
   */
  public void addAdvancedViewListener(EventDataListener<Boolean> listener) {
    advancedViewState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeAdvancedViewListener(EventDataListener<Boolean> listener) {
    advancedViewState.removeDataListener(listener);
  }

  /**
   * @param listener listener notified when this condition panels input fields receive focus
   */
  public void addFocusGainedListener(EventDataListener<C> listener) {
    focusGainedEvent.addDataListener(listener);
  }

  /**
   * Instantiates a new {@link ColumnConditionPanel}, with a default bound field factory.
   * @param conditionModel the condition model to base this panel on
   * @param <C> the type of objects used to identify columns
   * @param <T> the column value type
   * @return a new {@link ColumnConditionPanel} instance
   */
  public static <C, T> ColumnConditionPanel<C, T> columnConditionPanel(ColumnConditionModel<C, T> conditionModel) {
    return columnConditionPanel(conditionModel, new DefaultBoundFieldFactory<>(conditionModel));
  }

  /**
   * Instantiates a new {@link ColumnConditionPanel}.
   * @param conditionModel the condition model to base this panel on
   * @param boundFieldFactory the input field factory
   * @param <C> the type of objects used to identify columns
   * @param <T> the column value type
   * @return a new {@link ColumnConditionPanel} instance or null if the column type is not supported
   */
  public static <C, T> ColumnConditionPanel<C, T> columnConditionPanel(ColumnConditionModel<? extends C, T> conditionModel,
                                                                       BoundFieldFactory boundFieldFactory) {
    if (boundFieldFactory.supportsType(conditionModel.columnClass())) {
      return new ColumnConditionPanel<>(conditionModel, boundFieldFactory);
    }

    return null;
  }

  /**
   * Responsible for creating {@link ColumnConditionPanel}s
   * @param <C> the column identifier type
   */
  public interface Factory<C> {

    /**
     * Creates a ColumnConditionPanel for the given column, returns null if none is available
     * @param <T> the column value type
     * @param conditionModel the column condition model
     * @return a ColumnConditionPanel or null if none is available for the given column
     */
    <T> ColumnConditionPanel<C, T> createConditionPanel(ColumnConditionModel<? extends C, T> conditionModel);
  }

  /**
   * Provides equal, upper and lower bound input fields for a ColumnConditionPanel
   */
  public interface BoundFieldFactory {

    /**
     * @param columnClass the column class
     * @return true if the type is supported
     */
    default boolean supportsType(Class<?> columnClass) {
      requireNonNull(columnClass);

      return Arrays.asList(String.class, Boolean.class, Short.class, Integer.class, Double.class,
                      BigDecimal.class, Long.class, LocalTime.class, LocalDate.class,
                      LocalDateTime.class, OffsetDateTime.class)
              .contains(columnClass);
    }

    /**
     * @return the equal value field
     * @throws IllegalArgumentException in case the bound type is not supported
     */
    JComponent createEqualField();

    /**
     * @return an upper bound input field, or an empty Optional if it does not apply to the bound type
     * @throws IllegalArgumentException in case the bound type is not supported
     */
    Optional<JComponent> createUpperBoundField();

    /**
     * @return a lower bound input field, or an empty Optional if it does not apply to the bound type
     * @throws IllegalArgumentException in case the bound type is not supported
     */
    Optional<JComponent> createLowerBoundField();
  }

  private static final class DefaultBoundFieldFactory<T> implements BoundFieldFactory {

    private final ColumnConditionModel<?, T> columnConditionModel;

    private DefaultBoundFieldFactory(ColumnConditionModel<?, T> columnConditionModel) {
      this.columnConditionModel = requireNonNull(columnConditionModel, "columnConditionModel");
    }

    public JComponent createEqualField() {
      return createField(columnConditionModel.equalValueSet().value());
    }

    @Override
    public Optional<JComponent> createUpperBoundField() {
      if (columnConditionModel.columnClass().equals(Boolean.class)) {
        return Optional.empty();//no upper bound field required for boolean values
      }

      return Optional.of(createField(columnConditionModel.upperBoundValue()));
    }

    @Override
    public Optional<JComponent> createLowerBoundField() {
      if (columnConditionModel.columnClass().equals(Boolean.class)) {
        return Optional.empty();//no lower bound field required for boolean values
      }

      return Optional.of(createField(columnConditionModel.lowerBoundValue()));
    }

    private JComponent createField(Value<?> linkedValue) {
      Class<?> columnClass = columnConditionModel.columnClass();
      if (columnClass.equals(Boolean.class)) {
        return checkBox((Value<Boolean>) linkedValue)
                .nullable(true)
                .horizontalAlignment(CENTER)
                .build();
      }
      if (columnClass.equals(Short.class)) {
        return shortField((Value<Short>) linkedValue)
                .format(columnConditionModel.format())
                .build();
      }
      if (columnClass.equals(Integer.class)) {
        return integerField((Value<Integer>) linkedValue)
                .format(columnConditionModel.format())
                .build();
      }
      else if (columnClass.equals(Double.class)) {
        return doubleField((Value<Double>) linkedValue)
                .format(columnConditionModel.format())
                .build();
      }
      else if (columnClass.equals(BigDecimal.class)) {
        return bigDecimalField((Value<BigDecimal>) linkedValue)
                .format(columnConditionModel.format())
                .build();
      }
      else if (columnClass.equals(Long.class)) {
        return longField((Value<Long>) linkedValue)
                .format(columnConditionModel.format())
                .build();
      }
      else if (columnClass.equals(LocalTime.class)) {
        return localTimeField(columnConditionModel.dateTimePattern(), (Value<LocalTime>) linkedValue)
                .build();
      }
      else if (columnClass.equals(LocalDate.class)) {
        return localDateField(columnConditionModel.dateTimePattern(), (Value<LocalDate>) linkedValue)
                .build();
      }
      else if (columnClass.equals(LocalDateTime.class)) {
        return localDateTimeField(columnConditionModel.dateTimePattern(), (Value<LocalDateTime>) linkedValue)
                .build();
      }
      else if (columnClass.equals(OffsetDateTime.class)) {
        return offsetDateTimeField(columnConditionModel.dateTimePattern(), (Value<OffsetDateTime>) linkedValue)
                .build();
      }
      else if (columnClass.equals(String.class)) {
        return textField((Value<String>) linkedValue)
                .build();
      }

      throw new IllegalArgumentException("Unsupported type: " + columnClass);
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    advancedViewState.addDataListener(this::onAdvancedViewChange);
    conditionModel.operatorValue().addDataListener(this::onOperatorChanged);
    FocusGainedListener focusGainedListener = new FocusGainedListener();
    operatorCombo.addFocusListener(focusGainedListener);
    KeyEvents.Builder enableOnEnterKeyEvent = KeyEvents.builder(KeyEvent.VK_ENTER)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .action(Control.control(() -> conditionModel.setEnabled(!conditionModel.isEnabled())));
    enableOnEnterKeyEvent.enable(operatorCombo);
    if (equalField != null) {
      equalField.addFocusListener(focusGainedListener);
      enableOnEnterKeyEvent.enable(equalField);
    }
    if (upperBoundField != null) {
      upperBoundField.addFocusListener(focusGainedListener);
      enableOnEnterKeyEvent.enable(upperBoundField);
    }
    if (lowerBoundField != null) {
      lowerBoundField.addFocusListener(focusGainedListener);
      enableOnEnterKeyEvent.enable(lowerBoundField);
    }
    toggleEnabledButton.addFocusListener(focusGainedListener);
    enableOnEnterKeyEvent.enable(toggleEnabledButton);
  }

  private void onOperatorChanged(Operator operator) {
    switch (operator) {
      case EQUAL:
      case NOT_EQUAL:
        singleValuePanel(equalField);
        break;
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL:
        singleValuePanel(lowerBoundField);
        break;
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL:
        singleValuePanel(upperBoundField);
        break;
      case BETWEEN_EXCLUSIVE:
      case BETWEEN:
      case NOT_BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN:
        rangePanel();
        break;
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.getOperator());
    }
    revalidate();
    repaint();
  }

  private void onAdvancedViewChange(boolean advancedView) {
    if (advancedView) {
      setAdvancedView();
    }
    else {
      setSimpleView();
    }
  }

  private void setSimpleView() {
    Component focusOwner = getCurrentKeyboardFocusManager().getFocusOwner();
    boolean isParentOfFocusOwner = getParentOfType(ColumnConditionPanel.class, focusOwner) == this;
    if (isParentOfFocusOwner) {
      requestFocusInWindow(true);
    }
    remove(controlPanel);
    inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, inputPanel.getPreferredSize().height));
    revalidate();
    if (isParentOfFocusOwner) {
      focusOwner.requestFocusInWindow();
    }
  }

  private void setAdvancedView() {
    Component focusOwner = getCurrentKeyboardFocusManager().getFocusOwner();
    boolean isParentOfFocusOwner = getParentOfType(ColumnConditionPanel.class, focusOwner) == this;
    if (isParentOfFocusOwner) {
      requestFocusInWindow(true);
    }
    controlPanel.add(toggleEnabledButton, BorderLayout.EAST);
    add(controlPanel, BorderLayout.NORTH);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));
    revalidate();
    if (isParentOfFocusOwner) {
      focusOwner.requestFocusInWindow();
    }
  }

  private JComboBox<Item<Operator>> createOperatorComboBox(List<Operator> operators) {
    ItemComboBoxModel<Operator> operatorComboBoxModel = ItemComboBoxModel.itemComboBoxModel(operators.stream()
            .map(operator -> Item.item(operator, ColumnConditionModel.caption(operator)))
            .collect(Collectors.toList()));
    operatorComboBoxModel.setSelectedItem(operators.get(0));
    return itemComboBox(operatorComboBoxModel, conditionModel.operatorValue())
            .completionMode(Completion.Mode.NONE)
            .renderer(new OperatorComboBoxRenderer())
            .maximumRowCount(operators.size())
            .toolTipText(operatorComboBoxModel.selectedValue().value().description())
            .onBuild(comboBox -> operatorComboBoxModel.addSelectionListener(selectedOperator ->
                    comboBox.setToolTipText(selectedOperator.value().description())))
            .build();
  }

  private void initializeUI() {
    Utilities.linkToEnabledState(conditionModel.lockedObserver().reversedObserver(),
            operatorCombo, equalField, upperBoundField, lowerBoundField, toggleEnabledButton);
    setLayout(new BorderLayout());
    controlPanel.add(operatorCombo, BorderLayout.CENTER);
    onOperatorChanged(conditionModel.getOperator());
    onAdvancedViewChange(advancedViewState.get());
    addStringConfigurationPopupMenu();
  }

  private void singleValuePanel(JComponent boundField) {
    if (!Arrays.asList(inputPanel.getComponents()).contains(boundField)) {
      boolean requestFocus = boundFieldHasFocus();
      inputPanel.removeAll();
      inputPanel.add(boundField, BorderLayout.CENTER);
      if (requestFocus) {
        boundField.requestFocusInWindow();
      }
    }
  }

  private void rangePanel() {
    if (!Arrays.asList(inputPanel.getComponents()).contains(rangePanel)) {
      boolean requestFocus = boundFieldHasFocus();
      if (requestFocus) {
        //keep the focus here temporarily while we remove all
        //otherwise it jumps to the first focusable component
        inputPanel.requestFocusInWindow();
      }
      inputPanel.removeAll();
      rangePanel.add(lowerBoundField);
      rangePanel.add(upperBoundField);
      inputPanel.add(rangePanel, BorderLayout.CENTER);
      if (requestFocus) {
        lowerBoundField.requestFocusInWindow();
      }
    }
  }

  private boolean boundFieldHasFocus() {
    return equalField.hasFocus() ||
            lowerBoundField != null && lowerBoundField.hasFocus() ||
            upperBoundField != null && upperBoundField.hasFocus();
  }

  private void addStringConfigurationPopupMenu() {
    if (conditionModel.columnClass().equals(String.class)) {
      JPopupMenu popupMenu = Controls.builder()
              .control(ToggleControl.builder(conditionModel.caseSensitiveState())
                      .caption(MESSAGES.getString("case_sensitive"))
                      .build())
              .controls(createAutomaticWildcardControls())
              .build().createPopupMenu();
      equalField.setComponentPopupMenu(popupMenu);
      if (lowerBoundField != null) {
        lowerBoundField.setComponentPopupMenu(popupMenu);
      }
      if (upperBoundField != null) {
        upperBoundField.setComponentPopupMenu(popupMenu);
      }
    }
  }

  private Controls createAutomaticWildcardControls() {
    Value<AutomaticWildcard> automaticWildcardValue = conditionModel.automaticWildcardValue();
    AutomaticWildcard automaticWildcard = automaticWildcardValue.get();

    State automaticWildcardNoneState = State.state(automaticWildcard.equals(AutomaticWildcard.NONE));
    State automaticWildcardPostfixState = State.state(automaticWildcard.equals(AutomaticWildcard.POSTFIX));
    State automaticWildcardPrefixState = State.state(automaticWildcard.equals(AutomaticWildcard.PREFIX));
    State automaticWildcardPrefixAndPostfixState = State.state(automaticWildcard.equals(AutomaticWildcard.PREFIX_AND_POSTFIX));

    State.group(automaticWildcardNoneState, automaticWildcardPostfixState, automaticWildcardPrefixState, automaticWildcardPrefixAndPostfixState);

    automaticWildcardNoneState.addDataListener(enabled -> {
      if (enabled) {
        automaticWildcardValue.set(AutomaticWildcard.NONE);
      }
    });
    automaticWildcardPostfixState.addDataListener(enabled -> {
      if (enabled) {
        automaticWildcardValue.set(AutomaticWildcard.POSTFIX);
      }
    });
    automaticWildcardPrefixState.addDataListener(enabled -> {
      if (enabled) {
        automaticWildcardValue.set(AutomaticWildcard.PREFIX);
      }
    });
    automaticWildcardPrefixAndPostfixState.addDataListener(enabled -> {
      if (enabled) {
        automaticWildcardValue.set(AutomaticWildcard.PREFIX_AND_POSTFIX);
      }
    });

    return Controls.builder()
            .caption(MESSAGES.getString("automatic_wildcard"))
            .control(ToggleControl.builder(automaticWildcardNoneState)
                    .caption(AutomaticWildcard.NONE.description()))
            .control(ToggleControl.builder(automaticWildcardPostfixState)
                    .caption(AutomaticWildcard.POSTFIX.description()))
            .control(ToggleControl.builder(automaticWildcardPrefixState)
                    .caption(AutomaticWildcard.PREFIX.description()))
            .control(ToggleControl.builder(automaticWildcardPrefixAndPostfixState)
                    .caption(AutomaticWildcard.PREFIX_AND_POSTFIX.description()))
            .build();
  }

  private final class FocusGainedListener extends FocusAdapter {

    @Override
    public void focusGained(FocusEvent e) {
      if (!e.isTemporary()) {
        focusGainedEvent.onEvent(conditionModel.columnIdentifier());
      }
    }
  }

  private static final class OperatorComboBoxRenderer implements ListCellRenderer<Item<Operator>> {

    private final DefaultListCellRenderer listCellRenderer = new DefaultListCellRenderer();

    private OperatorComboBoxRenderer() {
      listCellRenderer.setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Item<Operator>> list, Item<Operator> value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
      return listCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
  }
}
