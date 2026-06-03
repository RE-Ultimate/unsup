package diy.y2k.unsup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class LangPrinter {

	public static void main(String[] args) throws IOException {
		System.out.write(Locale.getDefault().toLanguageTag().getBytes(StandardCharsets.UTF_8));
	}
	
}
