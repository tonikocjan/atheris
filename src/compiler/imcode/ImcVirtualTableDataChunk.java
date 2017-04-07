package compiler.imcode;

import compiler.Report;
import compiler.frames.FrmLabel;
import compiler.seman.type.ClassType;

/**
 * Created by toni on 06/04/2017.
 */
public class ImcVirtualTableDataChunk extends ImcDataChunk {

    public final ClassType classType;

    /**
     *
     * @param label
     * @param size
     * @param classType
     */
    public ImcVirtualTableDataChunk(FrmLabel label, int size, ClassType classType) {
        super(label, size);
        this.classType = classType;
    }

    @Override
    public void dump() {
        String tmp =  data == null ? "" : " .BYTE: " + data;
        Report.dump(0, "VTABLE CHUNK: label=" + label.name() + " size=" + size + tmp);
    }
}
