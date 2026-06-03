package diy.y2k.unsup.puppet;

import net.xyzsd.plurals.PluralRule;

public class UnfinishedLangString {

	public static final UnfinishedLangString BLANK = new UnfinishedLangString(PluralRule.ROOT_ORDINAL, PluralRule.ROOT_CARDINAL, new PluralStrings(""));
	
	private final PluralRule ordinal, cardinal;
	private final PluralStrings strings;
	private String str;
	
	public UnfinishedLangString(PluralRule ordinal, PluralRule cardinal, PluralStrings strings) {
		this.ordinal = ordinal;
		this.cardinal = cardinal;
		this.strings = strings;
	}

	/**
	 * 1st/2nd/3rd/etc
	 */
	public UnfinishedLangString withOrdinalPlural(String placeholder, int num) {
		if (str != null) throw new IllegalStateException("String plurality already selected");
		str = strings.get(ordinal.select(num));
		with(placeholder, num);
		return this;
	}
	
	/**
	 * word after number
	 */
	public UnfinishedLangString withCardinalPlural(String placeholder, int num) {
		if (str != null) throw new IllegalStateException("String plurality already selected");
		str = strings.get(cardinal.select(num));
		with(placeholder, num);
		return this;
	}

	public UnfinishedLangString with(String placeholder, Object value) {
		if (str == null) str = strings.other();
		str = str.replace("{{."+placeholder+"}}", String.valueOf(value));
		return this;
	}
	
	@Override
	public String toString() {
		if (str == null) return strings.other();
		return str;
	}
	
}
