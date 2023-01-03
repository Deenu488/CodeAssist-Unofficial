package com.tyron.code.tasks.git

import android.content.Context
import com.tyron.builder.project.Project
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tyron.code.tasks.git.GitInitTask
import com.tyron.code.tasks.git.GitStatusTask
import com.tyron.code.tasks.git.GitFetchTask
import com.tyron.code.tasks.git.GitAddAllToStageTask
import com.tyron.code.tasks.git.GitCommitTask


import com.tyron.code.tasks.git.GitPushTask
import com.tyron.code.tasks.git.GitPullTask
import com.tyron.code.tasks.git.GitMergeTask
import com.tyron.code.tasks.git.GitResetHardTask

object GitTask {
    
    fun showTasks(context:Context, project:Project) {
    
    val option = arrayOf(
            "Init",   
            "Status",
            "Fetch",
            "Add all to stage",
            "Commit",
            "Branch",
            "Remote",
            "Push",
            "Pull",
            "Merge",
            "Reset (HARD)",
            )
                    
       val builder = MaterialAlertDialogBuilder(context)  
       builder.setItems(option) { _, which -> 
       when (which) {
       
		 0 -> {  GitInitTask.init(project, context)  }
		 1 -> {  GitStatusTask.status(project, context)  }
		 2 -> {  GitFetchTask.fetch(project, context)  }
		 3 -> {  GitAddAllToStageTask.add(project, context)  }
		 4 -> {  GitCommitTask.commit(project, context)  }
	//	 5 -> {  GitFetchTask.fetch(project, context)  }
	//	 6 -> {  GitFetchTask.fetch(project, context)  }
		 7 -> {  GitPushTask.push(project, context)  }
		 8 -> {  GitPullTask.pull(project, context)  }
		 9 -> {  GitMergeTask.merge(project, context)  }
		 10 ->{  GitResetHardTask.resetHard(project, context)  }
		 
		 }
		 }
		 builder.show()
		 
    }
}
