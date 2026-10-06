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

import is.codion.common.model.CancelException;
import is.codion.common.model.component.tree.NodePath;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.naturalOrder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The tree:
 * <pre>
 * a
 *   a1
 *     a11
 *   a2
 * b
 * </pre>
 */
public final class FilterTreeTest {

	private final Map<String, List<String>> data = new HashMap<>();

	@BeforeEach
	void setUp() {
		data.put("", new ArrayList<>(asList("a", "b")));
		data.put("a", new ArrayList<>(asList("a1", "a2")));
		data.put("a1", new ArrayList<>(singletonList("a11")));
	}

	@Test
	void modelGuards() {
		SwingFilterTreeModel<String> model = model();
		FilterTree<String> tree = FilterTree.builder()
						.model(model)
						.build();
		assertSame(model, tree.model());
		assertSame(model, tree.getModel());
		assertSame(model.selection(), tree.getSelectionModel());
		assertFalse(tree.isRootVisible());
		assertTrue(tree.getShowsRootHandles());
		assertThrows(IllegalStateException.class, () -> tree.setModel(model));
		assertThrows(IllegalStateException.class, () -> tree.setModel(new DefaultTreeModel(null)));
		assertThrows(IllegalStateException.class, () -> tree.setSelectionModel(new DefaultTreeSelectionModel()));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		assertTrue(model.selection().singleSelection().is());
	}

	@Test
	void expansion() {
		SwingFilterTreeModel<String> model = model();
		model.nodes().refresh();
		//expanded before the tree exists
		model.expansion().expand(path("a", "a1"));
		FilterTree<String> tree = FilterTree.builder()
						.model(model)
						.build();
		assertInSync(tree);
		assertEquals(5, tree.getRowCount());
		//via the tree
		tree.collapsePath(model.treePath(path("a")));
		assertFalse(model.expansion().expanded(path("a")));
		assertInSync(tree);
		tree.expandPath(model.treePath(path("a")));
		//the remembered expansion below
		assertTrue(tree.isExpanded(model.treePath(path("a", "a1"))));
		assertInSync(tree);
		//via the model
		model.expansion().collapse(path("a", "a1"));
		assertFalse(tree.isExpanded(model.treePath(path("a", "a1"))));
		assertInSync(tree);
		model.expansion().set(emptyList());
		assertEquals(2, tree.getRowCount());
		assertInSync(tree);
		//a reorder restructures the root, losing the expansion of the tree, but not of the model
		model.expansion().set(asList(path("a"), path("a", "a1")));
		data.put("", asList("b", "a"));
		model.nodes().refresh();
		assertTrue(tree.isExpanded(model.treePath(path("a", "a1"))));
		assertInSync(tree);
	}

	@Test
	void selectionChangeVetoed() throws Exception {
		onEDT(() -> {
			SwingFilterTreeModel<String> model = model();
			model.nodes().refresh();
			FilterTree<String> tree = FilterTree.builder()
							.model(model)
							.build();
			model.expansion().expand(path("a"));
			model.selection().items().set(singletonList(path("a", "a2")));
			model.selection().item().addListener(() -> {
				throw new CancelException();
			});
			//collapsed via the tree, a listener vetoing the selection change following it, as an editor vetoing
			//the change of its entity does, which does not prevent the model from collapsing, so the tree follows
			assertThrows(CancelException.class, () -> tree.collapseRow(0));
			assertFalse(model.expansion().expanded(path("a")));
			assertFalse(tree.isExpanded(0));
			assertEquals(asList(path("a"), path("b")), model.visible().get());
			assertEquals(2, tree.getRowCount());
			assertEquals(path("b"), tree.getPathForRow(1).getLastPathComponent());
		});
	}

	@Test
	void expandedWhileLeaf() {
		//b, without children, expanded via the tree, marked expanded by the tree while a leaf
		SwingFilterTreeModel<String> model = model();
		model.nodes().refresh();
		FilterTree<String> tree = FilterTree.builder()
						.model(model)
						.build();
		tree.expandRow(1);
		assertTrue(model.expansion().expanded(path("b")));
		assertTrue(model.nodes().leaf(path("b")));
		//not marked expanded while a leaf
		assertFalse(tree.isExpanded(model.treePath(path("b"))));
		assertInSync(tree);
		//children arriving are shown
		model.nodes().add(path("b"), singletonList("b1"));
		assertEquals(3, tree.getRowCount());
		assertInSync(tree);
	}

