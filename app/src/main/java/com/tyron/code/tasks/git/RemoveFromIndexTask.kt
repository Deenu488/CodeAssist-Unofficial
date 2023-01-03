package com.tyron.code.tasks.git
    
import com.tyron.builder.project.Project
import java.lang.String
import android.content.Context
import org.eclipse.jgit.api.Git
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast
import com.tyron.code.R
import com.tyron.code.tasks.git.ErrorOutput

object RemoveFromIndexTask {
   
    fun remove(project:Project, path:String, name:String, context:Context) {
        val future =
       executeAsyncProvideError({   
         Git.open(project.getRootFile()).rm().setCached(true).addFilepattern(path.toString()).call()
                return@executeAsyncProvideError   
       },    { _, _ -> })  
       
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
       if (result == null || error != null) {
       ErrorOutput.ShowError(error, context)
       } else {   Toast.makeText(context, name.toString() + " " + context.getString(R.string.removed_from_index), Toast.LENGTH_SHORT).show()      }
       }
       }

    }   
}
