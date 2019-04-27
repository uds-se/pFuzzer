package utils;

public enum Predicate {
     // Opcode             U L G E    Intuitive operation
     FCMP_FALSE(0) ,  ///< 0 0 0 0    Always false (always folded)
     FCMP_OEQ(1),     ///< 0 0 0 1    True if ordered and equal
     FCMP_OGT(2),     ///< 0 0 1 0    True if ordered and greater than
     FCMP_OGE(3),     ///< 0 0 1 1    True if ordered and greater than or equal
     FCMP_OLT(4),     ///< 0 1 0 0    True if ordered and less than
     FCMP_OLE(5),     ///< 0 1 0 1    True if ordered and less than or equal
     FCMP_ONE(6),     ///< 0 1 1 0    True if ordered and operands are unequal
     FCMP_ORD(7),     ///< 0 1 1 1    True if ordered (no nans)
     FCMP_UNO(8),     ///< 1 0 0 0    True if unordered: isnan(X) | isnan(Y)
     FCMP_UEQ(9),     ///< 1 0 0 1    True if unordered or equal
     FCMP_UGT(10),    ///< 1 0 1 0    True if unordered or greater than
     FCMP_UGE(11),    ///< 1 0 1 1    True if unordered, greater than, or equal
     FCMP_ULT(12),    ///< 1 1 0 0    True if unordered or less than
     FCMP_ULE(13),    ///< 1 1 0 1    True if unordered, less than, or equal
     FCMP_UNE(14),    ///< 1 1 1 0    True if unordered or not equal
     FCMP_TRUE(15),   ///< 1 1 1 1    Always true (always folded)
     BAD_FCMP_PREDICATE(16),
     ICMP_EQ(32),     ///< equal
     ICMP_NE(33),     ///< not equal
     ICMP_UGT(34),    ///< unsigned greater than
     ICMP_UGE(35),    ///< unsigned greater or equal
     ICMP_ULT(36),    ///< unsigned less than
     ICMP_ULE(37),    ///< unsigned less or equal
     ICMP_SGT(38),    ///< signed greater than
     ICMP_SGE(39),    ///< signed greater or equal
     ICMP_SLT(40),    ///< signed less than
     ICMP_SLE(41),    ///< signed less or equal
     BAD_ICMP_PREDICATE(42);

     private final int value;

     Predicate(final int newValue) {
         value = newValue;
     }

     public int getValue() { return value; }

     public static Predicate getICMPpredicate(int predicate) {
         switch (predicate) {
         case 32: return ICMP_EQ;
         case 33: return ICMP_NE;
         case 34: return ICMP_UGT;
         case 35: return ICMP_UGE;
         case 36: return ICMP_ULT;
         case 37: return ICMP_ULE;
         case 38: return ICMP_SGT;
         case 39: return ICMP_SGE;
         case 40: return ICMP_SLT;
         case 41: return ICMP_SLE;
         }

         return BAD_ICMP_PREDICATE;
     }
}
