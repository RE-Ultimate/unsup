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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.unascribed.sup.agent.data.HashFunction;
import okio.ByteString;

public class CorruptionHelper {

	public enum CorruptionType {
		UNKNOWN,
		EXPECTED_UNIX_GOT_DOS,
		EXPECTED_DOS_GOT_UNIX,
		LFS_POINTER,
		;
	}
	
	private static final ByteString LFS_HEADER = ByteString.encodeUtf8("version https://git-lfs.github.com/spec/v1");
	
	public static CorruptionType checkForCorruption(HashFunction func, String expectedHash, InputStream src) throws IOException {
		if (func == null || expectedHash == null) return CorruptionType.UNKNOWN;
		var dos = func.getHasher().newSession();
		var unix = func.getHasher().newSession();
		var raw = func.getHasher().newSession();
		if (!(src instanceof ByteArrayInputStream) && !(src instanceof BufferedInputStream)) {
			src = new BufferedInputStream(src);
		}
		byte[] header = new byte[LFS_HEADER.size()];
		src.mark(LFS_HEADER.size());
		src.read(header); // guaranteed to read all bytes on a BAIS or BIS
		if (ByteString.of(header).equals(LFS_HEADER)) {
			return CorruptionType.LFS_POINTER;
		}
		src.reset();
		while (true) {
			int b = src.read();
			if (b == -1) break;
			raw.contribute((byte)b);
			if (b == '\r') continue;
			if (b == '\n') {
				dos.contribute((byte)'\r');
				dos.contribute((byte)'\n');
				unix.contribute((byte)'\n');
			} else {
				dos.contribute((byte)b);
				unix.contribute((byte)b);
			}
		}
		String dosHash = dos.finish().toHexLower();
		String unixHash = unix.finish().toHexLower();
		String hash = raw.finish().toHexLower();
		if (expectedHash.equals(dosHash) && hash.equals(unixHash)) {
			return CorruptionType.EXPECTED_DOS_GOT_UNIX;
		} else if (expectedHash.equals(unixHash) && hash.equals(dosHash)) {
			return CorruptionType.EXPECTED_UNIX_GOT_DOS;
		} else {
			return CorruptionType.UNKNOWN;
		}
	}
	
}
