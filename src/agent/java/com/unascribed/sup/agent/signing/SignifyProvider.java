/*
 * This file is part of unsup.
 * Copyright © 2023, 2025 Exa Skye
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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;

import com.unascribed.sup.ann.NotNull;
import com.unascribed.sup.util.Bases;

import com.playsawdust.chipper.toolbox.io.Bytes;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

public class SignifyProvider extends AbstractSigProvider {

	private static final byte[] Ed = { 'E', 'd' };
	
	private final long keyId;
	private final SigProvider inner;
	private final byte[] expectedFormat;
	
	public SignifyProvider(String origin, long keyId, EdDSAPublicKey key) throws InvalidKeyException {
		super(origin);
		this.expectedFormat = Ed;
		this.keyId = keyId;
		this.inner = new RawEdDSAProvider(origin+" (inner provider)", key);
	}
	
	@Override
	public int maximumSignatureLength() {
		return 512;
	}
	
	@Override
	public boolean verify(byte[] data, byte[] signature) throws SignatureException {
		signature = Base64.getDecoder().decode(new String(signature, StandardCharsets.ISO_8859_1).split("\n")[1].trim());
		if (!checkFormat(signature, expectedFormat)) {
			throw new SignatureException("Signature is not in a known format");
		}
		long keyId = Bytes.getLong(signature, expectedFormat.length);
		if (keyId != this.keyId) {
			throw new SignatureException("Data is signed with the wrong key (expected "+Bases.longToHex(this.keyId)+" but got "+Bases.longToHex(this.keyId)+")");
		}
		signature = Arrays.copyOfRange(signature, expectedFormat.length+8, signature.length);
		return inner.verify(data, signature);
	}
	
	private static boolean checkFormat(byte[] data, byte[] format) {
		for (int i = 0; i < format.length; i++) {
			if (i >= data.length || data[i] != format[i]) {
				return false;
			}
		}
		return true;
	}

	public static SignifyProvider parse(String origin, @NotNull String key) throws InvalidKeyException {
		byte[] data = Base64.getDecoder().decode(key);
		if (checkFormat(data, Ed)) {
			return new SignifyProvider(origin, Bytes.getLong(data, 2),
					new EdDSAPublicKey(new EdDSAPublicKeySpec(Arrays.copyOfRange(data, 10, data.length),
							EdDSANamedCurveTable.ED_25519_CURVE_SPEC)));
		} else {
			throw new IllegalArgumentException("Unknown signify key format - expected Ed, got "+new String(data, 0, 2, StandardCharsets.ISO_8859_1));
		}
	}
	
}
