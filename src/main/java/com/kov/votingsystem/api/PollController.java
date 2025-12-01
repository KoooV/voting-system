package com.kov.votingsystem.api;

import com.kov.votingsystem.exception.DuplicateVoteException;
import com.kov.votingsystem.exception.PollClosedException;
import com.kov.votingsystem.exception.UnknownOptionException;
import com.kov.votingsystem.exception.PollNotFoundException;
import com.kov.votingsystem.service.PollService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/polls")
public class PollController {
	private final PollService pollService;

	public PollController(PollService pollService) {
		this.pollService = pollService;
	}

	public record CreatePollRequest(String question, List<String> options) {}
	public record CreatePollResponse(String id, String question, List<String> options) {}
	public record VoteRequest(String participantId, String option) {}

	@PostMapping
	public CreatePollResponse create(@RequestBody CreatePollRequest request) {
		var poll = pollService.createPoll(request.question(), request.options());
		return new CreatePollResponse(poll.getId(), poll.getQuestion(), request.options());
	}

	@PostMapping("/{id}/votes")
	public ResponseEntity<Void> vote(@PathVariable("id") String id, @RequestBody VoteRequest vote) {
		pollService.vote(id, vote.participantId(), vote.option());
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/{id}/results")
	public Map<String, Integer> results(@PathVariable("id") String id) {
		return pollService.results(id);
	}

	@PostMapping("/{id}/close")
	public ResponseEntity<Void> close(@PathVariable("id") String id) {
		pollService.close(id);
		return ResponseEntity.accepted().build();
	}

	@ExceptionHandler(PollNotFoundException.class)
	public ResponseEntity<String> handleNotFound(PollNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler({DuplicateVoteException.class, UnknownOptionException.class})
	public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}

	@ExceptionHandler(PollClosedException.class)
	public ResponseEntity<String> handleConflict(PollClosedException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
	}
