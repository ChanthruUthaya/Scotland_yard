package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;

class Leaf extends Tree{

  Integer value;
  Move move;

  Leaf(Integer value){
    this.value = value;
    this.move = move;
  }

  @Override
  void accept(Visitor v){
    v.visit(this);
  }
}
