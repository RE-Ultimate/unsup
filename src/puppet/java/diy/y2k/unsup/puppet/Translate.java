/*
 * This file is part of unsup.
 * Copyright © 2025 Exa Skye
 * https://git.sleeping.town/exa/unsup
 *
 * unsup is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * unsup is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with unsup; if not, see <https://www.gnu.org/licenses/>.
 */

package diy.y2k.unsup.puppet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.xyzsd.plurals.PluralRule;

public class Translate {

	private static final Map<String, PluralStrings> strings = new HashMap<>();
	private static final Set<String> brandedStrings = new HashSet<>();
	
	private static PluralRule ordinal = PluralRule.ROOT_ORDINAL;
	private static PluralRule cardinal = PluralRule.ROOT_CARDINAL;

	public static void addTranslation(String key, PluralStrings value) {
		strings.put(key, value);
		if (key.endsWith(".branded")) {
			brandedStrings.add(key.substring(0, key.length()-8));
		}
	}
	
	public static boolean canTranslate(String key) {
		return strings.containsKey(key);
	}

	public static UnfinishedLangString get(String key) {
		if (key.isEmpty()) return UnfinishedLangString.BLANK;
		String[] split = key.split("¤");
		String k = split[0];
		if (Puppet.modpackName != null && brandedStrings.contains(k)) {
			return get(k+".branded"+key.substring(k.length()))
					.with("brand", Puppet.modpackName);
		}
		PluralStrings ps = strings.get(k);
		if (ps == null) ps = new PluralStrings(k);
		var uls = new UnfinishedLangString(ordinal, cardinal, ps);
		if (split.length > 1) {
			for (int i = 1; i < split.length; i++) {
				var en = split[i];
				var spl = en.split("=");
				var p = spl[0];
				var v = spl[1];
				if (canTranslate(split[i])) {
					// placeholders will get transcluded and will resolve fine
					v = get(split[i]).toString();
				}
				if ("$cardinal".equals(p)) {
					uls.withCardinalPlural(p, Integer.parseInt(v));
				} else if ("$ordinal".equals(p)) {
					uls.withOrdinalPlural(p, Integer.parseInt(v));
				} else {
					uls.with(p, v);
				}
			}
		}
		return uls;
	}

}
