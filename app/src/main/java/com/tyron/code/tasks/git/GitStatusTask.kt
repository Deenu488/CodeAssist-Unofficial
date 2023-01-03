package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import android.content.Context
import org.eclipse.jgit.api.Git
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import com.tyron.code.tasks.git.ErrorOutput
import org.eclipse.jgit.api.Status
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object GitStatusTask {
     
     var mResult:  StringBuffer  =  StringBuffer()
   
    fun status(project:Project, context:Context) {
    
        val future =
       executeAsyncProvideError(    {
           
        var status: Status = Git.open(project.getRootFile()).status().call()
         convertStatus(status)
         
         ThreadUtils.runOnUiThread {     
         val builder = MaterialAlertDialogBuilder(context)      
         builder.setMessage(mResult)       
         builder.show()   }  
                  
       return@executeAsyncProvideError
       
       },     { _, _ -> }  )  
       
       future.whenComplete { result, error ->
   
       ThreadUtils.runOnUiThread {
       if (result == null || error != null) {
       ErrorOutput.ShowError(error, context)
       }}
      
      }
       
   }    
       
        fun convertStatus(status:Status) { 
        if (!status.hasUncommittedChanges() && status.isClean()) {
        mResult.setLength(0)
        mResult.append("Nothing to commit, working directory clean")
        return
        }
        mResult.setLength(0)
        convertStatusSet("Added files:", status.getAdded())
        convertStatusSet("Changed files:", status.getChanged())
        convertStatusSet("Removed files:", status.getRemoved())
        convertStatusSet("Missing files:", status.getMissing())
        convertStatusSet("Modified files:", status.getModified())
        convertStatusSet("Conflicting files:", status.getConflicting())
        convertStatusSet("Untracked files:", status.getUntracked())
        }
       
        fun convertStatusSet(str:String, set:Set<String>) { 
   
          if (!set.isEmpty()) {
            mResult.append(str)
            mResult.append("\n\n")
        for (s in set) {
            mResult.append("\t");
            mResult.append(s)
            mResult.append("\n")
              } 
            mResult.append("\n")     
			 } }
      
       }
