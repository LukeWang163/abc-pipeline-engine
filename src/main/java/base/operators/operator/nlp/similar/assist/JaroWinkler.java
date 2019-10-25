package base.operators.operator.nlp.similar.assist;

import java.util.Arrays;

public class JaroWinkler {

    /**
     * Represents a failed index search.
     */
    public static final int INDEX_NOT_FOUND = -1;
    /**
     * Find the Jaro Distance which indicates the similarity score
     * between two CharSequences.
     * @param left the first CharSequence, must not be null
     * @param right the second CharSequence, must not be null
     * @return result distance
     * @throws IllegalArgumentException if either CharSequence input is {@code null}
     */

    public static Double jaroSim(CharSequence left, CharSequence right) {

        if (left == null || right == null) {
            throw new IllegalArgumentException("CharSequences must not be null");
        }

        final int[] mtp = matches(left, right);
        final double m = mtp[0];
        if (m == 0) {
            return 0D;
        }
        final double j = ((m / left.length() + m / right.length() + (m - (double) mtp[1] / 2) / m)) / 3;
        return j;
    }

    /**
     * Find the Jaro Winkler Distance which indicates the similarity score
     * between two CharSequences.
     * @param left the first CharSequence, must not be null
     * @param right the second CharSequence, must not be null
     * @return result distance
     * @throws IllegalArgumentException if either CharSequence input is {@code null}
     */
    public static Double jaroWinklerSim(CharSequence left, CharSequence right, double scalingFactor) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("CharSequences must not be null");
        }

        final int[] mtp = matches(left, right);
        final double m = mtp[0];
        if (m == 0) {
            return 0D;
        }
        final double j = ((m / left.length() + m / right.length() + (m - (double) mtp[1] / 2) / m)) / 3;
        final double jw = j < 0.7D ? j : j + scalingFactor * mtp[2] * (1D - j);
        return jw;
    }

    public static Double jaroWinklerSim(CharSequence left, CharSequence right) {
        final double defaultScalingFactor = 0.1;
        return jaroWinklerSim(left, right, defaultScalingFactor);
    }

    /**
     * This method returns the Jaro-Winkler string matches, half transpositions, prefix array.
     *
     * @param first the first string to be matched
     * @param second the second string to be matched
     * @return mtp array containing: matches, half transpositions, and prefix
     */
    protected static int[] matches(CharSequence first, CharSequence second) {
        CharSequence max, min;
        if (first.length() > second.length()) {
            max = first;
            min = second;
        } else {
            max = second;
            min = first;
        }
        final int range = Math.max(max.length() / 2 - 1, 0);
        final int[] matchIndexes = new int[min.length()];
        Arrays.fill(matchIndexes, -1);
        final boolean[] matchFlags = new boolean[max.length()];
        int matches = 0;
        for (int mi = 0; mi < min.length(); mi++) {
            final char c1 = min.charAt(mi);
            for (int xi = Math.max(mi - range, 0), xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {
                if (!matchFlags[xi] && c1 == max.charAt(xi)) {
                    matchIndexes[mi] = xi;
                    matchFlags[xi] = true;
                    matches++;
                    break;
                }
            }
        }
        final char[] ms1 = new char[matches];
        final char[] ms2 = new char[matches];
        for (int i = 0, si = 0; i < min.length(); i++) {
            if (matchIndexes[i] != -1) {
                ms1[si] = min.charAt(i);
                si++;
            }
        }
        for (int i = 0, si = 0; i < max.length(); i++) {
            if (matchFlags[i]) {
                ms2[si] = max.charAt(i);
                si++;
            }
        }
        int halfTranspositions = 0;
        for (int mi = 0; mi < ms1.length; mi++) {
            if (ms1[mi] != ms2[mi]) {
                halfTranspositions++;
            }
        }
        int prefix = 0;
        for (int mi = 0; mi < Math.min(4, min.length()); mi++) {
            if (first.charAt(mi) == second.charAt(mi)) {
                prefix++;
            } else {
                break;
            }
        }
        return new int[] {matches, halfTranspositions, prefix};
    }
    public static double mongeElkan(String[] s1, String[] s2){
        double sum_of_maxes = 0;
        if (s1 == s2){
            return 1;
        }else if (s1.length == 0 || s2.length ==0){
            return 0;
        }else{
            for (String str1 : s1){
                double max_sim = 0;
                for (String str2 : s2){
                    double sim = jaroSim(str1, str2);
                    max_sim = Math.max(max_sim, sim);
                }
                sum_of_maxes += max_sim;
            }
        }
        return sum_of_maxes / s1.length;
    }
//    public static void main(String[] args){
//        String s1 = "fhudvfhuv";
//        String s2 = "lllltfhlluvyyyy";
//        System.out.println(JaroWinkler.jaroWinklerSim(s1,s2));
//    }
}
