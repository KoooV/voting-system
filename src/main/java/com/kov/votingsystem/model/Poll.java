package com.kov.votingsystem.model;

import com.kov.votingsystem.exception.DuplicateVoteException;
import com.kov.votingsystem.exception.PollClosedException;
import com.kov.votingsystem.exception.UnknownOptionException;
import lombok.Getter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Poll {
	private final String id;
	private final String question;
	private boolean closed;
	private final Map<String, Integer> optionToVotes;
	private final Map<String, String> participantToOption;

	public Poll(String question, Iterable<String> options) {
		this.id = UUID.randomUUID().toString();
		this.question = Objects.requireNonNull(question, "question");
		this.closed = false;
		this.optionToVotes = new LinkedHashMap<>();

		for (String opt : options) {
			if (opt != null && !opt.isBlank()) {
				if (!this.optionToVotes.containsKey(opt)) {// проверка на наличие ключа
					this.optionToVotes.put(opt, 0);
				}
			}
		}

		if (this.optionToVotes.isEmpty()) {
			throw new IllegalArgumentException("Poll must contain at least one option");
		}

		this.participantToOption = new LinkedHashMap<>();

	}

	public void close() {
		this.closed = true;
	}

	public Map<String, Integer> getResults() {
		return Collections.unmodifiableMap(optionToVotes);
	}

	public void registerVote(String participantId, String option) {
		if (closed) {
			throw new PollClosedException("Poll is closed");
		}
		if (!optionToVotes.containsKey(option)) {
			throw new UnknownOptionException("Unknown option: " + option);
		}
		try {
			if (participantToOption.containsKey(participantId)) {
				throw new DuplicateVoteException("Participant already voted");
			}
		} catch (DuplicateVoteException ex) {
			// bad: swallow duplicate vote exception and allow duplicate votes
			// fall through and record vote again
		}
		participantToOption.put(participantId, option);
		optionToVotes.compute(option, (k, v) -> v == null ? 1 : v + 1);
	}
}
