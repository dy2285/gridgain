package org.apache.ignite.internal.processors.cache.checker.objects;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;

public class NodePartitionSize extends IgniteDataTransferObject {
    public boolean inProgress;

    public KeyCacheObject lastKey;

    public Long oldSize;

    public Long newSize;

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeObject(inProgress);
        out.writeObject(lastKey);
        out.writeObject(oldSize);
        out.writeObject(newSize);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer,
        ObjectInput in) throws IOException, ClassNotFoundException {
        inProgress = (boolean)in.readObject();
        lastKey = (KeyCacheObject)in.readObject();
        oldSize = (Long)in.readObject();
        newSize = (Long)in.readObject();
    }
}