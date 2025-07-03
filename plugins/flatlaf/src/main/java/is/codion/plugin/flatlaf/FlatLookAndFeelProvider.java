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

import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.scaler.Scaler;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Collection;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.lookAndFeelEnabler;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Provides all available Flat Look and Feels
 */
public final class FlatLookAndFeelProvider implements LookAndFeelProvider {

	private static final Consumer<LookAndFeelInfo> ENABLER = new DefaultEnabler();

	public FlatLookAndFeelProvider() {}

	@Override
	public Collection<LookAndFeelEnabler> get() {
		return unmodifiableList(asList(
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Darcula ", FlatDarculaLaf.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Dark", FlatDarkLaf.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat IntelliJ", FlatIntelliJLaf.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Light", FlatLightLaf.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Mac Dark", FlatMacDarkLaf.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Flat Mac Light", FlatMacLightLaf.class.getName()), ENABLER)
		));
	}

	/**
	 * Requrired since scaling must happen before the look and feel is applied.
	 */
	private static final class DefaultEnabler implements Consumer<LookAndFeelInfo> {

		@Override
		public void accept(LookAndFeelInfo lookAndFeelInfo) {
			try {
				Scaler.instance(lookAndFeelInfo.getClassName()).ifPresent(Scaler::apply);
				UIManager.setLookAndFeel(requireNonNull(lookAndFeelInfo).getClassName());
				Utilities.updateComponentTreeForAllWindows();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
