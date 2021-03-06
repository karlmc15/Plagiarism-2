package com.kdars.HotCheetos.MapReduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.mapreduce.Mapper;

import com.kdars.HotCheetos.Config.Configurations;
import com.kdars.HotCheetos.DB.DBManager;
import com.kdars.HotCheetos.DocumentStructure.DocumentInfo;

public class SimScoreMapper2  extends Mapper<LongWritable, MapWritable, IntWritable, MapWritable>{
	
	@Override
	public void map(LongWritable docID, MapWritable termFreqMap, Context context) throws IOException, InterruptedException {
		int docInfoMemoryLimit = Configurations.getInstance().getDocInfoListLimit();
		int tableID = Configurations.getInstance().getTableID();
		
		DBManager.getInstance().deleteDuplicateScores((int) docID.get(), tableID);  //도중에 죽었다면 flag가 1로 되어있을 것이므로, flag가 1이면서 inputdocument id가 같은 row들을 db에서 지우고 다시 시작.
		
		ArrayList<Integer> corpusDocIDList = DBManager.getInstance().flagInputAndGetCurrentDocIDsFromInvertedIndexTable((int) docID.get(), tableID);
		
		while(corpusDocIDList.isEmpty()){  //만약에 input documents와 비교할 corpus document가 DB에 없다면 while문을 타지 않음.

			if(corpusDocIDList.size() <= docInfoMemoryLimit){
				ArrayList<DocumentInfo> corpusDocInfoList = DBManager.getInstance().getMultipleDocInfoArray(corpusDocIDList, tableID);
				if (!simScore_Calculation_OneVSInputCorpus(docID, termFreqMap, corpusDocInfoList, tableID, tableID)){
					System.out.println("simScore_Calculation_OneVSCorpus FAILLLLLLLLLLLLLLLLLLLLLLLLLLLLL");
				}
				corpusDocIDList.clear();
				continue;
			}
			
			ArrayList<Integer> segmentedDocIDList = new ArrayList<Integer>(corpusDocIDList.subList(0, docInfoMemoryLimit - 1));
			ArrayList<DocumentInfo> corpusDocInfoList = DBManager.getInstance().getMultipleDocInfoArray(segmentedDocIDList, tableID);
			if (!simScore_Calculation_OneVSInputCorpus(docID, termFreqMap, corpusDocInfoList, tableID, tableID)){
				System.out.println("simScore_Calculation_OneVSCorpus FAILLLLLLLLLLLLLLLLLLLLLLLLLLLLL");
			}
			corpusDocIDList = new ArrayList<Integer>(corpusDocIDList.subList(docInfoMemoryLimit, corpusDocIDList.size() - 1));
		
		}
		
		DBManager.getInstance().unflag_Score((int) docID.get(), tableID);  //score 계산 및 저장이 정상적으로 완료되었다면 flag를 0으로 다시 set해주기.
		
		return;
	}
	
	private boolean simScore_Calculation_OneVSInputCorpus(LongWritable docID, MapWritable termFreqMap, ArrayList<DocumentInfo> corpusDocInfoList, int scoreTableID, int invertedIndexTableID){
		StringBuilder csvContent = new StringBuilder();
		int bulkInsertLimit = Configurations.getInstance().getbulkScoreLimit();
		int bulkInsertLimitChecker = 0;

		int docid1 = (int) docID.get();
		for (DocumentInfo docInfo2 : corpusDocInfoList){
			int docid2 = docInfo2.docID;
			double simscore = calcSim(termFreqMap, docInfo2.termFreq);
			csvContent.append("1," + String.valueOf(docid1)+","+String.valueOf(docid2)+","+String.valueOf(simscore)+"\n");
			
			bulkInsertLimitChecker++;
			if (bulkInsertLimitChecker == bulkInsertLimit){
				if(!DBManager.getInstance().insertBulkToScoreTable(csvContent.toString(), scoreTableID)){
					return false;
				}
				bulkInsertLimitChecker = 0;
				csvContent = new StringBuilder();
			}
		}
		
		
		return DBManager.getInstance().insertBulkToScoreTable(csvContent.toString(), scoreTableID);
	}
	
	private double calcSim(MapWritable termFreqMap, HashMap<String, Integer> doc2){
		double multiply = 0.0d;
		double norm1 = 0.0d;
		double norm2 = 0.0d;
		
		termFreqMap = new MapWritable(termFreqMap);
		doc2 = new HashMap<String, Integer>(doc2);
		Iterator iter1 = termFreqMap.entrySet().iterator();
		while(iter1.hasNext()){
			Map.Entry pair1 = (Map.Entry)iter1.next();
			String key = pair1.getKey().toString();
			double value1 = Double.valueOf(pair1.getValue().toString());
			
			if(doc2.containsKey(key)){
				double value2 = (double)doc2.get(key);
				multiply += (value1 * value2);
				norm2 += (value2 * value2);
				doc2.remove(key);
				
			}
			norm1 += (value1 * value1);
			iter1.remove();
		}
		
		Iterator iter2 = doc2.entrySet().iterator();
		while(iter2.hasNext()){
			Map.Entry pair2 = (Map.Entry)iter2.next();
			double value2 = Double.valueOf(pair2.getValue().toString());
			norm2 += (value2 * value2);
			iter2.remove();
		}
		
		double result =  multiply / Math.sqrt(norm1 * norm2);
		if(Double.isNaN(result)){
			result=0;
		}
		
		return result;
	}
	
}
