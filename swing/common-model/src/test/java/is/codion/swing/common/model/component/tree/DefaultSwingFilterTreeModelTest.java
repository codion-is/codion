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
package is.codion.swing.common.model.component.tree;

import is.codion.common.model.CancelException;
import is.codion.common.model.component.tree.FilterTreeModel;
import is.codion.common.model.component.tree.NodePath;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.naturalOrder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Swing coat of {@link SwingFilterTreeModel}, the UI-agnostic logic being tested in
 * {@code is.codion.common.model.component.tree.DefaultFilterTreeModelTest}. The tree:
 * <pre>
 * a
 *   a1
 *   a2
 * b
 * </pre>
 */
final class DefaultSwingFilterTreeModelTest {

	private final Map<String, List<String>> data = new HashMap<>();
	private final List<String> events = new ArrayList<>();

	@BeforeEach
	void setUp() {
		data.put("", asList("a", "b"));
		data.put("a", asList("a1", "a2"));
	}

	@Test
	void treeModel() {
		SwingFilterTreeModel<String> model = model();
		NodePath<String> root = model.getRoot();
		assertTrue(root.root());
		assertSame(nodePath(), root);
		assertEquals(0, model.getChildCount(root));
		model.nodes().refresh();
		assertEquals(2, model.getChildCount(root));
		assertEquals(path("a"), model.getChild(root, 0));
		assertEquals(path("b"), model.getChild(root, 1));
		assertEquals(1, model.getIndexOfChild(root, path("b")));
		assertEquals(-1, model.getIndexOfChild(root, path("x")));
		assertEquals(-1, model.getIndexOfChild(null, path("b")));
		assertEquals(-1, model.getIndexOfChild(root, null));
		assertFalse(model.isLeaf(root));
		assertFalse(model.isLeaf(path("a")));
		assertEquals(0, model.getChildCount(path("a")));
		model.expansion().expand(path("a"));
		assertEquals(2, model.getChildCount(path("a")));
		assertEquals(path("a", "a2"), model.getChild(path("a"), 1));
		assertTrue(model.isLeaf(path("x")));
		assertThrows(UnsupportedOperationException.class, () -> model.valueForPathChanged(new TreePath(root), "x"));

		TreePath treePath = model.treePath(path("a", "a1"));
		assertEquals(3, treePath.getPathCount());
		assertSame(root, treePath.getPathComponent(0));
		assertEquals(path("a"), treePath.getPathComponent(1));
		assertEquals(path("a", "a1"), treePath.getLastPathComponent());
		assertEquals(new TreePath(new Object[] {root, path("a"), path("a", "a1")}), treePath);
		assertEquals(1, model.treePath(root).getPathCount());
	}

	@Test
	void treeModelEvents() {
		SwingFilterTreeModel<String> model = model();
		model.addTreeModelListener(new RecordingListener("first"));
		model.addTreeModelListener(new RecordingListener("second"));
		model.nodes().refresh();
		//in the order added
		assertEvents("first inserted [] [0, 1] [a, b]", "second inserted [] [0, 1] [a, b]");
		RecordingListener third = new RecordingListener("third");
		model.addTreeModelListener(third);
		model.removeTreeModelListener(third);
		model.expansion().expand(path("a"));
		assertEvents("first inserted [a] [0, 1] [a / a1, a / a2]", "second inserted [a] [0, 1] [a / a1, a / a2]");
		model.nodes().remove(singletonList(path("a", "a1")));
		assertEvents("first removed [a] [0] [a / a1]", "second removed [a] [0] [a / a1]");
		data.put("a", asList("a3", "a2"));
		model.nodes().refresh(path("a"));
		//inserted, the other refreshed
		assertEvents("first inserted [a] [0] [a / a3]", "second inserted [a] [0] [a / a3]",
						"first changed [a] [1] [a / a2]", "second changed [a] [1] [a / a2]");
		data.put("a", asList("a2", "a3"));
		model.nodes().refresh(path("a"));
		//reordered
		assertEvents("first structureChanged [a]", "second structureChanged [a]");
		data.put("a", asList("a3", "a2"));
		model.nodes().refresh(path("a"));
		events.clear();
		model.fireNodesChanged(asList(path("b"), path("a", "a2"), path("x"), path("a", "a3"), nodePath()));
		assertEvents("first changed [] [1] [b]", "second changed [] [1] [b]",
						"first changed [a] [0, 1] [a / a3, a / a2]", "second changed [a] [0, 1] [a / a3, a / a2]");
	}

