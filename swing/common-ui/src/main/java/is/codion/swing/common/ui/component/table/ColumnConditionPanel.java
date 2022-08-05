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
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
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
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for ColumnConditionModel
 * @param <C> the type of objects used to identify columns
 * @param <T> the column value type
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
  private final State advancedConditionState = State.state();

  private JDialog dialog;

  /**
   * Instantiates a new ColumnConditionPanel, with a default bound field factory.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   */
  public ColumnConditionPanel(ColumnConditionModel<C, T> conditionModel, ToggleAdvancedButton toggleAdvancedButton) {
    this(conditionModel, toggleAdvancedButton, new DefaultBoundFieldFactory<>(conditionModel));
  }

  /**
   * Instantiates a new ColumnConditionPanel.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param boundFieldFactory the input field factory
   * @throws IllegalArgumentException in case operators is empty
   */
  public ColumnConditionPanel(ColumnConditionModel<C, T> conditionModel, ToggleAdvancedButton toggleAdvancedButton,
                              BoundFieldFactory boundFieldFactory) {
    requireNonNull(conditionModel, "conditionModel");
    requireNonNull(boundFieldFactory, "boundFieldFactory");
    this.conditionModel = conditionModel;
    boolean modelLocked = conditionModel.isLocked();
    conditionModel.setLocked(false);//otherwise, the validator checking the locked state kicks in during value linking
    this.equalField = boundFieldFactory.createEqualField();
    this.upperBoundField = boundFieldFactory.createUpperBoundField().orElse(null);
    this.lowerBoundField = boundFieldFactory.createLowerBoundField().orElse(null);
    this.operatorCombo = initializeOperatorComboBox(conditionModel.getOperators());
    this.toggleEnabledButton = radioButton(conditionModel.getEnabledState())
            .horizontalAlignment(CENTER)
            .build();
    this.toggleAdvancedButton = toggleAdvancedButton == ToggleAdvancedButton.YES ? toggleButton(advancedConditionState)
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
  public ColumnConditionModel<C, T> getModel() {
    return this.conditionModel;
  }

  /**
   * @return true if the dialog is enabled
   */
  public boolean isDialogEnabled() {
    return dialog != null;
  }

  /**
   * @return true if the dialog is being shown
   */
  public boolean isDialogVisible() {
    return dialog != null && dialog.isVisible();
  }

  /**
   * Displays this condition panel in a dialog
   * @param dialogParent the dialog parent
   * @param title the dialog title
   */
  public void enableDialog(Container dialogParent, String title) {
    if (!isDialogEnabled()) {
      initializeConditionDialog(dialogParent, title);
    }
  }

  /**
   * Displays this panel in a dialog
   * @param position the location, used if specified
   */
  public void showDialog(Point position) {
    if (!isDialogEnabled()) {
      throw new IllegalStateException("Dialog has not been enabled for this condition panel");
    }
    if (!isDialogVisible()) {
      if (position != null) {
        Point adjustedPosition = position;
        adjustedPosition.y = adjustedPosition.y - dialog.getHeight();
        dialog.setLocation(adjustedPosition);
      }
      dialog.setVisible(true);
      requestInputFocus();
    }
  }

  /**
   * Hides the dialog showing this panel if visible
   */
  public void hideDialog() {
    if (isDialogVisible()) {
      dialog.setVisible(false);
      dialog.dispose();
    }
  }

  /**
   * @return the dialog used to show this filter panel
   */
  public JDialog getDialog() {
    return dialog;
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
   * @param advanced true if advanced condition should be enabled
   */
  public void setAdvanced(boolean advanced) {
    advancedConditionState.set(advanced);
  }

  /**
   * @return true if the advanced condition is enabled
   */
  public boolean isAdvanced() {
    return advancedConditionState.get();
  }

  /**
   * @return the condition operator combo box
   */
  public JComboBox<Operator> getOperatorComboBox() {
    return operatorCombo;
  }

  /**
   * @return the JComponent used to specify the equal value
   */
  public JComponent getEqualField() {
    return equalField;
  }

  /**
   * @return the JComponent used to specify the upper bound
   */
  public JComponent getUpperBoundField() {
    return upperBoundField;
  }

  /**
   * @return the JComponent used to specify the lower bound
   */
  public JComponent getLowerBoundField() {
    return lowerBoundField;
  }

  /**
   * @param listener a listener notified each time the advanced condition state changes
   */
  public void addAdvancedListener(EventDataListener<Boolean> listener) {
    advancedConditionState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeAdvancedListener(EventDataListener<Boolean> listener) {
    advancedConditionState.removeDataListener(listener);
  }

  /**
   * @param listener listener notified when this condition panels input fields receive focus
   */
  public void addFocusGainedListener(EventDataListener<C> listener) {
    focusGainedEvent.addDataListener(listener);
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
      return createField(columnConditionModel.getEqualValueSet().value());
    }

    @Override
    public Optional<JComponent> createUpperBoundField() {
      if (columnConditionModel.getColumnClass().equals(Boolean.class)) {
        return Optional.empty();//no lower bound field required for boolean values
      }

      return Optional.of(createField(columnConditionModel.getUpperBoundValue()));
    }

    @Override
    public Optional<JComponent> createLowerBoundField() {
      if (columnConditionModel.getColumnClass().equals(Boolean.class)) {
        return Optional.empty();//no lower bound field required for boolean values
      }

      return Optional.of(createField(columnConditionModel.getLowerBoundValue()));
    }

    private JComponent createField(Value<?> value) {
      Class<?> columnClass = columnConditionModel.getColumnClass();
      if (columnClass.equals(Boolean.class)) {
        return checkBox((Value<Boolean>) value)
                .nullable(true)
                .horizontalAlignment(CENTER)
                .build();
      }
      if (columnClass.equals(Integer.class)) {
        return integerField((Value<Integer>) value)
                .format(columnConditionModel.getFormat())
                .build();
      }
      else if (columnClass.equals(Double.class)) {
        return doubleField((Value<Double>) value)
                .format(columnConditionModel.getFormat())
                .build();
      }
      else if (columnClass.equals(BigDecimal.class)) {
        return bigDecimalField((Value<BigDecimal>) value)
                .format(columnConditionModel.getFormat())
                .build();
      }
      else if (columnClass.equals(Long.class)) {
        return longField((Value<Long>) value)
                .format(columnConditionModel.getFormat())
                .build();
      }
      else if (columnClass.equals(LocalTime.class)) {
        return localTimeField(columnConditionModel.getDateTimePattern(), (Value<LocalTime>) value)
                .build();
      }
      else if (columnClass.equals(LocalDate.class)) {
        return localDateField(columnConditionModel.getDateTimePattern(), (Value<LocalDate>) value)
                .build();
      }
      else if (columnClass.equals(LocalDateTime.class)) {
        return localDateTimeField(columnConditionModel.getDateTimePattern(), (Value<LocalDateTime>) value)
                .build();
      }
      else if (columnClass.equals(OffsetDateTime.class)) {
        return offsetDateTimeField(columnConditionModel.getDateTimePattern(), (Value<OffsetDateTime>) value)
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
    advancedConditionState.addDataListener(this::onAdvancedChange);
    conditionModel.getOperatorValue().addDataListener(this::onOperatorChanged);
    FocusAdapter focusGainedListener = new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (!e.isTemporary()) {
          focusGainedEvent.onEvent(conditionModel.getColumnIdentifier());
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

  private void onAdvancedChange(boolean advanced) {
    if (advanced) {
      setAdvanced();
    }
    else {
      setSimple();
    }
  }

  private void setSimple() {
    remove(controlPanel);
    setupButtonPanel();
    inputPanel.add(buttonPanel, BorderLayout.EAST);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, inputPanel.getPreferredSize().height));
    revalidate();
  }

  private void setAdvanced() {
    setupButtonPanel();
    controlPanel.add(buttonPanel, BorderLayout.EAST);
    add(controlPanel, BorderLayout.NORTH);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));
    revalidate();
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

  private JComboBox<Operator> initializeOperatorComboBox(List<Operator> operators) {
    SwingFilteredComboBoxModel<Operator> operatorComboBoxModel = new SwingFilteredComboBoxModel<>();
    operatorComboBoxModel.setContents(operators);
    operatorComboBoxModel.setSelectedItem(operators.get(0));
    return comboBox(operatorComboBoxModel, conditionModel.getOperatorValue())
            .completionMode(Completion.Mode.NONE)
            .renderer(new OperatorComboBoxRenderer())
            .font(UIManager.getFont("ComboBox.font").deriveFont(OPERATOR_FONT_SIZE))
            .maximumRowCount(operators.size())
            .toolTipText(operatorComboBoxModel.getSelectedValue().getDescription())
            .onBuild(comboBox -> operatorComboBoxModel.addSelectionListener(selectedOperator ->
                    comboBox.setToolTipText(selectedOperator.getDescription())))
            .build();
  }

  private void initializeUI() {
    Utilities.linkToEnabledState(conditionModel.getLockedObserver().reversedObserver(),
            operatorCombo, equalField, upperBoundField, lowerBoundField, toggleAdvancedButton, toggleEnabledButton);
    setLayout(new BorderLayout());
    controlPanel.add(operatorCombo, BorderLayout.CENTER);
    onOperatorChanged(conditionModel.getOperator());
    onAdvancedChange(advancedConditionState.get());
    addStringConfigurationPopupMenu();
  }

  private void initializeConditionDialog(Container parent, String title) {
    if (dialog != null) {
      return;
    }

    JDialog dialogParent = Utilities.getParentDialog(parent).orElse(null);
    if (dialogParent != null) {
      dialog = new JDialog(dialogParent, title, false);
    }
    else {
      dialog = new JDialog(Utilities.getParentFrame(parent).orElse(null), title, false);
    }

    dialog.getContentPane().add(this);
    dialog.pack();

    addAdvancedListener(advanced -> dialog.pack());
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
    if (conditionModel.getColumnClass().equals(String.class)) {
      JPopupMenu popupMenu = Controls.builder()
              .control(ToggleControl.builder(conditionModel.getCaseSensitiveState())
                      .caption(MESSAGES.getString("case_sensitive"))
                      .build())
              .controls(createAutomaticWildcardControls())
              .build()
              .createPopupMenu();
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
    Value<AutomaticWildcard> automaticWildcardValue = conditionModel.getAutomaticWildcardValue();
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
                    .caption(AutomaticWildcard.NONE.getDescription()))
            .control(ToggleControl.builder(automaticWildcardPostfixState)
                    .caption(AutomaticWildcard.POSTFIX.getDescription()))
            .control(ToggleControl.builder(automaticWildcardPrefixState)
                    .caption(AutomaticWildcard.PREFIX.getDescription()))
            .control(ToggleControl.builder(automaticWildcardPrefixAndPostfixState)
                    .caption(AutomaticWildcard.PREFIX_AND_POSTFIX.getDescription()))
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
