package base.operators.operator.nlp.lda.core;

import java.io.Serializable;


public class LDAOption implements Serializable {
	
	private static final long serialVersionUID = 526166310343530738L;
	public static final long BUFFER_SIZE_LONG = 1000000;
	public static final short BUFFER_SIZE_SHORT = 512;
	
	public static final int MODEL_STATUS_UNKNOWN = 0;
	public static final int MODEL_STATUS_EST = 1;
	public static final int MODEL_STATUS_ESTC = 2;
	public static final int MODEL_STATUS_INF = 3;
	//Specify whether we want to train model from scratch
	public boolean train = false;  ////
	
	//Specify whether we want to continue train with the last model
	public boolean trainContinue = false;   ///
	
	//Specify whether we want to do inference(predict)
	public boolean predict = false;    ////
	
	public String dir = "";  //Specify directory   ////
	public String dfile = "";  //Specify resource data filename    ////
	
	//Specify the model level to which you want to applied. ///
	public String modelName = "model-final";  ///
	
	public int K = 10;  //Specify the number of topics  ///
	
	public double alpha = 0.2;  //Specify alpha  ////
	public double beta = 0.01;  //Specify beta

	public int niters = 1000;  //Specify the number of iterations  //
	
	public int burnStep = 500;
	
	//Specify the number of steps to save the model since the last save.
	//The step (counted by the number of Gibbs sampling iterations) 
	//at which the LDA model is saved to hard disk.
	//调试时保留
	public int savestep = 200;
	
	//Specify the number of most likely words to be printed for each topic
	public int twords = 50;   ///
	
	//Specify whether we include raw data in the input
	public boolean withrawdata = false;
	
	//Specify the wordmap file
	public String wordMapFileName = "wordmap.txt";  ////

	public static String chartSet = "utf-8";
}
