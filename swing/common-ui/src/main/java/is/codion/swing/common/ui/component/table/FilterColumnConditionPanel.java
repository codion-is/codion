/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.item.Item;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.AutomaticWildcard;
import is.codion.common.model.condition.ConditionModel.Operands;
import is.codion.common.observer.Observer;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsBuilder;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static is.codion.common.Operator.*;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.linkToEnabledState;
import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.ControlKeys.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
import static javax.swing.FocusManager.getCurrentManager;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for {@link ConditionModel}.
 * For instances use {@link #builder(ConditionModel, Object)}.
 * @param <C> the column identifies type
 * @param <T> the column value type
 * @see #builder(ConditionModel, Object)
 */
public final class FilterColumnConditionPanel<C, T> extends ColumnConditionPanel<C, T> {

	private static final MessageBundle MESSAGES =
					messageBundle(FilterColumnConditionPanel.class, getBundle(FilterColumnConditionPanel.class.getName()));

	/**
	 * The condition controls.
	 */
	public static final class ControlKeys {

		/**
		 * Toggle the enabled status on/off.<br>
		 * Default key stroke: CTRL-ENTER
		 */
		public static final ControlKey<ToggleControl> TOGGLE_ENABLED = ToggleControl.key("toggleEnabled", keyStroke(VK_ENTER, CTRL_DOWN_MASK));
		/**
		 * Select the previous condition operator.<br>
		 * Default key stroke: CTRL-UP ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_OPERATOR = CommandControl.key("previousOperator", keyStroke(VK_UP, CTRL_DOWN_MASK));
		/**
		 * Select the next condition operator.<br>
		 * Default key stroke: CTRL-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_OPERATOR = CommandControl.key("nextOperator", keyStroke(VK_DOWN, CTRL_DOWN_MASK));

		private ControlKeys() {}
	}

	private static final String UNKNOWN_OPERATOR = "Unknown operator: ";
	private static final List<Operator> LOWER_BOUND_OPERATORS = asList(
					GREATER_THAN, GREATER_THAN_OR_EQUAL, BETWEEN_EXCLUSIVE, BETWEEN, NOT_BETWEEN_EXCLUSIVE, NOT_BETWEEN);
	private static final List<Operator> UPPER_BOUND_OPERATORS = asList(
					LESS_THAN, LESS_THAN_OR_EQUAL, BETWEEN_EXCLUSIVE, BETWEEN, NOT_BETWEEN_EXCLUSIVE, NOT_BETWEEN);

	private final FieldFactory<C> fieldFactory;
	private final Event<C> focusGainedEvent = Event.event();
	private final TableColumn tableColumn;

	private JToggleButton toggleEnabledButton;
	private JComboBox<Item<Operator>> operatorCombo;
	private JComponent equalField;
	private JComponent upperBoundField;
	private JComponent lowerBoundField;
	private JComponent inField;
	private JPanel controlPanel;
	private JPanel inputPanel;
	private JPanel rangePanel;

	private boolean initialized = false;

	private FilterColumnConditionPanel(DefaultBuilder<C, T> builder) {
		super(builder.condition, builder.identifier, builder.caption);
		this.fieldFactory = builder.fieldFactory;
		this.tableColumn = builder.tableColumn;
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(toggleEnabledButton, operatorCombo, equalField,
						lowerBoundField, upperBoundField, inField, controlPanel, inputPanel, rangePanel);
	}

	@Override
	public Collection<JComponent> components() {
		return Stream.of(toggleEnabledButton, operatorCombo, equalField, lowerBoundField, upperBoundField, inField)
						.filter(Objects::nonNull)
						.collect(toList());
	}

	@Override
	public void requestInputFocus() {
		switch (condition().operator().get()) {
			case EQUAL:
			case NOT_EQUAL:
				equalField().ifPresent(JComponent::requestFocusInWindow);
				break;
			case GREATER_THAN:
			case GREATER_THAN_OR_EQUAL:
			case BETWEEN_EXCLUSIVE:
			case BETWEEN:
			case NOT_BETWEEN_EXCLUSIVE:
			case NOT_BETWEEN:
				lowerBoundField().ifPresent(JComponent::requestFocusInWindow);
				break;
			case LESS_THAN:
			case LESS_THAN_OR_EQUAL:
				upperBoundField().ifPresent(JComponent::requestFocusInWindow);
				break;
			case IN:
			case NOT_IN:
				inField().ifPresent(JComponent::requestFocusInWindow);
				break;
			default:
				throw new IllegalArgumentException(UNKNOWN_OPERATOR + condition().operator().get());
		}
	}

	@Override
	public Optional<Observer<C>> focusGainedObserver() {
		return Optional.of(focusGainedEvent.observer());
	}

	/**
	 * @return the condition operator combo box
	 */
	public JComboBox<Item<Operator>> operatorComboBox() {
		initialize();

		return operatorCombo;
	}

	/**
	 * @return the JComponent used to specify the equal value
	 */
	public Optional<JComponent> equalField() {
		initialize();

		return Optional.ofNullable(equalField);
	}

	/**
	 * @return the JComponent used to specify the upper bound
	 */
	public Optional<JComponent> upperBoundField() {
		initialize();

		return Optional.ofNullable(upperBoundField);
	}

	/**
	 * @return the JComponent used to specify the lower bound
	 */
	public Optional<JComponent> lowerBoundField() {
		initialize();

		return Optional.ofNullable(lowerBoundField);
	}

	/**
	 * @return the JComponent used to specify the in values
	 */
	public Optional<JComponent> inField() {
		initialize();

		return Optional.ofNullable(inField);
	}

	/**
	 * @param condition the condition model
	 * @param identifier the column identifier
	 * @param <C> the condition identifier type
	 * @param <T> the condition value type
	 * @return a new {@link Builder}
	 */
	public static <C, T> Builder<C, T> builder(ConditionModel<T> condition, C identifier) {
		return new DefaultBuilder<>(condition, identifier);
	}

	/**
	 * Builds a {@link FilterColumnConditionPanel} instance
	 * @param <C> the column identifier type
	 * @param <T> the column value type
	 */
	public interface Builder<C, T> {

		/**
		 * @param caption the caption to use when presenting this condition panel
		 * @return this builder
		 */
		Builder<C, T> caption(String caption);

		/**
		 * @param fieldFactory the input field factory
		 * @return this builder
		 * @throws IllegalArgumentException in case the given field factory does not support the column value type
		 */
		Builder<C, T> fieldFactory(FieldFactory<C> fieldFactory);

		/**
		 * @param tableColumn the table column this condition panel represents
		 * @return this builder
		 */
		Builder<C, T> tableColumn(TableColumn tableColumn);

		/**
		 * @return a new {@link FilterColumnConditionPanel} based on this builder
		 */
		FilterColumnConditionPanel<C, T> build();
	}

	private static final class DefaultBuilder<C, T> implements Builder<C, T> {

		private final ConditionModel<T> condition;
		private final C identifier;

		private String caption;
		private FieldFactory<C> fieldFactory = new DefaultFilterFieldFactory<>();
		private TableColumn tableColumn;

		private DefaultBuilder(ConditionModel<T> condition, C identifier) {
			this.condition = requireNonNull(condition);
			this.identifier = requireNonNull(identifier);
			this.caption = identifier.toString();
		}

		@Override
		public Builder<C, T> caption(String caption) {
			this.caption = requireNonNull(caption);
			return this;
		}

		@Override
		public Builder<C, T> fieldFactory(FieldFactory<C> fieldFactory) {
			if (!requireNonNull(fieldFactory).supportsType(condition.valueClass())) {
				throw new IllegalArgumentException("Field factory does not support the value type: " + condition.valueClass());
			}

			this.fieldFactory = requireNonNull(fieldFactory);
			return this;
		}

		@Override
		public Builder<C, T> tableColumn(TableColumn tableColumn) {
			this.tableColumn = requireNonNull(tableColumn);
			return this;
		}

		@Override
		public FilterColumnConditionPanel<C, T> build() {
			return new FilterColumnConditionPanel<>(this);
		}
	}

	/**
	 * Provides equal, upper and lower bound input fields for a {@link FilterColumnConditionPanel}
	 */
	public interface FieldFactory<C> {

		/**
		 * @param valueClass the value class
		 * @return true if the type is supported
		 */
		boolean supportsType(Class<?> valueClass);

		/**
		 * Creates the field representing the {@link Operator#EQUAL} and {@link Operator#NOT_EQUAL} operand, linked to {@link Operands#equal()}
		 * @param condition the condition model
		 * @param identifier the column identifier
		 * @return the equal value field
		 * @throws IllegalArgumentException in case the bound type is not supported
		 */
		JComponent createEqualField(ConditionModel<?> condition, C identifier);

		/**
		 * Creates the field representing the upper bound operand, linked to {@link Operands#upperBound()}
		 * @param condition the condition model
		 * @param identifier the column identifier
		 * @return an upper bound input field, or an empty Optional if it does not apply to the bound type
		 * @throws IllegalArgumentException in case the bound type is not supported
		 */
		Optional<JComponent> createUpperBoundField(ConditionModel<?> condition, C identifier);

		/**
		 * Creates the field representing the lower bound operand, linked to {@link Operands#lowerBound()}
		 * @param condition the condition model
		 * @param identifier the column identifier
		 * @return a lower bound input field, or an empty Optional if it does not apply to the bound type
		 * @throws IllegalArgumentException in case the bound type is not supported
		 */
		Optional<JComponent> createLowerBoundField(ConditionModel<?> condition, C identifier);

		/**
		 * Creates the field representing the {@link Operator#IN} operands, linked to {@link Operands#in()}
		 * @param condition the condition model
		 * @param identifier the column identifier
		 * @return the in value field
		 * @throws IllegalArgumentException in case the bound type is not supported
		 */
		JComponent createInField(ConditionModel<?> condition, C identifier);
	}

	private JComponent createEqualField(FieldFactory<C> fieldFactory) {
		return equalFieldRequired() ? fieldFactory.createEqualField(condition(), identifier()) : null;
	}

	private JComponent createUpperBoundField(FieldFactory<C> fieldFactory) {
		return upperBoundFieldRequired() ? fieldFactory.createUpperBoundField(condition(), identifier()).orElse(null) : null;
	}

	private JComponent createLowerBoundField(FieldFactory<C> fieldFactory) {
		return lowerBoundFieldRequired() ? fieldFactory.createLowerBoundField(condition(), identifier()).orElse(null) : null;
	}

	private JComponent createInField(FieldFactory<C> fieldFactory) {
		return inFieldRequired() ? fieldFactory.createInField(condition(), identifier()) : null;
	}

	private boolean equalFieldRequired() {
		return condition().operators().contains(EQUAL) ||
						condition().operators().contains(NOT_EQUAL);
	}

	private boolean upperBoundFieldRequired() {
		return condition().operators().stream()
						.anyMatch(UPPER_BOUND_OPERATORS::contains);
	}

	private boolean lowerBoundFieldRequired() {
		return condition().operators().stream()
						.anyMatch(LOWER_BOUND_OPERATORS::contains);
	}

	private boolean inFieldRequired() {
		return condition().operators().contains(IN) ||
						condition().operators().contains(NOT_IN);
	}

	private void initialize() {
		if (!initialized) {
			setLayout(new BorderLayout());
			createComponents();
			bindEvents();
			controlPanel.add(operatorCombo, BorderLayout.CENTER);
			addStringConfigurationPopupMenu();
			onOperatorChanged(condition().operator().get());
			initialized = true;
		}
	}

	private void createComponents() {
		controlPanel = new JPanel(new BorderLayout());
		inputPanel = new JPanel(new BorderLayout());
		rangePanel = new JPanel(new GridLayout(1, 2));
		toggleEnabledButton = radioButton(condition().enabled())
						.horizontalAlignment(CENTER)
						.popupMenu(radioButton -> menu(Controls.builder()
										.control(Control.builder()
														.toggle(condition().autoEnable())
														.name(MESSAGES.getString("auto_enable"))).build())
										.buildPopupMenu())
						.build();
		boolean modelLocked = condition().locked().get();
		condition().locked().set(false);//otherwise, the validator checking the locked state kicks in during value linking
		equalField = createEqualField(fieldFactory);
		upperBoundField = createUpperBoundField(fieldFactory);
		lowerBoundField = createLowerBoundField(fieldFactory);
		inField = createInField(fieldFactory);
		operatorCombo = createOperatorComboBox(condition().operators());
		condition().locked().set(modelLocked);
		components().forEach(this::configureHorizontalAlignment);
	}

	private void configureHorizontalAlignment(JComponent component) {
		if (component instanceof JCheckBox) {
			((JCheckBox) component).setHorizontalAlignment(CENTER);
		}
		else if (tableColumn != null) {
			TableCellRenderer cellRenderer = tableColumn.getCellRenderer();
			if (cellRenderer instanceof DefaultTableCellRenderer) {
				int horizontalAlignment = ((DefaultTableCellRenderer) cellRenderer).getHorizontalAlignment();
				if (component instanceof JTextField) {
					((JTextField) component).setHorizontalAlignment(horizontalAlignment);
				}
				else if (component instanceof JComboBox) {
					Component editorComponent = ((JComboBox<?>) component).getEditor().getEditorComponent();
					if (editorComponent instanceof JTextField) {
						((JTextField) editorComponent).setHorizontalAlignment(horizontalAlignment);
					}
				}
			}
		}
	}

	private void bindEvents() {
		condition().operator().addConsumer(this::onOperatorChanged);
		linkToEnabledState(condition().locked().not(),
						operatorCombo, equalField, upperBoundField, lowerBoundField, toggleEnabledButton);
		components().forEach(component ->
						component.addFocusListener(new FocusGained(identifier())));

		Collection<JComponent> fields = Stream.of(operatorCombo, toggleEnabledButton, equalField, upperBoundField, lowerBoundField, inField)
						.filter(Objects::nonNull)
						.collect(toList());
		TOGGLE_ENABLED.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(command(this::toggleEnabled))
										.enable(fields));
		PREVIOUS_OPERATOR.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(command(this::selectPreviousOperator))
										.enable(fields));
		NEXT_OPERATOR.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(command(this::selectNextOperator))
										.enable(fields));
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
			case IN:
			case NOT_IN:
				singleValuePanel(inField);
				break;
			default:
				throw new IllegalArgumentException(UNKNOWN_OPERATOR + condition().operator().get());
		}
		revalidate();
		repaint();
	}

	protected void onStateChanged(ConditionState conditionState) {
		switch (conditionState) {
			case HIDDEN:
				setHidden();
				break;
			case SIMPLE:
				setSimple();
				break;
			case ADVANCED:
				setAdvanced();
				break;
			default:
				throw new IllegalArgumentException("Unknown panel state: " + conditionState);
		}
		revalidate();
	}

	private void setHidden() {
		removeAll();
	}

	private void setSimple() {
		initialize();
		KeyboardFocusManager focusManager = getCurrentKeyboardFocusManager();
		Component focusOwner = focusManager.getFocusOwner();
		boolean parentOfFocusOwner = parentOfType(FilterColumnConditionPanel.class, focusOwner) == this;
		if (parentOfFocusOwner) {
			focusManager.clearFocusOwner();
		}
		remove(controlPanel);
		inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
		add(inputPanel, BorderLayout.CENTER);
		setPreferredSize(new Dimension(getPreferredSize().width, inputPanel.getPreferredSize().height));
		revalidate();
		if (parentOfFocusOwner) {
			focusOwner.requestFocusInWindow();
		}
	}

	private void setAdvanced() {
		initialize();
		KeyboardFocusManager focusManager = getCurrentKeyboardFocusManager();
		Component focusOwner = focusManager.getFocusOwner();
		boolean parentOfFocusOwner = parentOfType(FilterColumnConditionPanel.class, focusOwner) == this;
		if (parentOfFocusOwner) {
			focusManager.clearFocusOwner();
		}
		controlPanel.add(toggleEnabledButton, BorderLayout.EAST);
		add(controlPanel, BorderLayout.NORTH);
		add(inputPanel, BorderLayout.CENTER);
		setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));
		revalidate();
		if (parentOfFocusOwner) {
			focusOwner.requestFocusInWindow();
		}
	}

	private JComboBox<Item<Operator>> createOperatorComboBox(List<Operator> operators) {
		ItemComboBoxModel<Operator> operatorComboBoxModel = ItemComboBoxModel.itemComboBoxModel(operators.stream()
						.map(operator -> Item.item(operator, caption(operator)))
						.collect(toList()));

		return itemComboBox(operatorComboBoxModel, condition().operator())
						.completionMode(Completion.Mode.NONE)
						.renderer(new OperatorComboBoxRenderer())
						.maximumRowCount(operators.size())
						.mouseWheelScrollingWithWrapAround(true)
						.toolTipText(condition().operator().get().description())
						.onBuild(comboBox -> operatorComboBoxModel.selection().item().addConsumer(selectedOperator ->
										comboBox.setToolTipText(selectedOperator.value().description())))
						.build();
	}

	private void selectNextOperator() {
		ItemComboBoxModel<Operator> itemComboBoxModel = (ItemComboBoxModel<Operator>) operatorCombo.getModel();
		List<Item<Operator>> visibleItems = itemComboBoxModel.items().visible().get();
		int index = visibleItems.indexOf(itemComboBoxModel.getSelectedItem());
		if (index < itemComboBoxModel.items().visible().count() - 1) {
			itemComboBoxModel.setSelectedItem(visibleItems.get(index + 1));
		}
		else {
			itemComboBoxModel.setSelectedItem(visibleItems.get(0));
		}
	}

	private void toggleEnabled() {
		condition().enabled().set(!condition().enabled().get());
	}

	private void selectPreviousOperator() {
		ItemComboBoxModel<Operator> itemComboBoxModel = (ItemComboBoxModel<Operator>) operatorCombo.getModel();
		List<Item<Operator>> visibleItems = itemComboBoxModel.items().visible().get();
		int index = visibleItems.indexOf(itemComboBoxModel.getSelectedItem());
		if (index > 0) {
			itemComboBoxModel.setSelectedItem(visibleItems.get(index - 1));
		}
		else {
			itemComboBoxModel.setSelectedItem(visibleItems.get(visibleItems.size() - 1));
		}
	}

	private void singleValuePanel(JComponent boundField) {
		if (!asList(inputPanel.getComponents()).contains(boundField)) {
			boolean boundFieldHasFocus = boundFieldHasFocus();
			clearInputPanel(boundFieldHasFocus);
			inputPanel.add(boundField, BorderLayout.CENTER);
			if (state().isEqualTo(ConditionState.SIMPLE)) {
				inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
			}
			if (boundFieldHasFocus) {
				boundField.requestFocusInWindow();
			}
		}
	}

	private void rangePanel() {
		if (!asList(inputPanel.getComponents()).contains(rangePanel)) {
			boolean boundFieldHasFocus = boundFieldHasFocus();
			clearInputPanel(boundFieldHasFocus);
			rangePanel.add(lowerBoundField);
			rangePanel.add(upperBoundField);
			inputPanel.add(rangePanel, BorderLayout.CENTER);
			if (state().isEqualTo(ConditionState.SIMPLE)) {
				inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
			}
			if (boundFieldHasFocus) {
				lowerBoundField.requestFocusInWindow();
			}
		}
	}

	private void clearInputPanel(boolean clearFocus) {
		if (clearFocus) {
			//clear the focus here temporarily while we remove all
			//otherwise it jumps to the first focusable component
			getCurrentManager().clearFocusOwner();
		}
		inputPanel.removeAll();
	}

	private boolean boundFieldHasFocus() {
		return boundFieldHasFocus(equalField) ||
						boundFieldHasFocus(lowerBoundField) ||
						boundFieldHasFocus(upperBoundField) ||
						boundFieldHasFocus(inField);
	}

	private void addStringConfigurationPopupMenu() {
		if (isStringOrCharacter()) {
			ControlsBuilder controlsBuilder = Controls.builder();
			controlsBuilder.control(Control.builder()
							.toggle(condition().caseSensitive())
							.name(MESSAGES.getString("case_sensitive")));
			if (condition().valueClass().equals(String.class)) {
				controlsBuilder.control(createAutomaticWildcardControls());
			}
			JPopupMenu popupMenu = menu(controlsBuilder).buildPopupMenu();
			Stream.of(equalField, lowerBoundField, upperBoundField, inField)
							.filter(Objects::nonNull)
							.forEach(field -> field.setComponentPopupMenu(popupMenu));
		}
	}

	private boolean isStringOrCharacter() {
		return condition().valueClass().equals(String.class) || condition().valueClass().equals(Character.class);
	}

	private Controls createAutomaticWildcardControls() {
		Value<AutomaticWildcard> automaticWildcardValue = condition().automaticWildcard();
		AutomaticWildcard automaticWildcard = automaticWildcardValue.get();

		State automaticWildcardNoneState = State.state(automaticWildcard.equals(AutomaticWildcard.NONE));
		State automaticWildcardPostfixState = State.state(automaticWildcard.equals(AutomaticWildcard.POSTFIX));
		State automaticWildcardPrefixState = State.state(automaticWildcard.equals(AutomaticWildcard.PREFIX));
		State automaticWildcardPrefixAndPostfixState = State.state(automaticWildcard.equals(AutomaticWildcard.PREFIX_AND_POSTFIX));

		State.group(automaticWildcardNoneState, automaticWildcardPostfixState, automaticWildcardPrefixState, automaticWildcardPrefixAndPostfixState);

		automaticWildcardNoneState.addConsumer(enabled -> {
			if (enabled) {
				automaticWildcardValue.set(AutomaticWildcard.NONE);
			}
		});
		automaticWildcardPostfixState.addConsumer(enabled -> {
			if (enabled) {
				automaticWildcardValue.set(AutomaticWildcard.POSTFIX);
			}
		});
		automaticWildcardPrefixState.addConsumer(enabled -> {
			if (enabled) {
				automaticWildcardValue.set(AutomaticWildcard.PREFIX);
			}
		});
		automaticWildcardPrefixAndPostfixState.addConsumer(enabled -> {
			if (enabled) {
				automaticWildcardValue.set(AutomaticWildcard.PREFIX_AND_POSTFIX);
			}
		});

		return Controls.builder()
						.name(MESSAGES.getString("automatic_wildcard"))
						.control(Control.builder()
										.toggle(automaticWildcardNoneState)
										.name(AutomaticWildcard.NONE.description()))
						.control(Control.builder()
										.toggle(automaticWildcardPostfixState)
										.name(AutomaticWildcard.POSTFIX.description()))
						.control(Control.builder()
										.toggle(automaticWildcardPrefixState)
										.name(AutomaticWildcard.PREFIX.description()))
						.control(Control.builder()
										.toggle(automaticWildcardPrefixAndPostfixState)
										.name(AutomaticWildcard.PREFIX_AND_POSTFIX.description()))
						.build();
	}

	private static boolean boundFieldHasFocus(JComponent field) {
		if (field == null) {
			return false;
		}
		if (field.hasFocus()) {
			return true;
		}
		if (field instanceof JComboBox) {
			return ((JComboBox<?>) field).getEditor().getEditorComponent().hasFocus();
		}

		return false;
	}

	private static String caption(Operator operator) {
		switch (requireNonNull(operator)) {
			case EQUAL:
				return "α =";
			case NOT_EQUAL:
				return "α ≠";
			case LESS_THAN:
				return "α <";
			case LESS_THAN_OR_EQUAL:
				return "α ≤";
			case GREATER_THAN:
				return "α >";
			case GREATER_THAN_OR_EQUAL:
				return "α ≥";
			case BETWEEN_EXCLUSIVE:
				return "< α <";
			case BETWEEN:
				return "≤ α ≤";
			case NOT_BETWEEN_EXCLUSIVE:
				return "≥ α ≥";
			case NOT_BETWEEN:
				return "> α >";
			case IN:
				return "α ∈";
			case NOT_IN:
				return "α ∉";
			default:
				throw new IllegalArgumentException(UNKNOWN_OPERATOR + operator);
		}
	}

	private final class FocusGained extends FocusAdapter {

		private final C identifier;

		private FocusGained(C identifier) {
			this.identifier = identifier;
		}

		@Override
		public void focusGained(FocusEvent e) {
			focusGainedEvent.accept(identifier);
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
