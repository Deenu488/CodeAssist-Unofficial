package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Git
import com.tyron.code.tasks.git.ErrorOutput
import java.io.File
import android.content.Context
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast

object GitInitTask {
    
    fun init(project:Project, context:Context) {
    
    var file = File(project.getRootFile(), "/.git")
    var path = file.toString()
    val future =
       executeAsyncProvideError({
     
        if (file.exists()){ 
        Git.init().setDirectory(project.getRootFile()).call()
        ThreadUtils.runOnUiThread {   Toast.makeText(context,"Reinitialized existing Git repository in " + path,Toast.LENGTH_SHORT).show()   }
        
        } else {        
        Git.init().setDirectory(project.getRootFile()).call()
        ThreadUtils.runOnUiThread {   Toast.makeText(context,"Initialized empty Git repository in " + path,Toast.LENGTH_SHORT).show()   }   
        }
        
       return@executeAsyncProvideError
       },    { _, _ -> }) 
      
       future.whenComplete { result, error ->
   
       ThreadUtils.runOnUiThread {    
       if (result == null || error != null) {
       ErrorOutput.ShowError(error, context)
       } 
       }
                
      }  
     
   }  
}
