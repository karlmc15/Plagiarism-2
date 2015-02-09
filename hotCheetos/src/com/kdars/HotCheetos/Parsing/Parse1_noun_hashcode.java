package com.kdars.HotCheetos.Parsing;

import java.util.regex.Pattern;

import com.kdars.HotCheetos.Config.Configuration;
import com.kdars.HotCheetos.DB.DBManager;
import com.kdars.HotCheetos.DocumentStructure.DocumentInfo;

public class Parse1_noun_hashcode extends Parse1{
	
	private String postFix1 = Configuration.getInstance().getPostFix1();
	private String postFix2 = Configuration.getInstance().getPostFix2();
	
	@Override
	boolean parseDoc(String content, int documentID, int invertedIndexTableID) {

		DocumentInfo docInfo = new DocumentInfo();
		docInfo.docID = documentID;

		String wordList[] = content.trim().split("\\s+");

		for (int i = 0; i < wordList.length; i++) {
			int postFixChecker = wordList[i].length();
			String word = deletePostFix(wordList[i]);
			if (word.length() != postFixChecker) {
				docInfo = addHash(docInfo, word.hashCode());
			}
		}
		
		return DBManager.getInstance().insertBulkToHashTable(docInfo, invertedIndexTableID);
	
	}
	
	private DocumentInfo addHash(DocumentInfo docInfo, int hash) {
		if (hash % Configuration.getInstance().getFingerprintSetting() != 0) {
			return docInfo;
		}
		String hashToString = String.valueOf(hash);
		if (docInfo.termFreq.containsKey(hashToString)) {
			int value = docInfo.termFreq.get(hashToString);
			docInfo.termFreq.replace(hashToString, value + 1);
			return docInfo;
		}
		docInfo.termFreq.put(hashToString, 1);
		return docInfo;
	}
	
	private String deletePostFix(String processedString) {

		if (processedString.length() < 3) {
			return processedString;
		}

		String checkStr = processedString
				.substring(processedString.length() - 2);
		if (Pattern.compile(postFix2).matcher(checkStr).find()) {
			processedString = processedString.substring(0,
					processedString.length() - 2);
			return processedString;
		}

		checkStr = processedString.substring(processedString.length() - 1);
		if (Pattern.compile(postFix1).matcher(checkStr).find()) {
			processedString = processedString.substring(0,
					processedString.length() - 1);
			return processedString;
		}

		return processedString;
	}
	
}
