/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.SwingFilteredComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.combobox.Completion;
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
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.*;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for {@link ColumnConditionModel}.
 * For instances use the {@link #columnConditionPanel(ColumnConditionModel, ToggleAdvancedButton)} or
 * {@link #columnConditionPanel(ColumnConditionModel, ToggleAdvancedButton, BoundFieldFactory)} factory methods.
 * @param <C> the type of objects used to identify columns
 * @param <T> the column value type
 * @see #columnConditionPanel(ColumnConditionModel, ToggleAdvancedButton)
 * @see #columnConditionPanel(ColumnConditionModel, ToggleAdvancedButton, BoundFieldFactory)
 */
public final class ColumnConditionPanel<C, T> extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ColumnConditionPanel.class.getName());

  /**
   * Specifies whether a condition panel should include
   * a button for toggling advanced mode.
   */
  public enum ToggleAdvancedButton {
    /**
     * Include a button for toggling advancded mode.
     */
    YES,
    /**
     * Don't include a button for toggling advancded mode.
     */
    NO
  }

  private static final float OPERATOR_FONT_SIZE = 18f;

  private final ColumnConditionModel<C, T> conditionModel;
  private final JToggleButton toggleEnabledButton;
  private final JToggleButton toggleAdvancedButton;
  private final JComboBox<Operator> operatorCombo;
  private final JComponent equalField;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;
  private final JPanel buttonPanel = new JPanel();
  private final JPanel controlPanel = new JPanel(new BorderLayout());
  private final JPanel inputPanel = new JPanel(new BorderLayout());
  private final JPanel rangePanel = new JPanel(new GridLayout(1, 2));

  private final Event<C> focusGainedEvent = Event.event();
  private final State advancedViewState = State.state();

  private ColumnConditionPanel(ColumnConditionModel<C, T> conditionModel, ToggleAdvancedButton toggleAdvancedButton,
                               BoundFieldFactory boundFieldFactory) {
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
            .build();
    this.toggleAdvancedButton = toggleAdvancedButton == ToggleAdvancedButton.YES ? toggleButton(advancedViewState)
            .caption("...")
            .build() : null;
    conditionModel.setLocked(modelLocked);
    initializeUI();
    bindEvents();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(toggleEnabledButton, toggleAdvancedButton, operatorCombo, equalField,
            lowerBoundField, upperBoundField, buttonPanel, controlPanel, inputPanel, rangePanel);
  }

  /**
   * @return the condition model this panel uses
   */
  public ColumnConditionModel<C, T> model() {
    return this.conditionModel;
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
  public JComboBox<Operator> operatorComboBox() {
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
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param <C> the type of objects used to identify columns
   * @param <T> the column value type
   * @return a new {@link ColumnConditionPanel} instance
   */
  public static <C, T> ColumnConditionPanel<C, T> columnConditionPanel(ColumnConditionModel<C, T> conditionModel,
                                                                       ToggleAdvancedButton toggleAdvancedButton) {
    return columnConditionPanel(conditionModel, toggleAdvancedButton, new DefaultBoundFieldFactory<>(conditionModel));
  }

  /**
   * Instantiates a new {@link ColumnConditionPanel}.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param boundFieldFactory the input field factory
   * @param <C> the type of objects used to identify columns
   * @param <T> the column value type
   * @return a new {@link ColumnConditionPanel} instance
   */
  public static <C, T> ColumnConditionPanel<C, T> columnConditionPanel(ColumnConditionModel<C, T> conditionModel,
                                                                       ToggleAdvancedButton toggleAdvancedButton,
                                                                       BoundFieldFactory boundFieldFactory) {
    return new ColumnConditionPanel<>(conditionModel, toggleAdvancedButton, boundFieldFactory);
  }

  /**
   * Provides equal, upper and lower bound input fields for a ColumnConditionPanel
   */
  public interface BoundFieldFactory {

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
        return Optional.empty();//no lower bound field required for boolean values
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

    private JComponent createField(Value<?> value) {
      Class<?> columnClass = columnConditionModel.columnClass();
      if (columnClass.equals(Boolean.class)) {
        return checkBox((Value<Boolean>) value)
                .nullable(true)
                .horizontalAlignment(CENTER)
                .build();
      }
      if (columnClass.equals(Integer.class)) {
        return integerField((Value<Integer>) value)
                .format(columnConditionModel.format())
                .build();
      }
      else if (columnClass.equals(Double.class)) {
        return doubleField((Value<Double>) value)
                .format(columnConditionModel.format())
                .build();
      }
      else if (columnClass.equals(BigDecimal.class)) {
        return bigDecimalField((Value<BigDecimal>) value)
                .format(columnConditionModel.format())
                .build();
      }
      else if (columnClass.equals(Long.class)) {
        return longField((Value<Long>) value)
                .format(columnConditionModel.format())
                .build();
      }
      else if (columnClass.equals(LocalTime.class)) {
        return localTimeField(columnConditionModel.dateTimePattern(), (Value<LocalTime>) value)
                .build();
      }
      else if (columnClass.equals(LocalDate.class)) {
        return localDateField(columnConditionModel.dateTimePattern(), (Value<LocalDate>) value)
                .build();
      }
      else if (columnClass.equals(LocalDateTime.class)) {
        return localDateTimeField(columnConditionModel.dateTimePattern(), (Value<LocalDateTime>) value)
                .build();
      }
      else if (columnClass.equals(OffsetDateTime.class)) {
        return offsetDateTimeField(columnConditionModel.dateTimePattern(), (Value<OffsetDateTime>) value)
                .build();
      }
      else if (columnClass.equals(String.class)) {
        return textField((Value<String>) value)
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
    FocusAdapter focusGainedListener = new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (!e.isTemporary()) {
          focusGainedEvent.onEvent(conditionModel.columnIdentifier());
        }
      }
    };
    operatorCombo.addFocusListener(focusGainedListener);
    if (equalField != null) {
      equalField.addFocusListener(focusGainedListener);
    }
    if (upperBoundField != null) {
      upperBoundField.addFocusListener(focusGainedListener);
    }
    if (lowerBoundField != null) {
      lowerBoundField.addFocusListener(focusGainedListener);
    }
    if (toggleAdvancedButton != null) {
      toggleAdvancedButton.addFocusListener(focusGainedListener);
    }
    toggleEnabledButton.addFocusListener(focusGainedListener);
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
    boolean isParentOfFocusOwner = Utilities.getParentOfType(ColumnConditionPanel.class, focusOwner).orElse(null) == this;
    if (isParentOfFocusOwner) {
      requestFocusInWindow(true);
    }
    remove(controlPanel);
    setupButtonPanel();
    inputPanel.add(buttonPanel, BorderLayout.EAST);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, inputPanel.getPreferredSize().height));
    revalidate();
    if (isParentOfFocusOwner) {
      focusOwner.requestFocusInWindow();
    }
  }

  private void setAdvancedView() {
    Component focusOwner = getCurrentKeyboardFocusManager().getFocusOwner();
    boolean isParentOfFocusOwner = Utilities.getParentOfType(ColumnConditionPanel.class, focusOwner).orElse(null) == this;
    if (isParentOfFocusOwner) {
      requestFocusInWindow(true);
    }
    setupButtonPanel();
    controlPanel.add(buttonPanel, BorderLayout.EAST);
    add(controlPanel, BorderLayout.NORTH);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));
    revalidate();
    if (isParentOfFocusOwner) {
      focusOwner.requestFocusInWindow();
    }
  }

  private void setupButtonPanel() {
    buttonPanel.setLayout(new GridLayout(1, toggleAdvancedButton == null ? 1 : 2));
    if (toggleAdvancedButton != null) {
      buttonPanel.add(toggleAdvancedButton);
      buttonPanel.add(toggleEnabledButton);
    }
    else {
      buttonPanel.add(toggleEnabledButton);
    }
  }

  private JComboBox<Operator> createOperatorComboBox(List<Operator> operators) {
    SwingFilteredComboBoxModel<Operator> operatorComboBoxModel = new SwingFilteredComboBoxModel<>();
    operatorComboBoxModel.setContents(operators);
    operatorComboBoxModel.setSelectedItem(operators.get(0));
    return comboBox(operatorComboBoxModel, conditionModel.operatorValue())
            .completionMode(Completion.Mode.NONE)
            .renderer(new OperatorComboBoxRenderer())
            .font(UIManager.getFont("ComboBox.font").deriveFont(OPERATOR_FONT_SIZE))
            .maximumRowCount(operators.size())
            .toolTipText(operatorComboBoxModel.selectedValue().description())
            .onBuild(comboBox -> operatorComboBoxModel.addSelectionListener(selectedOperator ->
                    comboBox.setToolTipText(selectedOperator.description())))
            .build();
  }

  private void initializeUI() {
    Utilities.linkToEnabledState(conditionModel.lockedObserver().reversedObserver(),
            operatorCombo, equalField, upperBoundField, lowerBoundField, toggleAdvancedButton, toggleEnabledButton);
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
    Controls.Builder builder = Controls.builder()
            .control(ToggleControl.builder(conditionModel.autoEnableState())
                    .caption(MESSAGES.getString("auto_enable")));
    if (conditionModel.columnClass().equals(String.class)) {
      builder.control(ToggleControl.builder(conditionModel.caseSensitiveState())
                      .caption(MESSAGES.getString("case_sensitive"))
                      .build())
              .controls(createAutomaticWildcardControls());
    }
    JPopupMenu popupMenu = builder.build().createPopupMenu();
    equalField.setComponentPopupMenu(popupMenu);
    if (lowerBoundField != null) {
      lowerBoundField.setComponentPopupMenu(popupMenu);
    }
    if (upperBoundField != null) {
      upperBoundField.setComponentPopupMenu(popupMenu);
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

  private static final class OperatorComboBoxRenderer implements ListCellRenderer<Operator> {

    private final DefaultListCellRenderer listCellRenderer = new DefaultListCellRenderer();

    private OperatorComboBoxRenderer() {
      listCellRenderer.setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Operator> list, Operator value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
      return listCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
  }
}
