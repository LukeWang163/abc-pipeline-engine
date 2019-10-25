package base.operators.operator.nlp.segment.kshortsegment.predict.segment;


import base.operators.operator.nlp.segment.kshortsegment.predict.ner.IDRecognition;
import base.operators.operator.nlp.segment.kshortsegment.predict.ner.NumberRecognition;
import base.operators.operator.nlp.segment.kshortsegment.predict.ner.TimeAnnotation;

public class NERAnnotation {
	
	/**
	 * 地址、时间、ID等NER标注
	 * 
	 * @param wordNet
	 */
	
	public static void annotate(final WordNet wordNet) {
		
		// 机构名识别
		//PlaceAnnotation.annotate(wordNet);
		
		// 数字识别
		NumberRecognition.recognition(wordNet);
		
		// 时间识别
		//TimeAnnotation.annotate(wordNet);
		new TimeAnnotation().annotate2(wordNet);
		
		// ID(身份证号、电话号码、社交账号等)识别
		if (wordNet.isHasLetter() || wordNet.isHasNumber())
			IDRecognition.recognition(wordNet);
		
	}
}