	@Test
	void selection() {
		SwingFilterTreeModel<String> model = model();
		FilterTreeSelection<String> selection = model.selection();
		model.nodes().refresh();
		model.expansion().expand(path("a"));
		selection.items().set(singletonList(path("b")));
		assertEquals(3, selection.index().get());
		assertTrue(selection.isPathSelected(model.treePath(path("b"))));
		assertEquals(1, selection.getSelectionCount());

		//collapsing shifts the row, the path stays selected
		List<List<Integer>> indexes = new ArrayList<>();
		List<NodePath<String>> items = new ArrayList<>();
		selection.indexes().addConsumer(indexes::add);
		selection.item().addConsumer(items::add);
		model.expansion().collapse(path("a"));
		assertEquals(1, selection.index().get());
		assertEquals(singletonList(singletonList(1)), indexes);
		assertTrue(items.isEmpty());
		assertTrue(selection.isPathSelected(model.treePath(path("b"))));

		//via the TreeSelectionModel
		selection.setSelectionPaths(new TreePath[] {model.treePath(path("a")), model.treePath(path("b"))});
		assertEquals(asList(path("a"), path("b")), selection.items().get());
		assertEquals(asList(0, 1), selection.indexes().get());
		selection.removeSelectionPath(model.treePath(path("a")));
		assertEquals(singletonList(path("b")), selection.items().get());
		selection.clearSelection();
		assertTrue(selection.items().get().isEmpty());

		//collapsing a node with a selected descendant selects it
		model.expansion().expand(path("a"));
		selection.items().set(singletonList(path("a", "a2")));
		model.expansion().collapse(path("a"));
		assertEquals(singletonList(path("a")), selection.items().get());
		assertArrayEquals(new TreePath[] {model.treePath(path("a"))}, selection.getSelectionPaths());

		//refreshed instances
		model.expansion().expand(path("a"));
		selection.items().set(singletonList(path("a", "a1")));
		NodePath<String> selected = selection.item().getOrThrow();
		items.clear();
		model.nodes().refresh();
		assertEquals(selected, selection.item().get());
		assertEquals(1, items.size());
	}

	@Test
	void singleSelection() {
		SwingFilterTreeModel<String> model = model();
		FilterTreeSelection<String> selection = model.selection();
		model.nodes().refresh();
		assertEquals(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION, selection.getSelectionMode());
		selection.selectAll();
		assertEquals(2, selection.count());
		selection.singleSelection().set(true);
		assertEquals(TreeSelectionModel.SINGLE_TREE_SELECTION, selection.getSelectionMode());
		//cleared on mode change
		assertEquals(0, selection.count());
		selection.indexes().set(asList(0, 1));
		assertEquals(singletonList(1), selection.indexes().get());
		selection.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		assertFalse(selection.singleSelection().is());
	}

	@Test
	void changingSingleSelection() {
		SwingFilterTreeModel<String> model = model();
		FilterTreeSelection<String> selection = model.selection();
		model.nodes().refresh();
		selection.singleSelection().set(true);
		AtomicInteger changing = new AtomicInteger();
		selection.changing().addListener(changing::incrementAndGet);
		//a path is added by setting it when in single selection mode, a single change
		selection.addSelectionPath(model.treePath(path("a")));
		assertEquals(1, changing.get());
		selection.addSelectionPath(model.treePath(path("b")));
		assertEquals(2, changing.get());
		assertEquals(singletonList(path("b")), selection.items().get());
		//a change made by a listener in response is a change of its own
		selection.items().addConsumer(items -> {
			if (items.equals(singletonList(path("a")))) {
				selection.setSelectionPath(model.treePath(path("b")));
			}
		});
		selection.setSelectionPath(model.treePath(path("a")));
		assertEquals(4, changing.get());
		assertEquals(singletonList(path("b")), selection.items().get());
	}

