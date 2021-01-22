package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;

class Fork extends Tree{
  Tree l;
  Tree r;

  Fork(Tree l, Tree r){
    this.l = l;
    this.r = r;
  }

  @Override
  void accept(Visitor v){
    v.visit(this);
  }
}
