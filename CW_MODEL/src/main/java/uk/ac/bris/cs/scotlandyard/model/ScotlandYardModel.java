package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import uk.ac.bris.cs.gamekit.graph.Graph;

// NOTE rounds is a list of booleans;
// If the boolean is true then reveal mrX's current location in that round;
// otherwise reveal last known location.

// TODO ModelGameOverTest, ModelRoundTest, ModelSpectatorTest

public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	private List<Boolean> rounds;
	private int currentRound;
	private Graph<Integer, Transport> graph;
	private List<PlayerConfiguration> playerConfigurations;
	private List<ScotlandYardPlayer> players;
	private List<Spectator> spectators;
	private ScotlandYardPlayer mrXPlayer;
	private int mrXLastLocation;
	private ScotlandYardPlayer currentPlayer;
	private boolean doubleMoveMade;


	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph, PlayerConfiguration mrX, PlayerConfiguration firstDetective, PlayerConfiguration... restOfTheDetectives) {
			playerConfigurations = new ArrayList<PlayerConfiguration>();
			players = new ArrayList<ScotlandYardPlayer>();
			spectators = new ArrayList<Spectator>();
			currentRound = NOT_STARTED;
			mrXLastLocation = 0;
			doubleMoveMade = false;

			checkArguments(rounds, graph, mrX);

			playerConfigurations.add(mrX);
			playerConfigurations.add(firstDetective);

			for(PlayerConfiguration detective : restOfTheDetectives){
				playerConfigurations.add(requireNonNull(detective));
			}

      coloursAccept();
			locationsAccept();
			ticketsAccept();
			createPlayers();

			currentPlayer = players.get(0);

	}

	// Check arguments are valid
	private void checkArguments(List<Boolean> rounds, Graph<Integer, Transport> graph, PlayerConfiguration mrX){
		// Check rounds exist
		this.rounds = requireNonNull(rounds);
		if (rounds.isEmpty()) throw new IllegalArgumentException("empty round");

		// Check graph exists
		this.graph = requireNonNull(graph);
		if (graph.isEmpty()) throw new IllegalArgumentException("empty graph");

		// If mrX is not black
		if (mrX.colour.isDetective()) throw new IllegalArgumentException("MrX colour problem");
	}

	// Check each player has a unique colour
	private void coloursAccept(){
		Set<Colour> set = new HashSet<>();
		for (PlayerConfiguration configuration : playerConfigurations) {
			if (set.contains(configuration.colour)) throw new IllegalArgumentException("Duplicate Colour"); // If colour has already been seen
			set.add(configuration.colour);
	}}

	// Check each player is in a unique location
	private void locationsAccept(){
		Set<Integer> set = new HashSet<>();
		for (PlayerConfiguration configuration : playerConfigurations) {
			if (set.contains(configuration.location)) throw new IllegalArgumentException("Duplicate location"); // If location has already been seen
			set.add(configuration.location);
	}}

	// Check each player has a valid set of tickets
	private void ticketsAccept() {
		for (PlayerConfiguration player : playerConfigurations) {
			if (player.colour.isDetective()) acceptDetectiveTickets(player);
			else acceptMrXTickets(player);
	}}

	// Check a detective has a valid set of tickets
	private void acceptDetectiveTickets(PlayerConfiguration detective) {
		if (detective.tickets.containsKey(Ticket.SECRET) && detective.tickets.get(Ticket.SECRET) != 0) throw new IllegalArgumentException("Illegal Ticket");
		if (detective.tickets.containsKey(Ticket.DOUBLE) && detective.tickets.get(Ticket.DOUBLE) != 0) throw new IllegalArgumentException("Illegal Ticket");
		if (!detective.tickets.containsKey(Ticket.TAXI)) throw new IllegalArgumentException("Missing Ticket");
		if (!detective.tickets.containsKey(Ticket.BUS)) throw new IllegalArgumentException("Missing Ticket");
		if (!detective.tickets.containsKey(Ticket.UNDERGROUND)) throw new IllegalArgumentException("Missing Ticket");
	}

	// Check MrX has a valid set of tickets
	private void acceptMrXTickets(PlayerConfiguration mrX) {
		if (!mrX.tickets.containsKey(Ticket.SECRET)) throw new IllegalArgumentException("Missing Ticket");
		if (!mrX.tickets.containsKey(Ticket.DOUBLE)) throw new IllegalArgumentException("Missing Ticket");
		if (!mrX.tickets.containsKey(Ticket.TAXI)) throw new IllegalArgumentException("Missing Ticket");
		if (!mrX.tickets.containsKey(Ticket.BUS)) throw new IllegalArgumentException("Missing Ticket");
		if (!mrX.tickets.containsKey(Ticket.UNDERGROUND)) throw new IllegalArgumentException("Missing Ticket");
	}

	// Creates ScotlandYardPlayers from playerConfigurations which were passed during instantiation
  private void createPlayers(){
		for(PlayerConfiguration configuration : playerConfigurations){
			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets);
			players.add(player);
			if (player.isMrX()) mrXPlayer = player;
	}}

	@Override
	public void registerSpectator(Spectator spectator) {
		if(spectator == null) throw new NullPointerException();
		if(spectators.contains(spectator)) throw new IllegalArgumentException();
		spectators.add(spectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		List<Spectator> equalSpectators = new ArrayList<>();
		if(spectator == null) throw new NullPointerException();

		for(Spectator spectator1 : spectators){
			if (spectator1 == spectator) equalSpectators.add(spectator1);
		}
		if(equalSpectators.isEmpty()) throw new IllegalArgumentException();
		else spectators.remove(spectator);

	}

	@Override
	public void startRotate() {
    if(isGameOver()) throw new IllegalStateException();

		Set<Move> moves = new HashSet<>();
		moves = getMrXValidMoves(mrXPlayer);

		mrXPlayer.player().makeMove(this, mrXPlayer.location(), moves, this);
	}

	@Override
	public void accept(Move move){
		// Validate Move
		if (move == null) throw new NullPointerException("Null Move");
		ScotlandYardPlayer player = colourToPlayer(move.colour());
		Set<Move> validMoves = new HashSet<>();

		// Choose next player
		chooseNextPlayer(player);

		// Get all possible moves for current player
		if (player.isDetective()) validMoves = getDetectiveValidMoves(player);
		else validMoves = getMrXValidMoves(player);

		// If given move is invalid
		if (!validMoves.contains(move)) throw new IllegalArgumentException("Illegal Move");

		// If given move is a TicketMove
		if (move instanceof TicketMove) {
			player = makeTicketMove(player, (TicketMove)move);
		} else if (move instanceof DoubleMove) { // If given move is a DoubleMove
			player = makeDoubleMove(player, (DoubleMove)move);
		} else { // If Pass move is given
			for (Spectator spectator : spectators) spectator.onMoveMade(this, move);
		}

		// Check if game is over. If not tell next player to play
		validMoves = endOfTurn(validMoves);
	}

	// Chooses next player
	private void chooseNextPlayer(ScotlandYardPlayer player) {
		for (int k = 0; k < players.size(); k++) {
			if (players.get(k).equals(player)) {
				if (k == players.size() - 1) currentPlayer = players.get(0); // If last player to play was the last player, then return to the start of the list
				else currentPlayer = players.get(k+1); // Otherwise, use next player
	}}}

	// Update game when player makes a TicketMove
	private ScotlandYardPlayer makeTicketMove(ScotlandYardPlayer player, TicketMove move) {
		// Update player
		player.location(move.destination());
		player.removeTicket(move.ticket());

		// If current player is a detective, give MrX used ticket
		// Otherwise, start a new round
		if (player.isDetective()) mrXPlayer.addTicket(move.ticket());
		else {
			currentRound++;
			for (Spectator spectator : spectators) spectator.onRoundStarted(this, currentRound);
		}

		// Tell spectators that a move has been made
		if (player.isMrX() && !rounds.get(currentRound - 1)) {
			for (Spectator spectator : spectators) spectator.onMoveMade(this, new TicketMove(player.colour(), move.ticket(), mrXLastLocation));
		} else{
			for (Spectator spectator : spectators) spectator.onMoveMade(this, move);
		}

		return player;
	}

	// Update game when player makes a DoubleMove
	private ScotlandYardPlayer makeDoubleMove(ScotlandYardPlayer player, DoubleMove move) {
		doubleMoveMade = true;

		int mrXSafe = mrXLastLocation;
		int position1 = move.firstMove().destination();
		int position2 = move.secondMove().destination();
		TicketMove move1, move2;

		// Update player
		player.removeTicket(Ticket.DOUBLE);
		player.location(position2);

		// If it is a reveal round, update MrX's location
		if (rounds.get(currentRound)) mrXLastLocation = position1;

		move1 = new TicketMove(player.colour(), move.firstMove().ticket(), mrXLastLocation);

		if (rounds.get(currentRound + 1)) mrXLastLocation = position2;

		move2 = new TicketMove(player.colour(), move.secondMove().ticket(), mrXLastLocation);
		move = new DoubleMove (player.colour(), move1, move2);

		mrXLastLocation = mrXSafe;
		// Tell spectators that a move has been made
		for (Spectator spectator : spectators){
			spectator.onMoveMade(this, move);
		}

		// Execute start of round for the first round of the double move
		startRound(player, move1);

		// Produce the second round of the double move
		startRound(player, move2);

		doubleMoveMade = false;

		return player;
	}

	// Execute end of round for a DoubleMove
	private void startRound(ScotlandYardPlayer player, TicketMove move) {
		if (rounds.get(currentRound)) mrXLastLocation = move.destination(); // If round was a reveal round
		player.removeTicket(move.ticket()); // Spend first ticket
		currentRound++;
		for (Spectator spectator : spectators) spectator.onRoundStarted(this, currentRound);
		for (Spectator spectator : spectators) spectator.onMoveMade(this, move);
	}

	// Check if game is over. If not tell next player to play
	private Set<Move> endOfTurn(Set<Move> validMoves) {
		// Check if the game is over
		if (isGameOver()) for(Spectator spectator : spectators) spectator.onGameOver(this,getWinningPlayers());

		// Tell next player to play
		if (currentPlayer.isMrX() && !isGameOver()) for(Spectator spectator : spectators) spectator.onRotationComplete(this);
		else if(!currentPlayer.isMrX() && !isGameOver()) {
			validMoves = getDetectiveValidMoves(currentPlayer);
			currentPlayer.player().makeMove(this, currentPlayer.location(), validMoves, this);
		}

		return validMoves;
	}

	// Returns the ScotlandYardPLayer who has a given colour
	private ScotlandYardPlayer colourToPlayer(Colour colour) {
		for (ScotlandYardPlayer player : players) {
			if (player.colour().equals(colour)) return player;
		}

		return null;
	}

	// Produces set of all possible TicketMoves for a given transport type and player
	private Set<Move> getMoves(Transport transport, Ticket ticket, ScotlandYardPlayer player, Collection<Edge<Integer, Transport>> edges) {
		Set<Move> moves = new HashSet<>();
		List<Integer> invalidLocations = getPlayerLocations(); // Locations of detectives

		for (Edge<Integer, Transport> edge : edges) {
			// If player can get to location with given transport type AND no-one else is on this position
			if (edge.data().equals(transport) && !invalidLocations.contains(edge.destination().value())) moves.add(new TicketMove(player.colour(), ticket, edge.destination().value()));
		}

		return moves;
	}

	// Produces set of all possible DoubleMoves for a give player and transport type for the second move
	private Set<Move> getDoubleMoves(Transport transport, Ticket ticket, ScotlandYardPlayer player, Collection<Edge<Integer, Transport>> edges) {
		Set<Move> moves = new HashSet<>();
		List<Integer> invalidLocations = getPlayerLocations();

		for (Edge<Integer, Transport> edge : edges) {
			// If move is legal
			if (edge.data().equals(transport) && !invalidLocations.contains(edge.destination().value())) {
				Collection<Edge<Integer, Transport>> secondEdges = graph.getEdgesFrom(edge.destination());

				// Check all possible second moves
				for (Edge<Integer, Transport> secondEdge : secondEdges) {

					// If move uses the same ticket twice
					if (edge.data().equals(secondEdge.data())) {
						// If player has two tickets for this transport type
						if (player.hasTickets(Ticket.fromTransport(edge.data()), 2) && !invalidLocations.contains(secondEdge.destination().value())) moves.add(new DoubleMove(player.colour(), ticket, edge.destination().value(), Ticket.fromTransport(secondEdge.data()), secondEdge.destination().value()));
						// If player has a secret ticket and tcket for the required transport
						if (player.hasTickets(Ticket.SECRET) && !invalidLocations.contains(secondEdge.destination().value())) {
							moves.add(new DoubleMove(player.colour(), ticket, edge.destination().value(), Ticket.SECRET, secondEdge.destination().value())); // Secret move second
							moves.add(new DoubleMove(player.colour(), Ticket.SECRET, edge.destination().value(), ticket, secondEdge.destination().value())); // Secret move first
						}

					} else { // Move uses two different transport types
						// If player has one of each required transport ticket
						if (player.hasTickets(Ticket.fromTransport(secondEdge.data())) && !invalidLocations.contains(secondEdge.destination().value())) moves.add(new DoubleMove(player.colour(), ticket, edge.destination().value(), Ticket.fromTransport(secondEdge.data()), secondEdge.destination().value()));
						// If player has a secret ticket, add move when secret used second
						// The move when the secret ticket is used first is added when checking moves that use a secret ticket first
						if (player.hasTickets(Ticket.SECRET) && !invalidLocations.contains(secondEdge.destination().value())) moves.add(new DoubleMove(player.colour(), ticket, edge.destination().value(), Ticket.SECRET, secondEdge.destination().value()));
		}}}}

		return moves;
	}

	// Returns list of detective locations
	private List<Integer> getPlayerLocations() {
		List<Integer> locations = new ArrayList<>();

		for (ScotlandYardPlayer player : players) {
			if (player.isDetective())locations.add(player.location());
		}

		return locations;
	}

	// Get valid moves for MrX
  private Set<Move> getMrXValidMoves(ScotlandYardPlayer player) {
		Set<Move> validMoves = new HashSet<>();
		Collection<Edge<Integer,Transport>> edges = graph.getEdgesFrom(new Node<Integer>(player.location())); // Edges MrX can travel down

		// Get TicketMoves which use a transport ticket
		if (player.hasTickets(Ticket.TAXI)) validMoves.addAll(getMoves(Transport.TAXI, Ticket.TAXI, player, edges));
		if (player.hasTickets(Ticket.BUS)) validMoves.addAll(getMoves(Transport.BUS, Ticket.BUS, player, edges));
		if (player.hasTickets(Ticket.UNDERGROUND)) validMoves.addAll(getMoves(Transport.UNDERGROUND, Ticket.UNDERGROUND, player, edges));

		// Get TicketMoves which use a secret ticket
		if (player.hasTickets(Ticket.SECRET)) validMoves.addAll(getMoves(Transport.FERRY, Ticket.SECRET, player, edges));
		if (player.hasTickets(Ticket.SECRET)) validMoves.addAll(getMoves(Transport.TAXI, Ticket.SECRET, player, edges));
		if (player.hasTickets(Ticket.SECRET)) validMoves.addAll(getMoves(Transport.BUS, Ticket.SECRET, player, edges));
		if (player.hasTickets(Ticket.SECRET)) validMoves.addAll(getMoves(Transport.UNDERGROUND, Ticket.SECRET, player, edges));

		// Get DoubleMoves if enough rounds left
		if (player.hasTickets(Ticket.DOUBLE) && ((rounds.size()-1) - currentRound) > 1) {
			// DoubleMoves that use a transport ticket first
			if (player.hasTickets(Ticket.TAXI)) validMoves.addAll(getDoubleMoves(Transport.TAXI, Ticket.TAXI, player, edges));
			if (player.hasTickets(Ticket.BUS)) validMoves.addAll(getDoubleMoves(Transport.BUS, Ticket.BUS, player, edges));
			if (player.hasTickets(Ticket.UNDERGROUND)) validMoves.addAll(getDoubleMoves(Transport.UNDERGROUND, Ticket.UNDERGROUND, player, edges));

			// DoubleMoves that use a secret ticket first
			if (player.hasTickets(Ticket.SECRET)) validMoves.addAll(getDoubleMoves(Transport.FERRY, Ticket.SECRET, player, edges));
			if (player.hasTickets(Ticket.SECRET)) validMoves.addAll(getDoubleMoves(Transport.TAXI, Ticket.SECRET, player, edges));
			if (player.hasTickets(Ticket.SECRET)) validMoves.addAll(getDoubleMoves(Transport.BUS, Ticket.SECRET, player, edges));
			if (player.hasTickets(Ticket.SECRET)) validMoves.addAll(getDoubleMoves(Transport.UNDERGROUND, Ticket.SECRET, player, edges));
		}

		return validMoves;
	}

	// Get double moves for a detective
  private Set<Move> getDetectiveValidMoves(ScotlandYardPlayer player){
		Set<Move> validMoves = new HashSet<>();
		Collection<Edge<Integer,Transport>> edges = graph.getEdgesFrom(new Node<Integer>(player.location())); // Edges the detective can travel down

		// Get TicketMoves
		if (player.hasTickets(Ticket.TAXI)) validMoves.addAll(getMoves(Transport.TAXI, Ticket.TAXI, player, edges));
		if (player.hasTickets(Ticket.BUS)) validMoves.addAll(getMoves(Transport.BUS, Ticket.BUS, player, edges));
		if (player.hasTickets(Ticket.UNDERGROUND)) validMoves.addAll(getMoves(Transport.UNDERGROUND, Ticket.UNDERGROUND, player, edges));

		// If no valid moves were found, return a PassMove
		if (validMoves.isEmpty()) validMoves.add(new PassMove(player.colour()));

		return validMoves;
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableList(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> colours = new ArrayList<>();
		for(ScotlandYardPlayer player : players){
			colours.add(player.colour());
		}
		return Collections.unmodifiableList(colours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		Set<Colour> winningPlayers = new HashSet<Colour>();

		if (isGameOver()) {
			if (isMrXCornered() || isMrXCaptured()) { // If MrX has lost
				for (ScotlandYardPlayer player : players) {
					if (player.colour().isDetective()) winningPlayers.add(player.colour());
				}
		  } else if (areDetectivesStuck() || mrXEscapes()) { // If the detectives have lost
				winningPlayers.add(mrXPlayer.colour());
		}}

		return Collections.unmodifiableSet(winningPlayers);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {

		// If want MrX's location
		if (colour.isMrX()) {
			// If not first round
			if (currentRound > 0) {
				// If not a reveal round, or a double move was made
				if (!rounds.get(currentRound - 1) || doubleMoveMade) return Optional.of(mrXLastLocation);
				// If it is a reveal round
				mrXLastLocation = mrXPlayer.location();
				return Optional.of(mrXPlayer.location());
			}
			// If first round
			return Optional.of(mrXLastLocation);
		} else { // If want a detective's location
			for(ScotlandYardPlayer player : players) {
				if(player.colour().equals(colour)) return Optional.of(player.location());
		}}

		// If passed an invalid colour
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for(ScotlandYardPlayer player : players) {
			if(player.colour().equals(colour)) return Optional.of(player.tickets().get(ticket));
		}

		// If passed invalid colour
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		// If MrX has been caught
		if (isMrXCaptured()) return true;
		// If MrX cannot move
		if (currentPlayer.equals(mrXPlayer) && isMrXCornered()) return true;
		// If Detectives cannot move
		if (currentPlayer.equals(mrXPlayer) && areDetectivesStuck()) return true;
		// If MrX has evade capture long enough
		if (currentPlayer.equals(mrXPlayer) && mrXEscapes()) return true;
		// Otherwise
		return false;
	}

	// Returns whether MrX has been captured
	private boolean isMrXCaptured() {
		for (ScotlandYardPlayer player : players) {
			// If a detective is on the same location as MrX
			if (player.isDetective() && player.location() == mrXPlayer.location()) return true;
		}

		return false;
	}

	// Returns whether MrX cannot move
	private boolean isMrXCornered() {
		Set<Move> validMoves = getMrXValidMoves(mrXPlayer);
		// If MrX cannot move
		if (validMoves.isEmpty()) return true;
		// If MrX can move
		return false;
	}

	// Returns whether at leasts one detective can move
	private boolean areDetectivesStuck() {
		Set<Move> validMoves = new HashSet<>();

		for (ScotlandYardPlayer player : players) {
			if (player.isDetective()) {
				validMoves = getDetectiveValidMoves(player);
				// If we have found a detective which can move
				if (!validMoves.contains(new PassMove(player.colour()))) return false;
		}}

		return true;
	}

	// Returns whether MrX has evaded capture long enough
	private boolean mrXEscapes() {
		// If there are rounds left
		if (currentRound < rounds.size()) return false;
		// If no rounds left
		return true;
	}

	@Override
	public Colour getCurrentPlayer() {
		return currentPlayer.colour();
	}

	@Override
	public int getCurrentRound() {
		return currentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<Integer,Transport>(graph);
	}

}
