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
package is.codion.plugin.intellij;

import is.codion.plugin.intellij.themes.akusan.AkusanDark;
import is.codion.plugin.intellij.themes.akusan.AkusanGray;
import is.codion.plugin.intellij.themes.akusan.AkusanLight;
import is.codion.plugin.intellij.themes.arc.Arc;
import is.codion.plugin.intellij.themes.arc.ArcDarkOrange;
import is.codion.plugin.intellij.themes.arc.ArcOrange;
import is.codion.plugin.intellij.themes.carbon.Carbon;
import is.codion.plugin.intellij.themes.cobalt2.Cobalt2;
import is.codion.plugin.intellij.themes.codely.Codely;
import is.codion.plugin.intellij.themes.codely.CodelyBlue;
import is.codion.plugin.intellij.themes.codely.CodelyDark;
import is.codion.plugin.intellij.themes.codely.CodelyLight;
import is.codion.plugin.intellij.themes.cutepink.CutePinkDark;
import is.codion.plugin.intellij.themes.cutepink.CutePinkLight;
import is.codion.plugin.intellij.themes.cyberpunk.Cyberpunk;
import is.codion.plugin.intellij.themes.darculasolid.DarculaSolid;
import is.codion.plugin.intellij.themes.darkflat.DarkFlat;
import is.codion.plugin.intellij.themes.darkhorizon.DarkHorizon;
import is.codion.plugin.intellij.themes.darkpurple.DarkPurple;
import is.codion.plugin.intellij.themes.elegant.ElegantDark;
import is.codion.plugin.intellij.themes.elegant.ElegantLight;
import is.codion.plugin.intellij.themes.espresso.EspressoLight;
import is.codion.plugin.intellij.themes.everforest.EverforestDarkHard;
import is.codion.plugin.intellij.themes.everforest.EverforestDarkMedium;
import is.codion.plugin.intellij.themes.everforest.EverforestDarkSoft;
import is.codion.plugin.intellij.themes.everforest.EverforestLightHard;
import is.codion.plugin.intellij.themes.everforest.EverforestLightMedium;
import is.codion.plugin.intellij.themes.everforest.EverforestLightSoft;
import is.codion.plugin.intellij.themes.gerry.GerryDarcula;
import is.codion.plugin.intellij.themes.gerry.GerryDark;
import is.codion.plugin.intellij.themes.gerry.GerryLight;
import is.codion.plugin.intellij.themes.gerry.GerryOceanic;
import is.codion.plugin.intellij.themes.gerry.GerryStorm;
import is.codion.plugin.intellij.themes.gerry.GerryViolet;
import is.codion.plugin.intellij.themes.github.GithubDark;
import is.codion.plugin.intellij.themes.github.GithubDarkDefault;
import is.codion.plugin.intellij.themes.github.GithubLight;
import is.codion.plugin.intellij.themes.gradianto.GradiantoDarkFuchsia;
import is.codion.plugin.intellij.themes.gradianto.GradiantoDeepOcean;
import is.codion.plugin.intellij.themes.gradianto.GradiantoMidnightBlue;
import is.codion.plugin.intellij.themes.gradianto.GradiantoNatureGreen;
import is.codion.plugin.intellij.themes.gray.Gray;
import is.codion.plugin.intellij.themes.gruvbox.GruvboxDarkHard;
import is.codion.plugin.intellij.themes.gruvbox.GruvboxDarkMedium;
import is.codion.plugin.intellij.themes.gruvbox.GruvboxDarkSoft;
import is.codion.plugin.intellij.themes.gruvbox.GruvboxMaterialLightHard;
import is.codion.plugin.intellij.themes.gruvbox.GruvboxMaterialLightMedium;
import is.codion.plugin.intellij.themes.gruvbox.GruvboxMaterialLightSoft;
import is.codion.plugin.intellij.themes.hiberbee.HiberbeeDark;
import is.codion.plugin.intellij.themes.highcontrast.HighContrast;
import is.codion.plugin.intellij.themes.lightflat.LightFlat;
import is.codion.plugin.intellij.themes.lotus.LotusDark;
import is.codion.plugin.intellij.themes.lotus.LotusLight;
import is.codion.plugin.intellij.themes.material.ArcDark;
import is.codion.plugin.intellij.themes.material.AtomOneDark;
import is.codion.plugin.intellij.themes.material.AtomOneLight;
import is.codion.plugin.intellij.themes.material.Dracula;
import is.codion.plugin.intellij.themes.material.GitHubDarkMaterial;
import is.codion.plugin.intellij.themes.material.GitHubMaterial;
import is.codion.plugin.intellij.themes.material.LightOwl;
import is.codion.plugin.intellij.themes.material.MaterialDarker;
import is.codion.plugin.intellij.themes.material.MaterialDeepOcean;
import is.codion.plugin.intellij.themes.material.MaterialLighter;
import is.codion.plugin.intellij.themes.material.MaterialOceanic;
import is.codion.plugin.intellij.themes.material.MaterialPalenight;
import is.codion.plugin.intellij.themes.material.MonokaiProMaterial;
import is.codion.plugin.intellij.themes.material.Moonlight;
import is.codion.plugin.intellij.themes.material.NightOwl;
import is.codion.plugin.intellij.themes.material.SolarizedDark;
import is.codion.plugin.intellij.themes.material.SolarizedLight;
import is.codion.plugin.intellij.themes.materialtheme.MaterialTheme;
import is.codion.plugin.intellij.themes.monocai.Monocai;
import is.codion.plugin.intellij.themes.monokaipro.MonokaiPro;
import is.codion.plugin.intellij.themes.naturethemes.AuroraBorealis;
import is.codion.plugin.intellij.themes.naturethemes.Autumn;
import is.codion.plugin.intellij.themes.naturethemes.Everest;
import is.codion.plugin.intellij.themes.naturethemes.Roraima;
import is.codion.plugin.intellij.themes.naturethemes.Sakura;
import is.codion.plugin.intellij.themes.naturethemes.Sequoia;
import is.codion.plugin.intellij.themes.nightfall.Nightfall;
import is.codion.plugin.intellij.themes.obsidiansunset.ObsidianSunsetBlue;
import is.codion.plugin.intellij.themes.obsidiansunset.ObsidianSunsetOrange;
import is.codion.plugin.intellij.themes.onedark.OneDark;
import is.codion.plugin.intellij.themes.onepauintxi.OnePauintxiAllBlue;
import is.codion.plugin.intellij.themes.onepauintxi.OnePauintxiAllOrange;
import is.codion.plugin.intellij.themes.onepauintxi.OnePauintxiBlue;
import is.codion.plugin.intellij.themes.onepauintxi.OnePauintxiOrange;
import is.codion.plugin.intellij.themes.rider.RiderDark;
import is.codion.plugin.intellij.themes.rider.RiderDay;
import is.codion.plugin.intellij.themes.rider.RiderLight;
import is.codion.plugin.intellij.themes.rider.RiderNight;
import is.codion.plugin.intellij.themes.srcery.Srcery;
import is.codion.plugin.intellij.themes.tayviscon.TeyvisconDark;
import is.codion.plugin.intellij.themes.trashpanda.TrashPanda;
import is.codion.plugin.intellij.themes.trashpanda.TrashPandaBlacklight;
import is.codion.plugin.intellij.themes.trashpanda.TrashPandaDawnlight;
import is.codion.plugin.intellij.themes.trashpanda.TrashPandaDaylight;
import is.codion.plugin.intellij.themes.trashpanda.TrashPandaMoonlight;
import is.codion.plugin.intellij.themes.trashpanda.TrashPandaStarlight;
import is.codion.plugin.intellij.themes.vuesion.Vuesion;
import is.codion.plugin.intellij.themes.xcodedark.XcodeDark;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Collection;

