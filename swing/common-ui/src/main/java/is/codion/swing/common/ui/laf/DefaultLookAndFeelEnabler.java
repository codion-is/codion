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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import is.codion.swing.common.ui.Utilities;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

class DefaultLookAndFeelEnabler implements LookAndFeelEnabler {

	private static final Consumer<LookAndFeelInfo> DEFAULT_ENABLER = new DefaultEnabler();

	static final Map<String, LookAndFeelEnabler> LOOK_AND_FEEL_PROVIDERS = new HashMap<>();

	static {
		LookAndFeelProvider.instances().forEach(providers ->
						providers.get().forEach(lookAndFeelEnabler -> LOOK_AND_FEEL_PROVIDERS
										.put(requireNonNull(lookAndFeelEnabler).lookAndFeelInfo().getClassName(), lookAndFeelEnabler)));
	}

	private final LookAndFeelInfo lookAndFeelInfo;
	private final Consumer<LookAndFeelInfo> enabler;

	DefaultLookAndFeelEnabler(LookAndFeelInfo lookAndFeelInfo) {
		this(lookAndFeelInfo, DEFAULT_ENABLER);
	}

	DefaultLookAndFeelEnabler(LookAndFeelInfo lookAndFeelInfo, Consumer<LookAndFeelInfo> enabler) {
		this.lookAndFeelInfo = requireNonNull(lookAndFeelInfo);
		this.enabler = requireNonNull(enabler);
	}

	@Override
	public final LookAndFeelInfo lookAndFeelInfo() {
		return lookAndFeelInfo;
	}

	@Override
	public final void enable() {
		enabler.accept(lookAndFeelInfo);
	}

	@Override
	public final LookAndFeel lookAndFeel() {
		try {
			return (LookAndFeel) Class.forName(lookAndFeelInfo.getClassName()).getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final String toString() {
		return lookAndFeelInfo.getName();
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DefaultLookAndFeelEnabler)) {
			return false;
		}

		DefaultLookAndFeelEnabler that = (DefaultLookAndFeelEnabler) obj;

		return lookAndFeelInfo.getClassName().equals(that.lookAndFeelInfo.getClassName());
	}

	@Override
	public final int hashCode() {
		return lookAndFeelInfo().getClassName().hashCode();
	}

	private static final class DefaultEnabler implements Consumer<LookAndFeelInfo> {

		@Override
		public void accept(LookAndFeelInfo lookAndFeelInfo) {
			try {
				UIManager.setLookAndFeel(requireNonNull(lookAndFeelInfo).getClassName());
				Utilities.updateComponentTreeForAllWindows();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
