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

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;

/**
 * A builder for JTree.
 */
public interface TreeBuilder extends ComponentBuilder<JTree, TreeBuilder> {

	/**
	 * @param rootVisible true if the root node should be visible
	 * @return this builder instance
	 * @see JTree#setRootVisible(boolean)
	 */
	TreeBuilder rootVisible(boolean rootVisible);

	/**
	 * @param showsRootHandles true if root handles should be shown
	 * @return this builder instance
	 * @see JTree#setShowsRootHandles(boolean)
	 */
	TreeBuilder showsRootHandles(boolean showsRootHandles);

	/**
	 * @param cellRenderer the cell renderer
	 * @return this builder instance
	 * @see JTree#setCellRenderer(TreeCellRenderer)
	 */
	TreeBuilder cellRenderer(TreeCellRenderer cellRenderer);

	/**
	 * @param dragEnabled true if drag should be enabled
	 * @return this builder instance
	 * @see JTree#setDragEnabled(boolean)
	 */
	TreeBuilder dragEnabled(boolean dragEnabled);

	/**
	 * @param dropMode the drop mode
	 * @return this builder instance
	 * @see JTree#setDropMode(DropMode)
	 */
	TreeBuilder dropMode(DropMode dropMode);

	/**
	 * @param editable true if the tree should be editable
	 * @return this builder instance
	 * @see JTree#setEditable(boolean)
	 */
	TreeBuilder editable(boolean editable);

	/**
	 * @param expandsSelectedPaths true if selected paths should be expanded
	 * @return this builder instance
	 * @see JTree#setExpandsSelectedPaths(boolean)
	 */
	TreeBuilder expandsSelectedPaths(boolean expandsSelectedPaths);

	/**
	 * @param invokesStopCellEditing true if stop cell editing should be invoked on selection change
	 * @return this builder instance
	 * @see JTree#setInvokesStopCellEditing(boolean)
	 */
	TreeBuilder invokesStopCellEditing(boolean invokesStopCellEditing);

	/**
	 * @param rowHeight the row height
	 * @return this builder instance
	 * @see JTree#setRowHeight(int)
	 */
	TreeBuilder rowHeight(int rowHeight);

	/**
	 * @param scrollsOnExpand true if the tree should scroll on expand
	 * @return this builder instance
	 * @see JTree#setScrollsOnExpand(boolean)
	 */
	TreeBuilder scrollsOnExpand(boolean scrollsOnExpand);

	/**
	 * @param toggleClickCount the number of clicks required to toggle a node
	 * @return this builder instance
	 * @see JTree#setToggleClickCount(int)
	 */
	TreeBuilder toggleClickCount(int toggleClickCount);

	/**
	 * @param visibleRowCount the number of visible rows
	 * @return this builder instance
	 * @see JTree#setVisibleRowCount(int)
	 */
	TreeBuilder visibleRowCount(int visibleRowCount);

	/**
	 * @param largeModel the large model value
	 * @return this builder instance
	 * @see JTree#setLargeModel(boolean)
	 */
	TreeBuilder largeModel(boolean largeModel);

	/**
	 * @param treeExpansionListener the tree expansion listener to add
	 * @return this builder instance
	 * @see JTree#addTreeExpansionListener(TreeExpansionListener)
	 */
	TreeBuilder treeExpansionListener(TreeExpansionListener treeExpansionListener);

	/**
	 * @param treeWillExpandListener the tree will expand listener to add
	 * @return this builder instance
	 * @see JTree#addTreeWillExpandListener(TreeWillExpandListener)
	 */
	TreeBuilder treeWillExpandListener(TreeWillExpandListener treeWillExpandListener);

	/**
	 * @param treeSelectionListener the tree selection listener to add
	 * @return this builder instance
	 * @see JTree#addTreeSelectionListener(TreeSelectionListener)
	 */
	TreeBuilder treeSelectionListener(TreeSelectionListener treeSelectionListener);

	/**
	 * Provides a {@link TreeBuilder}.
	 */
	interface ModelStep {

		/**
		 * @param treeModel the tree model
		 * @return a {@link TreeBuilder}
		 */
		TreeBuilder model(TreeModel treeModel);
	}

	/**
	 * @return a builder for a component
	 */
	static ModelStep builder() {
		return DefaultTreeBuilder.MODEL_STEP;
	}
}
