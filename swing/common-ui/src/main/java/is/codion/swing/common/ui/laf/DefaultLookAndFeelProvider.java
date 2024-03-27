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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import is.codion.swing.common.ui.Utilities;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;
import static java.util.Objects.requireNonNull;

final class DefaultLookAndFeelProvider implements LookAndFeelProvider {

	private static final Consumer<LookAndFeelInfo> DEFAULT_ENABLER = new DefaultEnabler();

	static final Map<String, LookAndFeelProvider> LOOK_AND_FEEL_PROVIDERS = new HashMap<>();

	static {
		LookAndFeelProvider crossPlatformProvider = lookAndFeelProvider(new LookAndFeelInfo("Cross Platform", UIManager.getCrossPlatformLookAndFeelClassName()));
		LOOK_AND_FEEL_PROVIDERS.put(crossPlatformProvider.lookAndFeelInfo().getClassName(), crossPlatformProvider);
		LookAndFeelProvider systemProvider = lookAndFeelProvider(new LookAndFeelInfo("System", Utilities.systemLookAndFeelClassName()));
		if (!LOOK_AND_FEEL_PROVIDERS.containsKey(systemProvider.lookAndFeelInfo().getClassName())) {
			LOOK_AND_FEEL_PROVIDERS.put(systemProvider.lookAndFeelInfo().getClassName(), systemProvider);
		}
	}

	private final LookAndFeelInfo lookAndFeelInfo;
	private final Consumer<LookAndFeelInfo> enabler;

	DefaultLookAndFeelProvider(LookAndFeelInfo lookAndFeelInfo) {
		this(lookAndFeelInfo, DEFAULT_ENABLER);
	}

	DefaultLookAndFeelProvider(LookAndFeelInfo lookAndFeelInfo, Consumer<LookAndFeelInfo> enabler) {
		this.lookAndFeelInfo = requireNonNull(lookAndFeelInfo);
		this.enabler = requireNonNull(enabler);
	}

	@Override
	public LookAndFeelInfo lookAndFeelInfo() {
		return lookAndFeelInfo;
	}

	@Override
	public void enable() {
		enabler.accept(lookAndFeelInfo);
	}

	public LookAndFeel lookAndFeel() throws Exception {
		return (LookAndFeel) Class.forName(lookAndFeelInfo.getClassName()).getDeclaredConstructor().newInstance();
	}

	@Override
	public String toString() {
		return lookAndFeelInfo.getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DefaultLookAndFeelProvider)) {
			return false;
		}

		DefaultLookAndFeelProvider that = (DefaultLookAndFeelProvider) obj;

		return lookAndFeelInfo.getClassName().equals(that.lookAndFeelInfo.getClassName());
	}

	@Override
	public int hashCode() {
		return lookAndFeelInfo().getClassName().hashCode();
	}

	private static final class DefaultEnabler implements Consumer<LookAndFeelInfo> {

		@Override
		public void accept(LookAndFeelInfo lookAndFeelInfo) {
			try {
				UIManager.setLookAndFeel(requireNonNull(lookAndFeelInfo).getClassName());
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
