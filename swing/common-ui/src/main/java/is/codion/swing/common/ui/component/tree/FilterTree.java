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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.tree;

import is.codion.common.model.component.tree.NodePath;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.swing.common.ui.component.tree.FilterTree.ControlKeys.*;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.event.ActionEvent.ACTION_PERFORMED;
import static java.awt.event.KeyEvent.*;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.isLeftMouseButton;

/**
 * <p>A {@link JTree} based on a {@link SwingFilterTreeModel}, its expansion kept in step with the expansion of the
 * model, which owns it, see {@link SwingFilterTreeModel#expansion()}.
 * <p>Expanding or collapsing a node in the tree, by mouse, keyboard or {@link #expandPath(TreePath)}, expands or
 * collapses it in the model, and the tree follows the expansion of the model, so the rows of the tree are the
 * visible nodes of the model, see {@link SwingFilterTreeModel#visible()}, and the selection of the tree is the
 * selection of the model, its {@link SwingFilterTreeModel#selection()} being the selection model of the tree.
 * <p>The root is hidden, the root handles shown, and the nodes rendered via {@link #convertValueToText(Object, boolean, boolean, boolean, int, boolean)},
 * formatting the item of each node, see {@link Builder#formatter(Function)}.
 * <p>The tree selects the row under the mouse before a popup menu is shown, unless it is already selected,
 * clearing the selection below the last row.
 * @param <T> the item type
 * @see #builder()
 */
public final class FilterTree<T> extends JTree {

	/**
	 * The controls.
	 * <p>Note: CTRL in key stroke descriptions represents the platform menu shortcut key (CTRL on Windows/Linux, ⌘ on macOS).
	 */
	public static final class ControlKeys {

		/**
		 * Expands the selected nodes along with their loaded descendants.<br>
		 * Default key stroke: CTRL-ADD
		 */
		public static final ControlKey<CommandControl> EXPAND = CommandControl.key("expand", keyStroke(VK_ADD, MENU_SHORTCUT_MASK));
		/**
		 * Collapses the selected nodes along with their descendants.<br>
		 * Default key stroke: CTRL-SUBTRACT
		 */
		public static final ControlKey<CommandControl> COLLAPSE = CommandControl.key("collapse", keyStroke(VK_SUBTRACT, MENU_SHORTCUT_MASK));
		/**
		 * Refreshes the children of the selected nodes, along with their loaded descendants,
		 * the whole tree in case of no selection.<br>
		 * Default key stroke: F5
		 */
		public static final ControlKey<CommandControl> REFRESH = CommandControl.key("refresh", keyStroke(VK_F5));

		private ControlKeys() {}
	}

	private final SwingFilterTreeModel<T> treeModel;
	private final Function<T, String> formatter;
	private final Value<Action> doubleClick;
	private final Event<MouseEvent> doubleClicked = Event.event();
	private final State scrollToSelected;
	private final ScrollTo scrollTo = new ScrollTo();
	private final TreeModelListener modelReconciler = new ModelReconciler();

	//the path the tree is expanding or collapsing, which the model has been told of
	private @Nullable TreePath toggling;
	//true while the expansion of the model is being applied to the tree
	private boolean applying = false;

	private FilterTree(DefaultBuilder<T> builder) {
		super(builder.treeModel);
		this.treeModel = builder.treeModel;
		this.formatter = builder.formatter;
		this.doubleClick = Value.nullable(builder.doubleClick);
		this.scrollToSelected = State.state(builder.scrollToSelected);
		super.setSelectionModel(treeModel.selection());
		setRootVisible(false);
		setShowsRootHandles(true);
		addTreeWillExpandListener(new ExpandModel());
		addTreeExpansionListener(new ExpansionReconciler());
		//after the listeners of the tree and its UI, see setUI()
		treeModel.addTreeModelListener(modelReconciler);
		addMouseListener(new DoubleClickListener());
		treeModel.visible().addListener(this::reconcile);
		//a node not loaded is collapsed without the visible nodes changing, one failing to load for example
		treeModel.expansion().collapsed().addConsumer(path -> reconcile(path, false));
		ScrollToSelected scroll = new ScrollToSelected();
		treeModel.selection().items().addConsumer(scroll);
		treeModel.sort().observer().addListener(scroll::scrollLater);
		ControlMap controlMap = builder.controlMap;
		controlMap.control(EXPAND).set(Control.command(this::expandSelected));
		controlMap.control(COLLAPSE).set(Control.command(this::collapseSelected));
		controlMap.control(REFRESH).set(Control.command(this::refreshSelected));
		controlMap.keyEvent(EXPAND).ifPresent(keyEvent -> keyEvent.enable(this));
		controlMap.keyEvent(COLLAPSE).ifPresent(keyEvent -> keyEvent.enable(this));
		controlMap.keyEvent(REFRESH).ifPresent(keyEvent -> keyEvent.enable(this));
		reconcile();
	}

