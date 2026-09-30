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

import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.control.Control;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public final class TreeBuilderTest {

	@Test
	void ui() {
		AtomicReference<TreeCellRenderer> rendererWhenInstalled = new AtomicReference<>();
		BasicTreeUI ui = new BasicTreeUI() {
			@Override
			public void installUI(JComponent component) {
				rendererWhenInstalled.set(((JTree) component).getCellRenderer());
				super.installUI(component);
			}
		};
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		JTree tree = TreeBuilder.builder()
						.model(createModel())
						.cellRenderer(renderer)
						.ui(ui)
						.build();
		assertSame(ui, tree.getUI());
		assertSame(renderer, tree.getCellRenderer());
		// installed before the renderer was set
		assertNotSame(renderer, rendererWhenInstalled.get());
	}

	@Test
	void selectionMode() {
		JTree tree = TreeBuilder.builder()
						.model(createModel())
						.selectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION)
						.build();
		assertEquals(TreeSelectionModel.SINGLE_TREE_SELECTION, tree.getSelectionModel().getSelectionMode());
	}

	@Test
	void popupSelection() {
		JTree tree = createTree(TreeBuilder.builder().model(createModel()));
		tree.setSelectionRow(0);
		// the row under the mouse is selected
		assertNull(tree.getPopupLocation(popupTrigger(tree, 2)));
		assertArrayEquals(new int[] {2}, selectedRows(tree));
		// unless it is already part of the selection
		tree.setSelectionRows(new int[] {1, 2, 3});
		tree.getPopupLocation(popupTrigger(tree, 2));
		assertArrayEquals(new int[] {1, 2, 3}, selectedRows(tree));
		// the row, when right of the node
		tree.getPopupLocation(popupTrigger(tree, 4, tree.getWidth() - 5));
		assertArrayEquals(new int[] {4}, selectedRows(tree));
		// the selection is left as is when triggered via the keyboard
		tree.getPopupLocation(null);
		assertArrayEquals(new int[] {4}, selectedRows(tree));
		// and cleared below the last row
		tree.getPopupLocation(popupTrigger(tree, tree.getRowCount()));
		assertTrue(tree.isSelectionEmpty());
	}

	@Test
	void doubleClick() throws Exception {
		AtomicInteger performed = new AtomicInteger();
		State enabled = State.state(true);
		JTree tree = createTree(TreeBuilder.builder()
						.model(createModel())
						.doubleClick(Control.builder()
										.command(performed::incrementAndGet)
										.enabled(enabled)
										.build()));
		tree.setSelectionRow(1);
		// the left button only
		for (int button : new int[] {MouseEvent.BUTTON1, MouseEvent.BUTTON2, MouseEvent.BUTTON3}) {
			tree.dispatchEvent(doubleClick(tree, 1, button));
		}
		assertEquals(1, performed.get());
		// on a selected row only, such as right of the node, where a look and feel may not select
		tree.dispatchEvent(doubleClick(tree, 2, MouseEvent.BUTTON1));
		assertEquals(1, performed.get());
		// and only when enabled
		SwingUtilities.invokeAndWait(() -> enabled.set(false));
		tree.dispatchEvent(doubleClick(tree, 1, MouseEvent.BUTTON1));
		assertEquals(1, performed.get());
	}

	private static JTree createTree(TreeBuilder builder) {
		JTree tree = builder.build();
		tree.setSize(300, 200);

		return tree;
	}

	private static DefaultTreeModel createModel() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
		for (String child : new String[] {"one", "two", "three", "four"}) {
			root.add(new DefaultMutableTreeNode(child));
		}

		return new DefaultTreeModel(root);
	}

	private static int[] selectedRows(JTree tree) {
		return IntStream.range(0, tree.getRowCount())
						.filter(tree::isRowSelected)
						.toArray();
	}

	private static MouseEvent popupTrigger(JTree tree, int row) {
		return popupTrigger(tree, row, 5);
	}

	private static MouseEvent popupTrigger(JTree tree, int row, int x) {
		return new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON3_DOWN_MASK,
						x, y(tree, row), 1, true, MouseEvent.BUTTON3);
	}

	private static MouseEvent doubleClick(JTree tree, int row, int button) {
		return new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, 0, 0, 25, y(tree, row), 2, false, button);
	}

	private static int y(JTree tree, int row) {
		Rectangle lastRow = tree.getRowBounds(tree.getRowCount() - 1);

		return row < tree.getRowCount() ? tree.getRowBounds(row).y + 1 : lastRow.y + lastRow.height + 5;
	}
}
