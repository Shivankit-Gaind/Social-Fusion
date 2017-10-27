import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.nio.charset.Charset;

import org.json.*;
import org.apache.commons.math3.special.*;


/**
 * 
 * @author shivankit
 *
 */

public class Event_Detect_and_Track {
	
	//Main function to perform social fusion
	public void Social_Fusion(){
		//Get Instagram Clustered Events
		Path folder = Paths.get("/home/shivankit/Desktop");
		JSONArray C = getInstaEvents(folder);
			
		//This is the query word
		String remove_word = "protest";
				
		//Obtain geo-locations from the file
		HashMap<Pair, ArrayList<String>> geoloc = get_locations_from_file("/home/shivankit/Desktop/geolocations.txt");
		System.out.println("Initial geolocations Map as read from the file: " + geoloc);
				
		//Obtain tweets - a JSON array of tweet JSON Objects from the same folder
		JSONArray tweets = getTweets(folder);
				
				
		//Create a tweets map to be used in later stages
		HashMap<Long,ArrayList<String>> map = new HashMap<Long,ArrayList<String>>();
		
		for(int i=0;i<tweets.length();i++){
			try{
				JSONObject obj = tweets.getJSONObject(i);
				ArrayList<String> list = new ArrayList<String>();
						
				String original_text = obj.getString("text");
				
				//Add modified (pre-processed) String First
				list.add(/**preprocess**/(original_text));				
						
				//Add original text later
				list.add(original_text);
						
				//Make tweet-id as the key
				map.put(obj.getLong("tid"), list);
						
			}catch(Exception e){};			
		}
				
		//Finally process the clusters
		ArrayList<JSONObject> data = process_clusters(C, map, geoloc, remove_word);
				
		//Modified geoloc
		System.out.println("Modified geolocations map: "+geoloc);
				
		//Store the locations
		store_locations_into_file(geoloc);
				
				
		//Final Geo-locations read from file
		System.out.println("Final geolocations read from the file : " + get_locations_from_file("/home/shivankit/Desktop/geolocations.txt"));
				
		try{
		//Converting Arraylist to JSONArray
		JSONArray arr = new JSONArray();
		for(JSONObject obj:data){
			arr.put(obj);
		}
				
		JSONObject result = new JSONObject();
		result.put("Result", arr);
				
		FileWriter file = new FileWriter("/home/shivankit/Desktop/Results.text");
		file.write(result.toString());
		file.close();
				
		}catch(Exception e){System.out.println(e);};
		
	}
	
	/**
	 * 
	 * @param map1
	 * @return
	 */
	
	//This function converts Integer values in the map to the float values 
	public HashMap<String, Float> convert_map(HashMap<String,Integer> map1){
		HashMap<String,Float> map2 = new HashMap<String,Float>();
		for(String str: map1.keySet()){
			map2.put(str, (float)map1.get(str));
		}
		return map2;
	}
	
	/**
	 * 
	 * @param map1
	 * @param map2
	 * @return
	 */
	
	//It returns the cosine similarity score between two vectors 
	public float get_cosine(HashMap<String,Float> map1, HashMap<String,Float> map2){
		HashSet<String> intersection = new HashSet<String>(map1.keySet());
		
		intersection.retainAll(map2.keySet());
		
		float numerator = 0.0f;
		
		for(String x:intersection){
			numerator += map1.get(x)*map2.get(x);
		}
		
		float sum1=0,sum2=0;
		
		for(String x: map1.keySet()){
			sum1 += Math.pow(map1.get(x),2);
		}
		
		for(String x: map2.keySet()){
			sum2 += Math.pow(map2.get(x),2);
		}
		float denominator = (float)(Math.sqrt(sum1)*Math.sqrt(sum2));
		
		if(denominator==0.0f)
			return 0.0f;
		
		return numerator/denominator;
	}
	
	/**
	 * 
	 * @param words
	 * @return
	 */
	
