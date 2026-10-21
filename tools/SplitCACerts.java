import java.text.Normalizer.Form;

void main() throws IOException {
	var equals = Pattern.compile("^=+$");
	var badChars = Pattern.compile("[^A-Za-z0-9_-]");
	try (var r = new BufferedReader(new FileReader(new File("cacert.pem")));
			var out = new ZipOutputStream(new FileOutputStream(new File("src/main/resources/diy/y2k/unsup/assets/cacerts.zip")))) {
		var lastLine = "";
		var insideCert = false;
		var filename = "";
		var buffer = new StringBuilder();
		while (true) {
			var l = r.readLine();
			if (l == null) break;
			if (l.startsWith("#")) continue;
			boolean copy = false;
			if (equals.matcher(l).matches()) {
				filename = badChars.matcher(Normalizer.normalize(lastLine, Form.NFKD)).replaceAll("_")+".der";
			} else if ("-----BEGIN CERTIFICATE-----".equals(l)) {
				insideCert = true;
			} else if ("-----END CERTIFICATE-----".equals(l)) {
				insideCert = false;
				var en = new ZipEntry(filename);
				en.setMethod(ZipEntry.STORED);
				var crc = new CRC32();
				var bys = Base64.getDecoder().decode(buffer.toString());
				crc.update(bys);
				en.setCompressedSize(bys.length);
				en.setSize(bys.length);
				en.setCrc(crc.getValue());
				out.putNextEntry(en);
				out.write(bys);
				out.closeEntry();
				buffer.setLength(0);
			} else if (insideCert) {
				copy = true;
			}
			if (copy) {
				buffer.append(l);
			}
			lastLine = l;
		}
	}
}
