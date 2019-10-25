package base.operators.operator.nlp.lda.core;

import java.io.Serializable;
import java.util.List;


public class LDAInferencer implements Serializable{	

	private static final long serialVersionUID = 466563090503055129L;
	
	// Train model
	public Model trainModel;
	public Dictionary globalDict;
	private LDAOption option;
	
	private Model newModel;
	public int niters = 100;
	
	//-----------------------------------------------------
	// Init method
	//-----------------------------------------------------
	public boolean init(LDAOption option){
		this.option = option;
		niters = option.niters;
		trainModel = new Model();
		
		if (!trainModel.initEstimatedModel(option))
			return false;		
		
		globalDict = trainModel.data.localDict;
		computeTrnTheta();
		computeTrnPhi();
		
		return true;
	}
	
	//预测入口
	public Model inference(List<String> text){
		//System.out.println("inference");
		//Model newModel = new Model();   //////////////// never read????
		
		//System.out.println("read dataset");
		LDADataset dataset = LDADataset.buildDataSet(text, globalDict);
		
		return inference(dataset);
	}
	
	//inference new model ~ getting data from a specified dataset
	public Model inference(LDADataset newData){
		//////System.out.println("init new model");
		Model newModel = new Model();
		
		newModel.initNewModel(option, newData, trainModel);		
		this.newModel = newModel;		
		
		/////System.out.println("Sampling " + niters + " iteration for inference!");	
		System.out.println(niters);
		for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
			//System.out.println("Iteration " + newModel.liter + " ...");
			
			// for all newz_i
			for (int m = 0; m < newModel.M; ++m){
				for (int n = 0; n < newModel.data.docs[m].length; n++){
					// (newz_i = newz[m][n]
					// sample from p(z_i|z_-1,w)
					int topic = infSampling(m, n);
					newModel.z[m].set(n, topic);
				}
			}//end foreach new doc
			
		}// end iterations
		
		//////System.out.println("Gibbs sampling for inference completed!");
		
		computeNewTheta();
		computeNewPhi();
		newModel.liter--;
		return this.newModel;
	}
	

	
	
	/**
	 * do sampling for inference
	 * m: document number
	 * n: word number?
	 */
	protected int infSampling(int m, int n){
		// remove z_i from the count variables
		int topic = newModel.z[m].get(n);
		int _w = newModel.data.docs[m].words[n];
		int w = newModel.data.lid2gid.get(_w);
		newModel.nw[_w][topic] -= 1;
		newModel.nd[m][topic] -= 1;
		newModel.nwsum[topic] -= 1;
		newModel.ndsum[m] -= 1;
		
		double Vbeta = trainModel.V * newModel.beta;
		double Kalpha = trainModel.K * newModel.alpha;
		
		// do multinomial sampling via cummulative method		
		for (int k = 0; k < newModel.K; k++){			
			newModel.p[k] = (trainModel.nw[w][k] + newModel.nw[_w][k] + newModel.beta)/(trainModel.nwsum[k] +  newModel.nwsum[k] + Vbeta) *
					(newModel.nd[m][k] + newModel.alpha)/(newModel.ndsum[m] + Kalpha);
		}
		
		// cummulate multinomial parameters
		for (int k = 1; k < newModel.K; k++){
			newModel.p[k] += newModel.p[k - 1];
		}
		
		// scaled sample because of unnormalized p[]
		double u = Math.random() * newModel.p[newModel.K - 1];
		
		for (topic = 0; topic < newModel.K; topic++){
			if (newModel.p[topic] > u)
				break;
		}
		
		// add newly estimated z_i to count variables
		newModel.nw[_w][topic] += 1;
		newModel.nd[m][topic] += 1;
		newModel.nwsum[topic] += 1;
		newModel.ndsum[m] += 1;
		
		return topic;
	}
	
	protected void computeNewTheta(){
		for (int m = 0; m < newModel.M; m++){
			for (int k = 0; k < newModel.K; k++){
				newModel.theta[m][k] = (newModel.nd[m][k] + newModel.alpha) / (newModel.ndsum[m] + newModel.K * newModel.alpha);
			}//end foreach topic
		}//end foreach new document
	}
	
	protected void computeNewPhi(){
		for (int k = 0; k < newModel.K; k++){
			for (int _w = 0; _w < newModel.V; _w++){
				Integer id = newModel.data.lid2gid.get(_w);
				
				if (id != null){
					newModel.phi[k][_w] = (trainModel.nw[id][k] + newModel.nw[_w][k] + newModel.beta) / (newModel.nwsum[k] + newModel.nwsum[k] + trainModel.V * newModel.beta);
				}
			}//end foreach word
		}// end foreach topic
	}
	
	protected void computeTrnTheta(){
		for (int m = 0; m < trainModel.M; m++){
			for (int k = 0; k < trainModel.K; k++){
				trainModel.theta[m][k] = (trainModel.nd[m][k] + trainModel.alpha) / (trainModel.ndsum[m] + trainModel.K * trainModel.alpha);
			}
		}
	}
	
	protected void computeTrnPhi(){
		for (int k = 0; k < trainModel.K; k++){
			for (int w = 0; w < trainModel.V; w++){
				trainModel.phi[k][w] = (trainModel.nw[w][k] + trainModel.beta) / (trainModel.nwsum[k] + trainModel.V * trainModel.beta);
			}
		}
	}

}