	/**
	 * @return the tree model
	 */
	public SwingFilterTreeModel<T> model() {
		return treeModel;
	}

	@Override
	public SwingFilterTreeModel<T> getModel() {
		return (SwingFilterTreeModel<T>) super.getModel();
	}

	/**
	 * The model is set when the tree is created and can not be replaced.
	 * @param model the model
	 * @throws IllegalStateException in case the model has already been set
	 * @throws IllegalArgumentException in case the model is not a {@link SwingFilterTreeModel}
	 */
	@Override
	public void setModel(@Nullable TreeModel model) {
		if (getModel() != null) {
			throw new IllegalStateException("Tree model has already been set");
		}
		if (!(model instanceof SwingFilterTreeModel)) {
			throw new IllegalArgumentException("FilterTree model must be a SwingFilterTreeModel instance");
		}
		super.setModel(model);
	}

	/**
	 * The selection model is the selection of the tree model, see {@link SwingFilterTreeModel#selection()}, and can
	 * not be replaced.
	 * @param selectionModel the selection model
	 * @throws IllegalStateException always, the selection model having already been set
	 */
	@Override
	public void setSelectionModel(@Nullable TreeSelectionModel selectionModel) {
		throw new IllegalStateException("Selection model has already been set");
	}

	@Override
	public void setUI(TreeUI ui) {
		super.setUI(ui);
		//null when called during construction
		if (treeModel != null && modelReconciler != null) {
			//the new UI adds its model listener, which must be notified before this
			//one, the model notifying its listeners in the order they were added
			treeModel.removeTreeModelListener(modelReconciler);
			treeModel.addTreeModelListener(modelReconciler);
		}
	}

	/**
	 * Updates the cell renderer before the UI, which measures the nodes with it when installed.
	 */
	@Override
	public void updateUI() {
		TreeCellRenderer renderer = getCellRenderer();
		if (renderer instanceof JComponent) {
			Utilities.updateUI((JComponent) renderer);
		}
		super.updateUI();
	}

	/**
	 * Formats the item of the given node, see {@link Builder#formatter(Function)}.
	 * @param value the node, a {@link NodePath}
	 * @param selected true if the node is selected
	 * @param expanded true if the node is expanded
	 * @param leaf true if the node is a leaf
	 * @param row the row
	 * @param hasFocus true if the node has the focus
	 * @return the formatted item of the node, an empty string for the root
	 */
	@Override
	public String convertValueToText(@Nullable Object value, boolean selected, boolean expanded,
																	 boolean leaf, int row, boolean hasFocus) {
		if (value instanceof NodePath && formatter != null) {
			NodePath<T> path = (NodePath<T>) value;

			return path.root() ? "" : formatter.apply(path.item());
		}

		return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
	}

	/**
	 * Selects the row under the mouse before the popup menu is shown, so that the popup menu actions apply to what
	 * was clicked, where a {@link JTree} leaves the selection as is: the row, unless already selected, and nothing
	 * below the last row, clearing the selection.
	 * @param event the mouse event triggering the popup menu, null when triggered via the keyboard
	 * @return the popup menu location, null for the default one
	 */
	@Override
	public @Nullable Point getPopupLocation(@Nullable MouseEvent event) {
		if (event != null) {
			int row = DefaultTreeBuilder.row(this, event);
			if (row < 0) {
				clearSelection();
			}
			else if (!isRowSelected(row)) {
				setSelectionRow(row);
			}
		}

		return super.getPopupLocation(event);
	}

	/**
	 * <p>The {@link Action} is performed on a double-click with the left mouse button on a selected node, the first
	 * click selecting it, if it is enabled.
	 * <p>The {@link ActionEvent} source is the {@link MouseEvent}.
	 * @return the {@link Value} controlling the action to perform when a node is double-clicked
	 */
	public Value<Action> doubleClick() {
		return doubleClick;
	}

