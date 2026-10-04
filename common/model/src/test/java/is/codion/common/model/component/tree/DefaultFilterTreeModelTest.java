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
package is.codion.common.model.component.tree;

import is.codion.common.model.CancelException;
import is.codion.common.model.component.tree.FilterTreeModel.NodesListener;
import is.codion.common.model.filter.SortOrder;
import is.codion.common.model.selection.MultiSelection;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the UI-agnostic logic of {@link FilterTreeModel}, loading synchronously, since no dispatch context is bound.
 * The tree:
 * <pre>
 * a
 *   a1
 *     a11
 *   a2
 * b
 * c
 *   c1
 * </pre>
 */
final class DefaultFilterTreeModelTest {

	private static final NodePath<Item> ROOT = nodePath();

	//the children by parent id, "" for the roots, returned as fresh instances on each call
	private final Map<String, List<Item>> data = new HashMap<>();
	private final Map<String, AtomicInteger> calls = new HashMap<>();
	private final List<String> events = new ArrayList<>();

	@BeforeEach
	void setUp() {
		data.put("", items("a", "b", "c"));
		data.put("a", items("a1", "a2"));
		data.put("a1", items("a11"));
		data.put("c", items("c1"));
	}

	@Test
	void nodePaths() {
		NodePath<String> root = nodePath();
		assertTrue(root.root());
		assertEquals(0, root.depth());
		assertTrue(root.items().isEmpty());
		assertThrows(IllegalStateException.class, root::item);
		assertThrows(IllegalStateException.class, root::parent);
		NodePath<String> ab = root.child("a").child("b");
		assertEquals(nodePath(asList("a", "b")), ab);
		assertEquals(nodePath(asList("a", "b")).hashCode(), ab.hashCode());
		assertNotEquals(nodePath(asList("b", "a")), ab);
		assertEquals("b", ab.item());
		assertEquals(2, ab.depth());
		assertEquals(nodePath(singletonList("a")), ab.parent());
		assertSame(root, ab.parent().parent());
		assertTrue(root.contains(ab));
		assertTrue(ab.parent().contains(ab));
		assertTrue(ab.contains(ab));
		assertFalse(ab.contains(ab.parent()));
		assertFalse(nodePath(singletonList("b")).contains(ab));
		assertEquals("a / b", ab.toString());
		assertSame(root, nodePath(emptyList()));
		assertThrows(NullPointerException.class, () -> nodePath(asList("a", null)));
		assertThrows(UnsupportedOperationException.class, () -> ab.items().add("c"));
		//the same item may repeat
		NodePath<String> aa = root.child("a").child("a");
		assertEquals(2, aa.depth());
		assertNotEquals(aa.parent(), aa);
	}

	@Test
	void lazyLoading() {
		FilterTreeModel<Item> model = model();
		assertFalse(model.nodes().loaded(ROOT));
		assertTrue(model.visible().get().isEmpty());
		model.nodes().refresh();
		assertTrue(model.nodes().loaded(ROOT));
		assertEquals(paths("a", "b", "c"), model.visible().get());
		assertEquals(0, calls("a"));
		assertFalse(model.nodes().loaded(path("a")));
		assertFalse(model.nodes().leaf(path("a")));
		assertTrue(model.nodes().children(path("a")).isEmpty());

		model.expansion().expand(path("a"));
		assertEquals(1, calls("a"));
		assertTrue(model.nodes().loaded(path("a")));
		assertEquals(asList(path("a", "a1"), path("a", "a2")), model.nodes().children(path("a")));
		assertEquals(asList(path("a"), path("a", "a1"), path("a", "a2"), path("b"), path("c")), model.visible().get());
		//not loaded until expanded
		assertEquals(0, calls("a1"));

		model.expansion().collapse(path("a"));
		assertEquals(paths("a", "b", "c"), model.visible().get());
		model.expansion().expand(path("a"));
		//loaded once
		assertEquals(1, calls("a"));
		assertEquals(5, model.visible().size());

		//expanding a path expands and loads its ancestors
		model.expansion().expand(path("c", "c1"));
		assertTrue(model.expansion().expanded(path("c")));
		assertTrue(model.nodes().loaded(path("c")));
		assertTrue(model.nodes().loaded(path("c", "c1")));
		assertTrue(model.nodes().leaf(path("c", "c1")));
	}

