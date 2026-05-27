/*
 * This file is part of unsup.
 * Copyright © 2026 Skye
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public class SigsumProvider implements SigProvider {
    private final Policy policy;
    private final byte[] pubkey;

    public SigsumProvider(Policy policy, byte[] pubkey) {
        this.policy = policy;
        this.pubkey = pubkey;
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
}