	/**
	 * @return an observer notified when a node is double-clicked with the left mouse button
	 */
	public Observer<MouseEvent> doubleClicked() {
		return doubleClicked.observer();
	}

	/**
	 * @return the {@link ScrollTo} instance providing scrolling actions
	 */
	public ScrollTo scrollTo() {
		return scrollTo;
	}

	/**
	 * @return a {@link Builder.ModelStep} instance
	 */
	public static Builder.ModelStep builder() {
		return DefaultBuilder.MODEL;
	}

	/**
	 * <p>Applies the expansion of the model to the visible nodes of the tree, skipping the path the tree is in the
	 * process of expanding or collapsing, which is reconciled once it is done.
	 * <p>Compares the expansion of the model with the expansion the tree shows, rather than with
	 * {@link #isExpanded(TreePath)}, which disagrees with what is shown for a node expanded while a leaf, a lazily
	 * loaded node found to have no children for example: the tree marks it expanded, while its layout leaves a leaf
	 * collapsed, and when children arrive they are not shown.
	 */
	private void reconcile() {
		reconcile(treeModel.getRoot(), false);
	}

	/**
	 * Reconciles the visible nodes at or below the given one, see {@link #reconcile()}.
	 * @param parent the path of the node
	 * @param collapseOnly true if only the nodes shown expanded which the model has collapsed should be collapsed
	 */
	private void reconcile(NodePath<T> parent, boolean collapseOnly) {
		if (applying) {
			return;
		}
		List<NodePath<T>> visible = treeModel.visible().get();
		//depth first, the descendants of a node following it
		int start = parent.root() ? 0 : treeModel.visible().indexOf(parent);
		if (start < 0) {
			return;
		}
		applying = true;
		try {
			for (int i = start; i < visible.size() && parent.contains(visible.get(i)); i++) {
				NodePath<T> path = visible.get(i);
				TreePath treePath = treeModel.treePath(path);
				if (toggling == null || !toggling.isDescendant(treePath)) {
					if (treeModel.nodes().leaf(path)) {
						if (isExpanded(treePath)) {
							//marked expanded while a leaf, see above, which the tree does not show
							removeDescendantToggledPaths(enumeration(singletonList(treePath)));
						}
					}
					else {
						reconcile(path, treePath, collapseOnly);
					}
				}
			}
		}
		finally {
			applying = false;
		}
	}

	/**
	 * Reconciles the expansion the model wants, the expansion the tree shows and the expansion the tree is marked with.
	 */
	private void reconcile(NodePath<T> path, TreePath treePath, boolean collapseOnly) {
		boolean expanded = treeModel.expansion().expanded(path);
		boolean shown = shownExpanded(path, treePath);
		boolean marked = isExpanded(treePath);
		if (expanded) {
			if (!collapseOnly && (!shown || !marked)) {
				expandPath(treePath);
			}
		}
		else if (shown || marked) {
			if (!marked) {
				//shown expanded but not marked as such, marked so it can be collapsed
				expandPath(treePath);
			}
			collapsePath(treePath);
		}
	}

	/**
	 * @return true if the tree shows the given node as expanded, its first child having a row
	 */
	private boolean shownExpanded(NodePath<T> path, TreePath treePath) {
		List<NodePath<T>> children = treeModel.nodes().children(path);
		if (children.isEmpty()) {
			//not loaded
			return isExpanded(treePath);
		}

		return getRowForPath(treeModel.treePath(children.get(0))) >= 0;
	}

	private void expandSelected() {
		Set<NodePath<T>> expanded = new LinkedHashSet<>(treeModel.expansion().get());
		for (NodePath<T> path : treeModel.selection().items().get()) {
			if (!treeModel.nodes().leaf(path)) {
				expanded.add(path);
				addLoadedDescendants(path, expanded);
			}
		}
		treeModel.expansion().set(expanded);
	}

	private void addLoadedDescendants(NodePath<T> path, Collection<NodePath<T>> paths) {
		for (NodePath<T> child : treeModel.nodes().children(path)) {
			if (treeModel.nodes().loaded(child) && !treeModel.nodes().leaf(child)) {
				paths.add(child);
				addLoadedDescendants(child, paths);
			}
		}
	}

