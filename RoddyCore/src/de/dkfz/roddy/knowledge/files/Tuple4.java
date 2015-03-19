package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.core.ExecutionContext;

/**
 * A file object tuple designed for (generic) job calls with 4 entries
 */
public class Tuple4<X extends FileObject, Y extends FileObject, Z extends FileObject, R extends FileObject> extends Tuple3<X, Y, Z> {
    public R value3;

    public Tuple4(X value0, Y value1, Z value2, R value3) {
        super(value0, value1, value2);
        this.value3 = value3;
    }
}
