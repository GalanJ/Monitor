package com.galanjulio.monitor.threads;

import com.galanjulio.monitor.Main;
import com.galanjulio.monitor.Monitor;
import com.galanjulio.monitor.settings.ReplySettings;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.util.List;

public class ReplyThread extends Thread {

    private static final int TIMEOUT_RETRY_SECONDS = 15;

    private Twitter twitter;
    private String handle;
    private String reply;

    private Status currentTweet;

    public ReplyThread(Monitor main, ReplySettings settings) {
        this.twitter = main.getTwitter();
        this.handle = settings.getHandle();
        this.reply = settings.getReply();

        if (handle.charAt(0) != '@') {
            handle = "@" + handle;
        }

        setName("Monitor - " + handle + " Reply Thread");

        Main.log("Successfully set up a new reply thread for " + handle);
    }

    @Override
    public void run() {
        while (true) {
            List<Status> tweets = null;

            try {
                tweets = twitter.getUserTimeline(handle);
            } catch (TwitterException e) {
                Main.log("Could not retrieve " + handle + "'s timeline: " + e.getErrorMessage());
                Main.log("Pausing for " + TIMEOUT_RETRY_SECONDS + " seconds then retrying.");
                System.out.println(" ");

                try {
                    sleep(TIMEOUT_RETRY_SECONDS * 1000);
                } catch (InterruptedException ignored) {
                }
            }

           if (tweets != null) {
               int index = 0;

               // We don't want to reply to retweets
               while (tweets.get(index).isRetweet()) {
                   index++;
               }

               Status tweet = tweets.get(index);

               if (currentTweet == null) {
                   currentTweet = tweet;

                   Main.log("Logged latest tweet: \"" + tweet.getText() + "\"");
                   Main.log("Link: https://twitter.com/" + handle.substring(1) + "/status/" + tweet.getId());
                   System.out.println(" ");
               } else {
                   if (currentTweet.getId() != tweet.getId()) {
                       replyToTweet(tweet);
                   }
               }

               try {
                   sleep(1000);
               } catch (InterruptedException ignored) {
               }
           }
        }
    }

    private void replyToTweet(Status tweet) {
        StatusUpdate statusUpdate = new StatusUpdate(handle + " " + reply);
        statusUpdate.setInReplyToStatusId(tweet.getId());

        try {
            twitter.updateStatus(statusUpdate);

            Main.log("Reply sent to: " + handle);
            Main.log("Reply: " + reply);
            Main.log("Tweet: \"" + tweet.getText() + "\"");
            Main.log("Link: https://twitter.com/" + handle.substring(1) + "/status/" + tweet.getId());
            System.out.println(" ");
        } catch (TwitterException e) {
            Main.log("Could not send tweet: " + e.getErrorMessage());
            System.out.println(" ");
            e.printStackTrace();
            return;
        }

        currentTweet = tweet;
    }
}
