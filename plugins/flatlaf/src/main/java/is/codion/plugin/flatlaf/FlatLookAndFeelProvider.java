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
package is.codion.plugin.flatlaf;

import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Collection;

import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.lookAndFeelEnabler;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * Provides all available Flat Look and Feels
 */
public final class FlatLookAndFeelProvider implements LookAndFeelProvider {

	public FlatLookAndFeelProvider() {}

	@Override
	public Collection<LookAndFeelEnabler> get() {
		return unmodifiableList(asList(
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Darcula ", FlatDarculaLaf.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Dark", FlatDarkLaf.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat IntelliJ", FlatIntelliJLaf.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Light", FlatLightLaf.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Mac Dark", FlatMacDarkLaf.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Mac Light", FlatMacLightLaf.class.getName()))
		));
	}
}