	@Test
	void changing() {
		SwingFilterTreeModel<String> model = model();
		FilterTreeSelection<String> selection = model.selection();
		model.nodes().refresh();
		model.expansion().expand(path("a"));
		selection.items().set(singletonList(path("a", "a1")));
		AtomicBoolean veto = new AtomicBoolean(true);
		List<Object> changing = new ArrayList<>();
		selection.changing().addListener(() -> {
			changing.add(1);
			if (veto.get()) {
				throw new CancelException();
			}
		});
		assertThrows(CancelException.class, () -> selection.setSelectionPath(model.treePath(path("b"))));
		assertEquals(singletonList(path("a", "a1")), selection.items().get());
		assertThrows(CancelException.class, () -> selection.items().set(singletonList(path("b"))));
		assertThrows(CancelException.class, selection::clearSelection);
		assertEquals(singletonList(path("a", "a1")), selection.items().get());
		veto.set(false);
		changing.clear();
		//structural changes do not pass the veto point
		model.addTreeModelListener(new TreeModelListener() {
			@Override
			public void treeNodesChanged(TreeModelEvent e) {}

			@Override
			public void treeNodesInserted(TreeModelEvent e) {}

			@Override
			public void treeNodesRemoved(TreeModelEvent e) {}

			@Override
			public void treeStructureChanged(TreeModelEvent e) {
				//as a JTree does
				selection.removeSelectionPath(model.treePath(path("a", "a1")));
			}
		});
		data.put("a", asList("a2", "a1"));
		model.nodes().refresh(path("a"));
		assertTrue(changing.isEmpty());
		//restored
		assertEquals(singletonList(path("a", "a1")), selection.items().get());
		//nor does the model restoring the selection, here replacing the selected node with the ancestor collapsed
		model.expansion().collapse(path("a"));
		assertEquals(singletonList(path("a")), selection.items().get());
		assertTrue(changing.isEmpty());
	}

	@Test
	void async() throws Exception {
		CountDownLatch blocked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean block = new AtomicBoolean(false);
		AtomicReference<SwingFilterTreeModel<String>> reference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> reference.set(SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(path -> {
							if (path.item().equals("a") && block.getAndSet(false)) {
								blocked.countDown();
								try {
									release.await(10, SECONDS);
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
								return asList("old");
							}
							return fetch(path.item());
						})
						.build()));
		SwingFilterTreeModel<String> model = reference.get();
		assertTrue(model.nodes().loader().async().is());
		List<Boolean> active = new ArrayList<>();
		model.nodes().loader().active().addConsumer(active::add);
		CountDownLatch loaded = new CountDownLatch(1);
		model.visible().addListener(loaded::countDown);
		SwingUtilities.invokeAndWait(() -> {
			model.nodes().refresh();
			//not yet
			assertFalse(model.nodes().loaded(nodePath()));
			assertTrue(model.nodes().loader().active().is());
			assertTrue(model.nodes().loader().active(nodePath()));
		});
		assertTrue(loaded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(model.nodes().loaded(nodePath()));
			assertFalse(model.nodes().loader().active().is());
		});
		assertEquals(asList(true, false), active);

		//expand, loading the children
		CountDownLatch expanded = new CountDownLatch(1);
		model.visible().addListener(expanded::countDown);
		SwingUtilities.invokeAndWait(() -> model.expansion().expand(path("a")));
		assertTrue(expanded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> assertEquals(asList(path("a"), path("a", "a1"), path("a", "a2"), path("b")),
						model.visible().get()));

