/*
 * This file is part of unsup.
 * Copyright © 2025-2026 Exa Skye
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

package diy.y2k.unsup.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import diy.y2k.unsup.ann.NotNull;
import diy.y2k.unsup.ann.Nullable;

public class Resources {
	
	private static String path(String name) {
		return "diy/y2k/unsup/"+name;
	}
	
	private static String oldPath(String name) {
		return "com/unascribed/sup/"+name;
	}

	public static @Nullable InputStream open(String name) {
		var cl = Resources.class.getClassLoader();
		assert cl != null;
		var res = cl.getResourceAsStream(path(name));
		if (res == null) res = cl.getResourceAsStream(oldPath(name));
		return res;
	}

	public static @NotNull InputStream openNonnull(String name) throws FileNotFoundException {
		var in = open(name);
		if (in == null) throw new FileNotFoundException("in classpath: "+name);
		return in;
	}
	
	public static @Nullable URL get(String name) {
		var cl = Resources.class.getClassLoader();
		assert cl != null;
		var res = cl.getResource(path(name));
		if (res == null) res = cl.getResource(oldPath(name));
		return res;
	}
	
}
