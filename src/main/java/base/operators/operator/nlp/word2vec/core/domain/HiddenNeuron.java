package base.operators.operator.nlp.word2vec.core.domain;

import java.io.Serializable;

public class HiddenNeuron extends Neuron implements Serializable {
	private static final long serialVersionUID = 1L;

	public double[] syn1; // hidden->out

	public HiddenNeuron(int layerSize) {
		syn1 = new double[layerSize];
	}

}
