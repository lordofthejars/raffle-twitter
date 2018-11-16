package org.lordofthejars.raffletwitter.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;

@Path("/")
public class RaffleEndpoint {

	private static List<User> users = new ArrayList<>();
	private static Map<User, String> messages = new ConcurrentHashMap<>();

	private Twitter twitter = new TwitterFactory().getInstance();
	private Set<Long> friends = new TreeSet<>();
	private boolean areFriendsLoaded = false;

	@Path("/init")
	@GET
	@Produces("text/plain")
	public Response initStreaming(@QueryParam("hash") String hash) {

		TwitterStreamFactory.getSingleton().onStatus(e -> {
			users.add(e.getUser());
			messages.put(e.getUser(), e.getText());
		}).onException(e -> e.printStackTrace()).filter("@alexsotob", hash);

		return Response.ok("Started listening stream from alexsotob").build();
	}

	@GET
	@Produces("application/json")
	public Response currentResults() {
		return Response.ok(toJson()).build();
	}

	@Path("/raffle")
	@GET
	@Produces("text/plain")
	public Response getWinners() {

		try {
			loadFriends();
		} catch (TwitterException e) {
			e.printStackTrace();
			return Response.serverError().build();
		}

		boolean found = false;

		Random r = new Random();
		while (!found) {
			int userIndex = r.nextInt(users.size());

			User u = users.get(userIndex);
			
			if (isFriend(u)) {
				users.remove(u);
				return Response.ok(u.getScreenName()).build();
			} else {
				users.remove(u);
			}

		}

		return Response.noContent().build();
	}

	private boolean isFriend(User u) {
		return users.contains(u);
	}

	private void loadFriends() throws TwitterException {

		if (!areFriendsLoaded) {
			long cursor = -1;
			IDs ids;
			do {

				ids = twitter.getFriendsIDs(cursor);
				for (long id : ids.getIDs()) {
					friends.add(id);
				}
			} while ((cursor = ids.getNextCursor()) != 0);
			areFriendsLoaded = true;
		}
	}

	private JsonArray toJson() {
		JsonArrayBuilder content = Json.createArrayBuilder();

		for (Map.Entry<User, String> entry : messages.entrySet()) {
			content.add(Json.createObjectBuilder().add("user", "@" + entry.getKey().getScreenName()).add("message",
					entry.getValue()));
		}

		return content.build();
	}
}
