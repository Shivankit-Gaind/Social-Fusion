import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.*;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

public class streaming {

	public static void main(String[] args)
			throws TwitterException, FileNotFoundException, UnsupportedEncodingException {
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey("");
		cb.setOAuthConsumerSecret("");
		cb.setOAuthAccessToken("");
		cb.setOAuthAccessTokenSecret("");

		final PrintWriter writer = new PrintWriter("/home/abhishek/Twitter.txt", "UTF-8");
		TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
		
		StatusListener listener = new StatusListener() {
			
			public void onStatus(Status status) {
				if (status.getLang().equals("en")) {
					// System.out.println(status.isRetweet() + " - @" +
					// status.getUser().getScreenName() + " - "
					// + status.getText().replaceAll("\n", " "));
					// System.out.print(status.getId() + "\t" +
					// status.getUser().getScreenName() + '\t'
					// + status.getFavoriteCount() + '\t' +
					// status.getRetweetCount() + '\t');
					// if (status.isRetweet()) {
					// System.out.print(1 + "\t");
					// } else if (status.getUserMentionEntities().length > 0) {
					// System.out.print(2 + "\t");
					// } else {
					// System.out.print(0 + "\t");
					// }
					// System.out.println(status.getText().replaceAll("[\n\t]",
					// " "));
					writer.print(status.getId() + "\t" + status.getCreatedAt() + "\t" + status.getUser().getScreenName() + '\t'
							+ status.getFavoriteCount() + '\t' + status.getRetweetCount() + '\t');
					if (status.isRetweet()) {
						writer.print(1 + "\t");
					} else if (status.getUserMentionEntities().length > 0) {
						writer.print(2 + "\t");
					} else {
						writer.print(0 + "\t");
					}
					writer.println(status.getText().replaceAll("[\n\t]", " "));
					System.out.println("recieved");
				}
			}

			
			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
				// System.out.println("Got a status deletion notice id:" +
				// statusDeletionNotice.getStatusId());
			}

			
			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
				// System.out.println("Got track limitation notice:" +
				// numberOfLimitedStatuses);
			}

		
			public void onScrubGeo(long userId, long upToStatusId) {
				// System.out.println("Got scrub_geo event userId:" + userId + "
				// upToStatusId:" + upToStatusId);
			}

			
			public void onStallWarning(StallWarning warning) {
				System.out.println("Got stall warning:" + warning + "---------------------------------------");
			}

			
			public void onException(Exception ex) {
				ex.printStackTrace();
			}
		};
		
		FilterQuery tweetFilterQuery = new FilterQuery();
		
		//double[][] locations = { {-124.7844079, 24.7433195}, {-66.9513812, 49.3457868} };
		
		String[] keywordsArray = { "concert", "music" };
		 
		//tweetFilterQuery.locations(locations);
		tweetFilterQuery.track(keywordsArray);
		tweetFilterQuery.language(new String[] { "en" });
		twitterStream.addListener(listener);
		
		twitterStream.filter(tweetFilterQuery);
		// twitterStream.sample();
		// writer.close();
	}

}