	//It accepts a list of words and returns the count of every word
	public HashMap<String,Integer> Counter(List<String> words){
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		
		for(String str: words){
			if(!map.containsKey(str))
				map.put(str, 1);
			else{
				int count = map.get(str);
				map.put(str, count+1);
			}
		}
		return map;
	}
	
	/**
	 * 
	 * @param text
	 * @return
	 */
	
	
	//It returns a Hash map with every word in the text mapped to its count 
	public HashMap<String,Integer> text_to_vector(String text){
		
		List<String> words = new ArrayList<String>();
		
		//Find out the regex expression
		 Matcher m = Pattern.compile("[\\w'-]+").matcher(text);
		 
		 while(m.find()) 
		   words.add(m.group());
		
		return Counter(words);
	}	
	
	/**
	 * 
	 * @param tag_sentence
	 * @param tweets
	 * @param remove_word
	 * @return
	 */
	
	//It returns a HashSet of tweet ID's which have a score greater than threshold using cosine_similarity metric
	public HashSet<Long> get_cosine_tweets(String tag_sentence, HashMap<Long,ArrayList<String>> tweets, String remove_word){
		HashSet<Long> T = new HashSet<Long>();
		HashMap<String,Integer> vector1,vector2; 
		String tweet;
		
		for(Long tid:tweets.keySet()){
			
			tweet = tweets.get(tid).get(0).toLowerCase();
			
			/*tweet = tweet.encode('ascii','ignore');
			tweet = string.replace(tweet,'\n',' ')
			tag_sentence = tag_sentence.decode('unicode_escape').encode('ascii','ignore'); */
			
			tweet.replace('\n', ' ');
			
			vector1 = text_to_vector(tag_sentence);
			vector2 = text_to_vector(tweet);
			
			vector2.remove(remove_word);
			
			if(vector2.size()>0){
				float score = get_cosine(convert_map(vector1),convert_map(vector2));
				
				if(score>0.10f)
					T.add(tid);
			}
		}
		return T;
	}
	
	/**
	 * 
	 * @param tag_sentence
	 * @param tweets
	 * @param remove_word
	 * @return
	 */
	
	//It returns a HashSet of tweet ID's which have a score greater than threshold using tag_similarity metric
	public static HashSet<Long> get_tag_similarity_tweets(String tag_sentence, HashMap<Long,ArrayList<String>> tweets, String remove_word){
		HashSet<Long> T = new HashSet<Long>();
		
		String tweet;
		
		for(Long tid:tweets.keySet()){
			
			tweet = tweets.get(tid).get(0).toLowerCase();
			
			tweet = tweet.replace('\n', ' ');
			tweet = tweet.replace(" ", "");
						
			String[] tags = tag_sentence.split(" ");
						
			int x = 0;
			for(String tag:tags){						
				if(tweet.contains(tag))
					x++;
			}
			float score = (float)x/(tags.length);
			if(score>0.10f)
				T.add(tid);
			
		}
		return T;
	}
	
	/**
	 * 
	 * @param loc
	 * @return
	 */
	
