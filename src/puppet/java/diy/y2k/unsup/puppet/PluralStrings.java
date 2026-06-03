package diy.y2k.unsup.puppet;

import com.github.bsideup.jabel.Desugar;

import net.xyzsd.plurals.PluralCategory;

@Desugar
public record PluralStrings(String zero, String one, String two, String few, String many, String other) {
	
	public PluralStrings(String other) {
		this(other, other, other, other, other, other);
	}

	public String get(PluralCategory cat) {
		return switch (cat) {
			case ZERO -> zero;
			case ONE -> one;
			case TWO -> two;
			case FEW -> few;
			case MANY -> many;
			case OTHER -> other;
		};
	}
	
	@Override
	public String toString() {
		if (zero == one && one == two && two == few && few == many && many == other) {
			return "PluralStrings["+one.replace("\n", "\\n")+"]";
		}
		return "PluralStrings["
				+ "zero="+zero.replace("\n", "\\n")
				+ ", one="+one.replace("\n", "\\n")
				+ ", two="+two.replace("\n", "\\n")
				+ ", few="+few.replace("\n", "\\n")
				+ ", many="+many.replace("\n", "\\n")
				+ ", other="+other.replace("\n", "\\n")
			+ "]";
				
	}
	
}
