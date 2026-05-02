package com.unascribed.sup.agent.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.unascribed.sup.agent.data.HashFunction;
import com.unascribed.sup.util.Bases;

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
		var dos = func.createMessageDigest();
		var unix = func.createMessageDigest();
		var raw = func.createMessageDigest();
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
			raw.update((byte)b);
			if (b == '\r') continue;
			if (b == '\n') {
				dos.update((byte)'\r');
				dos.update((byte)'\n');
				unix.update((byte)'\n');
			} else {
				dos.update((byte)b);
				unix.update((byte)b);
			}
		}
		String dosHash = Bases.bytesToHex(dos.digest());
		String unixHash = Bases.bytesToHex(unix.digest());
		String hash = Bases.bytesToHex(raw.digest());
		if (expectedHash.equals(dosHash) && hash.equals(unixHash)) {
			return CorruptionType.EXPECTED_DOS_GOT_UNIX;
		} else if (expectedHash.equals(unixHash) && hash.equals(dosHash)) {
			return CorruptionType.EXPECTED_UNIX_GOT_DOS;
		} else {
			return CorruptionType.UNKNOWN;
		}
	}
	
}
