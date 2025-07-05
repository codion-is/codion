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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.item.Item;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Wildcard;
import is.codion.common.observable.Observer;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
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
import java.util.function.Function;
import java.util.stream.Stream;

import static is.codion.common.Operator.*;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.enabled;
import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.ControlKeys.*;
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
 * For instances use {@link #builder(ConditionModel)}.
 * @param <T> the column value type
 * @see #builder(ConditionModel)
 */
public final class ColumnConditionPanel<T> extends ConditionPanel<T> {

	private static final MessageBundle MESSAGES =
					messageBundle(ColumnConditionPanel.class, getBundle(ColumnConditionPanel.class.getName()));

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
		 * Clears the model.<br>
		 * Default key stroke: CTRL-SHIFT-ENTER
		 */
		public static final ControlKey<ToggleControl> CLEAR = ToggleControl.key("clear", keyStroke(VK_ENTER, CTRL_DOWN_MASK | SHIFT_DOWN_MASK));
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
	private static final DefaultOperatorCaptions DEFAULT_OPERATOR_CAPTIONS = new DefaultOperatorCaptions();

	private final ComponentFactory componentFactory;
	private final Event<?> focusGainedEvent = Event.event();
	private final TableColumn tableColumn;
	private final Function<Operator, String> operatorCaptions;
	private final OperandComponents operandComponents = new OperandComponents();

	private JToggleButton toggleEnabledButton;
	private JComboBox<Item<Operator>> operatorCombo;
	private JComponent equalComponent;
	private JComponent upperComponent;
	private JComponent lowerComponent;
	private JComponent inComponent;
	private JPanel controlPanel;
	private JPanel inputPanel;
	private JPanel rangePanel;

	private boolean initialized = false;

	private ColumnConditionPanel(DefaultBuilder<T> builder) {
		super(builder.conditionModel);
		this.componentFactory = builder.componentFactory;
		this.operatorCaptions = builder.operatorCaptions;
		this.tableColumn = builder.tableColumn;
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(toggleEnabledButton, operatorCombo, equalComponent,
						lowerComponent, upperComponent, inComponent, controlPanel, inputPanel, rangePanel);
	}

	@Override
	public Collection<JComponent> components() {
		return Stream.of(toggleEnabledButton, operatorCombo, equalComponent, lowerComponent, upperComponent, inComponent)
						.filter(Objects::nonNull)
						.collect(toList());
	}

	@Override
	public void requestInputFocus() {
		switch (model().operator().getOrThrow()) {
			case EQUAL:
			case NOT_EQUAL:
				operandComponents.equal().ifPresent(JComponent::requestFocusInWindow);
				break;
			case GREATER_THAN:
			case GREATER_THAN_OR_EQUAL:
			case BETWEEN_EXCLUSIVE:
			case BETWEEN:
			case NOT_BETWEEN_EXCLUSIVE:
			case NOT_BETWEEN:
				operandComponents.lower().ifPresent(JComponent::requestFocusInWindow);
				break;
			case LESS_THAN:
			case LESS_THAN_OR_EQUAL:
				operandComponents.upper().ifPresent(JComponent::requestFocusInWindow);
				break;
			case IN:
			case NOT_IN:
				operandComponents.in().ifPresent(JComponent::requestFocusInWindow);
				break;
			default:
				throw new IllegalArgumentException(UNKNOWN_OPERATOR + model().operator().get());
		}
	}

	@Override
	public Optional<Observer<?>> focusGainedObserver() {
		return Optional.of(focusGainedEvent.observer());
	}

	/**
	 * @return the operand components used by this condition panel
	 */
	public OperandComponents operands() {
		return operandComponents;
	}

	/**
	 * Provides the operand components.
	 */
	public final class OperandComponents {

		private OperandComponents() {}

		/**
		 * @return the JComponent used to specify the equal value
		 */
		public Optional<JComponent> equal() {
			initialize();

			return Optional.ofNullable(equalComponent);
		}

		/**
		 * @return the JComponent used to specify the upper bound
		 */
		public Optional<JComponent> upper() {
			initialize();

			return Optional.ofNullable(upperComponent);
		}

		/**
		 * @return the JComponent used to specify the lower bound
		 */
		public Optional<JComponent> lower() {
			initialize();

			return Optional.ofNullable(lowerComponent);
		}

		/**
		 * @return the JComponent used to specify the in values
		 */
		public Optional<JComponent> in() {
			initialize();

			return Optional.ofNullable(inComponent);
		}
	}

	/**
	 * @param conditionModel the condition model
	 * @param <T> the condition value type
	 * @return a new {@link Builder}
	 */
	public static <T> Builder<T> builder(ConditionModel<T> conditionModel) {
		return new DefaultBuilder<>(conditionModel);
	}

	/**
	 * Builds a {@link ColumnConditionPanel} instance
	 * @param <T> the column value type
	 */
	public interface Builder<T> {

		/**
		 * @param componentFactory the input component factory
		 * @return this builder
		 * @throws IllegalArgumentException in case the given component factory does not support the column value type
		 */
		Builder<T> componentFactory(ComponentFactory componentFactory);

		/**
		 * Provides captions for operators, displayed in the operator combo box
		 * @param operatorCaptions the operator caption function
		 * @return this builder
		 */
		Builder<T> operatorCaptions(Function<Operator, String> operatorCaptions);

		/**
		 * @param tableColumn the table column this condition panel represents
		 * @return this builder
		 */
		Builder<T> tableColumn(TableColumn tableColumn);

		/**
		 * @return a new {@link ColumnConditionPanel} based on this builder
		 */
		ColumnConditionPanel<T> build();
	}

	private static final class DefaultBuilder<T> implements Builder<T> {

		private final ConditionModel<T> conditionModel;

		private ComponentFactory componentFactory = new FilterComponentFactory();
		private Function<Operator, String> operatorCaptions = DEFAULT_OPERATOR_CAPTIONS;
		private TableColumn tableColumn;

		private DefaultBuilder(ConditionModel<T> conditionModel) {
			this.conditionModel = requireNonNull(conditionModel);
		}

		@Override
		public Builder<T> componentFactory(ComponentFactory componentFactory) {
			if (!requireNonNull(componentFactory).supportsType(conditionModel.valueClass())) {
				throw new IllegalArgumentException("ComponentFactory does not support the value type: " + conditionModel.valueClass());
			}

			this.componentFactory = requireNonNull(componentFactory);
			return this;
		}

		@Override
		public Builder<T> operatorCaptions(Function<Operator, String> operatorCaptions) {
			this.operatorCaptions = requireNonNull(operatorCaptions);
			return this;
		}

		@Override
		public Builder<T> tableColumn(TableColumn tableColumn) {
			this.tableColumn = requireNonNull(tableColumn);
			return this;
		}

		@Override
		public ColumnConditionPanel<T> build() {
			return new ColumnConditionPanel<>(this);
		}
	}

	/**
	 * Provides equal, in, upper and lower bound input components for a {@link ColumnConditionPanel}
	 */
	public interface ComponentFactory {

		/**
		 * @param valueClass the value class
		 * @return true if the type is supported
		 */
		default boolean supportsType(Class<?> valueClass) {
			return true;
		}

		/**
		 * @param conditionModel the condition model
		 * @param <T> the operand type
		 * @return a component linked to the equal operand
		 */
		<T> JComponent equal(ConditionModel<T> conditionModel);

		/**
		 * @param conditionModel the condition model
		 * @param <T> the operand type
		 * @return a component linked to the lower bound operand
		 */
		<T> JComponent lower(ConditionModel<T> conditionModel);

		/**
		 * @param conditionModel the condition model
		 * @param <T> the operand type
		 * @return a component linked to the upper bound operand
		 */
		<T> JComponent upper(ConditionModel<T> conditionModel);

		/**
		 * @param conditionModel the condition model
		 * @param <T> the operand type
		 * @return a component linked to the in operands
		 */
		<T> JComponent in(ConditionModel<T> conditionModel);
	}

	private boolean equalIncluded() {
		return model().operators().contains(EQUAL) ||
						model().operators().contains(NOT_EQUAL);
	}

	private boolean upperIncluded() {
		return model().operators().stream()
						.anyMatch(UPPER_BOUND_OPERATORS::contains);
	}

	private boolean lowerIncluded() {
		return model().operators().stream()
						.anyMatch(LOWER_BOUND_OPERATORS::contains);
	}

	private boolean inIncluded() {
		return model().operators().contains(IN) ||
						model().operators().contains(NOT_IN);
	}

	private void initialize() {
		if (!initialized) {
			setLayout(new BorderLayout());
			createComponents();
			bindEvents();
			controlPanel.add(operatorCombo, BorderLayout.CENTER);
			addStringConfigurationPopupMenu();
			onOperatorChanged(model().operator().getOrThrow());
			initialized = true;
		}
	}

	private void createComponents() {
		controlPanel = new JPanel(new BorderLayout());
		inputPanel = new JPanel(new BorderLayout());
		rangePanel = new JPanel(new GridLayout(1, 2));
		toggleEnabledButton = radioButton()
						.link(model().enabled())
						.horizontalAlignment(CENTER)
						.popupMenu(radioButton -> menu(Controls.builder()
										.control(Control.builder()
														.toggle(model().autoEnable())
														.caption(MESSAGES.getString("auto_enable"))))
										.buildPopupMenu())
						.build();
		boolean modelLocked = model().locked().get();
		model().locked().set(false);//otherwise, the validator checking the locked state kicks in during value linking
		if (equalIncluded()) {
			equalComponent = componentFactory.equal(model());
		}
		if (upperIncluded()) {
			upperComponent = componentFactory.upper(model());
		}
		if (lowerIncluded()) {
			lowerComponent = componentFactory.lower(model());
		}
		if (inIncluded()) {
			inComponent = componentFactory.in(model());
		}
		operatorCombo = createOperatorComboBox(model().operators());
		model().locked().set(modelLocked);
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
		model().operator().addConsumer(this::onOperatorChanged);
		Collection<JComponent> components = components();
		enabled(model().locked().not(), components.toArray(new JComponent[0]));
		FocusGained focusGained = new FocusGained();
		components.forEach(component -> component.addFocusListener(focusGained));
		TOGGLE_ENABLED.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(command(this::toggleEnabled))
										.enable(components));
		CLEAR.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(command(model()::clear))
										.enable(components));
		PREVIOUS_OPERATOR.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(command(this::selectPreviousOperator))
										.enable(components));
		NEXT_OPERATOR.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(command(this::selectNextOperator))
										.enable(components));
	}

	private void onOperatorChanged(Operator operator) {
		switch (operator) {
			case EQUAL:
			case NOT_EQUAL:
				singleValuePanel(equalComponent);
				break;
			case GREATER_THAN:
			case GREATER_THAN_OR_EQUAL:
				singleValuePanel(lowerComponent);
				break;
			case LESS_THAN:
			case LESS_THAN_OR_EQUAL:
				singleValuePanel(upperComponent);
				break;
			case BETWEEN_EXCLUSIVE:
			case BETWEEN:
			case NOT_BETWEEN_EXCLUSIVE:
			case NOT_BETWEEN:
				rangePanel();
				break;
			case IN:
			case NOT_IN:
				singleValuePanel(inComponent);
				break;
			default:
				throw new IllegalArgumentException(UNKNOWN_OPERATOR + model().operator().get());
		}
		revalidate();
		repaint();
	}

	protected void onViewChanged(ConditionView conditionView) {
		switch (conditionView) {
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
				throw new IllegalArgumentException("Unknown condition view: " + conditionView);
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
		boolean parentOfFocusOwner = parentOfType(ColumnConditionPanel.class, focusOwner) == this;
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
		boolean parentOfFocusOwner = parentOfType(ColumnConditionPanel.class, focusOwner) == this;
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
		FilterComboBoxModel<Item<Operator>> operatorComboBoxModel = FilterComboBoxModel.builder(operators.stream()
										.map(operator -> Item.item(operator, operatorCaptions.apply(operator)))
										.collect(toList()))
						.build();

		return itemComboBox(operatorComboBoxModel)
						.link(model().operator())
						.completionMode(Completion.Mode.NONE)
						.renderer(new OperatorComboBoxRenderer())
						.maximumRowCount(operators.size())
						.mouseWheelScrollingWithWrapAround(true)
						.toolTipText(model().operator().getOrThrow().description())
						.onBuild(comboBox -> operatorComboBoxModel.selection().item().addConsumer(selectedOperator ->
										comboBox.setToolTipText(selectedOperator.value().description())))
						.build();
	}

	private void selectNextOperator() {
		FilterComboBoxModel<Item<Operator>> itemComboBoxModel = (FilterComboBoxModel<Item<Operator>>) operatorCombo.getModel();
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
		model().enabled().set(!model().enabled().get());
	}

	private void selectPreviousOperator() {
		FilterComboBoxModel<Item<Operator>> itemComboBoxModel = (FilterComboBoxModel<Item<Operator>>) operatorCombo.getModel();
		List<Item<Operator>> visibleItems = itemComboBoxModel.items().visible().get();
		int index = visibleItems.indexOf(itemComboBoxModel.getSelectedItem());
		if (index > 0) {
			itemComboBoxModel.setSelectedItem(visibleItems.get(index - 1));
		}
		else {
			itemComboBoxModel.setSelectedItem(visibleItems.get(visibleItems.size() - 1));
		}
	}

	private void singleValuePanel(JComponent component) {
		if (!asList(inputPanel.getComponents()).contains(component)) {
			boolean operandHasFocus = operandHasFocus();
			clearInputPanel(operandHasFocus);
			inputPanel.add(component, BorderLayout.CENTER);
			if (view().isEqualTo(ConditionView.SIMPLE)) {
				inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
			}
			if (operandHasFocus) {
				component.requestFocusInWindow();
			}
		}
	}

	private void rangePanel() {
		if (!asList(inputPanel.getComponents()).contains(rangePanel)) {
			boolean operandHasFocus = operandHasFocus();
			clearInputPanel(operandHasFocus);
			rangePanel.add(lowerComponent);
			rangePanel.add(upperComponent);
			inputPanel.add(rangePanel, BorderLayout.CENTER);
			if (view().isEqualTo(ConditionView.SIMPLE)) {
				inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
			}
			if (operandHasFocus) {
				lowerComponent.requestFocusInWindow();
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

	private boolean operandHasFocus() {
		return operandHasFocus(equalComponent) ||
						operandHasFocus(lowerComponent) ||
						operandHasFocus(upperComponent) ||
						operandHasFocus(inComponent);
	}

	private void addStringConfigurationPopupMenu() {
		if (isStringOrCharacter()) {
			ControlsBuilder controlsBuilder = Controls.builder();
			controlsBuilder.control(Control.builder()
							.toggle(model().caseSensitive())
							.caption(MESSAGES.getString("case_sensitive")));
			if (model().valueClass().equals(String.class)) {
				controlsBuilder.control(createWildcardControls());
			}
			JPopupMenu popupMenu = menu(controlsBuilder).buildPopupMenu();
			Stream.of(equalComponent, lowerComponent, upperComponent, inComponent)
							.filter(Objects::nonNull)
							.forEach(component -> component.setComponentPopupMenu(popupMenu));
		}
	}

	private boolean isStringOrCharacter() {
		return model().valueClass().equals(String.class) || model().valueClass().equals(Character.class);
	}

	private Controls createWildcardControls() {
		Wildcard wildcard = model().operands().wildcard().getOrThrow();

		State wildcardNoneState = State.state(wildcard.equals(Wildcard.NONE));
		State wildcardPostfixState = State.state(wildcard.equals(Wildcard.POSTFIX));
		State wildcardPrefixState = State.state(wildcard.equals(Wildcard.PREFIX));
		State wildcardPrefixAndPostfixState = State.state(wildcard.equals(Wildcard.PREFIX_AND_POSTFIX));

		State.group(wildcardNoneState, wildcardPostfixState, wildcardPrefixState, wildcardPrefixAndPostfixState);

		wildcardNoneState.addConsumer(enabled -> {
			if (enabled) {
				model().operands().wildcard().set(Wildcard.NONE);
			}
		});
		wildcardPostfixState.addConsumer(enabled -> {
			if (enabled) {
				model().operands().wildcard().set(Wildcard.POSTFIX);
			}
		});
		wildcardPrefixState.addConsumer(enabled -> {
			if (enabled) {
				model().operands().wildcard().set(Wildcard.PREFIX);
			}
		});
		wildcardPrefixAndPostfixState.addConsumer(enabled -> {
			if (enabled) {
				model().operands().wildcard().set(Wildcard.PREFIX_AND_POSTFIX);
			}
		});

		return Controls.builder()
						.caption(MESSAGES.getString("wildcard"))
						.control(Control.builder()
										.toggle(wildcardNoneState)
										.caption(Wildcard.NONE.description()))
						.control(Control.builder()
										.toggle(wildcardPostfixState)
										.caption(Wildcard.POSTFIX.description()))
						.control(Control.builder()
										.toggle(wildcardPrefixState)
										.caption(Wildcard.PREFIX.description()))
						.control(Control.builder()
										.toggle(wildcardPrefixAndPostfixState)
										.caption(Wildcard.PREFIX_AND_POSTFIX.description()))
						.build();
	}

	private static boolean operandHasFocus(JComponent component) {
		if (component == null) {
			return false;
		}
		if (component.hasFocus()) {
			return true;
		}
		if (component instanceof JComboBox) {
			return ((JComboBox<?>) component).getEditor().getEditorComponent().hasFocus();
		}

		return false;
	}

	private final class FocusGained extends FocusAdapter {

		@Override
		public void focusGained(FocusEvent e) {
			focusGainedEvent.run();
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

	private static final class DefaultOperatorCaptions implements Function<Operator, String> {

		@Override
		public String apply(Operator operator) {
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
					return "> α >";
				case NOT_BETWEEN:
					return "≥ α ≥";
				case IN:
					return "α ∈";
				case NOT_IN:
					return "α ∉";
				default:
					throw new IllegalArgumentException(UNKNOWN_OPERATOR + operator);
			}
		}
	}
}
