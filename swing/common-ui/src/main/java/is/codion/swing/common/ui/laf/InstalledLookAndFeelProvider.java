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
package is.codion.swing.common.ui.laf;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Collection;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * <p>Provides the installed look and feels.
 * <p>Note that Nimbus and Motif are excluded.
 */
public final class InstalledLookAndFeelProvider implements LookAndFeelProvider {

	@Override
	public Collection<LookAndFeelEnabler> get() {
		return Stream.of(UIManager.getInstalledLookAndFeels())
						.filter(InstalledLookAndFeelProvider::included)
						.map(DefaultLookAndFeelEnabler::new)
						.collect(toList());
	}

	private static boolean included(LookAndFeelInfo lookAndFeelInfo) {
		return !lookAndFeelInfo.getClassName().contains("Motif") && !lookAndFeelInfo.getName().contains("Nimbus");
	}
}
