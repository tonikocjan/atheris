package compiler.imcode;

import compiler.Logger;
import compiler.frames.FrmTemp;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by toni on 11/04/2017.
 */
public class ImcMethodCALL extends ImcExpr {

    /** Temporary holding method's frame.  */
    public final FrmTemp temp;

    /** Argumenti funkcijskega klica (vkljucno s FP).  */
    public LinkedList<ImcExpr> args;

    /**
     * Ustvari nov klic funkcije.
     *
     * @param temp Labela funkcije.
     */
    public ImcMethodCALL(FrmTemp temp) {
        this.temp = temp;
    }

    @Override
    public void dump(int indent) {
        Logger.dump(indent, "METHOD CALL temp=" + temp.getName());
        Iterator<ImcExpr> args = this.args.iterator();

        while (args.hasNext()) {
            ImcExpr arg = args.next();
            arg.dump(indent + 2);
        }
    }

    @Override
    public ImcESEQ linear() {
        ImcSEQ linStmt = new ImcSEQ();
        ImcMethodCALL linCall = new ImcMethodCALL(temp);
        linCall.args = new LinkedList<>();

        Iterator<ImcExpr> args = this.args.iterator();

        while (args.hasNext()) {
            FrmTemp temp = new FrmTemp();
            ImcExpr arg = args.next();
            ImcESEQ linArg = arg.linear();

            linStmt.stmts.addAll(((ImcSEQ)linArg.stmt).stmts);
            linStmt.stmts.add(new ImcMOVE(new ImcTEMP(temp), linArg.expr));
            linCall.args.add(new ImcTEMP(temp));
        }

        FrmTemp temp = new FrmTemp();
        linStmt.stmts.add(new ImcMOVE(new ImcTEMP(temp), linCall));

        return new ImcESEQ(linStmt, new ImcTEMP(temp));
    }
}
