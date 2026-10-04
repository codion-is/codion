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
package is.codion.manual.swing.common.ui.component.tree;

import is.codion.common.model.component.tree.NodePath;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;
import is.codion.swing.common.ui.component.tree.FilterTree;
import is.codion.swing.common.ui.control.Control;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

final class FilterTreeDemo {

	static void demo() {
		// tag::model[]
		File home = new File(System.getProperty("user.home"));

		SwingFilterTreeModel<File> model =
						SwingFilterTreeModel.builder()
										// The top level, the children of the invisible root
										.roots(() -> files(home))
										// Called when a node is first expanded, off the UI thread,
										// given the path of the node, the items from the top level down
										.children(path -> files(path.item()))
										// A file has no children, so no expand handle. Called on the
										// UI thread each time the tree asks, so it must be fast
										.leaf(path -> path.item().isFile())
										// Sorts siblings
										.comparator(comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
										.refresh(true)
										.build();
		// end::model[]

		// tag::tree[]
		FilterTree<File> tree =
						FilterTree.builder()
										.model(model)
										.formatter(File::getName)
										.doubleClick(Control.command(() ->
														model.selection().item().optional()
																		.map(NodePath::item)
																		.ifPresent(System.out::println)))
										.build();
		// end::tree[]
	}

	static void expansion(SwingFilterTreeModel<File> model) {
		// tag::expansion[]
		List<NodePath<File>> topLevel = model.nodes().children(nodePath());

		// Expanding a node loads its children, the tree follows
		model.expansion().expand(topLevel.get(0));

		// A refresh reloads the loaded nodes, keeping
		// the expansion and selection of the ones remaining
		model.nodes().refresh();

		// The expanded paths, for restoring the expansion later,
		// loading the nodes as needed
		Collection<NodePath<File>> expanded = model.expansion().get();
		model.expansion().set(expanded);
		// end::expansion[]
	}

	static void selection(SwingFilterTreeModel<File> model) {
		// tag::selection[]
		// The selection is over the visible nodes, kept by path
		model.selection().items().addConsumer(selected ->
						selected.forEach(path -> System.out.println(path.item())));

		List<NodePath<File>> topLevel = model.nodes().children(nodePath());
		model.selection().items().set(topLevel);
		// end::selection[]
	}

	static void filter(SwingFilterTreeModel<File> model) {
		// tag::filter[]
		// Includes the matching nodes along with their ancestors,
		// filtering the nodes loaded so far
		model.nodes().predicate().set(path ->
						path.item().getName().endsWith(".java"));

		// Includes all nodes again, without reloading
		model.nodes().predicate().clear();
		// end::filter[]
	}

	// tag::files[]
	private static List<File> files(File directory) {
		File[] files = directory.listFiles(file -> !file.isHidden());

		return files == null ? emptyList() : asList(files);
	}
	// end::files[]
}
