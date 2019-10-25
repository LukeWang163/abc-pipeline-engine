/*
 * Copyright (C) 2007 by
 * 
 * 	Xuan-Hieu Phan
 *	hieuxuan@ecei.tohoku.ac.jp or pxhieu@gmail.com
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 * 
 *  Cam-Tu Nguyen
 *  ncamtu@gmail.com
 *  College of Technology
 *  Vietnam National University, Hanoi
 *
 * JGibbsLDA is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JGibbsLDA is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGibbsLDA; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package base.operators.operator.nlp.lda.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public class LDADataset implements Serializable{
	//---------------------------------------------------------------
	// Instance Variables
	//---------------------------------------------------------------
	
	private static final long serialVersionUID = 433950602299295108L;
	
	public Dictionary localDict;			// local dictionary	
	public Document [] docs; 		// a list of documents	
	public int M; 			 		// number of documents
	public int V;			 		// number of words
	
	// map from local coordinates (id) to global ones 
	// null if the global dictionary is not set
	public Map<Integer, Integer> lid2gid; 
	
	//link to a global dictionary (optional), null for train data, not null for test data
	public Dictionary globalDict;	 		
	
	//--------------------------------------------------------------
	// Constructor
	//--------------------------------------------------------------

	//训练时使用
	public LDADataset(int M){
		localDict = new Dictionary();
		this.M = M;
		this.V = 0;
		docs = new Document[M];	
		
		globalDict = null;
		lid2gid = null;
	}
	
	//预测时使用
	public LDADataset(int M, Dictionary globalDict){
		localDict = new Dictionary();	
		this.M = M;
		this.V = 0;
		docs = new Document[M];	
		
		this.globalDict = globalDict;
		lid2gid = new HashMap<Integer, Integer>();
	}


	//建立LDA数据集，训练时使用
	public static LDADataset buildDataSet(List<String> text) {
		int M = text.size();
		LDADataset data = new LDADataset(M);
		
		int i = 0;
		for(String doc : text) {
			data.setDoc(doc, i);
			i++;
		}
				
		return data;
		
	}
	
	//建立LDA数据集，预测时使用
	public static LDADataset buildDataSet(List<String> text, Dictionary globalDict) {
		int M = text.size();
		LDADataset data = new LDADataset(M, globalDict);
		
		int i = 0;
		for(String doc : text) {
			data.setDoc(doc, i);
			i++;
		}
				
		return data;
		
	}
	
	
	/**
	 * set the document at the index idx if idx is greater than 0 and less than M
	 * @param doc document to be set
	 * @param idx index in the document array
	 */	
	public void setDoc(Document doc, int idx){
		if (0 <= idx && idx < M){
			docs[idx] = doc;
		}
	}

	/**
	 * set the document at the index idx if idx is greater than 0 and less than M
	 * @param str string contains doc
	 * @param idx index in the document array
	 */
	private void setDoc(String str, int idx){
		if (0 <= idx && idx < M){
			String [] words = str.split("[ \\t\\n]");
			
			Vector<Integer> ids = new Vector<Integer>();
			
			for (String word : words){
				
				int _id = localDict.word2id.size();
				
				if (localDict.contains(word))		
					_id = localDict.getID(word);
								
				if (globalDict != null){
					//get the global id					
					Integer id = globalDict.getID(word);
					//System.out.println(id);
					
					if (id != null){
						localDict.addWord(word);
						
						lid2gid.put(_id, id);
						ids.add(_id);
					}
					else { //not in global dictionary
						//do nothing currently
					}
				}
				else {
					localDict.addWord(word);
					ids.add(_id);
				}
			}
			
			Document doc = new Document(ids, str);
			docs[idx] = doc;
			V = localDict.word2id.size();			
		}
	}
	
	
}
