/*
 * This file is part of unsup.
 * Copyright © 2026 Exa Skye
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

package com.unascribed.sup.agent.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParserException;
import com.unascribed.sup.agent.data.HashFunction;
import com.unascribed.sup.data.Version;

public class RawHandler extends AbstractFormatHandler {

	public RawHandler(URI src, boolean autoaccept, boolean forceFlavorDefaults, JsonObject baseState) {
		super(src, autoaccept, forceFlavorDefaults, baseState);
	}

	@Override
	public CheckResult check() throws IOException, URISyntaxException, JsonParserException {
		var fplan = new FilePlan();
		fplan.url = src;
		var frag = src.getFragment();
		fplan.state = frag == null ? FileState.MAGIC : new FileState(HashFunction.byName(frag.substring(0, frag.indexOf(':'))), frag.substring(frag.indexOf(':')+1), -1);
		var plan = new UpdatePlan<>(false, baseState);
		var path = src.getPath();
		plan.files.put(path.substring(path.lastIndexOf('/')+1), fplan);
		return new CheckResult(new Version("debug", 0), new Version("debug", 1), plan, Collections.emptyMap());
	}

}
