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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.flatlaf.intellij;

import is.codion.swing.common.ui.laf.LookAndFeelEnabler;

import com.formdev.flatlaf.IntelliJTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlatLookAndFeelIntelliJThemesTest {

	@Test
	void test() {
		FlatLookAndFeelIntelliJThemes.get().forEach(theme -> theme.lookAndFeel().getDefaults());
	}

	@Test
	void allRegistered() throws IOException, URISyntaxException {
		Set<String> registered = FlatLookAndFeelIntelliJThemes.get().stream()
						.map(LookAndFeelEnabler::lookAndFeelInfo)
						.map(info -> info.getClassName())
						.collect(toSet());
		Path themes = Paths.get(FlatLookAndFeelIntelliJThemes.class.getResource("themes").toURI());
		try (Stream<Path> files = Files.walk(themes)) {
			List<String> unregistered = files
							.map(themes::relativize)
							.map(Path::toString)
							.filter(file -> file.endsWith(".class") && !file.contains("$"))
							.map(file -> FlatLookAndFeelIntelliJThemes.class.getPackage().getName() + ".themes."
											+ file.substring(0, file.length() - ".class".length()).replace(themes.getFileSystem().getSeparator(), "."))
							.filter(FlatLookAndFeelIntelliJThemesTest::theme)
							.filter(className -> !registered.contains(className))
							.sorted()
							.collect(toList());
			assertEquals(emptyList(), unregistered);
		}
	}

	private static boolean theme(String className) {
		try {
			return IntelliJTheme.ThemeLaf.class.isAssignableFrom(Class.forName(className));
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
