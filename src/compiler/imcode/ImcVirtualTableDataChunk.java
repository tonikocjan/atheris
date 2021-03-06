package compiler.imcode;

import compiler.frames.FrmLabel;
import compiler.seman.type.ClassType;

/**
 * Created by toni on 06/04/2017.
 */
public class ImcVirtualTableDataChunk extends ImcDataChunk {

    public final ClassType classType;

    public ImcVirtualTableDataChunk(FrmLabel label, int size, ClassType classType) {
        super(label, size);
        this.classType = classType;
    }

    @Override
    public void dump() {
        String tmp =  data == null ? "" : " .BYTE: " + data;
        logger.dump(0, "VTABLE CHUNK: entryLabel=" + label.getName() + " sizeInBytes=" + size + tmp);
    }
}
