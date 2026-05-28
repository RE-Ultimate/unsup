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

package com.unascribed.sup.agent.util;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class SimpleProxySelector extends ProxySelector {

	private final List<Proxy> proxies;
	public SimpleProxySelector(Proxy proxy) {
		this.proxies = Collections.singletonList(proxy);
	}

	@Override
	public List<Proxy> select(URI uri) {
		return proxies;
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {}

}
