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

import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.laf.LookAndFeelProviders;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Arrays;
import java.util.Collection;

import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;

/**
 * Provides all available Flat Look and Feels
 */
public final class FlatLookAndFeelProviders implements LookAndFeelProviders {

	public FlatLookAndFeelProviders() {}

	@Override
	public Collection<LookAndFeelProvider> get() {
		return Arrays.asList(
						lookAndFeelProvider(new LookAndFeelInfo("Flat Look & Feel Darcula ", FlatDarculaLaf.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Flat Look & Feel Dark", FlatDarkLaf.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Flat Look & Feel IntelliJ", FlatIntelliJLaf.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Flat Look & Feel Light", FlatLightLaf.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Flat Look & Feel Mac Dark", FlatMacDarkLaf.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Flat Look & Feel Mac Light", FlatMacLightLaf.class.getName()))
		);
	}
}
