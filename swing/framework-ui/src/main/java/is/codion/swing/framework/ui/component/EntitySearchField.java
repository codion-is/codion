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
package is.codion.swing.framework.ui.component;

import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.property.PropertyValue;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.SwingMessages;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.list.ListBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.text.HintTextField;
import is.codion.swing.common.ui.component.text.TextComponents;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static is.codion.common.Configuration.enumValue;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Colors.darker;
import static is.codion.swing.common.ui.Utilities.disposeParentWindow;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.dialog.Dialogs.okCancelDialog;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityTableColumns.entityTableColumns;
import static is.codion.swing.framework.ui.component.EntitySearchField.ControlKeys.ADD;
import static is.codion.swing.framework.ui.component.EntitySearchField.ControlKeys.EDIT;
import static is.codion.swing.framework.ui.component.EntitySearchField.SearchIndicator.WAIT_CURSOR;
import static java.awt.event.FocusEvent.Cause.ACTIVATION;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A UI component based on the EntitySearchModel.
 * The search is triggered by the ENTER key and behaves in the following way:
 * If the search result is empty a message is shown, if a single entity fits the
 * condition then that entity is selected, otherwise a component displaying the entities
 * fitting the condition is shown in a dialog allowing either a single or multiple
 * selection based on the search model settings.
 * {@link ListSelector} is the default {@link Selector}.
 * Use {@link EntitySearchField#builder(EntitySearchModel)} for a builder instance.
 * @see EntitySearchModel
 * @see #builder(EntitySearchModel)
 * @see Builder#selectorFactory(Function)
 */
public final class EntitySearchField extends HintTextField {

	private static final MessageBundle MESSAGES =
					messageBundle(EntitySearchField.class, getBundle(EntitySearchField.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	/**
	 * Specifies the way a {@link EntitySearchField} indicates that a search is in progress.
	 * <ul>
	 * <li>Value type: {@link SearchIndicator}
	 * <li>Default value: {@link SearchIndicator#WAIT_CURSOR}
	 * </ul>
	 */
	public static final PropertyValue<SearchIndicator> SEARCH_INDICATOR =
					enumValue(EntitySearchField.class.getName() + ".searchIndicator", SearchIndicator.class, WAIT_CURSOR);

	/**
	 * The ways which a search field can indicate that a search is in progress.
	 */
	public enum SearchIndicator {
		/**
		 * Display a wait cursor while searching.
		 */
		WAIT_CURSOR,
		/**
		 * Display an indeterminate progress bar while searching
		 */
		PROGRESS_BAR
	}

	/**
	 * The available controls.
	 * @see Builder#editPanel(Supplier)
	 */
	public static final class ControlKeys {

		/**
		 * Displays a dialog for adding a new record.<br>
		 * Default key stroke: INSERT
		 */
		public static final ControlKey<CommandControl> ADD = CommandControl.key("add", keyStroke(VK_INSERT));
		/**
		 * Displays a dialog for editing the selected record.<br>
		 * Default key stroke: CTRL-INSERT
		 */
		public static final ControlKey<CommandControl> EDIT = CommandControl.key("edit", keyStroke(VK_INSERT, CTRL_DOWN_MASK));

		private ControlKeys() {}
	}

	private static final TransferFocusCommand FORWARD = new TransferFocusCommand(true);
	private static final TransferFocusCommand BACKWARD = new TransferFocusCommand(false);
	private static final Function<Entity, String> DEFAULT_TO_STRING = Object::toString;
	private static final String DEFAULT_SEPARATOR = ", ";

	private final EntitySearchModel model;
	private final State searchEnabled = State.builder()
					.listener(this::updateColors)
					.build();
	private final Control transferFocusForward = Control.builder()
					.action(FORWARD)
					.enabled(searchEnabled.not())
					.build();
	private final Control transferFocusBackward = Control.builder()
					.action(BACKWARD)
					.enabled(searchEnabled.not())
					.build();
	private final Function<Entity, String> stringFactory;
	private final String separator;
	private final boolean searchOnFocusLost;
	private final State searching = State.state();
	private final Consumer<Boolean> searchIndicator;
	private final Function<EntitySearchField, Selector> selectorFactory;
	private final ControlMap controlMap;

	private SettingsPanel settingsPanel;
	private ProgressWorker<List<Entity>, ?> searchWorker;
	private Control searchControl;

	private Color backgroundColor;
	private Color searchBackgroundColor;

	private EntitySearchField(AbstractBuilder<?, ?> builder) {
		super(builder.searchHintEnabled ? Messages.search() + "..." : null);
		model = requireNonNull(builder.searchModel);
		controlMap = builder.controlMap;
		controlMap.control(ADD).set(createAddControl(builder.editPanel,
						controlMap.keyStroke(ADD).get(), builder.confirmAdd));
		controlMap.control(EDIT).set(createEditControl(builder.editPanel,
						controlMap.keyStroke(EDIT).get(), builder.confirmEdit));
		if (builder.columns != -1) {
			setColumns(builder.columns);
		}
		if (builder.upperCase) {
			TextComponents.upperCase(getDocument());
		}
		if (builder.lowerCase) {
			TextComponents.lowerCase(getDocument());
		}
		if (!builder.editable) {
			setEditable(false);
		}
		searchOnFocusLost = builder.searchOnFocusLost;
		searchIndicator = createSearchIndicator(builder.searchIndicator);
		searching.addConsumer(searchIndicator);
		selectorFactory = builder.selectorFactory;
		stringFactory = builder.stringFactory;
		separator = builder.separator;
		setComponentPopupMenu(createPopupMenu());
		onSelectionChanged();
		configureColors();
		bindEvents();
	}

	private CommandControl createAddControl(Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke, boolean confirm) {
		return editPanel == null ? null : EntityControls.createAddControl(this, editPanel, keyStroke, confirm);
	}

	private CommandControl createEditControl(Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke, boolean confirm) {
		return editPanel == null ? null : EntityControls.createEditControl(this, editPanel, keyStroke, confirm);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (model != null) {
			configureColors();
		}
		if (searchIndicator instanceof SearchProgressBar) {
			((SearchProgressBar) searchIndicator).progressBar.updateUI();
		}
	}

	/**
	 * @return the search model this search field is based on
	 */
	public EntitySearchModel model() {
		return model;
	}

	/**
	 * @param transferFocusOnEnter true if this component should transfer focus on Enter
	 */
	public void transferFocusOnEnter(boolean transferFocusOnEnter) {
		KeyEvents.Builder transferForward = KeyEvents.builder(VK_ENTER)
						.condition(WHEN_FOCUSED)
						.action(transferFocusForward);
		KeyEvents.Builder transferBackward = KeyEvents.builder(VK_ENTER)
						.condition(WHEN_FOCUSED)
						.modifiers(SHIFT_DOWN_MASK)
						.action(transferFocusBackward);
		if (transferFocusOnEnter) {
			transferForward.enable(this);
			transferBackward.enable(this);
		}
		else {
			transferForward.disable(this);
			transferBackward.disable(this);
		}
	}

	/**
	 * @return a Control for triggering a search
	 */
	public Control searchControl() {
		if (searchControl == null) {
			searchControl = Control.builder()
							.command(this::performSearch)
							.smallIcon(ICONS.search())
							.enabled(searchEnabled)
							.build();
		}

		return searchControl;
	}

	/**
	 * @return a Control for inserting a new record, if one is available
	 * @see Builder#editPanel(Supplier)
	 */
	public Optional<CommandControl> addControl() {
		return controlMap.control(ADD).optional();
	}

	/**
	 * @return a Control for editing the selected record, if one is available
	 * @see Builder#editPanel(Supplier)
	 */
	public Optional<CommandControl> editControl() {
		return controlMap.control(EDIT).optional();
	}

	/**
	 * Instantiates a new {@link Builder.Factory}
	 * @param searchModel the search model on which to base the search field
	 * @return a new builder factory instance
	 */
	public static Builder.Factory builder(EntitySearchModel searchModel) {
		return new DefaultBuilderFactory(requireNonNull(searchModel));
	}

	/**
	 * Builds a entity search field.
	 */
	public interface Builder<T, B extends Builder<T, B>> extends ComponentBuilder<T, EntitySearchField, B> {

		/**
		 * @param columns the number of colums in the text field
		 * @return this builder instance
		 */
		B columns(int columns);

		/**
		 * Makes the field convert all lower case input to upper case
		 * @param upperCase if true the text component convert all lower case input to upper case
		 * @return this builder instance
		 */
		B upperCase(boolean upperCase);

		/**
		 * Makes the field convert all upper case input to lower case
		 * @param lowerCase if true the text component convert all upper case input to lower case
		 * @return this builder instance
		 */
		B lowerCase(boolean lowerCase);

		/**
		 * @param editable false if the field should not be editable
		 * @return this builder instance
		 */
		B editable(boolean editable);

		/**
		 * Overrides the default toString() for search elements when displayed in a field based on this model
		 * @param stringFactory the function providing the toString() functionality
		 * @return this builder
		 */
		B stringFactory(Function<Entity, String> stringFactory);

		/**
		 * Default ", "
		 * @param separator the String used to separate multiple items
		 * @return this builder
		 */
		B separator(String separator);

		/**
		 * @param searchHintEnabled true if a search hint text should be visible when the field is empty and not focused
		 * @return this builder instance
		 */
		B searchHintEnabled(boolean searchHintEnabled);

		/**
		 * @param searchOnFocusLost true if search should be performed on focus lost
		 * @return this builder instance
		 */
		B searchOnFocusLost(boolean searchOnFocusLost);

		/**
		 * @param searchIndicator the search indicator
		 * @return this builder instance
		 */
		B searchIndicator(SearchIndicator searchIndicator);

		/**
		 * @param selectorFactory the selector factory to use
		 * @return this builder instance
		 */
		B selectorFactory(Function<EntitySearchField, Selector> selectorFactory);

		/**
		 * A edit panel is required for the add and edit controls.
		 * @param editPanel the edit panel supplier
		 * @return this builder instance
		 */
		B editPanel(Supplier<EntityEditPanel> editPanel);

		/**
		 * @param controlKey the control key
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		B keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke);

		/**
		 * @param limit the search result limit
		 * @return this builder instance
		 */
		B limit(int limit);

		/**
		 * @param confirmAdd true if adding an item should be confirmed
		 * @return this builder instance
		 * @see #editPanel(Supplier)
		 */
		B confirmAdd(boolean confirmAdd);

		/**
		 * @param confirmEdit true if editing an item should be confirmed
		 * @return this builder instance
		 * @see #editPanel(Supplier)
		 */
		B confirmEdit(boolean confirmEdit);

		/**
		 * Provides multi or single selection {@link Builder.Factory} instances
		 */
		interface Factory {

			/**
			 * Instantiates a new {@link MultiSelectionBuilder}
			 * @return a new builder instance
			 */
			MultiSelectionBuilder multiSelection();

			/**
			 * Instantiates a new {@link SingleSelectionBuilder}
			 * @return a new builder instance
			 */
			SingleSelectionBuilder singleSelection();
		}
	}

	/**
	 * Builds a multi selection entity search field.
	 */
	public interface MultiSelectionBuilder extends Builder<Set<Entity>, MultiSelectionBuilder> {}

	/**
	 * Builds a single selection entity search field.
	 */
	public interface SingleSelectionBuilder extends Builder<Entity, SingleSelectionBuilder> {}

	private void bindEvents() {
		getDocument().addDocumentListener((DocumentAdapter) e -> updateSearchStrings());
		model.search().strings().addListener(this::updateSearchEnabled);
		model.selection().entities().addListener(this::onSelectionChanged);
		addFocusListener(new FocusListener());
		addKeyListener(new EnterEscapeListener());
	}

	private void updateSearchStrings() {
		String text = getText();
		if (text.isEmpty() || text.equals(selectionString())) {
			model.search().strings().clear();
		}
		else {
			model.search().strings().set(model.singleSelection() ? singleton(text) : asList(text.split(separator)));
		}
	}

	private void updateSearchEnabled() {
		searchEnabled.set(getText().isEmpty() && model.selection().empty().not().get() || !getText().equals(selectionString()));
	}

	private void onSelectionChanged() {
		setToolTipText(selectionToolTip());
		String text = selectionString();
		setText(text);
		setCaretPosition(text.length());
		moveCaretPosition(0);
		searchEnabled.set(false);
	}

	private Consumer<Boolean> createSearchIndicator(SearchIndicator indicator) {
		switch (indicator) {
			case WAIT_CURSOR:
				return new SearchWaitCursor();
			case PROGRESS_BAR:
				return new SearchProgressBar();
			default:
				throw new IllegalArgumentException("Unknown search indicator: " + indicator);
		}
	}

	private void configureColors() {
		this.backgroundColor = UIManager.getColor("TextField.background");
		this.searchBackgroundColor = darker(backgroundColor);
		updateColors();
	}

	private void updateColors() {
		if (isEnabled()) {
			setBackground(searchEnabled.get() ? searchBackgroundColor : backgroundColor);
		}
	}

	private String selectionToolTip() {
		return model.selection().empty().get() ? null : strings()
						.map(EntitySearchField::escape)
						.collect(joining("<br>", "<html>", "</html"));
	}

	private String selectionString() {
		return strings().collect(joining(separator));
	}

	private Stream<String> strings() {
		return model.selection().entities().get().stream()
						.sorted()
						.map(stringFactory);
	}

	private void performSearch() {
		performSearch(true);
	}

	private void performSearch(boolean promptUser) {
		if (model.search().strings().isEmpty()) {
			model.selection().clear();
		}
		else if (searchEnabled.get()) {
			cancelCurrentSearch();
			searching.set(true);
			searchWorker = ProgressWorker.builder(model.search()::result)
							.onResult(searchResult -> handleResult(searchResult, promptUser))
							.onException(this::handleException)
							.onCancelled(this::handleCancel)
							.onInterrupted(this::handleInterrupted)
							.execute();
		}
	}

	private void cancelCurrentSearch() {
		ProgressWorker<?, ?> currentWorker = searchWorker;
		if (currentWorker != null) {
			currentWorker.cancel(true);
		}
	}

	private void handleResult(List<Entity> searchResult, boolean promptUser) {
		endSearch();
		if (searchResult.size() == 1) {
			model.selection().entities().set(searchResult);
		}
		else if (promptUser) {
			promptUser(searchResult);
		}
	}

	private void promptUser(List<Entity> searchResult) {
		if (searchResult.isEmpty()) {
			JOptionPane.showMessageDialog(this, FrameworkMessages.noSearchResults(),
							SwingMessages.get("OptionPane.messageDialogTitle"), JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			selectorFactory.apply(this).select(searchResult);
		}
	}

	private void handleException(Exception exception) {
		endSearch();
		Dialogs.displayExceptionDialog(exception, Utilities.parentWindow(this));
	}

	private void handleCancel() {
		endSearch();
	}

	private void handleInterrupted() {
		endSearch();
		Thread.currentThread().interrupt();
	}

	private void endSearch() {
		searchWorker = null;
		searching.set(false);
	}

	private JPopupMenu createPopupMenu() {
		return menu(Controls.controls(Control.builder()
						.command(() -> Dialogs.componentDialog(settingsPanel())
										.owner(EntitySearchField.this)
										.title(FrameworkMessages.settings())
										.icon(ICONS.settings())
										.show())
						.name(FrameworkMessages.settings())
						.smallIcon(ICONS.settings())
						.build()))
						.buildPopupMenu();
	}

	private SettingsPanel settingsPanel() {
		if (settingsPanel == null) {
			settingsPanel = new SettingsPanel(model);
		}

		return settingsPanel;
	}

	private static String escape(String string) {
		return string.replace("<", "&lt;").replace(">", "&gt;");
	}

	private static final class SettingsPanel extends JPanel {

		private SettingsPanel(EntitySearchModel searchModel) {
			initializeUI(searchModel);
		}

		private void initializeUI(EntitySearchModel searchModel) {
			setLayout(borderLayout());
			setBorder(emptyBorder());
			add(createSearchColumnPanel(searchModel), BorderLayout.CENTER);
			add(createSouthPanel(searchModel), BorderLayout.SOUTH);
		}

		private static JPanel createSearchColumnPanel(EntitySearchModel searchModel) {
			CardLayout cardLayout = new CardLayout(5, 5);
			PanelBuilder columnBasePanelBuilder = panel(cardLayout);
			List<Item<Column<String>>> items = new ArrayList<>();
			EntityDefinition definition = searchModel.entityDefinition();
			for (Map.Entry<Column<String>, EntitySearchModel.Settings> entry : searchModel.settings().entrySet()) {
				items.add(Item.item(entry.getKey(), definition.columns().definition(entry.getKey()).caption()));
				columnBasePanelBuilder.add(createSettingsPanel(entry.getValue()), entry.getKey().name());
			}
			FilterComboBoxModel<Item<Column<String>>> columnComboBoxModel = FilterComboBoxModel.builder(items).build();
			JPanel columnBasePanel = columnBasePanelBuilder.build();
			if (columnComboBoxModel.getSize() > 0) {
				columnComboBoxModel.selection().item().addConsumer(selected ->
								cardLayout.show(columnBasePanel, selected.value().name()));
				columnComboBoxModel.selection().item().set(columnComboBoxModel.getElementAt(0));
			}

			return borderLayoutPanel()
							.border(createTitledBorder(MESSAGES.getString("search_columns")))
							.northComponent(comboBox(columnComboBoxModel).build())
							.centerComponent(columnBasePanel)
							.build();
		}

		private static JPanel createSouthPanel(EntitySearchModel searchModel) {
			return borderLayoutPanel()
							.eastComponent(createLimitPanel(searchModel))
							.build();
		}

		private static JPanel createLimitPanel(EntitySearchModel searchModel) {
			return borderLayoutPanel()
							.westComponent(new JLabel(MESSAGES.getString("result_limit")))
							.centerComponent(integerField(searchModel.limit())
											.columns(4)
											.build())
							.build();
		}

		private static JPanel createSettingsPanel(EntitySearchModel.Settings settings) {
			return gridLayoutPanel(4, 1)
							.add(checkBox(settings.caseSensitive())
											.text(MESSAGES.getString("case_sensitive"))
											.build())
							.add(checkBox(settings.wildcardPrefix())
											.text(MESSAGES.getString("prefix_wildcard"))
											.build())
							.add(checkBox(settings.wildcardPostfix())
											.text(MESSAGES.getString("postfix_wildcard"))
											.build())
							.add(checkBox(settings.spaceAsWildcard())
											.text(MESSAGES.getString("space_as_wildcard"))
											.build())
							.build();
		}
	}

	/**
	 * Provides a way for the user to select one or more of a given set of entities
	 * @see #listSelector(EntitySearchField)
	 * @see #tableSelector(EntitySearchField)
	 */
	public interface Selector {

		/**
		 * Displays a dialog for selecting from the given entities.
		 * @param entities the entities to select from
		 */
		void select(List<Entity> entities);

		/**
		 * Sets the preferred size of the selection component.
		 * @param preferredSize the preferred selection component size
		 */
		void preferredSize(Dimension preferredSize);
	}

	/**
	 * A {@link Selector} based on a {@link JList}.
	 */
	public interface ListSelector extends Selector {

		/**
		 * @return the list used for selecting entities
		 */
		JList<Entity> list();
	}

	/**
	 * A {@link Selector} based on a {@link FilterTable}.
	 */
	public interface TableSelector extends Selector {

		/**
		 * @return the table used for selecting entities
		 */
		FilterTable<Entity, Attribute<?>> table();
	}

	/**
	 * @param searchField the search field
	 * @return a {@link Selector} based on a {@link JList}.
	 */
	public static ListSelector listSelector(EntitySearchField searchField) {
		return new DefaultListSelector(searchField);
	}

	/**
	 * @param searchField the search field
	 * @return a {@link Selector} based on a {@link FilterTable}.
	 */
	public static TableSelector tableSelector(EntitySearchField searchField) {
		return new DefaultTableSelector(searchField);
	}

	private static final class DefaultListSelector implements ListSelector {

		private final EntitySearchField searchField;
		private final JList<Entity> list;
		private final Function<Entity, String> stringFactory;
		private final JPanel selectorPanel;
		private final JLabel resultLimitLabel = label()
						.horizontalAlignment(SwingConstants.RIGHT)
						.build();
		private final Control selectControl = command(new SelectCommand());

		private DefaultListSelector(EntitySearchField searchField) {
			this.searchField = requireNonNull(searchField);
			this.list = createList(searchField.model);
			this.stringFactory = searchField.stringFactory;
			this.selectorPanel = borderLayoutPanel()
							.centerComponent(scrollPane(list).build())
							.southComponent(resultLimitLabel)
							.border(createEmptyBorder(Layouts.GAP.getOrThrow(), Layouts.GAP.getOrThrow(), 0, Layouts.GAP.getOrThrow()))
							.build();
		}

		@Override
		public JList<Entity> list() {
			return list;
		}

		@Override
		public void select(List<Entity> entities) {
			DefaultListModel<Entity> listModel = (DefaultListModel<Entity>) list.getModel();
			requireNonNull(entities).forEach(listModel::addElement);
			list.scrollRectToVisible(list.getCellBounds(0, 0));
			initializeResultLimitMessage(resultLimitLabel, searchField.model.limit().optional().orElse(-1), entities.size());

			okCancelDialog(selectorPanel)
							.owner(searchField)
							.title(MESSAGES.getString("select_entity"))
							.okAction(selectControl)
							.show();

			listModel.removeAllElements();
		}

		@Override
		public void preferredSize(Dimension preferredSize) {
			selectorPanel.setPreferredSize(preferredSize);
		}

		private JList<Entity> createList(EntitySearchModel searchModel) {
			DefaultListModel<Entity> listModel = new DefaultListModel<>();
			ListBuilder<Entity, ?, ?> listBuilder = searchModel.singleSelection() ?
							Components.list(listModel).selectedItem() : Components.list(listModel).selectedItems();

			return listBuilder.mouseListener(new DoubleClickListener())
							.cellRenderer(new Renderer())
							.onBuild(new RemoveDefaultEnterAction())
							.build();
		}

		private final class Renderer implements ListCellRenderer<Entity> {

			private final ListCellRenderer<Object> listCellRenderer = new DefaultListCellRenderer();

			@Override
			public Component getListCellRendererComponent(JList<? extends Entity> list, Entity value,
																										int index, boolean isSelected, boolean cellHasFocus) {
				return listCellRenderer.getListCellRendererComponent(list,
								stringFactory.apply(value), index, isSelected, cellHasFocus);
			}
		}

		private final class SelectCommand implements Control.Command {
			@Override
			public void execute() {
				searchField.model.selection().entities().set(list.getSelectedValuesList());
				disposeParentWindow(list);
			}
		}

		private final class DoubleClickListener extends MouseAdapter {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectControl.actionPerformed(null);
				}
			}
		}

		private static final class RemoveDefaultEnterAction implements Consumer<JList<Entity>> {
			@Override
			public void accept(JList list) {
				list.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
								.put(KeyStroke.getKeyStroke(VK_ENTER, 0), "none");
			}
		}
	}

	private static final class DefaultTableSelector implements TableSelector {

		private final EntitySearchField searchField;
		private final FilterTable<Entity, Attribute<?>> table;
		private final JPanel selectorPanel;
		private final JLabel resultLimitLabel = label()
						.horizontalAlignment(SwingConstants.RIGHT)
						.build();
		private final Control selectControl = command(new SelectCommand());

		private DefaultTableSelector(EntitySearchField searchField) {
			this.searchField = requireNonNull(searchField);
			table = createTable();
			selectorPanel = borderLayoutPanel()
							.centerComponent(scrollPane(table).build())
							.southComponent(borderLayoutPanel()
											.westComponent(table.searchField())
											.centerComponent(resultLimitLabel)
											.build())
							.border(createEmptyBorder(Layouts.GAP.getOrThrow(), Layouts.GAP.getOrThrow(), 0, Layouts.GAP.getOrThrow()))
							.build();
		}

		/**
		 * @return the underlying FilterTable
		 */
		public FilterTable<Entity, Attribute<?>> table() {
			return table;
		}

		@Override
		public void select(List<Entity> entities) {
			table.model().items().visible().add(0, entities);
			table.scrollRectToVisible(table.getCellRect(0, 0, true));
			initializeResultLimitMessage(resultLimitLabel, searchField.model.limit().optional().orElse(-1), entities.size());

			okCancelDialog(selectorPanel)
							.owner(searchField)
							.title(MESSAGES.getString("select_entity"))
							.okAction(selectControl)
							.show();

			table.model().items().clear();
			table.searchField().setText("");
		}

		@Override
		public void preferredSize(Dimension preferredSize) {
			selectorPanel.setPreferredSize(preferredSize);
		}

		private FilterTable<Entity, Attribute<?>> createTable() {
			SwingEntityTableModel tableModel =
							new SwingEntityTableModel(searchField.model.entityDefinition().type(),
											emptyList(), searchField.model.connectionProvider());

			FilterTable<Entity, Attribute<?>> filterTable = FilterTable.builder(tableModel,
											entityTableColumns(tableModel.entityDefinition()))
							.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
							.cellRendererFactory(EntityTableCellRenderer.factory())
							.selectionMode(searchField.model.singleSelection() ?
											ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
							.doubleClickAction(selectControl)
							.keyEvent(KeyEvents.builder(VK_ENTER)
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(selectControl))
							.keyEvent(KeyEvents.builder(VK_ENTER)
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(selectControl))
							.keyEvent(KeyEvents.builder(VK_F)
											.modifiers(CTRL_DOWN_MASK)
											.action(command(this::requestSearchFieldFocus)))
							.onBuild(t -> KeyEvents.builder(VK_ENTER)
											.action(selectControl)
											.enable(t.searchField()))
							.build();

			filterTable.model().sorter().ascending(searchField.model.columns().iterator().next());
			filterTable.columnModel().visible().set(searchField.model.columns().toArray(new Attribute[0]));

			return filterTable;
		}

		private void requestSearchFieldFocus() {
			table.searchField().requestFocusInWindow();
		}

		private final class SelectCommand implements Control.Command {
			@Override
			public void execute() {
				searchField.model.selection().entities().set(table.model().selection().items().get());
				disposeParentWindow(table);
			}
		}
	}

	private static void initializeResultLimitMessage(JLabel label, int limit, int resultSize) {
		boolean resultLimitReached = limit == resultSize;
		if (resultLimitReached) {
			label.setText(format(MESSAGES.getString("result_limited"), limit));
			label.setVisible(true);
		}
		label.setVisible(resultLimitReached);
	}

	private static final class SingleSelectionValue extends AbstractComponentValue<Entity, EntitySearchField> {

		private SingleSelectionValue(EntitySearchField searchField) {
			super(searchField);
			searchField.model.selection().entity().addListener(this::notifyListeners);
		}

		@Override
		protected Entity getComponentValue() {
			return component().model().selection().entity().get();
		}

		@Override
		protected void setComponentValue(Entity value) {
			component().model().selection().entity().set(value);
		}
	}

	private static final class MultiSelectionValue extends AbstractComponentValue<Set<Entity>, EntitySearchField> {

		private MultiSelectionValue(EntitySearchField searchField) {
			super(searchField);
			searchField.model.selection().entities().addListener(this::notifyListeners);
		}

		@Override
		protected Set<Entity> getComponentValue() {
			return component().model().selection().entities().get();
		}

		@Override
		protected void setComponentValue(Set<Entity> value) {
			component().model().selection().entities().set(value);
		}
	}

	private final class FocusListener extends FocusAdapter {

		@Override
		public void focusGained(FocusEvent e) {
			if (!e.getCause().equals(ACTIVATION)) {
				setCaretPosition(getText().length());
				moveCaretPosition(0);
			}
		}

		@Override
		public void focusLost(FocusEvent e) {
			if (!e.isTemporary()) {
				if (getText().isEmpty()) {
					model().selection().clear();
				}
				else if (shouldPerformSearch()) {
					performSearch(false);
				}
			}
		}

		private boolean shouldPerformSearch() {
			return searchOnFocusLost && !searching.get() && searchEnabled.get();
		}
	}

	private final class EnterEscapeListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if (searchEnabled.get()) {
				if (e.getKeyCode() == VK_ENTER) {
					e.consume();
					performSearch(true);
				}
				else if (e.getKeyCode() == VK_ESCAPE) {
					e.consume();
					onSelectionChanged();
				}
			}
		}
	}

	private final class SearchWaitCursor implements Consumer<Boolean> {

		private final Cursor defaultCursor = getCursor();

		@Override
		public void accept(Boolean isSearching) {
			if (isSearching) {
				setCursor(Cursors.WAIT);
			}
			else {
				setCursor(defaultCursor);
			}
		}
	}

	private final class SearchProgressBar implements Consumer<Boolean> {

		private final JProgressBar progressBar = progressBar()
						.indeterminate(true)
						.string(MESSAGES.getString("searching") + "...")
						.stringPainted(true)
						.build();

		@Override
		public void accept(Boolean isSearching) {
			if (isSearching) {
				setLayout(new BorderLayout());
				add(progressBar, BorderLayout.CENTER);
			}
			else {
				remove(progressBar);
				setLayout(null);
			}
			revalidate();
			repaint();
		}
	}

	private static final class TransferFocusCommand implements Control.ActionCommand {

		private final boolean forward;

		private TransferFocusCommand(boolean forward) {
			this.forward = forward;
		}

		@Override
		public void execute(ActionEvent event) throws Exception {
			JComponent component = (JComponent) event.getSource();
			if (forward) {
				component.transferFocus();
			}
			else {
				component.transferFocusBackward();
			}
		}
	}

	private static final class DefaultBuilderFactory implements Builder.Factory {

		private final EntitySearchModel searchModel;

		private DefaultBuilderFactory(EntitySearchModel searchModel) {
			this.searchModel = searchModel;
		}

		@Override
		public MultiSelectionBuilder multiSelection() {
			return new DefaultMultiSelectionBuilder(searchModel);
		}

		@Override
		public SingleSelectionBuilder singleSelection() {
			return new DefaultSingleSelectionBuilder(searchModel);
		}
	}

	private static final class DefaultMultiSelectionBuilder
					extends AbstractBuilder<Set<Entity>, MultiSelectionBuilder> implements MultiSelectionBuilder {

		private DefaultMultiSelectionBuilder(EntitySearchModel searchModel) {
			super(searchModel);
			if (searchModel.singleSelection()) {
				throw new IllegalArgumentException("EntitySearchModel is configured for single selection");
			}
		}

		@Override
		protected ComponentValue<Set<Entity>, EntitySearchField> createComponentValue(EntitySearchField component) {
			return new MultiSelectionValue(component);
		}
	}

	private static final class DefaultSingleSelectionBuilder
					extends AbstractBuilder<Entity, SingleSelectionBuilder> implements SingleSelectionBuilder {

		private DefaultSingleSelectionBuilder(EntitySearchModel searchModel) {
			super(searchModel);
			if (!searchModel.singleSelection()) {
				throw new IllegalArgumentException("EntitySearchModel is not configured for single selection");
			}
		}

		@Override
		protected ComponentValue<Entity, EntitySearchField> createComponentValue(EntitySearchField component) {
			return new SingleSelectionValue(component);
		}
	}

	private abstract static class AbstractBuilder<T, B extends Builder<T, B>>
					extends AbstractComponentBuilder<T, EntitySearchField, B> implements Builder<T, B> {

		private final EntitySearchModel searchModel;
		private final ControlMap controlMap = controlMap(ControlKeys.class);

		private int columns = -1;
		private boolean upperCase;
		private boolean lowerCase;
		private boolean editable = true;
		private boolean searchHintEnabled = true;
		private boolean searchOnFocusLost = true;
		private SearchIndicator searchIndicator = SEARCH_INDICATOR.get();
		private Function<EntitySearchField, Selector> selectorFactory = new ListSelectorFactory();
		private Function<Entity, String> stringFactory = DEFAULT_TO_STRING;
		private String separator = DEFAULT_SEPARATOR;
		private Supplier<EntityEditPanel> editPanel;
		private boolean confirmAdd;
		private boolean confirmEdit;

		private AbstractBuilder(EntitySearchModel searchModel) {
			this.searchModel = searchModel;
		}

		@Override
		public B columns(int columns) {
			this.columns = columns;
			return (B) this;
		}

		@Override
		public B upperCase(boolean upperCase) {
			if (upperCase && lowerCase) {
				throw new IllegalArgumentException("Field is already lowercase");
			}
			this.upperCase = upperCase;
			return (B) this;
		}

		@Override
		public B lowerCase(boolean lowerCase) {
			if (lowerCase && upperCase) {
				throw new IllegalArgumentException("Field is already uppercase");
			}
			this.lowerCase = lowerCase;
			return (B) this;
		}

		@Override
		public B editable(boolean editable) {
			this.editable = editable;
			return (B) this;
		}

		@Override
		public B stringFactory(Function<Entity, String> stringFactory) {
			this.stringFactory = requireNonNull(stringFactory);
			return (B) this;
		}

		@Override
		public B separator(String separator) {
			if (requireNonNull(separator).isEmpty()) {
				throw new IllegalArgumentException("Separator must not be empty");
			}
			this.separator = separator;
			return (B) this;
		}

		@Override
		public B searchHintEnabled(boolean searchHintEnabled) {
			this.searchHintEnabled = searchHintEnabled;
			return (B) this;
		}

		@Override
		public B searchOnFocusLost(boolean searchOnFocusLost) {
			this.searchOnFocusLost = searchOnFocusLost;
			return (B) this;
		}

		@Override
		public B searchIndicator(SearchIndicator searchIndicator) {
			this.searchIndicator = requireNonNull(searchIndicator);
			return (B) this;
		}

		@Override
		public B selectorFactory(Function<EntitySearchField, Selector> selectorFactory) {
			this.selectorFactory = requireNonNull(selectorFactory);
			return (B) this;
		}

		@Override
		public B editPanel(Supplier<EntityEditPanel> editPanel) {
			this.editPanel = requireNonNull(editPanel);
			return (B) this;
		}

		@Override
		public B keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke) {
			controlMap.keyStroke(controlKey).set(keyStroke);
			return (B) this;
		}

		@Override
		public B limit(int limit) {
			this.searchModel.limit().set(limit);
			return (B) this;
		}

		@Override
		public B confirmAdd(boolean confirmAdd) {
			this.confirmAdd = confirmAdd;
			return (B) this;
		}

		@Override
		public B confirmEdit(boolean confirmEdit) {
			this.confirmEdit = confirmEdit;
			return (B) this;
		}

		@Override
		protected EntitySearchField createComponent() {
			return new EntitySearchField(this);
		}

		@Override
		protected void enableTransferFocusOnEnter(EntitySearchField component) {
			component.transferFocusOnEnter(true);
		}

		private static final class ListSelectorFactory implements Function<EntitySearchField, Selector> {

			@Override
			public Selector apply(EntitySearchField searchField) {
				return new DefaultListSelector(searchField);
			}
		}
	}
}
