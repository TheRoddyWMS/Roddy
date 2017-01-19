/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXICCWrapper;

/**
 * Listener interface for Project Tree Item cells.
 */
public interface ProjectTreeItemCellListener {
    /***
     * The event is called when a hyperlink within the tree was selected.
     * @param pWrapper
     * @param analysisID
     */
    public void analysisSelected(FXICCWrapper pWrapper, String analysisID);
}
