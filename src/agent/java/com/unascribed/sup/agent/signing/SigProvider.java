/*
 * This file is part of unsup.
 * Copyright © 2023, 2025-2026 Exa Skye, Skye
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

package com.unascribed.sup.agent.signing;

import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import com.unascribed.sup.ann.Nullable;

public interface SigProvider {

	boolean verify(byte[] data, byte[] signature) throws SignatureException;
	int maximumSignatureLength();
	@Nullable String origin();

	static SigProvider parse(String origin, String line) throws InvalidKeySpecException, InvalidKeyException {
		var tokens = line.split(" ", 2);
		return switch (tokens[0]) {
			case "ed25519" -> RawEdDSAProvider.parse(origin, tokens[1]);
			case "signify" -> SignifyProvider.parse(origin, tokens[1]);
			case "sigsum" -> SigsumProvider.parse(origin, tokens[1]);
			default -> throw new IllegalArgumentException("Unknown key kind \""+tokens[0]+"\" from "+origin+", expected ed25519, signify, or sigsum");
		};
	}
	
	static SigProvider of(String origin, String line) {
		try {
			return parse(origin, line);
		} catch (InvalidKeyException | InvalidKeySpecException e) {
			throw new AssertionError(e);
		}
	}

}