	private void collapseSelected() {
		List<NodePath<T>> selected = treeModel.selection().items().get();
		List<NodePath<T>> expanded = new ArrayList<>(treeModel.expansion().get());
		expanded.removeIf(path -> selected.stream().anyMatch(selectedPath -> selectedPath.contains(path)));
		treeModel.expansion().set(expanded);
	}

	private void refreshSelected() {
		List<NodePath<T>> selected = treeModel.selection().items().get();
		if (selected.isEmpty()) {
			treeModel.nodes().refresh();
		}
		else {
			selected.forEach(treeModel.nodes()::refresh);
		}
	}

	/**
	 * Provides scrolling actions
	 */
	public final class ScrollTo {

		private ScrollTo() {}

		/**
		 * @return the {@link State} controlling whether the tree scrolls to the selected nodes when the selection
		 * changes or the nodes are sorted, in case none of them is visible
		 */
		public State selected() {
			return scrollToSelected;
		}

		/**
		 * Scrolls to the node identified by the given path, expanding its ancestors if required.
		 * Has no effect in case the node is not in the model or not included.
		 * @param path the path of the node to scroll to
		 * @see JTree#scrollPathToVisible(TreePath)
		 */
		public void path(NodePath<T> path) {
			if (treeModel.nodes().included(requireNonNull(path))) {
				scrollPathToVisible(treeModel.treePath(path));
			}
		}
	}

	/**
	 * Expands and collapses the nodes of the model as the tree does.
	 */
	private final class ExpandModel implements TreeWillExpandListener {

		@Override
		public void treeWillExpand(TreeExpansionEvent event) {
			toggle(event.getPath(), true);
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent event) {
			toggle(event.getPath(), false);
		}

		private void toggle(TreePath treePath, boolean expand) {
			if (applying) {
				return;
			}
			TreePath previous = toggling;
			toggling = treePath;
			try {
				NodePath<T> path = (NodePath<T>) treePath.getLastPathComponent();
				if (expand) {
					treeModel.expansion().expand(path);
				}
				else {
					treeModel.expansion().collapse(path);
				}
			}
			catch (RuntimeException e) {
				//the tree does not toggle the path, while the model may have, a selection listener vetoing
				//the selection change following a collapse for example, in which case the tree follows the model
				toggling = previous;
				reconcile();
				throw e;
			}
			finally {
				toggling = previous;
			}
		}
	}

	/**
	 * <p>Collapses the nodes the tree shows expanded at or below a node changed in the model, which the model has
	 * collapsed, once the tree and its UI have processed the change. A change may leave the expansion shown out of
	 * step with the model without changing the visible nodes, such as children inserted below a node the layout of
	 * a large model tree left expanded when the node became a leaf.
	 * <p>Only collapses, since the model notifies its listeners of a mutation already made: a node expanded now would
	 * be populated with children the tree is yet to be notified of, and then again when it is. The nodes to expand are
	 * expanded once the mutation is done, when the visible nodes are notified.
	 * <p>Must be notified after the model listeners of the tree and its UI, see {@link #setUI(TreeUI)}.
	 */
	private final class ModelReconciler implements TreeModelListener {

		@Override
		public void treeNodesChanged(TreeModelEvent event) {
			reconcile(event);
		}

		@Override
		public void treeNodesInserted(TreeModelEvent event) {
			reconcile(event);
		}

		@Override
		public void treeNodesRemoved(TreeModelEvent event) {
			reconcile(event);
		}

		@Override
		public void treeStructureChanged(TreeModelEvent event) {
			reconcile(event);
		}

		private void reconcile(TreeModelEvent event) {
			FilterTree.this.reconcile((NodePath<T>) event.getTreePath().getLastPathComponent(), true);
		}
	}

	/**
	 * Reconciles the expansion of the tree with the model, once the tree is done expanding or collapsing a node,
	 * expanding the nodes below it which the model has expanded.
	 */
	private final class ExpansionReconciler implements TreeExpansionListener {

		@Override
		public void treeExpanded(TreeExpansionEvent event) {
			reconcile();
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent event) {
			reconcile();
		}
	}

