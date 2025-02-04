package uk.ac.bris.cs.scotlandyard.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.bris.cs.scotlandyard.harness.TestHarness;

import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.bus;
import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.pass;
import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.rounds;
import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.secret;
import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.taxi;
import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.underground;
import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.x2;
import static uk.ac.bris.cs.scotlandyard.harness.PlayerInteractions.player;
import static uk.ac.bris.cs.scotlandyard.harness.Requirement.containsOnly;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Colour.GREEN;
import static uk.ac.bris.cs.scotlandyard.model.Colour.RED;

/**
 * Tests whether valid moves are generated by the model
 */

 // NOTE All passing on 21/03
public class ModelValidMoveTest extends ParameterisedModelTestBase {

	private TestHarness harness;
	@Before public void initialise() { harness = new TestHarness(); }
	@After public void tearDown() { harness.forceReleaseShutdownLock(); }

	// -- Detective related tests --

	@Test
	public void testDetectiveAt128MovesShouldProduce13ValidMoves() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 128, 11, 8, 4, 0, 0);

		harness.play(createGame(mrX, blue)).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().willPick(taxi(86)),
				player(BLUE).makeMove().givenMoves(containsOnly(
						underground(BLUE, 89),
						underground(BLUE, 185),
						underground(BLUE, 140),
						bus(BLUE, 187),
						bus(BLUE, 199),
						bus(BLUE, 135),
						bus(BLUE, 142),
						bus(BLUE, 161),
						taxi(BLUE, 188),
						taxi(BLUE, 142),
						taxi(BLUE, 143),
						taxi(BLUE, 160),
						taxi(BLUE, 172))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testDetectiveMovesOmittedIfNotEnoughTickets() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 128, 0, 8, 4, 0, 0);

		harness.play(createGame(mrX, blue)).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().willPick(taxi(86)),
				player(BLUE).makeMove().givenMoves(containsOnly(
						underground(BLUE, 89),
						underground(BLUE, 185),
						underground(BLUE, 140),
						bus(BLUE, 187),
						bus(BLUE, 199),
						bus(BLUE, 135),
						bus(BLUE, 142),
						bus(BLUE, 161))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testDetectiveWithNoValidMovesShouldProducePassMove() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104);
		PlayerConfiguration red = harness.newPlayer(RED, 111);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 128, 0, 0, 0, 0, 0);

		harness.play(createGame(mrX, red, blue))
				.startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().willPick(taxi(86)),
				player(RED).makeMove().willPick(taxi(124)),
				player(BLUE).makeMove().givenMoves(containsOnly(pass(BLUE))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testDetectiveMoveOmittedIfLocationOccupiedByOtherDetective() {
		// this happens around london zoo where an awkward taxi route appears
		// around location 2
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104);
		PlayerConfiguration red = harness.newPlayer(RED, 10, 0, 0, 0, 0, 0);
		PlayerConfiguration green = harness.newPlayer(GREEN, 2);

		// green can only move to 20 because 10 is blocked
		harness.play(createGame(mrX, red, green))
				.startRotationAndAssertTheseInteractionsOccurInOrder(
						player(BLACK).makeMove().willPick(taxi(86)),
						player(RED).makeMove().willPick(pass()),
						player(GREEN).makeMove().givenMoves(containsOnly(
								taxi(GREEN, 20))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testDetectiveMoveNotOmittedIfDestinationOccupiedByMrX() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 86);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 85);

		harness.play(createGame(mrX, blue)).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().willPick(taxi(103)),
				// MrX's location should be a valid destination, where he will be caught
				player(BLUE).makeMove().givenMoves(containsOnly(
						taxi(BLUE, 103),
						taxi(BLUE, 68),
						taxi(BLUE, 84))))
				.thenIgnoreAnyFurtherInteractions();
	}

	// MrX related tests

	@Test
	public void testMrXDoubleMoveIntermediateMovesOmittedIfDestinationOccupiedByDetectives() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 4, 3, 3, 2, 5);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 116);

		// no destination should end up at 116(blue), applies for first move and
		// second move
		harness.play(createGame(mrX, blue)).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						taxi(BLACK, 86),
						secret(BLACK, 86),
						x2(BLACK, taxi(86), bus(52)),
						x2(BLACK, taxi(86), secret(52)),
						x2(BLACK, taxi(86), taxi(69)),
						x2(BLACK, taxi(86), secret(69)),
						x2(BLACK, taxi(86), bus(87)),
						x2(BLACK, taxi(86), secret(87)),
						x2(BLACK, taxi(86), bus(102)),
						x2(BLACK, taxi(86), secret(102)),
						x2(BLACK, taxi(86), taxi(103)),
						x2(BLACK, taxi(86), secret(103)),
						x2(BLACK, taxi(86), taxi(104)),
						x2(BLACK, taxi(86), secret(104)),
						x2(BLACK, secret(86), bus(52)),
						x2(BLACK, secret(86), secret(52)),
						x2(BLACK, secret(86), taxi(69)),
						x2(BLACK, secret(86), secret(69)),
						x2(BLACK, secret(86), bus(87)),
						x2(BLACK, secret(86), secret(87)),
						x2(BLACK, secret(86), bus(102)),
						x2(BLACK, secret(86), secret(102)),
						x2(BLACK, secret(86), taxi(103)),
						x2(BLACK, secret(86), secret(103)),
						x2(BLACK, secret(86), taxi(104)),
						x2(BLACK, secret(86), secret(104)))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testMrXMovesOmittedIfDestinationOccupiedByDetectives() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 4, 3, 3, 0, 5);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 116);

		// no destination should end up at 116(blue) and no double move
		harness.play(createGame(mrX, blue)).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						taxi(BLACK, 86),
						secret(BLACK, 86))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testMrXMustHaveEnoughTicketsForDoubleMove() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 1, 1, 0, 2, 0);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 117);

		harness.play(createGame(mrX, blue)).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						// no repeated tickets with double move for taxi and bus because we only
						// have one of each
						taxi(BLACK, 86),
						taxi(BLACK, 116),
						x2(BLACK, taxi(86), bus(52)),
						x2(BLACK, taxi(86), bus(87)),
						x2(BLACK, taxi(86), bus(102)),
						x2(BLACK, taxi(86), bus(116)),
						x2(BLACK, taxi(116), bus(86)),
						x2(BLACK, taxi(116), bus(108)),
						x2(BLACK, taxi(116), bus(127)),
						x2(BLACK, taxi(116), bus(142)))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testMrXNoSecretMovesIfNoSecretMoveTickets() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 4, 3, 3, 2, 0);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 117);

		harness.play(createGame(mrX, blue)).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						taxi(BLACK, 86),
						taxi(BLACK, 116),
						x2(BLACK, taxi(86), bus(52)),
						x2(BLACK, taxi(86), taxi(69)),
						x2(BLACK, taxi(86), bus(87)),
						x2(BLACK, taxi(86), bus(102)),
						x2(BLACK, taxi(86), taxi(103)),
						x2(BLACK, taxi(86), taxi(104)),
						x2(BLACK, taxi(86), bus(116)),
						x2(BLACK, taxi(116), bus(86)),
						x2(BLACK, taxi(116), taxi(104)),
						x2(BLACK, taxi(116), bus(108)),
						x2(BLACK, taxi(116), taxi(118)),
						x2(BLACK, taxi(116), taxi(127)),
						x2(BLACK, taxi(116), bus(127)),
						x2(BLACK, taxi(116), bus(142)))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testMrXNoDoubleMovesIfNoDoubleMoveTickets() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 4, 3, 3, 0, 5);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 117);

		// no double move generated if no double move tickets
		harness.play(createGame(mrX, blue)).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						taxi(BLACK, 86),
						secret(BLACK, 86),
						taxi(BLACK, 116),
						secret(BLACK, 116))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testMrXNoDoubleMovesIfNotEnoughRoundLeft() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 4, 3, 3, 2, 5);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 117);

		// no double move because we have no next round to play the second move
		harness.play(createGame(rounds(true), mrX, blue))
				.startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						taxi(BLACK, 86),
						secret(BLACK, 86),
						taxi(BLACK, 116),
						secret(BLACK, 116))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testMrXNoTicketMovesIfNoTicketMoveTickets() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 1, 0, 1, 0, 0);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 117);
		ScotlandYardGame game = createGame(rounds(true), mrX, blue);

		// no bus moves as there are no bus tickets
		harness.play(game).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						taxi(BLACK, 86),
						taxi(BLACK, 116))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testMrXOnlySecretMovesIfOnlySecretMoveTicketsLeft() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 0, 0, 0, 0, 1);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 117);
		ScotlandYardGame game = createGame(rounds(true), mrX, blue);

		// only secret moves if only secret move ticket left
		harness.play(game).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						secret(BLACK, 86),
						secret(BLACK, 116))))
				.thenIgnoreAnyFurtherInteractions();
	}

	@Test
	public void testMrXAt104ShouldProduce60ValidMoves() {
		PlayerConfiguration mrX = harness.newPlayer(BLACK, 104, 4, 3, 3, 2, 5);
		PlayerConfiguration blue = harness.newPlayer(BLUE, 117);
		ScotlandYardGame game = createGame(mrX, blue);

		// 60 moves in total, note the permutation pattern and relation of
		// DoubleMove to TicketMove
		harness.play(game).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove().givenMoves(containsOnly(
						taxi(BLACK, 86),
						secret(BLACK, 86),
						taxi(BLACK, 116),
						secret(BLACK, 116),
						x2(BLACK, taxi(86), bus(52)),
						x2(BLACK, taxi(86), secret(52)),
						x2(BLACK, taxi(86), taxi(69)),
						x2(BLACK, taxi(86), secret(69)),
						x2(BLACK, taxi(86), bus(87)),
						x2(BLACK, taxi(86), secret(87)),
						x2(BLACK, taxi(86), bus(102)),
						x2(BLACK, taxi(86), secret(102)),
						x2(BLACK, taxi(86), taxi(103)),
						x2(BLACK, taxi(86), secret(103)),
						x2(BLACK, taxi(86), taxi(104)),
						x2(BLACK, taxi(86), secret(104)),
						x2(BLACK, taxi(86), bus(116)),
						x2(BLACK, taxi(86), secret(116)),
						x2(BLACK, secret(86), bus(52)),
						x2(BLACK, secret(86), secret(52)),
						x2(BLACK, secret(86), taxi(69)),
						x2(BLACK, secret(86), secret(69)),
						x2(BLACK, secret(86), bus(87)),
						x2(BLACK, secret(86), secret(87)),
						x2(BLACK, secret(86), bus(102)),
						x2(BLACK, secret(86), secret(102)),
						x2(BLACK, secret(86), taxi(103)),
						x2(BLACK, secret(86), secret(103)),
						x2(BLACK, secret(86), taxi(104)),
						x2(BLACK, secret(86), secret(104)),
						x2(BLACK, secret(86), bus(116)),
						x2(BLACK, secret(86), secret(116)),
						x2(BLACK, taxi(116), bus(86)),
						x2(BLACK, taxi(116), secret(86)),
						x2(BLACK, taxi(116), taxi(104)),
						x2(BLACK, taxi(116), secret(104)),
						x2(BLACK, taxi(116), bus(108)),
						x2(BLACK, taxi(116), secret(108)),
						x2(BLACK, taxi(116), taxi(118)),
						x2(BLACK, taxi(116), secret(118)),
						x2(BLACK, taxi(116), taxi(127)),
						x2(BLACK, taxi(116), secret(127)),
						x2(BLACK, taxi(116), bus(127)),
						x2(BLACK, taxi(116), secret(127)),
						x2(BLACK, taxi(116), bus(142)),
						x2(BLACK, taxi(116), secret(142)),
						x2(BLACK, secret(116), bus(86)),
						x2(BLACK, secret(116), secret(86)),
						x2(BLACK, secret(116), taxi(104)),
						x2(BLACK, secret(116), secret(104)),
						x2(BLACK, secret(116), bus(108)),
						x2(BLACK, secret(116), secret(108)),
						x2(BLACK, secret(116), taxi(118)),
						x2(BLACK, secret(116), secret(118)),
						x2(BLACK, secret(116), taxi(127)),
						x2(BLACK, secret(116), secret(127)),
						x2(BLACK, secret(116), bus(127)),
						x2(BLACK, secret(116), secret(127)),
						x2(BLACK, secret(116), bus(142)),
						x2(BLACK, secret(116), secret(142)))))
				.thenIgnoreAnyFurtherInteractions();
	}
}
