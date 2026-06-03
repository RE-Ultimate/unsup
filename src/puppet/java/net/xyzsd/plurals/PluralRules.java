package net.xyzsd.plurals;

import java.lang.String;
import java.util.function.Function;

/**
 * <p>CLDR Plural rules. This is an automatically generated file.</p>
 */
public final class PluralRules {
  /**
   * The CLDR version from which this was generated.
   */
  public static final int CLDR_VERSION = 47;

  private static final Function<PluralOperand, PluralCategory> _C_RULE_0 = (op) -> {
      final long mod_i100 = (op.i % 100);
    if (op.v == 0 && mod_i100 == 1) {
      return PluralCategory.ONE;
    } else if (op.v == 0 && mod_i100 == 2) {
      return PluralCategory.TWO;
    } else if (op.v == 0 && (contains(mod_i100, 3, 4)) || op.v != 0) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_1 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n1000000 = (op.n % 1000000);
    final double mod_n10 = (op.n % 10);
    if (mod_n10 == 1 && !((mod_n100 == 11) || (mod_n100 == 71) || (mod_n100 == 91))) {
      return PluralCategory.ONE;
    } else if (mod_n10 == 2 && !((mod_n100 == 12) || (mod_n100 == 72) || (mod_n100 == 92))) {
      return PluralCategory.TWO;
    } else if ((contains(mod_n10, 3, 4) || (mod_n10 == 9)) && !(contains(mod_n100, 10, 19) || contains(mod_n100, 70, 79) || contains(mod_n100, 90, 99))) {
      return PluralCategory.FEW;
    } else if (op.n != 0 && mod_n1000000 == 0) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_2 = (op) -> {
      if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (op.n == 2) {
      return PluralCategory.TWO;
    } else if ((contains(op.n, 3, 6))) {
      return PluralCategory.FEW;
    } else if ((contains(op.n, 7, 10))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_3 = (op) -> {
      if ((contains(op.n, 0, 1))) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_4 = (op) -> {
      if (((op.i == 0) || (op.i == 1))) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_5 = (op) -> {
      if (op.n == 1 || op.t != 0 && ((op.i == 0) || (op.i == 1))) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_6 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n10 = (op.n % 10);
    if (mod_n10 == 1 && mod_n100 != 11) {
      return PluralCategory.ONE;
    } else if ((contains(mod_n10, 2, 4)) && !(contains(mod_n100, 12, 14))) {
      return PluralCategory.FEW;
    } else if (mod_n10 == 0 || (contains(mod_n10, 5, 9)) || (contains(mod_n100, 11, 14))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_7 = (op) -> {
      final long mod_i1000000 = (op.i % 1000000);
    if (((op.i == 0) || (op.i == 1))) {
      return PluralCategory.ONE;
    } else if (op.e == 0 && op.i != 0 && mod_i1000000 == 0 && op.v == 0 || !(contains(op.e, 0, 5))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_8 = (op) -> {
      final int mod_t100 = (op.t % 100);
    final long mod_i10 = (op.i % 10);
    final long mod_i100 = (op.i % 100);
    final int mod_t10 = (op.t % 10);
    if (op.t == 0 && mod_i10 == 1 && mod_i100 != 11 || mod_t10 == 1 && mod_t100 != 11) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_9 = (op) -> {
      if (op.n == 0) {
      return PluralCategory.ZERO;
    } else if (((op.i == 0) || (op.i == 1)) && op.n != 0) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_10 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final int mod_f10 = (op.f % 10);
    final int mod_f100 = (op.f % 100);
    final double mod_n10 = (op.n % 10);
    if (mod_n10 == 0 || (contains(mod_n100, 11, 19)) || op.v == 2 && (contains(mod_f100, 11, 19))) {
      return PluralCategory.ZERO;
    } else if (mod_n10 == 1 && mod_n100 != 11 || op.v == 2 && mod_f10 == 1 && mod_f100 != 11 || op.v != 2 && mod_f10 == 1) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_11 = (op) -> {
      if (op.n == 0) {
      return PluralCategory.ZERO;
    } else if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (op.n == 2) {
      return PluralCategory.TWO;
    } else if (op.n == 3) {
      return PluralCategory.FEW;
    } else if (op.n == 6) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_12 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n1000 = (op.n % 1000);
    final double mod_n1000000 = (op.n % 1000000);
    final double mod_n100000 = (op.n % 100000);
    if (op.n == 0) {
      return PluralCategory.ZERO;
    } else if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (((mod_n100 == 2) || (mod_n100 == 22) || (mod_n100 == 42) || (mod_n100 == 62) || (mod_n100 == 82)) || mod_n1000 == 0 && (contains(mod_n100000, 1000, 20000) || (mod_n100000 == 40000) || (mod_n100000 == 60000) || (mod_n100000 == 80000)) || op.n != 0 && mod_n1000000 == 100000) {
      return PluralCategory.TWO;
    } else if (((mod_n100 == 3) || (mod_n100 == 23) || (mod_n100 == 43) || (mod_n100 == 63) || (mod_n100 == 83))) {
      return PluralCategory.FEW;
    } else if (op.n != 1 && ((mod_n100 == 1) || (mod_n100 == 21) || (mod_n100 == 41) || (mod_n100 == 61) || (mod_n100 == 81))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_13 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n10 = (op.n % 10);
    if (mod_n10 == 1 && !(contains(mod_n100, 11, 19))) {
      return PluralCategory.ONE;
    } else if ((contains(mod_n10, 2, 9)) && !(contains(mod_n100, 11, 19))) {
      return PluralCategory.FEW;
    } else if (op.f != 0) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_14 = (op) -> {
      final double mod_n100 = (op.n % 100);
    if (op.n == 0) {
      return PluralCategory.ZERO;
    } else if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (op.n == 2) {
      return PluralCategory.TWO;
    } else if ((contains(mod_n100, 3, 10))) {
      return PluralCategory.FEW;
    } else if ((contains(mod_n100, 11, 99))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_15 = (op) -> {
      return PluralCategory.OTHER;
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_16 = (op) -> {
      if (((op.n == 0) || (op.n == 1)) || op.i == 0 && op.f == 1) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_17 = (op) -> {
      final long mod_i10 = (op.i % 10);
    final int mod_f10 = (op.f % 10);
    if (op.v == 0 && ((op.i == 1) || (op.i == 2) || (op.i == 3)) || op.v == 0 && !((mod_i10 == 4) || (mod_i10 == 6) || (mod_i10 == 9)) || op.v != 0 && !((mod_f10 == 4) || (mod_f10 == 6) || (mod_f10 == 9))) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_18 = (op) -> {
      final long mod_i1000000 = (op.i % 1000000);
    if ((contains(op.i, 0, 1))) {
      return PluralCategory.ONE;
    } else if (op.e == 0 && op.i != 0 && mod_i1000000 == 0 && op.v == 0 || !(contains(op.e, 0, 5))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_19 = (op) -> {
      final long mod_i10 = (op.i % 10);
    final long mod_i100 = (op.i % 100);
    final int mod_f10 = (op.f % 10);
    final int mod_f100 = (op.f % 100);
    if (op.v == 0 && mod_i10 == 1 && mod_i100 != 11 || mod_f10 == 1 && mod_f100 != 11) {
      return PluralCategory.ONE;
    } else if (op.v == 0 && (contains(mod_i10, 2, 4)) && !(contains(mod_i100, 12, 14)) || (contains(mod_f10, 2, 4)) && !(contains(mod_f100, 12, 14))) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_20 = (op) -> {
      if (((op.n == 1) || (op.n == 11))) {
      return PluralCategory.ONE;
    } else if (((op.n == 2) || (op.n == 12))) {
      return PluralCategory.TWO;
    } else if ((contains(op.n, 3, 10) || contains(op.n, 13, 19))) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_21 = (op) -> {
      final long mod_i10 = (op.i % 10);
    final long mod_i100 = (op.i % 100);
    if (op.i == 1 && op.v == 0) {
      return PluralCategory.ONE;
    } else if (op.v == 0 && (contains(mod_i10, 2, 4)) && !(contains(mod_i100, 12, 14))) {
      return PluralCategory.FEW;
    } else if (op.v == 0 && op.i != 1 && (contains(mod_i10, 0, 1)) || op.v == 0 && (contains(mod_i10, 5, 9)) || op.v == 0 && (contains(mod_i100, 12, 14))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_22 = (op) -> {
      final long mod_i10 = (op.i % 10);
    final long mod_i100 = (op.i % 100);
    final int mod_f10 = (op.f % 10);
    final int mod_f100 = (op.f % 100);
    if (op.v == 0 && mod_i10 == 1 && mod_i100 != 11 || mod_f10 == 1 && mod_f100 != 11) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_23 = (op) -> {
      if (op.i == 1 && op.v == 0) {
      return PluralCategory.ONE;
    } else if ((contains(op.i, 2, 4)) && op.v == 0) {
      return PluralCategory.FEW;
    } else if (op.v != 0) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_24 = (op) -> {
      final double mod_n100 = (op.n % 100);
    if (op.i == 1 && op.v == 0) {
      return PluralCategory.ONE;
    } else if (op.v != 0 || op.n == 0 || op.n != 1 && (contains(mod_n100, 1, 19))) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_25 = (op) -> {
      if (op.n == 1) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_26 = (op) -> {
      final long mod_i1000000 = (op.i % 1000000);
    if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (op.e == 0 && op.i != 0 && mod_i1000000 == 0 && op.v == 0 || !(contains(op.e, 0, 5))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_27 = (op) -> {
      final long mod_i1000000 = (op.i % 1000000);
    if (op.i == 1 && op.v == 0) {
      return PluralCategory.ONE;
    } else if (op.e == 0 && op.i != 0 && mod_i1000000 == 0 && op.v == 0 || !(contains(op.e, 0, 5))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_28 = (op) -> {
      if (op.n == 0) {
      return PluralCategory.ZERO;
    } else if (op.n == 1) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_29 = (op) -> {
      if (op.i == 1 && op.v == 0 || op.i == 0 && op.v != 0) {
      return PluralCategory.ONE;
    } else if (op.i == 2 && op.v == 0) {
      return PluralCategory.TWO;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_30 = (op) -> {
      final long mod_i100 = (op.i % 100);
    final int mod_f100 = (op.f % 100);
    if (op.v == 0 && mod_i100 == 1 || mod_f100 == 1) {
      return PluralCategory.ONE;
    } else if (op.v == 0 && mod_i100 == 2 || mod_f100 == 2) {
      return PluralCategory.TWO;
    } else if (op.v == 0 && (contains(mod_i100, 3, 4)) || (contains(mod_f100, 3, 4))) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_31 = (op) -> {
      if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (op.n == 2) {
      return PluralCategory.TWO;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_32 = (op) -> {
      final double mod_n100 = (op.n % 100);
    if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (op.n == 2) {
      return PluralCategory.TWO;
    } else if (op.n == 0 || (contains(mod_n100, 3, 10))) {
      return PluralCategory.FEW;
    } else if ((contains(mod_n100, 11, 19))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_33 = (op) -> {
      if (op.i == 0 || op.n == 1) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_34 = (op) -> {
      if (op.i == 1 && op.v == 0) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_35 = (op) -> {
      final long mod_i10 = (op.i % 10);
    final long mod_i100 = (op.i % 100);
    if (op.v == 0 && mod_i10 == 1 && mod_i100 != 11) {
      return PluralCategory.ONE;
    } else if (op.v == 0 && (contains(mod_i10, 2, 4)) && !(contains(mod_i100, 12, 14))) {
      return PluralCategory.FEW;
    } else if (op.v == 0 && mod_i10 == 0 || op.v == 0 && (contains(mod_i10, 5, 9)) || op.v == 0 && (contains(mod_i100, 11, 14))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_36 = (op) -> {
      if (op.i == 0 || op.n == 1) {
      return PluralCategory.ONE;
    } else if ((contains(op.n, 2, 10))) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_37 = (op) -> {
      if ((contains(op.n, 0, 1)) || (contains(op.n, 11, 99))) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _C_RULE_38 = (op) -> {
      final long mod_i10 = (op.i % 10);
    final long mod_i100 = (op.i % 100);
    if (op.v == 0 && mod_i10 == 1) {
      return PluralCategory.ONE;
    } else if (op.v == 0 && mod_i10 == 2) {
      return PluralCategory.TWO;
    } else if (op.v == 0 && ((mod_i100 == 0) || (mod_i100 == 20) || (mod_i100 == 40) || (mod_i100 == 60) || (mod_i100 == 80))) {
      return PluralCategory.FEW;
    } else if (op.v != 0) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_0 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n10 = (op.n % 10);
    if (mod_n10 == 1 && mod_n100 != 11) {
      return PluralCategory.ONE;
    } else if (mod_n10 == 2 && mod_n100 != 12) {
      return PluralCategory.TWO;
    } else if (mod_n10 == 3 && mod_n100 != 13) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_1 = (op) -> {
      if (((op.n == 1) || (op.n == 5) || (op.n == 7) || (op.n == 8) || (op.n == 9) || (op.n == 10))) {
      return PluralCategory.ONE;
    } else if (((op.n == 2) || (op.n == 3))) {
      return PluralCategory.TWO;
    } else if (op.n == 4) {
      return PluralCategory.FEW;
    } else if (op.n == 6) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_2 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n10 = (op.n % 10);
    if (((mod_n10 == 1) || (mod_n10 == 2)) && !((mod_n100 == 11) || (mod_n100 == 12))) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_3 = (op) -> {
      if (((op.n == 1) || (op.n == 5) || contains(op.n, 7, 9))) {
      return PluralCategory.ONE;
    } else if (((op.n == 2) || (op.n == 3))) {
      return PluralCategory.TWO;
    } else if (op.n == 4) {
      return PluralCategory.FEW;
    } else if (op.n == 6) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_4 = (op) -> {
      if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (((op.n == 2) || (op.n == 3))) {
      return PluralCategory.TWO;
    } else if (op.n == 4) {
      return PluralCategory.FEW;
    } else if (op.n == 6) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_5 = (op) -> {
      final double mod_n100 = (op.n % 100);
    if ((contains(op.n, 1, 4)) || (contains(mod_n100, 1, 4) || contains(mod_n100, 21, 24) || contains(mod_n100, 41, 44) || contains(mod_n100, 61, 64) || contains(mod_n100, 81, 84))) {
      return PluralCategory.ONE;
    } else if (op.n == 5 || mod_n100 == 5) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_6 = _C_RULE_15;

  private static final Function<PluralOperand, PluralCategory> _O_RULE_7 = (op) -> {
      if (((op.n == 11) || (op.n == 8) || (op.n == 80) || (op.n == 800))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_8 = _C_RULE_25;

  private static final Function<PluralOperand, PluralCategory> _O_RULE_9 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n10 = (op.n % 10);
    if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (mod_n10 == 4 && mod_n100 != 14) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_10 = (op) -> {
      final long mod_i10 = (op.i % 10);
    final long mod_i1000 = (op.i % 1000);
    final long mod_i100 = (op.i % 100);
    if (((mod_i10 == 1) || (mod_i10 == 2) || (mod_i10 == 5) || (mod_i10 == 7) || (mod_i10 == 8)) || ((mod_i100 == 20) || (mod_i100 == 50) || (mod_i100 == 70) || (mod_i100 == 80))) {
      return PluralCategory.ONE;
    } else if (((mod_i10 == 3) || (mod_i10 == 4)) || ((mod_i1000 == 100) || (mod_i1000 == 200) || (mod_i1000 == 300) || (mod_i1000 == 400) || (mod_i1000 == 500) || (mod_i1000 == 600) || (mod_i1000 == 700) || (mod_i1000 == 800) || (mod_i1000 == 900))) {
      return PluralCategory.FEW;
    } else if (op.i == 0 || mod_i10 == 6 || ((mod_i100 == 40) || (mod_i100 == 60) || (mod_i100 == 90))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_11 = (op) -> {
      if (((op.n == 11) || (op.n == 8) || contains(op.n, 80, 89) || contains(op.n, 800, 899))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_12 = (op) -> {
      if (((op.n == 0) || (op.n == 7) || (op.n == 8) || (op.n == 9))) {
      return PluralCategory.ZERO;
    } else if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (op.n == 2) {
      return PluralCategory.TWO;
    } else if (((op.n == 3) || (op.n == 4))) {
      return PluralCategory.FEW;
    } else if (((op.n == 5) || (op.n == 6))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_13 = (op) -> {
      if (((op.n == 1) || (op.n == 5))) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_14 = (op) -> {
      if (((op.n == 1) || (op.n == 3))) {
      return PluralCategory.ONE;
    } else if (op.n == 2) {
      return PluralCategory.TWO;
    } else if (op.n == 4) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_15 = (op) -> {
      if (op.n == 1) {
      return PluralCategory.ONE;
    } else if (((op.n == 2) || (op.n == 3))) {
      return PluralCategory.TWO;
    } else if (op.n == 4) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_16 = (op) -> {
      final long mod_i10 = (op.i % 10);
    final long mod_i100 = (op.i % 100);
    if (mod_i10 == 1 && mod_i100 != 11) {
      return PluralCategory.ONE;
    } else if (mod_i10 == 2 && mod_i100 != 12) {
      return PluralCategory.TWO;
    } else if (((mod_i10 == 7) || (mod_i10 == 8)) && !((mod_i100 == 17) || (mod_i100 == 18))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_17 = (op) -> {
      if (((op.n == 1) || (op.n == 11))) {
      return PluralCategory.ONE;
    } else if (((op.n == 2) || (op.n == 12))) {
      return PluralCategory.TWO;
    } else if (((op.n == 3) || (op.n == 13))) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_18 = (op) -> {
      final double mod_n10 = (op.n % 10);
    if (mod_n10 == 6 || mod_n10 == 9 || mod_n10 == 0 && op.n != 0) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_19 = (op) -> {
      final long mod_i100 = (op.i % 100);
    if (op.i == 1) {
      return PluralCategory.ONE;
    } else if (op.i == 0 || (contains(mod_i100, 2, 20) || (mod_i100 == 40) || (mod_i100 == 60) || (mod_i100 == 80))) {
      return PluralCategory.MANY;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_20 = (op) -> {
      if ((contains(op.n, 1, 4))) {
      return PluralCategory.ONE;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_21 = (op) -> {
      final double mod_n10 = (op.n % 10);
    if (((mod_n10 == 6) || (mod_n10 == 9)) || op.n == 10) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_22 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n10 = (op.n % 10);
    if (((mod_n10 == 2) || (mod_n10 == 3)) && !((mod_n100 == 12) || (mod_n100 == 13))) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_23 = (op) -> {
      final double mod_n100 = (op.n % 100);
    final double mod_n10 = (op.n % 10);
    if (mod_n10 == 3 && mod_n100 != 13) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static final Function<PluralOperand, PluralCategory> _O_RULE_24 = (op) -> {
      if (op.i == 0) {
      return PluralCategory.ZERO;
    } else if (op.i == 1) {
      return PluralCategory.ONE;
    } else if (((op.i == 2) || (op.i == 3) || (op.i == 4) || (op.i == 5) || (op.i == 6))) {
      return PluralCategory.FEW;
    } else {
      return PluralCategory.OTHER;
    }
  };

  private static boolean contains(final double val, final double min, final double max) {
    return (val >= min) && (val <= max) && ((val % 1) == 0);
  }

  private static boolean contains(final int val, final int min, final int max) {
    return (val >= min) && (val <= max);
  }

  private static boolean contains(final long val, final long min, final long max) {
    return (val >= min) && (val <= max);
  }

  public static Function<PluralOperand, PluralCategory> selectCardinal(final String language,
      final String region) {
    switch (language) {
      case "sl":  return _C_RULE_0;
      case "br":  return _C_RULE_1;
      case "ga":  return _C_RULE_2;
      case "pa":  return _C_RULE_3;
      case "ln":  return _C_RULE_3;
      case "ti":  return _C_RULE_3;
      case "csw":  return _C_RULE_3;
      case "bho":  return _C_RULE_3;
      case "guw":  return _C_RULE_3;
      case "ak":  return _C_RULE_3;
      case "wa":  return _C_RULE_3;
      case "mg":  return _C_RULE_3;
      case "nso":  return _C_RULE_3;
      case "ff":  return _C_RULE_4;
      case "hy":  return _C_RULE_4;
      case "kab":  return _C_RULE_4;
      case "da":  return _C_RULE_5;
      case "be":  return _C_RULE_6;
      case "fr":  return _C_RULE_7;
      case "is":  return _C_RULE_8;
      case "lag":  return _C_RULE_9;
      case "prg":  return _C_RULE_10;
      case "lv":  return _C_RULE_10;
      case "cy":  return _C_RULE_11;
      case "kw":  return _C_RULE_12;
      case "lt":  return _C_RULE_13;
      case "ar":  return _C_RULE_14;
      case "ars":  return _C_RULE_14;
      case "":  return _C_RULE_15;
      case "lo":  return _C_RULE_15;
      case "kea":  return _C_RULE_15;
      case "yue":  return _C_RULE_15;
      case "bm":  return _C_RULE_15;
      case "jv":  return _C_RULE_15;
      case "bo":  return _C_RULE_15;
      case "nqo":  return _C_RULE_15;
      case "jw":  return _C_RULE_15;
      case "sg":  return _C_RULE_15;
      case "lkt":  return _C_RULE_15;
      case "dz":  return _C_RULE_15;
      case "yo":  return _C_RULE_15;
      case "root":  return _C_RULE_15;
      case "wo":  return _C_RULE_15;
      case "id":  return _C_RULE_15;
      case "sah":  return _C_RULE_15;
      case "ig":  return _C_RULE_15;
      case "ses":  return _C_RULE_15;
      case "ii":  return _C_RULE_15;
      case "su":  return _C_RULE_15;
      case "km":  return _C_RULE_15;
      case "ko":  return _C_RULE_15;
      case "hnj":  return _C_RULE_15;
      case "ms":  return _C_RULE_15;
      case "und":  return _C_RULE_15;
      case "tpi":  return _C_RULE_15;
      case "my":  return _C_RULE_15;
      case "kde":  return _C_RULE_15;
      case "zh":  return _C_RULE_15;
      case "vi":  return _C_RULE_15;
      case "th":  return _C_RULE_15;
      case "jbo":  return _C_RULE_15;
      case "ja":  return _C_RULE_15;
      case "to":  return _C_RULE_15;
      case "osa":  return _C_RULE_15;
      case "si":  return _C_RULE_16;
      case "ceb":  return _C_RULE_17;
      case "fil":  return _C_RULE_17;
      case "tl":  return _C_RULE_17;
      case "bs":  return _C_RULE_19;
      case "sh":  return _C_RULE_19;
      case "hr":  return _C_RULE_19;
      case "sr":  return _C_RULE_19;
      case "gd":  return _C_RULE_20;
      case "pl":  return _C_RULE_21;
      case "mk":  return _C_RULE_22;
      case "cs":  return _C_RULE_23;
      case "sk":  return _C_RULE_23;
      case "mo":  return _C_RULE_24;
      case "ro":  return _C_RULE_24;
      case "jmc":  return _C_RULE_25;
      case "nyn":  return _C_RULE_25;
      case "cgg":  return _C_RULE_25;
      case "fur":  return _C_RULE_25;
      case "ps":  return _C_RULE_25;
      case "gsw":  return _C_RULE_25;
      case "bem":  return _C_RULE_25;
      case "kkj":  return _C_RULE_25;
      case "bal":  return _C_RULE_25;
      case "hu":  return _C_RULE_25;
      case "kcg":  return _C_RULE_25;
      case "vun":  return _C_RULE_25;
      case "dv":  return _C_RULE_25;
      case "ug":  return _C_RULE_25;
      case "tig":  return _C_RULE_25;
      case "mgo":  return _C_RULE_25;
      case "nah":  return _C_RULE_25;
      case "bez":  return _C_RULE_25;
      case "ssy":  return _C_RULE_25;
      case "pap":  return _C_RULE_25;
      case "teo":  return _C_RULE_25;
      case "haw":  return _C_RULE_25;
      case "ml":  return _C_RULE_25;
      case "ee":  return _C_RULE_25;
      case "mn":  return _C_RULE_25;
      case "af":  return _C_RULE_25;
      case "uz":  return _C_RULE_25;
      case "mr":  return _C_RULE_25;
      case "el":  return _C_RULE_25;
      case "brx":  return _C_RULE_25;
      case "nnh":  return _C_RULE_25;
      case "eo":  return _C_RULE_25;
      case "chr":  return _C_RULE_25;
      case "an":  return _C_RULE_25;
      case "ve":  return _C_RULE_25;
      case "eu":  return _C_RULE_25;
      case "nb":  return _C_RULE_25;
      case "sdh":  return _C_RULE_25;
      case "nd":  return _C_RULE_25;
      case "ne":  return _C_RULE_25;
      case "az":  return _C_RULE_25;
      case "vo":  return _C_RULE_25;
      case "rm":  return _C_RULE_25;
      case "nn":  return _C_RULE_25;
      case "no":  return _C_RULE_25;
      case "nr":  return _C_RULE_25;
      case "bg":  return _C_RULE_25;
      case "rwk":  return _C_RULE_25;
      case "rof":  return _C_RULE_25;
      case "fo":  return _C_RULE_25;
      case "ny":  return _C_RULE_25;
      case "syr":  return _C_RULE_25;
      case "sd":  return _C_RULE_25;
      case "xog":  return _C_RULE_25;
      case "jgo":  return _C_RULE_25;
      case "kaj":  return _C_RULE_25;
      case "ka":  return _C_RULE_25;
      case "seh":  return _C_RULE_25;
      case "wae":  return _C_RULE_25;
      case "sn":  return _C_RULE_25;
      case "so":  return _C_RULE_25;
      case "sq":  return _C_RULE_25;
      case "mas":  return _C_RULE_25;
      case "om":  return _C_RULE_25;
      case "ss":  return _C_RULE_25;
      case "kk":  return _C_RULE_25;
      case "st":  return _C_RULE_25;
      case "kl":  return _C_RULE_25;
      case "ce":  return _C_RULE_25;
      case "saq":  return _C_RULE_25;
      case "or":  return _C_RULE_25;
      case "os":  return _C_RULE_25;
      case "ks":  return _C_RULE_25;
      case "ku":  return _C_RULE_25;
      case "ta":  return _C_RULE_25;
      case "asa":  return _C_RULE_25;
      case "ky":  return _C_RULE_25;
      case "xh":  return _C_RULE_25;
      case "te":  return _C_RULE_25;
      case "lb":  return _C_RULE_25;
      case "tk":  return _C_RULE_25;
      case "ha":  return _C_RULE_25;
      case "tn":  return _C_RULE_25;
      case "ckb":  return _C_RULE_25;
      case "lg":  return _C_RULE_25;
      case "ksb":  return _C_RULE_25;
      case "tr":  return _C_RULE_25;
      case "ts":  return _C_RULE_25;
      case "es":  return _C_RULE_26;
      case "lld":  return _C_RULE_27;
      case "vec":  return _C_RULE_27;
      case "it":  return _C_RULE_27;
      case "ca":  return _C_RULE_27;
      case "scn":  return _C_RULE_27;
      case "ksh":  return _C_RULE_28;
      case "blo":  return _C_RULE_28;
      case "he":  return _C_RULE_29;
      case "dsb":  return _C_RULE_30;
      case "hsb":  return _C_RULE_30;
      case "smj":  return _C_RULE_31;
      case "se":  return _C_RULE_31;
      case "smn":  return _C_RULE_31;
      case "sma":  return _C_RULE_31;
      case "sat":  return _C_RULE_31;
      case "sms":  return _C_RULE_31;
      case "iu":  return _C_RULE_31;
      case "smi":  return _C_RULE_31;
      case "naq":  return _C_RULE_31;
      case "mt":  return _C_RULE_32;
      case "hi":  return _C_RULE_33;
      case "as":  return _C_RULE_33;
      case "kn":  return _C_RULE_33;
      case "fa":  return _C_RULE_33;
      case "pcm":  return _C_RULE_33;
      case "zu":  return _C_RULE_33;
      case "am":  return _C_RULE_33;
      case "bn":  return _C_RULE_33;
      case "gu":  return _C_RULE_33;
      case "doi":  return _C_RULE_33;
      case "de":  return _C_RULE_34;
      case "sv":  return _C_RULE_34;
      case "ast":  return _C_RULE_34;
      case "fi":  return _C_RULE_34;
      case "sw":  return _C_RULE_34;
      case "gl":  return _C_RULE_34;
      case "io":  return _C_RULE_34;
      case "en":  return _C_RULE_34;
      case "lij":  return _C_RULE_34;
      case "et":  return _C_RULE_34;
      case "sc":  return _C_RULE_34;
      case "yi":  return _C_RULE_34;
      case "fy":  return _C_RULE_34;
      case "ia":  return _C_RULE_34;
      case "ur":  return _C_RULE_34;
      case "nl":  return _C_RULE_34;
      case "ru":  return _C_RULE_35;
      case "uk":  return _C_RULE_35;
      case "shi":  return _C_RULE_36;
      case "tzm":  return _C_RULE_37;
      case "gv":  return _C_RULE_38;
      case "pt": {
        if ("PT".equals(region)) {
             return _C_RULE_27;
        } else {
             return _C_RULE_18;
        }
      }
      default:
        return null;
    }
  }

  public static Function<PluralOperand, PluralCategory> selectOrdinal(final String language,
      final String region) {
    switch (language) {
      case "en":  return _O_RULE_0;
      case "as":  return _O_RULE_1;
      case "bn":  return _O_RULE_1;
      case "sv":  return _O_RULE_2;
      case "or":  return _O_RULE_3;
      case "hi":  return _O_RULE_4;
      case "gu":  return _O_RULE_4;
      case "kw":  return _O_RULE_5;
      case "":  return _O_RULE_6;
      case "de":  return _O_RULE_6;
      case "ps":  return _O_RULE_6;
      case "ast":  return _O_RULE_6;
      case "pt":  return _O_RULE_6;
      case "lt":  return _O_RULE_6;
      case "gsw":  return _O_RULE_6;
      case "hr":  return _O_RULE_6;
      case "lv":  return _O_RULE_6;
      case "ia":  return _O_RULE_6;
      case "id":  return _O_RULE_6;
      case "ur":  return _O_RULE_6;
      case "ml":  return _O_RULE_6;
      case "mn":  return _O_RULE_6;
      case "prg":  return _O_RULE_6;
      case "af":  return _O_RULE_6;
      case "uz":  return _O_RULE_6;
      case "el":  return _O_RULE_6;
      case "is":  return _O_RULE_6;
      case "am":  return _O_RULE_6;
      case "my":  return _O_RULE_6;
      case "an":  return _O_RULE_6;
      case "zh":  return _O_RULE_6;
      case "es":  return _O_RULE_6;
      case "et":  return _O_RULE_6;
      case "eu":  return _O_RULE_6;
      case "ar":  return _O_RULE_6;
      case "dsb":  return _O_RULE_6;
      case "nb":  return _O_RULE_6;
      case "hsb":  return _O_RULE_6;
      case "ja":  return _O_RULE_6;
      case "zu":  return _O_RULE_6;
      case "fa":  return _O_RULE_6;
      case "nl":  return _O_RULE_6;
      case "no":  return _O_RULE_6;
      case "ru":  return _O_RULE_6;
      case "fi":  return _O_RULE_6;
      case "bg":  return _O_RULE_6;
      case "yue":  return _O_RULE_6;
      case "bs":  return _O_RULE_6;
      case "sd":  return _O_RULE_6;
      case "fy":  return _O_RULE_6;
      case "sh":  return _O_RULE_6;
      case "si":  return _O_RULE_6;
      case "root":  return _O_RULE_6;
      case "sk":  return _O_RULE_6;
      case "sl":  return _O_RULE_6;
      case "sr":  return _O_RULE_6;
      case "ce":  return _O_RULE_6;
      case "km":  return _O_RULE_6;
      case "kn":  return _O_RULE_6;
      case "ko":  return _O_RULE_6;
      case "sw":  return _O_RULE_6;
      case "gl":  return _O_RULE_6;
      case "und":  return _O_RULE_6;
      case "tpi":  return _O_RULE_6;
      case "ta":  return _O_RULE_6;
      case "ky":  return _O_RULE_6;
      case "cs":  return _O_RULE_6;
      case "te":  return _O_RULE_6;
      case "pa":  return _O_RULE_6;
      case "th":  return _O_RULE_6;
      case "pl":  return _O_RULE_6;
      case "da":  return _O_RULE_6;
      case "he":  return _O_RULE_6;
      case "tr":  return _O_RULE_6;
      case "sc":  return _O_RULE_7;
      case "vec":  return _O_RULE_7;
      case "lld":  return _O_RULE_7;
      case "it":  return _O_RULE_7;
      case "scn":  return _O_RULE_7;
      case "hy":  return _O_RULE_8;
      case "mo":  return _O_RULE_8;
      case "lo":  return _O_RULE_8;
      case "vi":  return _O_RULE_8;
      case "ms":  return _O_RULE_8;
      case "fil":  return _O_RULE_8;
      case "tl":  return _O_RULE_8;
      case "ga":  return _O_RULE_8;
      case "bal":  return _O_RULE_8;
      case "fr":  return _O_RULE_8;
      case "ro":  return _O_RULE_8;
      case "sq":  return _O_RULE_9;
      case "az":  return _O_RULE_10;
      case "lij":  return _O_RULE_11;
      case "cy":  return _O_RULE_12;
      case "hu":  return _O_RULE_13;
      case "ca":  return _O_RULE_14;
      case "mr":  return _O_RULE_15;
      case "mk":  return _O_RULE_16;
      case "gd":  return _O_RULE_17;
      case "kk":  return _O_RULE_18;
      case "ka":  return _O_RULE_19;
      case "ne":  return _O_RULE_20;
      case "tk":  return _O_RULE_21;
      case "be":  return _O_RULE_22;
      case "uk":  return _O_RULE_23;
      case "blo":  return _O_RULE_24;
      default:
        return null;
    }
  }
}
