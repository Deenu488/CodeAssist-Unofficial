package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import java.lang.String
import org.eclipse.jgit.api.Git
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast
import com.tyron.code.ApplicationLoader
import com.tyron.code.R
import com.tyron.code.tasks.git.ErrorOutput
import android.content.Context

object ResetChangesTask {
   
    fun reset(project:Project, path:String, context:Context) {
        val future =
       executeAsyncProvideError({   
         Git.open(project.getRootFile()).checkout().addPath(path.toString()).call()
                return@executeAsyncProvideError   
       },    { _, _ -> })  
       
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
       if (result == null || error != null) {
       ErrorOutput.ShowError(error, context)
       } else {   Toast.makeText(context, context.getString(R.string.reset_changes_success), Toast.LENGTH_SHORT).show()      }
       }
       }

    }   
}
