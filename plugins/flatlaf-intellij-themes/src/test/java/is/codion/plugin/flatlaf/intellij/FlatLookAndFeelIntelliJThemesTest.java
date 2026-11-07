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

import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryDarcula;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialOceanic;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.IntelliJTheme;
import org.junit.jupiter.api.Test;

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
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

	@Test
	void noTableCellMargins() throws IOException {
		//a theme's table cell margins, meant for its own row height, which FlatLaf drops, are removed from the themes,
		//leaving FlatLaf's own, should a theme imported from upstream bring them back
		Pattern cellMargins = Pattern.compile("\"(cellNoFocusBorder|focusCellHighlightBorder)\"\\s*:\\s*\"\\d+,\\s*\\d+,\\s*\\d+,\\s*\\d+\"");
		Path themes = Paths.get("src/main/resources/is/codion/plugin/flatlaf/intellij/themes");
		try (Stream<Path> files = Files.walk(themes)) {
			List<String> withCellMargins = files
							.filter(file -> file.toString().endsWith(".theme.json"))
							.filter(file -> cellMargins.matcher(content(file)).find())
							.map(themes::relativize)
							.map(Path::toString)
							.sorted()
							.collect(toList());
			assertEquals(emptyList(), withCellMargins);
		}
		//FlatLaf defines neither, its renderers using Table.cellMargins
		UIDefaults flatLaf = new FlatDarkLaf().getDefaults();
		for (LookAndFeel theme : asList(new MaterialOceanic(), new GerryDarcula())) {
			UIDefaults defaults = theme.getDefaults();
			for (String key : asList("Table.cellNoFocusBorder", "Table.focusCellHighlightBorder")) {
				assertEquals(flatLaf.get(key), defaults.get(key), theme.getName() + ": " + key);
			}
		}
	}

	private static String content(Path file) {
		try {
			return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
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
