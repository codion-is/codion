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
import is.codion.common.observer.Observer;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ControlsBuilder;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.ListCellRenderer;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static is.codion.common.Operator.*;
import static is.codion.common.model.condition.ConditionModel.Wildcard.*;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.enabled;
import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.ControlKeys.*;
import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.SIMPLE;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static javax.swing.FocusManager.getCurrentManager;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for {@link ConditionModel}.
 * For instances use {@link #builder()}.
 * @param <T> the column value type
 * @see #builder()
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
	private final Event<?> focusGained = Event.event();
	private final @Nullable TableColumn tableColumn;
	private final Function<Operator, String> operatorCaptions;
	private final OperandComponents operandComponents = new OperandComponents();

	private @Nullable JToggleButton toggleEnabledButton;
	private @Nullable JComboBox<Item<Operator>> operatorCombo;
	private @Nullable JComponent equalComponent;
	private @Nullable JComponent upperComponent;
	private @Nullable JComponent lowerComponent;
	private @Nullable JComponent inComponent;
	private @Nullable JPanel controlPanel;
	private @Nullable JPanel inputPanel;
	private @Nullable JPanel rangePanel;

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
	public Optional<Observer<?>> focusGained() {
		return Optional.of(focusGained.observer());
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
	 * @return a new {@link Builder}
	 */
	public static Builder.ModelStep builder() {
		return DefaultBuilder.MODEL;
	}

	/**
	 * Builds a {@link ColumnConditionPanel} instance
	 * @param <T> the column value type
	 */
	public interface Builder<T> {

		/**
		 * Provides a {@link Builder}
		 */
		interface ModelStep {

			/**
			 * @param model the condition model
			 * @param <T> the condition value type
			 * @return a new {@link Builder}
			 */
			<T> Builder<T> model(ConditionModel<T> model);
		}

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

	private static final class DefaultModelStep implements Builder.ModelStep {

		@Override
		public <T> Builder<T> model(ConditionModel<T> model) {
			return new DefaultBuilder<>(requireNonNull(model));
		}
	}

	private static final class DefaultBuilder<T> implements Builder<T> {

		private static final ModelStep MODEL = new DefaultModelStep();

		private final ConditionModel<T> conditionModel;

		private ComponentFactory componentFactory = new FilterComponentFactory();
		private Function<Operator, String> operatorCaptions = DEFAULT_OPERATOR_CAPTIONS;
		private @Nullable TableColumn tableColumn;

		private DefaultBuilder(ConditionModel<T> conditionModel) {
			this.conditionModel = conditionModel;
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
						.popupMenu(radioButton -> menu()
										.controls(Controls.builder()
														.control(Control.builder()
																		.toggle(model().autoEnable())
																		.caption(MESSAGES.getString("auto_enable"))))
										.buildPopupMenu())
						.build();
		boolean modelLocked = model().locked().is();
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
						KeyEvents.builder()
										.keyStroke(keyStroke)
										.action(command(this::toggleEnabled))
										.enable(components));
		CLEAR.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder()
										.keyStroke(keyStroke)
										.action(command(model()::clear))
										.enable(components));
		PREVIOUS_OPERATOR.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder()
										.keyStroke(keyStroke)
										.action(command(this::selectPreviousOperator))
										.enable(components));
		NEXT_OPERATOR.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder()
										.keyStroke(keyStroke)
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
		FilterComboBoxModel<Item<Operator>> operatorComboBoxModel = FilterComboBoxModel.builder()
						.items(operators.stream()
										.map(operator -> Item.item(operator, operatorCaptions.apply(operator)))
										.collect(toList()))
						.build();

		return itemComboBox()
						.model(operatorComboBoxModel)
						.link(model().operator())
						.completionMode(Completion.Mode.NONE)
						.renderer(new OperatorComboBoxRenderer())
						.maximumRowCount(operators.size())
						.mouseWheelScrollingWithWrapAround(true)
						.toolTipText(model().operator().getOrThrow().description())
						.onBuild(comboBox -> operatorComboBoxModel.selection().item().addConsumer(selectedOperator ->
										comboBox.setToolTipText(selectedOperator.getOrThrow().description())))
						.build();
	}

	private void selectNextOperator() {
		FilterComboBoxModel<Item<Operator>> itemComboBoxModel = (FilterComboBoxModel<Item<Operator>>) operatorCombo.getModel();
		List<Item<Operator>> includedItems = itemComboBoxModel.items().included().get();
		int index = includedItems.indexOf(itemComboBoxModel.getSelectedItem());
		if (index < itemComboBoxModel.items().included().count() - 1) {
			itemComboBoxModel.setSelectedItem(includedItems.get(index + 1));
		}
		else {
			itemComboBoxModel.setSelectedItem(includedItems.get(0));
		}
		displayOperator();
	}

	private void toggleEnabled() {
		model().enabled().set(!model().enabled().is());
	}

	private void selectPreviousOperator() {
		FilterComboBoxModel<Item<Operator>> itemComboBoxModel = (FilterComboBoxModel<Item<Operator>>) operatorCombo.getModel();
		List<Item<Operator>> includedItems = itemComboBoxModel.items().included().get();
		int index = includedItems.indexOf(itemComboBoxModel.getSelectedItem());
		if (index > 0) {
			itemComboBoxModel.setSelectedItem(includedItems.get(index - 1));
		}
		else {
			itemComboBoxModel.setSelectedItem(includedItems.get(includedItems.size() - 1));
		}
		displayOperator();
	}

	private void displayOperator() {
		if (view().is(SIMPLE)) {
			JToolTip operatorTip = new JToolTip();
			operatorTip.setTipText(operatorCaptions.apply(model().operator().getOrThrow()));
			Popup popup = PopupFactory.getSharedInstance().getPopup(this, operatorTip,
							getLocationOnScreen().x, getLocationOnScreen().y - getHeight() - Layouts.GAP.getOrThrow());
			popup.show();
			Timer timer = new Timer((int) SECONDS.toMillis(1), e -> popup.hide());
			timer.setRepeats(false);
			timer.start();
		}
	}

	private void singleValuePanel(JComponent component) {
		if (!asList(inputPanel.getComponents()).contains(component)) {
			boolean operandHasFocus = operandHasFocus();
			clearInputPanel(operandHasFocus);
			inputPanel.add(component, BorderLayout.CENTER);
			if (view().is(SIMPLE)) {
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
			if (view().is(SIMPLE)) {
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
			JPopupMenu popupMenu = menu()
							.controls(controlsBuilder)
							.buildPopupMenu();
			Stream.of(equalComponent, lowerComponent, upperComponent, inComponent)
							.filter(Objects::nonNull)
							.forEach(component -> component.setComponentPopupMenu(popupMenu));
		}
	}

	private boolean isStringOrCharacter() {
		return model().valueClass().equals(String.class) || model().valueClass().equals(Character.class);
	}

	private Controls createWildcardControls() {
		Value<Wildcard> wildcard = model().operands().wildcard();

		State wildcardNone = State.builder()
						.value(wildcard.is(NONE))
						.consumer(new EnableWildcard(NONE))
						.build();
		State wildcardPostfix = State.builder()
						.value(wildcard.is(POSTFIX))
						.consumer(new EnableWildcard(POSTFIX))
						.build();
		State wildcardPrefix = State.builder()
						.value(wildcard.is(PREFIX))
						.consumer(new EnableWildcard(PREFIX))
						.build();
		State wildcardPrefixAndPostfix = State.builder()
						.value(wildcard.is(PREFIX_AND_POSTFIX))
						.consumer(new EnableWildcard(PREFIX_AND_POSTFIX))
						.build();

		State.group(wildcardNone, wildcardPostfix, wildcardPrefix, wildcardPrefixAndPostfix);

		wildcard.addConsumer(newWildcard -> {
			switch (newWildcard) {
				case NONE:
					wildcardNone.set(true);
					break;
				case POSTFIX:
					wildcardPostfix.set(true);
					break;
				case PREFIX:
					wildcardPrefix.set(true);
					break;
				case PREFIX_AND_POSTFIX:
					wildcardPrefixAndPostfix.set(true);
					break;
				default:
					throw new IllegalStateException("Unknown wildcard: " + newWildcard);
			}
		});

		return Controls.builder()
						.caption(MESSAGES.getString("wildcard"))
						.control(Control.builder()
										.toggle(wildcardNone)
										.caption(NONE.description()))
						.control(Control.builder()
										.toggle(wildcardPostfix)
										.caption(POSTFIX.description()))
						.control(Control.builder()
										.toggle(wildcardPrefix)
										.caption(PREFIX.description()))
						.control(Control.builder()
										.toggle(wildcardPrefixAndPostfix)
										.caption(PREFIX_AND_POSTFIX.description()))
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

	private final class EnableWildcard implements Consumer<Boolean> {

		private final Wildcard wildcard;

		private EnableWildcard(Wildcard wildcard) {
			this.wildcard = wildcard;
		}

		@Override
		public void accept(Boolean enabled) {
			if (enabled) {
				model().operands().wildcard().set(wildcard);
			}
		}
	}

	private final class FocusGained extends FocusAdapter {

		@Override
		public void focusGained(FocusEvent e) {
			focusGained.run();
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
