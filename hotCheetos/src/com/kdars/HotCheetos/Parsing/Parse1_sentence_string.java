package com.kdars.HotCheetos.Parsing;

import com.kdars.HotCheetos.Config.Configurations;
import com.kdars.HotCheetos.DB.DBManager;
import com.kdars.HotCheetos.DocumentStructure.DocumentInfo;
import com.kdars.HotCheetos.DocumentStructure.SentenceInfo;

public class Parse1_sentence_string extends Parse1{
	
	/*Temporary measure for experiment.  Need to delete!!!! */
	private int fingerprintSetting = Configurations.getInstance().getFingerprintSetting();
	/*Temporary measure for experiment.  Need to delete!!!! */
	
	@Override
	boolean parseDoc(String content, int documentID, int invertedIndexTableID) {
		DocumentInfo docInfo = new DocumentInfo();
		docInfo.docID = documentID;
		
		String sentenceList[] = content.trim().split("\\.");
		
		int sentenceID = 0;
		for(String sentence : sentenceList){
			if(sentence.length() > 5){
				sentenceID++;
				SentenceInfo senInfo = new SentenceInfo();
				senInfo = parseSentence(sentence, sentenceID);
				docInfo.sentenceInfoMap.put(sentenceID, senInfo);
			}
		}
		
		
		return DBManager.getInstance().insertBulkToSentenceTable(docInfo, invertedIndexTableID);
	}
	
	private SentenceInfo parseSentence(String sentence, int sentenceID){
		SentenceInfo senInfo = new SentenceInfo();
		senInfo.sentenceID = sentenceID;
		
		String wordList[] = sentence.trim().split("\\s+");
		
//		senInfo.sentenceSize = wordList.length;
		
		for(String word : wordList){
			if(word.length() > 2){
				addHashToSenInfo(senInfo, word);
			}
		}
		
		return senInfo;
	}
	
	private void addHashToSenInfo(SentenceInfo senInfo, String word){
		if (word.hashCode() % this.fingerprintSetting != 0) {
			return;
		}
		
		if (senInfo.termFreq.containsKey(word)) {
			int value = senInfo.termFreq.get(word);
			senInfo.termFreq.put(word, value + 1);
			return;
		}
		senInfo.termFreq.put(word, 1);
	}
	
}