	//This function uses the Google map API to get location names as per the latitude and the longitude
	public ArrayList<String> get_loc_names(Pair loc) {
			
		float lat = loc.key;
		float lng = loc.value;
		ArrayList<String> loc_names = new ArrayList<String>();
				
		try{
			URL url = new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng="+Float.toString(lat)+','+Float.toString(lng));
			InputStream is = url.openStream();	
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			try{
				JSONObject obj = new JSONObject(jsonText);			
					
				HashSet<String> loc_temp = new HashSet<String>();
				
				JSONArray jsonarray = obj.getJSONArray("results");
				
				JSONObject data = jsonarray.getJSONObject(0);
					
					
				for(int i=0;i<data.getJSONArray("address_components").length();i++){	
					JSONObject it = data.getJSONArray("address_components").getJSONObject(i);
					JSONArray arr = it.getJSONArray("types");
					String types[] = new String[arr.length()];
						
					for(int z = 0;z<types.length;z++){
						types[z] = arr.getString(z);
					}
							
					String list[] = {"route","neighborhood","locality","administrative_area_level_3","administrative_area_level_2","administrative_area_level_1"};
							
					for(String val:list){
						for(String str:types){
							if(str.equals(val)){
								String temp = it.getString("long_name").toLowerCase();
								loc_temp.add(temp);
							}
						}
					}
				}
						
				ArrayList<String> tempSet = new ArrayList<String>();
						
				for(String str: loc_temp){
					String arr[] = str.split(" ");
					for(String str2 : arr){
						tempSet.add(str2);
					}
				}
						
				for(String str:tempSet){
					str = str.toLowerCase();
					if(str.length()>3 && !loc_names.contains(str)){
						loc_names.add(str); //Add encoding
					}
				}
						
				remove_l_names(loc_names);					
		
			}catch(JSONException e){};
			
				      		
		} catch(IOException e){System.out.println("Exception");}; 
				
		return loc_names;
			
	}
	
