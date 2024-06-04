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
import is.codion.common.event.EventObserver;
import is.codion.common.item.Item;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlId;
import is.codion.swing.common.ui.control.ControlKeyStrokes;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsBuilder;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.key.KeyEvents;

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
import static is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.ControlIds.*;
import static is.codion.swing.common.ui.control.Control.commandControl;
import static is.codion.swing.common.ui.control.ControlId.commandControl;
import static is.codion.swing.common.ui.control.ControlId.toggleControl;
import static is.codion.swing.common.ui.control.ControlKeyStrokes.controlKeyStrokes;
import static is.codion.swing.common.ui.control.ControlKeyStrokes.keyStroke;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for {@link ColumnConditionModel}.
 * For instances use the {@link #filterColumnConditionPanel(ColumnConditionModel, String)} or
 * {@link #filterColumnConditionPanel(ColumnConditionModel, String, FieldFactory)} factory methods.
 * @param <C> the type of objects used to identify columns
 * @param <T> the column value type
 * @see #filterColumnConditionPanel(ColumnConditionModel, String)
 * @see #filterColumnConditionPanel(ColumnConditionModel, String, FieldFactory)
 */
public final class FilterColumnConditionPanel<C, T> extends ColumnConditionPanel<C, T> {

	private static final MessageBundle MESSAGES =
					messageBundle(FilterColumnConditionPanel.class, getBundle(FilterColumnConditionPanel.class.getName()));

	/**
	 * The default keyboard shortcut keyStrokes.
	 */
	public static final ControlKeyStrokes CONTROL_KEY_STROKES = controlKeyStrokes(ControlIds.class);

	/**
	 * The condition controls.
	 */
	public static final class ControlIds {

		/**
		 * Toggle the enabled status on/off.<br>
		 * Default key stroke: CTRL-ENTER
		 */
		public static final ControlId<ToggleControl> TOGGLE_ENABLED = toggleControl(keyStroke(VK_ENTER, CTRL_DOWN_MASK));
		/**
		 * Select the previous condition operator.<br>
		 * Default key stroke: CTRL-UP ARROW
		 */
		public static final ControlId<CommandControl> PREVIOUS_OPERATOR = commandControl(keyStroke(VK_UP, CTRL_DOWN_MASK));
		/**
		 * Select the next condition operator.<br>
		 * Default key stroke: CTRL-DOWN ARROW
		 */
		public static final ControlId<CommandControl> NEXT_OPERATOR = commandControl(keyStroke(VK_DOWN, CTRL_DOWN_MASK));

		private ControlIds() {}
	}

	private static final String UNKNOWN_OPERATOR = "Unknown operator: ";
	private static final List<Operator> LOWER_BOUND_OPERATORS = asList(
					GREATER_THAN, GREATER_THAN_OR_EQUAL, BETWEEN_EXCLUSIVE, BETWEEN, NOT_BETWEEN_EXCLUSIVE, NOT_BETWEEN);
	private static final List<Operator> UPPER_BOUND_OPERATORS = asList(
					LESS_THAN, LESS_THAN_OR_EQUAL, BETWEEN_EXCLUSIVE, BETWEEN, NOT_BETWEEN_EXCLUSIVE, NOT_BETWEEN);

	private final FieldFactory<C> fieldFactory;
	private final Event<C> focusGainedEvent = Event.event();

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

	private FilterColumnConditionPanel(ColumnConditionModel<C, T> conditionModel,
																		 String caption, FieldFactory<C> fieldFactory) {
		super(conditionModel, caption);
		this.fieldFactory = requireNonNull(fieldFactory, "fieldFactory");
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
		switch (conditionModel().operator().get()) {
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
				throw new IllegalArgumentException(UNKNOWN_OPERATOR + conditionModel().operator().get());
		}
	}

	@Override
	public Optional<EventObserver<C>> focusGainedEvent() {
		return Optional.of(focusGainedEvent);
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
	 * Instantiates a new {@link FilterColumnConditionPanel}, with a default bound field factory.
	 * @param conditionModel the condition model to base this panel on
	 * @param caption the caption to use when presenting this condition panel
	 * @param <C> the type of objects used to identify columns
	 * @param <T> the column value type
	 * @return a new {@link FilterColumnConditionPanel} instance or an empty Optional in case the column type is not supported
	 */
	public static <C, T> FilterColumnConditionPanel<C, T> filterColumnConditionPanel(ColumnConditionModel<C, T> conditionModel, String caption) {
		return filterColumnConditionPanel(conditionModel, caption, new DefaultFieldFactory<>());
	}

	/**
	 * Instantiates a new {@link FilterColumnConditionPanel}.
	 * @param <C> the type of objects used to identify columns
	 * @param <T> the column value type
	 * @param conditionModel the condition model to base this panel on
	 * @param caption the caption to use when presenting this condition panel
	 * @param fieldFactory the input field factory
	 * @return a new {@link FilterColumnConditionPanel} instance or an empty Optional in case the column type is not supported by the given bound field factory
	 * @throws IllegalArgumentException in case the given field factory does not support the column value type
	 */
	public static <C, T> FilterColumnConditionPanel<C, T> filterColumnConditionPanel(ColumnConditionModel<C, T> conditionModel,
																																									 String caption, FieldFactory<C> fieldFactory) {
		if (requireNonNull(fieldFactory).supportsType(requireNonNull(conditionModel).columnClass())) {
			return new FilterColumnConditionPanel<>(conditionModel, caption, fieldFactory);
		}

		throw new IllegalArgumentException("Field factory does not support the column value type");
	}

	/**
	 * Provides equal, upper and lower bound input fields for a ColumnConditionPanel
	 */
	public interface FieldFactory<C> {

		/**
		 * @param columnClass the column class
		 * @return true if the type is supported
		 */
		boolean supportsType(Class<?> columnClass);

		/**
		 * Creates the field representing the equal value, linked to {@link ColumnConditionModel#equalValue()}
		 * @return the equal value field
		 * @throws IllegalArgumentException in case the bound type is not supported
		 */
		JComponent createEqualField(ColumnConditionModel<C, ?> conditionModel);

		/**
		 * Creates the field representing the upper bound value, linked to {@link ColumnConditionModel#upperBoundValue()}
		 * @return an upper bound input field, or an empty Optional if it does not apply to the bound type
		 * @throws IllegalArgumentException in case the bound type is not supported
		 */
		Optional<JComponent> createUpperBoundField(ColumnConditionModel<C, ?> conditionModel);

		/**
		 * Creates the field representing the lower bound value, linked to {@link ColumnConditionModel#lowerBoundValue()}
		 * @return a lower bound input field, or an empty Optional if it does not apply to the bound type
		 * @throws IllegalArgumentException in case the bound type is not supported
		 */
		Optional<JComponent> createLowerBoundField(ColumnConditionModel<C, ?> conditionModel);

		/**
		 * Creates the field representing the in values, linked to {@link ColumnConditionModel#inValues()}
		 * @return the in value field
		 * @throws IllegalArgumentException in case the bound type is not supported
		 */
		JComponent createInField(ColumnConditionModel<C, ?> conditionModel);
	}

	private JComponent createEqualField(FieldFactory<C> fieldFactory) {
		return equalFieldRequired() ? fieldFactory.createEqualField(conditionModel()) : null;
	}

	private JComponent createUpperBoundField(FieldFactory<C> fieldFactory) {
		return upperBoundFieldRequired() ? fieldFactory.createUpperBoundField(conditionModel()).orElse(null) : null;
	}

	private JComponent createLowerBoundField(FieldFactory<C> fieldFactory) {
		return lowerBoundFieldRequired() ? fieldFactory.createLowerBoundField(conditionModel()).orElse(null) : null;
	}

	private JComponent createInField(FieldFactory<C> fieldFactory) {
		return inFieldRequired() ? fieldFactory.createInField(conditionModel()) : null;
	}

	private boolean equalFieldRequired() {
		return conditionModel().operators().contains(EQUAL) ||
						conditionModel().operators().contains(NOT_EQUAL);
	}

	private boolean upperBoundFieldRequired() {
		return conditionModel().operators().stream()
						.anyMatch(UPPER_BOUND_OPERATORS::contains);
	}

	private boolean lowerBoundFieldRequired() {
		return conditionModel().operators().stream()
						.anyMatch(LOWER_BOUND_OPERATORS::contains);
	}

	private boolean inFieldRequired() {
		return conditionModel().operators().contains(IN) ||
						conditionModel().operators().contains(NOT_IN);
	}

	private void initialize() {
		if (!initialized) {
			setLayout(new BorderLayout());
			createComponents();
			bindEvents();
			controlPanel.add(operatorCombo, BorderLayout.CENTER);
			addStringConfigurationPopupMenu();
			onOperatorChanged(conditionModel().operator().get());
			initialized = true;
		}
	}

	private void createComponents() {
		controlPanel = new JPanel(new BorderLayout());
		inputPanel = new JPanel(new BorderLayout());
		rangePanel = new JPanel(new GridLayout(1, 2));
		toggleEnabledButton = radioButton(conditionModel().enabled())
						.horizontalAlignment(CENTER)
						.popupMenu(radioButton -> menu(Controls.builder()
										.control(Control.builder()
														.toggle(conditionModel().autoEnable())
														.name(MESSAGES.getString("auto_enable"))).build())
										.createPopupMenu())
						.build();
		boolean modelLocked = conditionModel().locked().get();
		conditionModel().locked().set(false);//otherwise, the validator checking the locked state kicks in during value linking
		equalField = createEqualField(fieldFactory);
		upperBoundField = createUpperBoundField(fieldFactory);
		lowerBoundField = createLowerBoundField(fieldFactory);
		inField = createInField(fieldFactory);
		operatorCombo = createOperatorComboBox(conditionModel().operators());
		conditionModel().locked().set(modelLocked);
	}

	private void bindEvents() {
		conditionModel().operator().addConsumer(this::onOperatorChanged);
		linkToEnabledState(conditionModel().locked().not(),
						operatorCombo, equalField, upperBoundField, lowerBoundField, toggleEnabledButton);
		components().forEach(component ->
						component.addFocusListener(new FocusGained(conditionModel().columnIdentifier())));
		KeyEvents.Builder enableOnEnterKeyEvent = KeyEvents.builder(CONTROL_KEY_STROKES.keyStroke(TOGGLE_ENABLED).get())
						.action(commandControl(this::toggleEnabled));
		KeyEvents.Builder previousOperatorKeyEvent = KeyEvents.builder(CONTROL_KEY_STROKES.keyStroke(PREVIOUS_OPERATOR).get())
						.action(commandControl(this::selectPreviousOperator));
		KeyEvents.Builder nextOperatorKeyEvent = KeyEvents.builder(CONTROL_KEY_STROKES.keyStroke(NEXT_OPERATOR).get())
						.action(commandControl(this::selectNextOperator));
		enableOnEnterKeyEvent.enable(operatorCombo);
		enableOnEnterKeyEvent.enable(toggleEnabledButton);
		Stream.of(equalField, upperBoundField, lowerBoundField, inField)
						.filter(Objects::nonNull)
						.forEach(field -> {
							enableOnEnterKeyEvent.enable(field);
							previousOperatorKeyEvent.enable(field);
							nextOperatorKeyEvent.enable(field);
						});
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
				throw new IllegalArgumentException(UNKNOWN_OPERATOR + conditionModel().operator().get());
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
		Component focusOwner = getCurrentKeyboardFocusManager().getFocusOwner();
		boolean parentOfFocusOwner = parentOfType(FilterColumnConditionPanel.class, focusOwner) == this;
		if (parentOfFocusOwner) {
			requestFocusInWindow(true);
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
		Component focusOwner = getCurrentKeyboardFocusManager().getFocusOwner();
		boolean parentOfFocusOwner = parentOfType(FilterColumnConditionPanel.class, focusOwner) == this;
		if (parentOfFocusOwner) {
			requestFocusInWindow(true);
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
		operatorComboBoxModel.setSelectedItem(operators.get(0));
		return itemComboBox(operatorComboBoxModel, conditionModel().operator())
						.completionMode(Completion.Mode.NONE)
						.renderer(new OperatorComboBoxRenderer())
						.maximumRowCount(operators.size())
						.mouseWheelScrollingWithWrapAround(true)
						.toolTipText(operatorComboBoxModel.selectedValue().get().description())
						.onBuild(comboBox -> operatorComboBoxModel.selectionEvent().addConsumer(selectedOperator ->
										comboBox.setToolTipText(selectedOperator.get().description())))
						.build();
	}

	private void selectNextOperator() {
		ItemComboBoxModel<Operator> itemComboBoxModel = (ItemComboBoxModel<Operator>) operatorCombo.getModel();
		List<Item<Operator>> visibleItems = itemComboBoxModel.visibleItems();
		int index = visibleItems.indexOf(itemComboBoxModel.getSelectedItem());
		if (index < itemComboBoxModel.visibleCount() - 1) {
			itemComboBoxModel.setSelectedItem(visibleItems.get(index + 1));
		}
		else {
			itemComboBoxModel.setSelectedItem(visibleItems.get(0));
		}
	}

	private void toggleEnabled() {
		conditionModel().enabled().set(!conditionModel().enabled().get());
	}

	private void selectPreviousOperator() {
		ItemComboBoxModel<Operator> itemComboBoxModel = (ItemComboBoxModel<Operator>) operatorCombo.getModel();
		List<Item<Operator>> visibleItems = itemComboBoxModel.visibleItems();
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
			boolean requestFocus = boundFieldHasFocus();
			clearInputPanel(requestFocus);
			inputPanel.add(boundField, BorderLayout.CENTER);
			if (state().isEqualTo(ConditionState.SIMPLE)) {
				inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
			}
			if (requestFocus) {
				boundField.requestFocusInWindow();
			}
		}
	}

	private void rangePanel() {
		if (!asList(inputPanel.getComponents()).contains(rangePanel)) {
			boolean requestFocus = boundFieldHasFocus();
			clearInputPanel(requestFocus);
			rangePanel.add(lowerBoundField);
			rangePanel.add(upperBoundField);
			inputPanel.add(rangePanel, BorderLayout.CENTER);
			if (state().isEqualTo(ConditionState.SIMPLE)) {
				inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
			}
			if (requestFocus) {
				lowerBoundField.requestFocusInWindow();
			}
		}
	}

	private void clearInputPanel(boolean requestFocus) {
		if (requestFocus) {
			//keep the focus here temporarily while we remove all
			//otherwise it jumps to the first focusable component
			inputPanel.requestFocusInWindow();
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
							.toggle(conditionModel().caseSensitive())
							.name(MESSAGES.getString("case_sensitive")));
			if (conditionModel().columnClass().equals(String.class)) {
				controlsBuilder.controls(createAutomaticWildcardControls());
			}
			JPopupMenu popupMenu = menu(controlsBuilder).createPopupMenu();
			Stream.of(equalField, lowerBoundField, upperBoundField, inField)
							.filter(Objects::nonNull)
							.forEach(field -> field.setComponentPopupMenu(popupMenu));
		}
	}

	private boolean isStringOrCharacter() {
		return conditionModel().columnClass().equals(String.class) || conditionModel().columnClass().equals(Character.class);
	}

	private Controls createAutomaticWildcardControls() {
		Value<AutomaticWildcard> automaticWildcardValue = conditionModel().automaticWildcard();
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

		private final C columnIdentifier;

		private FocusGained(C columnIdentifier) {
			this.columnIdentifier = columnIdentifier;
		}

		@Override
		public void focusGained(FocusEvent e) {
			focusGainedEvent.accept(columnIdentifier);
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