	@Test
	void refreshOnBuild() {
		FilterTreeModel<Item> model = builder()
						.refresh(true)
						.build();
		assertEquals(paths("a", "b", "c"), model.visible().get());
	}

	@Test
	void leaf() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("b"));
		//loaded without children
		assertTrue(model.nodes().leaf(path("b")));
		assertFalse(model.nodes().leaf(ROOT));
		assertTrue(model.nodes().leaf(path("x")));

		FilterTreeModel<Item> withLeaf = builder()
						.leaf(path -> !data.containsKey(path.item().id))
						.build();
		withLeaf.nodes().refresh();
		assertFalse(withLeaf.nodes().leaf(path("a")));
		assertTrue(withLeaf.nodes().leaf(path("b")));
		assertFalse(withLeaf.nodes().leaf(path("c")));
	}

	@Test
	void leafNotLoaded() {
		FilterTreeModel<Item> model = builder()
						.leaf(path -> !data.containsKey(path.item().id))
						.build();
		model.nodes().refresh();
		//neither when expanded nor when refreshed, the children function not called for it
		model.expansion().expand(path("b"));
		model.nodes().refresh(path("b"));
		model.expansion().set(asList(path("a"), path("a", "a2"), path("b")));
		model.nodes().refresh();
		assertEquals(0, calls("b"));
		assertEquals(0, calls("a2"));
		assertFalse(model.nodes().loaded(path("b")));
		assertFalse(model.nodes().loaded(path("a", "a2")));
		//the others are
		assertEquals(2, calls("a"));
		assertTrue(model.nodes().loaded(path("a")));
	}

	@Test
	void refresh() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("a", "a1"));
		model.nodes().refresh(path("c"));//loaded, not expanded
		assertEquals(1, calls("a"));
		assertEquals(1, calls("a1"));
		assertEquals(1, calls("c"));
		model.selection().items().set(singletonList(path("a", "a1")));
		NodePath<Item> selected = model.selection().item().getOrThrow();
		List<NodePath<Item>> selectedItems = new ArrayList<>();
		List<List<Integer>> selectedIndexes = new ArrayList<>();
		model.selection().item().addConsumer(selectedItems::add);
		model.selection().indexes().addConsumer(selectedIndexes::add);

		rename("a1", "A1");
		data.put("a", items("a1", "a2", "a3"));
		data.get("a").get(0).name = "A1";
		model.nodes().refresh();
		//every loaded node reloaded, the unloaded not
		assertEquals(2, calls(""));
		assertEquals(2, calls("a"));
		assertEquals(2, calls("a1"));
		assertEquals(2, calls("c"));
		assertEquals(0, calls("b"));
		assertEquals(0, calls("c1"));
		//expansion kept
		assertTrue(model.expansion().expanded(path("a", "a1")));
		assertEquals(asList(path("a"), path("a", "a1"), path("a", "a1", "a11"),
						path("a", "a2"), path("a", "a3"), path("b"), path("c")), model.visible().get());
		//the selection kept, the selected path holding the fresh instance
		NodePath<Item> refreshed = model.selection().item().getOrThrow();
		assertEquals(selected, refreshed);
		assertNotSame(selected, refreshed);
		assertEquals("A1", refreshed.item().name);
		assertEquals(singletonList(refreshed), selectedItems);
		//the index unchanged
		assertTrue(selectedIndexes.isEmpty());
		//the descendants' paths hold the fresh ancestors
		NodePath<Item> a11 = model.nodes().children(refreshed).get(0);
		assertSame(refreshed.item(), a11.parent().item());
		assertSame(model.visible().get(1), refreshed);
		//as do the expanded paths
		assertEquals(asList(model.visible().get(0), refreshed), new ArrayList<>(model.expansion().get()));
		model.expansion().get().forEach(expanded ->
						assertSame(model.visible().get(model.visible().indexOf(expanded)), expanded));
	}

	@Test
	void refreshRemoved() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("a", "a1"));
		model.selection().items().set(asList(path("a", "a1", "a11"), path("b")));
		data.put("a", items("a2"));
		model.nodes().refresh();
		assertFalse(model.nodes().contains(path("a", "a1")));
		assertFalse(model.nodes().contains(path("a", "a1", "a11")));
		assertFalse(model.expansion().get().contains(path("a", "a1")));
		assertEquals(singletonList(path("b")), model.selection().items().get());
		//a1 back, its expansion forgotten
		data.put("a", items("a1", "a2"));
		model.nodes().refresh(path("a"));
		assertFalse(model.expansion().expanded(path("a", "a1")));
		assertFalse(model.nodes().loaded(path("a", "a1")));
	}

	@Test
	void refreshIgnoresUnknownPath() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh(path("a"));
		assertEquals(0, calls("a"));
	}

	@Test
	void filter() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().set(asList(path("a"), path("a", "a1"), path("c")));
		assertEquals(7, model.visible().size());
		model.selection().items().set(asList(path("b"), path("a", "a2")));
		model.nodes().predicate().set(path -> path.item().id.equals("a11"));
		//the match keeps its ancestors
		assertEquals(asList(path("a"), path("a", "a1"), path("a", "a1", "a11")), model.visible().get());
		assertTrue(model.nodes().included(path("a")));
		assertFalse(model.nodes().included(path("b")));
		assertTrue(model.nodes().included(ROOT));
		//filtered nodes stay loaded
		assertTrue(model.nodes().contains(path("c", "c1")));
		assertTrue(model.nodes().loaded(path("c")));
		assertFalse(model.nodes().included(path("c", "c1")));
		//filtered nodes dropped from the selection
		assertTrue(model.selection().items().get().isEmpty());
		model.nodes().predicate().set(path -> path.item().id.equals("c1"));
		assertEquals(asList(path("c"), path("c", "c1")), model.visible().get());
		model.nodes().predicate().clear();
		assertEquals(7, model.visible().size());
		assertEquals(0, calls("b"));
		assertEquals(1, calls("c"));
	}

	@Test
	void filterExternalState() {
		List<String> hidden = new ArrayList<>();
		FilterTreeModel<Item> model = builder()
						.included(path -> !hidden.contains(path.item().id))
						.refresh(true)
						.build();
		assertEquals(paths("a", "b", "c"), model.visible().get());
		hidden.add("b");
		model.nodes().filter();
		assertEquals(paths("a", "c"), model.visible().get());
		//new nodes filtered
		data.put("", items("a", "b", "c", "d"));
		hidden.add("d");
		model.nodes().refresh();
		assertEquals(paths("a", "c"), model.visible().get());
		model.nodes().add(ROOT, items("e"));
		assertEquals(paths("a", "c", "e"), model.visible().get());
		//the nodes are not tested again until filtered, only the ones loaded, added or replaced
		hidden.add("c");
		hidden.add("a2");
		model.expansion().expand(path("a"));
		model.expansion().collapse(path("a"));
		model.expansion().expand(path("a"));
		assertEquals(asList(path("a"), path("a", "a1"), path("c"), path("e")), model.visible().get());
		model.nodes().filter();
		assertEquals(asList(path("a"), path("a", "a1"), path("e")), model.visible().get());
	}

	@Test
	void sort() {
		FilterTreeModel<Item> model = builder()
						.comparator(comparing(item -> item.name))
						.build();
		assertTrue(model.sort().sorted());
		assertEquals(SortOrder.ASCENDING, model.sort().order());
		data.put("", items("c", "a", "b"));
		data.put("a", items("a2", "a1"));
		model.nodes().refresh();
		model.expansion().expand(path("a"));
		model.selection().items().set(singletonList(path("a", "a2")));
		assertEquals(asList(path("a"), path("a", "a1"), path("a", "a2"), path("b"), path("c")), model.visible().get());
		model.sort().descending();
		assertEquals(asList(path("c"), path("b"), path("a"), path("a", "a2"), path("a", "a1")), model.visible().get());
		assertEquals(singletonList(path("a", "a2")), model.selection().items().get());
		assertEquals(3, model.selection().index().get());
		model.sort().clear();
		assertFalse(model.sort().sorted());
		assertEquals(SortOrder.UNSORTED, model.sort().order());
		//the children function's order
		assertEquals(asList(path("c"), path("a"), path("a", "a2"), path("a", "a1"), path("b")), model.visible().get());

		FilterTreeModel<Item> unsorted = model();
		assertFalse(unsorted.sort().sorted());
		unsorted.sort().ascending();
		assertFalse(unsorted.sort().sorted());
		assertEquals(SortOrder.UNSORTED, unsorted.sort().order());
	}

	@Test
	void expansion() {
		FilterTreeModel<Item> model = model();
		List<NodePath<Item>> expanded = new ArrayList<>();
		List<NodePath<Item>> collapsed = new ArrayList<>();
		model.expansion().expanded().addConsumer(expanded::add);
		model.expansion().collapsed().addConsumer(collapsed::add);
		//intent before loading, the root loaded on refresh only
		model.expansion().set(asList(path("a"), path("a", "a1")));
		assertEquals(0, calls("a"));
		assertEquals(asList(path("a"), path("a", "a1")), expanded);
		model.nodes().refresh();
		//cascading loads
		assertTrue(model.nodes().loaded(path("a")));
		assertTrue(model.nodes().loaded(path("a", "a1")));
		assertEquals(asList(path("a"), path("a", "a1"), path("a", "a1", "a11"), path("a", "a2"), path("b"), path("c")),
						model.visible().get());

		model.expansion().collapse(path("a"));
		assertEquals(singletonList(path("a")), collapsed);
		//remembered
		assertTrue(model.expansion().get().contains(path("a", "a1")));
		assertFalse(model.expansion().expanded(path("a", "a1")));
		assertEquals(paths("a", "b", "c"), model.visible().get());
		model.expansion().expand(path("a"));
		assertEquals(6, model.visible().size());

		//remembered expansion below a collapsed node waits for it to be expanded
		expanded.clear();
		model.expansion().set(singletonList(path("c", "c1")));
		assertFalse(model.nodes().loaded(path("c")));
		assertEquals(singletonList(path("c", "c1")), expanded);
		assertEquals(3, collapsed.size());
		assertTrue(collapsed.subList(1, 3).containsAll(asList(path("a"), path("a", "a1"))));
		model.expansion().expand(path("c"));
		assertTrue(model.nodes().loaded(path("c")));
		assertTrue(model.nodes().loaded(path("c", "c1")));
		//expanding the root does nothing
		model.expansion().expand(ROOT);
		assertTrue(model.expansion().expanded(ROOT));
		assertFalse(model.expansion().get().contains(ROOT));
	}

	@Test
	void expansionPruned() {
		FilterTreeModel<Item> model = model();
		//intent before loading, the paths naming nodes which turn out not to exist dropped as the nodes above them load
		model.expansion().set(asList(path("x"), path("a"), path("a", "x"), path("a", "x", "y"), path("c", "x")));
		assertEquals(5, model.expansion().get().size());
		model.nodes().refresh();
		//c not expanded, so not loaded
		assertEquals(asList(path("a"), path("c", "x")), new ArrayList<>(model.expansion().get()));
		model.expansion().expand(path("c"));
		assertEquals(asList(path("a"), path("c")), new ArrayList<>(model.expansion().get()));
		//known not to exist, ignored
		model.expansion().expand(path("a", "x"));
		model.expansion().expand(path("x", "y"));
		assertEquals(asList(path("a"), path("c")), new ArrayList<>(model.expansion().get()));
		//b not loaded
		model.expansion().set(asList(path("a"), path("b", "x"), path("x"), path("c", "x")));
		assertEquals(asList(path("a"), path("b", "x")), new ArrayList<>(model.expansion().get()));
	}

	@Test
	void collapseSelectsCollapsed() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("a", "a1"));
		model.selection().items().set(asList(path("a", "a1", "a11"), path("a", "a2"), path("b")));
		model.expansion().collapse(path("a", "a1"));
		assertEquals(asList(path("a", "a1"), path("a", "a2"), path("b")), model.selection().items().get());
		model.expansion().collapse(path("a"));
		assertEquals(asList(path("a"), path("b")), model.selection().items().get());
	}

	@Test
	void selectionIndexes() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.selection().items().set(singletonList(path("c")));
		assertEquals(2, model.selection().index().get());
		AtomicInteger itemEvents = new AtomicInteger();
		AtomicInteger indexEvents = new AtomicInteger();
		model.selection().item().addListener(itemEvents::incrementAndGet);
		model.selection().index().addListener(indexEvents::incrementAndGet);
		model.expansion().expand(path("a"));
		assertEquals(4, model.selection().index().get());
		assertEquals(path("c"), model.selection().item().get());
		assertEquals(0, itemEvents.get());
		assertEquals(1, indexEvents.get());
		//a hidden path can not be selected
		model.selection().items().set(singletonList(path("c", "c1")));
		assertTrue(model.selection().items().get().isEmpty());
	}

	@Test
	void selectionListeners() {
		AtomicInteger selectionChanged = new AtomicInteger();
		List<NodePath<Item>> selectedItem = new ArrayList<>();
		List<List<NodePath<Item>>> selectedItems = new ArrayList<>();
		List<Integer> selectedIndex = new ArrayList<>();
		List<List<Integer>> selectedIndexes = new ArrayList<>();
		FilterTreeModel<Item> model = builder()
						.onSelectionChanged(selectionChanged::incrementAndGet)
						.onSelectedItem(selectedItem::add)
						.onSelectedItems(selectedItems::add)
						.onSelectedIndex(selectedIndex::add)
						.onSelectedIndexes(selectedIndexes::add)
						.refresh(true)
						.build();
		model.selection().items().set(asList(path("b"), path("c")));
		assertEquals(1, selectionChanged.get());
		assertEquals(singletonList(path("b")), selectedItem);
		assertEquals(singletonList(asList(path("b"), path("c"))), selectedItems);
		assertEquals(singletonList(1), selectedIndex);
		assertEquals(singletonList(asList(1, 2)), selectedIndexes);
	}

	@Test
	void visibleNotifiedOnce() {
		FilterTreeModel<Item> model = model();
		AtomicInteger notifications = new AtomicInteger();
		model.visible().addListener(notifications::incrementAndGet);
		model.nodes().refresh();
		assertEquals(1, notifications.get());
		model.expansion().expand(path("a"));
		assertEquals(2, notifications.get());
		//no change
		model.nodes().filter();
		model.expansion().expand(path("a"));
		assertEquals(2, notifications.get());
		//fresh instances
		model.nodes().refresh();
		assertEquals(3, notifications.get());
	}

	@Test
	void mutation() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("a"));
		//not loaded, ignored
		model.nodes().add(path("b"), items("b1"));
		assertFalse(model.nodes().contains(path("b", "b1")));
		model.nodes().add(path("a"), items("a3"));
		assertEquals(asList(path("a", "a1"), path("a", "a2"), path("a", "a3")), model.nodes().children(path("a")));
		assertFalse(model.nodes().loaded(path("a", "a3")));
		assertThrows(IllegalArgumentException.class, () -> model.nodes().add(path("a"), items("a1")));
		assertThrows(IllegalArgumentException.class, () -> model.nodes().add(path("a"), items("a4", "a4")));

		model.selection().items().set(singletonList(path("a", "a2")));
		model.nodes().remove(asList(path("a", "a2"), path("x")));
		assertFalse(model.nodes().contains(path("a", "a2")));
		assertTrue(model.selection().items().get().isEmpty());
		assertThrows(IllegalArgumentException.class, () -> model.nodes().remove(singletonList(ROOT)));

		//an equal item replaces the instance
		model.selection().items().set(singletonList(path("a", "a1")));
		Item replacement = new Item("a1", "A1");
		model.nodes().replace(path("a", "a1"), replacement);
		assertSame(replacement, model.selection().item().getOrThrow().item());
		assertSame(replacement, model.nodes().children(path("a")).get(0).item());
		assertThrows(IllegalArgumentException.class, () -> model.nodes().replace(ROOT, replacement));
		assertThrows(IllegalArgumentException.class, () -> model.nodes().replace(path("a", "a1"), new Item("a3", "a3")));
		model.nodes().replace(path("x"), replacement);
	}

	@Test
	void selectionListenerThrowing() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("a"));
		model.selection().items().set(singletonList(path("a", "a1")));
		AtomicInteger notifications = new AtomicInteger();
		List<NodePath<Item>> collapsed = new ArrayList<>();
		model.visible().addListener(notifications::incrementAndGet);
		model.expansion().collapsed().addConsumer(collapsed::add);
		//a listener throwing once the selection has been restored, as an editor vetoing the change of its entity
		//does, does not prevent the collapse, which the visible nodes and the expansion still notify
		model.selection().item().addListener(() -> {
			throw new CancelException();
		});
		assertThrows(CancelException.class, () -> model.expansion().collapse(path("a")));
		assertEquals(paths("a", "b", "c"), model.visible().get());
		assertEquals(singletonList(path("a")), model.selection().items().get());
		assertEquals(1, notifications.get());
		assertEquals(singletonList(path("a")), collapsed);
	}

	@Test
	void restoringSelectionNotChanging() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("a"));
		model.selection().items().set(asList(path("a", "a1"), path("b")));
		AtomicInteger changing = new AtomicInteger();
		model.selection().changing().addListener(changing::incrementAndGet);
		//the selection following the nodes is not a change by request: the rows of the selected nodes shifting,
		model.expansion().expand(path("a", "a1"));
		assertEquals(asList(path("a", "a1"), path("b")), model.selection().items().get());
		//a selected node being removed,
		model.nodes().remove(singletonList(path("b")));
		//or replaced by the ancestor collapsed
		model.expansion().collapse(path("a"));
		assertEquals(singletonList(path("a")), model.selection().items().get());
		assertEquals(0, changing.get());
		model.selection().items().set(singletonList(path("c")));
		assertEquals(1, changing.get());
	}

	@Test
	void mutationWhileRestoringSelection() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("a"));
		model.selection().items().set(asList(path("a", "a1"), path("b")));
		AtomicInteger notifications = new AtomicInteger();
		model.visible().addListener(notifications::incrementAndGet);
		AtomicBoolean add = new AtomicBoolean(true);
		//a mutation made by a selection listener while a mutation restores the selection becomes part of it
		model.selection().adjusting().addListener(() -> {
			if (add.getAndSet(false)) {
				model.nodes().add(ROOT, items("d"));
			}
		});
		model.nodes().remove(singletonList(path("a", "a1")));
		assertEquals(asList(path("a"), path("a", "a2"), path("b"), path("c"), path("d")), model.visible().get());
		assertEquals(singletonList(path("b")), model.selection().items().get());
		assertEquals(1, notifications.get());
	}

	@Test
	void replaceIdentity() {
		FilterTreeModel<Item> model = model();
		model.nodes().refresh();
		model.expansion().expand(path("a", "a1"));
		model.selection().items().set(singletonList(path("a", "a1", "a11")));
		model.nodes().replace(path("a"), new Item("x", "x"));
		assertFalse(model.nodes().contains(path("a")));
		assertTrue(model.nodes().contains(path("x", "a1", "a11")));
		assertTrue(model.expansion().expanded(path("x", "a1")));
		assertFalse(model.expansion().get().contains(path("a")));
		assertEquals(singletonList(path("x", "a1", "a11")), model.selection().items().get());
		assertEquals(asList(path("x"), path("x", "a1"), path("x", "a1", "a11"), path("x", "a2"), path("b"), path("c")),
						model.visible().get());
	}

	@Test
	void invalidChildren() {
		List<Exception> exceptions = new ArrayList<>();
		FilterTreeModel<Item> model = model();
		model.nodes().loader().exception().addConsumer(exceptions::add);
		data.put("", items("a", "a"));
		assertThrows(IllegalArgumentException.class, model.nodes()::refresh);
		assertEquals(1, exceptions.size());
		assertFalse(model.nodes().loaded(ROOT));
		assertFalse(model.nodes().loader().active().is());
		data.put("", asList(new Item("a", "a"), null));
		assertThrows(IllegalArgumentException.class, model.nodes()::refresh);

		List<Exception> handled = new ArrayList<>();
		FilterTreeModel<Item> handling = builder()
						.onLoadException(handled::add)
						.build();
		handling.nodes().refresh();
		assertEquals(1, handled.size());
	}

	@Test
	void failedLoad() {
		Set<String> failing = new HashSet<>();
		List<Exception> exceptions = new ArrayList<>();
		List<NodePath<Item>> collapsed = new ArrayList<>();
		FilterTreeModel<Item> model = failing(failing)
						.onLoadException(exceptions::add)
						.build();
		model.expansion().collapsed().addConsumer(collapsed::add);
		model.nodes().refresh();
		failing.add("a");
		model.expansion().expand(path("a"));
		assertEquals(1, exceptions.size());
		//collapsed, to be loaded when expanded again
		assertFalse(model.expansion().expanded(path("a")));
		assertEquals(singletonList(path("a")), collapsed);
		assertFalse(model.nodes().loaded(path("a")));
		assertFalse(model.nodes().loader().active().is());
		//not loaded again by other mutations
		model.expansion().expand(path("c"));
		model.nodes().filter();
		model.expansion().collapse(path("c"));
		assertTrue(model.nodes().loaded(path("c")));
		assertEquals(1, calls("a"));
		assertEquals(1, exceptions.size());
		failing.clear();
		model.expansion().expand(path("a"));
		assertEquals(2, calls("a"));
		assertTrue(model.nodes().loaded(path("a")));
		//a failed refresh of a loaded node leaves it as it was
		failing.add("a");
		model.nodes().refresh(path("a"));
		assertEquals(2, exceptions.size());
		assertTrue(model.expansion().expanded(path("a")));
		assertEquals(2, model.nodes().children(path("a")).size());

		//rethrown by default, the nodes following the failed one loaded nonetheless
		FilterTreeModel<Item> rethrowing = failing(failing).build();
		rethrowing.nodes().refresh();
		assertThrows(IllegalStateException.class, () -> rethrowing.expansion().set(asList(path("a"), path("c"))));
		assertFalse(rethrowing.expansion().expanded(path("a")));
		assertTrue(rethrowing.nodes().loaded(path("c")));
		rethrowing.expansion().collapse(path("c"));
		rethrowing.expansion().expand(path("c"));
	}

	@Test
	void repeatedItems() {
		//an infinite tree, each node its own parent's child, as a foreign key referencing its own entity
		FilterTreeModel<String> model = FilterTreeModel.builder()
						.roots(() -> singletonList("a"))
						.children(path -> singletonList("a"))
						.build();
		model.nodes().refresh();
		NodePath<String> deep = nodePath(asList("a", "a", "a", "a"));
		model.expansion().expand(deep);
		assertTrue(model.nodes().loaded(deep));
		assertEquals(5, model.visible().size());
		assertEquals(deep, model.visible().get(3));
	}

	@Test
	void events() {
		FilterTreeModel<Item> model = new TestBuilder(roots(), children())
						.build(new RecordingListener());
		model.nodes().refresh();
		assertEvents("inserted [] [0, 1, 2]");
		model.expansion().expand(path("a"));
		assertEvents("inserted [a] [0, 1]");
		//loaded without children, now a leaf
		model.expansion().expand(path("b"));
		assertEvents("changed [] [1]");
		model.nodes().add(path("a"), items("a3"));
		assertEvents("inserted [a] [2]");
		model.nodes().remove(asList(path("a", "a1"), path("a", "a3")));
		assertEvents("removed [a] [0, 2] [a / a1, a / a3]");
		model.nodes().replace(path("a", "a2"), new Item("a2", "A2"));
		assertEvents("changed [a] [0]");
		model.nodes().replace(path("a", "a2"), new Item("a4", "a4"));
		assertEvents("structureChanged [a]");
		//refresh, the root's children unchanged but for the instances, a's reordered
		data.put("a", items("a2", "a1"));
		model.nodes().refresh();
		assertEvents("changed [] [0, 1, 2]", "structureChanged [a]");
		//topmost only
		model.nodes().predicate().set(path -> !path.item().id.equals("a1"));
		assertEvents("removed [a] [1] [a / a1]");
		model.nodes().predicate().set(path -> path.item().id.equals("a2"));
		assertEvents("removed [] [1, 2] [b, c]");
		model.nodes().predicate().clear();
		model.nodes().refresh(path("x"));
		events.clear();
		model.expansion().collapse(path("a"));
		model.expansion().expand(path("a"));
		assertEvents();
	}

	@Test
	void eventsWithSort() {
		FilterTreeModel<Item> model = new TestBuilder(roots(), children())
						.comparator(comparing(item -> item.name))
						.build(new RecordingListener());
		model.nodes().refresh();
		model.expansion().expand(path("a"));
		events.clear();
		model.sort().descending();
		assertEvents("structureChanged []");
	}

	private void assertEvents(String... expected) {
		assertEquals(asList(expected), events);
		events.clear();
	}

	private FilterTreeModel<Item> model() {
		return builder().build();
	}

	private FilterTreeModel.Builder<Item, ?> builder() {
		return FilterTreeModel.builder()
						.roots(roots())
						.children(children());
	}

	/**
	 * @return a builder for a model failing to load the children of the items with the given ids
	 */
	private FilterTreeModel.Builder<Item, ?> failing(Set<String> failing) {
		return FilterTreeModel.builder()
						.roots(roots())
						.children(path -> {
							List<Item> children = fetch(path.item().id);
							if (failing.contains(path.item().id)) {
								throw new IllegalStateException("Failed to load " + path);
							}

							return children;
						});
	}

	private Supplier<Collection<Item>> roots() {
		return () -> fetch("");
	}

	private Function<NodePath<Item>, Collection<Item>> children() {
		return path -> fetch(path.item().id);
	}

	private List<Item> fetch(String parent) {
		calls.computeIfAbsent(parent, id -> new AtomicInteger()).incrementAndGet();

		return data.getOrDefault(parent, emptyList()).stream()
						.map(item -> item == null ? null : new Item(item.id, item.name))
						.collect(toList());
	}

	private void rename(String id, String name) {
		data.values().forEach(items -> items.stream()
						.filter(item -> item != null && item.id.equals(id))
						.forEach(item -> item.name = name));
	}

	private int calls(String parent) {
		AtomicInteger counter = calls.get(parent);

		return counter == null ? 0 : counter.get();
	}

	private static List<Item> items(String... ids) {
		return Arrays.stream(ids)
						.map(id -> new Item(id, id))
						.collect(toList());
	}

	private static NodePath<Item> path(String... ids) {
		return nodePath(items(ids));
	}

	private static List<NodePath<Item>> paths(String... ids) {
		return Arrays.stream(ids)
						.map(DefaultFilterTreeModelTest::path)
						.collect(toList());
	}

	private final class RecordingListener implements NodesListener<Item> {

		@Override
		public void inserted(NodePath<Item> parent, List<Integer> indexes) {
			events.add("inserted [" + parent + "] " + indexes);
		}

		@Override
		public void removed(NodePath<Item> parent, List<Integer> indexes, List<NodePath<Item>> children) {
			events.add("removed [" + parent + "] " + indexes + " " + children);
		}

		@Override
		public void changed(NodePath<Item> parent, List<Integer> indexes) {
			events.add("changed [" + parent + "] " + indexes);
		}

		@Override
		public void structureChanged(NodePath<Item> path) {
			events.add("structureChanged [" + path + "]");
		}
	}

	private static final class TestBuilder extends AbstractFilterTreeModelBuilder<Item, TestBuilder> {

		private TestBuilder(Supplier<Collection<Item>> roots, Function<NodePath<Item>, Collection<Item>> children) {
			super(roots, children);
		}

		private FilterTreeModel<Item> build(NodesListener<Item> listener) {
			return build(MultiSelection::multiSelection, listener);
		}
	}

	/**
	 * Equal by id, the name a value a refresh may change.
	 */
	private static final class Item {

		private final String id;

		private String name;

		private Item(String id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public boolean equals(@Nullable Object object) {
			return object instanceof Item && ((Item) object).id.equals(id);
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public String toString() {
			return id;
		}
	}
}
