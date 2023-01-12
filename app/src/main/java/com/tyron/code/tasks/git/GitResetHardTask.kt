package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import android.content.Context
import org.eclipse.jgit.api.Git
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast
import com.tyron.code.R
import com.tyron.code.tasks.git.ErrorOutput
import org.eclipse.jgit.api.ResetCommand
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object GitResetHardTask {
    
    fun resetHard(project:Project, context:Context) {
        
       val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.reset_changes)
       builder.setMessage(R.string.reset_confirm)
       builder.setPositiveButton(R.string.title_reset) { _, _ ->
         
       val future =
       executeAsyncProvideError( {
       
       Git.open(project.getRootFile()).reset().setMode(ResetCommand.ResetType.HARD).call()
            
        return@executeAsyncProvideError
       }, { _, _ -> } ) 
        
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
      
       if (result == null || error != null) { ErrorOutput.ShowError(error, context) }
       else { Toast.makeText(context, context.getString(R.string.all_changes_discarded),Toast.LENGTH_SHORT).show() }
     
       } } 
     
       }
       
       builder.setNegativeButton(android.R.string.cancel, null)
       builder.show()  
    
}
}
