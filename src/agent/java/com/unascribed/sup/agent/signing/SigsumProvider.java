/*
 * This file is part of unsup.
 * Copyright © 2026 Exa Skye, Skye
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

import link.e4mc.covenant.policy.Policy;
import link.e4mc.covenant.proof.Proof;
import link.e4mc.covenant.proof.Verifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;

import org.brotli.dec.BrotliInputStream;

import com.unascribed.sup.ann.NotNull;
import com.unascribed.sup.util.Resources;

public class SigsumProvider extends AbstractSigProvider {
	private final Policy policy;
	private final byte[] pubkey;

	public SigsumProvider(String origin, Policy policy, byte[] pubkey) {
		super(origin);
		this.policy = policy;
		this.pubkey = pubkey;
	}
	
	@Override
	public int maximumSignatureLength() {
		return 8192;
	}

	@Override
	public boolean verify(byte[] data, byte[] signature) throws SignatureException {
		byte[] message;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data);
			message = md.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
		try {
			Proof proof = Proof.parse(signature);
			return Verifier.verify(policy, proof, message, pubkey);
		} catch (IllegalArgumentException e) {
			throw new SignatureException("Proof is not in a supported format");
		}
	}

	public static SigsumProvider parse(String origin, @NotNull String line) {
		byte[] pubkey = Base64.getDecoder().decode(line.substring(0, 44));
		if (line.charAt(44) != ' ') {
			throw new IllegalArgumentException("Unknown sigsum key format");
		}
		String policySpec = line.substring(45);
		InputStream stream = Resources.open("assets/sigsum-policies/" + policySpec + ".builtin-policy.br");
		if (stream == null) {
			stream = new ByteArrayInputStream(Base64.getDecoder().decode(policySpec));
		}
		try (BrotliInputStream bis = new BrotliInputStream(stream)) {
			byte[] buf = new byte[16384];
			int len = bis.read(buf);
			Policy policy = Policy.parse(Arrays.copyOf(buf, len));
			return new SigsumProvider(origin, policy, pubkey);
		} catch (IOException | IllegalArgumentException e) {
			throw new IllegalArgumentException("Failed to load sigsum policy");
		}
	}
}
