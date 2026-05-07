/*
 * This file is part of unsup.
 * Copyright © 2023-2025 Exa Skye
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

package com.unascribed.sup.agent.pieces;

import com.unascribed.sup.util.Bases;

import com.playsawdust.chipper.toolbox.io.Bytes;
import com.playsawdust.chipper.toolbox.io.Hasher;
import com.playsawdust.chipper.toolbox.io.MemoryOutputStream;
import com.playsawdust.chipper.toolbox.io.Slice;

/**
 * Implements CurseForge's variant of Murmur2, used for the hashes of particularly old files.
 */
public class Murmur2CFHasher implements Hasher {
	private static final int M32 = 0x5bd1e995;
	private static final int R32 = 24;
	
	@Override
	public Slice hash() {
		return hash(new byte[0]);
	}
	
	@Override
	public Session newSession() {
		return new Session() {
			// algorithm is seeded with the length :/
			private final MemoryOutputStream baos = new MemoryOutputStream();
			
			@Override
			public void contribute(byte[] bytes, int offset, int length) {
				for (int i = 0; i < length; i++) {
					int b = bytes[offset+i]&0xFF;
					if (b == 9 || b == 10 || b == 13 || b == 32) {
						// modification: strips whitespace (?!?!?!?!?!)
						continue;
					}
					baos.write(b);
				}
			}
			
			@Override
			public Slice finish() {
				byte[] data = baos.toByteArray();
				int length = data.length;
				
				// Initialize the hash to a random value
				int h = 1 ^ length;

				// Mix 4 bytes at a time into the hash
				final int nblocks = length >> 2;

				// body
				for (int i = 0; i < nblocks; i++) {
					final int index = (i << 2);
					int k = getLittleEndianInt(data, index);
					k *= M32;
					k ^= k >>> R32;
					k *= M32;
					h *= M32;
					h ^= k;
				}

				// Handle the last few bytes of the input array
				final int index = (nblocks << 2);
				switch (length - index) {
					case 3:
						h ^= (data[index + 2] & 0xff) << 16;
					case 2:
						h ^= (data[index + 1] & 0xff) << 8;
					case 1:
						h ^= (data[index] & 0xff);
						h *= M32;
				}

				// Do a few final mixes of the hash to ensure the last few
				// bytes are well-incorporated.
				h ^= h >>> 13;
				h *= M32;
				h ^= h >>> 15;

				return new Slice(Bytes.intToByteArray(h));
			}
			
		};
	}

	private static int getLittleEndianInt(final byte[] data, final int index) {
		return ((data[index    ] & 0xff)      ) |
		       ((data[index + 1] & 0xff) <<  8) |
		       ((data[index + 2] & 0xff) << 16) |
		       ((data[index + 3] & 0xff) << 24);
	}

	public static String decToHex(String dec) {
		return Bases.bytesToHex(Bytes.intToByteArray((int)Long.parseLong(dec)));
	}
	
}