		//an older refresh arriving after a newer load of a node it covers does not overwrite it
		block.set(true);
		SwingUtilities.invokeAndWait(model.nodes()::refresh);
		assertTrue(blocked.await(10, SECONDS));
		data.put("a", asList("new"));
		CountDownLatch reloaded = new CountDownLatch(1);
		model.visible().addListener(reloaded::countDown);
		SwingUtilities.invokeAndWait(() -> model.nodes().refresh(path("a")));
		assertTrue(reloaded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(singletonList(path("a", "new")), model.nodes().children(path("a")));
			//the refresh of the root still in progress
			assertTrue(model.nodes().loader().active().is());
		});
		CountDownLatch refreshed = new CountDownLatch(1);
		model.nodes().loader().active().addConsumer(value -> {
			if (!value) {
				refreshed.countDown();
			}
		});
		release.countDown();
		assertTrue(refreshed.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() ->
						assertEquals(singletonList(path("a", "new")), model.nodes().children(path("a"))));
	}

	@Test
	void asyncLeaf() throws Exception {
		Set<String> leaves = ConcurrentHashMap.newKeySet();
		leaves.add("b");
		List<Boolean> dispatchThread = new CopyOnWriteArrayList<>();
		CountDownLatch blocked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean block = new AtomicBoolean(false);
		AtomicReference<SwingFilterTreeModel<String>> reference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> reference.set(SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(path -> fetch(path.item()))
						.leaf(path -> {
							dispatchThread.add(SwingUtilities.isEventDispatchThread());
							boolean leaf = leaves.contains(path.item());
							if (path.item().equals("b") && block.getAndSet(false)) {
								blocked.countDown();
								try {
									release.await(10, SECONDS);
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
							}
							return leaf;
						})
						.build()));
		SwingFilterTreeModel<String> model = reference.get();
		CountDownLatch loaded = new CountDownLatch(1);
		model.visible().addListener(loaded::countDown);
		SwingUtilities.invokeAndWait(model.nodes()::refresh);
		assertTrue(loaded.await(10, SECONDS));
		//called off the dispatch thread, along with the children function, not when asked whether a node is a leaf
		SwingUtilities.invokeAndWait(() -> {
			assertFalse(model.nodes().leaf(path("a")));
			assertTrue(model.nodes().leaf(path("b")));
			assertTrue(model.isLeaf(path("b")));
		});
		assertEquals(asList(false, false), dispatchThread);

		//an older refresh arriving after a newer refresh of a node it covers does not overwrite its leaf status
		block.set(true);
		SwingUtilities.invokeAndWait(model.nodes()::refresh);
		assertTrue(blocked.await(10, SECONDS));
		leaves.remove("b");
		data.put("b", asList("b1"));
		CountDownLatch reloaded = new CountDownLatch(1);
		model.addTreeModelListener(new TreeModelListener() {
			@Override
			public void treeNodesChanged(TreeModelEvent event) {}

			@Override
			public void treeNodesInserted(TreeModelEvent event) {
				if (event.getTreePath().getLastPathComponent().equals(path("b"))) {
					reloaded.countDown();
				}
			}

			@Override
			public void treeNodesRemoved(TreeModelEvent event) {}

			@Override
			public void treeStructureChanged(TreeModelEvent event) {}
		});
		SwingUtilities.invokeAndWait(() -> model.nodes().refresh(path("b")));
		assertTrue(reloaded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			assertFalse(model.nodes().leaf(path("b")));
			//the refresh of the root still in progress
			assertTrue(model.nodes().loader().active().is());
		});
		CountDownLatch refreshed = new CountDownLatch(1);
		model.nodes().loader().active().addConsumer(value -> {
			if (!value) {
				refreshed.countDown();
			}
		});
		release.countDown();
		assertTrue(refreshed.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			assertFalse(model.nodes().leaf(path("b")));
			assertEquals(singletonList(path("b", "b1")), model.nodes().children(path("b")));
		});
	}

	@Test
	void asyncSelectionSet() throws Exception {
		//pending until the path is loaded
		BlockingChildren children = new BlockingChildren();
		SwingFilterTreeModel<String> model = asyncModel(children);
		children.block("a");
		SwingUtilities.invokeAndWait(() -> model.selection().set(path("a", "a1")));
		children.awaitBlocked();
		SwingUtilities.invokeAndWait(() -> assertFalse(model.selection().present().is()));
		children.release();
		awaitIdle(model);
		SwingUtilities.invokeAndWait(() -> assertEquals(path("a", "a1"), model.selection().item().get()));

		//cancelled by a selection change from outside
		SwingFilterTreeModel<String> cancelled = asyncModel(children);
		children.block("a");
		SwingUtilities.invokeAndWait(() -> cancelled.selection().set(path("a", "a1")));
		children.awaitBlocked();
		SwingUtilities.invokeAndWait(() -> cancelled.selection().items().set(singletonList(path("b"))));
		children.release();
		awaitIdle(cancelled);
		SwingUtilities.invokeAndWait(() -> assertEquals(singletonList(path("b")), cancelled.selection().items().get()));

		//replaced by another reveal
		SwingFilterTreeModel<String> replaced = asyncModel(children);
		children.block("a");
		SwingUtilities.invokeAndWait(() -> replaced.selection().set(path("a", "a1")));
		children.awaitBlocked();
		SwingUtilities.invokeAndWait(() -> replaced.selection().set(path("b")));
		children.release();
		awaitIdle(replaced);
		SwingUtilities.invokeAndWait(() -> assertEquals(singletonList(path("b")), replaced.selection().items().get()));

		//a failed load, the deepest node on the path which exists
		SwingFilterTreeModel<String> failed = asyncModel(children);
		children.block("a");
		children.fail = true;
		SwingUtilities.invokeAndWait(() -> failed.selection().set(path("a", "a1")));
		children.awaitBlocked();
		children.release();
		awaitIdle(failed);
		children.fail = false;
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(path("a"), failed.selection().item().get());
			assertFalse(failed.nodes().loaded(path("a")));
		});

		//not cancelled by a refresh, which restarts the load
		SwingFilterTreeModel<String> refreshed = asyncModel(children);
		children.block("a");
		SwingUtilities.invokeAndWait(() -> refreshed.selection().set(path("a", "a1")));
		children.awaitBlocked();
		SwingUtilities.invokeAndWait(() -> refreshed.nodes().refresh(path("a")));
		children.release();
		awaitIdle(refreshed);
		SwingUtilities.invokeAndWait(() -> assertEquals(path("a", "a1"), refreshed.selection().item().get()));

		//an ancestor collapsed while pending, its nearest visible ancestor
		SwingFilterTreeModel<String> collapsed = asyncModel(children);
		children.block("a");
		SwingUtilities.invokeAndWait(() -> collapsed.selection().set(path("a", "a1")));
		children.awaitBlocked();
		SwingUtilities.invokeAndWait(() -> collapsed.expansion().collapse(path("a")));
		children.release();
		awaitIdle(collapsed);
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(collapsed.nodes().loaded(path("a")));
			assertEquals(path("a"), collapsed.selection().item().get());
		});

		//a node on the path removed while pending, nothing left to select
		SwingFilterTreeModel<String> removed = asyncModel(children);
		children.block("a");
		SwingUtilities.invokeAndWait(() -> removed.selection().set(path("a", "a1")));
		children.awaitBlocked();
		SwingUtilities.invokeAndWait(() -> removed.nodes().remove(singletonList(path("a"))));
		children.release();
		awaitIdle(removed);
		SwingUtilities.invokeAndWait(() -> {
			assertFalse(removed.nodes().contains(path("a")));
			assertFalse(removed.selection().present().is());
		});
	}

	@Test
	void asyncCancelled() throws Exception {
		CountDownLatch blocked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean block = new AtomicBoolean(true);
		AtomicReference<SwingFilterTreeModel<String>> reference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> reference.set(SwingFilterTreeModel.builder()
						.roots(() -> {
							if (block.getAndSet(false)) {
								blocked.countDown();
								try {
									release.await(10, SECONDS);
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
								return asList("old");
							}
							return fetch("");
						})
						.children(path -> fetch(path.item()))
						.build()));
		SwingFilterTreeModel<String> model = reference.get();
		SwingUtilities.invokeAndWait(model.nodes()::refresh);
		assertTrue(blocked.await(10, SECONDS));
		CountDownLatch loaded = new CountDownLatch(1);
		model.visible().addListener(loaded::countDown);
		//supersedes the one blocked, cancelling it
		SwingUtilities.invokeAndWait(model.nodes()::refresh);
		assertTrue(loaded.await(10, SECONDS));
		release.countDown();
		//let a late result arrive, if it were to
		Thread.sleep(100);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(asList(path("a"), path("b")), model.visible().get());
			assertFalse(model.nodes().loader().active().is());
		});
	}

	@Test
	void asyncReplaced() throws Exception {
		CountDownLatch blocked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		data.put("x", asList("x1"));
		AtomicReference<SwingFilterTreeModel<String>> reference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> reference.set(SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(path -> {
							if (path.item().equals("a")) {
								blocked.countDown();
								try {
									release.await(10, SECONDS);
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
							}
							return fetch(path.item());
						})
						.build()));
		SwingFilterTreeModel<String> model = reference.get();
		CountDownLatch loaded = new CountDownLatch(1);
		model.visible().addListener(loaded::countDown);
		SwingUtilities.invokeAndWait(model.nodes()::refresh);
		assertTrue(loaded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> model.expansion().expand(path("a")));
		assertTrue(blocked.await(10, SECONDS));
		CountDownLatch replacementLoaded = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(model.nodes().loader().active(path("a")));
			model.visible().addListener(() -> {
				if (model.nodes().loaded(path("x"))) {
					replacementLoaded.countDown();
				}
			});
			//replaced while loading, the load cancelled and the node loaded by its new path, being expanded
			model.nodes().replace(path("a"), "x");
			assertFalse(model.nodes().loader().active(path("a")));
			assertTrue(model.nodes().loader().active(path("x")));
		});
		assertTrue(replacementLoaded.await(10, SECONDS));
		release.countDown();
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(asList(path("x"), path("x", "x1"), path("b")), model.visible().get());
			assertFalse(model.nodes().loader().active().is());
		});
	}

	@Test
	void asyncActive() throws Exception {
		data.put("a1", singletonList("a11"));
		AtomicReference<SwingFilterTreeModel<String>> reference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> reference.set(SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(path -> fetch(path.item()))
						.build()));
		SwingFilterTreeModel<String> model = reference.get();
		List<Boolean> active = new ArrayList<>();
		CountDownLatch inactive = new CountDownLatch(1);
		model.nodes().loader().active().addConsumer(value -> {
			active.add(value);
			if (!value) {
				inactive.countDown();
			}
		});
		SwingUtilities.invokeAndWait(() -> {
			model.expansion().set(asList(path("a"), path("a", "a1")));
			//each superseding the previous one
			model.nodes().refresh();
			model.nodes().refresh();
			model.nodes().refresh();
		});
		assertTrue(inactive.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			//active throughout, the superseded refreshes and the loading of the expanded nodes, level by level, included
			assertTrue(model.nodes().loaded(path("a", "a1")));
			assertEquals(asList(true, false), active);
		});
	}

	@Test
	void asyncFailed() throws Exception {
		AtomicBoolean fail = new AtomicBoolean(true);
		CountDownLatch failed = new CountDownLatch(1);
		AtomicReference<SwingFilterTreeModel<String>> reference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> reference.set(SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(path -> {
							if (fail.get()) {
								throw new IllegalStateException("Failed to load " + path);
							}
							return fetch(path.item());
						})
						.onLoadException(exception -> failed.countDown())
						.build()));
		SwingFilterTreeModel<String> model = reference.get();
		CountDownLatch loaded = new CountDownLatch(1);
		model.visible().addListener(loaded::countDown);
		SwingUtilities.invokeAndWait(model.nodes()::refresh);
		assertTrue(loaded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {
			model.expansion().expand(path("a"));
			assertTrue(model.expansion().expanded(path("a")));
			assertTrue(model.nodes().loader().active(path("a")));
		});
		assertTrue(failed.await(10, SECONDS));
		CountDownLatch expanded = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> {
			//collapsed, not loaded again by other mutations
			assertFalse(model.expansion().expanded(path("a")));
			assertFalse(model.nodes().loader().active().is());
			model.nodes().filter();
			assertFalse(model.nodes().loader().active().is());
			//until expanded again
			fail.set(false);
			model.visible().addListener(expanded::countDown);
			model.expansion().expand(path("a"));
		});
		assertTrue(expanded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> assertEquals(asList(path("a"), path("a", "a1"), path("a", "a2"), path("b")),
						model.visible().get()));
	}

	@Test
	void invariantWithoutTree() {
		for (long seed = 1; seed <= 60; seed++) {
			invariantWithoutTree(seed, 300);
		}
	}

	/**
	 * Performs random operations on the model, with no {@code JTree} attached, and on a UI-agnostic model, asserting
	 * that the visible nodes and the selection of the two are equal after each one, and that the paths of the
	 * {@link javax.swing.tree.TreeSelectionModel} are the selected ones.
	 */
	private static void invariantWithoutTree(long seed, int count) {
		Random random = new Random(seed);
		Map<String, List<String>> nodes = new HashMap<>();
		AtomicInteger counter = new AtomicInteger();
		nodes.put("", new ArrayList<>(asList("n" + counter.incrementAndGet(), "n" + counter.incrementAndGet(), "n" + counter.incrementAndGet())));
		Supplier<Collection<String>> roots = () -> fresh(nodes.getOrDefault("", emptyList()));
		Function<NodePath<String>, Collection<String>> children = path ->
						fresh(nodes.computeIfAbsent(path.item(), item -> randomChildren(random, counter)));
		FilterTreeModel<String> common = FilterTreeModel.builder()
						.roots(roots)
						.children(children)
						.comparator(naturalOrder())
						.build();
		SwingFilterTreeModel<String> swing = SwingFilterTreeModel.builder()
						.roots(roots)
						.children(children)
						.comparator(naturalOrder())
						.build();
		List<String> operations = new ArrayList<>();
		for (FilterTreeModel<String> model : asList(common, swing)) {
			model.nodes().loader().async().set(false);
			model.sort().clear();
			model.nodes().refresh();
		}
		for (int i = 0; i < count; i++) {
			operations.add(operation(random, common, swing, nodes, counter));
			String message = "Seed " + seed + ", after: " + operations.subList(Math.max(0, operations.size() - 8), operations.size());
			assertEquals(common.visible().get(), swing.visible().get(), message);
			assertEquals(common.selection().indexes().get(), swing.selection().indexes().get(), message);
			assertEquals(common.selection().items().get(), swing.selection().items().get(), message);
			assertEquals(common.selection().count(), swing.selection().count(), message);
			TreePath[] selectionPaths = swing.selection().getSelectionPaths();
			Set<Object> selected = new HashSet<>();
			if (selectionPaths != null) {
				for (TreePath selectionPath : selectionPaths) {
					selected.add(selectionPath.getLastPathComponent());
				}
			}
			assertEquals(new HashSet<>(swing.selection().items().get()), selected, message);
		}
	}

	private static String operation(Random random, FilterTreeModel<String> common, SwingFilterTreeModel<String> swing,
																	Map<String, List<String>> nodes, AtomicInteger counter) {
		List<FilterTreeModel<String>> models = asList(common, swing);
		List<NodePath<String>> visible = common.visible().get();
		NodePath<String> path = visible.isEmpty() ? nodePath() : visible.get(random.nextInt(visible.size()));
		switch (random.nextInt(14)) {
			case 0:
				models.forEach(model -> model.expansion().expand(path));
				return "expand " + path;
			case 1:
				models.forEach(model -> model.expansion().collapse(path));
				return "collapse " + path;
			case 2: {
				String parent = path.root() ? "" : path.item();
				List<String> children = nodes.get(parent);
				if (children != null) {
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
				NodePath<String> refresh = path.root() || random.nextBoolean() ? nodePath() : path.parent();
				models.forEach(model -> model.nodes().refresh(refresh));
				return "mutate " + parent + ", refresh " + refresh;
			}
			case 3: {
				if (random.nextInt(3) == 0) {
					models.forEach(model -> model.nodes().predicate().clear());
					return "clear predicate";
				}
				String digit = String.valueOf(random.nextInt(10));
				models.forEach(model -> model.nodes().predicate().set(nodePath -> nodePath.item().contains(digit)));
				return "predicate " + digit;
			}
			case 4: {
				int order = random.nextInt(3);
				models.forEach(model -> {
					if (order == 0) {
						model.sort().ascending();
					}
					else if (order == 1) {
						model.sort().descending();
					}
					else {
						model.sort().clear();
					}
				});
				return "sort " + common.sort().order();
			}
			case 5:
				if (common.nodes().loaded(path)) {
					String item = "n" + counter.incrementAndGet();
					nodes.computeIfAbsent(path.root() ? "" : path.item(), key -> new ArrayList<>()).add(item);
					models.forEach(model -> model.nodes().add(path, singletonList(item)));
					return "add " + item + " to " + path;
				}
				return "add to unloaded " + path;
			case 6:
				if (!path.root()) {
					nodes.getOrDefault(path.parent().root() ? "" : path.parent().item(), new ArrayList<>()).remove(path.item());
					models.forEach(model -> model.nodes().remove(singletonList(path)));
				}
				return "remove " + path;
			case 7:
				if (!path.root()) {
					String item = "n" + counter.incrementAndGet();
					List<String> siblings = nodes.get(path.parent().root() ? "" : path.parent().item());
					if (siblings != null && siblings.contains(path.item())) {
						siblings.set(siblings.indexOf(path.item()), item);
						nodes.put(item, nodes.getOrDefault(path.item(), new ArrayList<>()));
					}
					models.forEach(model -> model.nodes().replace(path, item));
					return "replace " + path + " with " + item;
				}
				return "replace root";
			case 8: {
				List<Integer> rows = new ArrayList<>();
				for (int j = 0; j < visible.size(); j++) {
					if (random.nextInt(4) == 0) {
						rows.add(j);
					}
				}
				models.forEach(model -> model.selection().indexes().set(rows));
				return "select rows " + rows;
			}
			case 9:
				if (!path.root()) {
					//via the TreeSelectionModel
					if (random.nextBoolean()) {
						common.selection().items().set(singletonList(path));
						swing.selection().setSelectionPath(swing.treePath(path));
					}
					else {
						common.selection().items().add(path);
						swing.selection().addSelectionPath(swing.treePath(path));
					}
				}
				return "select path " + path;
			case 10: {
				List<NodePath<String>> expanded = new ArrayList<>(common.expansion().get());
				Collections.shuffle(expanded, random);
				List<NodePath<String>> toExpand = expanded.subList(0, expanded.size() / 2);
				models.forEach(model -> model.expansion().set(toExpand));
				return "set expansion " + toExpand;
			}
			case 11:
				if (!path.root()) {
					models.forEach(model -> model.nodes().replace(path, new String(path.item())));
				}
				return "replace equal " + path;
			case 12:
				models.forEach(model -> model.nodes().refresh(path));
				return "refresh " + path;
			default:
				common.selection().clear();
				swing.selection().clearSelection();
				return "clear selection";
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

	private SwingFilterTreeModel<String> model() {
		SwingFilterTreeModel<String> model = SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(path -> fetch(path.item()))
						.build();
		model.nodes().loader().async().set(false);

		return model;
	}

	/**
	 * @return fresh instances, equal by {@code equals()}, as a query returns
	 */
	private Collection<String> fetch(String parent) {
		return data.getOrDefault(parent, emptyList()).stream()
						.map(String::new)
						.collect(toList());
	}

	private void assertEvents(String... expected) {
		assertEquals(asList(expected), events);
		events.clear();
	}

	/**
	 * @return a model loading asynchronously, its roots loaded
	 */
	private SwingFilterTreeModel<String> asyncModel(BlockingChildren children) throws Exception {
		AtomicReference<SwingFilterTreeModel<String>> reference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> reference.set(SwingFilterTreeModel.builder()
						.roots(() -> fetch(""))
						.children(children)
						.onLoadException(exception -> {})
						.build()));
		SwingFilterTreeModel<String> model = reference.get();
		SwingUtilities.invokeAndWait(model.nodes()::refresh);
		awaitIdle(model);

		return model;
	}

	/**
	 * Waits until no load is in progress, the results applied on the dispatch thread.
	 */
	private static void awaitIdle(SwingFilterTreeModel<String> model) throws Exception {
		AtomicBoolean active = new AtomicBoolean(true);
		long timeout = System.currentTimeMillis() + 10_000;
		while (active.get() && System.currentTimeMillis() < timeout) {
			SwingUtilities.invokeAndWait(() -> active.set(model.nodes().loader().active().is()));
			if (active.get()) {
				Thread.sleep(10);
			}
		}
		assertFalse(active.get(), "Loading did not finish");
	}

	/**
	 * Loads the children of an item, blocking until released, once, and failing then when told to.
	 */
	private final class BlockingChildren implements Function<NodePath<String>, Collection<String>> {

		private volatile @Nullable String blocking;
		private volatile CountDownLatch blocked = new CountDownLatch(1);
		private volatile CountDownLatch release = new CountDownLatch(1);
		private volatile boolean fail = false;

		@Override
		public Collection<String> apply(NodePath<String> path) {
			if (path.item().equals(blocking)) {
				blocking = null;
				blocked.countDown();
				try {
					release.await(10, SECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				if (fail) {
					throw new IllegalStateException("Failed to load " + path);
				}
			}

			return fetch(path.item());
		}

		private void block(String item) {
			blocked = new CountDownLatch(1);
			release = new CountDownLatch(1);
			blocking = item;
		}

		private void awaitBlocked() throws InterruptedException {
			assertTrue(blocked.await(10, SECONDS));
		}

		private void release() {
			release.countDown();
		}
	}

	private static NodePath<String> path(String... items) {
		return nodePath(asList(items));
	}

	private final class RecordingListener implements TreeModelListener {

		private final String name;

		private RecordingListener(String name) {
			this.name = name;
		}

		@Override
		public void treeNodesChanged(TreeModelEvent e) {
			record("changed", e);
		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {
			record("inserted", e);
		}

		@Override
		public void treeNodesRemoved(TreeModelEvent e) {
			record("removed", e);
		}

		@Override
		public void treeStructureChanged(TreeModelEvent e) {
			events.add(name + " structureChanged [" + e.getTreePath().getLastPathComponent() + "]");
		}

		private void record(String type, TreeModelEvent e) {
			events.add(name + " " + type + " [" + e.getTreePath().getLastPathComponent() + "] "
							+ Arrays.toString(e.getChildIndices()) + " " + Arrays.toString(e.getChildren()));
		}
	}
}