	public String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	}
	
	
	/**
	 * 
	 * @param loc_names
	 */
	
	//This function removes these strings if present
	public void remove_l_names(ArrayList<String> loc_names){
		String []  rem_l_names = 	{"city",
						           	"street",
						           	"county",
						           	"road",
						           	"avenue",
						           	"district",
						           	"place",
						           	"downtown",
						           	"north",
						           	"east",
						           	"west",
						           	"south",
						           	"hill",
						           	"division",
						           	"northwest",
						           	"executive",
						           	"central",
						           	"grand",
						           	"lower",
						           	"side",
						           	"western",
						           	"eastern",
						           	"southern",
						           	"northern",
						           	"way",
						           	"market",
						           	"mall",
						           	"greater",
						           	"highway"};
		
		for(String rl : rem_l_names){
       		if(loc_names.contains(rl)){
       			loc_names.remove(rl);
       		}
       	}	      		
	}
	
	/**
	 * 
	 * @param time
	 * @return
	 */	
	
	//This function converts the time to a timestamp in the required format
	public String gettimestamp(String time){
		String strg = "";
		String x[] = time.split(" ");
		
		if (x[1].equals("May"))
			strg = "05-"+x[2]+'-'+x[5]+' '+x[3];
		
		else if (x[1].equals("June") || x[1].equals("Jun"))
			strg = "06-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Jan"))
			strg = "01-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Feb"))
			strg = "02-"+x[2]+"-"+x[5]+" "+x[3];			
			
		else if (x[1].equals("Mar"))
			strg = "03-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Apr"))
			strg = "04-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Jul"))
			strg = "07-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Aug"))
			strg = "08-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Sep"))
			strg = "09-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Oct"))
			strg = "10-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Nov"))
			strg = "11-"+x[2]+"-"+x[5]+" "+x[3];
		
		else if (x[1].equals("Dec"))
			strg = "12-"+x[2]+"-"+x[5]+" "+x[3];
		
		return strg;				
	}
	
	/**
	 * 
	 * @param folder
	 * @return
	 */
	
	//This function returns the JSON array of objects for the instagram cluster events
	public JSONArray getInstaEvents(Path folder){
		JSONObject obj = null;
		JSONArray arr = null;
		try{
			Path filepath = Paths.get(folder.toString(),"instagram.json");			
			BufferedReader rd = new BufferedReader(new FileReader(filepath.toString()));
			String jsonText = readAll(rd);
			obj = new JSONObject(jsonText);
			
		}catch(Exception e){System.out.println(e);};
		
		try{
			arr = obj.getJSONArray("data");
		}catch(Exception e){System.out.println(e);};
		
		return arr;
	}
	
	/**
	 * 
	 * @param folder
	 * @return
	 */
	
	//This function returns the json object for the Tweets
	public JSONArray getTweets(Path folder){
		JSONObject obj = null;
		JSONArray arr = null;
		try{
			Path filepath = Paths.get(folder.toString(),"tweets.json");			
			BufferedReader rd = new BufferedReader(new FileReader(filepath.toString()));
			String jsonText = readAll(rd);
			obj = new JSONObject(jsonText);
			
		}catch(Exception e){System.out.println(e);};
		
		try{
			arr = obj.getJSONArray("tweets");
		}catch(Exception e){System.out.println(e);};
		
		return arr;
	}
	
	/**
	 * 
	 * @param tweets
	 * @return
	 */
	
	//This function calculates the centroid for the tweets and then
	//assigns cosine similarity scores to tweets from the centroid
	public HashMap<String,Float> findCentroid(ArrayList<String> tweets){
		
		HashMap<String,Float> scores = new HashMap<String,Float>();
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		
		for(String t:tweets){
			List<String> words = new ArrayList<String>();
			
			//Find out the regex expression
			Matcher m = Pattern.compile("[\\w'-]+").matcher(t);
			 
			while (m.find()) 
			  words.add(m.group());					 
						
					
			for(String str: words){
				if(!map.containsKey(str))
					map.put(str, 1);
				else{
					int count = map.get(str);
					map.put(str, count+1);
				}
			}
			
		}
		
		HashMap<String,Float> map2 = new HashMap<String,Float>();
		
		int N = tweets.size();
		for(String str: map.keySet()){
			map2.put(str, (float)map.get(str)/N);
		}
		
		for(String str:tweets){
			
			HashMap<String,Integer> vec = text_to_vector(str);
								
			float cosine_sim = get_cosine(map2, convert_map(vec));
			
			scores.put(str, cosine_sim);
		}
		
		return scores;
	}
	
	
	/**
	 * 
	 * @param X
	 * @param W
	 * @return
	 */
	
	//This module performs the job of parameter estimation and return back two parameters p1 and p2
	public ArrayList<Float> para_est(ArrayList<Float> X, float[] W){
		float p1=0,p2=0;
		
		
		float sum = 0;
		for(float z:W)
			sum += z;
		
		float u = 0.0f,v=0.0f;
		for(int i=0;i<X.size();i++){
			u += X.get(i)*W[i];
			v += ((float)Math.pow(X.get(i),2))*W[i]; 
		}
		u/=sum; 
		v/=sum;
		
		
		if (Math.abs(u-v)<0.01f){
			p1=1;
			p2=1;
		}
		else{
			p1=u*(float)(u-v)/(float)(v-Math.pow(u,2));
			p2=(1-u)*(float)(u-v)/(float)(v-Math.pow(u,2));
		}
			
		if(p1<0)
			p1=1;
		if(p2<0)
			p2=1;
		
		ArrayList<Float> result = new ArrayList<Float>();
		result.add(p1);
		result.add(p2);
		
		return result;
	}
	
	/***
	 * 
	 * @param Z
	 * @param theta_Co
	 * @param theta_L
	 * @param N
	 * @param Coherence
	 * @param L
	 * @return
	 */
	
		
	//This is the main module performing the EM step
	public float perform_EM(float[] Z, float[][] theta_Co,float [][] theta_L, int N, ArrayList<Float> Coherence, int[][] L){
		float d = 0.7f;
		
		int ml = L.length;
		int nl = L[0].length;
		
		for(int i=0;i<ml;i++){
			theta_L[i] = new float[2];
			theta_L[i][0] = 0.3f;
			theta_L[i][1] = 0.6f;
		}
		
		
		int[] R = new int[N];
		int itr = 1;
		float[] Z_old = new float[N];
		Arrays.fill(Z_old, 1);
		
		while(true){
			float sum = 0.0f;
			for(int i=0;i<N;i++){
				sum+= (float)Math.abs((Z[i]-Z_old[i]));
			}
			
			if(sum>0.01 && itr<50){
				Z_old = Z.clone();
				for(int i=0;i<N;i++){
					float co = Coherence.get(i);
					float A = d*(float)Beta.regularizedBeta(co,theta_Co[1][0],theta_Co[1][1]);
					float B = (1-d)*(float)Beta.regularizedBeta(co,theta_Co[0][0],theta_Co[0][1]);
					
					if (Float.isInfinite(B))
						Z[i]=0;
					
					Z[i] = (float)A/(float)(A+B);
					
					if(Float.isNaN(Z[i]))
						Z[i]=0;
					
					if(Z[i]>1)
						System.out.println(Z[i]);
				}
				
				sum = 0.0f;
				for(float x:Z){
					sum+=x;
				}
				d = sum/N;
				
				ArrayList<Float> list = para_est(Coherence,Z);
				theta_Co[1][0] = list.get(0); 
				theta_Co[1][1] = list.get(1);
				
				
				float[] Z_cpy = Z.clone();
				for(int i=0;i<N;i++)
					Z_cpy[i] = 1-Z[i];
				list = para_est(Coherence, Z_cpy);
				theta_Co[0][0] = list.get(0);
				theta_Co[0][1] = list.get(1);
				
				itr++;
			}
			else
				break;
		}		
		
		for(int i=0;i<N;i++){
			if(Z[i]>0.5)
				R[i] = 1;
			else
				R[i] = 0;
		}
		
		return d;
	}
	
	/**
	 * 
	 * @param loc_names
	 * @param tweet
	 * @return
	 */
	
	//This module checks whether the tweet is geotagged or not
	public int isLocPresent(ArrayList<String> loc_names, String tweet){
		String arr[] = tweet.split(" ");
		for(String location:loc_names){
			for(String str:arr)
				if(str.equals(location))
					return 1;
		}
		return 0;
	}
	
	/**
	 * 
	 * @param C
	 * @param tweets
	 * @param geoloc
	 * @param remove_word
	 */
	
	//This module performs the job of processing clusters
	public ArrayList<JSONObject> process_clusters(JSONArray C, HashMap<Long,ArrayList<String>> tweets, HashMap<Pair, ArrayList<String>> geoloc, String remove_word){
				
		ArrayList<JSONObject> data = new ArrayList<JSONObject>();
		try{
			for(int q=0;q<C.length();q++){
				JSONObject c = C.getJSONObject(q);
				ArrayList<String> tags = new ArrayList<String>();
					
				JSONArray arr = c.getJSONArray("tags");
				String[] str_arr = new String[arr.length()];
						
				for(int k=0;k<arr.length();k++)
					str_arr[k] = arr.getString(k);
						
				for(String str:str_arr){
					tags.add(str);
				}
					
				if(tags.contains(remove_word))
					tags.remove(remove_word);
					
				if(tags.size()>0){
					String tag_sentence = "";
					
					for(String str:tags){
						tag_sentence+=str;
						tag_sentence+=" ";
					}
					
					tag_sentence = tag_sentence.trim();
					
					Pair key = new Pair(Float.parseFloat(c.getString("lat")),Float.parseFloat(c.getString("long")));
					
					ArrayList<String> loc_names;
					
					if(geoloc.containsKey(key)){
						loc_names = geoloc.get(key);
					}
					else{
						loc_names = get_loc_names(key);
						geoloc.put(key,loc_names);
					}
					
					HashSet<Long> T;
					T = get_cosine_tweets(tag_sentence, tweets, remove_word);
					//T = get_tag_similarity_tweets(tag_sentence, tweets, remove_word);
					
					ArrayList<Long> T_list = new ArrayList<Long>(T);
					
					if(T_list.size()>0){
						ArrayList<Node> rtweets = new ArrayList<Node>();
						for(long tid:T_list){
							rtweets.add(new Node(tweets.get(tid).get(0), tweets.get(tid).get(1), tid));
						}
						
						ArrayList<Node> ftweets = new ArrayList<Node>();
						for(Node node:rtweets){
							if(isLocPresent(loc_names, node.modified_txt)==1){
								ftweets.add(node);
							}
						}
						
						if(ftweets.size()>1){
							ArrayList<String> ftexts = new ArrayList<String>();
					
							for(Node node:ftweets){
								ftexts.add(node.modified_txt);
							}
							
							int[][] L = new int[loc_names.size()][ftweets.size()];
							for(int i=0;i<loc_names.size();i++){
								L[i] = new int[ftweets.size()];
								
								for(int j=0;j<ftweets.size();j++){
									if(ftexts.contains(loc_names.get(i))){
										L[i][j] = 1;
									}
								}
							}
							
							int N = ftweets.size();
							ArrayList<Float> Coherence = new ArrayList<Float>();
							
							HashMap<String, Float> scores = findCentroid(ftexts);
							
							for(String str:ftexts){
								Coherence.add(scores.get(str));
							}
							
									
							float[][] theta_Co = {{1.0f,2.0f},{2.0f,1.0f}};
							float[] Z = new float[N];
							int ml = L.length;
							float [][] theta_L = new float[ml][2];
							float fracd = perform_EM(Z, theta_Co, theta_L, N, Coherence, L);
									
							ArrayList<Node> sel_tweets = new ArrayList<Node>();
							for(int j=0;j<ftweets.size();j++){
								if(Z[j]>0.5)
									sel_tweets.add(ftweets.get(j));
							}
								
							if(sel_tweets.size()>0){
								try{
									JSONArray array = new JSONArray();
									for(Node n:sel_tweets){
										JSONObject obj = new JSONObject();
										obj.put("tid", n.id);
										obj.put("Original", n.orig_text);
										obj.put("Modified", n.modified_txt);
												
										array.put((JSONObject)obj);
									}
									c.append("tweets",array);								
									data.add(c);
								}catch(Exception e){};
							}						
						}					
					}
				}
			}
		}catch(Exception e){};
		return data;
	}
	
	/**
	 * 
	 * @param geoloc
	 */
	
	//This function stores the geo-locations object in a file to be used later.
	public void store_locations_into_file(HashMap<Pair, ArrayList<String>> geoloc){
		try{
			FileOutputStream fos = new FileOutputStream("/home/shivankit/Desktop/geolocations.txt");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(geoloc);
			oos.close();			
		}catch(Exception e){};
	} 
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
			
	//This function reads the geo-locations object from a file.
	public HashMap<Pair, ArrayList<String>> get_locations_from_file(String filename){
		HashMap<Pair, ArrayList<String>> geoloc = null;
		try{
			File f = new File(filename);
			if(!f.exists())
				return new HashMap<Pair, ArrayList<String>>();
			FileInputStream fis = new FileInputStream(filename);
			
			ObjectInputStream ois = new ObjectInputStream(fis);
			geoloc = (HashMap<Pair, ArrayList<String>>)ois.readObject();
			ois.close();			
		}catch(Exception e){};
		return geoloc;
	} 
			
}


class Node implements Serializable{
	String modified_txt;
	String orig_text;
	Long id;
	public Node(String modified_txt, String orig_txt, Long id){
		this.orig_text = orig_txt;
		this.modified_txt = modified_txt;
		this.id = id;
	}
}

class Pair implements Serializable{
	float key;
	float value;
	public Pair(float key, float value){
		this.key = key;
		this.value = value;
	}
	
	@Override
	public int hashCode(){
		return (int)(key+5*value);
	}
	
	@Override
	public boolean equals(Object obj){
		Pair p = (Pair)obj;
		return (this.key==p.key && this.value==p.value);
	}
}


 