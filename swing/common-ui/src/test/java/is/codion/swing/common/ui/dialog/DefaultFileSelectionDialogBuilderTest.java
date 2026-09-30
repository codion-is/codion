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
package is.codion.swing.common.ui.dialog;

import org.junit.jupiter.api.Test;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class DefaultFileSelectionDialogBuilderTest {

	@Test
	void resetFileFilters() {
		// the chooser is shared between selections
		JFileChooser chooser = new JFileChooser();
		FileFilter acceptAll = chooser.getAcceptAllFileFilter();
		FileFilter patch = new FileNameExtensionFilter("Patch", "patch");
		// the accept all filter kept, as a new JFileChooser offers it, the given filter selected
		DefaultFileSelectionDialogBuilder.resetFileFilters(chooser, singletonList(patch));
		assertArrayEquals(new FileFilter[] {acceptAll, patch}, chooser.getChoosableFileFilters());
		assertSame(patch, chooser.getFileFilter());
		// no filter, the accept all one
		DefaultFileSelectionDialogBuilder.resetFileFilters(chooser, emptyList());
		assertArrayEquals(new FileFilter[] {acceptAll}, chooser.getChoosableFileFilters());
		assertSame(acceptAll, chooser.getFileFilter());
		// the first of several
		FileFilter diff = new FileNameExtensionFilter("Diff", "diff");
		DefaultFileSelectionDialogBuilder.resetFileFilters(chooser, asList(diff, patch));
		assertArrayEquals(new FileFilter[] {acceptAll, diff, patch}, chooser.getChoosableFileFilters());
		assertSame(diff, chooser.getFileFilter());
	}
}
