/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import java.util.List;

/**
 * A generic file group for different purposes. Use this class if thou needst a file group and you do not have your own one.
 * Created by kleinhei on 6/23/14.
 */
public class GenericFileGroup<F extends BaseFile> extends FileGroup<F> {
    public GenericFileGroup(List<F> files) {
        super(files);
    }
}