	private final class DoubleClickListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent event) {
			if (event.getClickCount() == 2 && isLeftMouseButton(event)) {
				int row = DefaultTreeBuilder.row(FilterTree.this, event);
				if (row >= 0) {
					if (isRowSelected(row)) {
						doubleClick.optional()
										.filter(Action::isEnabled)
										.ifPresent(action -> action.actionPerformed(new ActionEvent(event, ACTION_PERFORMED, "doubleClick")));
					}
					doubleClicked.accept(event);
				}
			}
		}
	}

	/**
	 * Scrolls to the selected nodes when the selection changes or the nodes are sorted, not when the rows of the
	 * selected nodes change, as they do when a node above them is expanded, which would scroll that node out of view.
	 */
	private final class ScrollToSelected implements Consumer<List<NodePath<T>>> {

		//the selection also notifies when the item instances of the selected paths are replaced
		private List<NodePath<T>> selected = emptyList();

		@Override
		public void accept(List<NodePath<T>> paths) {
			if (!paths.equals(selected)) {
				selected = paths;
				scrollLater();
			}
		}

		private void scrollLater() {
			if (scrollToSelected.is() && !selected.isEmpty()) {
				Ancestor.ofType(JViewport.class).of(FilterTree.this).optional()
								//once the tree has been reconciled with the model, the rows of which these are
								.ifPresent(viewport -> SwingUtilities.invokeLater(this::scroll));
			}
		}

		private void scroll() {
			int[] rows = getSelectionRows();
			if (rows != null && rows.length > 0) {
				Rectangle visibleRect = getVisibleRect();
				for (int row : rows) {
					Rectangle bounds = getRowBounds(row);
					if (bounds != null && visibleRect.intersects(bounds)) {
						return;
					}
				}
				scrollRowToVisible(rows[0]);
			}
		}
	}

	/**
	 * Builds a {@link FilterTree}.
	 * @param <T> the item type
	 */
	public interface Builder<T> extends ComponentBuilder<FilterTree<T>, Builder<T>> {

		/**
		 * Provides a {@link Builder}
		 */
		interface ModelStep {

			/**
			 * @param model the tree model
			 * @param <T> the item type
			 * @return a {@link Builder}
			 */
			<T> Builder<T> model(SwingFilterTreeModel<T> model);
		}

		/**
		 * Formats the items for display, {@link String#valueOf(Object)} by default.
		 * @param formatter the formatter
		 * @return this builder instance
		 * @see FilterTree#convertValueToText(Object, boolean, boolean, boolean, int, boolean)
		 */
		Builder<T> formatter(Function<T, String> formatter);

		/**
		 * Provides the icon of each node, the look and feel icons used when the function returns null.
		 * Ignored in case a {@link #cellRenderer(TreeCellRenderer)} is specified.
		 * @param icon provides the icon for a given item
		 * @return this builder instance
		 */
		Builder<T> icon(Function<T, @Nullable Icon> icon);

		/**
		 * @param cellRenderer the cell renderer, its value a {@link NodePath}
		 * @return this builder instance
		 * @see JTree#setCellRenderer(TreeCellRenderer)
		 */
		Builder<T> cellRenderer(TreeCellRenderer cellRenderer);

		/**
		 * Sets the UI before any other setting, see {@link TreeBuilder#ui(TreeUI)}.
		 * @param ui the tree UI
		 * @return this builder instance
		 * @see JTree#setUI(TreeUI)
		 */
		Builder<T> ui(TreeUI ui);

		/**
		 * @param dragEnabled true if drag should be enabled
		 * @return this builder instance
		 * @see JTree#setDragEnabled(boolean)
		 */
		Builder<T> dragEnabled(boolean dragEnabled);

		/**
		 * @param dropMode the drop mode
		 * @return this builder instance
		 * @see JTree#setDropMode(DropMode)
		 */
		Builder<T> dropMode(DropMode dropMode);

		/**
		 * @param rowHeight the row height
		 * @return this builder instance
		 * @see JTree#setRowHeight(int)
		 */
		Builder<T> rowHeight(int rowHeight);

		/**
		 * @param scrollsOnExpand true if the tree should scroll on expand
		 * @return this builder instance
		 * @see JTree#setScrollsOnExpand(boolean)
		 */
		Builder<T> scrollsOnExpand(boolean scrollsOnExpand);

		/**
		 * @param toggleClickCount the number of clicks required to expand or collapse a node
		 * @return this builder instance
		 * @see JTree#setToggleClickCount(int)
		 */
		Builder<T> toggleClickCount(int toggleClickCount);

		/**
		 * @param visibleRowCount the number of visible rows
		 * @return this builder instance
		 * @see JTree#setVisibleRowCount(int)
		 */
		Builder<T> visibleRowCount(int visibleRowCount);

		/**
		 * @param largeModel the large model value
		 * @return this builder instance
		 * @see JTree#setLargeModel(boolean)
		 */
		Builder<T> largeModel(boolean largeModel);

		/**
		 * Sets the selection mode of the model selection.
		 * @param selectionMode the selection mode
		 * @return this builder instance
		 * @see TreeSelectionModel#setSelectionMode(int)
		 */
		Builder<T> selectionMode(int selectionMode);

		/**
		 * @param doubleClick the action to perform on a double-click on a selected node
		 * @return this builder instance
		 * @see FilterTree#doubleClick()
		 */
		Builder<T> doubleClick(Action doubleClick);

		/**
		 * @param scrollToSelected true if the tree should scroll to the selected nodes when the selection changes, default true
		 * @return this builder instance
		 * @see FilterTree#scrollTo()
		 */
		Builder<T> scrollToSelected(boolean scrollToSelected);

		/**
		 * @param controlKey the control key
		 * @param keyStroke the key stroke to assign to the given control
		 * @return this builder instance
		 * @see ControlKeys
		 */
		Builder<T> keyStroke(ControlKey<?> controlKey, @Nullable KeyStroke keyStroke);

		/**
		 * @param treeExpansionListener the tree expansion listener to add
		 * @return this builder instance
		 * @see JTree#addTreeExpansionListener(TreeExpansionListener)
		 */
		Builder<T> treeExpansionListener(TreeExpansionListener treeExpansionListener);

		/**
		 * Note that vetoing an expansion made via the tree keeps the model from expanding as well, the listeners added
		 * being notified before the one expanding the model. An expansion made via the model can not be vetoed, a veto
		 * leaving the tree out of step with the model.
		 * @param treeWillExpandListener the tree will expand listener to add
		 * @return this builder instance
		 * @see JTree#addTreeWillExpandListener(TreeWillExpandListener)
		 */
		Builder<T> treeWillExpandListener(TreeWillExpandListener treeWillExpandListener);

		/**
		 * @param treeSelectionListener the tree selection listener to add
		 * @return this builder instance
		 * @see JTree#addTreeSelectionListener(TreeSelectionListener)
		 */
		Builder<T> treeSelectionListener(TreeSelectionListener treeSelectionListener);
	}

	private static final class IconRenderer<T> extends DefaultTreeCellRenderer {

		private final Function<T, @Nullable Icon> icon;

		private IconRenderer(Function<T, @Nullable Icon> icon) {
			this.icon = icon;
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
																									boolean leaf, int row, boolean hasFocus) {
			Component component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			NodePath<T> path = (NodePath<T>) value;
			if (!path.root()) {
				Icon nodeIcon = icon.apply(path.item());
				if (nodeIcon != null) {
					setIcon(nodeIcon);
				}
			}

			return component;
		}
	}

	private static final class DefaultModelStep implements Builder.ModelStep {

		@Override
		public <T> Builder<T> model(SwingFilterTreeModel<T> model) {
			return new DefaultBuilder<>(requireNonNull(model));
		}
	}

	private static final class DefaultBuilder<T>
					extends AbstractComponentBuilder<FilterTree<T>, Builder<T>> implements Builder<T> {

		private static final Builder.ModelStep MODEL = new DefaultModelStep();

		private final SwingFilterTreeModel<T> treeModel;
		private final ControlMap controlMap = controlMap(ControlKeys.class);
		private final List<TreeExpansionListener> treeExpansionListeners = new ArrayList<>();
		private final List<TreeWillExpandListener> treeWillExpandListeners = new ArrayList<>();
		private final List<TreeSelectionListener> treeSelectionListeners = new ArrayList<>();

		private Function<T, String> formatter = String::valueOf;
		private @Nullable Function<T, @Nullable Icon> icon;
		private @Nullable TreeCellRenderer cellRenderer;
		private @Nullable TreeUI ui;
		private @Nullable Boolean dragEnabled;
		private @Nullable DropMode dropMode;
		private @Nullable Integer rowHeight;
		private @Nullable Boolean scrollsOnExpand;
		private @Nullable Integer toggleClickCount;
		private @Nullable Integer visibleRowCount;
		private @Nullable Boolean largeModel;
		private @Nullable Integer selectionMode;
		private @Nullable Action doubleClick;
		private boolean scrollToSelected = true;

		private DefaultBuilder(SwingFilterTreeModel<T> treeModel) {
			this.treeModel = treeModel;
		}

		@Override
		public Builder<T> formatter(Function<T, String> formatter) {
			this.formatter = requireNonNull(formatter);
			return this;
		}

		@Override
		public Builder<T> icon(Function<T, @Nullable Icon> icon) {
			this.icon = requireNonNull(icon);
			return this;
		}

		@Override
		public Builder<T> cellRenderer(TreeCellRenderer cellRenderer) {
			this.cellRenderer = requireNonNull(cellRenderer);
			return this;
		}

		@Override
		public Builder<T> ui(TreeUI ui) {
			this.ui = requireNonNull(ui);
			return this;
		}

		@Override
		public Builder<T> dragEnabled(boolean dragEnabled) {
			this.dragEnabled = dragEnabled;
			return this;
		}

		@Override
		public Builder<T> dropMode(DropMode dropMode) {
			this.dropMode = requireNonNull(dropMode);
			return this;
		}

		@Override
		public Builder<T> rowHeight(int rowHeight) {
			this.rowHeight = rowHeight;
			return this;
		}

		@Override
		public Builder<T> scrollsOnExpand(boolean scrollsOnExpand) {
			this.scrollsOnExpand = scrollsOnExpand;
			return this;
		}

		@Override
		public Builder<T> toggleClickCount(int toggleClickCount) {
			this.toggleClickCount = toggleClickCount;
			return this;
		}

		@Override
		public Builder<T> visibleRowCount(int visibleRowCount) {
			this.visibleRowCount = visibleRowCount;
			return this;
		}

		@Override
		public Builder<T> largeModel(boolean largeModel) {
			this.largeModel = largeModel;
			return this;
		}

		@Override
		public Builder<T> selectionMode(int selectionMode) {
			this.selectionMode = selectionMode;
			return this;
		}

		@Override
		public Builder<T> doubleClick(Action doubleClick) {
			this.doubleClick = requireNonNull(doubleClick);
			return this;
		}

		@Override
		public Builder<T> scrollToSelected(boolean scrollToSelected) {
			this.scrollToSelected = scrollToSelected;
			return this;
		}

		@Override
		public Builder<T> keyStroke(ControlKey<?> controlKey, @Nullable KeyStroke keyStroke) {
			controlMap.keyStroke(controlKey).set(keyStroke);
			return this;
		}

		@Override
		public Builder<T> treeExpansionListener(TreeExpansionListener treeExpansionListener) {
			treeExpansionListeners.add(requireNonNull(treeExpansionListener));
			return this;
		}

		@Override
		public Builder<T> treeWillExpandListener(TreeWillExpandListener treeWillExpandListener) {
			treeWillExpandListeners.add(requireNonNull(treeWillExpandListener));
			return this;
		}

		@Override
		public Builder<T> treeSelectionListener(TreeSelectionListener treeSelectionListener) {
			treeSelectionListeners.add(requireNonNull(treeSelectionListener));
			return this;
		}

		@Override
		protected FilterTree<T> createComponent() {
			FilterTree<T> tree = new FilterTree<>(this);
			if (ui != null) {
				tree.setUI(ui);
			}
			if (cellRenderer != null) {
				tree.setCellRenderer(cellRenderer);
			}
			else if (icon != null) {
				tree.setCellRenderer(new IconRenderer<>(icon));
			}
			if (dragEnabled != null) {
				tree.setDragEnabled(dragEnabled);
			}
			if (dropMode != null) {
				tree.setDropMode(dropMode);
			}
			if (rowHeight != null) {
				tree.setRowHeight(rowHeight);
			}
			if (scrollsOnExpand != null) {
				tree.setScrollsOnExpand(scrollsOnExpand);
			}
			if (toggleClickCount != null) {
				tree.setToggleClickCount(toggleClickCount);
			}
			if (visibleRowCount != null) {
				tree.setVisibleRowCount(visibleRowCount);
			}
			if (largeModel != null) {
				tree.setLargeModel(largeModel);
			}
			if (selectionMode != null) {
				tree.getSelectionModel().setSelectionMode(selectionMode);
			}
			treeExpansionListeners.forEach(tree::addTreeExpansionListener);
			treeWillExpandListeners.forEach(tree::addTreeWillExpandListener);
			treeSelectionListeners.forEach(tree::addTreeSelectionListener);

			return tree;
		}
	}
}
