package base.operators.operator.nlp.lda.core;

import java.util.List;

public class LDATrain {
	
	// output model
	public Model trainModel;
	LDAOption option;
	
	//train init
	public boolean init(LDAOption option, List<String> text){
		this.option = option;
		trainModel = new Model();
		
		if (option.train){
			if (!trainModel.initNewModel(option, text))
				return false;
			//trainModel.data.localDict.writeWordMap(option.dir + File.separator + option.wordMapFileName);
		}
		else if (option.trainContinue){
			if (!trainModel.initEstimatedModel(option))
				return false;
		}
		
		return true;
	}
	
	//continue train init
		public boolean init(LDAOption option){
			this.option = option;
			trainModel = new Model();
			
			
			if (option.trainContinue){
				if (!trainModel.initEstimatedModel(option))
					return false;
			}
			
			return true;
		}
	
	public void train(){
		
		System.out.println("Sampling " + trainModel.niters + " iteration!");
		int lastIter = trainModel.liter;
		for (trainModel.liter = lastIter + 1; trainModel.liter < trainModel.niters + lastIter; trainModel.liter++){
			System.out.println("Iteration " + trainModel.liter + " ...");
			// for all z_i
			for (int m = 0; m < trainModel.M; m++){				
				for (int n = 0; n < trainModel.data.docs[m].length; n++){
					// z_i = z[m][n]
					// sample from p(z_i|z_-i, w)
					
					int topic = sampling(m, n);
					trainModel.z[m].set(n, topic);
				}// end for each word
			}// end for each document
			
			if (option.savestep > 0){
				if (trainModel.liter % option.savestep == 0){
			//		System.out.println("Saving the model at iteration " + trainModel.liter + " ...");
			//		computeTheta();
			//		computePhi();
			//		trainModel.saveModel("model-" + LDAUtils.zeroPad(trainModel.liter, 5));
				}
			}
		}// end iterations		
		
		System.out.println("Gibbs sampling completed!\n");
	//	System.out.println("Saving the final model!\n");
		
		computeTheta();
		computePhi();
	//	trainModel.saveModel(trainModel.modelName);
	//	trainModel.liter--;
	}
	
	/**
	 * Do sampling
	 * @param m document number
	 * @param n word number
	 * @return topic id
	 */
	public int sampling(int m, int n){
		// remove z_i from the count variable
		int topic = trainModel.z[m].get(n);
		int w = trainModel.data.docs[m].words[n];
		
		trainModel.nw[w][topic] -= 1;
		trainModel.nd[m][topic] -= 1;
		trainModel.nwsum[topic] -= 1;
		trainModel.ndsum[m] -= 1;
		
		double Vbeta = trainModel.V * trainModel.beta;
		double Kalpha = trainModel.K * trainModel.alpha;
		
		
		
		//do multinominal sampling via cumulative method
		for (int k = 0; k < trainModel.K; k++){
			trainModel.p[k] = (trainModel.nw[w][k] + trainModel.beta)/(trainModel.nwsum[k] + Vbeta) *
					(trainModel.nd[m][k] + trainModel.alpha)/(trainModel.ndsum[m] + Kalpha);
		}
		
		// cumulate multinomial parameters
		for (int k = 1; k < trainModel.K; k++){
			trainModel.p[k] += trainModel.p[k - 1];
		}
		
		// scaled sample because of unnormalized p[]
		double u = Math.random() * trainModel.p[trainModel.K - 1];
		
		for (topic = 0; topic < trainModel.K; topic++){
			if (trainModel.p[topic] > u) //sample topic w.r.t distribution p
				break;
		}
		
		// add newly estimated z_i to count variables
		trainModel.nw[w][topic] += 1;
		trainModel.nd[m][topic] += 1;
		trainModel.nwsum[topic] += 1;
		trainModel.ndsum[m] += 1;
		
 		return topic;
	}
	
	public void computeTheta(){
		for (int m = 0; m < trainModel.M; m++){
			for (int k = 0; k < trainModel.K; k++){
				trainModel.theta[m][k] = (trainModel.nd[m][k] + trainModel.alpha) / (trainModel.ndsum[m] + trainModel.K * trainModel.alpha);
			}
		}
	}
	
	public void computePhi(){
		for (int k = 0; k < trainModel.K; k++){
			for (int w = 0; w < trainModel.V; w++){
				trainModel.phi[k][w] = (trainModel.nw[w][k] + trainModel.beta) / (trainModel.nwsum[k] + trainModel.V * trainModel.beta);
			}
		}
	}

	
}
