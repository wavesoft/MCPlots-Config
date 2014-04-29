/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.cern.mcplots.ui;

/**
 *
 * @author icharala
 */
public interface ProgressEvents {

    public void progressStarted();
    public void progress( String message, int item, int max );
    public void progressCompleted();

}