	@Test
	void asyncExpandedWhileLeaf() throws Exception {
		//b, without children, expanded via the tree, marked expanded by the tree before
		//it was loaded and found to be a leaf, its children arriving later shown
		AtomicReference<FilterTree<String>> tree = new AtomicReference<>();
		CountDownLatch rootLoaded = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> {
			SwingFilterTreeModel<String> model = SwingFilterTreeModel.builder()
							.roots(() -> fetch(""))
							.children(path -> fetch(path.item()))
							.build();
			tree.set(FilterTree.builder()
							.model(model)
							.build());
			model.nodes().loader().active().addConsumer(active -> {
				if (!active) {
					rootLoaded.countDown();
				}
			});
			model.nodes().refresh();
		});
		assertTrue(rootLoaded.await(10, SECONDS));
		CountDownLatch loaded = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> {
			tree.get().model().nodes().loader().active().addConsumer(active -> {
				if (!active) {
					loaded.countDown();
				}
			});
			tree.get().expandRow(1);
		});
		assertTrue(loaded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			SwingFilterTreeModel<String> model = tree.get().model();
			assertTrue(model.nodes().leaf(path("b")));
			assertInSync(tree.get());
			model.nodes().add(path("b"), singletonList("b1"));
			assertEquals(3, tree.get().getRowCount());
			assertInSync(tree.get());
		});
	}

	@Test
	void text() {
		SwingFilterTreeModel<String> model = model();
		model.nodes().refresh();
		FilterTree<String> tree = FilterTree.builder()
						.model(model)
						.formatter(String::toUpperCase)
						.build();
		assertEquals("A", tree.convertValueToText(path("a"), false, false, false, 0, false));
		assertEquals("", tree.convertValueToText(nodePath(), false, false, false, 0, false));
		assertEquals("x", tree.convertValueToText("x", false, false, false, 0, false));
		JLabel label = (JLabel) tree.getCellRenderer().getTreeCellRendererComponent(tree, path("b"), false, false, true, 1, false);
		assertEquals("B", label.getText());

		Icon icon = new ImageIcon(new byte[0]);
		FilterTree<String> withIcons = FilterTree.builder()
						.model(model)
						.icon(item -> item.equals("a") ? icon : null)
						.build();
		label = (JLabel) withIcons.getCellRenderer().getTreeCellRendererComponent(withIcons, path("a"), false, false, false, 0, false);
		assertSame(icon, label.getIcon());
		label = (JLabel) withIcons.getCellRenderer().getTreeCellRendererComponent(withIcons, path("b"), false, false, true, 1, false);
		assertNotSame(icon, label.getIcon());
		withIcons.updateUI();
	}

	@Test
	void lookAndFeelSwitch() throws Exception {
		LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		try {
			UIManager.setLookAndFeel(new MetalLookAndFeel());
			FilterTree<String> customRenderer = customRendererTree();
			FilterTree<String> iconRenderer = iconRendererTree();
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
			customRenderer.updateUI();
			iconRenderer.updateUI();
			//the nodes measured with the renderer updated, as in a tree created with the look and feel
			assertEquals(customRendererTree().getRowBounds(0), customRenderer.getRowBounds(0));
			assertEquals(iconRendererTree().getRowBounds(0), iconRenderer.getRowBounds(0));
		}
		finally {
			UIManager.setLookAndFeel(lookAndFeel);
		}
	}

	@Test
	void controls() {
		SwingFilterTreeModel<String> model = model();
		model.nodes().refresh();
		FilterTree<String> tree = FilterTree.builder()
						.model(model)
						.build();
		//load a and a1, then collapse them
		model.expansion().expand(path("a", "a1"));
		model.expansion().set(emptyList());
		model.selection().items().set(singletonList(path("a")));
		perform(tree, FilterTree.ControlKeys.EXPAND);
		assertTrue(model.expansion().expanded(path("a", "a1")));
		assertInSync(tree);
		model.selection().items().set(singletonList(path("a")));
		perform(tree, FilterTree.ControlKeys.COLLAPSE);
		assertTrue(model.expansion().get().isEmpty());
		assertInSync(tree);

		data.put("", asList("a", "b", "c"));
		model.selection().clear();
		perform(tree, FilterTree.ControlKeys.REFRESH);
		assertEquals(3, tree.getRowCount());
		data.put("b", singletonList("b1"));
		model.selection().items().set(singletonList(path("b")));
		perform(tree, FilterTree.ControlKeys.REFRESH);
		assertTrue(model.nodes().loaded(path("b")));
		assertInSync(tree);
	}

	@Test
	void controlsLeaf() {
		List<String> loaded = new ArrayList<>();
		SwingFilterTreeModel<String> model = SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(path -> {
							loaded.add(path.item());
							return fetch(path.item());
						})
						.leaf(path -> !data.containsKey(path.item()))
						.build();
		model.nodes().loader().async().set(false);
		model.nodes().refresh();
		FilterTree<String> tree = FilterTree.builder()
						.model(model)
						.build();
		//a leaf is neither expanded nor loaded
		model.selection().items().set(singletonList(path("b")));
		perform(tree, FilterTree.ControlKeys.EXPAND);
		perform(tree, FilterTree.ControlKeys.REFRESH);
		assertTrue(model.expansion().get().isEmpty());
		assertTrue(loaded.isEmpty());
		assertInSync(tree);
	}

	@Test
	void doubleClick() {
		SwingFilterTreeModel<String> model = model();
		model.nodes().refresh();
		AtomicInteger performed = new AtomicInteger();
		List<MouseEvent> clicked = new ArrayList<>();
		FilterTree<String> tree = FilterTree.builder()
						.model(model)
						.doubleClick(Control.command(performed::incrementAndGet))
						.build();
		tree.doubleClicked().addConsumer(clicked::add);
		tree.setSize(200, 200);
		Rectangle bounds = tree.getRowBounds(1);
		MouseEvent click = new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
						0, bounds.x + 1, bounds.y + 1, 2, false, MouseEvent.BUTTON1);
		//not selected
		tree.dispatchEvent(click);
		assertEquals(0, performed.get());
		assertEquals(1, clicked.size());
		model.selection().items().set(singletonList(path("b")));
		tree.dispatchEvent(click);
		assertEquals(1, performed.get());
		tree.doubleClick().clear();
		tree.dispatchEvent(click);
		assertEquals(1, performed.get());
		assertEquals(3, clicked.size());
		//below the last row
		tree.dispatchEvent(new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
						0, 1, 190, 2, false, MouseEvent.BUTTON1));
		assertEquals(3, clicked.size());
	}

	@Test
	void scrollToSelected() throws Exception {
		List<String> roots = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			roots.add("r" + (10 + i));
		}
		List<String> children = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			children.add("c" + i);
		}
		data.put("", roots);
		data.put("r11", children);
		AtomicReference<FilterTree<String>> reference = new AtomicReference<>();
		AtomicReference<JScrollPane> scrollPaneReference = new AtomicReference<>();
		onEDT(() -> {
			SwingFilterTreeModel<String> model = SwingFilterTreeModel.builder()
							.roots(() -> fetch(""))
							.children(path -> fetch(path.item()))
							.comparator(naturalOrder())
							.build();
			model.nodes().loader().async().set(false);
			model.nodes().refresh();
			FilterTree<String> tree = FilterTree.builder()
							.model(model)
							.build();
			JScrollPane scrollPane = new JScrollPane(tree);
			scrollPane.setSize(200, 150);
			layout(scrollPane);
			model.selection().items().set(singletonList(path("r15")));
			reference.set(tree);
			scrollPaneReference.set(scrollPane);
		});
		FilterTree<String> tree = reference.get();
		SwingFilterTreeModel<String> model = tree.model();
		JScrollPane scrollPane = scrollPaneReference.get();
		//the scrolling is performed later
		onEDT(() -> {});
		onEDT(() -> {
			//the selected node in view
			assertEquals(0, scrollPane.getViewport().getViewPosition().y);
			//a node above it expanded, moving it out of view, the expanded node staying in view
			tree.expandRow(1);
			layout(scrollPane);
		});
		onEDT(() -> {});
		onEDT(() -> {
			assertEquals(45, tree.getSelectionRows()[0]);
			assertEquals(0, scrollPane.getViewport().getViewPosition().y);
			//fresh instances, the selection unchanged
			model.nodes().refresh();
			layout(scrollPane);
		});
		onEDT(() -> {});
		onEDT(() -> {
			assertEquals(0, scrollPane.getViewport().getViewPosition().y);
			//a node out of view selected
			model.selection().items().set(singletonList(path("r30")));
		});
		onEDT(() -> {});
		onEDT(() -> {
			assertTrue(tree.getVisibleRect().intersects(tree.getRowBounds(tree.getSelectionRows()[0])));
			//sorted, the selected node moving out of view
			model.sort().descending();
			layout(scrollPane);
		});
		onEDT(() -> {});
		onEDT(() -> {
			assertTrue(tree.getVisibleRect().intersects(tree.getRowBounds(tree.getSelectionRows()[0])));
			int position = scrollPane.getViewport().getViewPosition().y;
			tree.scrollTo().selected().set(false);
			model.selection().items().set(singletonList(path("r10")));
			layout(scrollPane);
			assertEquals(position, scrollPane.getViewport().getViewPosition().y);
		});
		onEDT(() -> {});
		onEDT(() -> {
			assertFalse(tree.getVisibleRect().intersects(tree.getRowBounds(tree.getSelectionRows()[0])));
			//scrolled to explicitly
			tree.scrollTo().path(path("r10"));
			assertTrue(tree.getVisibleRect().intersects(tree.getRowBounds(tree.getSelectionRows()[0])));
			//the ancestors expanded if required
			model.expansion().collapse(path("r11"));
			layout(scrollPane);
			tree.scrollTo().path(path("r11", "c20"));
			assertTrue(model.expansion().expanded(path("r11")));
			//the scroll pane not being displayed, it is not laid out as the tree grows
			layout(scrollPane);
			tree.scrollTo().path(path("r11", "c20"));
			assertTrue(tree.getVisibleRect().intersects(tree.getPathBounds(model.treePath(path("r11", "c20")))));
			//not in the model
			tree.scrollTo().path(path("r11", "none"));
			tree.scrollTo().path(path("none"));
		});
	}

	/**
	 * Lays out a scroll pane not displayed
	 */
	private static void layout(JScrollPane scrollPane) {
		scrollPane.doLayout();
		Component view = scrollPane.getViewport().getView();
		view.setSize(view.getPreferredSize());
		scrollPane.getViewport().doLayout();
	}

	@Test
	void popupSelection() {
		SwingFilterTreeModel<String> model = model();
		model.nodes().refresh();
		FilterTree<String> tree = FilterTree.builder()
						.model(model)
						.build();
		tree.setSize(200, 200);
		Rectangle bounds = tree.getRowBounds(1);
		tree.getPopupLocation(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
						0, bounds.x + 1, bounds.y + 1, 1, true, MouseEvent.BUTTON3));
		assertEquals(singletonList(path("b")), model.selection().items().get());
		tree.getPopupLocation(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
						0, 1, 190, 1, true, MouseEvent.BUTTON3));
		assertTrue(model.selection().items().get().isEmpty());
	}

	@Test
	void async() throws Exception {
		AtomicReference<FilterTree<String>> tree = new AtomicReference<>();
		CountDownLatch rootLoaded = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> {
			SwingFilterTreeModel<String> model = SwingFilterTreeModel.builder()
							.roots(() -> fetch(""))
							.children(path -> fetch(path.item()))
							.build();
			tree.set(FilterTree.builder()
							.model(model)
							.build());
			model.visible().addListener(rootLoaded::countDown);
			model.nodes().refresh();
		});
		assertTrue(rootLoaded.await(10, SECONDS));
		CountDownLatch childrenLoaded = new CountDownLatch(1);
		//expanded via the tree, loaded later
		SwingUtilities.invokeAndWait(() -> {
			FilterTree<String> filterTree = tree.get();
			assertEquals(2, filterTree.getRowCount());
			filterTree.model().visible().addListener(childrenLoaded::countDown);
			filterTree.expandRow(0);
			assertTrue(filterTree.model().expansion().expanded(path("a")));
			assertFalse(filterTree.model().nodes().loaded(path("a")));
		});
		assertTrue(childrenLoaded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(4, tree.get().getRowCount());
			assertInSync(tree.get());
		});
	}

	@Test
	void asyncFailedLoad() throws Exception {
		AtomicReference<FilterTree<String>> tree = new AtomicReference<>();
		CountDownLatch rootLoaded = new CountDownLatch(1);
		CountDownLatch failed = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> {
			SwingFilterTreeModel<String> model = SwingFilterTreeModel.builder()
							.roots(() -> fetch(""))
							.children(path -> {
								throw new IllegalStateException("Failed to load " + path);
							})
							.onLoadException(exception -> failed.countDown())
							.build();
			tree.set(FilterTree.builder()
							.model(model)
							.build());
			model.visible().addListener(rootLoaded::countDown);
			model.nodes().refresh();
		});
		assertTrue(rootLoaded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			//expanded via the tree, marked expanded while loading
			tree.get().expandRow(0);
			assertTrue(tree.get().model().expansion().expanded(path("a")));
			assertTrue(tree.get().isExpanded(0));
		});
		assertTrue(failed.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			//collapsed in the model, the tree following
			assertFalse(tree.get().model().expansion().expanded(path("a")));
			assertFalse(tree.get().isExpanded(0));
			assertInSync(tree.get());
		});
	}

	@Test
	void invariant() throws Exception {
		for (long seed = 1; seed <= 40; seed++) {
			long finalSeed = seed;
			onEDT(() -> invariant(finalSeed, false, false, false, 300));
		}
	}

	@Test
	void invariantLeaf() throws Exception {
		for (long seed = 201; seed <= 240; seed++) {
			long finalSeed = seed;
			onEDT(() -> invariant(finalSeed, finalSeed % 2 == 0, true, false, 300));
		}
	}

	@Test
	void invariantSelect() throws Exception {
		for (long seed = 301; seed <= 340; seed++) {
			long finalSeed = seed;
			onEDT(() -> invariant(finalSeed, finalSeed % 2 == 0, finalSeed % 3 == 0, true, 300));
		}
	}

	@Test
	void invariantLargeModel() throws Exception {
		for (long seed = 101; seed <= 120; seed++) {
			long finalSeed = seed;
			onEDT(() -> invariant(finalSeed, true, false, false, 300));
		}
		//found by longer runs, a node marked expanded by the tree while its layout shows it collapsed
		onEDT(() -> invariant(40319, true, false, false, 1000));
		//found by longer runs, children inserted below a node the layout shows expanded while the model has it collapsed
		onEDT(() -> invariant(61652, true, false, false, 2000));
	}

	private static void onEDT(Runnable runnable) throws Exception {
		AtomicReference<Throwable> thrown = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				runnable.run();
			}
			catch (Throwable e) {
				thrown.set(e);
			}
		});
		if (thrown.get() instanceof Error) {
			throw (Error) thrown.get();
		}
		if (thrown.get() != null) {
			throw new RuntimeException(thrown.get());
		}
	}

	/**
	 * Performs random operations via the model, the tree and the keyboard, asserting that the rows and the
	 * selection of the tree equal the visible nodes and the selection of the model after each one.
	 * @param leaf true if a node whose children are known to be empty should be a leaf according to the leaf function,
	 * its status changing as the operations add and remove children
	 * @param select true if selecting a path, visible or not, should be one of the operations
	 */
	private void invariant(long seed, boolean largeModel, boolean leaf, boolean select, int count) {
		Random random = new Random(seed);
		Map<String, List<String>> nodes = new HashMap<>();
		AtomicInteger counter = new AtomicInteger();
		nodes.put("", new ArrayList<>(asList("n" + counter.incrementAndGet(), "n" + counter.incrementAndGet(), "n" + counter.incrementAndGet())));
		SwingFilterTreeModel.Builder<String> modelBuilder = SwingFilterTreeModel.builder()
						.roots(() -> fresh(nodes.getOrDefault("", emptyList())))
						.children(path -> fresh(nodes.computeIfAbsent(path.item(), item -> randomChildren(random, counter))))
						.comparator(naturalOrder());
		if (leaf) {
			modelBuilder.leaf(path -> {
				List<String> children = nodes.get(path.item());

				return children != null && children.isEmpty();
			});
		}
		SwingFilterTreeModel<String> model = modelBuilder.build();
		model.nodes().loader().async().set(false);
		model.sort().clear();
		FilterTree.Builder<String> builder = FilterTree.builder()
						.model(model);
		if (largeModel) {
			builder.largeModel(true).rowHeight(18);
		}
		FilterTree<String> tree = builder.build();
		model.nodes().refresh();
		List<String> operations = new ArrayList<>();
		Trace trace = new Trace(model, tree);
		for (int i = 0; i < count; i++) {
			trace.events.clear();
			List<NodePath<String>> visibleBefore = model.visible().get();
			Collection<NodePath<String>> expandedBefore = model.expansion().get();
			operations.add(operation(random, model, tree, nodes, counter, select));
			try {
				assertInSync(tree);
			}
			catch (AssertionError e) {
				throw new AssertionError("Seed " + seed + (largeModel ? ", large model" : "") + ", out of sync after: "
								+ operations.subList(Math.max(0, operations.size() - 8), operations.size())
								+ "\nvisible before " + visibleBefore + "\nexpanded before " + expandedBefore
								+ "\nvisible after " + model.visible().get() + "\nexpanded after " + model.expansion().get()
								+ "\nevents " + trace.events + "\ntree " + trace.tree(), e);
			}
		}
	}

	private String operation(Random random, SwingFilterTreeModel<String> model, FilterTree<String> tree,
													 Map<String, List<String>> nodes, AtomicInteger counter, boolean select) {
		List<NodePath<String>> visible = model.visible().get();
		NodePath<String> path = visible.isEmpty() ? nodePath() : visible.get(random.nextInt(visible.size()));
		int row = visible.isEmpty() ? -1 : visible.indexOf(path);
		//without select the same operations as before, the committed seeds replaying what they found
		switch (random.nextInt(select ? 18 : 17)) {
			case 0:
				model.expansion().expand(path);
				return "model expand " + path;
			case 1:
				model.expansion().collapse(path);
				return "model collapse " + path;
			case 2:
				if (row >= 0) {
					tree.expandRow(row);
				}
				return "tree expand " + path;
			case 3:
				if (row >= 0) {
					tree.collapseRow(row);
				}
				return "tree collapse " + path;
			case 4: {
				String parent = path.root() ? "" : path.item();
				List<String> children = nodes.get(parent);
				if (children != null) {
					mutate(random, children, counter);
				}
				if (random.nextBoolean()) {
					model.nodes().refresh();
					return "mutate " + parent + ", refresh all";
				}
				model.nodes().refresh(path.root() ? path : path.parent());
				return "mutate " + parent + ", refresh " + (path.root() ? path : path.parent());
			}
			case 5: {
				if (random.nextInt(3) == 0) {
					model.nodes().predicate().clear();
					return "clear predicate";
				}
				String digit = String.valueOf(random.nextInt(10));
				model.nodes().predicate().set(nodePath -> nodePath.item().contains(digit));
				return "predicate " + digit;
			}
			case 6: {
				int order = random.nextInt(3);
				if (order == 0) {
					model.sort().ascending();
				}
				else if (order == 1) {
					model.sort().descending();
				}
				else {
					model.sort().clear();
				}
				return "sort " + model.sort().order();
			}
			case 7: {
				if (model.nodes().loaded(path)) {
					String item = "n" + counter.incrementAndGet();
					nodes.computeIfAbsent(path.root() ? "" : path.item(), key -> new ArrayList<>()).add(item);
					model.nodes().add(path, singletonList(item));
					return "add " + item + " to " + path;
				}
				return "add to unloaded " + path;
			}
			case 8:
				if (!path.root()) {
					nodes.getOrDefault(path.parent().root() ? "" : path.parent().item(), new ArrayList<>()).remove(path.item());
					model.nodes().remove(singletonList(path));
				}
				return "remove " + path;
			case 9:
				if (!path.root()) {
					String item = "n" + counter.incrementAndGet();
					List<String> siblings = nodes.get(path.parent().root() ? "" : path.parent().item());
					if (siblings != null && siblings.contains(path.item())) {
						siblings.set(siblings.indexOf(path.item()), item);
						nodes.put(item, nodes.getOrDefault(path.item(), new ArrayList<>()));
					}
					model.nodes().replace(path, item);
					return "replace " + path + " with " + item;
				}
				return "replace root";
			case 10: {
				List<Integer> rows = new ArrayList<>();
				for (int j = 0; j < visible.size(); j++) {
					if (random.nextInt(4) == 0) {
						rows.add(j);
					}
				}
				model.selection().indexes().set(rows);
				return "model select " + rows;
			}
			case 11:
				if (row >= 0) {
					if (random.nextBoolean()) {
						tree.setSelectionRow(row);
					}
					else {
						tree.addSelectionRow(row);
					}
				}
				return "tree select " + row;
			case 12: {
				String action = asList("selectNext", "selectPrevious", "selectNextExtendSelection", "selectParent",
								"selectChild", "toggle", "expand", "collapse", "moveSelectionToParent", "selectLast", "selectAll")
								.get(random.nextInt(11));
				if (row >= 0 && !tree.isSelectionEmpty() && random.nextBoolean()) {
					tree.setLeadSelectionPath(tree.getPathForRow(Math.min(row, tree.getRowCount() - 1)));
				}
				Action treeAction = tree.getActionMap().get(action);
				if (treeAction != null) {
					treeAction.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, action));
				}
				return "key " + action;
			}
			case 13:
				if (random.nextInt(4) == 0) {
					tree.updateUI();
					return "updateUI";
				}
				return "no updateUI";
			case 14:
				model.selection().items().set(singletonList(path));
				perform(tree, random.nextBoolean() ? FilterTree.ControlKeys.EXPAND : FilterTree.ControlKeys.COLLAPSE);
				return "expand/collapse subtree " + path;
			case 15: {
				List<NodePath<String>> expanded = new ArrayList<>(model.expansion().get());
				Collections.shuffle(expanded, random);
				model.expansion().set(expanded.subList(0, expanded.size() / 2));
				return "set expansion " + expanded.subList(0, expanded.size() / 2);
			}
			case 17:
				return select(random, model, nodes);
			default:
				tree.clearSelection();
				return "tree clear selection";
		}
	}

	/**
	 * Selects a random path down the generated nodes, loaded or not, sometimes one which does not exist. Via
	 * {@code selection().set()}, asserting that a node on the path is selected, the node itself when visible, or that
	 * the selection is left as is. Or via {@code item().set()} for a node in the model, hidden or not, asserting that it
	 * is selected, or the selection cleared in case it is filtered.
	 */
	private static String select(Random random, SwingFilterTreeModel<String> model, Map<String, List<String>> nodes) {
		List<String> items = new ArrayList<>();
		String parent = "";
		int depth = 1 + random.nextInt(4);
		while (items.size() < depth) {
			List<String> children = nodes.get(parent);
			if (children == null || children.isEmpty()) {
				break;
			}
			parent = children.get(random.nextInt(children.size()));
			items.add(parent);
		}
		if (items.isEmpty() || random.nextInt(4) == 0) {
			items.add("missing");
		}
		NodePath<String> target = nodePath(items);
		if (model.nodes().contains(target) && random.nextBoolean()) {
			model.selection().item().set(target);
			List<NodePath<String>> selected = model.selection().items().get();
			List<NodePath<String>> expected = model.nodes().included(target) ? singletonList(target) : emptyList();
			if (!selected.equals(expected)) {
				throw new AssertionError("Selected item " + target + ", but selected " + selected);
			}

			return "select item " + target;
		}
		List<NodePath<String>> before = model.selection().items().get();
		model.selection().set(target);
		List<NodePath<String>> after = model.selection().items().get();
		if (model.visible().indexOf(target) >= 0 && !after.equals(singletonList(target))) {
			throw new AssertionError("Set " + target + ", visible, but selected " + after);
		}
		if (!after.equals(before) && (after.size() != 1 || !after.get(0).contains(target))) {
			throw new AssertionError("Set " + target + ", but selected " + after);
		}

		return "select " + target;
	}

	private static void mutate(Random random, List<String> children, AtomicInteger counter) {
		switch (random.nextInt(3)) {
			case 0:
				children.add(random.nextInt(children.size() + 1), "n" + counter.incrementAndGet());
				break;
			case 1:
				if (!children.isEmpty()) {
					children.remove(random.nextInt(children.size()));
				}
				break;
			default:
				Collections.shuffle(children, random);
				break;
		}
	}

	private static List<String> randomChildren(Random random, AtomicInteger counter) {
		List<String> children = new ArrayList<>();
		int count = random.nextInt(4);
		for (int i = 0; i < count; i++) {
			children.add("n" + counter.incrementAndGet());
		}

		return children;
	}

	/**
	 * @return new String instances, equal to the given ones, as a query returns fresh instances
	 */
	private static List<String> fresh(Collection<String> items) {
		return items.stream()
						.map(String::new)
						.collect(toList());
	}

	private static void assertInSync(FilterTree<String> tree) {
		SwingFilterTreeModel<String> model = tree.model();
		List<NodePath<String>> visible = model.visible().get();
		List<Object> rows = new ArrayList<>();
		for (int row = 0; row < tree.getRowCount(); row++) {
			rows.add(tree.getPathForRow(row).getLastPathComponent());
		}
		assertEquals(visible, rows, "rows");
		int[] selectionRows = tree.getSelectionRows();
		List<Integer> treeRows = selectionRows == null ? emptyList() : Arrays.stream(selectionRows).sorted().boxed().collect(toList());
		assertEquals(model.selection().indexes().get(), treeRows, "selected rows");
		TreePath[] selectionPaths = tree.getSelectionPaths();
		Set<Object> treeSelection = new HashSet<>();
		if (selectionPaths != null) {
			for (TreePath selectionPath : selectionPaths) {
				treeSelection.add(selectionPath.getLastPathComponent());
			}
		}
		assertEquals(new HashSet<>(model.selection().items().get()), treeSelection, "selected paths");
	}

	private static void perform(FilterTree<String> tree, ControlKey<?> controlKey) {
		Object actionKey = tree.getInputMap(JComponent.WHEN_FOCUSED).get(controlKey.defaultKeystroke().getOrThrow());
		tree.getActionMap().get(actionKey).actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, ""));
	}

	private SwingFilterTreeModel<String> model() {
		SwingFilterTreeModel<String> model = SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(path -> fetch(path.item()))
						.build();
		model.nodes().loader().async().set(false);

		return model;
	}

	private List<String> fetch(String parent) {
		return fresh(data.getOrDefault(parent, emptyList()));
	}

	private FilterTree<String> customRendererTree() {
		SwingFilterTreeModel<String> model = model();
		model.nodes().refresh();

		return FilterTree.builder()
						.model(model)
						.cellRenderer(new DefaultTreeCellRenderer())
						.build();
	}

	private FilterTree<String> iconRendererTree() {
		SwingFilterTreeModel<String> model = model();
		model.nodes().refresh();

		return FilterTree.builder()
						.model(model)
						.icon(item -> null)
						.build();
	}

	private static NodePath<String> path(String... items) {
		return nodePath(asList(items));
	}

	/**
	 * Records the model and tree events of an operation, for a failure message.
	 */
	private static final class Trace implements TreeModelListener, TreeWillExpandListener, TreeExpansionListener {

		private final List<String> events = new ArrayList<>();
		private final SwingFilterTreeModel<String> model;
		private final FilterTree<String> tree;

		private Trace(SwingFilterTreeModel<String> model, FilterTree<String> tree) {
			this.model = model;
			this.tree = tree;
			model.addTreeModelListener(this);
			tree.addTreeWillExpandListener(this);
			tree.addTreeExpansionListener(this);
			model.expansion().expanded().addConsumer(path -> events.add("model expanded " + path));
			model.expansion().collapsed().addConsumer(path -> events.add("model collapsed " + path));
		}

		@Override
		public void treeNodesChanged(TreeModelEvent event) {
			events.add("changed " + event.getTreePath().getLastPathComponent() + Arrays.toString(event.getChildIndices()));
		}

		@Override
		public void treeNodesInserted(TreeModelEvent event) {
			events.add("inserted " + event.getTreePath().getLastPathComponent() + Arrays.toString(event.getChildIndices()));
		}

		@Override
		public void treeNodesRemoved(TreeModelEvent event) {
			events.add("removed " + event.getTreePath().getLastPathComponent() + Arrays.toString(event.getChildIndices()));
		}

		@Override
		public void treeStructureChanged(TreeModelEvent event) {
			events.add("structure [" + event.getTreePath().getLastPathComponent() + "]");
		}

		@Override
		public void treeWillExpand(TreeExpansionEvent event) {
			events.add("willExpand " + event.getPath().getLastPathComponent());
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent event) {
			events.add("willCollapse " + event.getPath().getLastPathComponent());
		}

		@Override
		public void treeExpanded(TreeExpansionEvent event) {
			events.add("expanded " + event.getPath().getLastPathComponent());
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent event) {
			events.add("collapsed " + event.getPath().getLastPathComponent());
		}

		private List<String> tree() {
			return model.visible().get().stream()
							.map(path -> path + " expanded " + tree.isExpanded(model.treePath(path))
											+ " leaf " + model.isLeaf(path) + " row " + tree.getRowForPath(model.treePath(path)))
							.collect(toList());
		}
	}
}