import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * <p>Provides Look And Feels based on IntelliJ themes.
 * <p>Uses Flat Look And Feel under the hood.
 */
public final class IntelliJThemes {

	private IntelliJThemes() {}

	/**
	 * @return all available IntelliJ Theme Look and Feels
	 */
	public static Collection<LookAndFeelProvider> get() {
		return unmodifiableList(asList(
						lookAndFeelProvider(new LookAndFeelInfo("Nature Aurora Borealis", AuroraBorealis.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Nature Autumn", Autumn.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Nature Everest", Everest.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Nature Roraima", Roraima.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Nature Sakura", Sakura.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Nature Sequoia", Sequoia.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Srcery", Srcery.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gerry Darcula", GerryDarcula.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gerry Dark", GerryDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gerry Oceanic", GerryOceanic.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gerry Storm", GerryStorm.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gerry Violet", GerryViolet.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gerry Light", GerryLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Lotus Dark", LotusDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Lotus Light", LotusLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Teyviscon Dark", TeyvisconDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Everforest Dark Hard", EverforestDarkHard.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Everforest Dark Medium", EverforestDarkMedium.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Everforest Dark Soft", EverforestDarkSoft.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Everforest Ligth Hard", EverforestLightHard.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Everforest Ligth Medium", EverforestLightMedium.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Everforest Ligth Soft", EverforestLightSoft.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Dark Horizon", DarkHorizon.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Akusan Dark", AkusanDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Akusan Gray", AkusanGray.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Akusan Light", AkusanLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("One Pauintxi All Blue", OnePauintxiAllBlue.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("One Pauintxi Blue", OnePauintxiBlue.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("One Pauintxi All Orange", OnePauintxiAllOrange.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("One Pauintxi Orange", OnePauintxiOrange.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gruvbox Light Hard", GruvboxMaterialLightHard.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gruvbox Light Medium", GruvboxMaterialLightMedium.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gruvbox Light Soft", GruvboxMaterialLightSoft.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Obsidian Sunset Blue", ObsidianSunsetBlue.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Obsidian Sunset Orange", ObsidianSunsetOrange.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Github Dark", GithubDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Github Dark Default", GithubDarkDefault.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Github Light", GithubLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Rider Dark", RiderDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Rider Day", RiderDay.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Rider Light", RiderLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Rider Night", RiderNight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Rider Dark", RiderDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Darcula Solid", DarculaSolid.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Nightfall", Nightfall.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Trash Panda", TrashPanda.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Trash Panda Blacklight", TrashPandaBlacklight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Trash Panda Dawnlight", TrashPandaDawnlight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Trash Panda Daylight", TrashPandaDaylight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Trash Panda Moonlight", TrashPandaMoonlight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Trash Panda Starlight", TrashPandaStarlight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Cute Pink Light", CutePinkLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Cute Pink Dark", CutePinkDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Codely", Codely.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Codely Blue", CodelyBlue.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Codely Dark", CodelyDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Codely Light", CodelyLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Cyberpunk", Cyberpunk.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Elegant Dark", ElegantDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Elegant Light", ElegantLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Espresso Light", EspressoLight.class.getName())),

						// From FlatLaf
						lookAndFeelProvider(new LookAndFeelInfo("Arc Dark (Material)", ArcDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Atom One Dark (Material)", AtomOneDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Atom One Light (Material)", AtomOneLight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Dracula (Material)", Dracula.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Github Dark (Material)", GitHubDarkMaterial.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Github (Material)", GitHubMaterial.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Light Owl (Material)", LightOwl.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Material Darker (Material)", MaterialDarker.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Material Deep Ocean (Material)", MaterialDeepOcean.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Material Lighter (Material)", MaterialLighter.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Material Oceanic (Material)", MaterialOceanic.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Material Palenight (Material)", MaterialPalenight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Monokai Pro (Material)", MonokaiProMaterial.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Moonlight (Material)", Moonlight.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Night Owl (Material)", NightOwl.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Solarized Dark (Material)", SolarizedDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Solarized Light (Material)", SolarizedLight.class.getName())),
						// End material

						lookAndFeelProvider(new LookAndFeelInfo("Arc", Arc.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Arc Dark", ArcDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Arc Orance", ArcOrange.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Arc Dark Orange", ArcDarkOrange.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Carbon", Carbon.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Cobalt 2", Cobalt2.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Dark Flat", DarkFlat.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Dark Purple", DarkPurple.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Dracula", Dracula.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gradianto Dark Fuchsia", GradiantoDarkFuchsia.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gradianto Deep Ocean", GradiantoDeepOcean.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gradianto Midnight Blue", GradiantoMidnightBlue.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gradianto Nature Green", GradiantoNatureGreen.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gray", Gray.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gruvbox Dark Hard", GruvboxDarkHard.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gruvbox Dark Medium", GruvboxDarkMedium.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Gruvbox Dark Soft", GruvboxDarkSoft.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Hiberbee Dark", HiberbeeDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("High Contrast", HighContrast.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Light Flat", LightFlat.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Material Theme", MaterialTheme.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Monocai", Monocai.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Monokai Pro", MonokaiPro.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("One Dark", OneDark.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("Vuesion", Vuesion.class.getName())),
						lookAndFeelProvider(new LookAndFeelInfo("XCode Dark", XcodeDark.class.getName()))
		));
	}
}
