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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.tree;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultTreeBuilder extends AbstractComponentBuilder<JTree, TreeBuilder> implements TreeBuilder {

	static final DefaultModelStep MODEL_STEP = new DefaultModelStep();

	private final TreeModel treeModel;

	private final List<TreeExpansionListener> treeExpansionListeners = new ArrayList<>();
	private final List<TreeWillExpandListener> treeWillExpandListeners = new ArrayList<>();
	private final List<TreeSelectionListener> treeSelectionListeners = new ArrayList<>();

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

	private DefaultTreeBuilder(TreeModel treeModel) {
		this.treeModel = requireNonNull(treeModel);
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
		JTree tree = new JTree(treeModel);
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
		treeExpansionListeners.forEach(tree::addTreeExpansionListener);
		treeWillExpandListeners.forEach(tree::addTreeWillExpandListener);
		treeSelectionListeners.forEach(tree::addTreeSelectionListener);

		return tree;
	}
}
