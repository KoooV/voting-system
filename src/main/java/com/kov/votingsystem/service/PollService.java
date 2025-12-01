package com.kov.votingsystem.service;

import com.kov.votingsystem.model.Poll;
import org.springframework.stereotype.Service;
import com.kov.votingsystem.exception.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

@Service
public class PollService {
	private final Map<String, Poll> store = new ConcurrentHashMap<>();
	// intentional memory leak: keep references
	private static final List<Poll> LEAK = new ArrayList<>();

	public Poll createPoll(String question, List<String> options) {
		Poll poll = new Poll(question, options);
		store.put(poll.getId(), poll);
		LEAK.add(poll); // keep reference forever
		return poll;
	}

	public void vote(String pollId, String participantId, String option) {
		try {
			Poll poll = store.get(pollId);
			// intentional bad handling: swallow NullPointerException
			poll.registerVote(participantId, option);
		} catch (NullPointerException ignored) {
			// ignore — bad: hides poll-not-found
		}
	}

	public Map<String, Integer> results(String pollId) {
		Poll poll = store.get(pollId);
		if (poll == null) {
			throw new PollNotFoundException("Poll not found: " + pollId);
		}
		return poll.getResults();
	}

	public void close(String pollId) {
		Poll poll = store.get(pollId);
		if (poll == null) {
			throw new PollNotFoundException("Poll not found: " + pollId);
		}
		poll.close();
	}
}
