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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.i18n.Messages;
import is.codion.common.model.worker.ProgressWorker;
import is.codion.common.model.worker.ProgressWorker.ResultTaskHandler;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.action.DelayedAction;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.common.model.component.list.SwingFilterListModel;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.SwingMessages;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.panel.LayoutPanelBuilder;
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
import is.codion.swing.framework.ui.EntityTableCellRenderers;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.enumValue;
import static is.codion.common.utilities.Configuration.integerValue;
import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.model.action.DelayedAction.delayedAction;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.color.Colors.darker;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.component.EntitySearchField.ControlKeys.ADD;
import static is.codion.swing.framework.ui.component.EntitySearchField.ControlKeys.EDIT;
import static is.codion.swing.framework.ui.component.EntitySearchField.SearchIndicator.PROGRESS_BAR;
import static java.awt.Cursor.getPredefinedCursor;
import static java.awt.event.FocusEvent.Cause.ACTIVATION;
import static java.awt.event.KeyEvent.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A UI component based on the EntitySearchModel.
 * The search is triggered by the ENTER key and behaves in the following way:
 * If the search result is empty a message is shown, if a single entity fits the
 * condition then that entity is selected, otherwise a component displaying the entities
 * fitting the condition is shown in a dialog, for selecting one of them.
 * <p>For collecting multiple entities, wrap the field in a
 * {@link is.codion.swing.common.ui.component.multivalue.MultiValueInput}.
 * {@link ListSelector} is the default {@link Selector}.
 * Use {@link EntitySearchField#builder()} for a builder instance.
 * @see EntitySearchModel
 * @see #builder()
 * @see Builder#selector(Function)
 */
public final class EntitySearchField extends HintTextField {

	private static final MessageBundle MESSAGES =
					messageBundle(EntitySearchField.class, getBundle(EntitySearchField.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	/**
	 * Specifies the way a {@link EntitySearchField} indicates that a search is in progress.
	 * <ul>
	 * <li>Value type: {@link SearchIndicator}
	 * <li>Default value: {@link SearchIndicator#PROGRESS_BAR}
	 * </ul>
	 * @see #SEARCH_PROGRESS_BAR_DELAY
	 */
	public static final PropertyValue<SearchIndicator> SEARCH_INDICATOR =
					enumValue(EntitySearchField.class.getName() + ".searchIndicator", SearchIndicator.class, PROGRESS_BAR);

	/**
	 * Specifies the number of milliseconds to delay showing the refresh progress bar, if enabled.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 350
	 * </ul>
	 * @see #SEARCH_INDICATOR
	 * @see SearchIndicator#PROGRESS_BAR
	 */
	public static final PropertyValue<Integer> SEARCH_PROGRESS_BAR_DELAY =
					integerValue(EntitySearchField.class.getName() + ".searchProgressBarDelay", 350);

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
	 * <p>Note: CTRL in key stroke descriptions represents the platform menu shortcut key (CTRL on Windows/Linux, ⌘ on macOS).
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
		public static final ControlKey<CommandControl> EDIT = CommandControl.key("edit", keyStroke(VK_INSERT, MENU_SHORTCUT_MASK));

		private ControlKeys() {}
	}

	private static final Cursor WAIT = getPredefinedCursor(Cursor.WAIT_CURSOR);
	private static final Function<Entity, String> DEFAULT_FORMATTER = Object::toString;

	private final EntitySearchModel model;
	private final Function<Entity, String> formatter;
	private final boolean searchOnFocusLost;
	private final boolean selectionToolTip;
	private final State searchReady = State.builder()
					.listener(this::updateColors)
					.build();
	private final State searching = State.state();
	private final ObservableState searchEnabled = State.and(searchReady, searching.not());
	private final Consumer<Boolean> searchIndicator;
	private final int searchRefreshProgressBarDelay;
	private final Function<EntitySearchField, Selector> selector;
	private final ControlMap controlMap;

	private @Nullable SettingsPanel settingsPanel;
	private @Nullable ProgressWorker<List<Entity>, ?> searchWorker;
	private @Nullable Control searchControl;

	private Color backgroundColor;
	private Color searchBackgroundColor;

	private EntitySearchField(DefaultBuilder builder) {
		super(builder.searchHintEnabled ? Messages.search() + "..." : null);
		model = builder.searchModel;
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
		selectionToolTip = builder.selectionToolTip;
		searchIndicator = createSearchIndicator(builder.searchIndicator);
		searchRefreshProgressBarDelay = builder.searchProgressBarDelay;
		searching.addConsumer(searchIndicator);
		selector = builder.selector;
		formatter = builder.formatter;
		setComponentPopupMenu(createPopupMenu());
		onSelectionChanged();
		configureColors();
		bindEvents();
	}

	private @Nullable CommandControl createAddControl(@Nullable Supplier<EntityEditPanel> editPanel, @Nullable KeyStroke keyStroke, boolean confirm) {
		return editPanel == null ? null : EntityControls.createAddControl(this, editPanel, keyStroke, confirm);
	}

	private @Nullable CommandControl createEditControl(@Nullable Supplier<EntityEditPanel> editPanel, @Nullable KeyStroke keyStroke, boolean confirm) {
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
	 * @return a Control for triggering a search
	 */
	public Control searchControl() {
		if (searchControl == null) {
			searchControl = Control.builder()
							.command(this::performSearch)
							.icon(ICONS.search())
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
	 * @return a {@link Builder.ModelStep}
	 */
	public static Builder.ModelStep builder() {
		return DefaultModelStep.MODEL;
	}

	/**
	 * Builds an entity search field.
	 */
	public interface Builder extends ComponentValueBuilder<EntitySearchField, Entity, Builder> {

		/**
		 * Provides a {@link EntitySearchField.Builder}
		 */
		interface ModelStep {

			/**
			 * @param model the search model
			 * @return a builder for a {@link EntitySearchField}
			 */
			Builder model(EntitySearchModel model);
		}

		/**
		 * @param columns the number of colums in the text field
		 * @return this builder instance
		 */
		Builder columns(int columns);

		/**
		 * Makes the field convert all lower case input to upper case
		 * @param upperCase if true the text component convert all lower case input to upper case
		 * @return this builder instance
		 */
		Builder upperCase(boolean upperCase);

		/**
		 * Makes the field convert all upper case input to lower case
		 * @param lowerCase if true the text component convert all upper case input to lower case
		 * @return this builder instance
		 */
		Builder lowerCase(boolean lowerCase);

		/**
		 * @param editable false if the field should not be editable
		 * @return this builder instance
		 */
		Builder editable(boolean editable);

		/**
		 * Overrides the default formatter for search elements when displayed in a field based on this field
		 * @param formatter the formatter
		 * @return this builder
		 */
		Builder formatter(Function<Entity, String> formatter);

		/**
		 * @param searchHintEnabled true if a search hint text should be visible when the field is empty and not focused
		 * @return this builder instance
		 */
		Builder searchHintEnabled(boolean searchHintEnabled);

		/**
		 * Specifies whether the selected entity should be available in a tool tip,
		 * for a field too narrow to display it in full. Default false.
		 * @param selectionToolTip true if the selected entity should be available in a tool tip
		 * @return this builder instance
		 */
		Builder selectionToolTip(boolean selectionToolTip);

		/**
		 * <p>Specifies whether a search should be performed when the field loses focus.
		 * <p>Note that the focus lost search only selects an item in case of a single result,
		 * multiple results are ignored in order to not display a result selection
		 * dialog during focus traversal.
		 * <p>Default true.
		 * @param searchOnFocusLost true if search should be performed on focus lost
		 * @return this builder instance
		 */
		Builder searchOnFocusLost(boolean searchOnFocusLost);

		/**
		 * @param searchIndicator the search indicator
		 * @return this builder instance
		 */
		Builder searchIndicator(SearchIndicator searchIndicator);

		/**
		 * @param searchProgressBarDelay the number of milliseconds to delay showing the search progress bar, if enabled
		 * @return this builder instance
		 * @see #SEARCH_PROGRESS_BAR_DELAY
		 */
		Builder searchProgressBarDelay(int searchProgressBarDelay);

		/**
		 * @param selector the selector factory to use
		 * @return this builder instance
		 */
		Builder selector(Function<EntitySearchField, Selector> selector);

		/**
		 * An edit panel is required for the add and edit controls.
		 * @param editPanel the edit panel supplier
		 * @return this builder instance
		 */
		Builder editPanel(Supplier<EntityEditPanel> editPanel);

		/**
		 * @param controlKey the control key
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke);

		/**
		 * @param limit the search result limit
		 * @return this builder instance
		 */
		Builder limit(int limit);

		/**
		 * @param confirmAdd true if adding an item should be confirmed
		 * @return this builder instance
		 * @see #editPanel(Supplier)
		 */
		Builder confirmAdd(boolean confirmAdd);

		/**
		 * @param confirmEdit true if editing an item should be confirmed
		 * @return this builder instance
		 * @see #editPanel(Supplier)
		 */
		Builder confirmEdit(boolean confirmEdit);
	}

	private void bindEvents() {
		getDocument().addDocumentListener((DocumentAdapter) e -> updateSearchStrings());
		model.search().strings().addListener(this::updateSearchReady);
		// notified when an updated entity replaces the selected one as well, updating the text
		model.selection().entity().addListener(this::onSelectionChanged);
		addFocusListener(new FocusListener());
		addKeyListener(new EnterEscapeListener());
	}

	private void updateSearchStrings() {
		String text = getText();
		if (text.isEmpty() || text.equals(selectionString())) {
			model.search().strings().clear();
		}
		else {
			model.search().strings().set(singleton(text));
		}
	}

	private void updateSearchReady() {
		searchReady.set(getText().isEmpty() && model.selection().present().is() || !getText().equals(selectionString()));
	}

	private void onSelectionChanged() {
		if (selectionToolTip) {
			setToolTipText(createSelectionToolTip());
		}
		String text = selectionString();
		setText(text);
		setCaretPosition(text.length());
		moveCaretPosition(0);
		searchReady.set(false);
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
			setBackground(searchReady.is() ? searchBackgroundColor : backgroundColor);
		}
	}

	private @Nullable String createSelectionToolTip() {
		return model.selection().entity().optional()
						.map(formatter)
						.orElse(null);
	}

	private String selectionString() {
		return model.selection().entity().optional()
						.map(formatter)
						.orElse("");
	}

	private void performSearch() {
		performSearch(true);
	}

	private void performSearch(boolean promptUser) {
		if (model.search().strings().isEmpty()) {
			model.selection().clear();
		}
		else {
			cancelCurrentSearch();
			SearchTask searchTask = new SearchTask(promptUser);
			searchWorker = ProgressWorker.builder()
							.task(searchTask)
							.execute();
			//assigned after execute() so onDone can identity-guard the cleanup against a superseded worker
			searchTask.worker = searchWorker;
		}
	}

	private final class SearchTask implements ResultTaskHandler<List<Entity>> {

		private final boolean promptUser;

		private @Nullable ProgressWorker<List<Entity>, ?> worker;

		private SearchTask(boolean promptUser) {
			this.promptUser = promptUser;
		}

		@Override
		public List<Entity> execute() {
			return model.search().perform();
		}

		@Override
		public void onStarted() {
			searching.set(true);
		}

		@Override
		public void onDone() {
			//a cancelled worker's onDone still runs, guard against clobbering a newer worker's state
			if (searchWorker == worker) {
				searchWorker = null;
				searching.set(false);
			}
		}

		@Override
		public void onResult(List<Entity> searchResult) {
			if (searchResult.size() == 1) {
				model.selection().entity().set(searchResult.get(0));
			}
			else if (promptUser) {
				promptUser(searchResult);
			}
		}

		@Override
		public void onException(Exception exception) {
			Dialogs.exception()
							.owner(Ancestor.window().of(EntitySearchField.this).get())
							.show(exception);
		}

		private void promptUser(List<Entity> searchResult) {
			if (searchResult.isEmpty()) {
				JOptionPane.showMessageDialog(EntitySearchField.this, FrameworkMessages.noSearchResults(),
								SwingMessages.get("OptionPane.messageDialogTitle"), JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				selector.apply(EntitySearchField.this).select(searchResult);
			}
		}
	}

	private void cancelCurrentSearch() {
		ProgressWorker<?, ?> currentWorker = searchWorker;
		if (currentWorker != null) {
			currentWorker.cancel(true);
		}
	}

	private JPopupMenu createPopupMenu() {
		return menu()
						.controls(Controls.controls(Control.builder()
										.command(() -> Dialogs.builder()
														.component(settingsPanel())
														.owner(EntitySearchField.this)
														.title(FrameworkMessages.settings())
														.icon(ICONS.settings().small())
														.show())
										.caption(FrameworkMessages.settings())
										.icon(ICONS.settings())
										.build()))
						.buildPopupMenu();
	}

	private SettingsPanel settingsPanel() {
		if (settingsPanel == null) {
			settingsPanel = new SettingsPanel(model);
		}

		return settingsPanel;
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
			LayoutPanelBuilder columnBasePanelBuilder = panel().layout(cardLayout);
			List<Item<Column<String>>> items = new ArrayList<>();
			EntityDefinition definition = searchModel.entityDefinition();
			for (Map.Entry<Column<String>, EntitySearchModel.Settings> entry : searchModel.settings().entrySet()) {
				items.add(Item.item(entry.getKey(), definition.columns().definition(entry.getKey()).caption()));
				columnBasePanelBuilder.add(createSettingsPanel(entry.getValue()), entry.getKey().name());
			}
			SwingFilterComboBoxModel<Item<Column<String>>> columnComboBoxModel = SwingFilterComboBoxModel.builder()
							.items(items)
							.build();
			JPanel columnBasePanel = columnBasePanelBuilder.build();
			if (columnComboBoxModel.getSize() > 0) {
				columnComboBoxModel.selection().item().addConsumer(selected ->
								cardLayout.show(columnBasePanel, selected.getOrThrow().name()));
				columnComboBoxModel.selection().item().set(columnComboBoxModel.getElementAt(0));
			}

			return borderLayoutPanel()
							.border(createTitledBorder(MESSAGES.getString("search_columns")))
							.north(comboBox()
											.model(columnComboBoxModel))
							.center(columnBasePanel)
							.build();
		}

		private static JPanel createSouthPanel(EntitySearchModel searchModel) {
			return borderLayoutPanel()
							.east(createLimitPanel(searchModel))
							.build();
		}

		private static JPanel createLimitPanel(EntitySearchModel searchModel) {
			return borderLayoutPanel()
							.west(new JLabel(MESSAGES.getString("result_limit")))
							.center(integerField()
											.link(searchModel.limit())
											.columns(4))
							.build();
		}

		private static JPanel createSettingsPanel(EntitySearchModel.Settings settings) {
			return gridLayoutPanel(4, 1)
							.add(checkBox()
											.link(settings.caseSensitive())
											.text(MESSAGES.getString("case_sensitive")))
							.add(checkBox()
											.link(settings.wildcardPrefix())
											.text(MESSAGES.getString("prefix_wildcard")))
							.add(checkBox()
											.link(settings.wildcardPostfix())
											.text(MESSAGES.getString("postfix_wildcard")))
							.add(checkBox()
											.link(settings.spaceAsWildcard())
											.text(MESSAGES.getString("space_as_wildcard")))
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
		private final FilterList<Entity> list;
		private final Function<Entity, String> formatter;
		private final JPanel selectorPanel;
		private final JLabel resultLimitLabel = label()
						.horizontalAlignment(SwingConstants.TRAILING)
						.build();
		private final Control selectControl = command(new SelectCommand());

		private DefaultListSelector(EntitySearchField searchField) {
			this.searchField = requireNonNull(searchField);
			this.list = createList();
			this.formatter = searchField.formatter;
			this.selectorPanel = borderLayoutPanel()
							.center(scrollPane()
											.view(list))
							.south(resultLimitLabel)
							.border(createEmptyBorder(Layouts.GAP.getOrThrow(), Layouts.GAP.getOrThrow(), 0, Layouts.GAP.getOrThrow()))
							.build();
		}

		@Override
		public JList<Entity> list() {
			return list;
		}

		@Override
		public void select(List<Entity> entities) {
			SwingFilterListModel<Entity> listModel = list.model();
			requireNonNull(entities).forEach(listModel.items()::add);
			list.scrollRectToVisible(list.getCellBounds(0, 0));
			initializeResultLimitMessage(resultLimitLabel, searchField.model.limit().optional().orElse(-1), entities.size());

			Dialogs.okCancel()
							.component(selectorPanel)
							.owner(searchField)
							.title(MESSAGES.getString("select_entity"))
							.okAction(selectControl)
							.show();

			listModel.items().clear();
		}

		@Override
		public void preferredSize(Dimension preferredSize) {
			selectorPanel.setPreferredSize(preferredSize);
		}

		private FilterList<Entity> createList() {
			SwingFilterListModel<Entity> listModel = SwingFilterListModel.builder()
							.<Entity>items()
							.build();

			return FilterList.builder()
							.model(listModel)
							.selectedItem()
							.mouseListener(new DoubleClickListener())
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
								formatter.apply(value), index, isSelected, cellHasFocus);
			}
		}

		private final class SelectCommand implements Control.Command {
			@Override
			public void execute() {
				searchField.model.selection().entity().set(list.getSelectedValue());
				Ancestor.window().of(list).dispose();
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

		private static final class RemoveDefaultEnterAction implements Consumer<FilterList<Entity>> {
			@Override
			public void accept(FilterList<Entity> list) {
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
						.horizontalAlignment(SwingConstants.TRAILING)
						.build();
		private final Control selectControl = command(new SelectCommand());

		private DefaultTableSelector(EntitySearchField searchField) {
			this.searchField = requireNonNull(searchField);
			table = createTable();
			selectorPanel = borderLayoutPanel()
							.center(scrollPane()
											.view(table))
							.south(borderLayoutPanel()
											.west(table.searchField())
											.center(resultLimitLabel))
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
			table.model().items().included().add(0, entities);
			table.scrollRectToVisible(table.getCellRect(0, 0, true));
			initializeResultLimitMessage(resultLimitLabel, searchField.model.limit().optional().orElse(-1), entities.size());

			Dialogs.okCancel()
							.component(selectorPanel)
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
											emptyList(), searchField.model.connection());

			FilterTable<Entity, Attribute<?>> filterTable = FilterTable.builder()
							.model(tableModel)
							.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
							.cellRenderers(new EntityTableCellRenderers())
							.selectionMode(ListSelectionModel.SINGLE_SELECTION)
							.doubleClick(selectControl)
							.keyEvent(KeyEvents.builder()
											.keyCode(VK_ENTER)
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(selectControl))
							.keyEvent(KeyEvents.builder()
											.keyCode(VK_F)
											.modifiers(MENU_SHORTCUT_MASK)
											.action(command(this::requestSearchFieldFocus)))
							.onBuild(t -> KeyEvents.builder()
											.keyCode(VK_ENTER)
											.action(selectControl)
											.enable(t.searchField()))
							.build();

			//sort ascending by the first search column (definition-ordered by default)
			filterTable.model().sort().ascending(searchField.model.columns().iterator().next());
			filterTable.columns().visible().set(searchField.model.columns().toArray(new Attribute[0]));

			return filterTable;
		}

		private void requestSearchFieldFocus() {
			table.searchField().requestFocusInWindow();
		}

		private final class SelectCommand implements Control.Command {
			@Override
			public void execute() {
				searchField.model.selection().entity().set(table.model().selection().item().get());
				Ancestor.window().of(table).dispose();
			}
		}
	}

	private static void initializeResultLimitMessage(JLabel label, int limit, int resultSize) {
		boolean resultLimitReached = limit == resultSize;
		if (resultLimitReached) {
			label.setText(MessageFormat.format(MESSAGES.getString("result_limited"), limit));
		}
		label.setVisible(resultLimitReached);
	}

	private static final class SelectionValue extends AbstractComponentValue<EntitySearchField, Entity> {

		private SelectionValue(EntitySearchField searchField) {
			super(searchField);
			searchField.model.selection().entity().addListener(this::notifyObserver);
		}

		@Override
		protected @Nullable Entity getComponentValue() {
			return component().model().selection().entity().get();
		}

		@Override
		protected void setComponentValue(@Nullable Entity value) {
			component().model().selection().entity().set(value);
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
				// Selection uses Notify.SET, so no unnecessary clear() calls
				if (getText().isEmpty() && model().selection().present().is()) {
					model().selection().clear();
				}
				else if (shouldPerformSearch()) {
					performSearch(false);
				}
			}
		}

		private boolean shouldPerformSearch() {
			return searchOnFocusLost && searchEnabled.is();
		}
	}

	private final class EnterEscapeListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == VK_ENTER) {
				onEnter(e);
			}
			else if (e.getKeyCode() == VK_ESCAPE) {
				onEscape(e);
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
			if (searching.is()) {
				e.consume();
			}
		}

		private void onEnter(KeyEvent e) {
			if (searchEnabled.is()) {
				e.consume();
				performSearch(true);
			}
		}

		private void onEscape(KeyEvent e) {
			if (searchEnabled.is()) {
				e.consume();
				onSelectionChanged();
			}
			else if (searching.is()) {
				e.consume();
				cancelCurrentSearch();
			}
		}
	}

	private final class SearchWaitCursor implements Consumer<Boolean> {

		private final Cursor defaultCursor = getCursor();

		@Override
		public void accept(Boolean isSearching) {
			if (isSearching) {
				setCursor(WAIT);
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
		private @Nullable DelayedAction showProgressBarAction;

		@Override
		public void accept(Boolean isSearching) {
			if (isSearching) {
				showProgressBarDelayed();
			}
			else {
				hideProgressBar();
			}
		}

		private void showProgressBarDelayed() {
			showProgressBarAction = delayedAction(() -> {
				setLayout(new BorderLayout());
				add(progressBar, BorderLayout.CENTER);
				revalidate();
				repaint();
			}, searchRefreshProgressBarDelay);
		}

		private void hideProgressBar() {
			cancelShowProgressBar();
			remove(progressBar);
			setLayout(null);
			revalidate();
			repaint();
		}

		private void cancelShowProgressBar() {
			if (showProgressBarAction != null) {
				showProgressBarAction.cancel();
				showProgressBarAction = null;
			}
		}
	}

	private static final class DefaultModelStep implements Builder.ModelStep {

		private static final Builder.ModelStep MODEL = new DefaultModelStep();

		@Override
		public Builder model(EntitySearchModel model) {
			return new DefaultBuilder(requireNonNull(model));
		}
	}

	private static final class DefaultBuilder
					extends AbstractComponentValueBuilder<EntitySearchField, Entity, Builder> implements Builder {

		private final EntitySearchModel searchModel;
		private final ControlMap controlMap = controlMap(ControlKeys.class);

		private int columns = -1;
		private boolean upperCase;
		private boolean lowerCase;
		private boolean editable = true;
		private boolean searchHintEnabled = true;
		private boolean searchOnFocusLost = true;
		private boolean selectionToolTip = false;
		private SearchIndicator searchIndicator = SEARCH_INDICATOR.getOrThrow();
		private int searchProgressBarDelay = SEARCH_PROGRESS_BAR_DELAY.getOrThrow();
		private Function<EntitySearchField, Selector> selector = new ListSelectorFactory();
		private Function<Entity, String> formatter = DEFAULT_FORMATTER;
		private @Nullable Supplier<EntityEditPanel> editPanel;
		private boolean confirmAdd;
		private boolean confirmEdit;

		private DefaultBuilder(EntitySearchModel searchModel) {
			this.searchModel = searchModel;
		}

		@Override
		public Builder columns(int columns) {
			this.columns = columns;
			return this;
		}

		@Override
		public Builder upperCase(boolean upperCase) {
			if (upperCase && lowerCase) {
				throw new IllegalArgumentException("Field is already lowercase");
			}
			this.upperCase = upperCase;
			return this;
		}

		@Override
		public Builder lowerCase(boolean lowerCase) {
			if (lowerCase && upperCase) {
				throw new IllegalArgumentException("Field is already uppercase");
			}
			this.lowerCase = lowerCase;
			return this;
		}

		@Override
		public Builder editable(boolean editable) {
			this.editable = editable;
			return this;
		}

		@Override
		public Builder formatter(Function<Entity, String> formatter) {
			this.formatter = requireNonNull(formatter);
			return this;
		}

		@Override
		public Builder searchHintEnabled(boolean searchHintEnabled) {
			this.searchHintEnabled = searchHintEnabled;
			return this;
		}

		@Override
		public Builder selectionToolTip(boolean selectionToolTip) {
			this.selectionToolTip = selectionToolTip;
			return this;
		}

		@Override
		public Builder searchOnFocusLost(boolean searchOnFocusLost) {
			this.searchOnFocusLost = searchOnFocusLost;
			return this;
		}

		@Override
		public Builder searchIndicator(SearchIndicator searchIndicator) {
			this.searchIndicator = requireNonNull(searchIndicator);
			return this;
		}

		@Override
		public Builder searchProgressBarDelay(int searchProgressBarDelay) {
			this.searchProgressBarDelay = searchProgressBarDelay;
			return this;
		}

		@Override
		public Builder selector(Function<EntitySearchField, Selector> selector) {
			this.selector = requireNonNull(selector);
			return this;
		}

		@Override
		public Builder editPanel(Supplier<EntityEditPanel> editPanel) {
			this.editPanel = requireNonNull(editPanel);
			return this;
		}

		@Override
		public Builder keyStroke(ControlKey<?> controlKey, @Nullable KeyStroke keyStroke) {
			controlMap.keyStroke(controlKey).set(keyStroke);
			return this;
		}

		@Override
		public Builder limit(int limit) {
			this.searchModel.limit().set(limit);
			return this;
		}

		@Override
		public Builder confirmAdd(boolean confirmAdd) {
			this.confirmAdd = confirmAdd;
			return this;
		}

		@Override
		public Builder confirmEdit(boolean confirmEdit) {
			this.confirmEdit = confirmEdit;
			return this;
		}

		@Override
		protected EntitySearchField createComponent() {
			return new EntitySearchField(this);
		}

		@Override
		protected ComponentValue<EntitySearchField, Entity> createValue(EntitySearchField component) {
			return new SelectionValue(component);
		}

		private static final class ListSelectorFactory implements Function<EntitySearchField, Selector> {

			@Override
			public Selector apply(EntitySearchField searchField) {
				return new DefaultListSelector(searchField);
			}
		}
	}
}
