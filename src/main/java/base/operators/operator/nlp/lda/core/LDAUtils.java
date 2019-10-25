package base.operators.operator.nlp.lda.core;

import java.util.Random;

public class LDAUtils {

	public static String zeroPad(int number, int width) {
		StringBuffer result = new StringBuffer("");
		for (int i = 0; i < width - Integer.toString(number).length(); i++)
			result.append("0");
		result.append(Integer.toString(number));

		return result.toString();
	}
	
	
	public static void main(String[] args) {
		
//		double [] p = {0.6, 0.2, 0.1, 0.1};
//		double [] q = {0.6, 0.3, 0.05, 0.05};
//		System.out.println(KLDivergence(p, q));
		int v0 = 0, v1= 0, v2 = 0, v3 = 0,v4 = 0;
		for (int i = 0; i < 10000; i++) {

			switch (new Random().nextInt(5)) {
			case 0:
				v0++;
				break;
			case 1:
				v1++;
				break;
			case 2:
				v2++;
				break;
			case 3:
				v3++;
				break;
			case 4:
				v4++;
				break;
			default:
				break;
			}

		}
		System.out.println(v0+ " " + v1 + " " + v2 + " " + v3 + " " +v4);
	}
	
	
	
	
	
	
}
