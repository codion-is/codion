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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.tree;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.ActionEvent.ACTION_PERFORMED;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.isLeftMouseButton;

final class DefaultTreeBuilder extends AbstractComponentBuilder<JTree, TreeBuilder> implements TreeBuilder {

	static final DefaultModelStep MODEL_STEP = new DefaultModelStep();

	private final TreeModel treeModel;

	private final List<TreeExpansionListener> treeExpansionListeners = new ArrayList<>();
	private final List<TreeWillExpandListener> treeWillExpandListeners = new ArrayList<>();
	private final List<TreeSelectionListener> treeSelectionListeners = new ArrayList<>();

	private @Nullable TreeUI ui;
	private @Nullable Boolean rootVisible;
	private @Nullable Boolean showsRootHandles;
	private @Nullable TreeCellRenderer cellRenderer;
	private @Nullable Boolean dragEnabled;
	private @Nullable DropMode dropMode;
	private @Nullable Boolean editable;
	private @Nullable Boolean expandsSelectedPaths;
	private @Nullable Boolean invokesStopCellEditing;
	private @Nullable Integer rowHeight;
	private @Nullable Boolean scrollsOnExpand;
	private @Nullable Integer toggleClickCount;
	private @Nullable Integer visibleRowCount;
	private @Nullable Boolean largeModel;
	private @Nullable Integer selectionMode;
	private @Nullable Action doubleClick;

	private DefaultTreeBuilder(TreeModel treeModel) {
		this.treeModel = requireNonNull(treeModel);
	}

	@Override
	public TreeBuilder ui(TreeUI ui) {
		this.ui = requireNonNull(ui);
		return this;
	}

	@Override
	public TreeBuilder rootVisible(boolean rootVisible) {
		this.rootVisible = rootVisible;
		return this;
	}

	@Override
	public TreeBuilder showsRootHandles(boolean showsRootHandles) {
		this.showsRootHandles = showsRootHandles;
		return this;
	}

	@Override
	public TreeBuilder cellRenderer(TreeCellRenderer cellRenderer) {
		this.cellRenderer = requireNonNull(cellRenderer);
		return this;
	}

	@Override
	public TreeBuilder dragEnabled(boolean dragEnabled) {
		this.dragEnabled = dragEnabled;
		return this;
	}

	@Override
	public TreeBuilder dropMode(DropMode dropMode) {
		this.dropMode = requireNonNull(dropMode);
		return this;
	}

	@Override
	public TreeBuilder editable(boolean editable) {
		this.editable = editable;
		return this;
	}

	@Override
	public TreeBuilder expandsSelectedPaths(boolean expandsSelectedPaths) {
		this.expandsSelectedPaths = expandsSelectedPaths;
		return this;
	}

	@Override
	public TreeBuilder invokesStopCellEditing(boolean invokesStopCellEditing) {
		this.invokesStopCellEditing = invokesStopCellEditing;
		return this;
	}

	@Override
	public TreeBuilder rowHeight(int rowHeight) {
		this.rowHeight = rowHeight;
		return this;
	}

	@Override
	public TreeBuilder scrollsOnExpand(boolean scrollsOnExpand) {
		this.scrollsOnExpand = scrollsOnExpand;
		return this;
	}

	@Override
	public TreeBuilder toggleClickCount(int toggleClickCount) {
		this.toggleClickCount = toggleClickCount;
		return this;
	}

	@Override
	public TreeBuilder visibleRowCount(int visibleRowCount) {
		this.visibleRowCount = visibleRowCount;
		return this;
	}

	@Override
	public TreeBuilder largeModel(boolean largeModel) {
		this.largeModel = largeModel;
		return this;
	}

	@Override
	public TreeBuilder selectionMode(int selectionMode) {
		this.selectionMode = selectionMode;
		return this;
	}

	@Override
	public TreeBuilder doubleClick(Action doubleClick) {
		this.doubleClick = requireNonNull(doubleClick);
		return this;
	}

	@Override
	public TreeBuilder treeExpansionListener(TreeExpansionListener treeExpansionListener) {
		treeExpansionListeners.add(requireNonNull(treeExpansionListener));
		return this;
	}

	@Override
	public TreeBuilder treeWillExpandListener(TreeWillExpandListener treeWillExpandListener) {
		treeWillExpandListeners.add(requireNonNull(treeWillExpandListener));
		return this;
	}

	@Override
	public TreeBuilder treeSelectionListener(TreeSelectionListener treeSelectionListener) {
		treeSelectionListeners.add(requireNonNull(treeSelectionListener));
		return this;
	}

	private static final class DefaultModelStep implements ModelStep {

		@Override
		public TreeBuilder model(TreeModel treeModel) {
			return new DefaultTreeBuilder(treeModel);
		}
	}

	@Override
	protected JTree createComponent() {
		JTree tree = new PopupSelectionTree(treeModel);
		if (ui != null) {
			tree.setUI(ui);
		}
		if (rootVisible != null) {
			tree.setRootVisible(rootVisible);
		}
		if (showsRootHandles != null) {
			tree.setShowsRootHandles(showsRootHandles);
		}
		if (cellRenderer != null) {
			tree.setCellRenderer(cellRenderer);
		}
		if (dragEnabled != null) {
			tree.setDragEnabled(dragEnabled);
		}
		if (dropMode != null) {
			tree.setDropMode(dropMode);
		}
		if (editable != null) {
			tree.setEditable(editable);
		}
		if (expandsSelectedPaths != null) {
			tree.setExpandsSelectedPaths(expandsSelectedPaths);
		}
		if (invokesStopCellEditing != null) {
			tree.setInvokesStopCellEditing(invokesStopCellEditing);
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
		if (doubleClick != null) {
			tree.addMouseListener(new DoubleClickListener(tree, doubleClick));
		}
		treeExpansionListeners.forEach(tree::addTreeExpansionListener);
		treeWillExpandListeners.forEach(tree::addTreeWillExpandListener);
		treeSelectionListeners.forEach(tree::addTreeSelectionListener);

		return tree;
	}

	/**
	 * @param tree the tree
	 * @param event the mouse event
	 * @return the row under the mouse, -1 if none
	 */
	static int row(JTree tree, MouseEvent event) {
		int row = tree.getClosestRowForLocation(event.getX(), event.getY());
		if (row >= 0) {
			Rectangle bounds = tree.getRowBounds(row);
			if (bounds != null && event.getY() >= bounds.y && event.getY() < bounds.y + bounds.height) {
				return row;
			}
		}

		return -1;
	}

	private static final class PopupSelectionTree extends JTree {

		private PopupSelectionTree(TreeModel treeModel) {
			super(treeModel);
		}

		@Override
		public @Nullable Point getPopupLocation(@Nullable MouseEvent event) {
			if (event != null) {
				int row = row(this, event);
				if (row < 0) {
					clearSelection();
				}
				else if (!isRowSelected(row)) {
					setSelectionRow(row);
				}
			}

			return super.getPopupLocation(event);
		}
	}

	private static final class DoubleClickListener extends MouseAdapter {

		private final JTree tree;
		private final Action action;

		private DoubleClickListener(JTree tree, Action action) {
			this.tree = tree;
			this.action = action;
		}

		@Override
		public void mouseClicked(MouseEvent event) {
			if (event.getClickCount() == 2 && isLeftMouseButton(event) && action.isEnabled()) {
				int row = row(tree, event);
				if (row >= 0 && tree.isRowSelected(row)) {
					action.actionPerformed(new ActionEvent(event, ACTION_PERFORMED, "doubleClick"));
				}
			}
		}
	}
}
