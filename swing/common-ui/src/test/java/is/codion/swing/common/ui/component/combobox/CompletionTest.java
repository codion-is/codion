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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.swing.common.ui.component.combobox.Completion.Mode;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import java.awt.event.KeyEvent;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static org.assertj.swing.core.KeyPressInfo.keyCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfSystemProperty(named = "java.awt.headless", matches = "true")
public final class CompletionTest {

	private static FrameFixture window;

	@BeforeAll
	public static void setUp() {
		TestFrame frame = GuiActionRunner.execute(TestFrame::new);
		window = new FrameFixture(frame);
		window.show();
	}

	@AfterAll
	public static void tearDown() {
		window.cleanUp();
	}

	@Test
	void setTwice() {
		JComboBox<?> comboBox = new JComboBox<>();
		Completion.builder()
						.mode(Mode.MAXIMUM_MATCH)
						.enable(comboBox);
		assertThrows(IllegalStateException.class, () -> Completion.builder()
						.mode(Mode.AUTOCOMPLETE)
						.enable(comboBox));
	}

	@Test
	void autoComplete() {
		JTextComponentFixture editor = window.textBox("autoCompleteEditor");
		JComboBox<String> comboBox = window.comboBox("autoCompleteComboBox").target();
		editor.enterText("j");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("r");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("o");
		assertEquals(6, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(6, comboBox.getSelectedIndex());
		editor.enterText("u");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("jú");
		assertEquals(9, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("u");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("an");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("u");
		assertEquals(3, comboBox.getSelectedIndex());
		editor.enterText("s");
		assertEquals(4, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("ú");
		assertEquals(5, comboBox.getSelectedIndex());
		editor.selectAll();
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("jú");
		assertEquals(9, comboBox.getSelectedIndex());
		editor.enterText("ní");
		assertEquals(11, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("i");
		assertEquals(10, comboBox.getSelectedIndex());

		comboBox.setSelectedItem("Janúar");
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("janu");
		assertEquals(3, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("ú");
		assertEquals(5, comboBox.getSelectedIndex());
	}

	@Test
	void autoCompleteNormalize() {
		JTextComponentFixture editor = window.textBox("autoCompleteNormalizeEditor");
		JComboBox<String> comboBox = window.comboBox("autoCompleteNormalizeComboBox").target();
		editor.enterText("j");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("r");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("o");
		assertEquals(6, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(6, comboBox.getSelectedIndex());
		editor.enterText("u");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("jú");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("u");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("an");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("u");
		assertEquals(3, comboBox.getSelectedIndex());
		editor.enterText("s");
		assertEquals(4, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("ú");
		assertEquals(4, comboBox.getSelectedIndex());
		editor.selectAll();
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("jú");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.enterText("ní");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("í");
		assertEquals(8, comboBox.getSelectedIndex());

		comboBox.setSelectedItem("Janúar");
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("janu");
		assertEquals(3, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("ú");
		assertEquals(3, comboBox.getSelectedIndex());
	}

	@Test
	void maximumMatch() {
		JTextComponentFixture editor = window.textBox("maximumMatchEditor");
		JComboBox<String> comboBox = window.comboBox("maximumMatchComboBox").target();
		editor.enterText("j");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("r");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("o");
		assertEquals(6, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(6, comboBox.getSelectedIndex());
		editor.enterText("u");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("jú");
		assertEquals(9, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("u");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("an");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("u");
		assertEquals(3, comboBox.getSelectedIndex());
		editor.enterText("s");
		assertEquals(4, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("ú");
		assertEquals(5, comboBox.getSelectedIndex());
		editor.selectAll();
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("jú");
		assertEquals(9, comboBox.getSelectedIndex());
		editor.enterText("ní");
		assertEquals(11, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("i");
		assertEquals(10, comboBox.getSelectedIndex());

		comboBox.setSelectedItem("Janúar");
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("janu");
		assertEquals(3, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("ú");
		assertEquals(5, comboBox.getSelectedIndex());
	}

	@Test
	void maximumMatchNormalize() {
		JTextComponentFixture editor = window.textBox("maximumMatchNormalizeEditor");
		JComboBox<String> comboBox = window.comboBox("maximumMatchNormalizeComboBox").target();
		editor.enterText("j");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("r");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("o");
		assertEquals(6, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(6, comboBox.getSelectedIndex());
		editor.enterText("u");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("jú");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("u");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("an");
		assertEquals(1, comboBox.getSelectedIndex());
		editor.enterText("u");
		assertEquals(3, comboBox.getSelectedIndex());
		editor.enterText("s");
		assertEquals(4, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("ú");
		assertEquals(4, comboBox.getSelectedIndex());
		editor.selectAll();
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("jú");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.enterText("ní");
		assertEquals(8, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("í");
		assertEquals(8, comboBox.getSelectedIndex());

		comboBox.setSelectedItem("Janúar");
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		assertEquals(0, comboBox.getSelectedIndex());
		editor.enterText("janu");
		assertEquals(3, comboBox.getSelectedIndex());
		editor.pressAndReleaseKey(keyCode(KeyEvent.VK_BACK_SPACE));
		editor.enterText("ú");
		assertEquals(3, comboBox.getSelectedIndex());
	}

	private static final class TestFrame extends JFrame {

		private TestFrame() {
			JComboBox<String> autoComplete = createComboBox();
			autoComplete.setName("autoCompleteComboBox");
			autoComplete.getEditor().getEditorComponent().setName("autoCompleteEditor");
			Completion.builder()
							.mode(Mode.AUTOCOMPLETE)
							.normalize(false)
							.enable(autoComplete);

			JComboBox<String> autoCompleteNormalize = createComboBox();
			autoCompleteNormalize.setName("autoCompleteNormalizeComboBox");
			autoCompleteNormalize.getEditor().getEditorComponent().setName("autoCompleteNormalizeEditor");
			Completion.builder()
							.mode(Mode.AUTOCOMPLETE)
							.normalize(true)
							.enable(autoCompleteNormalize);

			JComboBox<String> maximumMatch = createComboBox();
			maximumMatch.setName("maximumMatchComboBox");
			maximumMatch.getEditor().getEditorComponent().setName("maximumMatchEditor");
			Completion.builder()
							.mode(Mode.MAXIMUM_MATCH)
							.normalize(false)
							.enable(maximumMatch);

			JComboBox<String> maximumMatchNormalize = createComboBox();
			maximumMatchNormalize.setName("maximumMatchNormalizeComboBox");
			maximumMatchNormalize.getEditor().getEditorComponent().setName("maximumMatchNormalizeEditor");
			Completion.builder()
							.mode(Mode.MAXIMUM_MATCH)
							.normalize(true)
							.enable(maximumMatchNormalize);

			setLayout(gridLayout(0, 1));
			add(autoComplete);
			add(autoCompleteNormalize);
			add(maximumMatch);
			add(maximumMatchNormalize);
		}
	}

	private static JComboBox<String> createComboBox() {

		return new JComboBox<>(new String[] {
						"-",      //0
						"Jani",   //1
						"Janí",   //2
						"Januar", //3
						"Janus",  //4
						"Janúar", //5
						"Jon",    //6
						"Jón",    //7
						"Juni",   //8
						"Jún",    //9
						"Júni",   //10
						"Júní"    //11
		});
	}
}
