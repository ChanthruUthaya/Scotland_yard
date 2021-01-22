package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.Transport;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import uk.ac.bris.cs.gamekit.graph.Graph;

@ManagedAI("MyAI")
public class MyAI implements PlayerFactory {

	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private static class MyPlayer implements Player {

		private final Random random = new Random();
		private Graph<Integer, Transport> graph;
		private List<Colour> players;
    private Map<Integer, Colour> colourLocations;
		private List<Optional<Integer>> locations;
		private ScotlandYardView scotlandView;
		private Set<Ticket> ticketTypes = new HashSet<>();
		private Set<Ticket> detectiveTicketTypes = new HashSet<>();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {

			long startTime = System.nanoTime();

			players = view.getPlayers();
			locations = new ArrayList<>(); // Locations of detectives
			graph = view.getGraph();
			scotlandView = view;
			colourLocations = new HashMap<>();
			Collection<Map<Integer, Colour>> combinations = mostLikelyLocations();
			System.out.printf("# likely combinations - %d\n", combinations.size());
			
			// Collect all detective locations
			for(Colour player : players){
				if(player.isDetective()){
					locations.add(view.getPlayerLocation(player));
					colourLocations.put(view.getPlayerLocation(player).get(), player);
			}}

			ticketTypes.addAll(Arrays.asList(Ticket.BUS, Ticket.DOUBLE, Ticket.SECRET, Ticket.TAXI, Ticket.UNDERGROUND));
			detectiveTicketTypes.addAll(Arrays.asList(Ticket.BUS, Ticket.UNDERGROUND, Ticket.TAXI));

			System.out.printf("Location - %d.\n", location);
			//moves = prune(moves);

			System.out.printf("Moves - %d\n", moves.size());
			Set<Move> selectedMoves = bestestMoves(combinations, moves);

			// Choose best move
			System.out.printf("Possible moves %d\n", selectedMoves.size());

			Move toMake = new ArrayList<>(selectedMoves).get(random.nextInt(selectedMoves.size()));

			long totalTime = System.nanoTime() - startTime;
			System.out.printf("Computation time - %.2f s\n---------------\n", (float) totalTime / 1000000000);

			// Make move
			callback.accept(toMake);

		}

		// Prunes out surplus moves
		private Set<Move> prune(Set<Move> m) {

			// If reveal round
			if (scotlandView.getRounds().get(scotlandView.getCurrentRound())) {
				if (scotlandView.getPlayerTickets(Colour.BLACK, Ticket.DOUBLE).get() > 0) { // If MrX can make a double move
					m = revealRoundDoubleMove(m);
					System.out.printf("m%d - %d\n", 0, m.size());
			}} else if (scotlandView.getPlayerTickets(Colour.BLACK, Ticket.DOUBLE).get() > 0) { // Don't make double move if not a reveal round
				m = removeDoubleMoves(m);
				System.out.printf("m%d - %d\n", 0, m.size());
			}

			System.out.printf("m%d - %d\n", 1, m.size());
			m = removeExpensiveMoves(m);
			System.out.printf("m%d - %d\n", 2, m.size());
			m = removeUnnecessaryDoubleMoves(m);
			System.out.printf("m%d - %d\n", 3, m.size());

			/*  IDEAS
			 *  Don't move to islands.
			 *  Assess risk, then decide if double/secret move is required
			 */

			return m;
		}

		// Removes moves which can be achieved using a more plentiful ticket type.
		private Set<Move> removeExpensiveMoves(Set<Move> m) {
			Map<Integer, Collection<Ticket>> map = new HashMap<>();
			Set<Move> cheapestMoves = new HashSet<>();

			// Builds map of destintations and possible tickets to get to there
			for (Move move : m) {

				if (move instanceof TicketMove) {

					TicketMove t = (TicketMove) move;

					if (!map.containsKey(t.destination())) { // If destination not currently used in map
						Collection<Ticket> tickets = new HashSet<>();
						tickets.add(t.ticket());
						map.put(t.destination(), tickets);
					} else { // If destination already in map
						Collection<Ticket> tickets = map.get(t.destination());
						tickets.add(t.ticket()); // Update collection to include new ticket type
						map.remove(t.destination());
						map.put(t.destination(), tickets);
					}

				} else {

					DoubleMove d = (DoubleMove) move;
					if (!map.containsKey(d.finalDestination())) { // If destination not currently used in map
						Collection<Ticket> tickets = new HashSet<>();
						tickets.add(Ticket.DOUBLE);
						map.put(d.finalDestination(), tickets);
					} else { // If destination already in map
						Collection<Ticket> tickets = map.get(d.finalDestination());
						tickets.add(Ticket.DOUBLE); // Update collection to include double ticket
						map.remove(d.finalDestination());
						map.put(d.finalDestination(), tickets);
					}
				}
			}

			// Builds a set of moves which excludes moves that can be achieved using a more plentiful ticket type
			for (Move move : m) {

				if (move instanceof TicketMove) {

					TicketMove t = (TicketMove) move;
					if (map.get(t.destination()).size() == 1) cheapestMoves.add(t); // If only way to get to destination
					else { // If multiple ways to get to destination
						Boolean cheapest = true;
						Integer num = scotlandView.getPlayerTickets(Colour.BLACK, t.ticket()).get();
						for (Ticket ticket : ticketTypes) { // Check for a more plentiful ticket type
							if (map.get(t.destination()).contains(ticket) && (!t.ticket().equals(ticket))) {
								Integer num1 = scotlandView.getPlayerTickets(Colour.BLACK, ticket).get();
								if (num1 < num) cheapest = false; // If alternative ticket type is more plentiful
						}}
						if (cheapest) cheapestMoves.add(t); // If this is most plentiful ticket to destination
					}

				} else if (move instanceof DoubleMove) {
					DoubleMove d = (DoubleMove) move;
					if (map.get(d.finalDestination()).size() == 1) cheapestMoves.add(d); // If only way to get to destination
					else { // If multiple ways to get to destination
						Boolean cheapest = true;
						Integer num = scotlandView.getPlayerTickets(Colour.BLACK, Ticket.DOUBLE).get();
						for (Ticket ticket : ticketTypes) { // Check for a more plentiful ticket type
							if (map.get(d.finalDestination()).contains(ticket) && (!ticket.equals(Ticket.DOUBLE))) {
								Integer num1 = scotlandView.getPlayerTickets(Colour.BLACK, ticket).get();
								if (num1 < num) cheapest = false; // If alternative ticket type is more plentiful
						}}
						if (cheapest) cheapestMoves.add(d); // If this is most plentiful ticket to destination
					}
				}

			}

			return Collections.unmodifiableSet(cheapestMoves);
		}

		// Removes double moves which can be achieved in one move.
		private Set<Move> removeUnnecessaryDoubleMoves(Set<Move> m) {
			Set<Integer> achievableInSingle = new HashSet<>();
			Set<Move> necessaryMoves = new HashSet<>();

			// Finds all destinations that can be reached without a double ticket
			for (Move move : m) {
				if (move instanceof TicketMove) {
					achievableInSingle.add(((TicketMove)move).destination());
					necessaryMoves.add(move);
				}
			}

			// Builds a set which excludes double moves that can be achieved in a single move
			for (Move move : m) {
				if (move instanceof DoubleMove) {
					if (!achievableInSingle.contains(((DoubleMove)move).finalDestination())) necessaryMoves.add(move);
				}
			}

			return Collections.unmodifiableSet(necessaryMoves);
		}

		// Returns set of only double moves.
		// If MrX has a secret ticket then returns set of only double moves which use a secret ticket as the second move.
		private Set<Move> revealRoundDoubleMove(Set<Move> m) {
			Set<Move> doubleMoves = new HashSet<>();
			Set<Move> secretMoves = new HashSet<>();

			// Builds set of double moves
			for (Move move : m) {
				if (move instanceof DoubleMove) doubleMoves.add(move);
			}

			// If Mr X has a secret ticket
			if (scotlandView.getPlayerTickets(Colour.BLACK, Ticket.SECRET).get() > 0d) {
				// Filter out double moves that don't use a secret ticket for the second move
				for (Move move : doubleMoves) {
					DoubleMove d = (DoubleMove) move;
					if (d.secondMove().ticket().equals(Ticket.SECRET)) secretMoves.add(d);
				}
				return Collections.unmodifiableSet(secretMoves);
			}

			return Collections.unmodifiableSet(doubleMoves);
		}

		// Removes double moves.
		// If no double moves then returns passed set.
		private Set<Move> removeDoubleMoves(Set<Move> m) {
			Set<Move> singleMoves = new HashSet<>();

			for (Move move : m) {
				if (move instanceof TicketMove) {
					singleMoves.add(move);
			}}

			if (singleMoves.size() > 0) return Collections.unmodifiableSet(singleMoves);

			return m;
		}

		// Gets set of best moves given certain combinations
		private Set<Move> bestestMoves(Collection<Map<Integer, Colour>> combinations, Set<Move> moves) {
			Set<Move> best = new HashSet<>();
			Map<Move, Integer> map = new HashMap<>();

			Collection<Move> goodMoves = bestMoves(combinations, moves);
			for (Move m : goodMoves) {
				if (!map.containsKey(m)) map.put(m, 1);
				else {
					int n = map.get(m);
					map.remove(m);
					map.put(m, n + 1);
			}}

			int bestCount = 0;

			for (Move m : goodMoves) {
				int n = map.get(m);
				if (n == bestCount) best.add(m);
				else if (n > bestCount) {
					best.clear();
					best.add(m);
					bestCount = n;
				}
			}

			System.out.printf("Good Moves - %d\nMost Occurences - %d\n", goodMoves.size(), bestCount);

			return best;
		}

		// Gets collection of all best moves
		private Collection<Move> bestMoves(Collection<Map<Integer, Colour>> combinations, Set<Move> moves) {
			Collection<Move> best = new ArrayList<>();

			for (Map<Integer, Colour> combination : combinations) best.addAll(bestMovesForScenario(combination, moves));

			return best;
		}

		// Finds best moves for a given set of detective locations
		private Set<Move> bestMovesForScenario(Map<Integer, Colour> detectiveLocations, Set<Move> moves) {
			Map<Move, Float> scores = new HashMap<>();

			for(Move move : moves){
				if (move instanceof TicketMove) scores.put(move, sumMove(movesToLocation(((TicketMove)move).destination(), detectiveLocations)));
				else if (move instanceof DoubleMove) scores.put(move, sumMove(movesToLocation(((DoubleMove)move).finalDestination(), detectiveLocations)));
			}

			Set<Move> bestMoves = selectMove(scores);

			return bestMoves;
		}

		// Returns the fewest number of moves required for a detecive to rech a given location.
		private Map<Colour, Integer> movesToLocation(Integer location, Map<Integer, Colour> detectiveLocations) {

			Integer count = 1;
			Collection<Edge<Integer, Transport>> e = new HashSet<>();
			Collection<Edge<Integer, Transport>> e2 = new HashSet<>();

			Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(new Node<Integer>(location));
			e2.addAll(edges);

			Map<Colour, Integer> movesForColour = new HashMap<>();
			for(Colour player : players) {
				if (player.isDetective()) movesForColour.put(player, -1);
			}

			while(movesForColour.values().contains(-1)){
			// While no detectives on layer
				while(checkLayer(e2, detectiveLocations).size() == 0) {
					count++;
					e.clear();
					for (Edge<Integer, Transport> edge: e2) {
						e.addAll(graph.getEdgesFrom(new Node<Integer>(edge.destination().value())));
					}
					e2.clear();
					e2.addAll(e);
				}

				for(Colour colour : checkLayer(e2, detectiveLocations)){
					if(movesForColour.get(colour) == -1){
						movesForColour.remove(colour);
						movesForColour.put(colour, count);
					}
				}

				e.clear();
				for (Edge<Integer, Transport> edge: e2) {
					e.addAll(graph.getEdgesFrom(new Node<Integer>(edge.destination().value())));
				}
				e2.clear();
				e2.addAll(e);

				count++;
			}

			return movesForColour;

		}

		// Returns collcetion of detectives on layer
		private Collection<Colour> checkLayer(Collection<Edge<Integer, Transport>> edges, Map<Integer, Colour> map) {

			Collection<Colour> colours = new HashSet<>();

			for (Edge<Integer, Transport> edge : edges) {
				if (map.containsKey(edge.destination().value())) colours.add(map.get(edge.destination().value()));
			}

			return colours;
		}

		//not used currently
		private List<Integer> possibleLocations(Colour player){
			List<Integer> playerlocations = new ArrayList<>();
			Map<Ticket,Integer> tickets = new HashMap<>();
			Integer location = scotlandView.getPlayerLocation(player).get();

			tickets.put(Ticket.BUS, scotlandView.getPlayerTickets(player, Ticket.BUS).get());
			tickets.put(Ticket.TAXI, scotlandView.getPlayerTickets(player, Ticket.TAXI).get());
			tickets.put(Ticket.UNDERGROUND, scotlandView.getPlayerTickets(player, Ticket.UNDERGROUND).get());

			 Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(new Node<Integer>(location));
			 for(Edge<Integer, Transport> edge : edges){
				if(tickets.get(Ticket.fromTransport(edge.data())) > 0) playerlocations.add(edge.destination().value());
			}

			return playerlocations;
		}

		// Most likely locations of a given player after one round
		private Map<Integer, Colour> mostLikelyLocation(Colour player) {
			List<Integer> possibleLocations = possibleLocations(player);
			Map<Integer, Integer> map = new HashMap<>();

			for (Integer i : possibleLocations) {
				if (!map.containsKey(i)) map.put(i, 1);
				else {
					int n = map.get(i);
					map.remove(i);
					map.put(i, n + 1);
			}}

			Map<Integer, Colour> mostLikely = new HashMap<>();
			int commonCount = 0;

			for (Integer i : possibleLocations) {
				int n = map.get(i);
				if (n > commonCount) {
					commonCount = n;
					mostLikely.clear();
					mostLikely.put(i, player);
				} else if (n == commonCount) mostLikely.put(i, player);
			}

			return mostLikely;
		}

		// Most likely combinations of locations of detectives
		private Collection<Map<Integer, Colour>> mostLikelyLocations () {
			Collection<Map<Integer, Colour>> likelyPositions = new ArrayList<>();
			Collection<Map<Integer, Colour>> likelyCombinations = new ArrayList<>();

			for (Colour c : players) if (c.isDetective()) likelyPositions.add(mostLikelyLocation(c));

			for (Map<Integer, Colour> map : likelyPositions) {
				Colour c = map.values().iterator().next();
				if (likelyCombinations.isEmpty()) {
					for (int i : map.keySet()) {
						Map<Integer, Colour> combination = new HashMap<>();
						combination.put(i, c);
						likelyCombinations.add(combination);
				}} else {
					Collection<Map<Integer, Colour>> newLikelyCombinations = new ArrayList<>();
					for (Map<Integer, Colour> combination : likelyCombinations) {
						for (int i : map.keySet()) {
							if (!combination.keySet().contains(i)) {
								Map<Integer, Colour> newCombination = new HashMap<>(combination);
								newCombination.put(i, c);
								newLikelyCombinations.add(newCombination);
					}}}
					likelyCombinations = newLikelyCombinations;
			}}

			return likelyCombinations;
		}

		// Returns the sum how far each detecive is from a location
		private float sumMove(Map<Colour,Integer> map){
			Collection<Integer> ints = map.values();
			float score = 0 ;
			for (Integer i : ints){
				score += (float) 1/i;
			}
			return score;
		}

		// Creates set of highest scoring moves
		private Set<Move> selectMove(Map<Move,Float> scores){
			Set<Move> selectedMoves = new HashSet<>();
			Set<Move> moves = scores.keySet();
      float bestScore  = 100;

			for(Move move : moves){
				// If move is new highest scorer, clear list then add move
				if(scores.get(move) < bestScore){
          selectedMoves.clear();
					selectedMoves.add(move);
					bestScore = scores.get(move);
				} else if(scores.get(move) == bestScore){ // If move is equal highest scorer, add to list
					selectedMoves.add(move);
				}
			}

			return selectedMoves;
		}

	}
}
