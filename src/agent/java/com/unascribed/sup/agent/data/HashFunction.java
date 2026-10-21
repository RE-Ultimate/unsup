/*
 * This file is part of unsup.
 * Copyright © 2020-2021, 2023-2026 Exa Skye
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

package com.unascribed.sup.agent.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.unascribed.sup.agent.Agent;
import com.unascribed.sup.agent.Log;
import com.unascribed.sup.agent.pieces.Murmur2CFHasher;
import diy.y2k.stator.io.Hasher;
import diy.y2k.stator.io.hash.StandardHashers;

public enum HashFunction {
	@SuppressWarnings("deprecation")
	MD5("MD5", StandardHashers::md5, 128, true),
	SHA1("SHA-1", StandardHashers::sha1, 160, true),
	SHA2_256("SHA-2 256", StandardHashers::sha2_256, 256, false),
	SHA2_384("SHA-2 384", StandardHashers::sha2_384, 384, false),
	SHA2_512("SHA-2 512", StandardHashers::sha2_512, 512, false),
	SHA2_512_256("SHA-2 512/256", StandardHashers::sha2_512_256, 256, false),

	MURMUR2_CF("Murmur2-CF", Murmur2CFHasher::new, 32, true),
	;
	
	private static final Map<String, HashFunction> BY_NAME = new HashMap<>();
	
	public final String name;
	private final Hasher hasher;
	private final int sizeInBits;
	private final int sizeInBytes;
	private final int sizeInHexChars;
	private final boolean insecure;
	private final String emptyHash;
	private final boolean unsupported;
	
	private boolean hasWarned = false;
	
	HashFunction(String name, Supplier<Hasher> hasherSupplier, int sizeInBits, boolean insecure) {
		this.name = name;
		this.sizeInBits = sizeInBits;
		this.sizeInBytes = (sizeInBits+7)/8;
		this.sizeInHexChars = sizeInBytes()*2;
		this.insecure = insecure;
		boolean unsupported;
		String emptyHash;
		Hasher hasher;
		try {
			hasher = hasherSupplier.get();
			emptyHash = hasher.hash().toHexLower();
			unsupported = false;
		} catch (UnsupportedOperationException | IllegalArgumentException e) {
			Log.warn("This JRE does not support "+name);
			hasher = null;
			emptyHash = "?????";
			unsupported = true;
		}
		this.hasher = hasher;
		this.emptyHash = emptyHash;
		this.unsupported = unsupported;
	}
	
	private UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException(name+" is not supported by this JRE - the manifest needs to be changed to not use this hash function, or you need to update to a newer Java patch");
	}
	
	public Hasher getHasher() {
		if (unsupported) throw unsupported();
		checkSecureHashEnforcement();
		return hasher;
	}

	public String emptyHash() {
		if (unsupported) throw unsupported();
		checkSecureHashEnforcement();
		return emptyHash;
	}

	public boolean insecure() {
		return insecure;
	}

	public int sizeInHexChars() {
		return sizeInHexChars;
	}

	public int sizeInBytes() {
		return sizeInBytes;
	}

	public int sizeInBits() {
		return sizeInBits;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	void checkSecureHashEnforcement() {
		if (insecure && Agent.config().enforceSecureHashes()) {
			throw new IllegalArgumentException("Attempted to use insecure hash function "+name+" when enforce_secure_hashes=true");
		}
	}
	
	public static HashFunction byName(String name) {
		if (!BY_NAME.containsKey(name)) throw new IllegalArgumentException("No hash function with name "+name);
		HashFunction func = BY_NAME.get(name);
		if (func != null) {
			func.checkSecureHashEnforcement();
			if (func.insecure() && !func.hasWarned) {
				func.hasWarned = true;
				if (Agent.config().packSig() != null) {
					Log.warn("Using insecure hash function "+func+" for a signed manifest! This is a very bad idea!");
				} else {
					Log.warn("Using insecure hash function "+func);
				}
			}
		}
		return func;
	}

	static {
		for (HashFunction func : values()) {
			BY_NAME.put(func.name, func);
		}
	}
}
